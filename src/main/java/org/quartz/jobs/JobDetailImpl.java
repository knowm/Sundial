/*
 * Copyright 2001-2009 Terracotta, Inc.
 * Copyright 2011 Xeiam, LLC
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

package org.quartz.jobs;

import org.quartz.annotations.AnnotationUtils;
import org.quartz.annotations.DisallowConcurrentExecution;
import org.quartz.builders.JobBuilder;
import org.quartz.core.JobExecutionContext;
import org.quartz.core.Scheduler;
import org.quartz.triggers.Trigger;

/**
 * <p>
 * Conveys the detail properties of a given <code>Job</code> instance.
 * </p>
 * <p>
 * Quartz does not store an actual instance of a <code>Job</code> class, but instead allows you to define an instance of one, through the use of a
 * <code>JobDetail</code>.
 * </p>
 * <p>
 * <code>Job</code>s have a name and group associated with them, which should uniquely identify them within a single <code>{@link Scheduler}</code>.
 * </p>
 * <p>
 * <code>Trigger</code>s are the 'mechanism' by which <code>Job</code>s are scheduled. Many <code>Trigger</code>s can point to the same
 * <code>Job</code>, but a single <code>Trigger</code> can only point to one <code>Job</code>.
 * </p>
 *
 * @see Job
 * @see StatefulJob
 * @see JobDataMap
 * @see Trigger
 * @author James House
 * @author Sharada Jambula
 * @author timmolter
 */
public class JobDetailImpl implements Cloneable, java.io.Serializable, JobDetail {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private String name;

  private String description;

  private Class<? extends Job> jobClass;

  private JobDataMap jobDataMap;

  private boolean durability = true;

  private boolean shouldRecover = false;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Create a <code>JobDetail</code> with no specified name or group, and the default settings of all the other properties.
   * </p>
   * <p>
   * Note that the {@link #setName(String)},{@link #setGroup(String)}and {@link #setJobClass(Class)}methods must be called before the job can be
   * placed into a {@link Scheduler}
   * </p>
   */
  public JobDetailImpl() {

    // do nothing...
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Get the name of this <code>Job</code>.
   * </p>
   */
  @Override
  public String getName() {

    return name;
  }

  /**
   * <p>
   * Set the name of this <code>Job</code>.
   * </p>
   *
   * @exception IllegalArgumentException if name is null or empty.
   */
  public void setName(String name) {

    if (name == null || name.trim().length() == 0) {
      throw new IllegalArgumentException("Job name cannot be empty.");
    }

    this.name = name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  /**
   * <p>
   * Set a description for the <code>Job</code> instance - may be useful for remembering/displaying the purpose of the job, though the description has
   * no meaning to Quartz.
   * </p>
   */
  public void setDescription(String description) {

    this.description = description;
  }

  @Override
  public Class<? extends Job> getJobClass() {

    return jobClass;
  }

  /**
   * <p>
   * Set the instance of <code>Job</code> that will be executed.
   * </p>
   *
   * @exception IllegalArgumentException if jobClass is null or the class is not a <code>Job</code>.
   */
  public void setJobClass(Class<? extends Job> jobClass) {

    if (jobClass == null) {
      throw new IllegalArgumentException("Job class cannot be null.");
    }

    if (!Job.class.isAssignableFrom(jobClass)) {
      throw new IllegalArgumentException("Job class must implement the Job interface.");
    }

    this.jobClass = jobClass;
  }

  @Override
  public JobDataMap getJobDataMap() {

    if (jobDataMap == null) {
      jobDataMap = new JobDataMap();
    }
    return jobDataMap;
  }

  /**
   * <p>
   * Set the <code>JobDataMap</code> to be associated with the <code>Job</code>.
   * </p>
   */
  public void setJobDataMap(JobDataMap jobDataMap) {

    this.jobDataMap = jobDataMap;
  }

  /**
   * <p>
   * Set whether or not the <code>Job</code> should remain stored after it is orphaned (no <code>{@link Trigger}s</code> point to it).
   * </p>
   * <p>
   * If not explicitly set, the default value is <code>false</code>.
   * </p>
   */
  public void setDurability(boolean durability) {

    this.durability = durability;
  }

  /**
   * <p>
   * Set whether or not the the <code>Scheduler</code> should re-execute the <code>Job</code> if a 'recovery' or 'fail-over' situation is encountered.
   * </p>
   * <p>
   * If not explicitly set, the default value is <code>false</code>.
   * </p>
   *
   * @see JobExecutionContext#isRecovering()
   */
  public void setRequestsRecovery(boolean shouldRecover) {

    this.shouldRecover = shouldRecover;
  }

  @Override
  public boolean isDurable() {

    return durability;
  }

  /**
   * @return whether the associated Job class carries the {@link DisallowConcurrentExecution} annotation.
   */
  @Override
  public boolean isConcurrentExectionDisallowed() {

    return AnnotationUtils.isAnnotationPresent(jobClass, DisallowConcurrentExecution.class);
  }

  @Override
  public boolean requestsRecovery() {

    return shouldRecover;
  }

  /**
   * <p>
   * Return a simple string representation of this object.
   * </p>
   */
  @Override
  public String toString() {

    return "JobDetail '" + getName() + "':  jobClass: '" + ((getJobClass() == null) ? null : getJobClass().getName())
        + " concurrentExectionDisallowed: " + isConcurrentExectionDisallowed() + " isDurable: " + isDurable() + " requestsRecovers: "
        + requestsRecovery();
  }

  @Override
  public boolean equals(Object obj) {

    if (!(obj instanceof JobDetail)) {
      return false;
    }

    JobDetail other = (JobDetail) obj;

    if (other.getName() == null || getName() == null) {
      return false;
    }

    if (!other.getName().equals(getName())) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {

    return getName().hashCode();
  }

  @Override
  public Object clone() {

    JobDetailImpl copy;
    try {
      copy = (JobDetailImpl) super.clone();
      if (jobDataMap != null) {
        copy.jobDataMap = (JobDataMap) jobDataMap.clone();
      }
    } catch (CloneNotSupportedException ex) {
      throw new IncompatibleClassChangeError("Not Cloneable.");
    }

    return copy;
  }

  @Override
  public JobBuilder getJobBuilder() {

    JobBuilder b = JobBuilder.newJob().ofType(getJobClass()).requestRecovery(requestsRecovery()).storeDurably(isDurable())
        .usingJobData(getJobDataMap()).withDescription(getDescription()).withIdentity(getName());
    return b;
  }

}
