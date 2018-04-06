package org.quartz.core;

import org.quartz.exceptions.SchedulerConfigException;
import org.quartz.exceptions.SchedulerException;

/**
 * Responsible for creating the instances of a {@link JobRunShell} to be used within the
 * <class>{@link org.quartz.QuartzScheduler} </code> instance. It will create a standard {@link
 * JobRunShell} unless the job class has the {@link ExecuteInJTATransaction} annotation in which
 * case it will create a {@link JTAJobRunShell}.
 *
 * <p>This implementation does not re-use any objects, it simply makes a new JTAJobRunShell each
 * time <code>borrowJobRunShell()</code> is called.
 *
 * @author James House
 */
public class StandardJobRunShellFactory implements JobRunShellFactory {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private Scheduler scheduler;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  public StandardJobRunShellFactory() {}

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * Initialize the factory, providing a handle to the <code>Scheduler</code> that should be made
   * available within the <code>JobRunShell</code> and the <code>JobExecutionContext</code> s within
   * it, and a handle to the <code>SchedulingContext</code> that the shell will use in its own
   * operations with the <code>JobStore</code>.
   */
  @Override
  public void initialize(Scheduler scheduler) throws SchedulerConfigException {

    this.scheduler = scheduler;
  }

  /**
   * Called by the <class>{@link org.quartz.core.QuartzSchedulerThread} </code> to obtain instances
   * of <code>
   * {@link org.quartz.core.JobRunShell}</code>.
   */
  @Override
  public JobRunShell createJobRunShell(TriggerFiredBundle bundle) throws SchedulerException {

    return new JobRunShell(scheduler, bundle);
  }
}
