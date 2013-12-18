/* 
 * Copyright 2001-2009 Terracotta, Inc. 
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

import java.io.Serializable;

import org.quartz.utils.StringKeyDirtyFlagMap;

/**
 * Holds state information for <code>Job</code> instances.
 * <p>
 * <code>JobDataMap</code> instances are stored once when the <code>Job</code> is added to a scheduler. They are also re-persisted after every execution of <code>StatefulJob</code> instances.
 * </p>
 * <p>
 * <code>JobDataMap</code> instances can also be stored with a <code>Trigger</code>. This can be useful in the case where you have a Job that is stored in the scheduler for regular/repeated use by
 * multiple Triggers, yet with each independent triggering, you want to supply the Job with different data inputs.
 * </p>
 * <p>
 * The <code>JobExecutionContext</code> passed to a Job at execution time also contains a convenience <code>JobDataMap</code> that is the result of merging the contents of the trigger's JobDataMap (if
 * any) over the Job's JobDataMap (if any).
 * </p>
 * 
 * @see Job
 * @see StatefulJob
 * @see Trigger
 * @see JobExecutionContext
 * @author James House
 */
public class JobDataMap extends StringKeyDirtyFlagMap implements Serializable {

  private static final long serialVersionUID = -6939901990106713909L;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Create an empty <code>JobDataMap</code>.
   * </p>
   */
  public JobDataMap() {

    super(15);
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

}
