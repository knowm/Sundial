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
package org.knowm.sundial;

import org.knowm.sundial.exceptions.JobInterruptException;
import org.knowm.sundial.exceptions.RequiredParameterException;
import org.quartz.core.JobExecutionContext;
import org.quartz.exceptions.JobExecutionException;
import org.quartz.exceptions.UnableToInterruptJobException;
import org.quartz.jobs.InterruptableJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author timmolter
 */
public abstract class Job extends JobContainer implements InterruptableJob {

  private final Logger logger = LoggerFactory.getLogger(Job.class);

  /**
   * Required no-arg constructor
   */
  public Job() {

    // this is a comment.
  }

  @Override
  public final void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

    // check for global lock
    if (SundialJobScheduler.getGlobalLock()) {
      logger.info("Global Lock in place! Job aborted.");
      return;
    }

    try {

      initContextContainer(jobExecutionContext);

      doRun();

    } catch (RequiredParameterException e) {
    } catch (JobInterruptException e) {
    } catch (Exception e) {
      logger.error("Error executing Job! Job aborted!!!", e);
    } finally {
      cleanup();
      destroyContext(); // remove the JobContext from the ThreadLocal
    }

  }

  @Override
  public void interrupt() throws UnableToInterruptJobException {

    setTerminate();
    logger.info("Interrupt called!");

  }

  /**
   * Override and place any code in here that should be called no matter what after the Job runs or throws an exception.
   */
  public void cleanup() {

  }

  public abstract void doRun() throws JobInterruptException;

}
