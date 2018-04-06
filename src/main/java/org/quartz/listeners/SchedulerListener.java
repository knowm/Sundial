package org.quartz.listeners;

import org.quartz.core.Scheduler;
import org.quartz.exceptions.SchedulerException;
import org.quartz.jobs.JobDetail;
import org.quartz.triggers.Trigger;

/**
 * The interface to be implemented by classes that want to be informed of major <code>
 * {@link Scheduler}</code> events.
 *
 * @author James House
 */
public interface SchedulerListener {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * Called by the <code>{@link Scheduler}</code> when a <code>{@link org.quartz.jobs.JobDetail}
   * </code> is scheduled.
   */
  void jobScheduled(Trigger trigger);

  /**
   * Called by the <code>{@link Scheduler}</code> when a <code>{@link org.quartz.jobs.JobDetail}
   * </code> is unscheduled.
   *
   * @see SchedulerListener#schedulingDataCleared()
   */
  void jobUnscheduled(String triggerKey);

  /**
   * Called by the <code>{@link Scheduler}</code> when a <code>{@link Trigger}</code> has reached
   * the condition in which it will never fire again.
   */
  void triggerFinalized(Trigger trigger);

  /**
   * Called by the <code>{@link Scheduler}</code> when a <code>{@link org.quartz.jobs.JobDetail}
   * </code> has been added.
   */
  void jobAdded(JobDetail jobDetail);

  /**
   * Called by the <code>{@link Scheduler}</code> when a <code>{@link org.quartz.jobs.JobDetail}
   * </code> has been deleted.
   */
  void jobDeleted(String jobKey);

  /**
   * Called by the <code>{@link Scheduler}</code> when a serious error has occurred within the
   * scheduler - such as repeated failures in the <code>JobStore</code>, or the inability to
   * instantiate a <code>Job</code> instance when its <code>Trigger</code> has fired.
   *
   * <p>The <code>getErrorCode()</code> method of the given SchedulerException can be used to
   * determine more specific information about the type of error that was encountered.
   */
  void schedulerError(String msg, SchedulerException cause);

  /**
   * Called by the <code>{@link Scheduler}</code> to inform the listener that it has move to standby
   * mode.
   */
  void schedulerInStandbyMode();

  /** Called by the <code>{@link Scheduler}</code> to inform the listener that it has started. */
  void schedulerStarted();

  /** Called by the <code>{@link Scheduler}</code> to inform the listener that it has shutdown. */
  void schedulerShutdown();

  /**
   * Called by the <code>{@link Scheduler}</code> to inform the listener that it has begun the
   * shutdown sequence.
   */
  void schedulerShuttingdown();

  /**
   * Called by the <code>{@link Scheduler}</code> to inform the listener that all jobs, triggers and
   * calendars were deleted.
   */
  void schedulingDataCleared();
}
