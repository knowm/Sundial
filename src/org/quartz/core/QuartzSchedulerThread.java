
/*
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

import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.quartz.JobPersistenceException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.spi.TriggerFiredBundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>
 * The thread responsible for performing the work of firing <code>{@link Trigger}</code>
 * s that are registered with the <code>{@link QuartzScheduler}</code>.
 * </p>
 *
 * @see QuartzScheduler
 * @see org.quartz.Job
 * @see Trigger
 *
 * @author James House
 */
public class QuartzSchedulerThread extends Thread {
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Data members.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    private QuartzScheduler qs;

    private QuartzSchedulerResources qsRsrcs;

    private final Object sigLock = new Object();

    private boolean signaled;
    private long signaledNextFireTime;

    private boolean paused;

    private AtomicBoolean halted;

    private Random random = new Random(System.currentTimeMillis());

    // When the scheduler finds there is no current trigger to fire, how long
    // it should wait until checking again...
    private static long DEFAULT_IDLE_WAIT_TIME = 30L * 1000L;

    private long idleWaitTime = DEFAULT_IDLE_WAIT_TIME;

    private int idleWaitVariablness = 7 * 1000;

    private long dbFailureRetryInterval = 15L * 1000L;

    private final Logger log = LoggerFactory.getLogger(getClass());

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Constructors.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Construct a new <code>QuartzSchedulerThread</code> for the given
     * <code>QuartzScheduler</code> as a non-daemon <code>Thread</code>
     * with normal priority.
     * </p>
     */
    QuartzSchedulerThread(QuartzScheduler qs, QuartzSchedulerResources qsRsrcs) {
        this(qs, qsRsrcs, qsRsrcs.getMakeSchedulerThreadDaemon(), Thread.NORM_PRIORITY);
    }

    /**
     * <p>
     * Construct a new <code>QuartzSchedulerThread</code> for the given
     * <code>QuartzScheduler</code> as a <code>Thread</code> with the given
     * attributes.
     * </p>
     */
    QuartzSchedulerThread(QuartzScheduler qs, QuartzSchedulerResources qsRsrcs, boolean setDaemon, int threadPrio) {
        super(qs.getSchedulerThreadGroup(), qsRsrcs.getThreadName());
        this.qs = qs;
        this.qsRsrcs = qsRsrcs;
        this.setDaemon(setDaemon);
        if(qsRsrcs.isThreadsInheritInitializersClassLoadContext()) {
            log.info("QuartzSchedulerThread Inheriting ContextClassLoader of thread: " + Thread.currentThread().getName());
            this.setContextClassLoader(Thread.currentThread().getContextClassLoader());
        }

        this.setPriority(threadPrio);

        // start the underlying thread, but put this object into the 'paused'
        // state
        // so processing doesn't start yet...
        paused = true;
        halted = new AtomicBoolean(false);
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Interface.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    void setIdleWaitTime(long waitTime) {
        idleWaitTime = waitTime;
        idleWaitVariablness = (int) (waitTime * 0.2);
    }

    private long getDbFailureRetryInterval() {
        return dbFailureRetryInterval;
    }

    public void setDbFailureRetryInterval(long dbFailureRetryInterval) {
        this.dbFailureRetryInterval = dbFailureRetryInterval;
    }

    private long getRandomizedIdleWaitTime() {
        return idleWaitTime - random.nextInt(idleWaitVariablness);
    }

    /**
     * <p>
     * Signals the main processing loop to pause at the next possible point.
     * </p>
     */
    void togglePause(boolean pause) {
        synchronized (sigLock) {
            paused = pause;

            if (paused) {
                signalSchedulingChange(0);
            } else {
                sigLock.notifyAll();
            }
        }
    }

    /**
     * <p>
     * Signals the main processing loop to pause at the next possible point.
     * </p>
     */
    void halt() {
        synchronized (sigLock) {
            halted.set(true);

            if (paused) {
                sigLock.notifyAll();
            } else {
                signalSchedulingChange(0);
            }
        }
    }

    boolean isPaused() {
        return paused;
    }

    /**
     * <p>
     * Signals the main processing loop that a change in scheduling has been
     * made - in order to interrupt any sleeping that may be occuring while
     * waiting for the fire time to arrive.
     * </p>
     *
     * @param candidateNewNextFireTime the time (in millis) when the newly scheduled trigger
     * will fire.  If this method is being called do to some other even (rather
     * than scheduling a trigger), the caller should pass zero (0).
     */
    public void signalSchedulingChange(long candidateNewNextFireTime) {
        synchronized(sigLock) {
            signaled = true;
            signaledNextFireTime = candidateNewNextFireTime;
            sigLock.notifyAll();
        }
    }

    public void clearSignaledSchedulingChange() {
        synchronized(sigLock) {
            signaled = false;
            signaledNextFireTime = 0;
        }
    }

    public boolean isScheduleChanged() {
        synchronized(sigLock) {
            return signaled;
        }
    }

    public long getSignaledNextFireTime() {
        synchronized(sigLock) {
            return signaledNextFireTime;
        }
    }

    /**
     * <p>
     * The main processing loop of the <code>QuartzSchedulerThread</code>.
     * </p>
     */
    public void run() {
        boolean lastAcquireFailed = false;

        while (!halted.get()) {
            try {
                // check if we're supposed to pause...
                synchronized (sigLock) {
                    while (paused && !halted.get()) {
                        try {
                            // wait until togglePause(false) is called...
                            sigLock.wait(1000L);
                        } catch (InterruptedException ignore) {
                        }
                    }

                    if (halted.get()) {
                        break;
                    }
                }

                int availThreadCount = qsRsrcs.getThreadPool().blockForAvailableThreads();
                if(availThreadCount > 0) { // will always be true, due to semantics of blockForAvailableThreads...

                    List<OperableTrigger> triggers = null;

                    long now = System.currentTimeMillis();

                    clearSignaledSchedulingChange();
                    try {
                        triggers = qsRsrcs.getJobStore().acquireNextTriggers(
                                now + idleWaitTime, Math.min(availThreadCount, qsRsrcs.getMaxBatchSize()), qsRsrcs.getBatchTimeWindow());
                        lastAcquireFailed = false;
                        if (log.isDebugEnabled()) 
                            log.debug("batch acquisition of " + (triggers == null ? 0 : triggers.size()) + " triggers");
                    } catch (JobPersistenceException jpe) {
                        if(!lastAcquireFailed) {
                            qs.notifySchedulerListenersError(
                                "An error occurred while scanning for the next triggers to fire.",
                                jpe);
                        }
                        lastAcquireFailed = true;
                    } catch (RuntimeException e) {
                        if(!lastAcquireFailed) {
                            getLog().error("quartzSchedulerThreadLoop: RuntimeException "
                                    +e.getMessage(), e);
                        }
                        lastAcquireFailed = true;
                    }

                    if (triggers != null && !triggers.isEmpty()) {

                        now = System.currentTimeMillis();
                        long triggerTime = triggers.get(0).getNextFireTime().getTime();
                        long timeUntilTrigger = triggerTime - now;
                        while(timeUntilTrigger > 2) {
                            synchronized (sigLock) {
                                if (halted.get()) {
                                    break;
                                }
                                if (!isCandidateNewTimeEarlierWithinReason(triggerTime, false)) {
                                    try {
                                        // we could have blocked a long while
                                        // on 'synchronize', so we must recompute
                                        now = System.currentTimeMillis();
                                        timeUntilTrigger = triggerTime - now;
                                        if(timeUntilTrigger >= 1)
                                            sigLock.wait(timeUntilTrigger);
                                    } catch (InterruptedException ignore) {
                                    }
                                }
                            }
                            if(releaseIfScheduleChangedSignificantly(triggers, triggerTime)) {
                                break;
                            }
                            now = System.currentTimeMillis();
                            timeUntilTrigger = triggerTime - now;
                        }

                        // this happens if releaseIfScheduleChangedSignificantly decided to release triggers
                        if(triggers.isEmpty())
                            continue;

                        // set triggers to 'executing'
                        List<TriggerFiredResult> bndles = new ArrayList<TriggerFiredResult>();

                        boolean goAhead = true;
                        synchronized(sigLock) {
                            goAhead = !halted.get();
                        }
                        if(goAhead) {
                            try {
                                List<TriggerFiredResult> res = qsRsrcs.getJobStore().triggersFired(triggers);
                                if(res != null)
                                    bndles = res;
                            } catch (SchedulerException se) {
                                qs.notifySchedulerListenersError(
                                        "An error occurred while firing triggers '"
                                                + triggers + "'", se);
                            }

                        }

                        for (int i = 0; i < bndles.size(); i++) {
                            TriggerFiredResult result =  bndles.get(i);
                            TriggerFiredBundle bndle =  result.getTriggerFiredBundle();
                            Exception exception = result.getException();

                            if (exception instanceof RuntimeException) {
                                getLog().error(
                                    "RuntimeException while firing trigger " +
                                    triggers.get(i), exception);
                                // db connection must have failed... keep
                                // retrying until it's up...
                                releaseTriggerRetryLoop(triggers.get(i));
                                continue;
                            }

                            // it's possible to get 'null' if the triggers was paused,
                            // blocked, or other similar occurrences that prevent it being
                            // fired at this time...  or if the scheduler was shutdown (halted)
                            if (bndle == null) {
                                try {
                                    qsRsrcs.getJobStore().releaseAcquiredTrigger(triggers.get(i));
                                } catch (SchedulerException se) {
                                    qs.notifySchedulerListenersError(
                                            "An error occurred while releasing triggers '"
                                                    + triggers.get(i).getKey() + "'", se);
                                    // db connection must have failed... keep retrying
                                    // until it's up...
                                    releaseTriggerRetryLoop(triggers.get(i));
                                }
                                continue;
                            }


                            // TODO: improvements:
                            //
                            // 2- make sure we can get a job runshell before firing triggers, or
                            //   don't let that throw an exception (right now it never does,
                            //   but the signature says it can).
                            // 3- acquire more triggers at a time (based on num threads available?)


                            JobRunShell shell = null;
                            try {
                                shell = qsRsrcs.getJobRunShellFactory().createJobRunShell(bndle);
                                shell.initialize(qs);
                            } catch (SchedulerException se) {
                                try {
                                    qsRsrcs.getJobStore().triggeredJobComplete(
                                            triggers.get(i), bndle.getJobDetail(), CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR);
                                } catch (SchedulerException se2) {
                                    qs.notifySchedulerListenersError(
                                            "An error occurred while placing job's triggers in error state '"
                                                    + triggers.get(i).getKey() + "'", se2);
                                    // db connection must have failed... keep retrying
                                    // until it's up...
                                    errorTriggerRetryLoop(bndle);
                                }
                                continue;
                            }

                            if (qsRsrcs.getThreadPool().runInThread(shell) == false) {
                                try {
                                    // this case should never happen, as it is indicative of the
                                    // scheduler being shutdown or a bug in the thread pool or
                                    // a thread pool being used concurrently - which the docs
                                    // say not to do...
                                    getLog().error("ThreadPool.runInThread() return false!");
                                    qsRsrcs.getJobStore().triggeredJobComplete(
                                            triggers.get(i), bndle.getJobDetail(), CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR);
                                } catch (SchedulerException se2) {
                                    qs.notifySchedulerListenersError(
                                            "An error occurred while placing job's triggers in error state '"
                                                    + triggers.get(i).getKey() + "'", se2);
                                    // db connection must have failed... keep retrying
                                    // until it's up...
                                    releaseTriggerRetryLoop(triggers.get(i));
                                }
                            }

                        }

                        continue; // while (!halted)
                    }
                } else { // if(availThreadCount > 0)
                    // should never happen, if threadPool.blockForAvailableThreads() follows contract
                    continue; // while (!halted)
                }

                long now = System.currentTimeMillis();
                long waitTime = now + getRandomizedIdleWaitTime();
                long timeUntilContinue = waitTime - now;
                synchronized(sigLock) {
                    try {
                        sigLock.wait(timeUntilContinue);
                    } catch (InterruptedException ignore) {
                    }
                }

            } catch(RuntimeException re) {
                getLog().error("Runtime error occurred in main trigger firing loop.", re);
            }
        } // while (!halted)

        // drop references to scheduler stuff to aid garbage collection...
        qs = null;
        qsRsrcs = null;
    }

    private boolean releaseIfScheduleChangedSignificantly(
            List<OperableTrigger> triggers, long triggerTime) {
        if (isCandidateNewTimeEarlierWithinReason(triggerTime, true)) {
            for (OperableTrigger trigger : triggers) {
                try {
                    // above call does a clearSignaledSchedulingChange()
                    qsRsrcs.getJobStore().releaseAcquiredTrigger(trigger);
                } catch (JobPersistenceException jpe) {
                    qs.notifySchedulerListenersError(
                            "An error occurred while releasing trigger '"
                                    + trigger.getKey() + "'", jpe);
                    // db connection must have failed... keep
                    // retrying until it's up...
                    releaseTriggerRetryLoop(trigger);
                } catch (RuntimeException e) {
                    getLog().error(
                            "releaseTriggerRetryLoop: RuntimeException "
                                    + e.getMessage(), e);
                    // db connection must have failed... keep
                    // retrying until it's up...
                    releaseTriggerRetryLoop(trigger);
                }
            }
            triggers.clear();
            return true;
        }
        return false;
    }

    private boolean isCandidateNewTimeEarlierWithinReason(long oldTime, boolean clearSignal) {

        // So here's the deal: We know due to being signaled that 'the schedule'
        // has changed.  We may know (if getSignaledNextFireTime() != 0) the
        // new earliest fire time.  We may not (in which case we will assume
        // that the new time is earlier than the trigger we have acquired).
        // In either case, we only want to abandon our acquired trigger and
        // go looking for a new one if "it's worth it".  It's only worth it if
        // the time cost incurred to abandon the trigger and acquire a new one
        // is less than the time until the currently acquired trigger will fire,
        // otherwise we're just "thrashing" the job store (e.g. database).
        //
        // So the question becomes when is it "worth it"?  This will depend on
        // the job store implementation (and of course the particular database
        // or whatever behind it).  Ideally we would depend on the job store
        // implementation to tell us the amount of time in which it "thinks"
        // it can abandon the acquired trigger and acquire a new one.  However
        // we have no current facility for having it tell us that, so we make
        // a somewhat educated but arbitrary guess ;-).

        synchronized(sigLock) {

            if (!isScheduleChanged())
                return false;

            boolean earlier = false;

            if(getSignaledNextFireTime() == 0)
                earlier = true;
            else if(getSignaledNextFireTime() < oldTime )
                earlier = true;

            if(earlier) {
                // so the new time is considered earlier, but is it enough earlier?
                long diff = oldTime - System.currentTimeMillis();
                if(diff < (qsRsrcs.getJobStore().supportsPersistence() ? 70L : 7L))
                    earlier = false;
            }

            if(clearSignal) {
                clearSignaledSchedulingChange();
            }

            return earlier;
        }
    }

    public void errorTriggerRetryLoop(TriggerFiredBundle bndle) {
        int retryCount = 0;
        try {
            while (!halted.get()) {
                try {
                    Thread.sleep(getDbFailureRetryInterval()); // retry every N
                    // seconds (the db
                    // connection must
                    // be failed)
                    retryCount++;
                    qsRsrcs.getJobStore().triggeredJobComplete(
                            bndle.getTrigger(), bndle.getJobDetail(), CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR);
                    retryCount = 0;
                    break;
                } catch (JobPersistenceException jpe) {
                    if(retryCount % 4 == 0) {
                        qs.notifySchedulerListenersError(
                            "An error occurred while releasing trigger '"
                                    + bndle.getTrigger().getKey() + "'", jpe);
                    }
                } catch (RuntimeException e) {
                    getLog().error("releaseTriggerRetryLoop: RuntimeException "+e.getMessage(), e);
                } catch (InterruptedException e) {
                    getLog().error("releaseTriggerRetryLoop: InterruptedException "+e.getMessage(), e);
                }
            }
        } finally {
            if(retryCount == 0) {
                getLog().info("releaseTriggerRetryLoop: connection restored.");
            }
        }
    }

    public void releaseTriggerRetryLoop(OperableTrigger trigger) {
        int retryCount = 0;
        try {
            while (!halted.get()) {
                try {
                    Thread.sleep(getDbFailureRetryInterval()); // retry every N
                    // seconds (the db
                    // connection must
                    // be failed)
                    retryCount++;
                    qsRsrcs.getJobStore().releaseAcquiredTrigger(trigger);
                    retryCount = 0;
                    break;
                } catch (JobPersistenceException jpe) {
                    if(retryCount % 4 == 0) {
                        qs.notifySchedulerListenersError(
                            "An error occurred while releasing trigger '"
                                    + trigger.getKey() + "'", jpe);
                    }
                } catch (RuntimeException e) {
                    getLog().error("releaseTriggerRetryLoop: RuntimeException "+e.getMessage(), e);
                } catch (InterruptedException e) {
                    getLog().error("releaseTriggerRetryLoop: InterruptedException "+e.getMessage(), e);
                }
            }
        } finally {
            if(retryCount == 0) {
                getLog().info("releaseTriggerRetryLoop: connection restored.");
            }
        }
    }

    public Logger getLog() {
        return log;
    }

} // end of QuartzSchedulerThread
