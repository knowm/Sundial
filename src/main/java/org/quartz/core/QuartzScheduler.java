/** 
 * Copyright 2001-2009 Terracotta, Inc. 
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
package org.quartz.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.quartz.Calendar;
import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.ListenerManager;
import org.quartz.Matcher;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerListener;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.TriggerListener;
import org.quartz.exceptions.JobExecutionException;
import org.quartz.exceptions.JobPersistenceException;
import org.quartz.exceptions.ObjectAlreadyExistsException;
import org.quartz.exceptions.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.listeners.SchedulerListenerSupport;
import org.quartz.simpl.SimpleJobFactory;
import org.quartz.spi.JobFactory;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerPlugin;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.utils.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This is the heart of Quartz, an indirect implementation of the <code>{@link org.quartz.Scheduler}</code> interface, containing methods to schedule <code>{@link org.quartz.Job}</code>s, register
 * <code>{@link org.quartz.JobListener}</code> instances, etc.
 * </p>
 * 
 * @see org.quartz.Scheduler
 * @see org.quartz.core.QuartzSchedulerThread
 * @see org.quartz.spi.JobStore
 * @see org.quartz.spi.ThreadPool
 * @author James House
 */
public class QuartzScheduler implements Scheduler {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private final QuartzSchedulerResources mQuartzSchedulerResources;

  private final QuartzSchedulerThread mQuartzSchedulerThread;

  private ThreadGroup threadGroup;

  private final SchedulerContext context = new SchedulerContext();

  private final ListenerManager listenerManager = new ListenerManagerImpl();

  private final HashMap<String, JobListener> internalJobListeners = new HashMap<String, JobListener>(10);

  private final HashMap<String, TriggerListener> internalTriggerListeners = new HashMap<String, TriggerListener>(10);

  private final ArrayList<SchedulerListener> internalSchedulerListeners = new ArrayList<SchedulerListener>(10);

  private JobFactory jobFactory = new SimpleJobFactory();

  private ExecutingJobsManager jobMgr = null;

  private ErrorLogger errLogger = null;

  private final SchedulerSignaler signaler;

  private final Random random = new Random();

  private final ArrayList<Object> holdToPreventGC = new ArrayList<Object>(5);

  private boolean signalOnSchedulingChange = true;

  private volatile boolean closed = false;

  private volatile boolean shuttingDown = false;

  private Date initialStart = null;

  private final Logger log = LoggerFactory.getLogger(getClass());

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Create a <code>QuartzScheduler</code> with the given configuration properties.
   * </p>
   * 
   * @see QuartzSchedulerResources
   */
  public QuartzScheduler(QuartzSchedulerResources pQuartzSchedulerResources) throws SchedulerException {

    mQuartzSchedulerResources = pQuartzSchedulerResources;
    if (pQuartzSchedulerResources.getJobStore() instanceof JobListener) {
      addInternalJobListener((JobListener) pQuartzSchedulerResources.getJobStore());
    }

    mQuartzSchedulerThread = new QuartzSchedulerThread(this, pQuartzSchedulerResources);

    jobMgr = new ExecutingJobsManager();
    addInternalJobListener(jobMgr);
    errLogger = new ErrorLogger();
    addInternalSchedulerListener(errLogger);

    signaler = new SchedulerSignalerImpl(this, this.mQuartzSchedulerThread);

  }

  public void initialize() throws SchedulerException {

    mQuartzSchedulerThread.start();
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  public SchedulerSignaler getSchedulerSignaler() {

    return signaler;
  }

  public Logger getLog() {

    return log;
  }

  /**
   * <p>
   * Returns the name of the thread group for Quartz's main threads.
   * </p>
   */
  public ThreadGroup getSchedulerThreadGroup() {

    if (threadGroup == null) {
      threadGroup = new ThreadGroup("QuartzScheduler");
      if (mQuartzSchedulerResources.getMakeSchedulerThreadDaemon()) {
        threadGroup.setDaemon(true);
      }
    }

    return threadGroup;
  }

  /**
   * <p>
   * Returns the <code>SchedulerContext</code> of the <code>Scheduler</code>.
   * </p>
   */
  public SchedulerContext getSchedulerContext() throws SchedulerException {

    return context;
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
   * Starts the <code>QuartzScheduler</code>'s threads that fire <code>{@link org.quartz.Trigger}s</code>.
   * </p>
   * <p>
   * All <code>{@link org.quartz.Trigger}s</code> that have misfired will be passed to the appropriate TriggerListener(s).
   * </p>
   */
  @Override
  public void start() throws SchedulerException {

    if (shuttingDown || closed) {
      throw new SchedulerException("The Scheduler cannot be restarted after shutdown() has been called.");
    }

    if (initialStart == null) {
      initialStart = new Date();
      mQuartzSchedulerResources.getJobStore().schedulerStarted();
      startPlugins();
    }

    mQuartzSchedulerThread.togglePause(false);

    getLog().info("Scheduler started.");

    notifySchedulerListenersStarted();
  }

  @Override
  public void startDelayed(final int seconds) throws SchedulerException {

    if (shuttingDown || closed) {
      throw new SchedulerException("The Scheduler cannot be restarted after shutdown() has been called.");
    }

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
          getLog().error("Unable to start secheduler after startup delay.", se);
        }
      }
    });
    t.start();
  }

  /**
   * <p>
   * Temporarily halts the <code>QuartzScheduler</code>'s firing of <code>{@link org.quartz.Trigger}s</code>.
   * </p>
   * <p>
   * The scheduler is not destroyed, and can be re-started at any time.
   * </p>
   */
  @Override
  public void standby() {

    mQuartzSchedulerThread.togglePause(true);
    getLog().info("Scheduler paused.");
    notifySchedulerListenersInStandbyMode();
  }

  /**
   * <p>
   * Reports whether the <code>Scheduler</code> is paused.
   * </p>
   */
  @Override
  public boolean isInStandbyMode() {

    return mQuartzSchedulerThread.isPaused();
  }

  public Class getJobStoreClass() {

    return mQuartzSchedulerResources.getJobStore().getClass();
  }

  public Class getThreadPoolClass() {

    return mQuartzSchedulerResources.getThreadPool().getClass();
  }

  public int getThreadPoolSize() {

    return mQuartzSchedulerResources.getThreadPool().getPoolSize();
  }

  /**
   * <p>
   * Halts the <code>QuartzScheduler</code>'s firing of <code>{@link org.quartz.Trigger}s</code>, and cleans up all resources associated with the QuartzScheduler.
   * </p>
   * <p>
   * The scheduler cannot be re-started.
   * </p>
   * 
   * @param waitForJobsToComplete if <code>true</code> the scheduler will not allow this method to return until all currently executing jobs have completed.
   */
  @Override
  public void shutdown(boolean waitForJobsToComplete) {

    if (shuttingDown || closed) {
      return;
    }

    shuttingDown = true;

    getLog().info("Scheduler  shutting down.");

    standby();

    mQuartzSchedulerThread.halt();

    notifySchedulerListenersShuttingdown();

    if ((mQuartzSchedulerResources.isInterruptJobsOnShutdown() && !waitForJobsToComplete) || (mQuartzSchedulerResources.isInterruptJobsOnShutdownWithWait() && waitForJobsToComplete)) {
      List<JobExecutionContext> jobs = getCurrentlyExecutingJobs();
      for (JobExecutionContext job : jobs) {
        if (job.getJobInstance() instanceof InterruptableJob) {
          try {
            ((InterruptableJob) job.getJobInstance()).interrupt();
          } catch (Throwable e) {
            // do nothing, this was just a courtesy effort
            getLog().warn("Encountered error when interrupting job {} during shutdown: {}", job.getJobDetail().getKey(), e);
          }
        }
      }
    }

    mQuartzSchedulerResources.getThreadPool().shutdown(waitForJobsToComplete);

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
      mQuartzSchedulerThread.join();
    } catch (InterruptedException ignore) {
    }

    closed = true;

    shutdownPlugins();

    mQuartzSchedulerResources.getJobStore().shutdown();

    notifySchedulerListenersShutdown();

    holdToPreventGC.clear();

    getLog().info("Scheduler shutdown complete.");
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
   * This method is not cluster aware. That is, it will only return Jobs currently executing in this Scheduler instance, not across the entire cluster.
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
   * Add the <code>{@link org.quartz.Job}</code> identified by the given <code>{@link org.quartz.JobDetail}</code> to the Scheduler, and associate the given <code>{@link org.quartz.Trigger}</code>
   * with it.
   * </p>
   * <p>
   * If the given Trigger does not reference any <code>Job</code>, then it will be set to reference the Job passed with it into this method.
   * </p>
   * 
   * @throws SchedulerException if the Job or Trigger cannot be added to the Scheduler, or there is an internal Scheduler error.
   */
  @Override
  public Date scheduleJob(JobDetail jobDetail, Trigger trigger) throws SchedulerException {

    validateState();

    if (jobDetail == null) {
      throw new SchedulerException("JobDetail cannot be null");
    }

    if (trigger == null) {
      throw new SchedulerException("Trigger cannot be null");
    }

    if (jobDetail.getKey() == null) {
      throw new SchedulerException("Job's key cannot be null");
    }

    if (jobDetail.getJobClass() == null) {
      throw new SchedulerException("Job's class cannot be null");
    }

    OperableTrigger trig = (OperableTrigger) trigger;

    if (trigger.getJobKey() == null) {
      trig.setJobKey(jobDetail.getKey());
    }
    else if (!trigger.getJobKey().equals(jobDetail.getKey())) {
      throw new SchedulerException("Trigger does not reference given job!");
    }

    trig.validate();

    Calendar cal = null;
    if (trigger.getCalendarName() != null) {
      cal = mQuartzSchedulerResources.getJobStore().retrieveCalendar(trigger.getCalendarName());
    }
    Date ft = trig.computeFirstFireTime(cal);

    if (ft == null) {
      throw new SchedulerException("Based on configured schedule, the given trigger will never fire.");
    }

    mQuartzSchedulerResources.getJobStore().storeJobAndTrigger(jobDetail, trig);
    notifySchedulerListenersJobAdded(jobDetail);
    notifySchedulerThread(trigger.getNextFireTime().getTime());
    notifySchedulerListenersSchduled(trigger);

    return ft;
  }

  /**
   * <p>
   * Schedule the given <code>{@link org.quartz.Trigger}</code> with the <code>Job</code> identified by the <code>Trigger</code>'s settings.
   * </p>
   * 
   * @throws SchedulerException if the indicated Job does not exist, or the Trigger cannot be added to the Scheduler, or there is an internal Scheduler error.
   */

  @Override
  public Date scheduleJob(Trigger trigger) throws SchedulerException {

    validateState();

    if (trigger == null) {
      throw new SchedulerException("Trigger cannot be null");
    }

    OperableTrigger trig = (OperableTrigger) trigger;

    trig.validate();

    Calendar cal = null;
    if (trigger.getCalendarName() != null) {
      cal = mQuartzSchedulerResources.getJobStore().retrieveCalendar(trigger.getCalendarName());
      if (cal == null) {
        throw new SchedulerException("Calendar not found: " + trigger.getCalendarName());
      }
    }
    Date ft = trig.computeFirstFireTime(cal);

    if (ft == null) {
      throw new SchedulerException("Based on configured schedule, the given trigger will never fire.");
    }

    mQuartzSchedulerResources.getJobStore().storeTrigger(trig, false);
    notifySchedulerThread(trigger.getNextFireTime().getTime());
    notifySchedulerListenersSchduled(trigger);

    return ft;
  }

  /**
   * <p>
   * Add the given <code>Job</code> to the Scheduler - with no associated <code>Trigger</code>. The <code>Job</code> will be 'dormant' until it is scheduled with a <code>Trigger</code>, or
   * <code>Scheduler.triggerJob()</code> is called for it.
   * </p>
   * <p>
   * The <code>Job</code> must by definition be 'durable', if it is not, SchedulerException will be thrown.
   * </p>
   * 
   * @throws SchedulerException if there is an internal Scheduler error, or if the Job is not durable, or a Job with the same name already exists, and <code>replace</code> is <code>false</code>.
   */

  @Override
  public void addJob(JobDetail jobDetail, boolean replace) throws SchedulerException {

    validateState();

    if (!jobDetail.isDurable() && !replace) {
      throw new SchedulerException("Jobs added with no trigger must be durable.");
    }

    mQuartzSchedulerResources.getJobStore().storeJob(jobDetail, replace);
    notifySchedulerThread(0L);
    notifySchedulerListenersJobAdded(jobDetail);
  }

  /**
   * <p>
   * Remove (delete) the <code>{@link org.quartz.Trigger}</code> with the given name, and store the new given one - which must be associated with the same job.
   * </p>
   * 
   * @param newTrigger The new <code>Trigger</code> to be stored.
   * @return <code>null</code> if a <code>Trigger</code> with the given name & group was not found and removed from the store, otherwise the first fire time of the newly scheduled trigger.
   */

  @Override
  public Date rescheduleJob(TriggerKey triggerKey, Trigger newTrigger) throws SchedulerException {

    validateState();

    if (triggerKey == null) {
      throw new IllegalArgumentException("triggerKey cannot be null");
    }
    if (newTrigger == null) {
      throw new IllegalArgumentException("newTrigger cannot be null");
    }

    OperableTrigger trig = (OperableTrigger) newTrigger;
    Trigger oldTrigger = getTrigger(triggerKey);
    if (oldTrigger == null) {
      return null;
    }
    else {
      trig.setJobKey(oldTrigger.getJobKey());
    }
    trig.validate();

    Calendar cal = null;
    if (newTrigger.getCalendarName() != null) {
      cal = mQuartzSchedulerResources.getJobStore().retrieveCalendar(newTrigger.getCalendarName());
    }
    Date ft = trig.computeFirstFireTime(cal);

    if (ft == null) {
      throw new SchedulerException("Based on configured schedule, the given trigger will never fire.");
    }

    if (mQuartzSchedulerResources.getJobStore().replaceTrigger(triggerKey, trig)) {
      notifySchedulerThread(newTrigger.getNextFireTime().getTime());
      notifySchedulerListenersUnscheduled(triggerKey);
      notifySchedulerListenersSchduled(newTrigger);
    }
    else {
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
   * Trigger the identified <code>{@link org.quartz.Job}</code> (execute it now) - with a non-volatile trigger.
   * </p>
   */
  @Override
  public void triggerJob(JobKey jobKey, JobDataMap data) throws SchedulerException {

    validateState();

    OperableTrigger trig =
        (OperableTrigger) TriggerBuilder.newTrigger().withIdentity(jobKey.getName() + "-trigger", Key.DEFAULT_GROUP).forJob(jobKey).withSchedule(SimpleScheduleBuilder.simpleSchedule()).startAt(
            new Date()).build();
    // OperableTrigger trig = new org.quartz.impl.triggers.SimpleTriggerImpl(newTriggerId(), Key.DEFAULT_GROUP, jobKey.getName(), jobKey.getGroup(), new Date(), null, 0, 0);
    trig.computeFirstFireTime(null);
    if (data != null) {
      trig.setJobDataMap(data);
    }

    boolean collision = true;
    while (collision) {
      try {
        mQuartzSchedulerResources.getJobStore().storeTrigger(trig, false);
        collision = false;
      } catch (ObjectAlreadyExistsException oaee) {
        trig.setKey(new TriggerKey(newTriggerId(), Key.DEFAULT_GROUP));
      }
    }

    notifySchedulerThread(trig.getNextFireTime().getTime());
    notifySchedulerListenersSchduled(trig);
  }

  /**
   * <p>
   * Get the names of all the <code>{@link org.quartz.Job}s</code> in the matching groups.
   * </p>
   */

  @Override
  public Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher) throws SchedulerException {

    validateState();

    if (matcher == null) {
      matcher = GroupMatcher.groupEquals(Key.DEFAULT_GROUP);
    }

    return mQuartzSchedulerResources.getJobStore().getJobKeys(matcher);
  }

  /**
   * <p>
   * Get all <code>{@link Trigger}</code> s that are associated with the identified <code>{@link org.quartz.JobDetail}</code>.
   * </p>
   */

  @Override
  public List<? extends Trigger> getTriggersOfJob(JobKey jobKey) throws SchedulerException {

    validateState();

    return mQuartzSchedulerResources.getJobStore().getTriggersForJob(jobKey);
  }

  /**
   * <p>
   * Get the <code>{@link JobDetail}</code> for the <code>Job</code> instance with the given name and group.
   * </p>
   */
  @Override
  public JobDetail getJobDetail(JobKey jobKey) throws SchedulerException {

    validateState();

    return mQuartzSchedulerResources.getJobStore().retrieveJob(jobKey);
  }

  /**
   * <p>
   * Get the <code>{@link Trigger}</code> instance with the given name and group.
   * </p>
   */

  @Override
  public Trigger getTrigger(TriggerKey triggerKey) throws SchedulerException {

    validateState();

    return mQuartzSchedulerResources.getJobStore().retrieveTrigger(triggerKey);
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
   * Add the given <code>{@link org.quartz.JobListener}</code> to the <code>Scheduler</code>'s <i>internal</i> list.
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
   * Get a List containing all of the <code>{@link org.quartz.JobListener}</code>s in the <code>Scheduler</code>'s <i>internal</i> list.
   * </p>
   */
  public List<JobListener> getInternalJobListeners() {

    synchronized (internalJobListeners) {
      return java.util.Collections.unmodifiableList(new LinkedList<JobListener>(internalJobListeners.values()));
    }
  }

  /**
   * <p>
   * Get a list containing all of the <code>{@link org.quartz.TriggerListener}</code>s in the <code>Scheduler</code>'s <i>internal</i> list.
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
  void addInternalSchedulerListener(SchedulerListener schedulerListener) {

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
  boolean removeInternalSchedulerListener(SchedulerListener schedulerListener) {

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

  protected void notifyJobStoreJobComplete(OperableTrigger trigger, JobDetail detail, CompletedExecutionInstruction instCode) throws JobPersistenceException {

    mQuartzSchedulerResources.getJobStore().triggeredJobComplete(trigger, detail, instCode);
  }

  protected void notifyJobStoreJobVetoed(OperableTrigger trigger, JobDetail detail, CompletedExecutionInstruction instCode) throws JobPersistenceException {

    mQuartzSchedulerResources.getJobStore().triggeredJobComplete(trigger, detail, instCode);
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

  private boolean matchJobListener(JobListener listener, JobKey key) {

    List<Matcher<JobKey>> matchers = getListenerManager().getJobListenerMatchers(listener.getName());
    if (matchers == null) {
      return true;
    }
    for (Matcher<JobKey> matcher : matchers) {
      if (matcher.isMatch(key)) {
        return true;
      }
    }
    return false;
  }

  private boolean matchTriggerListener(TriggerListener listener, TriggerKey key) {

    List<Matcher<TriggerKey>> matchers = getListenerManager().getTriggerListenerMatchers(listener.getName());
    if (matchers == null) {
      return true;
    }
    for (Matcher<TriggerKey> matcher : matchers) {
      if (matcher.isMatch(key)) {
        return true;
      }
    }
    return false;
  }

  boolean notifyTriggerListenersFired(JobExecutionContext jec) throws SchedulerException {

    boolean vetoedExecution = false;

    // build a list of all trigger listeners that are to be notified...
    List<TriggerListener> triggerListeners = buildTriggerListenerList();

    // notify all trigger listeners in the list
    for (TriggerListener tl : triggerListeners) {
      try {
        if (!matchTriggerListener(tl, jec.getTrigger().getKey())) {
          continue;
        }
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

  void notifyTriggerListenersMisfired(Trigger trigger) throws SchedulerException {

    // build a list of all trigger listeners that are to be notified...
    List<TriggerListener> triggerListeners = buildTriggerListenerList();

    // notify all trigger listeners in the list
    for (TriggerListener tl : triggerListeners) {
      try {
        if (!matchTriggerListener(tl, trigger.getKey())) {
          continue;
        }
        tl.triggerMisfired(trigger);
      } catch (Exception e) {
        SchedulerException se = new SchedulerException("TriggerListener '" + tl.getName() + "' threw exception: " + e.getMessage(), e);
        throw se;
      }
    }
  }

  void notifyTriggerListenersComplete(JobExecutionContext jec, CompletedExecutionInstruction instCode) throws SchedulerException {

    // build a list of all trigger listeners that are to be notified...
    List<TriggerListener> triggerListeners = buildTriggerListenerList();

    // notify all trigger listeners in the list
    for (TriggerListener tl : triggerListeners) {
      try {
        if (!matchTriggerListener(tl, jec.getTrigger().getKey())) {
          continue;
        }
        tl.triggerComplete(jec.getTrigger(), jec, instCode);
      } catch (Exception e) {
        SchedulerException se = new SchedulerException("TriggerListener '" + tl.getName() + "' threw exception: " + e.getMessage(), e);
        throw se;
      }
    }
  }

  void notifyJobListenersToBeExecuted(JobExecutionContext jec) throws SchedulerException {

    // build a list of all job listeners that are to be notified...
    List<JobListener> jobListeners = buildJobListenerList();

    // notify all job listeners
    for (JobListener jl : jobListeners) {
      try {
        if (!matchJobListener(jl, jec.getJobDetail().getKey())) {
          continue;
        }
        jl.jobToBeExecuted(jec);
      } catch (Exception e) {
        SchedulerException se = new SchedulerException("JobListener '" + jl.getName() + "' threw exception: " + e.getMessage(), e);
        throw se;
      }
    }
  }

  void notifyJobListenersWasVetoed(JobExecutionContext jec) throws SchedulerException {

    // build a list of all job listeners that are to be notified...
    List<JobListener> jobListeners = buildJobListenerList();

    // notify all job listeners
    for (JobListener jl : jobListeners) {
      try {
        if (!matchJobListener(jl, jec.getJobDetail().getKey())) {
          continue;
        }
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
        if (!matchJobListener(jl, jec.getJobDetail().getKey())) {
          continue;
        }
        jl.jobWasExecuted(jec, je);
      } catch (Exception e) {
        SchedulerException se = new SchedulerException("JobListener '" + jl.getName() + "' threw exception: " + e.getMessage(), e);
        throw se;
      }
    }
  }

  void notifySchedulerListenersError(String msg, SchedulerException se) {

    // build a list of all scheduler listeners that are to be notified...
    List<SchedulerListener> schedListeners = buildSchedulerListenerList();

    // notify all scheduler listeners
    for (SchedulerListener sl : schedListeners) {
      try {
        sl.schedulerError(msg, se);
      } catch (Exception e) {
        getLog().error("Error while notifying SchedulerListener of error: ", e);
        getLog().error("  Original error (for notification) was: " + msg, se);
      }
    }
  }

  private void notifySchedulerListenersSchduled(Trigger trigger) {

    // build a list of all scheduler listeners that are to be notified...
    List<SchedulerListener> schedListeners = buildSchedulerListenerList();

    // notify all scheduler listeners
    for (SchedulerListener sl : schedListeners) {
      try {
        sl.jobScheduled(trigger);
      } catch (Exception e) {
        getLog().error("Error while notifying SchedulerListener of scheduled job." + "  Triger=" + trigger.getKey(), e);
      }
    }
  }

  private void notifySchedulerListenersUnscheduled(TriggerKey triggerKey) {

    // build a list of all scheduler listeners that are to be notified...
    List<SchedulerListener> schedListeners = buildSchedulerListenerList();

    // notify all scheduler listeners
    for (SchedulerListener sl : schedListeners) {
      try {
        if (triggerKey == null) {
          sl.schedulingDataCleared();
        }
        else {
          sl.jobUnscheduled(triggerKey);
        }
      } catch (Exception e) {
        getLog().error("Error while notifying SchedulerListener of unscheduled job." + "  Triger=" + (triggerKey == null ? "ALL DATA" : triggerKey), e);
      }
    }
  }

  void notifySchedulerListenersFinalized(Trigger trigger) {

    // build a list of all scheduler listeners that are to be notified...
    List<SchedulerListener> schedListeners = buildSchedulerListenerList();

    // notify all scheduler listeners
    for (SchedulerListener sl : schedListeners) {
      try {
        sl.triggerFinalized(trigger);
      } catch (Exception e) {
        getLog().error("Error while notifying SchedulerListener of finalized trigger." + "  Triger=" + trigger.getKey(), e);
      }
    }
  }

  private void notifySchedulerListenersPausedTrigger(TriggerKey triggerKey) {

    // build a list of all scheduler listeners that are to be notified...
    List<SchedulerListener> schedListeners = buildSchedulerListenerList();

    // notify all scheduler listeners
    for (SchedulerListener sl : schedListeners) {
      try {
        sl.triggerPaused(triggerKey);
      } catch (Exception e) {
        getLog().error("Error while notifying SchedulerListener of paused trigger: " + triggerKey, e);
      }
    }
  }

  private void notifySchedulerListenersResumedTrigger(TriggerKey key) {

    // build a list of all scheduler listeners that are to be notified...
    List<SchedulerListener> schedListeners = buildSchedulerListenerList();

    // notify all scheduler listeners
    for (SchedulerListener sl : schedListeners) {
      try {
        sl.triggerResumed(key);
      } catch (Exception e) {
        getLog().error("Error while notifying SchedulerListener of resumed trigger: " + key, e);
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
        getLog().error("Error while notifying SchedulerListener of inStandByMode.", e);
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
        getLog().error("Error while notifying SchedulerListener of startup.", e);
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
        getLog().error("Error while notifying SchedulerListener of shutdown.", e);
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
        getLog().error("Error while notifying SchedulerListener of shutdown.", e);
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
        getLog().error("Error while notifying SchedulerListener of JobAdded.", e);
      }
    }
  }

  void notifySchedulerListenersJobDeleted(JobKey jobKey) {

    // build a list of all scheduler listeners that are to be notified...
    List<SchedulerListener> schedListeners = buildSchedulerListenerList();

    // notify all scheduler listeners
    for (SchedulerListener sl : schedListeners) {
      try {
        sl.jobDeleted(jobKey);
      } catch (Exception e) {
        getLog().error("Error while notifying SchedulerListener of JobAdded.", e);
      }
    }
  }

  public JobFactory getJobFactory() {

    return jobFactory;
  }

  private void shutdownPlugins() {

    java.util.Iterator itr = mQuartzSchedulerResources.getSchedulerPlugins().iterator();
    while (itr.hasNext()) {
      SchedulerPlugin plugin = (SchedulerPlugin) itr.next();
      plugin.shutdown();
    }
  }

  private void startPlugins() {

    java.util.Iterator itr = mQuartzSchedulerResources.getSchedulerPlugins().iterator();
    while (itr.hasNext()) {
      SchedulerPlugin plugin = (SchedulerPlugin) itr.next();
      plugin.start();
    }
  }

}

// ///////////////////////////////////////////////////////////////////////////
//
// ErrorLogger - Scheduler Listener Class
//
// ///////////////////////////////////////////////////////////////////////////

class ErrorLogger extends SchedulerListenerSupport {

  ErrorLogger() {

  }

  @Override
  public void schedulerError(String msg, SchedulerException cause) {

    getLog().error(msg, cause);
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
