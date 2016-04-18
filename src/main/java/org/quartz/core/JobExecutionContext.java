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
package org.quartz.core;

import java.util.Date;

import org.quartz.jobs.Job;
import org.quartz.jobs.JobDataMap;
import org.quartz.jobs.JobDetail;
import org.quartz.listeners.JobListener;
import org.quartz.listeners.TriggerListener;
import org.quartz.triggers.Trigger;

/**
 * A context bundle containing handles to various environment information, that is given to a <code>{@link org.quartz.jobs.JobDetail}</code> instance
 * as it is executed, and to a <code>{@link Trigger}</code> instance after the execution completes.
 * <p>
 * The <code>JobDataMap</code> found on this object (via the <code>getMergedJobDataMap()</code> method) serves as a convenience - it is a merge of the
 * <code>JobDataMap</code> found on the <code>JobDetail</code> and the one found on the <code>Trigger</code>, with the value in the latter overriding
 * any same-named values in the former. <i>It is thus considered a 'best practice' that the execute code of a Job retrieve data from the JobDataMap
 * found on this object</i> NOTE: Do not expect value 'set' into this JobDataMap to somehow be set back onto a <code>StatefulJob</code>'s own
 * JobDataMap.
 * </p>
 * <p>
 * <code>JobExecutionContext</code> s are also returned from the <code>Scheduler.getCurrentlyExecutingJobs()</code> method. These are the same
 * instances as those passed into the jobs that are currently executing within the scheduler. The exception to this is when your application is using
 * Quartz remotely (i.e. via RMI) - in which case you get a clone of the <code>JobExecutionContext</code>s, and their references to the
 * <code>Scheduler</code> and <code>Job</code> instances have been lost (a clone of the <code>JobDetail</code> is still available - just not a handle
 * to the job instance that is running).
 * </p>
 * 
 * @see #getScheduler()
 * @see #getMergedJobDataMap()
 * @see #getJobDetail()
 * @see Job
 * @see Trigger
 * @see JobDataMap
 * @author James House
 */
public interface JobExecutionContext {

  /**
   * <p>
   * Get a handle to the <code>Scheduler</code> instance that fired the <code>Job</code>.
   * </p>
   */
  public Scheduler getScheduler();

  /**
   * <p>
   * Get a handle to the <code>Trigger</code> instance that fired the <code>Job</code>.
   * </p>
   */
  public Trigger getTrigger();

  /**
   * <p>
   * Get a handle to the <code>Calendar</code> referenced by the <code>Trigger</code> instance that fired the <code>Job</code>.
   * </p>
   */
  public Calendar getCalendar();

  /**
   * <p>
   * If the <code>Job</code> is being re-executed because of a 'recovery' situation, this method will return <code>true</code>.
   * </p>
   */
  public boolean isRecovering();

  public int getRefireCount();

  /**
   * <p>
   * Get the convenience <code>JobDataMap</code> of this execution context.
   * </p>
   * <p>
   * The <code>JobDataMap</code> found on this object serves as a convenience - it is a merge of the <code>JobDataMap</code> found on the
   * <code>JobDetail</code> and the one found on the <code>Trigger</code>, with the value in the latter overriding any same-named values in the
   * former. <i>It is thus considered a 'best practice' that the execute code of a Job retrieve data from the JobDataMap found on this object.</i>
   * </p>
   * <p>
   * NOTE: Do not expect value 'set' into this JobDataMap to somehow be set back onto a <code>StatefulJob</code>'s own JobDataMap.
   * </p>
   * <p>
   * Attempts to change the contents of this map typically result in an <code>IllegalStateException</code>.
   * </p>
   */
  public JobDataMap getMergedJobDataMap();

  /**
   * <p>
   * Get the <code>JobDetail</code> associated with the <code>Job</code>.
   * </p>
   */
  public JobDetail getJobDetail();

  /**
   * <p>
   * Get the instance of the <code>Job</code> that was created for this execution.
   * </p>
   * <p>
   * Note: The Job instance is not available through remote scheduler interfaces.
   * </p>
   */
  public Job getJobInstance();

  /**
   * The actual time the trigger fired. For instance the scheduled time may have been 10:00:00 but the actual fire time may have been 10:00:03 if the
   * scheduler was too busy.
   * 
   * @return Returns the fireTime.
   * @see #getScheduledFireTime()
   */
  public Date getFireTime();

  /**
   * The scheduled time the trigger fired for. For instance the scheduled time may have been 10:00:00 but the actual fire time may have been 10:00:03
   * if the scheduler was too busy.
   * 
   * @return Returns the scheduledFireTime.
   * @see #getFireTime()
   */
  public Date getScheduledFireTime();

  public Date getPreviousFireTime();

  public Date getNextFireTime();

  /**
   * Returns the result (if any) that the <code>Job</code> set before its execution completed (the type of object set as the result is entirely up to
   * the particular job).
   * <p>
   * The result itself is meaningless to Quartz, but may be informative to <code>{@link JobListener}s</code> or <code>{@link TriggerListener}s</code>
   * that are watching the job's execution.
   * </p>
   * 
   * @return Returns the result.
   */
  public Object getResult();

  /**
   * Set the result (if any) of the <code>Job</code>'s execution (the type of object set as the result is entirely up to the particular job).
   * <p>
   * The result itself is meaningless to Quartz, but may be informative to <code>{@link JobListener}s</code> or <code>{@link TriggerListener}s</code>
   * that are watching the job's execution.
   * </p>
   */
  public void setResult(Object result);

  /**
   * The amount of time the job ran for (in milliseconds). The returned value will be -1 until the job has actually completed (or thrown an
   * exception), and is therefore generally only useful to <code>JobListener</code>s and <code>TriggerListener</code>s.
   * 
   * @return Returns the jobRunTime.
   */
  public long getJobRunTime();

}