package org.knowm.sundial.jobs;

import org.knowm.sundial.Job;
import org.knowm.sundial.annotations.CronTrigger;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CronTrigger(cron = "0/20 * * * * ?")
public class SampleJob5 extends Job {

  private final Logger logger = LoggerFactory.getLogger(SampleJob5.class);

  @Override
  public void doRun() throws JobInterruptException {

    logger.info("Running SampleJob5.");

    // Do something interesting...

  }
}
