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
package org.knowm.sundial.jobs;

import org.knowm.sundial.Job;
import org.knowm.sundial.annotations.CronTrigger;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CronTrigger(cron = "0/5 * * * * ?", jobDataMap = { "KEY_1:VALUE_1", "KEY_2:1000" })
public class SampleJob4 extends Job {

  private final Logger logger = LoggerFactory.getLogger(SampleJob4.class);

  @Override
  public void doRun() throws JobInterruptException {

    logger.info("Running SampleJob4.");

    // Do something interesting...

    String value1 = getJobContext().get("KEY_1");
    logger.info("value1 = " + value1);

    String value2AsString = getJobContext().get("KEY_2");
    Integer valueAsInt = Integer.valueOf(value2AsString);
    logger.info("value2 = " + valueAsInt);

  }
}
