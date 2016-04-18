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

import org.quartz.QuartzScheduler;
import org.quartz.exceptions.JobExecutionException;
import org.quartz.exceptions.JobPersistenceException;
import org.quartz.exceptions.SchedulerException;
import org.quartz.jobs.Job;
import org.quartz.jobs.JobDetail;
import org.quartz.listeners.SchedulerListenerSupport;
import org.quartz.triggers.OperableTrigger;
import org.quartz.triggers.Trigger.CompletedExecutionInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * JobRunShell instances are responsible for providing the 'safe' environment for <code>Job</code> s to run in, and for performing all of the work of
 * executing the <code>Job</code>, catching ANY thrown exceptions, updating the <code>Trigger</code> with the <code>Job</code>'s completion code, etc.
 * </p>
 * <p>
 * A <code>JobRunShell</code> instance is created by a <code>JobRunShellFactory</code> on behalf of the <code>QuartzSchedulerThread</code> which then
 * runs the shell in a thread from the configured <code>ThreadPool</code> when the scheduler determines that a <code>Job</code> has been triggered.
 * </p>
 *
 * @see JobRunShellFactory
 * @see org.quartz.core.QuartzSchedulerThread
 * @see org.quartz.jobs.Job
 * @see org.quartz.triggers.Trigger
 * @author James House
 */
public class JobRunShell extends SchedulerListenerSupport implements Runnable {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private JobExecutionContextImpl jec = null;

  private QuartzScheduler qs = null;

  private TriggerFiredBundle firedTriggerBundle = null;

  private Scheduler scheduler = null;

  private volatile boolean shutdownRequested = false;

  private final Logger log = LoggerFactory.getLogger(getClass());

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Create a JobRunShell instance with the given settings.
   * </p>
   *
   * @param jobRunShellFactory A handle to the <code>JobRunShellFactory</code> that produced this <code>JobRunShell</code>.
   * @param scheduler The <code>Scheduler</code> instance that should be made available within the <code>JobExecutionContext</code>.
   * @param schdCtxt the <code>SchedulingContext</code> that should be used by the <code>JobRunShell</code> when making updates to the
   *        <code>JobStore</code>.
   */
  JobRunShell(Scheduler scheduler, TriggerFiredBundle bndle) {

    this.scheduler = scheduler;
    this.firedTriggerBundle = bndle;
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  @Override
  public void schedulerShuttingdown() {

    requestShutdown();
  }

  @Override
  protected Logger getLog() {

    return log;
  }

  void initialize(QuartzScheduler qs) throws SchedulerException {

    this.qs = qs;

    Job job = null;
    JobDetail jobDetail = firedTriggerBundle.getJobDetail();

    try {
      job = qs.getJobFactory().newJob(firedTriggerBundle, scheduler);
    } catch (SchedulerException se) {
      qs.notifySchedulerListenersError("An error occured instantiating job to be executed. job= '" + jobDetail.getName() + "'", se);
      throw se;
    } catch (Throwable ncdfe) { // such as NoClassDefFoundError
      SchedulerException se = new SchedulerException("Problem instantiating class '" + jobDetail.getJobClass().getName() + "' - ", ncdfe);
      qs.notifySchedulerListenersError("An error occured instantiating job to be executed. job= '" + jobDetail.getName() + "'", se);
      throw se;
    }

    this.jec = new JobExecutionContextImpl(scheduler, firedTriggerBundle, job);
  }

  private void requestShutdown() {

    shutdownRequested = true;
  }

  @Override
  public void run() {

    qs.addInternalSchedulerListener(this);

    try {
      OperableTrigger trigger = (OperableTrigger) jec.getTrigger();
      JobDetail jobDetail = jec.getJobDetail();

      do {

        JobExecutionException jobExEx = null;
        Job job = jec.getJobInstance();

        try {
          begin();
        } catch (SchedulerException se) {
          qs.notifySchedulerListenersError("Error executing Job (" + jec.getJobDetail().getName() + ": couldn't begin execution.", se);
          break;
        }

        // notify job & trigger listeners...
        try {
          if (!notifyListenersBeginning(jec)) {
            break;
          }
        } catch (VetoedException ve) {
          try {
            CompletedExecutionInstruction instCode = trigger.executionComplete(jec, null);
            try {
              qs.notifyJobStoreJobVetoed(trigger, jobDetail, instCode);
            } catch (JobPersistenceException jpe) {
              vetoedJobRetryLoop(trigger, jobDetail, instCode);
            }
            complete(true);
          } catch (SchedulerException se) {
            qs.notifySchedulerListenersError("Error during veto of Job (" + jec.getJobDetail().getName() + ": couldn't finalize execution.", se);
          }
          break;
        }

        long startTime = System.currentTimeMillis();
        long endTime = startTime;

        // execute the job
        try {
          log.debug("Calling execute on job " + jobDetail.getName());
          job.execute(jec);
          endTime = System.currentTimeMillis();
        } catch (JobExecutionException jee) {
          endTime = System.currentTimeMillis();
          jobExEx = jee;
          getLog().info("Job " + jobDetail.getName() + " threw a JobExecutionException: ", jobExEx);
        } catch (Throwable e) {
          endTime = System.currentTimeMillis();
          getLog().error("Job " + jobDetail.getName() + " threw an unhandled Exception: ", e);
          SchedulerException se = new SchedulerException("Job threw an unhandled exception.", e);
          qs.notifySchedulerListenersError("Job (" + jec.getJobDetail().getName() + " threw an exception.", se);
          jobExEx = new JobExecutionException(se, false);
        }

        jec.setJobRunTime(endTime - startTime);

        // notify all job listeners
        if (!notifyJobListenersComplete(jec, jobExEx)) {
          break;
        }

        CompletedExecutionInstruction instCode = CompletedExecutionInstruction.NOOP;

        // update the trigger
        try {
          instCode = trigger.executionComplete(jec, jobExEx);
        } catch (Exception e) {
          // If this happens, there's a bug in the trigger...
          SchedulerException se = new SchedulerException("Trigger threw an unhandled exception.", e);
          qs.notifySchedulerListenersError("Please report this error to the Quartz developers.", se);
        }

        // notify all trigger listeners
        if (!notifyTriggerListenersComplete(jec, instCode)) {
          break;
        }

        // update job/trigger or re-execute job
        if (instCode == CompletedExecutionInstruction.RE_EXECUTE_JOB) {
          jec.incrementRefireCount();
          try {
            complete(false);
          } catch (SchedulerException se) {
            qs.notifySchedulerListenersError("Error executing Job (" + jec.getJobDetail().getName() + ": couldn't finalize execution.", se);
          }
          continue;
        }

        try {
          complete(true);
        } catch (SchedulerException se) {
          qs.notifySchedulerListenersError("Error executing Job (" + jec.getJobDetail().getName() + ": couldn't finalize execution.", se);
          continue;
        }

        try {
          qs.notifyJobStoreJobComplete(trigger, jobDetail, instCode);
        } catch (JobPersistenceException jpe) {
          qs.notifySchedulerListenersError("An error occured while marking executed job complete. job= '" + jobDetail.getName() + "'", jpe);
          if (!completeTriggerRetryLoop(trigger, jobDetail, instCode)) {
            return;
          }
        }

        break;
      } while (true);

    } finally {
      qs.removeInternalSchedulerListener(this);
    }
  }

  private void begin() throws SchedulerException {

  }

  private void complete(boolean successfulExecution) throws SchedulerException {

  }

  private boolean notifyListenersBeginning(JobExecutionContext jec) throws VetoedException {

    boolean vetoed = false;

    // notify all trigger listeners
    try {
      vetoed = qs.notifyTriggerListenersFired(jec);
    } catch (SchedulerException se) {
      qs.notifySchedulerListenersError("Unable to notify TriggerListener(s) while firing trigger " + "(Trigger and Job will NOT be fired!). trigger= "
          + jec.getTrigger().getName() + " job= " + jec.getJobDetail().getName(), se);

      return false;
    }

    if (vetoed) {
      try {
        qs.notifyJobListenersWasVetoed(jec);
      } catch (SchedulerException se) {
        qs.notifySchedulerListenersError("Unable to notify JobListener(s) of vetoed execution " + "while firing trigger (Trigger and Job will NOT be "
            + "fired!). trigger= " + jec.getTrigger().getName() + " job= " + jec.getJobDetail().getName(), se);

      }
      throw new VetoedException();
    }

    // notify all job listeners
    try {
      qs.notifyJobListenersToBeExecuted(jec);
    } catch (SchedulerException se) {
      qs.notifySchedulerListenersError("Unable to notify JobListener(s) of Job to be executed: " + "(Job will NOT be executed!). trigger= "
          + jec.getTrigger().getName() + " job= " + jec.getJobDetail().getName(), se);

      return false;
    }

    return true;
  }

  private boolean notifyJobListenersComplete(JobExecutionContext jec, JobExecutionException jobExEx) {

    try {
      qs.notifyJobListenersWasExecuted(jec, jobExEx);
    } catch (SchedulerException se) {
      qs.notifySchedulerListenersError("Unable to notify JobListener(s) of Job that was executed: " + "(error will be ignored). trigger= "
          + jec.getTrigger().getName() + " job= " + jec.getJobDetail().getName(), se);

      return false;
    }

    return true;
  }

  private boolean notifyTriggerListenersComplete(JobExecutionContext jec, CompletedExecutionInstruction instCode) {

    try {
      qs.notifyTriggerListenersComplete(jec, instCode);

    } catch (SchedulerException se) {
      qs.notifySchedulerListenersError("Unable to notify TriggerListener(s) of Job that was executed: " + "(error will be ignored). trigger= "
          + jec.getTrigger().getName() + " job= " + jec.getJobDetail().getName(), se);

      return false;
    }
    if (jec.getTrigger().getNextFireTime() == null) {
      qs.notifySchedulerListenersFinalized(jec.getTrigger());
    }

    return true;
  }

  private boolean completeTriggerRetryLoop(OperableTrigger trigger, JobDetail jobDetail, CompletedExecutionInstruction instCode) {

    long count = 0;
    while (!shutdownRequested && !qs.isShuttingDown()) {
      try {
        Thread.sleep(15 * 1000L); // retry every 15 seconds (the db
        // connection must be failed)
        qs.notifyJobStoreJobComplete(trigger, jobDetail, instCode);
        return true;
      } catch (JobPersistenceException jpe) {
        if (count % 4 == 0) {
          qs.notifySchedulerListenersError(
              "An error occured while marking executed job complete (will continue attempts). job= '" + jobDetail.getName() + "'", jpe);
        }
      } catch (InterruptedException ignore) {
      }
      count++;
    }
    return false;
  }

  private boolean vetoedJobRetryLoop(OperableTrigger trigger, JobDetail jobDetail, CompletedExecutionInstruction instCode) {

    while (!shutdownRequested) {
      try {
        Thread.sleep(5 * 1000L); // retry every 5 seconds (the db
        // connection must be failed)
        qs.notifyJobStoreJobVetoed(trigger, jobDetail, instCode);
        return true;
      } catch (JobPersistenceException jpe) {
        qs.notifySchedulerListenersError("An error occured while marking executed job vetoed. job= '" + jobDetail.getName() + "'", jpe);
      } catch (InterruptedException ignore) {
      }
    }
    return false;
  }

  private static class VetoedException extends Exception {

    public VetoedException() {

    }
  }

}
