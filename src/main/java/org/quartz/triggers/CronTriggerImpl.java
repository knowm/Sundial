/**
 * Copyright 2015 Knowm Inc. (http://knowm.org) and contributors.
 * Copyright 2013-2015 Xeiam LLC (http://xeiam.com) and contributors.
 * Copyright 2001-2011 Terracotta Inc. (http://terracotta.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quartz.triggers;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.quartz.core.JobExecutionContext;
import org.quartz.core.Scheduler;
import org.quartz.exceptions.JobExecutionException;

/**
 * <p>
 * A concrete <code>{@link Trigger}</code> that is used to fire a <code>{@link org.quartz.jobs.JobDetail}</code> at given moments in time, defined
 * with Unix 'cron-like' definitions.
 * </p>
 *
 * @author Sharada Jambula, James House
 * @author Contributions from Mads Henderson
 */
public class CronTriggerImpl extends AbstractTrigger implements CronTrigger {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constants.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * Required for serialization support. Introduced in Quartz 1.6.1 to maintain compatibility after the introduction of hasAdditionalProperties
   * method.
   *
   * @see java.io.Serializable
   */
  private static final long serialVersionUID = -8644953146451592766L;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private CronExpression cronEx = null;
  private transient TimeZone timeZone = null;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Create a <code>CronTrigger</code> with no settings.
   * </p>
   * <p>
   * The start-time will also be set to the current time, and the time zone will be set the the system's default time zone.
   * </p>
   */
  public CronTriggerImpl() {

    super();
    setStartTime(new Date());
    setTimeZone(TimeZone.getDefault());
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  @Override
  public Object clone() {

    CronTriggerImpl copy = (CronTriggerImpl) super.clone();
    if (cronEx != null) {
      copy.setCronExpression(new CronExpression(cronEx));
    }
    return copy;
  }

  public void setCronExpression(String cronExpression) throws ParseException {

    TimeZone origTz = getTimeZone();
    this.cronEx = new CronExpression(cronExpression);
    this.cronEx.setTimeZone(origTz);
  }

  @Override
  public String getCronExpression() {

    return cronEx == null ? null : cronEx.getCronExpression();
  }

  /**
   * Set the CronExpression to the given one. The TimeZone on the passed-in CronExpression over-rides any that was already set on the Trigger.
   *
   * @param cronExpression
   */
  public void setCronExpression(CronExpression cronExpression) {

    this.cronEx = cronExpression;
    this.timeZone = cronExpression.getTimeZone();
  }

  /**
   * <p>
   * Get the time at which the <code>CronTrigger</code> should occur.
   * </p>
   */
  @Override
  public Date getStartTime() {

    return this.startTime;
  }

  @Override
  public void setStartTime(Date startTime) {

    if (startTime == null) {
      throw new IllegalArgumentException("Start time cannot be null");
    }

    Date eTime = getEndTime();
    if (eTime != null && startTime != null && eTime.before(startTime)) {
      throw new IllegalArgumentException("End time cannot be before start time");
    }

    // round off millisecond...
    // Note timeZone is not needed here as parameter for
    // Calendar.getInstance(),
    // since time zone is implicit when using a Date in the setTime method.
    Calendar cl = Calendar.getInstance();
    cl.setTime(startTime);
    cl.set(Calendar.MILLISECOND, 0);

    this.startTime = cl.getTime();
  }

  /**
   * <p>
   * Get the time at which the <code>CronTrigger</code> should quit repeating - even if repeastCount isn't yet satisfied.
   * </p>
   *
   * @see #getFinalFireTime()
   */
  @Override
  public Date getEndTime() {

    return this.endTime;
  }

  @Override
  public void setEndTime(Date endTime) {

    Date sTime = getStartTime();
    if (sTime != null && endTime != null && sTime.after(endTime)) {
      throw new IllegalArgumentException("End time cannot be before start time");
    }

    this.endTime = endTime;
  }

  /**
   * <p>
   * Returns the next time at which the <code>Trigger</code> is scheduled to fire. If the trigger will not fire again, <code>null</code> will be
   * returned. Note that the time returned can possibly be in the past, if the time that was computed for the trigger to next fire has already
   * arrived, but the scheduler has not yet been able to fire the trigger (which would likely be due to lack of resources e.g. threads).
   * </p>
   * <p>
   * The value returned is not guaranteed to be valid until after the <code>Trigger</code> has been added to the scheduler.
   * </p>
   *
   * @see TriggerUtils#computeFireTimesBetween(Trigger, org.quartz.core.Calendar , Date, Date)
   */
  @Override
  public Date getNextFireTime() {

    return this.nextFireTime;
  }

  /**
   * <p>
   * Returns the previous time at which the <code>CronTrigger</code> fired. If the trigger has not yet fired, <code>null</code> will be returned.
   */
  @Override
  public Date getPreviousFireTime() {

    return this.previousFireTime;
  }

  /**
   * <p>
   * Sets the next time at which the <code>CronTrigger</code> will fire. <b>This method should not be invoked by client code.</b>
   * </p>
   */
  @Override
  public void setNextFireTime(Date nextFireTime) {

    this.nextFireTime = nextFireTime;
  }

  /**
   * <p>
   * Set the previous time at which the <code>CronTrigger</code> fired.
   * </p>
   * <p>
   * <b>This method should not be invoked by client code.</b>
   * </p>
   */
  @Override
  public void setPreviousFireTime(Date previousFireTime) {

    this.previousFireTime = previousFireTime;
  }

  @Override
  public TimeZone getTimeZone() {

    if (cronEx != null) {
      return cronEx.getTimeZone();
    }

    if (timeZone == null) {
      timeZone = TimeZone.getDefault();
    }
    return timeZone;
  }

  /**
   * <p>
   * Sets the time zone for which the <code>cronExpression</code> of this <code>CronTrigger</code> will be resolved.
   * </p>
   * <p>
   * If {@link #setCronExpression(CronExpression)} is called after this method, the TimeZone setting on the CronExpression will "win". However if
   * {@link #setCronExpression(String)} is called after this method, the time zone applied by this method will remain in effect, since the String cron
   * expression does not carry a time zone!
   */
  public void setTimeZone(TimeZone timeZone) {

    if (cronEx != null) {
      cronEx.setTimeZone(timeZone);
    }
    this.timeZone = timeZone;
  }

  /**
   * <p>
   * Returns the next time at which the <code>CronTrigger</code> will fire, after the given time. If the trigger will not fire after the given time,
   * <code>null</code> will be returned.
   * </p>
   * <p>
   * Note that the date returned is NOT validated against the related org.quartz.Calendar (if any)
   * </p>
   */
  @Override
  public Date getFireTimeAfter(Date afterTime) {

    if (afterTime == null) {
      afterTime = new Date();
    }

    if (getStartTime().after(afterTime)) {
      afterTime = new Date(getStartTime().getTime() - 1000l);
    }

    if (getEndTime() != null && (afterTime.compareTo(getEndTime()) >= 0)) {
      return null;
    }

    Date pot = getTimeAfter(afterTime);
    if (getEndTime() != null && pot != null && pot.after(getEndTime())) {
      return null;
    }

    return pot;
  }

  /**
   * <p>
   * NOT YET IMPLEMENTED: Returns the final time at which the <code>CronTrigger</code> will fire.
   * </p>
   * <p>
   * Note that the return time *may* be in the past. and the date returned is not validated against org.quartz.calendar
   * </p>
   */
  @Override
  public Date getFinalFireTime() {

    Date resultTime;
    if (getEndTime() != null) {
      resultTime = getTimeBefore(new Date(getEndTime().getTime() + 1000l));
    } else {
      resultTime = (cronEx == null) ? null : cronEx.getFinalFireTime();
    }

    if ((resultTime != null) && (getStartTime() != null) && (resultTime.before(getStartTime()))) {
      return null;
    }

    return resultTime;
  }

  /**
   * <p>
   * Determines whether or not the <code>CronTrigger</code> will occur again.
   * </p>
   */
  @Override
  public boolean mayFireAgain() {

    return (getNextFireTime() != null);
  }

  @Override
  protected boolean validateMisfireInstruction(int misfireInstruction) {

    if (misfireInstruction < MISFIRE_INSTRUCTION_SMART_POLICY) {
      return false;
    }

    if (misfireInstruction > MISFIRE_INSTRUCTION_DO_NOTHING) {
      return false;
    }

    return true;
  }

  /**
   * <p>
   * Updates the <code>CronTrigger</code>'s state based on the MISFIRE_INSTRUCTION_XXX that was selected when the <code>CronTrigger</code> was
   * created.
   * </p>
   * <p>
   * If the misfire instruction is set to MISFIRE_INSTRUCTION_SMART_POLICY, then the following scheme will be used: <br>
   * <ul>
   * <li>The instruction will be interpreted as <code>MISFIRE_INSTRUCTION_FIRE_ONCE_NOW</code>
   * </ul>
   * </p>
   */
  @Override
  public void updateAfterMisfire(org.quartz.core.Calendar cal) {

    int instr = getMisfireInstruction();

    if (instr == Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY) {
      return;
    }

    if (instr == MISFIRE_INSTRUCTION_SMART_POLICY) {
      instr = MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
    }

    if (instr == MISFIRE_INSTRUCTION_DO_NOTHING) {
      Date newFireTime = getFireTimeAfter(new Date());
      while (newFireTime != null && cal != null && !cal.isTimeIncluded(newFireTime.getTime())) {
        newFireTime = getFireTimeAfter(newFireTime);
      }
      setNextFireTime(newFireTime);
    } else if (instr == MISFIRE_INSTRUCTION_FIRE_ONCE_NOW) {
      setNextFireTime(new Date());
    }
  }

  /**
   * <p>
   * Called when the <code>{@link Scheduler}</code> has decided to 'fire' the trigger (execute the associated <code>Job</code>), in order to give the
   * <code>Trigger</code> a chance to update itself for its next triggering (if any).
   * </p>
   *
   * @see #executionComplete(JobExecutionContext, JobExecutionException)
   */
  @Override
  public void triggered(org.quartz.core.Calendar calendar) {

    previousFireTime = nextFireTime;
    nextFireTime = getFireTimeAfter(nextFireTime);

    while (nextFireTime != null && calendar != null && !calendar.isTimeIncluded(nextFireTime.getTime())) {
      nextFireTime = getFireTimeAfter(nextFireTime);
    }
  }

  /**
   * <p>
   * Called by the scheduler at the time a <code>Trigger</code> is first added to the scheduler, in order to have the <code>Trigger</code> compute its
   * first fire time, based on any associated calendar.
   * </p>
   * <p>
   * After this method has been called, <code>getNextFireTime()</code> should return a valid answer.
   * </p>
   *
   * @return the first time at which the <code>Trigger</code> will be fired by the scheduler, which is also the same value
   *         <code>getNextFireTime()</code> will return (until after the first firing of the <code>Trigger</code>).
   *         </p>
   */
  @Override
  public Date computeFirstFireTime(org.quartz.core.Calendar calendar) {

    nextFireTime = getFireTimeAfter(new Date(getStartTime().getTime() - 1000l));

    while (nextFireTime != null && calendar != null && !calendar.isTimeIncluded(nextFireTime.getTime())) {
      nextFireTime = getFireTimeAfter(nextFireTime);
    }

    return nextFireTime;
  }

  @Override
  public String getExpressionSummary() {

    return cronEx == null ? null : cronEx.getExpressionSummary();
  }

  // //////////////////////////////////////////////////////////////////////////
  //
  // Computation Functions
  //
  // //////////////////////////////////////////////////////////////////////////

  private Date getTimeAfter(Date afterTime) {

    return (cronEx == null) ? null : cronEx.getTimeAfter(afterTime);
  }

  /**
   * NOT YET IMPLEMENTED: Returns the time before the given time that this <code>CronTrigger</code> will fire.
   */
  private Date getTimeBefore(Date endTime) {

    return (cronEx == null) ? null : cronEx.getTimeBefore(endTime);
  }

  @Override
  public String toString() {

    return super.toString() + ", cronEx: " + getCronExpression() + ", timeZone: " + getTimeZone().getID();
  }

}
