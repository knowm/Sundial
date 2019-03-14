package org.knowm.sundial.jobs;

import org.knowm.sundial.Job;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleJob1 extends Job {

  private final Logger logger = LoggerFactory.getLogger(SampleJob1.class);

  @Override
  public void doRun() throws JobInterruptException {

    logger.info("Running SampleJob1.");

    // Do something interesting...

  }
}
