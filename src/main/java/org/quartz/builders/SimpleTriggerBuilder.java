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
package org.quartz.builders;

import java.util.Date;

import org.quartz.jobs.JobDataMap;
import org.quartz.triggers.OperableTrigger;
import org.quartz.triggers.SimpleTrigger;
import org.quartz.triggers.SimpleTriggerImpl;

/**
 * <code>SimpleScheduleBuilder</code> is a {@link ScheduleBuilder} that defines strict/literal interval-based schedules for <code>Trigger</code>s.
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
 * JobDetail job = newJob(MyJob.class).withIdentity(&quot;myJob&quot;).build(); Trigger trigger =
 * newTrigger().withIdentity(triggerKey(&quot;myTrigger&quot;, &quot;myTriggerGroup&quot;))
 * .withSchedule(simpleSchedule().withIntervalInHours(1).repeatForever()).startAt(futureDate(10, MINUTES)).build(); scheduler.scheduleJob(job,
 * trigger);
 *
 * <pre>
 */
public class SimpleTriggerBuilder extends TriggerBuilder {

  private long interval = 0;
  private int repeatCount = 0;
  private int misfireInstruction = SimpleTrigger.MISFIRE_INSTRUCTION_SMART_POLICY;

  private SimpleTriggerBuilder() {

  }

  /**
   * Create a SimpleScheduleBuilder.
   *
   * @return the new SimpleScheduleBuilder
   */
  public static SimpleTriggerBuilder simpleTriggerBuilder() {

    return new SimpleTriggerBuilder();
  }

  /**
   * Build the actual Trigger -- NOT intended to be invoked by end users, but will rather be invoked by a TriggerBuilder which this ScheduleBuilder is
   * given to.
   */
  @Override
  public OperableTrigger instantiate() {

    SimpleTriggerImpl st = new SimpleTriggerImpl();
    st.setRepeatInterval(interval);
    st.setRepeatCount(repeatCount);
    st.setMisfireInstruction(misfireInstruction);
    return st;
  }

  /**
   * Specify a repeat interval in milliseconds.
   *
   * @param intervalInMillis the number of seconds at which the trigger should repeat.
   * @return the updated SimpleScheduleBuilder
   * @see SimpleTrigger#getRepeatInterval()
   * @see #withRepeatCount(int)
   */
  public SimpleTriggerBuilder withIntervalInMilliseconds(long intervalInMillis) {

    this.interval = intervalInMillis;
    return this;
  }

  /**
   * Specify a the number of time the trigger will repeat - total number of firings will be this number + 1.
   *
   * @param repeatCount the number of seconds at which the trigger should repeat.
   * @return the updated SimpleScheduleBuilder
   * @see SimpleTrigger#getRepeatCount()
   * @see #repeatForever()
   */
  public SimpleTriggerBuilder withRepeatCount(int repeatCount) {

    this.repeatCount = repeatCount;
    return this;
  }

  /**
   * Specify that the trigger will repeat indefinitely.
   *
   * @return the updated SimpleScheduleBuilder
   * @see SimpleTrigger#getRepeatCount()
   * @see SimpleTrigger#REPEAT_INDEFINITELY
   * @see #withIntervalInMilliseconds(long)
   */
  public SimpleTriggerBuilder repeatForever() {

    this.repeatCount = SimpleTrigger.REPEAT_INDEFINITELY;
    return this;
  }

  /**
   * If the Trigger misfires, use the {@link SimpleTrigger#MISFIRE_INSTRUCTION_FIRE_NOW} instruction.
   *
   * @return the updated SimpleScheduleBuilder
   * @see SimpleTrigger#MISFIRE_INSTRUCTION_FIRE_NOW
   */

  public SimpleTriggerBuilder withMisfireHandlingInstructionFireNow() {

    misfireInstruction = SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW;
    return this;
  }

  /**
   * If the Trigger misfires, use the {@link SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT} instruction.
   *
   * @return the updated SimpleScheduleBuilder
   * @see SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT
   */
  public SimpleTriggerBuilder withMisfireHandlingInstructionNextWithExistingCount() {

    misfireInstruction = SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT;
    return this;
  }

  /**
   * If the Trigger misfires, use the {@link SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT} instruction.
   *
   * @return the updated SimpleScheduleBuilder
   * @see SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT
   */
  public SimpleTriggerBuilder withMisfireHandlingInstructionNextWithRemainingCount() {

    misfireInstruction = SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT;
    return this;
  }

  /**
   * If the Trigger misfires, use the {@link SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT} instruction.
   *
   * @return the updated SimpleScheduleBuilder
   * @see SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT
   */
  public SimpleTriggerBuilder withMisfireHandlingInstructionNowWithExistingCount() {

    misfireInstruction = SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT;
    return this;
  }

  /**
   * If the Trigger misfires, use the {@link SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT} instruction.
   *
   * @return the updated SimpleScheduleBuilder
   * @see SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT
   */
  public SimpleTriggerBuilder withMisfireHandlingInstructionNowWithRemainingCount() {

    misfireInstruction = SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT;
    return this;
  }

  public SimpleTriggerBuilder withIdentity(String name) {
    return (SimpleTriggerBuilder)super.withIdentity(name);
  }

  public SimpleTriggerBuilder withDescription(String description) {
    return (SimpleTriggerBuilder)super.withDescription(description);
  }

  public SimpleTriggerBuilder withPriority(int priority) {
    return (SimpleTriggerBuilder)super.withPriority(priority);
  }

  public SimpleTriggerBuilder modifiedByCalendar(String calendarName) {
    return (SimpleTriggerBuilder)super.modifiedByCalendar(calendarName);
  }

  public SimpleTriggerBuilder startNow() {
    return (SimpleTriggerBuilder)super.startNow();
  }

  public SimpleTriggerBuilder startAt(Date startTime) {
    return (SimpleTriggerBuilder)super.startAt(startTime);
  }

  public SimpleTriggerBuilder endAt(Date endTime) {
    return (SimpleTriggerBuilder)super.endAt(endTime);
  }

  public SimpleTriggerBuilder forJob(String jobName) {
    return (SimpleTriggerBuilder)super.forJob(jobName);
  }

  public SimpleTriggerBuilder usingJobData(JobDataMap newJobDataMap) {
    return (SimpleTriggerBuilder)super.usingJobData(newJobDataMap);
  }
}
