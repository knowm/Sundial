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

import org.quartz.core.JobExecutionContext;
import org.quartz.core.Scheduler;
import org.quartz.exceptions.JobExecutionException;
import org.quartz.triggers.Trigger;

/**
 * The interface to be implemented by classes that want to be informed when a <code>{@link org.quartz.jobs.JobDetail}</code> executes. In general,
 * applications that use a <code>Scheduler</code> will not have use for this mechanism.
 *
 * @author James House
 */
public interface JobListener {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Get the name of the <code>JobListener</code>.
   * </p>
   */
  String getName();

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a <code>{@link org.quartz.jobs.JobDetail}</code> is about to be executed (an associated
   * <code>{@link Trigger}</code> has occurred).
   * </p>
   * <p>
   * This method will not be invoked if the execution of the Job was vetoed by a <code>{@link TriggerListener}</code>.
   * </p>
   *
   * @see #jobExecutionVetoed(JobExecutionContext)
   */
  void jobToBeExecuted(JobExecutionContext context);

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a <code>{@link org.quartz.jobs.JobDetail}</code> was about to be executed (an associated
   * <code>{@link Trigger}</code> has occurred), but a <code>{@link TriggerListener}</code> vetoed it's execution.
   * </p>
   *
   * @see #jobToBeExecuted(JobExecutionContext)
   */
  void jobExecutionVetoed(JobExecutionContext context);

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> after a <code>{@link org.quartz.jobs.JobDetail}</code> has been executed, and be for the associated
   * <code>Trigger</code>'s <code>triggered(xx)</code> method has been called.
   * </p>
   */
  void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException);

}
