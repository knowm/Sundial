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
package org.quartz.builders;

import java.util.Date;
import java.util.UUID;

import org.quartz.core.Calendar;
import org.quartz.jobs.JobDataMap;
import org.quartz.triggers.MutableTrigger;
import org.quartz.triggers.Trigger;

/**
 * <code>TriggerBuilder</code> is used to instantiate {@link Trigger}s.
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
 * JobDetail job = newJob(MyJob.class).withIdentity(&quot;myJob&quot;).build();
 * 
 * Trigger trigger = newTrigger().withIdentity(triggerKey(&quot;myTrigger&quot;, &quot;myTriggerGroup&quot;))
 *     .withSchedule(simpleSchedule().withIntervalInHours(1).repeatForever()).startAt(futureDate(10, MINUTES)).build();
 *
 * scheduler.scheduleJob(job, trigger);
 *
 * <pre>
 *
 * @see JobBuilder
 * @see ScheduleBuilder
 * @see DateBuilder
 * @see Trigger
 */
public class TriggerBuilder<T extends Trigger> {

  private String key;
  private String description;
  private Date startTime = new Date();
  private Date endTime;
  private int priority = Trigger.DEFAULT_PRIORITY;
  private String calendarName;
  private String jobKey;
  private JobDataMap jobDataMap = new JobDataMap();

  private ScheduleBuilder scheduleBuilder = null;

  /**
   * Constructor
   */
  private TriggerBuilder() {

  }

  /**
   * Create a new TriggerBuilder with which to define a specification for a Trigger.
   *
   * @return the new TriggerBuilder
   */
  public static TriggerBuilder<Trigger> newTrigger() {

    return new TriggerBuilder<Trigger>();
  }

  /**
   * Produce the <code>Trigger</code>.
   *
   * @return a Trigger that meets the specifications of the builder.
   */
  public T build() {

    if (scheduleBuilder == null) {
      scheduleBuilder = SimpleScheduleBuilder.simpleSchedule();
    }
    MutableTrigger trig = scheduleBuilder.build();

    trig.setCalendarName(calendarName);
    trig.setDescription(description);
    trig.setEndTime(endTime);
    if (key == null) {
      key = UUID.randomUUID().toString();
    }
    trig.setName(key);
    if (jobKey != null) {
      trig.setJobName(jobKey);
    }
    trig.setPriority(priority);
    trig.setStartTime(startTime);

    if (!jobDataMap.isEmpty()) {
      trig.setJobDataMap(jobDataMap);
    }

    return (T) trig;
  }

  /**
   * Use the given TriggerKey to identify the Trigger.
   * <p>
   * If none of the 'withIdentity' methods are set on the TriggerBuilder, then a random, unique TriggerKey will be generated.
   * </p>
   *
   * @param key the TriggerKey for the Trigger to be built
   * @return the updated TriggerBuilder
   * @see TriggerKey
   * @see Trigger#getName()
   */
  public TriggerBuilder<T> withIdentity(String key) {

    this.key = key;
    return this;
  }

  /**
   * Set the given (human-meaningful) description of the Trigger.
   *
   * @param description the description for the Trigger
   * @return the updated TriggerBuilder
   * @see Trigger#getDescription()
   */
  public TriggerBuilder<T> withDescription(String description) {

    this.description = description;
    return this;
  }

  /**
   * Set the Trigger's priority. When more than one Trigger have the same fire time, the scheduler will fire the one with the highest priority first.
   *
   * @param priority the priority for the Trigger
   * @return the updated TriggerBuilder
   * @see Trigger#DEFAULT_PRIORITY
   * @see Trigger#getPriority()
   */
  public TriggerBuilder<T> withPriority(int priority) {

    this.priority = priority;
    return this;
  }

  /**
   * Set the name of the {@link Calendar} that should be applied to this Trigger's schedule.
   *
   * @param calendarName the name of the Calendar to reference.
   * @return the updated TriggerBuilder
   * @see Calendar
   * @see Trigger#getCalendarName()
   */
  public TriggerBuilder<T> modifiedByCalendar(String calendarName) {

    this.calendarName = calendarName;
    return this;
  }

  /**
   * Set the time the Trigger should start at to the current moment - the trigger may or may not fire at this time - depending upon the schedule
   * configured for the Trigger.
   *
   * @return the updated TriggerBuilder
   * @see Trigger#getStartTime()
   */
  public TriggerBuilder<T> startNow() {

    this.startTime = new Date();
    return this;
  }

  /**
   * Set the time the Trigger should start at - the trigger may or may not fire at this time - depending upon the schedule configured for the Trigger.
   * However the Trigger will NOT fire before this time, regardless of the Trigger's schedule.
   *
   * @param startTime the start time for the Trigger.
   * @return the updated TriggerBuilder
   * @see Trigger#getStartTime()
   * @see DateBuilder
   */
  public TriggerBuilder<T> startAt(Date startTime) {

    this.startTime = startTime;
    return this;
  }

  /**
   * Set the time at which the Trigger will no longer fire - even if it's schedule has remaining repeats.
   *
   * @param endTime the end time for the Trigger. If null, the end time is indefinite.
   * @return the updated TriggerBuilder
   * @see Trigger#getEndTime()
   * @see DateBuilder
   */
  public TriggerBuilder<T> endAt(Date endTime) {

    this.endTime = endTime;
    return this;
  }

  /**
   * Set the {@link ScheduleBuilder} that will be used to define the Trigger's schedule.
   * <p>
   * The particular <code>SchedulerBuilder</code> used will dictate the concrete type of Trigger that is produced by the TriggerBuilder.
   * </p>
   *
   * @param scheduleBuilder the SchedulerBuilder to use.
   * @return the updated TriggerBuilder
   * @see ScheduleBuilder
   * @see SimpleScheduleBuilder
   * @see CronScheduleBuilder
   * @see CalendarIntervalScheduleBuilder
   */
  public <SBT extends T> TriggerBuilder<SBT> withSchedule(ScheduleBuilder<SBT> scheduleBuilder) {

    this.scheduleBuilder = scheduleBuilder;
    return (TriggerBuilder<SBT>) this;
  }

  /**
   * Set the identity of the Job which should be fired by the produced Trigger.
   *
   * @param jobKey the identity of the Job to fire.
   * @return the updated TriggerBuilder
   * @see Trigger#getJobName()
   */
  public TriggerBuilder<T> forJob(String jobKey) {

    this.jobKey = jobKey;
    return this;
  }

  /**
   * Set the Trigger's {@link JobDataMap}.
   *
   * @return the updated TriggerBuilder
   * @see Trigger#getJobDataMap()
   */
  public TriggerBuilder<T> usingJobData(JobDataMap newJobDataMap) {

    this.jobDataMap = newJobDataMap; // set new map as the map to use
    return this;
  }

}
