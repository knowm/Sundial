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
import org.quartz.exceptions.SchedulerConfigException;
import org.quartz.exceptions.SchedulerException;

/**
 * <p>
 * Responsible for creating the instances of <code>{@link JobRunShell}</code> to be used within the <class>{@link QuartzScheduler}</code> instance.
 * </p>
 * 
 * @author James House
 */
public interface JobRunShellFactory {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Initialize the factory, providing a handle to the <code>Scheduler</code> that should be made available within the <code>JobRunShell</code> and
   * the <code>JobExecutionContext</code> s within it.
   * </p>
   */
  void initialize(Scheduler scheduler) throws SchedulerConfigException;

  /**
   * <p>
   * Called by the <code>{@link org.quartz.core.QuartzSchedulerThread}</code> to obtain instances of <code>{@link JobRunShell}</code>.
   * </p>
   */
  JobRunShell createJobRunShell(TriggerFiredBundle bundle) throws SchedulerException;
}