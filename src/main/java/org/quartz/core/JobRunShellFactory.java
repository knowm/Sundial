package org.quartz.core;

import org.quartz.QuartzScheduler;
import org.quartz.exceptions.SchedulerConfigException;
import org.quartz.exceptions.SchedulerException;

/**
 * Responsible for creating the instances of <code>{@link JobRunShell}</code> to be used within the
 * <class>{@link QuartzScheduler}</code> instance.
 *
 * @author James House
 */
public interface JobRunShellFactory {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * Initialize the factory, providing a handle to the <code>Scheduler</code> that should be made
   * available within the <code>JobRunShell</code> and the <code>JobExecutionContext</code> s within
   * it.
   */
  void initialize(Scheduler scheduler) throws SchedulerConfigException;

  /**
   * Called by the <code>{@link org.quartz.core.QuartzSchedulerThread}</code> to obtain instances of
   * <code>{@link JobRunShell}</code>.
   */
  JobRunShell createJobRunShell(TriggerFiredBundle bundle) throws SchedulerException;
}
