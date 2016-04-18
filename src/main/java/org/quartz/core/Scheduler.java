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
import java.util.List;
import java.util.Set;

import org.quartz.classloading.CascadingClassLoadHelper;
import org.quartz.exceptions.SchedulerException;
import org.quartz.jobs.JobDataMap;
import org.quartz.jobs.JobDetail;
import org.quartz.jobs.JobFactory;
import org.quartz.listeners.JobListener;
import org.quartz.listeners.ListenerManager;
import org.quartz.listeners.SchedulerListener;
import org.quartz.listeners.TriggerListener;
import org.quartz.triggers.OperableTrigger;
import org.quartz.triggers.Trigger;

/**
 * This is the main interface of a Quartz Scheduler.
 * <p>
 * A <code>Scheduler</code> maintains a registry of <code>{@link org.quartz.jobs.JobDetail}</code>s and <code>{@link Trigger}</code>s. Once
 * registered, the <code>Scheduler</code> is responsible for executing <code>Job</code> s when their associated <code>Trigger</code> s fire (when
 * their scheduled time arrives).
 * </p>
 * <p>
 * <code>Scheduler</code> instances are produced by a <code>{@link SchedulerFactory}</code>. A scheduler that has already been created/initialized can
 * be found and used through the same factory that produced it. After a <code>Scheduler</code> has been created, it is in "stand-by" mode, and must
 * have its <code>start()</code> method called before it will fire any <code>Job</code>s.
 * </p>
 * <p>
 * <code>Job</code> s are to be created by the 'client program', by defining a class that implements the <code>{@link org.quartz.jobs.Job}</code>
 * interface. <code>{@link JobDetail}</code> objects are then created (also by the client) to define a individual instances of the <code>Job</code>.
 * <code>JobDetail</code> instances can then be registered with the <code>Scheduler</code> via the <code>scheduleJob(JobDetail, Trigger)</code> or
 * <code>addJob(JobDetail, boolean)</code> method.
 * </p>
 * <p>
 * <code>Trigger</code> s can then be defined to fire individual <code>Job</code> instances based on given schedules. <code>SimpleTrigger</code> s are
 * most useful for one-time firings, or firing at an exact moment in time, with N repeats with a given delay between them. <code>CronTrigger</code> s
 * allow scheduling based on time of day, day of week, day of month, and month of year.
 * </p>
 * <p>
 * <code>Job</code> s and <code>Trigger</code> s have a name and group associated with them, which should uniquely identify them within a single
 * <code>{@link Scheduler}</code>. The 'group' feature may be useful for creating logical groupings or categorizations of <code>Jobs</code> s and
 * <code>Triggers</code>s. If you don't have need for assigning a group to a given <code>Jobs</code> of <code>Triggers</code>, then you can use the
 * <code>DEFAULT_GROUP</code> constant defined on this interface.
 * </p>
 * <p>
 * Stored <code>Job</code> s can also be 'manually' triggered through the use of the <code>triggerJob(String jobName, String jobGroup)</code>
 * function.
 * </p>
 * <p>
 * Client programs may also be interested in the 'listener' interfaces that are available from Quartz. The <code>{@link JobListener}</code> interface
 * provides notifications of <code>Job</code> executions. The <code>{@link TriggerListener}</code> interface provides notifications of
 * <code>Trigger</code> firings. The <code>{@link SchedulerListener}</code> interface provides notifications of <code>Scheduler</code> events and
 * errors. Listeners can be associated with local schedulers through the {@link ListenerManager} interface.
 * </p>
 * <p>
 * The setup/configuration of a <code>Scheduler</code> instance is very customizable. Please consult the documentation distributed with Quartz.
 * </p>
 *
 * @author James House
 * @author Sharada Jambula
 */
public interface Scheduler {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  // /////////////////////////////////////////////////////////////////////////
  // /
  // / Scheduler State Management Methods
  // /
  // /////////////////////////////////////////////////////////////////////////

  /**
   * Starts the <code>Scheduler</code>'s threads that fire <code>{@link Trigger}s</code>. When a scheduler is first created it is in "stand-by" mode,
   * and will not fire triggers. The scheduler can also be put into stand-by mode by calling the <code>standby()</code> method.
   * <p>
   * The misfire/recovery process will be started, if it is the initial call to this method on this scheduler instance.
   * </p>
   *
   * @throws SchedulerException if <code>shutdown()</code> has been called, or there is an error within the <code>Scheduler</code>.
   */
  void start() throws SchedulerException;

  /**
   * Calls {#start()} after the indicated number of seconds. (This call does not block). This can be useful within applications that have initializers
   * that create the scheduler immediately, before the resources needed by the executing jobs have been fully initialized.
   *
   * @throws SchedulerException if <code>shutdown()</code> has been called, or there is an error within the <code>Scheduler</code>.
   */
  void startDelayed(int seconds) throws SchedulerException;

  /**
   * Whether the scheduler has been started.
   * <p>
   * Note: This only reflects whether <code>{@link #start()}</code> has ever been called on this Scheduler, so it will return <code>true</code> even
   * if the <code>Scheduler</code> is currently in standby mode or has been since shutdown.
   * </p>
   */
  public boolean isStarted() throws SchedulerException;

  /**
   * Temporarily halts the <code>Scheduler</code>'s firing of <code>{@link Trigger}s</code>.
   * <p>
   * When <code>start()</code> is called (to bring the scheduler out of stand-by mode), trigger misfire instructions will NOT be applied during the
   * execution of the <code>start()</code> method - any misfires will be detected immediately afterward (by the <code>JobStore</code>'s normal
   * process).
   * </p>
   * <p>
   * The scheduler is not destroyed, and can be re-started at any time.
   * </p>
   */
  void standby() throws SchedulerException;

  /**
   * Reports whether the <code>Scheduler</code> is in stand-by mode.
   */
  boolean isInStandbyMode() throws SchedulerException;

  /**
   * Halts the <code>Scheduler</code>'s firing of <code>{@link Trigger}s</code>, and cleans up all resources associated with the Scheduler.
   * <p>
   * The scheduler cannot be re-started.
   * </p>
   *
   * @param waitForJobsToComplete if <code>true</code> the scheduler will not allow this method to return until all currently executing jobs have
   *        completed.
   */
  void shutdown(boolean waitForJobsToComplete) throws SchedulerException;

  /**
   * Reports whether the <code>Scheduler</code> has been shutdown.
   */
  boolean isShutdown() throws SchedulerException;

  /**
   * Return a list of <code>JobExecutionContext</code> objects that represent all currently executing Jobs in this Scheduler instance.
   * <p>
   * This method is not cluster aware. That is, it will only return Jobs currently executing in this Scheduler instance, not across the entire
   * cluster.
   * </p>
   * <p>
   * Note that the list returned is an 'instantaneous' snap-shot, and that as soon as it's returned, the true list of executing jobs may be different.
   * Also please read the doc associated with <code>JobExecutionContext</code>- especially if you're using RMI.
   * </p>
   */
  List<JobExecutionContext> getCurrentlyExecutingJobs() throws SchedulerException;

  /**
   * Get the keys of all the <code>{@link org.quartz.jobs.JobDetail}s</code> in the matching groups.
   *
   * @param matcher Matcher to evaluate against known groups
   * @return Set of all keys matching
   * @throws SchedulerException On error
   */
  Set<String> getJobKeys() throws SchedulerException;

  /**
   * Set the <code>JobFactory</code> that will be responsible for producing
   * instances of <code>Job</code> classes.
   *
   * <p>
   * JobFactories may be of use to those wishing to have their application
   * produce <code>Job</code> instances via some special mechanism, such as to
   * give the opportunity for dependency injection.
   * </p>
   *
   * @see org.quartz.spi.JobFactory
   */
  void setJobFactory(JobFactory factory) throws SchedulerException;

  /**
   * Get a reference to the scheduler's <code>ListenerManager</code>, through which listeners may be registered.
   *
   * @return the scheduler's <code>ListenerManager</code>
   * @throws SchedulerException if the scheduler is not local
   */
  ListenerManager getListenerManager() throws SchedulerException;

  // /////////////////////////////////////////////////////////////////////////
  // /
  // / Scheduling-related Methods
  // /
  // /////////////////////////////////////////////////////////////////////////

  /**
   * Add the given <code>{@link org.quartz.jobs.JobDetail}</code> to the Scheduler, and associate the given <code>{@link OperableTrigger}</code> with
   * it.
   * <p>
   * If the given Trigger does not reference any <code>Job</code>, then it will be set to reference the Job passed with it into this method.
   * </p>
   *
   * @throws SchedulerException if the Job or Trigger cannot be added to the Scheduler, or there is an internal Scheduler error.
   */
  Date scheduleJob(JobDetail jobDetail, OperableTrigger trigger) throws SchedulerException;

  /**
   * Schedule the given <code>{@link org.quartz.triggers.OperableTrigger}</code> with the <code>Job</code> identified by the <code>Trigger</code>'s
   * settings.
   *
   * @throws SchedulerException if the indicated Job does not exist, or the Trigger cannot be added to the Scheduler, or there is an internal
   *         Scheduler error.
   */
  Date scheduleJob(OperableTrigger trigger) throws SchedulerException;

  /**
   * Remove (delete) the <code>{@link org.quartz.triggers.OperableTrigger}</code> with the given key, and store the new given one - which must be
   * associated with the same job (the new trigger must have the job name specified) - however, the new trigger need not have the same name as the old
   * trigger.
   *
   * @param triggerName identity of the trigger to replace
   * @param newTrigger The new <code>Trigger</code> to be stored.
   * @return <code>null</code> if a <code>Trigger</code> with the given name & group was not found and removed from the store, otherwise the first
   *         fire time of the newly scheduled trigger.
   */
  Date rescheduleJob(String triggerName, OperableTrigger newTrigger) throws SchedulerException;

  /**
   * Add the given <code>Job</code> to the Scheduler - with no associated <code>Trigger</code>. The <code>Job</code> will be 'dormant' until it is
   * scheduled with a <code>Trigger</code>, or <code>Scheduler.triggerJob()</code> is called for it.
   * <p>
   * The <code>Job</code> must by definition be 'durable', if it is not, SchedulerException will be thrown.
   * </p>
   *
   * @throws SchedulerException if there is an internal Scheduler error, or if the Job is not durable, or a Job with the same name already exists, and
   *         <code>replace</code> is <code>false</code>.
   */
  void addJob(JobDetail jobDetail) throws SchedulerException;

  /**
   * Trigger the identified <code>{@link org.quartz.jobs.JobDetail}</code> (execute it now).
   *
   * @param data the (possibly <code>null</code>) JobDataMap to be associated with the trigger that fires the job immediately.
   */
  void triggerJob(String jobKey, JobDataMap data) throws SchedulerException;

  /**
   * Get all <code>{@link Trigger}</code> s that are associated with the identified <code>{@link org.quartz.jobs.JobDetail}</code>.
   * <p>
   * The returned Trigger objects will be snap-shots of the actual stored triggers. If you wish to modify a trigger, you must re-store the trigger
   * afterward (e.g. see {@link #rescheduleJob(TriggerKey, Trigger)}).
   * </p>
   */
  List<Trigger> getTriggersOfJob(String jobKey) throws SchedulerException;

  /**
   * Get the <code>{@link JobDetail}</code> for the <code>Job</code> instance with the given key.
   * <p>
   * The returned JobDetail object will be a snap-shot of the actual stored JobDetail. If you wish to modify the JobDetail, you must re-store the
   * JobDetail afterward (e.g. see {@link #addJob(JobDetail, boolean)}).
   * </p>
   */
  JobDetail getJobDetail(String jobKey) throws SchedulerException;

  /**
   * Get the <code>{@link Trigger}</code> instance with the given key.
   * <p>
   * The returned Trigger object will be a snap-shot of the actual stored trigger. If you wish to modify the trigger, you must re-store the trigger
   * afterward (e.g. see {@link #rescheduleJob(TriggerKey, Trigger)}).
   * </p>
   */
  Trigger getTrigger(String triggerKey) throws SchedulerException;

  /**
   * Delete the identified <code>Job</code> from the Scheduler - and any associated <code>Trigger</code>s.
   *
   * @return true if the Job was found and deleted.
   * @throws SchedulerException if there is an internal Scheduler error.
   */
  void deleteJob(String jobKey) throws SchedulerException;

  /**
   * Remove the indicated <code>{@link Trigger}</code> from the scheduler.
   * <p>
   * If the related job does not have any other triggers, and the job is not durable, then the job will also be deleted.
   * </p>
   */
  void unscheduleJob(String triggerKey) throws SchedulerException;

  CascadingClassLoadHelper getCascadingClassLoadHelper();

}
