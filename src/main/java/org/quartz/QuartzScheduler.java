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
package org.quartz;

import static org.quartz.builders.SimpleTriggerBuilder.simpleTriggerBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.quartz.classloading.CascadingClassLoadHelper;
import org.quartz.core.Calendar;
import org.quartz.core.JobExecutionContext;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.core.QuartzSchedulerThread;
import org.quartz.core.Scheduler;
import org.quartz.core.SchedulerSignaler;
import org.quartz.core.SchedulerSignalerImpl;
import org.quartz.exceptions.JobExecutionException;
import org.quartz.exceptions.JobPersistenceException;
import org.quartz.exceptions.ObjectAlreadyExistsException;
import org.quartz.exceptions.SchedulerException;
import org.quartz.jobs.InterruptableJob;
import org.quartz.jobs.Job;
import org.quartz.jobs.JobDataMap;
import org.quartz.jobs.JobDetail;
import org.quartz.jobs.JobFactory;
import org.quartz.jobs.SimpleJobFactory;
import org.quartz.listeners.JobListener;
import org.quartz.listeners.ListenerManager;
import org.quartz.listeners.ListenerManagerImpl;
import org.quartz.listeners.SchedulerListener;
import org.quartz.listeners.SchedulerListenerSupport;
import org.quartz.listeners.TriggerListener;
import org.quartz.plugins.SchedulerPlugin;
import org.quartz.triggers.OperableTrigger;
import org.quartz.triggers.Trigger;
import org.quartz.triggers.Trigger.CompletedExecutionInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This is the heart of Quartz, an indirect implementation of the <code>{@link org.quartz.core.Scheduler}</code> interface, containing methods to
 * schedule <code>{@link org.quartz.jobs.Job}</code>s, register <code>{@link org.quartz.listeners.JobListener}</code> instances, etc.
 * </p>
 *
 * @author James House
 */
public class QuartzScheduler implements Scheduler {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private final QuartzSchedulerResources quartzSchedulerResources;

  private final QuartzSchedulerThread quartzSchedulerThread;

  private ThreadGroup threadGroup;

  private final ListenerManager listenerManager = new ListenerManagerImpl();

  private final Map<String, JobListener> internalJobListeners = new HashMap<String, JobListener>(10);

  private final Map<String, TriggerListener> internalTriggerListeners = new HashMap<String, TriggerListener>(10);

  private final List<SchedulerListener> internalSchedulerListeners = new ArrayList<SchedulerListener>(10);

  private JobFactory jobFactory = new SimpleJobFactory();

  private ExecutingJobsManager jobMgr = null;

  private ErrorLoggingScheduleListener errLogger = null;

  private final SchedulerSignaler signaler;

  private final Random random = new Random();

  private boolean signalOnSchedulingChange = true;

  private volatile boolean closed = false;

  private volatile boolean shuttingDown = false;

  private Date initialStart = null;

  CascadingClassLoadHelper cascadingClassLoadHelper = new CascadingClassLoadHelper();

  private final Logger logger = LoggerFactory.getLogger(QuartzScheduler.class);

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Create a <code>QuartzScheduler</code> with the given configuration properties.
   * </p>
   *
   * @see QuartzSchedulerResources
   */
  public QuartzScheduler(QuartzSchedulerResources quartzSchedulerResources) throws SchedulerException {

    this.quartzSchedulerResources = quartzSchedulerResources;
    if (quartzSchedulerResources.getJobStore() instanceof JobListener) {
      addInternalJobListener((JobListener) quartzSchedulerResources.getJobStore());
    }

    this.quartzSchedulerThread = new QuartzSchedulerThread(this, quartzSchedulerResources);

    jobMgr = new ExecutingJobsManager();
    addInternalJobListener(jobMgr);
    errLogger = new ErrorLoggingScheduleListener();
    addInternalSchedulerListener(errLogger);

    signaler = new SchedulerSignalerImpl(this, this.quartzSchedulerThread);

    cascadingClassLoadHelper.initialize();
  }

  public void initialize() throws SchedulerException {

    this.quartzSchedulerThread.start();
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  public SchedulerSignaler getSchedulerSignaler() {

    return signaler;
  }

  /**
   * <p>
   * Returns the name of the thread group for Quartz's main threads.
   * </p>
   */
  public ThreadGroup getSchedulerThreadGroup() {

    if (threadGroup == null) {
      threadGroup = new ThreadGroup("QuartzScheduler");
      if (quartzSchedulerResources.getMakeSchedulerThreadDaemon()) {
        threadGroup.setDaemon(true);
      }
    }

    return threadGroup;
  }

  public boolean isSignalOnSchedulingChange() {

    return signalOnSchedulingChange;
  }

  public void setSignalOnSchedulingChange(boolean signalOnSchedulingChange) {

    this.signalOnSchedulingChange = signalOnSchedulingChange;
  }

  // /////////////////////////////////////////////////////////////////////////
  // /
  // / Scheduler State Management Methods
  // /
  // /////////////////////////////////////////////////////////////////////////

  /**
   * <p>
   * Starts the <code>QuartzScheduler</code>'s threads that fire <code>{@link org.quartz.triggers.Trigger}s</code>.
   * </p>
   * <p>
   * All <code>{@link org.quartz.triggers.Trigger}s</code> that have misfired will be passed to the appropriate TriggerListener(s).
   * </p>
   */
  @Override
  public void start() throws SchedulerException {

    if (shuttingDown || closed) {
      throw new SchedulerException("The Scheduler cannot be restarted after shutdown() has been called.");
    }

    if (initialStart == null) {
      initialStart = new Date();
      quartzSchedulerResources.getJobStore().schedulerStarted();
      startPlugins();
    }

    this.quartzSchedulerThread.togglePause(false);

    logger.info("Scheduler started.");

    notifySchedulerListenersStarted();
  }

  @Override
  public void startDelayed(final int seconds) throws SchedulerException {

    Thread t = new Thread(new Runnable() {

      @Override
      public void run() {

        try {
          Thread.sleep(seconds * 1000L);
        } catch (InterruptedException ignore) {
        }
        try {
          start();
        } catch (SchedulerException se) {
          logger.error("Unable to start secheduler after startup delay.", se);
        }
      }
    });
    t.start();
  }

  /**
   * <p>
   * Temporarily halts the <code>QuartzScheduler</code>'s firing of <code>{@link org.quartz.triggers.Trigger}s</code>.
   * </p>
   * <p>
   * The scheduler is not destroyed, and can be re-started at any time.
   * </p>
   */
  @Override
  public void standby() {

    this.quartzSchedulerThread.togglePause(true);
    logger.info("Scheduler paused.");
    notifySchedulerListenersInStandbyMode();
  }

  /**
   * <p>
   * Reports whether the <code>Scheduler</code> is paused.
   * </p>
   */
  @Override
  public boolean isInStandbyMode() {

    return this.quartzSchedulerThread.isPaused();
  }

  public Class getJobStoreClass() {

    return quartzSchedulerResources.getJobStore().getClass();
  }

  public Class getThreadPoolClass() {

    return quartzSchedulerResources.getThreadPool().getClass();
  }

  public int getThreadPoolSize() {

    return quartzSchedulerResources.getThreadPool().getPoolSize();
  }

  /**
   * <p>
   * Halts the <code>QuartzScheduler</code>'s firing of <code>{@link org.quartz.triggers.Trigger}s</code>, and cleans up all resources associated with
   * the QuartzScheduler.
   * </p>
   * <p>
   * The scheduler cannot be re-started.
   * </p>
   *
   * @param waitForJobsToComplete if <code>true</code> the scheduler will not allow this method to return until all currently executing jobs have
   *        completed.
   */
  @Override
  public void shutdown(boolean waitForJobsToComplete) {

    if (shuttingDown || closed) {
      return;
    }

    shuttingDown = true;

    logger.info("Scheduler shutting down...");

    standby();

    this.quartzSchedulerThread.halt();

    notifySchedulerListenersShuttingdown();

    if ((quartzSchedulerResources.isInterruptJobsOnShutdown() && !waitForJobsToComplete)
        || (quartzSchedulerResources.isInterruptJobsOnShutdownWithWait() && waitForJobsToComplete)) {
      List<JobExecutionContext> jobs = getCurrentlyExecutingJobs();
      for (JobExecutionContext job : jobs) {
        if (job.getJobInstance() instanceof InterruptableJob) {
          try {
            ((InterruptableJob) job.getJobInstance()).interrupt();
          } catch (Throwable e) {
            // do nothing, this was just a courtesy effort
            logger.warn("Encountered error when interrupting job {} during shutdown: {}", job.getJobDetail().getName(), e);
          }
        }
      }
    }

    quartzSchedulerResources.getThreadPool().shutdown(waitForJobsToComplete);

    if (waitForJobsToComplete) {
      while (jobMgr.getNumJobsCurrentlyExecuting() > 0) {
        try {
          Thread.sleep(100);
        } catch (Exception ignore) {
        }
      }
    }

    // Scheduler thread may have be waiting for the fire time of an acquired
    // trigger and need time to release the trigger once halted, so make sure
    // the thread is dead before continuing to shutdown the job store.
    try {
      this.quartzSchedulerThread.join();
    } catch (InterruptedException ignore) {
    }

    closed = true;

    shutdownPlugins();

    notifySchedulerListenersShutdown();

    logger.info("Scheduler shutdown complete.");
  }

  /**
   * <p>
   * Reports whether the <code>Scheduler</code> has been shutdown.
   * </p>
   */
  @Override
  public boolean isShutdown() {

    return closed;
  }

  public boolean isShuttingDown() {

    return shuttingDown;
  }

  @Override
  public boolean isStarted() {

    return !shuttingDown && !closed && !isInStandbyMode() && initialStart != null;
  }

  private void validateState() throws SchedulerException {

    if (isShutdown()) {
      throw new SchedulerException("The Scheduler has been shutdown.");
    }

    // other conditions to check (?)
  }

  /**
   * <p>
   * Return a list of <code>JobExecutionContext</code> objects that represent all currently executing Jobs in this Scheduler instance.
   * </p>
   * <p>
   * This method is not cluster aware. That is, it will only return Jobs currently executing in this Scheduler instance, not across the entire
   * cluster.
   * </p>
   * <p>
   * Note that the list returned is an 'instantaneous' snap-shot, and that as soon as it's returned, the true list of executing jobs may be different.
   * </p>
   */
  @Override
  public List<JobExecutionContext> getCurrentlyExecutingJobs() {

    return jobMgr.getExecutingJobs();
  }

  // /////////////////////////////////////////////////////////////////////////
  // /
  // / Scheduling-related Methods
  // /
  // /////////////////////////////////////////////////////////////////////////

  /**
   * <p>
   * Add the <code>{@link org.quartz.jobs.Job}</code> identified by the given <code>{@link org.quartz.jobs.JobDetail}</code> to the Scheduler, and
   * associate the given <code>{@link org.quartz.triggers.Trigger}</code> with it.
   * </p>
   * <p>
   * If the given Trigger does not reference any <code>Job</code>, then it will be set to reference the Job passed with it into this method.
   * </p>
   *
   * @throws SchedulerException if the Job or Trigger cannot be added to the Scheduler, or there is an internal Scheduler error.
   */
  @Override
  public Date scheduleJob(JobDetail jobDetail, OperableTrigger trigger) throws SchedulerException {

    validateState();

    if (jobDetail == null) {
      throw new SchedulerException("JobDetail cannot be null");
    }

    if (trigger == null) {
      throw new SchedulerException("Trigger cannot be null");
    }

    if (jobDetail.getName() == null) {
      throw new SchedulerException("Job's key cannot be null");
    }

    if (jobDetail.getJobClass() == null) {
      throw new SchedulerException("Job's class cannot be null");
    }

    OperableTrigger trig = trigger;

    if (trigger.getJobName() == null) {
      trig.setJobName(jobDetail.getName());
    } else if (!trigger.getJobName().equals(jobDetail.getName())) {
      throw new SchedulerException("Trigger does not reference given job!");
    }

    trig.validate();

    Calendar cal = null;
    if (trigger.getCalendarName() != null) {
      cal = quartzSchedulerResources.getJobStore().retrieveCalendar(trigger.getCalendarName());
      if (cal == null) {
        throw new SchedulerException("Calendar not found: " + trigger.getCalendarName());
      }
    }
    Date ft = trig.computeFirstFireTime(cal);

    if (ft == null) {
      throw new SchedulerException("Based on configured schedule, the given trigger will never fire.");
    }

    quartzSchedulerResources.getJobStore().storeJobAndTrigger(jobDetail, trig);
    notifySchedulerListenersJobAdded(jobDetail);
    notifySchedulerThread(trigger.getNextFireTime().getTime());
    notifySchedulerListenersScheduled(trigger);

    return ft;
  }

  /**
   * <p>
   * Schedule the given <code>{@link org.quartz.triggers.Trigger}</code> with the <code>Job</code> identified by the <code>Trigger</code>'s settings.
   * </p>
   *
   * @throws SchedulerException if the indicated Job does not exist, or the Trigger cannot be added to the Scheduler, or there is an internal
   *         Scheduler error.
   */

  @Override
  public Date scheduleJob(OperableTrigger trigger) throws SchedulerException {

    validateState();

    if (trigger == null) {
      throw new SchedulerException("Trigger cannot be null");
    }

    OperableTrigger trig = trigger;

    trig.validate();

    Calendar cal = null;
    if (trigger.getCalendarName() != null) {
      cal = quartzSchedulerResources.getJobStore().retrieveCalendar(trigger.getCalendarName());
      if (cal == null) {
        throw new SchedulerException("Calendar not found: " + trigger.getCalendarName());
      }
    }
    Date ft = trig.computeFirstFireTime(cal);

    if (ft == null) {
      throw new SchedulerException("Based on configured schedule, the given trigger will never fire.");
    }

    quartzSchedulerResources.getJobStore().storeTrigger(trig, false);
    notifySchedulerThread(trigger.getNextFireTime().getTime());
    notifySchedulerListenersScheduled(trigger);

    return ft;
  }

  /**
   * <p>
   * Add the given <code>Job</code> to the Scheduler - with no associated <code>Trigger</code>. The <code>Job</code> will be 'dormant' until it is
   * scheduled with a <code>Trigger</code>, or <code>Scheduler.triggerJob()</code> is called for it.
   * </p>
   */
  @Override
  public void addJob(JobDetail jobDetail) throws SchedulerException {

    validateState();

    quartzSchedulerResources.getJobStore().storeJob(jobDetail, true);
    notifySchedulerThread(0L);
    notifySchedulerListenersJobAdded(jobDetail);
  }

  @Override
  public void deleteJob(String jobKey) throws SchedulerException {

    validateState();

    List<? extends Trigger> triggers = getTriggersOfJob(jobKey);
    for (Trigger trigger : triggers) {
      unscheduleJob(trigger.getName());
    }
    boolean result = quartzSchedulerResources.getJobStore().removeJob(jobKey);
    if (result) {
      notifySchedulerThread(0L);
      notifySchedulerListenersJobDeleted(jobKey);
    }
  }

  @Override
  public void unscheduleJob(String triggerKey) throws SchedulerException {

    validateState();

    if (quartzSchedulerResources.getJobStore().removeTrigger(triggerKey)) {
      notifySchedulerThread(0L);
      notifySchedulerListenersUnscheduled(triggerKey);
    }
  }

  @Override
  public Date rescheduleJob(String triggerName, OperableTrigger newTrigger) throws SchedulerException {

    validateState();

    if (triggerName == null) {
      throw new IllegalArgumentException("triggerKey cannot be null");
    }
    if (newTrigger == null) {
      throw new IllegalArgumentException("newTrigger cannot be null");
    }

    OperableTrigger trig = newTrigger;
    Trigger oldTrigger = getTrigger(triggerName);
    if (oldTrigger == null) {
      return null;
    } else {
      trig.setJobName(oldTrigger.getJobName());
    }
    trig.validate();

    Calendar cal = null;
    if (newTrigger.getCalendarName() != null) {
      cal = quartzSchedulerResources.getJobStore().retrieveCalendar(newTrigger.getCalendarName());
    }
    Date ft = trig.computeFirstFireTime(cal);

    if (ft == null) {
      throw new SchedulerException("Based on configured schedule, the given trigger will never fire.");
    }

    if (quartzSchedulerResources.getJobStore().replaceTrigger(triggerName, trig)) {
      notifySchedulerThread(newTrigger.getNextFireTime().getTime());
      notifySchedulerListenersUnscheduled(triggerName);
      notifySchedulerListenersScheduled(newTrigger);
    } else {
      return null;
    }

    return ft;
  }

  private String newTriggerId() {

    long r = random.nextLong();
    if (r < 0) {
      r = -r;
    }
    return "MT_" + Long.toString(r, 30 + (int) (System.currentTimeMillis() % 7));
  }

  /**
   * <p>
   * Trigger the identified <code>{@link org.quartz.jobs.Job}</code> (execute it now) - with a non-volatile trigger.
   * </p>
   */
  @Override
  public void triggerJob(String jobKey, JobDataMap data) throws SchedulerException {

    validateState();

    OperableTrigger operableTrigger = simpleTriggerBuilder().withIdentity(jobKey + "-trigger").forJob(jobKey).startAt(new Date()).build();

    //    OperableTrigger operableTrigger = TriggerBuilder.newTriggerBuilder().withIdentity(jobKey + "-trigger").forJob(jobKey)
    //        .withTriggerImplementation(SimpleScheduleBuilder.simpleScheduleBuilderBuilder().instantiate()).startAt(new Date()).build();

    // TODO what does this accomplish??? Seems to sets it's next fire time internally
    operableTrigger.computeFirstFireTime(null);

    if (data != null) {
      operableTrigger.setJobDataMap(data);
    }

    boolean collision = true;
    while (collision) {
      try {
        quartzSchedulerResources.getJobStore().storeTrigger(operableTrigger, false);
        collision = false;
      } catch (ObjectAlreadyExistsException oaee) {
        operableTrigger.setName(newTriggerId());
      }
    }

    notifySchedulerThread(operableTrigger.getNextFireTime().getTime());
    notifySchedulerListenersScheduled(operableTrigger);
  }

  /**
   * <p>
   * Get all <code>{@link Trigger}</code> s that are associated with the identified <code>{@link org.quartz.jobs.JobDetail}</code>.
   * </p>
   */

  @Override
  public List<Trigger> getTriggersOfJob(String jobKey) throws SchedulerException {

    validateState();

    return quartzSchedulerResources.getJobStore().getTriggersForJob(jobKey);
  }

  /**
   * <p>
   * Get the <code>{@link JobDetail}</code> for the <code>Job</code> instance with the given name and group.
   * </p>
   */
  @Override
  public JobDetail getJobDetail(String jobKey) throws SchedulerException {

    validateState();

    return quartzSchedulerResources.getJobStore().retrieveJob(jobKey);
  }

  /**
   * <p>
   * Get the <code>{@link Trigger}</code> instance with the given name and group.
   * </p>
   */

  @Override
  public Trigger getTrigger(String triggerKey) throws SchedulerException {

    validateState();

    return quartzSchedulerResources.getJobStore().retrieveTrigger(triggerKey);
  }

  /**
   * Clears (deletes!) all scheduling data - all {@link Job}s, {@link Trigger}s {@link Calendar}s.
   *
   * @throws SchedulerException
   */

  @Override
  public ListenerManager getListenerManager() {

    return listenerManager;
  }

  /**
   * <p>
   * Add the given <code>{@link org.quartz.listeners.JobListener}</code> to the <code>Scheduler</code>'s <i>internal</i> list.
   * </p>
   */
  private void addInternalJobListener(JobListener jobListener) {

    if (jobListener.getName() == null || jobListener.getName().length() == 0) {
      throw new IllegalArgumentException("JobListener name cannot be empty.");
    }

    synchronized (internalJobListeners) {
      internalJobListeners.put(jobListener.getName(), jobListener);
    }
  }

  /**
   * <p>
   * Get a List containing all of the <code>{@link org.quartz.listeners.JobListener}</code>s in the <code>Scheduler</code>'s <i>internal</i> list.
   * </p>
   */
  public List<JobListener> getInternalJobListeners() {

    synchronized (internalJobListeners) {
      return java.util.Collections.unmodifiableList(new LinkedList<JobListener>(internalJobListeners.values()));
    }
  }

  /**
   * <p>
   * Get a list containing all of the <code>{@link org.quartz.listeners.TriggerListener}</code>s in the <code>Scheduler</code>'s <i>internal</i> list.
   * </p>
   */
  public List<TriggerListener> getInternalTriggerListeners() {

    synchronized (internalTriggerListeners) {
      return java.util.Collections.unmodifiableList(new LinkedList<TriggerListener>(internalTriggerListeners.values()));
    }
  }

  /**
   * <p>
   * Register the given <code>{@link SchedulerListener}</code> with the <code>Scheduler</code>'s list of internal listeners.
   * </p>
   */
  public void addInternalSchedulerListener(SchedulerListener schedulerListener) {

    synchronized (internalSchedulerListeners) {
      internalSchedulerListeners.add(schedulerListener);
    }
  }

  /**
   * <p>
   * Remove the given <code>{@link SchedulerListener}</code> from the <code>Scheduler</code>'s list of internal listeners.
   * </p>
   *
   * @return true if the identified listener was found in the list, and removed.
   */
  public boolean removeInternalSchedulerListener(SchedulerListener schedulerListener) {

    synchronized (internalSchedulerListeners) {
      return internalSchedulerListeners.remove(schedulerListener);
    }
  }

  /**
   * <p>
   * Get a List containing all of the <i>internal</i> <code>{@link SchedulerListener}</code>s registered with the <code>Scheduler</code>.
   * </p>
   */
  public List<SchedulerListener> getInternalSchedulerListeners() {

    synchronized (internalSchedulerListeners) {
      return java.util.Collections.unmodifiableList(new ArrayList<SchedulerListener>(internalSchedulerListeners));
    }
  }

  public void notifyJobStoreJobComplete(OperableTrigger trigger, JobDetail detail, CompletedExecutionInstruction instCode)
      throws JobPersistenceException {

    quartzSchedulerResources.getJobStore().triggeredJobComplete(trigger, detail, instCode);
  }

  public void notifyJobStoreJobVetoed(OperableTrigger trigger, JobDetail detail, CompletedExecutionInstruction instCode)
      throws JobPersistenceException {

    quartzSchedulerResources.getJobStore().triggeredJobComplete(trigger, detail, instCode);
  }

  private void notifySchedulerThread(long candidateNewNextFireTime) {

    if (isSignalOnSchedulingChange()) {
      signaler.signalSchedulingChange(candidateNewNextFireTime);
    }
  }

  private List<TriggerListener> buildTriggerListenerList() throws SchedulerException {

    List<TriggerListener> allListeners = new LinkedList<TriggerListener>();
    allListeners.addAll(getListenerManager().getTriggerListeners());
    allListeners.addAll(getInternalTriggerListeners());

    return allListeners;
  }

  private List<JobListener> buildJobListenerList() throws SchedulerException {

    List<JobListener> allListeners = new LinkedList<JobListener>();
    allListeners.addAll(getListenerManager().getJobListeners());
    allListeners.addAll(getInternalJobListeners());

    return allListeners;
  }

  private List<SchedulerListener> buildSchedulerListenerList() {

    List<SchedulerListener> allListeners = new LinkedList<SchedulerListener>();
    allListeners.addAll(getListenerManager().getSchedulerListeners());
    allListeners.addAll(getInternalSchedulerListeners());

    return allListeners;
  }

  public boolean notifyTriggerListenersFired(JobExecutionContext jec) throws SchedulerException {

    boolean vetoedExecution = false;

    // build a list of all trigger listeners that are to be notified...
    List<TriggerListener> triggerListeners = buildTriggerListenerList();

    // notify all trigger listeners in the list
    for (TriggerListener tl : triggerListeners) {
      try {
        tl.triggerFired(jec.getTrigger(), jec);

        if (tl.vetoJobExecution(jec.getTrigger(), jec)) {
          vetoedExecution = true;
        }
      } catch (Exception e) {
        SchedulerException se = new SchedulerException("TriggerListener '" + tl.getName() + "' threw exception: " + e.getMessage(), e);
        throw se;
      }
    }

    return vetoedExecution;
  }

  public void notifyTriggerListenersMisfired(Trigger trigger) throws SchedulerException {

    // build a list of all trigger listeners that are to be notified...
    List<TriggerListener> triggerListeners = buildTriggerListenerList();

    // notify all trigger listeners in the list
    for (TriggerListener tl : triggerListeners) {
      try {
        tl.triggerMisfired(trigger);
      } catch (Exception e) {
        SchedulerException se = new SchedulerException("TriggerListener '" + tl.getName() + "' threw exception: " + e.getMessage(), e);
        throw se;
      }
    }
  }

  public void notifyTriggerListenersComplete(JobExecutionContext jec, CompletedExecutionInstruction instCode) throws SchedulerException {

    // build a list of all trigger listeners that are to be notified...
    List<TriggerListener> triggerListeners = buildTriggerListenerList();

    // notify all trigger listeners in the list
    for (TriggerListener tl : triggerListeners) {
      try {
        tl.triggerComplete(jec.getTrigger(), jec, instCode);
      } catch (Exception e) {
        SchedulerException se = new SchedulerException("TriggerListener '" + tl.getName() + "' threw exception: " + e.getMessage(), e);
        throw se;
      }
    }
  }

  public void notifyJobListenersToBeExecuted(JobExecutionContext jec) throws SchedulerException {

    // build a list of all job listeners that are to be notified...
    List<JobListener> jobListeners = buildJobListenerList();

    // notify all job listeners
    for (JobListener jl : jobListeners) {
      try {
        jl.jobToBeExecuted(jec);
      } catch (Exception e) {
        SchedulerException se = new SchedulerException("JobListener '" + jl.getName() + "' threw exception: " + e.getMessage(), e);
        throw se;
      }
    }
  }

  public void notifyJobListenersWasVetoed(JobExecutionContext jec) throws SchedulerException {

    // build a list of all job listeners that are to be notified...
    List<JobListener> jobListeners = buildJobListenerList();

    // notify all job listeners
    for (JobListener jl : jobListeners) {
      try {
        jl.jobExecutionVetoed(jec);
      } catch (Exception e) {
        SchedulerException se = new SchedulerException("JobListener '" + jl.getName() + "' threw exception: " + e.getMessage(), e);
        throw se;
      }
    }
  }

  public void notifyJobListenersWasExecuted(JobExecutionContext jec, JobExecutionException je) throws SchedulerException {

    // build a list of all job listeners that are to be notified...
    List<JobListener> jobListeners = buildJobListenerList();

    // notify all job listeners
    for (JobListener jl : jobListeners) {
      try {
        jl.jobWasExecuted(jec, je);
      } catch (Exception e) {
        SchedulerException se = new SchedulerException("JobListener '" + jl.getName() + "' threw exception: " + e.getMessage(), e);
        throw se;
      }
    }
  }

  public void notifySchedulerListenersError(String msg, SchedulerException se) {

    // build a list of all scheduler listeners that are to be notified...
    List<SchedulerListener> schedListeners = buildSchedulerListenerList();

    // notify all scheduler listeners
    for (SchedulerListener sl : schedListeners) {
      try {
        sl.schedulerError(msg, se);
      } catch (Exception e) {
        logger.error("Error while notifying SchedulerListener of error: ", e);
        logger.error("  Original error (for notification) was: " + msg, se);
      }
    }
  }

  private void notifySchedulerListenersScheduled(Trigger trigger) {

    // build a list of all scheduler listeners that are to be notified...
    List<SchedulerListener> schedListeners = buildSchedulerListenerList();

    // notify all scheduler listeners
    for (SchedulerListener sl : schedListeners) {
      try {
        sl.jobScheduled(trigger);
      } catch (Exception e) {
        logger.error("Error while notifying SchedulerListener of scheduled job." + "  Triger=" + trigger.getName(), e);
      }
    }
  }

  private void notifySchedulerListenersUnscheduled(String triggerKey) {

    // build a list of all scheduler listeners that are to be notified...
    List<SchedulerListener> schedListeners = buildSchedulerListenerList();

    // notify all scheduler listeners
    for (SchedulerListener sl : schedListeners) {
      try {
        if (triggerKey == null) {
          sl.schedulingDataCleared();
        } else {
          sl.jobUnscheduled(triggerKey);
        }
      } catch (Exception e) {
        logger.error("Error while notifying SchedulerListener of unscheduled job." + "  Triger=" + (triggerKey == null ? "ALL DATA" : triggerKey), e);
      }
    }
  }

  public void notifySchedulerListenersFinalized(Trigger trigger) {

    // build a list of all scheduler listeners that are to be notified...
    List<SchedulerListener> schedListeners = buildSchedulerListenerList();

    // notify all scheduler listeners
    for (SchedulerListener sl : schedListeners) {
      try {
        sl.triggerFinalized(trigger);
      } catch (Exception e) {
        logger.error("Error while notifying SchedulerListener of finalized trigger." + "  Triger=" + trigger.getName(), e);
      }
    }
  }

  private void notifySchedulerListenersInStandbyMode() {

    // build a list of all scheduler listeners that are to be notified...
    List<SchedulerListener> schedListeners = buildSchedulerListenerList();

    // notify all scheduler listeners
    for (SchedulerListener sl : schedListeners) {
      try {
        sl.schedulerInStandbyMode();
      } catch (Exception e) {
        logger.error("Error while notifying SchedulerListener of inStandByMode.", e);
      }
    }
  }

  private void notifySchedulerListenersStarted() {

    // build a list of all scheduler listeners that are to be notified...
    List<SchedulerListener> schedListeners = buildSchedulerListenerList();

    // notify all scheduler listeners
    for (SchedulerListener sl : schedListeners) {
      try {
        sl.schedulerStarted();
      } catch (Exception e) {
        logger.error("Error while notifying SchedulerListener of startup.", e);
      }
    }
  }

  private void notifySchedulerListenersShutdown() {

    // build a list of all scheduler listeners that are to be notified...
    List<SchedulerListener> schedListeners = buildSchedulerListenerList();

    // notify all scheduler listeners
    for (SchedulerListener sl : schedListeners) {
      try {
        sl.schedulerShutdown();
      } catch (Exception e) {
        logger.error("Error while notifying SchedulerListener of shutdown.", e);
      }
    }
  }

  private void notifySchedulerListenersShuttingdown() {

    // build a list of all scheduler listeners that are to be notified...
    List<SchedulerListener> schedListeners = buildSchedulerListenerList();

    // notify all scheduler listeners
    for (SchedulerListener sl : schedListeners) {
      try {
        sl.schedulerShuttingdown();
      } catch (Exception e) {
        logger.error("Error while notifying SchedulerListener of shutdown.", e);
      }
    }
  }

  private void notifySchedulerListenersJobAdded(JobDetail jobDetail) {

    // build a list of all scheduler listeners that are to be notified...
    List<SchedulerListener> schedListeners = buildSchedulerListenerList();

    // notify all scheduler listeners
    for (SchedulerListener sl : schedListeners) {
      try {
        sl.jobAdded(jobDetail);
      } catch (Exception e) {
        logger.error("Error while notifying SchedulerListener of Job Added.", e);
      }
    }
  }

  public void notifySchedulerListenersJobDeleted(String jobKey) {

    // build a list of all scheduler listeners that are to be notified...
    List<SchedulerListener> schedListeners = buildSchedulerListenerList();

    // notify all scheduler listeners
    for (SchedulerListener sl : schedListeners) {
      try {
        sl.jobDeleted(jobKey);
      } catch (Exception e) {
        logger.error("Error while notifying SchedulerListener of Job Deleted.", e);
      }
    }
  }

  @Override
  public void setJobFactory(JobFactory factory) throws SchedulerException {

    if(factory == null) {
      throw new IllegalArgumentException("JobFactory cannot be set to null!");
    }

    logger.info("JobFactory set to: " + factory);
    this.jobFactory = factory;
  }

  public JobFactory getJobFactory() {

    return jobFactory;
  }

  private void shutdownPlugins() {

    java.util.Iterator itr = quartzSchedulerResources.getSchedulerPlugins().iterator();
    while (itr.hasNext()) {
      SchedulerPlugin plugin = (SchedulerPlugin) itr.next();
      plugin.shutdown();
    }
  }

  private void startPlugins() {

    java.util.Iterator itr = quartzSchedulerResources.getSchedulerPlugins().iterator();
    while (itr.hasNext()) {
      SchedulerPlugin plugin = (SchedulerPlugin) itr.next();
      plugin.start();
    }
  }

  @Override
  public CascadingClassLoadHelper getCascadingClassLoadHelper() {
    return this.cascadingClassLoadHelper;
  }

  /**
   * <p>
   * Get the names of all the <code>{@link org.quartz.jobs.Job}s</code> in the matching groups.
   * </p>
   */

  @Override
  public Set<String> getJobKeys() throws SchedulerException {

    validateState();

    return quartzSchedulerResources.getJobStore().getJobKeys();
  }

}

// ///////////////////////////////////////////////////////////////////////////
//
// ErrorLogger - Scheduler Listener Class
//
// ///////////////////////////////////////////////////////////////////////////

class ErrorLoggingScheduleListener extends SchedulerListenerSupport {

  private final Logger logger = LoggerFactory.getLogger(ErrorLoggingScheduleListener.class);

  /**
   * Constructor
   */
  ErrorLoggingScheduleListener() {

  }

  @Override
  public void schedulerError(String msg, SchedulerException cause) {

    logger.error(msg, cause);
  }

}

// ///////////////////////////////////////////////////////////////////////////
//
// ExecutingJobsManager - Job Listener Class
//
// ///////////////////////////////////////////////////////////////////////////

class ExecutingJobsManager implements JobListener {

  private HashMap<String, JobExecutionContext> executingJobs = new HashMap<String, JobExecutionContext>();

  private AtomicInteger numJobsFired = new AtomicInteger(0);

  ExecutingJobsManager() {

  }

  @Override
  public String getName() {

    return getClass().getName();
  }

  public int getNumJobsCurrentlyExecuting() {

    synchronized (executingJobs) {
      return executingJobs.size();
    }
  }

  @Override
  public void jobToBeExecuted(JobExecutionContext context) {

    numJobsFired.incrementAndGet();

    synchronized (executingJobs) {
      executingJobs.put(((OperableTrigger) context.getTrigger()).getFireInstanceId(), context);
    }
  }

  @Override
  public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {

    synchronized (executingJobs) {
      executingJobs.remove(((OperableTrigger) context.getTrigger()).getFireInstanceId());
    }
  }

  public int getNumJobsFired() {

    return numJobsFired.get();
  }

  public List<JobExecutionContext> getExecutingJobs() {

    synchronized (executingJobs) {
      return java.util.Collections.unmodifiableList(new ArrayList(executingJobs.values()));
    }
  }

  @Override
  public void jobExecutionVetoed(JobExecutionContext context) {

  }

}
