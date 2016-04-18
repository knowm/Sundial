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

import java.io.Serializable;

import org.quartz.builders.JobBuilder;
import org.quartz.core.Scheduler;

/**
 * Conveys the detail properties of a given <code>Job</code> instance. JobDetails are to be created/defined with {@link JobBuilder}.
 * <p>
 * Quartz does not store an actual instance of a <code>Job</code> class, but instead allows you to define an instance of one, through the use of a
 * <code>JobDetail</code>.
 * </p>
 * <p>
 * <code>Job</code>s have a name associated with them, which should uniquely identify them within a single <code>{@link Scheduler}</code>.
 * </p>
 * <p>
 * <code>Trigger</code>s are the 'mechanism' by which <code>Job</code>s are scheduled. Many <code>Trigger</code>s can point to the same
 * <code>Job</code>, but a single <code>Trigger</code> can only point to one <code>Job</code>.
 * </p>
 *
 * @author James House
 */
public interface JobDetail extends Serializable, Cloneable {

  public String getName();

  /**
   * <p>
   * Return the description given to the <code>Job</code> instance by its creator (if any).
   * </p>
   *
   * @return null if no description was set.
   */
  public String getDescription();

  /**
   * <p>
   * Get the instance of <code>Job</code> that will be executed.
   * </p>
   */
  public Class<? extends Job> getJobClass();

  /**
   * <p>
   * Get the <code>JobDataMap</code> that is associated with the <code>Job</code>.
   * </p>
   */
  public JobDataMap getJobDataMap();

  /**
   * The default behavior is to veto any job is currently running concurrent. However, concurrent jobs can be created by setting the 'Concurrency' to
   * true
   *
   * @return
   */
  public boolean isConcurrencyAllowed();

  public Object clone();

  /**
   * Get a {@link JobBuilder} that is configured to produce a <code>JobDetail</code> identical to this one.
   */
  public JobBuilder getJobBuilder();

}