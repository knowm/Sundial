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
import java.util.UUID;

import org.quartz.core.Calendar;
import org.quartz.jobs.JobDataMap;
import org.quartz.triggers.OperableTrigger;
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
 * JobDetail job = newJob(MyJob.class).withIdentity(&quot;myJob&quot;).build(); Trigger trigger =
 * newTrigger().withIdentity(triggerKey(&quot;myTrigger&quot;, &quot;myTriggerGroup&quot;))
 * .withSchedule(simpleSchedule().withIntervalInHours(1).repeatForever()).startAt(futureDate(10, MINUTES)).build(); scheduler.scheduleJob(job,
 * trigger);
 *
 * <pre>
 */
public abstract class TriggerBuilder {

  private String name;
  private String description;
  private Date startTime = new Date();
  private Date endTime;
  private int priority = Trigger.DEFAULT_PRIORITY;
  private String calendarName;
  private String jobName;
  private JobDataMap jobDataMap = new JobDataMap();

  private OperableTrigger operableTrigger = null;

  public abstract OperableTrigger instantiate();

  /**
   * Produce the <code>OperableTrigger</code>.
   *
   * @return a OperableTrigger that meets the specifications of the builder.
   */
  public OperableTrigger build() {

    //    if (scheduleBuilder == null) {
    //      scheduleBuilder = SimpleScheduleBuilder.simpleScheduleBuilder();
    //    }

    // get a trigger impl. but without the meta data filled in yet
    //    OperableTrigger operableTrigger = operableTrigger;

    operableTrigger = instantiate();

    // fill in metadata
    operableTrigger.setCalendarName(calendarName);
    operableTrigger.setDescription(description);
    operableTrigger.setEndTime(endTime);
    if (name == null) {
      name = UUID.randomUUID().toString();
    }
    operableTrigger.setName(name);
    if (jobName != null) {
      operableTrigger.setJobName(jobName);
    }
    operableTrigger.setPriority(priority);
    operableTrigger.setStartTime(startTime);

    if (!jobDataMap.isEmpty()) {
      operableTrigger.setJobDataMap(jobDataMap);
    }

    return operableTrigger;
  }

  /**
   * Use the given TriggerKey to identify the Trigger.
   * <p>
   * If none of the 'withIdentity' methods are set on the TriggerBuilder, then a random, unique TriggerKey will be generated.
   * </p>
   *
   * @param name the TriggerKey for the Trigger to be built
   * @return the updated TriggerBuilder
   */
  public TriggerBuilder withIdentity(String name) {

    this.name = name;
    return this;
  }

  /**
   * Set the given (human-meaningful) description of the Trigger.
   *
   * @param description the description for the Trigger
   * @return the updated TriggerBuilder
   */
  public TriggerBuilder withDescription(String description) {

    this.description = description;
    return this;
  }

  /**
   * Set the Trigger's priority. When more than one Trigger have the same fire time, the scheduler will fire the one with the highest priority first.
   *
   * @param priority the priority for the Trigger
   * @return the updated TriggerBuilder
   */
  public TriggerBuilder withPriority(int priority) {

    this.priority = priority;
    return this;
  }

  /**
   * Set the name of the {@link Calendar} that should be applied to this Trigger's schedule.
   *
   * @param calendarName the name of the Calendar to reference.
   * @return the updated TriggerBuilder
   */
  public TriggerBuilder modifiedByCalendar(String calendarName) {

    this.calendarName = calendarName;
    return this;
  }

  /**
   * Set the time the Trigger should start at to the current moment - the trigger may or may not fire at this time - depending upon the schedule
   * configured for the Trigger.
   *
   * @return the updated TriggerBuilder
   */
  public TriggerBuilder startNow() {

    this.startTime = new Date();
    return this;
  }

  /**
   * Set the time the Trigger should start at - the trigger may or may not fire at this time - depending upon the schedule configured for the Trigger.
   * However the Trigger will NOT fire before this time, regardless of the Trigger's schedule.
   *
   * @param startTime the start time for the Trigger.
   * @return the updated TriggerBuilder
   */
  public TriggerBuilder startAt(Date startTime) {

    this.startTime = startTime;
    return this;
  }

  /**
   * Set the time at which the Trigger will no longer fire - even if it's schedule has remaining repeats.
   *
   * @param endTime the end time for the Trigger. If null, the end time is indefinite.
   * @return the updated TriggerBuilder
   */
  public TriggerBuilder endAt(Date endTime) {

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
   */
  public TriggerBuilder withTriggerImplementation(OperableTrigger operableTrigger) {

    this.operableTrigger = operableTrigger;
    return this;
  }

  /**
   * Set the identity of the Job which should be fired by the produced Trigger.
   *
   * @param jobName the identity of the Job to fire.
   * @return the updated TriggerBuilder
   */
  public TriggerBuilder forJob(String jobName) {

    this.jobName = jobName;
    return this;
  }

  /**
   * Set the Trigger's {@link JobDataMap}.
   *
   * @return the updated TriggerBuilder
   */
  public TriggerBuilder usingJobData(JobDataMap newJobDataMap) {

    this.jobDataMap = newJobDataMap; // set new map as the map to use
    return this;
  }

}
