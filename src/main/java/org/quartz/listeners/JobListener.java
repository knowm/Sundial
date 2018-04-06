package org.quartz.listeners;

import org.quartz.core.JobExecutionContext;
import org.quartz.core.Scheduler;
import org.quartz.exceptions.JobExecutionException;
import org.quartz.triggers.Trigger;

/**
 * The interface to be implemented by classes that want to be informed when a <code>
 * {@link org.quartz.jobs.JobDetail}</code> executes. In general, applications that use a <code>
 * Scheduler</code> will not have use for this mechanism.
 *
 * @author James House
 */
public interface JobListener {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /** Get the name of the <code>JobListener</code>. */
  String getName();

  /**
   * Called by the <code>{@link Scheduler}</code> when a <code>{@link org.quartz.jobs.JobDetail}
   * </code> is about to be executed (an associated <code>{@link Trigger}</code> has occurred).
   *
   * <p>This method will not be invoked if the execution of the Job was vetoed by a <code>
   * {@link TriggerListener}</code>.
   *
   * @see #jobExecutionVetoed(JobExecutionContext)
   */
  void jobToBeExecuted(JobExecutionContext context);

  /**
   * Called by the <code>{@link Scheduler}</code> when a <code>{@link org.quartz.jobs.JobDetail}
   * </code> was about to be executed (an associated <code>{@link Trigger}</code> has occurred), but
   * a <code>{@link TriggerListener}</code> vetoed it's execution.
   *
   * @see #jobToBeExecuted(JobExecutionContext)
   */
  void jobExecutionVetoed(JobExecutionContext context);

  /**
   * Called by the <code>{@link Scheduler}</code> after a <code>{@link org.quartz.jobs.JobDetail}
   * </code> has been executed, and be for the associated <code>Trigger</code>'s <code>triggered(xx)
   * </code> method has been called.
   */
  void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException);
}
