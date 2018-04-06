package org.knowm.sundial.jobs;

import org.knowm.sundial.Job;
import org.knowm.sundial.annotations.SimpleTrigger;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SimpleTrigger(repeatInterval = 1000, repeatCount = 10)
public class SampleJob6 extends Job {

  private final Logger logger = LoggerFactory.getLogger(SampleJob6.class);

  @Override
  public void doRun() throws JobInterruptException {

    logger.info("Running SampleJob6.");

    // Do something interesting...
  }
}
