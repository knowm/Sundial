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