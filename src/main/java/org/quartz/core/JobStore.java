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

import java.util.List;
import java.util.Set;

import org.quartz.exceptions.JobPersistenceException;
import org.quartz.exceptions.ObjectAlreadyExistsException;
import org.quartz.exceptions.SchedulerConfigException;
import org.quartz.exceptions.SchedulerException;
import org.quartz.jobs.JobDetail;
import org.quartz.triggers.OperableTrigger;
import org.quartz.triggers.Trigger;
import org.quartz.triggers.Trigger.CompletedExecutionInstruction;

/**
 * <p>
 * The interface to be implemented by classes that want to provide a <code>{@link org.quartz.jobs.Job}</code> and
 * <code>{@link org.quartz.triggers.Trigger}</code> storage mechanism for the <code>{@link org.quartz.QuartzScheduler}</code>'s use.
 * </p>
 * <p>
 * Storage of <code>Job</code> s and <code>Trigger</code> s should be keyed on the combination of their name and group for uniqueness.
 * </p>
 *
 * @author James House
 * @author Eric Mueller
 */
public interface JobStore {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * Called by the QuartzScheduler before the <code>JobStore</code> is used, in order to give the it a chance to initialize.
   */
  void initialize(SchedulerSignaler signaler) throws SchedulerConfigException;

  /**
   * Called by the QuartzScheduler to inform the <code>JobStore</code> that the scheduler has started.
   */
  void schedulerStarted() throws SchedulerException;

  // ///////////////////////////////////////////////////////////////////////////
  //
  // Job & Trigger Storage methods
  //
  // ///////////////////////////////////////////////////////////////////////////

  /**
   * Store the given <code>{@link org.quartz.jobs.JobDetail}</code> and <code>{@link org.quartz.triggers.Trigger}</code>.
   *
   * @param newJob The <code>JobDetail</code> to be stored.
   * @param newTrigger The <code>Trigger</code> to be stored.
   * @throws ObjectAlreadyExistsException if a <code>Job</code> with the same name/group already exists.
   */
  void storeJobAndTrigger(JobDetail newJob, OperableTrigger newTrigger) throws ObjectAlreadyExistsException, JobPersistenceException;

  /**
   * Store the given <code>{@link org.quartz.jobs.JobDetail}</code>.
   *
   * @param newJob The <code>JobDetail</code> to be stored.
   * @param replaceExisting If <code>true</code>, any <code>Job</code> existing in the <code>JobStore</code> with the same name should be
   *        over-written.
   * @throws ObjectAlreadyExistsException if a <code>Job</code> with the same name/group already exists, and replaceExisting is set to false.
   */
  void storeJob(JobDetail newJob, boolean replaceExisting) throws ObjectAlreadyExistsException, JobPersistenceException;

  /**
   * Remove (delete) the <code>{@link org.quartz.jobs.Job}</code> with the given key, and any <code>{@link org.quartz.triggers.Trigger}</code> s that
   * reference it.
   * <p>
   * If removal of the <code>Job</code> results in an empty group, the group should be removed from the <code>JobStore</code>'s list of known group
   * names.
   * </p>
   *
   * @return <code>true</code> if a <code>Job</code> with the given name & group was found and removed from the store.
   */
  boolean removeJob(String jobKey) throws JobPersistenceException;

  /**
   * Retrieve the <code>{@link org.quartz.jobs.JobDetail}</code> for the given <code>{@link org.quartz.jobs.Job}</code>.
   *
   * @return The desired <code>Job</code>, or null if there is no match.
   */
  JobDetail retrieveJob(String jobKey) throws JobPersistenceException;

  /**
   * Store the given <code>{@link org.quartz.triggers.Trigger}</code>.
   *
   * @param newTrigger The <code>Trigger</code> to be stored.
   * @param replaceExisting If <code>true</code>, any <code>Trigger</code> existing in the <code>JobStore</code> with the same name & group should be
   *        over-written.
   * @throws ObjectAlreadyExistsException if a <code>Trigger</code> with the same name/group already exists, and replaceExisting is set to false.
   * @see #pauseTriggers(org.quartz.impl.matchers.GroupMatcher)
   */
  void storeTrigger(OperableTrigger newTrigger, boolean replaceExisting) throws ObjectAlreadyExistsException, JobPersistenceException;

  /**
   * Remove (delete) the <code>{@link org.quartz.triggers.Trigger}</code> with the given key.
   * <p>
   * If removal of the <code>Trigger</code> results in an empty group, the group should be removed from the <code>JobStore</code>'s list of known
   * group names.
   * </p>
   * <p>
   * If removal of the <code>Trigger</code> results in an 'orphaned' <code>Job</code> that is not 'durable', then the <code>Job</code> should be
   * deleted also.
   * </p>
   *
   * @return <code>true</code> if a <code>Trigger</code> with the given name was found and removed from the store.
   */
  boolean removeTrigger(String triggerKey) throws JobPersistenceException;

  /**
   * Remove (delete) the <code>{@link org.quartz.triggers.Trigger}</code> with the given key, and store the new given one - which must be associated
   * with the same job.
   *
   * @param newTrigger The new <code>Trigger</code> to be stored.
   * @return <code>true</code> if a <code>Trigger</code> with the given name was found and removed from the store.
   */
  boolean replaceTrigger(String triggerKey, OperableTrigger newTrigger) throws JobPersistenceException;

  /**
   * Retrieve the given <code>{@link org.quartz.triggers.Trigger}</code>.
   *
   * @return The desired <code>Trigger</code>, or null if there is no match.
   */
  OperableTrigger retrieveTrigger(String triggerKey) throws JobPersistenceException;

  /**
   * Retrieve the given <code>{@link org.quartz.triggers.Trigger}</code>.
   *
   * @param calName The name of the <code>Calendar</code> to be retrieved.
   * @return The desired <code>Calendar</code>, or null if there is no match.
   */
  Calendar retrieveCalendar(String calName) throws JobPersistenceException;

  // ///////////////////////////////////////////////////////////////////////////
  //
  // Informational methods
  //
  // ///////////////////////////////////////////////////////////////////////////

  /**
   * Get all of the Triggers that are associated to the given Job.
   * <p>
   * If there are no matches, a zero-length array should be returned.
   * </p>
   */
  List<Trigger> getTriggersForJob(String jobKey) throws JobPersistenceException;

  /**
   * Get the keys of all of the <code>{@link org.quartz.jobs.Job}</code> s
   * <p>
   * If there are no jobs in the given group name, the result should be an empty collection (not <code>null</code>).
   * </p>
   */
  Set<String> getJobKeys() throws JobPersistenceException;

  // ///////////////////////////////////////////////////////////////////////////
  //
  // Trigger State manipulation methods
  //
  // ///////////////////////////////////////////////////////////////////////////

  // ///////////////////////////////////////////////////////////////////////////
  //
  // Trigger-Firing methods
  //
  // ///////////////////////////////////////////////////////////////////////////

  /**
   * Get a handle to the next trigger to be fired, and mark it as 'reserved' by the calling scheduler.
   *
   * @param noLaterThan If > 0, the JobStore should only return a Trigger that will fire no later than the time represented in this value as
   *        milliseconds.
   * @see #releaseAcquiredTrigger(Trigger)
   */
  List<OperableTrigger> acquireNextTriggers(long noLaterThan, int maxCount, long timeWindow) throws JobPersistenceException;

  /**
   * Inform the <code>JobStore</code> that the scheduler no longer plans to fire the given <code>Trigger</code>, that it had previously acquired
   * (reserved).
   */
  void releaseAcquiredTrigger(OperableTrigger trigger) throws JobPersistenceException;

  /**
   * Inform the <code>JobStore</code> that the scheduler is now firing the given <code>Trigger</code> (executing its associated <code>Job</code>),
   * that it had previously acquired (reserved).
   *
   * @return may return null if all the triggers or their calendars no longer exist, or if the trigger was not successfully put into the 'executing'
   *         state. Preference is to return an empty list if none of the triggers could be fired.
   */
  List<TriggerFiredResult> triggersFired(List<OperableTrigger> triggers) throws JobPersistenceException;

  /**
   * Inform the <code>JobStore</code> that the scheduler has completed the firing of the given <code>Trigger</code> (and the execution of its
   * associated <code>Job</code> completed, threw an exception, or was vetoed), and that the <code>{@link org.quartz.jobs.JobDataMap}</code> in the
   * given <code>JobDetail</code> should be updated if the <code>Job</code> is stateful.
   */
  void triggeredJobComplete(OperableTrigger trigger, JobDetail jobDetail, CompletedExecutionInstruction triggerInstCode)
      throws JobPersistenceException;

  /**
   * Tells the JobStore the pool size used to execute jobs
   *
   * @param poolSize amount of threads allocated for job execution
   * @since 2.0
   */
  void setThreadPoolSize(int poolSize);
}
