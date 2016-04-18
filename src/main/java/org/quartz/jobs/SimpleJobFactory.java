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

import org.quartz.core.Scheduler;
import org.quartz.core.TriggerFiredBundle;
import org.quartz.exceptions.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default JobFactory used by Quartz - simply calls <code>newInstance()</code> on the job class.
 *
 * @see JobFactory
 * @see PropertySettingJobFactory
 * @author jhouse
 */
public class SimpleJobFactory implements JobFactory {

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public Job newJob(TriggerFiredBundle bundle, Scheduler Scheduler) throws SchedulerException {

    JobDetail jobDetail = bundle.getJobDetail();
    Class<? extends Job> jobClass = jobDetail.getJobClass();
    try {
      if (log.isDebugEnabled()) {
        log.debug("Producing instance of Job '" + jobDetail.getName() + "', class=" + jobClass.getName());
      }

      return jobClass.newInstance();
    } catch (Exception e) {
      SchedulerException se = new SchedulerException("Problem instantiating class '" + jobDetail.getJobClass().getName() + "'", e);
      throw se;
    }
  }

}
