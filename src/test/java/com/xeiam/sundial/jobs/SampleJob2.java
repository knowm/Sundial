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
import com.xeiam.sundial.JobContext;
import com.xeiam.sundial.exceptions.JobInterruptException;

public class SampleJob2 extends Job {

  private final Logger logger = LoggerFactory.getLogger(SampleJob2.class);

  @Override
  public void doRun() throws JobInterruptException {

    logger.info("Running SampleJob2.");

    JobContext context = getJobContext();

    String valueAsString = context.get("MyParam");
    logger.info("valueAsString = " + valueAsString);

    Integer valueAsInt = Integer.valueOf(valueAsString);
    logger.info("valueAsInt = " + valueAsInt);

  }
}
