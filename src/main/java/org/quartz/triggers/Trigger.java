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

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

import org.quartz.builders.TriggerBuilder;
import org.quartz.core.Calendar;
import org.quartz.core.Scheduler;
import org.quartz.jobs.JobDataMap;

/**
 * The base interface with properties common to all <code>Trigger</code>s - use {@link TriggerBuilder} to instantiate an actual Trigger.
 * <p>
 * <code>Triggers</code>s have a {@link TriggerKey} associated with them, which should uniquely identify them within a single
 * <code>{@link Scheduler}</code>.
 * </p>
 * <p>
 * <code>Trigger</code>s are the 'mechanism' by which <code>Job</code>s are scheduled. Many <code>Trigger</code>s can point to the same
 * <code>Job</code>, but a single <code>Trigger</code> can only point to one <code>Job</code>.
 * </p>
 * <p>
 * Triggers can 'send' parameters/data to <code>Job</code>s by placing contents into the <code>JobDataMap</code> on the <code>Trigger</code>.
 * </p>
 *
 * @author James House
 */
public interface Trigger extends Serializable, Cloneable, Comparable<Trigger> {

  public static final long serialVersionUID = -3904243490805975570L;

  /**
   * <p>
   * <code>NOOP</code> Instructs the <code>{@link Scheduler}</code> that the <code>{@link Trigger}</code> has no further instructions.
   * </p>
   * <p>
   * <code>RE_EXECUTE_JOB</code> Instructs the <code>{@link Scheduler}</code> that the <code>{@link Trigger}</code> wants the
   * <code>{@link org.quartz.jobs.JobDetail}</code> to re-execute immediately. If not in a 'RECOVERING' or 'FAILED_OVER' situation, the execution
   * context will be re-used (giving the <code>Job</code> the ability to 'see' anything placed in the context by its last execution).
   * </p>
   * <p>
   * <code>SET_TRIGGER_COMPLETE</code> Instructs the <code>{@link Scheduler}</code> that the <code>{@link Trigger}</code> should be put in the
   * <code>COMPLETE</code> state.
   * </p>
   * <p>
   * <code>DELETE_TRIGGER</code> Instructs the <code>{@link Scheduler}</code> that the <code>{@link Trigger}</code> wants itself deleted.
   * </p>
   * <p>
   * <code>SET_ALL_JOB_TRIGGERS_COMPLETE</code> Instructs the <code>{@link Scheduler}</code> that all <code>Trigger</code>s referencing the same
   * <code>{@link org.quartz.jobs.JobDetail}</code> as this one should be put in the <code>COMPLETE</code> state.
   * </p>
   * <p>
   * <code>SET_TRIGGER_ERROR</code> Instructs the <code>{@link Scheduler}</code> that all <code>Trigger</code>s referencing the same
   * <code>{@link org.quartz.jobs.JobDetail}</code> as this one should be put in the <code>ERROR</code> state.
   * </p>
   * <p>
   * <code>SET_ALL_JOB_TRIGGERS_ERROR</code> Instructs the <code>{@link Scheduler}</code> that the <code>Trigger</code> should be put in the
   * <code>ERROR</code> state.
   * </p>
   */
  public enum CompletedExecutionInstruction {
    NOOP, RE_EXECUTE_JOB, SET_TRIGGER_COMPLETE, DELETE_TRIGGER, SET_ALL_JOB_TRIGGERS_COMPLETE, SET_TRIGGER_ERROR, SET_ALL_JOB_TRIGGERS_ERROR
  };

  /**
   * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire situation, the <code>updateAfterMisfire()</code> method will be called on the
   * <code>Trigger</code> to determine the mis-fire instruction, which logic will be trigger-implementation-dependent.
   * <p>
   * In order to see if this instruction fits your needs, you should look at the documentation for the <code>getSmartMisfirePolicy()</code> method on
   * the particular <code>Trigger</code> implementation you are using.
   * </p>
   */
  public static final int MISFIRE_INSTRUCTION_SMART_POLICY = 0;

  /**
   * Instructs the <code>{@link Scheduler}</code> that the <code>Trigger</code> will never be evaluated for a misfire situation, and that the
   * scheduler will simply try to fire it as soon as it can, and then update the Trigger as if it had fired at the proper time.
   * <p>
   * NOTE: if a trigger uses this instruction, and it has missed several of its scheduled firings, then
   */
  public static final int MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY = -1;

  /**
   * The default value for priority.
   */
  public static final int DEFAULT_PRIORITY = 5;

  /**
   * <p>
   * Get the name of this <code>Trigger</code>.
   * </p>
   */
  public String getName();

  /**
   * <p>
   * Get the name of the associated <code>{@link org.quartz.jobs.JobDetail}</code>.
   * </p>
   */
  public String getJobName();

  /**
   * Return the description given to the <code>Trigger</code> instance by its creator (if any).
   *
   * @return null if no description was set.
   */
  public String getDescription();

  /**
   * Get the name of the <code>{@link Calendar}</code> associated with this Trigger.
   *
   * @return <code>null</code> if there is no associated Calendar.
   */
  public String getCalendarName();

  /**
   * Get the <code>JobDataMap</code> that is associated with the <code>Trigger</code>.
   * <p>
   * Changes made to this map during job execution are not re-persisted, and in fact typically result in an <code>IllegalStateException</code>.
   * </p>
   */
  public JobDataMap getJobDataMap();

  /**
   * The priority of a <code>Trigger</code> acts as a tiebreaker such that if two <code>Trigger</code>s have the same scheduled fire time, then the
   * one with the higher priority will get first access to a worker thread.
   * <p>
   * If not explicitly set, the default value is <code>5</code>.
   * </p>
   *
   * @see #DEFAULT_PRIORITY
   */
  public int getPriority();

  /**
   * Used by the <code>{@link Scheduler}</code> to determine whether or not it is possible for this <code>Trigger</code> to fire again.
   * <p>
   * If the returned value is <code>false</code> then the <code>Scheduler</code> may remove the <code>Trigger</code> from the
   * <code>{@link org.quartz.core.JobStore}</code>.
   * </p>
   */
  public boolean mayFireAgain();

  /**
   * Get the time at which the <code>Trigger</code> should occur.
   */
  public Date getStartTime();

  /**
   * Get the time at which the <code>Trigger</code> should quit repeating - regardless of any remaining repeats (based on the trigger's particular
   * repeat settings).
   *
   * @see #getFinalFireTime()
   */
  public Date getEndTime();

  /**
   * Returns the next time at which the <code>Trigger</code> is scheduled to fire. If the trigger will not fire again, <code>null</code> will be
   * returned. Note that the time returned can possibly be in the past, if the time that was computed for the trigger to next fire has already
   * arrived, but the scheduler has not yet been able to fire the trigger (which would likely be due to lack of resources e.g. threads).
   * <p>
   * The value returned is not guaranteed to be valid until after the <code>Trigger</code> has been added to the scheduler.
   * </p>
   *
   * @see TriggerUtils#computeFireTimesBetween(Trigger, Calendar, Date, Date)
   */
  public Date getNextFireTime();

  /**
   * Returns the previous time at which the <code>Trigger</code> fired. If the trigger has not yet fired, <code>null</code> will be returned.
   */
  public Date getPreviousFireTime();

  /**
   * Returns the next time at which the <code>Trigger</code> will fire, after the given time. If the trigger will not fire after the given time,
   * <code>null</code> will be returned.
   */
  public Date getFireTimeAfter(Date afterTime);

  /**
   * Returns the last time at which the <code>Trigger</code> will fire, if the Trigger will repeat indefinitely, null will be returned.
   * <p>
   * Note that the return time *may* be in the past.
   * </p>
   */
  public Date getFinalFireTime();

  /**
   * Get the instruction the <code>Scheduler</code> should be given for handling misfire situations for this <code>Trigger</code>- the concrete
   * <code>Trigger</code> type that you are using will have defined a set of additional <code>MISFIRE_INSTRUCTION_XXX</code> constants that may be set
   * as this property's value.
   * <p>
   * If not explicitly set, the default value is <code>MISFIRE_INSTRUCTION_SMART_POLICY</code>.
   * </p>
   *
   * @see #MISFIRE_INSTRUCTION_SMART_POLICY
   * @see #updateAfterMisfire(Calendar)
   * @see SimpleTrigger
   * @see CronTrigger
   */
  public int getMisfireInstruction();

  /**
   * Trigger equality is based upon the equality of the TriggerKey.
   *
   * @return true if the key of this Trigger equals that of the given Trigger.
   */
  @Override
  public boolean equals(Object other);

  /**
   * A Comparator that compares trigger's next fire times, or in other words, sorts them according to earliest next fire time. If the fire times are
   * the same, then the triggers are sorted according to priority (highest value first), if the priorities are the same, then they are sorted by key.
   */
  class TriggerTimeComparator implements Comparator<Trigger>, Serializable {

    @Override
    public int compare(Trigger trig1, Trigger trig2) {

      Date t1 = trig1.getNextFireTime();
      Date t2 = trig2.getNextFireTime();

      if (t1 != null || t2 != null) {
        if (t1 == null) {
          return 1;
        }

        if (t2 == null) {
          return -1;
        }

        if (t1.before(t2)) {
          return -1;
        }

        if (t1.after(t2)) {
          return 1;
        }
      }

      int comp = trig2.getPriority() - trig1.getPriority();
      if (comp != 0) {
        return comp;
      }

      return trig1.getName().compareTo(trig2.getName());
    }
  }
}
