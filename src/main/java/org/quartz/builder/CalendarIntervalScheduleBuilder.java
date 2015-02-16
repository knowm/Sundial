/**
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
package org.quartz.builder;

import org.quartz.trigger.CalendarIntervalTrigger;
import org.quartz.trigger.CalendarIntervalTrigger.IntervalUnit;
import org.quartz.trigger.CalendarIntervalTriggerImpl;
import org.quartz.trigger.MutableTrigger;

/**
 * <code>CalendarIntervalScheduleBuilder</code> is a {@link ScheduleBuilder} that defines calendar time (day, week, month, year) interval-based
 * schedules for <code>Trigger</code>s.
 * <p>
 * Quartz provides a builder-style API for constructing scheduling-related entities via a Domain-Specific Language (DSL). The DSL can best be utilized
 * through the usage of static imports of the methods on the classes <code>TriggerBuilder</code>, <code>JobBuilder</code>, <code>DateBuilder</code>,
 * <code>JobKey</code>, <code>TriggerKey</code> and the various <code>ScheduleBuilder</code> implementations.
 * </p>
 * <p>
 * Client code can then use the DSL to write code such as this:
 * </p>
 *
 * <pre>
 *
 *
 *
 *
 *
 *
 *
 * JobDetail job = newJob(MyJob.class).withIdentity(&quot;myJob&quot;).build();
 * 
 * Trigger trigger = newTrigger().withIdentity(triggerKey(&quot;myTrigger&quot;, &quot;myTriggerGroup&quot;))
 *     .withSchedule(simpleSchedule().withIntervalInHours(1).repeatForever()).startAt(futureDate(10, MINUTES)).build();
 *
 * scheduler.scheduleJob(job, trigger);
 *
 * <pre>
 */
public class CalendarIntervalScheduleBuilder extends ScheduleBuilder<CalendarIntervalTrigger> {

  private int interval = 1;
  private IntervalUnit intervalUnit = IntervalUnit.DAY;

  private int misfireInstruction = CalendarIntervalTrigger.MISFIRE_INSTRUCTION_SMART_POLICY;

  private CalendarIntervalScheduleBuilder() {

  }

  /**
   * Create a CalendarIntervalScheduleBuilder.
   *
   * @return the new CalendarIntervalScheduleBuilder
   */
  public static CalendarIntervalScheduleBuilder calendarIntervalSchedule() {

    return new CalendarIntervalScheduleBuilder();
  }

  /**
   * Build the actual Trigger -- NOT intended to be invoked by end users, but will rather be invoked by a TriggerBuilder which this ScheduleBuilder is
   * given to.
   */
  @Override
  public MutableTrigger build() {

    CalendarIntervalTriggerImpl st = new CalendarIntervalTriggerImpl();
    st.setRepeatInterval(interval);
    st.setRepeatIntervalUnit(intervalUnit);
    st.setMisfireInstruction(misfireInstruction);

    return st;
  }

  /**
   * Specify the time unit and interval for the Trigger to be produced.
   *
   * @param interval the interval at which the trigger should repeat.
   * @param unit the time unit (IntervalUnit) of the interval.
   * @return the updated CalendarIntervalScheduleBuilder
   * @see CalendarIntervalTrigger#getRepeatInterval()
   * @see CalendarIntervalTrigger#getRepeatIntervalUnit()
   */
  public CalendarIntervalScheduleBuilder withInterval(int interval, CalendarIntervalTrigger.IntervalUnit unit) {

    if (unit == null) {
      throw new IllegalArgumentException("TimeUnit must be specified.");
    }
    validateInterval(interval);
    this.interval = interval;
    this.intervalUnit = unit;
    return this;
  }

  /**
   * If the Trigger misfires, use the {@link CalendarIntervalTrigger#MISFIRE_INSTRUCTION_DO_NOTHING} instruction.
   *
   * @return the updated CalendarIntervalScheduleBuilder
   * @see CalendarIntervalTrigger#MISFIRE_INSTRUCTION_DO_NOTHING
   */
  public CalendarIntervalScheduleBuilder withMisfireHandlingInstructionDoNothing() {

    misfireInstruction = CalendarIntervalTrigger.MISFIRE_INSTRUCTION_DO_NOTHING;
    return this;
  }

  /**
   * If the Trigger misfires, use the {@link CalendarIntervalTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW} instruction.
   *
   * @return the updated CalendarIntervalScheduleBuilder
   * @see CalendarIntervalTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW
   */
  public CalendarIntervalScheduleBuilder withMisfireHandlingInstructionFireAndProceed() {

    misfireInstruction = CalendarIntervalTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
    return this;
  }

  private void validateInterval(int interval) {

    if (interval <= 0) {
      throw new IllegalArgumentException("Interval must be a positive value.");
    }
  }
}
