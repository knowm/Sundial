package org.quartz.listeners;

import java.util.List;

/**
 * Client programs may be interested in the 'listener' interfaces that are available from Quartz. The <code>{@link JobListener}</code> interface
 * provides notifications of <code>Job</code> executions. The <code>{@link TriggerListener}</code> interface provides notifications of
 * <code>Trigger</code> firings. The <code>{@link SchedulerListener}</code> interface provides notifications of <code>Scheduler</code> events and
 * errors. Listeners can be associated with local schedulers through the {@link ListenerManager} interface.
 *
 * @author jhouse
 * @since 2.0 - previously listeners were managed directly on the Scheduler interface.
 */
public interface ListenerManager {

  /**
   * Get a List containing all of the <code>{@link JobListener}</code>s in the <code>Scheduler</code>.
   */
  public List<JobListener> getJobListeners();

  /**
   * Add the given <code>{@link TriggerListener}</code> to the <code>Scheduler</code>, and register it to receive events for Triggers
   */
  public void addTriggerListener(TriggerListener triggerListener);

  /**
   * Get a List containing all of the <code>{@link TriggerListener}</code>s in the <code>Scheduler</code>.
   */
  public List<TriggerListener> getTriggerListeners();

  /**
   * Get a List containing all of the <code>{@link SchedulerListener}</code>s registered with the <code>Scheduler</code>.
   */
  public List<SchedulerListener> getSchedulerListeners();

}