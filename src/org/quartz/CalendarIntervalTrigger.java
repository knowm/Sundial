/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 * 
 */

package org.quartz;

import java.util.Calendar;

import org.quartz.DateBuilder.IntervalUnit;

/**
 * A concrete <code>{@link Trigger}</code> that is used to fire a <code>{@link org.quartz.JobDetail}</code>
 * based upon repeating calendar time intervals.
 * 
 * <p>The trigger will fire every N (see {@link #setRepeatInterval(int)} ) units of calendar time
 * (see {@link #setRepeatIntervalUnit(IntervalUnit)}) as specified in the trigger's definition.  
 * This trigger can achieve schedules that are not possible with {@link SimpleTrigger} (e.g 
 * because months are not a fixed number of seconds) or {@link CronTrigger} (e.g. because
 * "every 5 months" is not an even divisor of 12).</p>
 * 
 * <p>If you use an interval unit of <code>MONTH</code> then care should be taken when setting
 * a <code>startTime</code> value that is on a day near the end of the month.  For example,
 * if you choose a start time that occurs on January 31st, and have a trigger with unit
 * <code>MONTH</code> and interval <code>1</code>, then the next fire time will be February 28th, 
 * and the next time after that will be March 28th - and essentially each subsequent firing will 
 * occur on the 28th of the month, even if a 31st day exists.  If you want a trigger that always
 * fires on the last day of the month - regardless of the number of days in the month, 
 * you should use <code>CronTrigger</code>.</p> 
 * 
 * @see TriggerBuilder
 * @see CalendarIntervalScheduleBuilder
 * @see SimpleScheduleBuilder
 * @see CronScheduleBuilder
 * 
 * @author James House
 */
public interface CalendarIntervalTrigger extends Trigger {

    /**
     * <p>
     * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire
     * situation, the <code>{@link CalendarIntervalTrigger}</code> wants to be 
     * fired now by <code>Scheduler</code>.
     * </p>
     */
    public static final int MISFIRE_INSTRUCTION_FIRE_ONCE_NOW = 1;
    /**
     * <p>
     * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire
     * situation, the <code>{@link CalendarIntervalTrigger}</code> wants to have it's
     * next-fire-time updated to the next time in the schedule after the
     * current time (taking into account any associated <code>{@link Calendar}</code>,
     * but it does not want to be fired now.
     * </p>
     */
    public static final int MISFIRE_INSTRUCTION_DO_NOTHING = 2;

    /**
     * <p>Get the interval unit - the time unit on with the interval applies.</p>
     */
    public IntervalUnit getRepeatIntervalUnit();

    /**
     * <p>
     * Get the the time interval that will be added to the <code>DateIntervalTrigger</code>'s
     * fire time (in the set repeat interval unit) in order to calculate the time of the 
     * next trigger repeat.
     * </p>
     */
    public int getRepeatInterval();

    /**
     * <p>
     * Get the number of times the <code>DateIntervalTrigger</code> has already
     * fired.
     * </p>
     */
    public int getTimesTriggered();

	TriggerBuilder<CalendarIntervalTrigger> getTriggerBuilder();
}