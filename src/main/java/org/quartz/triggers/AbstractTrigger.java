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

import java.util.Arrays;
import java.util.Date;

import org.quartz.core.JobExecutionContext;
import org.quartz.core.Scheduler;
import org.quartz.exceptions.JobExecutionException;
import org.quartz.exceptions.SchedulerException;
import org.quartz.jobs.JobDataMap;

/**
 * <p>
 * The base abstract class to be extended by all <code>Trigger</code>s.
 * </p>
 * <p>
 * <code>Triggers</code> s have a name and group associated with them, which should uniquely identify them within a single
 * <code>{@link Scheduler}</code>.
 * </p>
 * <p>
 * <code>Trigger</code>s are the 'mechanism' by which <code>Job</code> s are scheduled. Many <code>Trigger</code> s can point to the same
 * <code>Job</code>, but a single <code>Trigger</code> can only point to one <code>Job</code>.
 * </p>
 * <p>
 * Triggers can 'send' parameters/data to <code>Job</code>s by placing contents into the <code>JobDataMap</code> on the <code>Trigger</code>.
 * </p>
 *
 * @author James House
 * @author Sharada Jambula
 */
public abstract class AbstractTrigger implements OperableTrigger {

  private static final long serialVersionUID = -3904243490805975570L;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ abstract methods.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  protected abstract boolean validateMisfireInstruction(int misfireInstruction);

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  protected Date startTime = null;
  protected Date endTime = null;
  protected Date nextFireTime = null;
  protected Date previousFireTime = null;

  private String name;

  private String jobName;

  private String description;

  private JobDataMap jobDataMap;

  private String calendarName = null;

  private String fireInstanceId = null;

  private int misfireInstruction = MISFIRE_INSTRUCTION_SMART_POLICY;

  private int priority = DEFAULT_PRIORITY;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Create a <code>Trigger</code> with no specified name, group, or <code>{@link org.quartz.jobs.JobDetail}</code>.
   * </p>
   * <p>
   * Note that the {@link #setName(String)}and the {@link #setJobName(String)} methods must be called before the <code>Trigger</code> can be placed
   * into a {@link Scheduler}.
   * </p>
   */
  public AbstractTrigger() {

    // do nothing...
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Trigger / MutableTrigger.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String getJobName() {
    return this.jobName;
  }

  @Override
  public void setName(String name) {

    if (name == null || name.trim().length() == 0) {
      throw new IllegalArgumentException("Trigger name cannot be null or empty.");
    }

    this.name = name;
  }

  @Override
  public void setJobName(String jobName) {

    if (jobName == null || jobName.trim().length() == 0) {
      throw new IllegalArgumentException("Job name cannot be null or empty.");
    }

    this.jobName = jobName;
  }

  @Override
  public String getDescription() {

    return description;
  }

  @Override
  public void setDescription(String description) {

    this.description = description;
  }

  @Override
  public void setCalendarName(String calendarName) {

    this.calendarName = calendarName;
  }

  @Override
  public String getCalendarName() {

    return calendarName;
  }

  @Override
  public JobDataMap getJobDataMap() {

    if (jobDataMap == null) {
      jobDataMap = new JobDataMap();
    }
    return jobDataMap;
  }

  @Override
  public void setJobDataMap(JobDataMap jobDataMap) {

    this.jobDataMap = jobDataMap;
  }

  @Override
  public int getPriority() {

    return priority;
  }

  @Override
  public void setPriority(int priority) {

    this.priority = priority;
  }

  @Override
  public int getMisfireInstruction() {

    return misfireInstruction;
  }

  @Override
  public void setMisfireInstruction(int misfireInstruction) {

    if (!validateMisfireInstruction(misfireInstruction)) {
      throw new IllegalArgumentException("The misfire instruction code is invalid for this type of trigger.");
    }
    this.misfireInstruction = misfireInstruction;
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Operational Trigger methods.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  @Override
  public CompletedExecutionInstruction executionComplete(JobExecutionContext context, JobExecutionException result) {

    if (result != null && result.refireImmediately()) {
      return CompletedExecutionInstruction.RE_EXECUTE_JOB;
    }

    if (result != null && result.unscheduleFiringTrigger()) {
      return CompletedExecutionInstruction.SET_TRIGGER_COMPLETE;
    }

    if (result != null && result.unscheduleAllTriggers()) {
      return CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_COMPLETE;
    }

    if (!mayFireAgain()) {
      return CompletedExecutionInstruction.DELETE_TRIGGER;
    }

    return CompletedExecutionInstruction.NOOP;
  }

  @Override
  public void validate() throws SchedulerException {

    if (name == null) {
      throw new SchedulerException("Trigger's name cannot be null");
    }

    if (jobName == null) {
      throw new SchedulerException("Trigger's related Job's name cannot be null");
    }
  }

  @Override
  public void setFireInstanceId(String id) {

    this.fireInstanceId = id;
  }

  @Override
  public String getFireInstanceId() {

    return fireInstanceId;
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ core Java method overrides.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Compare the next fire time of this <code>Trigger</code> to that of another by comparing their keys, or in other words, sorts them according to
   * the natural (i.e. alphabetical) order of their keys.
   * </p>
   */
  @Override
  public int compareTo(Trigger other) {

    if (other.getName() == null && getName() == null) {
      return 0;
    }
    if (other.getName() == null) {
      return -1;
    }
    if (getName() == null) {
      return 1;
    }

    return getName().compareTo(other.getName());
  }

  /**
   * Trigger equality is based upon the equality of the Trigger name.
   *
   * @return true if the key of this Trigger equals that of the given Trigger.
   */
  @Override
  public boolean equals(Object o) {

    if (!(o instanceof Trigger)) {
      return false;
    }

    Trigger other = (Trigger) o;

    if (other.getName() == null || getName() == null) {
      return false;
    }

    return getName().equals(other.getName());
  }

  @Override
  public int hashCode() {

    if (getName() == null) {
      return super.hashCode();
    }

    return getName().hashCode();
  }

  @Override
  public Object clone() {

    AbstractTrigger copy;
    try {
      copy = (AbstractTrigger) super.clone();

      // Shallow copy the jobDataMap. Note that this means that if a user
      // modifies a value object in this map from the cloned Trigger
      // they will also be modifying this Trigger.
      if (jobDataMap != null) {
        copy.jobDataMap = jobDataMap.shallowCopy();
      }

    } catch (CloneNotSupportedException ex) {
      throw new IncompatibleClassChangeError("Not Cloneable.");
    }
    return copy;
  }

  @Override
  public String toString() {

    return "Trigger '" + getName() + "',  triggerClass: " + getClass().getSimpleName() + ", jobName: " + getJobName() + ", jobDataMap: "
        + ((jobDataMap == null) ? "empty" : Arrays.toString(jobDataMap.entrySet().toArray())) + ", calendar: " + getCalendarName()
        + ", misfireInstruction: " + getMisfireInstruction() + ", priority: " + getPriority() + ", nextFireTime: " + getNextFireTime();
  }
}
