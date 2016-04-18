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
