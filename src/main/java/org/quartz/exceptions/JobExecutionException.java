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
package org.quartz.exceptions;

import org.quartz.core.JobExecutionContext;
import org.quartz.core.Scheduler;
import org.quartz.jobs.Job;

/**
 * An exception that can be thrown by a <code>{@link org.quartz.jobs.Job}</code> to indicate to the Quartz <code>{@link Scheduler}</code> that an
 * error occurred while executing, and whether or not the <code>Job</code> requests to be re-fired immediately (using the same
 * <code>{@link JobExecutionContext}</code>, or whether it wants to be unscheduled.
 * <p>
 * Note that if the flag for 'refire immediately' is set, the flags for unscheduling the Job are ignored.
 * </p>
 * 
 * @see Job
 * @see JobExecutionContext
 * @see SchedulerException
 * @author James House
 */
public class JobExecutionException extends SchedulerException {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private boolean refire = false;

  private boolean unscheduleTrigg = false;

  private boolean unscheduleAllTriggs = false;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Create a JobExcecutionException, with the 're-fire immediately' flag set to <code>false</code>.
   * </p>
   */
  public JobExecutionException() {

  }

  /**
   * <p>
   * Create a JobExcecutionException with the given underlying exception, and the 're-fire immediately' flag set to the given value.
   * </p>
   */
  public JobExecutionException(Throwable cause, boolean refireImmediately) {

    super(cause);

    refire = refireImmediately;
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  public void setRefireImmediately(boolean refire) {

    this.refire = refire;
  }

  public boolean refireImmediately() {

    return refire;
  }

  public void setUnscheduleFiringTrigger(boolean unscheduleTrigg) {

    this.unscheduleTrigg = unscheduleTrigg;
  }

  public boolean unscheduleFiringTrigger() {

    return unscheduleTrigg;
  }

  public void setUnscheduleAllTriggers(boolean unscheduleAllTriggs) {

    this.unscheduleAllTriggs = unscheduleAllTriggs;
  }

  public boolean unscheduleAllTriggers() {

    return unscheduleAllTriggs;
  }

}
