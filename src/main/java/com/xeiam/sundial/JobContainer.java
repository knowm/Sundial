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
package com.xeiam.sundial;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xeiam.sundial.exceptions.JobInterruptException;

/**
 * The highest class of the Job hierarchy which contains the ThreadLocal instance, the JobContext, the logging methods, and handles terminating of Jobs.
 * 
 * @author timmolter
 */
public abstract class JobContainer {

  /** ThreadLocal container */
  private static ThreadLocal<JobContext> sContextContainer = new ThreadLocal<JobContext>();

  /** slf4J logger wrapper */
  Logger logger = LoggerFactory.getLogger(JobContainer.class);

  /** terminate flag */
  private boolean mTerminate = false;

  /**
   * Initialize the ThreadLocal with a JobExecutionContext object
   * 
   * @param pJobContext
   */
  protected void initContextContainer(JobExecutionContext jobExecutionContext) {

    JobContext lJobContext = new JobContext();
    lJobContext.addQuartzContext(jobExecutionContext);
    sContextContainer.set(lJobContext);
  }

  /**
   * Empty the ThreadLocal container
   */
  protected void destroyContext() {

    sContextContainer.remove();
  }

  /**
   * Get the JobContext object
   * 
   * @return
   */
  protected JobContext getJobContext() {

    return JobContainer.getContext();
  }

  /**
   * Get the JobContext object
   * 
   * @return
   */
  private static JobContext getContext() {

    return sContextContainer.get();
  }

  /**
   * Check if the terminate flag is true, and throw a JobInterruptException if it is.
   */
  public void checkTerminated() {

    if (mTerminate) {
      throw new JobInterruptException();
    }
  }

  /**
   * Set the terminate flag to true
   */
  public void setTerminate() {

    mTerminate = true;
  }

}
