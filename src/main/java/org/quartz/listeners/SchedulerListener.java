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
package org.quartz.listeners;

import org.quartz.core.Scheduler;
import org.quartz.exceptions.SchedulerException;
import org.quartz.jobs.JobDetail;
import org.quartz.triggers.Trigger;

/**
 * The interface to be implemented by classes that want to be informed of major <code>{@link Scheduler}</code> events.
 *
 * @author James House
 */
public interface SchedulerListener {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a <code>{@link org.quartz.jobs.JobDetail}</code> is scheduled.
   * </p>
   */
  void jobScheduled(Trigger trigger);

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a <code>{@link org.quartz.jobs.JobDetail}</code> is unscheduled.
   * </p>
   *
   * @see SchedulerListener#schedulingDataCleared()
   */
  void jobUnscheduled(String triggerKey);

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a <code>{@link Trigger}</code> has reached the condition in which it will never fire again.
   * </p>
   */
  void triggerFinalized(Trigger trigger);

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a <code>{@link org.quartz.jobs.JobDetail}</code> has been added.
   * </p>
   */
  void jobAdded(JobDetail jobDetail);

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a <code>{@link org.quartz.jobs.JobDetail}</code> has been deleted.
   * </p>
   */
  void jobDeleted(String jobKey);

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a serious error has occurred within the scheduler - such as repeated failures in the
   * <code>JobStore</code>, or the inability to instantiate a <code>Job</code> instance when its <code>Trigger</code> has fired.
   * </p>
   * <p>
   * The <code>getErrorCode()</code> method of the given SchedulerException can be used to determine more specific information about the type of error
   * that was encountered.
   * </p>
   */
  void schedulerError(String msg, SchedulerException cause);

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> to inform the listener that it has move to standby mode.
   * </p>
   */
  void schedulerInStandbyMode();

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> to inform the listener that it has started.
   * </p>
   */
  void schedulerStarted();

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> to inform the listener that it has shutdown.
   * </p>
   */
  void schedulerShutdown();

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> to inform the listener that it has begun the shutdown sequence.
   * </p>
   */
  void schedulerShuttingdown();

  /**
   * Called by the <code>{@link Scheduler}</code> to inform the listener that all jobs, triggers and calendars were deleted.
   */
  void schedulingDataCleared();
}
