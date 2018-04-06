package org.quartz.core;

import org.quartz.QuartzScheduler;
import org.quartz.exceptions.SchedulerException;
import org.quartz.triggers.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An interface to be used by <code>JobStore</code> instances in order to communicate signals back to the <code>QuartzScheduler</code>.
 *
 * @author jhouse
 */
public class SchedulerSignalerImpl implements SchedulerSignaler {

  private Logger logger = LoggerFactory.getLogger(SchedulerSignalerImpl.class);

  private QuartzScheduler quartzScheduler;
  private QuartzSchedulerThread quartzSchedulerThread;

  /**
   * Constructor
   *
   * @param quartzScheduler
   * @param quartzSchedulerThread
   */
  public SchedulerSignalerImpl(QuartzScheduler quartzScheduler, QuartzSchedulerThread quartzSchedulerThread) {

    this.quartzScheduler = quartzScheduler;
    this.quartzSchedulerThread = quartzSchedulerThread;

    logger.info("Initialized Scheduler Signaler of type: " + getClass());
  }

  @Override
  public void notifyTriggerListenersMisfired(Trigger trigger) {

    try {
      quartzScheduler.notifyTriggerListenersMisfired(trigger);
    } catch (SchedulerException se) {
      logger.error("Error notifying listeners of trigger misfire.", se);
      quartzScheduler.notifySchedulerListenersError("Error notifying listeners of trigger misfire.", se);
    }
  }

  @Override
  public void notifySchedulerListenersFinalized(Trigger trigger) {

    quartzScheduler.notifySchedulerListenersFinalized(trigger);
  }

  @Override
  public void signalSchedulingChange(long candidateNewNextFireTime) {

    quartzSchedulerThread.signalSchedulingChange(candidateNewNextFireTime);
  }

  @Override
  public void notifySchedulerListenersJobDeleted(String jobKey) {

    quartzScheduler.notifySchedulerListenersJobDeleted(jobKey);
  }
}
