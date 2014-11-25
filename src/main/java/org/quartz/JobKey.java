/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */

package org.quartz;

import org.quartz.utils.Key;

/**
 * Uniquely identifies a {@link JobDetail}.
 * <p>
 * Keys are composed of both a name and group, and the name must be unique within the group. If only a group is specified then the default group name will be used.
 * </p>
 *
 * @see Job
 * @see Key#DEFAULT_GROUP
 */
public final class JobKey extends Key<JobKey> {

  /**
   * Constructor
   *
   * @param name
   */
  public JobKey(String name) {

    super(name, null);
  }

  /**
   * Constructor
   *
   * @param name
   * @param group
   */
  public JobKey(String name, String group) {

    super(name, group);
  }

}
