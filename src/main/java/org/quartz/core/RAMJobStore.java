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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import org.quartz.exceptions.JobPersistenceException;
import org.quartz.exceptions.ObjectAlreadyExistsException;
import org.quartz.exceptions.SchedulerException;
import org.quartz.jobs.JobDetail;
import org.quartz.triggers.OperableTrigger;
import org.quartz.triggers.Trigger;
import org.quartz.triggers.Trigger.CompletedExecutionInstruction;
import org.quartz.triggers.Trigger.TriggerTimeComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class implements a <code>{@link org.quartz.core.JobStore}</code> that utilizes RAM as its storage device.
 * </p>
 * <p>
 * As you should know, the ramification of this is that access is extremely fast, but the data is completely volatile - therefore this
 * <code>JobStore</code> should not be used if true persistence between program shutdowns is required.
 * </p>
 *
 * @author James House
 * @author Sharada Jambula
 * @author Eric Mueller
 */
public class RAMJobStore implements JobStore {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private HashMap<String, JobWrapper> jobsByKey = new HashMap<String, JobWrapper>(1000);

  private HashMap<String, TriggerWrapper> wrappedTriggersByKey = new HashMap<String, TriggerWrapper>(1000);

  private TreeSet<TriggerWrapper> timeWrappedTriggers = new TreeSet<TriggerWrapper>(new TriggerWrapperComparator());

  private HashMap<String, Calendar> calendarsByName = new HashMap<String, Calendar>(25);

  private ArrayList<TriggerWrapper> wrappedTriggers = new ArrayList<TriggerWrapper>(1000);

  private final Object lock = new Object();

  private HashSet<String> blockedJobs = new HashSet<String>();

  private long misfireThreshold = 5000L;

  private SchedulerSignaler mSignaler;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Create a new <code>RAMJobStore</code>.
   * </p>
   */
  public RAMJobStore() {

  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Called by the QuartzScheduler before the <code>JobStore</code> is used, in order to give the it a chance to initialize.
   * </p>
   */

  @Override
  public void initialize(SchedulerSignaler signaler) {

    mSignaler = signaler;

    logger.info("RAMJobStore initialized.");
  }

  @Override
  public void schedulerStarted() throws SchedulerException {

    // nothing to do
  }

  public long getMisfireThreshold() {

    return misfireThreshold;
  }

  /**
   * The number of milliseconds by which a trigger must have missed its next-fire-time, in order for it to be considered "misfired" and thus have its
   * misfire instruction applied.
   *
   * @param misfireThreshold
   */
  public void setMisfireThreshold(long misfireThreshold) {

    if (misfireThreshold < 1) {
      throw new IllegalArgumentException("Misfirethreashold must be larger than 0");
    }
    this.misfireThreshold = misfireThreshold;
  }

  /**
   * <p>
   * Store the given <code>{@link org.quartz.jobs.JobDetail}</code> and <code>{@link org.quartz.triggers.Trigger}</code>.
   * </p>
   *
   * @param newJob The <code>JobDetail</code> to be stored.
   * @param newTrigger The <code>Trigger</code> to be stored.
   * @throws ObjectAlreadyExistsException if a <code>Job</code> with the same name/group already exists.
   */
  @Override
  public void storeJobAndTrigger(JobDetail newJob, OperableTrigger newTrigger) throws JobPersistenceException {

    storeJob(newJob, false);
    storeTrigger(newTrigger, false);
  }

  /**
   * <p>
   * Store the given <code>{@link org.quartz.jobs.Job}</code>.
   * </p>
   *
   * @param newJob The <code>Job</code> to be stored.
   * @param replaceExisting If <code>true</code>, any <code>Job</code> existing in the <code>JobStore</code> with the same name & group should be
   *        over-written.
   * @throws ObjectAlreadyExistsException if a <code>Job</code> with the same name/group already exists, and replaceExisting is set to false.
   */
  @Override
  public void storeJob(JobDetail newJob, boolean replaceExisting) throws ObjectAlreadyExistsException {

    JobWrapper jw = new JobWrapper((JobDetail) newJob.clone());

    boolean repl = false;

    synchronized (lock) {

      if (jobsByKey.get(jw.key) != null) {
        if (!replaceExisting) {
          throw new ObjectAlreadyExistsException(newJob);
        }
        repl = true;
      }

      if (!repl) {

        // add to jobs by FQN map
        jobsByKey.put(jw.key, jw);
      } else {
        // update job detail
        JobWrapper orig = jobsByKey.get(jw.key);
        orig.jobDetail = jw.jobDetail; // already cloned
      }
    }
  }

  /**
   * <p>
   * Remove (delete) the <code>{@link org.quartz.jobs.Job}</code> with the given name, and any <code>{@link org.quartz.triggers.Trigger}</code> s that
   * reference it.
   * </p>
   *
   * @return <code>true</code> if a <code>Job</code> with the given name & group was found and removed from the store.
   */
  @Override
  public boolean removeJob(String jobKey) {

    boolean found = false;

    synchronized (lock) {
      List<Trigger> triggers = getTriggersForJob(jobKey);
      for (Trigger trig : triggers) {
        this.removeTrigger(trig.getName());
        found = true;
      }

      found = (jobsByKey.remove(jobKey) != null) | found;
      if (found) {

      }
    }

    return found;
  }

  /**
   * <p>
   * Store the given <code>{@link org.quartz.triggers.Trigger}</code>.
   * </p>
   *
   * @param newTrigger The <code>Trigger</code> to be stored.
   * @param replaceExisting If <code>true</code>, any <code>Trigger</code> existing in the <code>JobStore</code> with the same name & group should be
   *        over-written.
   * @throws ObjectAlreadyExistsException if a <code>Trigger</code> with the same name/group already exists, and replaceExisting is set to false.
   * @see #pauseTriggerGroup(SchedulingContext, String)
   */
  @Override
  public void storeTrigger(OperableTrigger newTrigger, boolean replaceExisting) throws JobPersistenceException {

    TriggerWrapper tw = new TriggerWrapper((OperableTrigger) newTrigger.clone());

    synchronized (lock) {
      if (wrappedTriggersByKey.get(tw.key) != null) {
        if (!replaceExisting) {
          throw new ObjectAlreadyExistsException(newTrigger);
        }

        removeTrigger(newTrigger.getName());
      }

      if (retrieveJob(newTrigger.getJobName()) == null) {
        throw new JobPersistenceException("The job (" + newTrigger.getJobName() + ") referenced by the trigger does not exist.");
      }

      // add to triggers array
      wrappedTriggers.add(tw);

      // add to triggers by FQN map
      wrappedTriggersByKey.put(tw.key, tw);

      if (blockedJobs.contains(tw.jobKey)) {
        tw.state = TriggerWrapper.STATE_BLOCKED;
      } else {
        timeWrappedTriggers.add(tw);
      }
    }
  }

  @Override
  public boolean removeTrigger(String triggerName) {

    boolean found = false;

    synchronized (lock) {
      // remove from triggers by FQN map
      found = (wrappedTriggersByKey.remove(triggerName) == null) ? false : true;
      if (found) {
        TriggerWrapper tw = null;
        // remove from triggers array
        Iterator<TriggerWrapper> tgs = wrappedTriggers.iterator();
        while (tgs.hasNext()) {
          tw = tgs.next();
          if (triggerName.equals(tw.key)) {
            tgs.remove();
            break;
          }
        }
        timeWrappedTriggers.remove(tw);

      }
    }

    return found;
  }

  /**
   * @see org.quartz.core.JobStore#replaceTrigger(org.quartz.core.SchedulingContext, java.lang.String, java.lang.String, org.quartz.triggers.Trigger)
   */
  @Override
  public boolean replaceTrigger(String triggerKey, OperableTrigger newTrigger) throws JobPersistenceException {

    boolean found = false;

    synchronized (lock) {
      // remove from triggers by FQN map
      TriggerWrapper tw = wrappedTriggersByKey.remove(triggerKey);
      found = (tw == null) ? false : true;

      if (found) {

        if (!tw.getTrigger().getJobName().equals(newTrigger.getJobName())) {
          throw new JobPersistenceException("New trigger is not related to the same job as the old trigger.");
        }

        tw = null;
        // remove from triggers array
        Iterator<TriggerWrapper> tgs = wrappedTriggers.iterator();
        while (tgs.hasNext()) {
          tw = tgs.next();
          if (triggerKey.equals(tw.key)) {
            tgs.remove();
            break;
          }
        }
        timeWrappedTriggers.remove(tw);

        try {
          storeTrigger(newTrigger, false);
        } catch (JobPersistenceException jpe) {
          storeTrigger(tw.getTrigger(), false); // put previous trigger back...
          throw jpe;
        }
      }
    }

    return found;
  }

  /**
   * <p>
   * Retrieve the <code>{@link org.quartz.jobs.JobDetail}</code> for the given <code>{@link org.quartz.jobs.Job}</code>.
   * </p>
   *
   * @return The desired <code>Job</code>, or null if there is no match.
   */
  @Override
  public JobDetail retrieveJob(String jobKey) {

    synchronized (lock) {
      JobWrapper jw = jobsByKey.get(jobKey);
      return (jw != null) ? (JobDetail) jw.jobDetail.clone() : null;
    }
  }

  /**
   * <p>
   * Retrieve the given <code>{@link org.quartz.triggers.Trigger}</code>.
   * </p>
   *
   * @return The desired <code>Trigger</code>, or null if there is no match.
   */
  @Override
  public OperableTrigger retrieveTrigger(String triggerKey) {

    synchronized (lock) {
      TriggerWrapper tw = wrappedTriggersByKey.get(triggerKey);

      return (tw != null) ? (OperableTrigger) tw.getTrigger().clone() : null;
    }
  }

  /**
   * <p>
   * Retrieve the given <code>{@link org.quartz.triggers.Trigger}</code>.
   * </p>
   *
   * @param calName The name of the <code>Calendar</code> to be retrieved.
   * @return The desired <code>Calendar</code>, or null if there is no match.
   */
  @Override
  public Calendar retrieveCalendar(String calName) {

    synchronized (lock) {
      Calendar cal = calendarsByName.get(calName);
      if (cal != null) {
        return (Calendar) cal.clone();
      }
      return null;
    }
  }

  /**
   * <p>
   * Get all of the Triggers that are associated to the given Job.
   * </p>
   * <p>
   * If there are no matches, a zero-length array should be returned.
   * </p>
   */
  @Override
  public List<Trigger> getTriggersForJob(String jobKey) {

    ArrayList<Trigger> trigList = new ArrayList<Trigger>();

    synchronized (lock) {
      for (int i = 0; i < wrappedTriggers.size(); i++) {
        TriggerWrapper tw = wrappedTriggers.get(i);
        if (tw.jobKey.equals(jobKey)) {
          trigList.add((OperableTrigger) tw.trigger.clone());
        }
      }
    }

    return trigList;
  }

  private ArrayList<TriggerWrapper> getTriggerWrappersForJob(String jobKey) {

    ArrayList<TriggerWrapper> trigList = new ArrayList<TriggerWrapper>();

    synchronized (lock) {
      for (int i = 0; i < wrappedTriggers.size(); i++) {
        TriggerWrapper tw = wrappedTriggers.get(i);
        if (tw.jobKey.equals(jobKey)) {
          trigList.add(tw);
        }
      }
    }

    return trigList;
  }

  private boolean applyMisfire(TriggerWrapper tw) {

    long misfireTime = System.currentTimeMillis();
    if (getMisfireThreshold() > 0) {
      misfireTime -= getMisfireThreshold();
    }

    Date tnft = tw.trigger.getNextFireTime();
    if (tnft == null || tnft.getTime() > misfireTime || tw.trigger.getMisfireInstruction() == Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY) {
      return false;
    }

    Calendar cal = null;
    if (tw.trigger.getCalendarName() != null) {
      cal = retrieveCalendar(tw.trigger.getCalendarName());
    }

    mSignaler.notifyTriggerListenersMisfired((OperableTrigger) tw.trigger.clone());

    tw.trigger.updateAfterMisfire(cal);

    if (tw.trigger.getNextFireTime() == null) {
      tw.state = TriggerWrapper.STATE_COMPLETE;
      mSignaler.notifySchedulerListenersFinalized(tw.trigger);
      synchronized (lock) {
        timeWrappedTriggers.remove(tw);
      }
    } else if (tnft.equals(tw.trigger.getNextFireTime())) {
      return false;
    }

    return true;
  }

  private static final AtomicLong ftrCtr = new AtomicLong(System.currentTimeMillis());

  private String getFiredTriggerRecordId() {

    return String.valueOf(ftrCtr.incrementAndGet());
  }

  /**
   * <p>
   * Get a handle to the next trigger to be fired, and mark it as 'reserved' by the calling scheduler.
   * </p>
   *
   * @see #releaseAcquiredTrigger(SchedulingContext, Trigger)
   */
  @Override
  public List<OperableTrigger> acquireNextTriggers(long noLaterThan, int maxCount, long timeWindow) {

    synchronized (lock) {
      List<OperableTrigger> result = new ArrayList<OperableTrigger>();

      while (true) {
        TriggerWrapper tw;

        try {
          tw = timeWrappedTriggers.first();
          if (tw == null) {
            return result;
          }
          timeWrappedTriggers.remove(tw);
        } catch (java.util.NoSuchElementException nsee) {
          return result;
        }

        if (tw.trigger.getNextFireTime() == null) {
          continue;
        }

        if (applyMisfire(tw)) {
          if (tw.trigger.getNextFireTime() != null) {
            timeWrappedTriggers.add(tw);
          }
          continue;
        }

        if (tw.getTrigger().getNextFireTime().getTime() > noLaterThan + timeWindow) {
          timeWrappedTriggers.add(tw);
          return result;
        }

        tw.state = TriggerWrapper.STATE_ACQUIRED;

        tw.trigger.setFireInstanceId(getFiredTriggerRecordId());
        OperableTrigger trig = (OperableTrigger) tw.trigger.clone();
        result.add(trig);

        if (result.size() == maxCount) {
          return result;
        }
      }
    }
  }

  /**
   * <p>
   * Inform the <code>JobStore</code> that the scheduler no longer plans to fire the given <code>Trigger</code>, that it had previously acquired
   * (reserved).
   * </p>
   */
  @Override
  public void releaseAcquiredTrigger(OperableTrigger trigger) {

    synchronized (lock) {
      TriggerWrapper tw = wrappedTriggersByKey.get(trigger.getName());
      if (tw != null && tw.state == TriggerWrapper.STATE_ACQUIRED) {
        tw.state = TriggerWrapper.STATE_WAITING;
        timeWrappedTriggers.add(tw);
      }
    }
  }

  /**
   * <p>
   * Inform the <code>JobStore</code> that the scheduler is now firing the given <code>Trigger</code> (executing its associated <code>Job</code>),
   * that it had previously acquired (reserved).
   * </p>
   */
  @Override
  public List<TriggerFiredResult> triggersFired(List<OperableTrigger> triggers) {

    synchronized (lock) {
      List<TriggerFiredResult> results = new ArrayList<TriggerFiredResult>();

      for (OperableTrigger trigger : triggers) {
        TriggerWrapper tw = wrappedTriggersByKey.get(trigger.getName());
        // was the trigger deleted since being acquired?
        if (tw == null || tw.trigger == null) {
          continue;
        }
        // was the trigger completed, paused, blocked, etc. since being acquired?
        if (tw.state != TriggerWrapper.STATE_ACQUIRED) {
          continue;
        }

        Calendar cal = null;
        if (tw.trigger.getCalendarName() != null) {
          cal = retrieveCalendar(tw.trigger.getCalendarName());
          if (cal == null) {
            continue;
          }
        }
        Date prevFireTime = trigger.getPreviousFireTime();
        // in case trigger was replaced between acquiring and firing
        timeWrappedTriggers.remove(tw);
        // call triggered on our copy, and the scheduler's copy
        tw.trigger.triggered(cal);
        trigger.triggered(cal);
        // tw.state = TriggerWrapper.STATE_EXECUTING;
        tw.state = TriggerWrapper.STATE_WAITING;

        TriggerFiredBundle bndle = new TriggerFiredBundle(retrieveJob(tw.jobKey), trigger, cal, false, new Date(), trigger.getPreviousFireTime(),
            prevFireTime, trigger.getNextFireTime());

        JobDetail job = bndle.getJobDetail();

        if (!job.isConcurrencyAllowed()) {
          ArrayList<TriggerWrapper> trigs = getTriggerWrappersForJob(job.getName());
          Iterator<TriggerWrapper> itr = trigs.iterator();
          while (itr.hasNext()) {
            TriggerWrapper ttw = itr.next();
            if (ttw.state == TriggerWrapper.STATE_WAITING) {
              ttw.state = TriggerWrapper.STATE_BLOCKED;
            }
            if (ttw.state == TriggerWrapper.STATE_PAUSED) {
              ttw.state = TriggerWrapper.STATE_PAUSED_BLOCKED;
            }
            timeWrappedTriggers.remove(ttw);
          }
          blockedJobs.add(job.getName());
        } else if (tw.trigger.getNextFireTime() != null) {
          synchronized (lock) {
            timeWrappedTriggers.add(tw);
          }
        }

        results.add(new TriggerFiredResult(bndle));
      }
      return results;
    }
  }

  /**
   * <p>
   * Inform the <code>JobStore</code> that the scheduler has completed the firing of the given <code>Trigger</code> (and the execution its associated
   * <code>Job</code>), and that the <code>{@link org.quartz.jobs.JobDataMap}</code> in the given <code>JobDetail</code> should be updated if the
   * <code>Job</code> is stateful.
   * </p>
   */
  @Override
  public void triggeredJobComplete(OperableTrigger trigger, JobDetail jobDetail, CompletedExecutionInstruction triggerInstCode) {

    synchronized (lock) {

      JobWrapper jw = jobsByKey.get(jobDetail.getName());
      TriggerWrapper tw = wrappedTriggersByKey.get(trigger.getName());

      // It's possible that the job is null if:
      // 1- it was deleted during execution
      // 2- RAMJobStore is being used only for volatile jobs / triggers
      // from the JDBC job store
      if (jw != null) {
        JobDetail jd = jw.jobDetail;

        if (!jd.isConcurrencyAllowed()) {
          blockedJobs.remove(jd.getName());
          ArrayList<TriggerWrapper> trigs = getTriggerWrappersForJob(jd.getName());
          for (TriggerWrapper ttw : trigs) {
            if (ttw.state == TriggerWrapper.STATE_BLOCKED) {
              ttw.state = TriggerWrapper.STATE_WAITING;
              timeWrappedTriggers.add(ttw);
            }
            if (ttw.state == TriggerWrapper.STATE_PAUSED_BLOCKED) {
              ttw.state = TriggerWrapper.STATE_PAUSED;
            }
          }
          mSignaler.signalSchedulingChange(0L);
        }
      } else { // even if it was deleted, there may be cleanup to do
        blockedJobs.remove(jobDetail.getName());
      }

      // check for trigger deleted during execution...
      if (tw != null) {
        if (triggerInstCode == CompletedExecutionInstruction.DELETE_TRIGGER) {

          if (trigger.getNextFireTime() == null) {
            // double check for possible reschedule within job
            // execution, which would cancel the need to delete...
            if (tw.getTrigger().getNextFireTime() == null) {
              removeTrigger(trigger.getName());
            }
          } else {
            removeTrigger(trigger.getName());
            mSignaler.signalSchedulingChange(0L);
          }
        } else if (triggerInstCode == CompletedExecutionInstruction.SET_TRIGGER_COMPLETE) {
          tw.state = TriggerWrapper.STATE_COMPLETE;
          timeWrappedTriggers.remove(tw);
          mSignaler.signalSchedulingChange(0L);
        } else if (triggerInstCode == CompletedExecutionInstruction.SET_TRIGGER_ERROR) {
          logger.info("Trigger " + trigger.getName() + " set to ERROR state.");
          tw.state = TriggerWrapper.STATE_ERROR;
          mSignaler.signalSchedulingChange(0L);
        } else if (triggerInstCode == CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR) {
          logger.info("All triggers of Job " + trigger.getJobName() + " set to ERROR state.");
          setAllTriggersOfJobToState(trigger.getJobName(), TriggerWrapper.STATE_ERROR);
          mSignaler.signalSchedulingChange(0L);
        } else if (triggerInstCode == CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_COMPLETE) {
          setAllTriggersOfJobToState(trigger.getJobName(), TriggerWrapper.STATE_COMPLETE);
          mSignaler.signalSchedulingChange(0L);
        }
      }
    }
  }

  private void setAllTriggersOfJobToState(String jobKey, int state) {

    ArrayList tws = getTriggerWrappersForJob(jobKey);
    Iterator itr = tws.iterator();
    while (itr.hasNext()) {
      TriggerWrapper tw = (TriggerWrapper) itr.next();
      tw.state = state;
      if (state != TriggerWrapper.STATE_WAITING) {
        timeWrappedTriggers.remove(tw);
      }
    }
  }

  @Override
  public void setThreadPoolSize(final int poolSize) {

    //
  }

  /**
   * <p>
   * Get the names of all of the <code>{@link org.quartz.jobs.Job}</code> s
   * </p>
   */
  @Override
  public Set<String> getJobKeys() {

    Set<String> outList = new HashSet<String>();

    synchronized (lock) {

      for (JobWrapper jw : jobsByKey.values()) {

        if (jw != null) {

          outList.add(jw.jobDetail.getName());
        }
      }
    }

    return outList == null ? java.util.Collections.<String> emptySet() : outList;
  }

}

/**
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * Helper Classes. * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * *
 */

class TriggerWrapperComparator implements Comparator<TriggerWrapper> {

  private TriggerTimeComparator ttc = new TriggerTimeComparator();

  @Override
  public int compare(TriggerWrapper trig1, TriggerWrapper trig2) {

    return ttc.compare(trig1.trigger, trig2.trigger);
  }

  @Override
  public boolean equals(Object obj) {

    return (obj instanceof TriggerWrapperComparator);
  }
}

class JobWrapper {

  public String key;

  public JobDetail jobDetail;

  JobWrapper(JobDetail jobDetail) {

    this.jobDetail = jobDetail;
    key = jobDetail.getName();
  }

  @Override
  public boolean equals(Object obj) {

    if (obj instanceof JobWrapper) {
      JobWrapper jw = (JobWrapper) obj;
      if (jw.key.equals(this.key)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public int hashCode() {

    return key.hashCode();
  }

}

class TriggerWrapper {

  public String key;

  public String jobKey;

  public OperableTrigger trigger;

  public int state = STATE_WAITING;

  public static final int STATE_WAITING = 0;

  public static final int STATE_ACQUIRED = 1;

  // public static final int STATE_EXECUTING = 2;

  public static final int STATE_COMPLETE = 3;

  public static final int STATE_PAUSED = 4;

  public static final int STATE_BLOCKED = 5;

  public static final int STATE_PAUSED_BLOCKED = 6;

  public static final int STATE_ERROR = 7;

  TriggerWrapper(OperableTrigger trigger) {

    this.trigger = trigger;
    key = trigger.getName();
    this.jobKey = trigger.getJobName();
  }

  @Override
  public boolean equals(Object obj) {

    if (obj instanceof TriggerWrapper) {
      TriggerWrapper tw = (TriggerWrapper) obj;
      if (tw.key.equals(this.key)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public int hashCode() {

    return key.hashCode();
  }

  public OperableTrigger getTrigger() {

    return this.trigger;
  }
}
