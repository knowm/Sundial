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
package org.quartz.jobs;

import org.quartz.core.JobExecutionContext;
import org.quartz.core.Scheduler;
import org.quartz.exceptions.JobExecutionException;
import org.quartz.listeners.JobListener;
import org.quartz.listeners.TriggerListener;
import org.quartz.triggers.Trigger;

/**
 * <p>
 * The interface to be implemented by classes which represent a 'job' to be performed.
 * </p>
 * <p>
 * Instances of <code>Job</code> must have a <code>public</code> no-argument constructor.
 * </p>
 * <p>
 * <code>JobDataMap</code> provides a mechanism for 'instance member data' that may be required by some implementations of this interface.
 * </p>
 *
 * @author James House
 */
public interface Job {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a <code>{@link Trigger}</code> fires that is associated with the <code>Job</code>.
   * </p>
   * <p>
   * The implementation may wish to set a {@link JobExecutionContext#setResult(Object) result} object on the {@link JobExecutionContext} before this
   * method exits. The result itself is meaningless to Quartz, but may be informative to <code>{@link JobListener}s</code> or
   * <code>{@link TriggerListener}s</code> that are watching the job's execution.
   * </p>
   *
   * @throws JobExecutionException if there is an exception while executing the job.
   */
  void execute(JobExecutionContext context) throws JobExecutionException;

}
