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
package com.xeiam.sundial.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xeiam.sundial.Job;
import com.xeiam.sundial.annotations.CronTrigger;
import com.xeiam.sundial.exceptions.JobInterruptException;

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
