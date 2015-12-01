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

import java.util.Date;

import org.quartz.core.Calendar;
import org.quartz.core.JobExecutionContext;
import org.quartz.core.Scheduler;
import org.quartz.exceptions.JobExecutionException;
import org.quartz.exceptions.SchedulerException;

/**
 * <p>
 * A concrete <code>{@link Trigger}</code> that is used to fire a <code>{@link org.quartz.jobs.JobDetail}</code> at a given moment in time, and
 * optionally repeated at a specified interval.
 * </p>
 *
 * @author James House
 * @author contributions by Lieven Govaerts of Ebitec Nv, Belgium.
 */
public class SimpleTriggerImpl extends AbstractTrigger implements SimpleTrigger {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constants.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private static final int YEAR_TO_GIVEUP_SCHEDULING_AT = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + 100;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private int repeatCount = 0;
  private long repeatInterval = 0;
  private int timesTriggered = 0;
  private final boolean complete = false;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Create a <code>SimpleTrigger</code> with no settings.
   * </p>
   */
  public SimpleTriggerImpl() {

    super();
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Get the time at which the <code>SimpleTrigger</code> should occur.
   * </p>
   */
  @Override
  public Date getStartTime() {

    return startTime;
  }

  /**
   * <p>
   * Set the time at which the <code>SimpleTrigger</code> should occur.
   * </p>
   *
   * @exception IllegalArgumentException if startTime is <code>null</code>.
   */
  @Override
  public void setStartTime(Date startTime) {

    if (startTime == null) {
      throw new IllegalArgumentException("Start time cannot be null");
    }

    Date eTime = getEndTime();
    if (eTime != null && startTime != null && eTime.before(startTime)) {
      throw new IllegalArgumentException("End time cannot be before start time");
    }

    this.startTime = startTime;
  }

  /**
   * <p>
   * Get the time at which the <code>SimpleTrigger</code> should quit repeating - even if repeastCount isn't yet satisfied.
   * </p>
   *
   * @see #getFinalFireTime()
   */
  @Override
  public Date getEndTime() {

    return endTime;
  }

  /**
   * <p>
   * Set the time at which the <code>SimpleTrigger</code> should quit repeating (and be automatically deleted).
   * </p>
   *
   * @exception IllegalArgumentException if endTime is before start time.
   */
  @Override
  public void setEndTime(Date endTime) {

    Date sTime = getStartTime();
    if (sTime != null && endTime != null && sTime.after(endTime)) {
      throw new IllegalArgumentException("End time cannot be before start time");
    }

    this.endTime = endTime;
  }

  @Override
  public int getRepeatCount() {

    return repeatCount;
  }

  /**
   * <p>
   * Set the the number of time the <code>SimpleTrigger</code> should repeat, after which it will be automatically deleted.
   * </p>
   *
   * @see #REPEAT_INDEFINITELY
   * @exception IllegalArgumentException if repeatCount is < 0
   */
  public void setRepeatCount(int repeatCount) {

    if (repeatCount < 0 && repeatCount != REPEAT_INDEFINITELY) {
      throw new IllegalArgumentException("Repeat count must be >= 0, use the " + "constant REPEAT_INDEFINITELY for infinite.");
    }

    this.repeatCount = repeatCount;
  }

  @Override
  public long getRepeatInterval() {

    return repeatInterval;
  }

  /**
   * <p>
   * Set the the time interval (in milliseconds) at which the <code>SimpleTrigger</code> should repeat.
   * </p>
   *
   * @exception IllegalArgumentException if repeatInterval is <= 0
   */
  public void setRepeatInterval(long repeatInterval) {

    if (repeatInterval < 0) {
      throw new IllegalArgumentException("Repeat interval must be >= 0");
    }

    this.repeatInterval = repeatInterval;
  }

  /**
   * <p>
   * Get the number of times the <code>SimpleTrigger</code> has already fired.
   * </p>
   */
  @Override
  public int getTimesTriggered() {

    return timesTriggered;
  }

  /**
   * <p>
   * Set the number of times the <code>SimpleTrigger</code> has already fired.
   * </p>
   */
  public void setTimesTriggered(int timesTriggered) {

    this.timesTriggered = timesTriggered;
  }

  @Override
  protected boolean validateMisfireInstruction(int misfireInstruction) {

    if (misfireInstruction < MISFIRE_INSTRUCTION_SMART_POLICY) {
      return false;
    }

    if (misfireInstruction > MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT) {
      return false;
    }

    return true;
  }

  /**
   * <p>
   * Updates the <code>SimpleTrigger</code>'s state based on the MISFIRE_INSTRUCTION_XXX that was selected when the <code>SimpleTrigger</code> was
   * created.
   * </p>
   * <p>
   * If the misfire instruction is set to MISFIRE_INSTRUCTION_SMART_POLICY, then the following scheme will be used: <br>
   * <ul>
   * <li>If the Repeat Count is <code>0</code>, then the instruction will be interpreted as <code>MISFIRE_INSTRUCTION_FIRE_NOW</code>.</li>
   * <li>If the Repeat Count is <code>REPEAT_INDEFINITELY</code>, then the instruction will be interpreted as
   * <code>MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT</code>. <b>WARNING:</b> using
   * MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT with a trigger that has a non-null end-time may cause the trigger to never fire again if
   * the end-time arrived during the misfire time span.</li>
   * <li>If the Repeat Count is <code>&gt; 0</code>, then the instruction will be interpreted as
   * <code>MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT</code>.</li>
   * </ul>
   * </p>
   * <p>
   * If the misfire instruction is set to <code>Trigger.MISFIRE_INSTRUCTION_SKIP_TO_NEXT_FIRE_AFTER_CURRENT_DATE</code> then the behavior will be
   * identical to
   */
  @Override
  public void updateAfterMisfire(Calendar cal) {

    int instr = getMisfireInstruction();

    if (instr == Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY) {
      return;
    }

    if (instr == Trigger.MISFIRE_INSTRUCTION_SMART_POLICY) {
      if (getRepeatCount() == 0) {
        instr = MISFIRE_INSTRUCTION_FIRE_NOW;
      } else if (getRepeatCount() == REPEAT_INDEFINITELY) {
        instr = MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT;
      } else {
        // if (getRepeatCount() > 0)
        instr = MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT;
      }
    } else if (instr == MISFIRE_INSTRUCTION_FIRE_NOW && getRepeatCount() != 0) {
      instr = MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT;
    }

    if (instr == MISFIRE_INSTRUCTION_FIRE_NOW) {
      setNextFireTime(new Date());
    } else if (instr == MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT) {
      Date newFireTime = getFireTimeAfter(new Date());
      while (newFireTime != null && cal != null && !cal.isTimeIncluded(newFireTime.getTime())) {
        newFireTime = getFireTimeAfter(newFireTime);

        if (newFireTime == null) {
          break;
        }

        // avoid infinite loop
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTime(newFireTime);
        if (c.get(java.util.Calendar.YEAR) > YEAR_TO_GIVEUP_SCHEDULING_AT) {
          newFireTime = null;
        }
      }
      setNextFireTime(newFireTime);
    } else if (instr == MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT) {
      Date newFireTime = getFireTimeAfter(new Date());
      while (newFireTime != null && cal != null && !cal.isTimeIncluded(newFireTime.getTime())) {
        newFireTime = getFireTimeAfter(newFireTime);

        if (newFireTime == null) {
          break;
        }

        // avoid infinite loop
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTime(newFireTime);
        if (c.get(java.util.Calendar.YEAR) > YEAR_TO_GIVEUP_SCHEDULING_AT) {
          newFireTime = null;
        }
      }
      if (newFireTime != null) {
        int timesMissed = computeNumTimesFiredBetween(nextFireTime, newFireTime);
        setTimesTriggered(getTimesTriggered() + timesMissed);
      }

      setNextFireTime(newFireTime);
    } else if (instr == MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT) {
      Date newFireTime = new Date();
      if (repeatCount != 0 && repeatCount != REPEAT_INDEFINITELY) {
        setRepeatCount(getRepeatCount() - getTimesTriggered());
        setTimesTriggered(0);
      }

      if (getEndTime() != null && getEndTime().before(newFireTime)) {
        setNextFireTime(null); // We are past the end time
      } else {
        setStartTime(newFireTime);
        setNextFireTime(newFireTime);
      }
    } else if (instr == MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT) {
      Date newFireTime = new Date();

      int timesMissed = computeNumTimesFiredBetween(nextFireTime, newFireTime);

      if (repeatCount != 0 && repeatCount != REPEAT_INDEFINITELY) {
        int remainingCount = getRepeatCount() - (getTimesTriggered() + timesMissed);
        if (remainingCount <= 0) {
          remainingCount = 0;
        }
        setRepeatCount(remainingCount);
        setTimesTriggered(0);
      }

      if (getEndTime() != null && getEndTime().before(newFireTime)) {
        setNextFireTime(null); // We are past the end time
      } else {
        setStartTime(newFireTime);
        setNextFireTime(newFireTime);
      }
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
  public void triggered(Calendar calendar) {

    timesTriggered++;
    previousFireTime = nextFireTime;
    nextFireTime = getFireTimeAfter(nextFireTime);

    while (nextFireTime != null && calendar != null && !calendar.isTimeIncluded(nextFireTime.getTime())) {

      nextFireTime = getFireTimeAfter(nextFireTime);

      if (nextFireTime == null) {
        break;
      }

      // avoid infinite loop
      java.util.Calendar c = java.util.Calendar.getInstance();
      c.setTime(nextFireTime);
      if (c.get(java.util.Calendar.YEAR) > YEAR_TO_GIVEUP_SCHEDULING_AT) {
        nextFireTime = null;
      }
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
  public Date computeFirstFireTime(Calendar calendar) {

    nextFireTime = getStartTime();

    while (nextFireTime != null && calendar != null && !calendar.isTimeIncluded(nextFireTime.getTime())) {
      nextFireTime = getFireTimeAfter(nextFireTime);

      if (nextFireTime == null) {
        break;
      }

      // avoid infinite loop
      java.util.Calendar c = java.util.Calendar.getInstance();
      c.setTime(nextFireTime);
      if (c.get(java.util.Calendar.YEAR) > YEAR_TO_GIVEUP_SCHEDULING_AT) {
        return null;
      }
    }

    return nextFireTime;
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
   * @see TriggerUtils#computeFireTimesBetween(Trigger, Calendar, Date, Date)
   */
  @Override
  public Date getNextFireTime() {

    return nextFireTime;
  }

  /**
   * <p>
   * Returns the previous time at which the <code>SimpleTrigger</code> fired. If the trigger has not yet fired, <code>null</code> will be returned.
   */
  @Override
  public Date getPreviousFireTime() {

    return previousFireTime;
  }

  /**
   * <p>
   * Set the next time at which the <code>SimpleTrigger</code> should fire.
   * </p>
   * <p>
   * <b>This method should not be invoked by client code.</b>
   * </p>
   */
  @Override
  public void setNextFireTime(Date nextFireTime) {

    this.nextFireTime = nextFireTime;
  }

  /**
   * <p>
   * Set the previous time at which the <code>SimpleTrigger</code> fired.
   * </p>
   * <p>
   * <b>This method should not be invoked by client code.</b>
   * </p>
   */
  @Override
  public void setPreviousFireTime(Date previousFireTime) {

    this.previousFireTime = previousFireTime;
  }

  /**
   * <p>
   * Returns the next time at which the <code>SimpleTrigger</code> will fire, after the given time. If the trigger will not fire after the given time,
   * <code>null</code> will be returned.
   * </p>
   */
  @Override
  public Date getFireTimeAfter(Date afterTime) {

    if (complete) {
      return null;
    }

    if ((timesTriggered > repeatCount) && (repeatCount != REPEAT_INDEFINITELY)) {
      return null;
    }

    if (afterTime == null) {
      afterTime = new Date();
    }

    if (repeatCount == 0 && afterTime.compareTo(getStartTime()) >= 0) {
      return null;
    }

    long startMillis = getStartTime().getTime();
    long afterMillis = afterTime.getTime();
    long endMillis = (getEndTime() == null) ? Long.MAX_VALUE : getEndTime().getTime();

    if (endMillis <= afterMillis) {
      return null;
    }

    if (afterMillis < startMillis) {
      return new Date(startMillis);
    }

    long numberOfTimesExecuted = ((afterMillis - startMillis) / repeatInterval) + 1;

    if ((numberOfTimesExecuted > repeatCount) && (repeatCount != REPEAT_INDEFINITELY)) {
      return null;
    }

    Date time = new Date(startMillis + (numberOfTimesExecuted * repeatInterval));

    if (endMillis <= time.getTime()) {
      return null;
    }

    return time;
  }

  /**
   * <p>
   * Returns the last time at which the <code>SimpleTrigger</code> will fire, before the given time. If the trigger will not fire before the given
   * time, <code>null</code> will be returned.
   * </p>
   */
  private Date getFireTimeBefore(Date end) {

    if (end.getTime() < getStartTime().getTime()) {
      return null;
    }

    int numFires = computeNumTimesFiredBetween(getStartTime(), end);

    return new Date(getStartTime().getTime() + (numFires * repeatInterval));
  }

  private int computeNumTimesFiredBetween(Date start, Date end) {

    if (repeatInterval < 1) {
      return 0;
    }

    long time = end.getTime() - start.getTime();

    return (int) (time / repeatInterval);
  }

  /**
   * <p>
   * Returns the final time at which the <code>SimpleTrigger</code> will fire, if repeatCount is REPEAT_INDEFINITELY, null will be returned.
   * </p>
   * <p>
   * Note that the return time may be in the past.
   * </p>
   */
  @Override
  public Date getFinalFireTime() {

    if (repeatCount == 0) {
      return startTime;
    }

    if (repeatCount == REPEAT_INDEFINITELY) {
      return (getEndTime() == null) ? null : getFireTimeBefore(getEndTime());
    }

    long lastTrigger = startTime.getTime() + (repeatCount * repeatInterval);

    if ((getEndTime() == null) || (lastTrigger < getEndTime().getTime())) {
      return new Date(lastTrigger);
    } else {
      return getFireTimeBefore(getEndTime());
    }
  }

  /**
   * <p>
   * Determines whether or not the <code>SimpleTrigger</code> will occur again.
   * </p>
   */
  @Override
  public boolean mayFireAgain() {

    return (getNextFireTime() != null);
  }

  /**
   * <p>
   * Validates whether the properties of the <code>JobDetail</code> are valid for submission into a <code>Scheduler</code>.
   *
   * @throws IllegalStateException if a required property (such as Name, Group, Class) is not set.
   */
  @Override
  public void validate() throws SchedulerException {

    super.validate();

    if (repeatCount != 0 && repeatInterval < 1) {
      throw new SchedulerException("Repeat Interval cannot be zero.");
    }
  }

  @Override
  public String toString() {

    return super.toString() + ", repeatCount: " + getRepeatCount() + ", repeatInterval: " + getRepeatInterval();
  }
}
