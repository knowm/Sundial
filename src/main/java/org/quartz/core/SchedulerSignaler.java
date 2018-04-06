package org.quartz.core;

import org.quartz.triggers.Trigger;

/**
 * An interface to be used by <code>JobStore</code> instances in order to communicate signals back to the <code>QuartzScheduler</code>.
 *
 * @author jhouse
 */
public interface SchedulerSignaler {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  void notifyTriggerListenersMisfired(Trigger trigger);

  void notifySchedulerListenersFinalized(Trigger trigger);

  void notifySchedulerListenersJobDeleted(String jobKey);

  void signalSchedulingChange(long candidateNewNextFireTime);
}
