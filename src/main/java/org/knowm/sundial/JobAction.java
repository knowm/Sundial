/**
 * Copyright 2011 - 2013 Xeiam LLC.
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
package org.knowm.sundial;

/**
 * A JobAction encapsulates business logic that can be reused in more than one place. Extenders of JobAction have access to the JobContext and Job
 * logging functions.
 * 
 * @author timothy.molter
 */
public abstract class JobAction extends JobContainer {

  /**
   * Call this method to start the Action
   */
  public void run() {

    doRun();
    cleanup();
  }

  /**
   * Override and place any code in here that should be called no matter what after the Job runs or throws an exception.
   */
  public void cleanup() {

  }

  /**
   * Implement this method. Don't not call it directly.
   */
  public abstract void doRun();

}
