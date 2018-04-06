package org.knowm.sundial.jobs;

import java.util.concurrent.TimeUnit;
import org.knowm.sundial.Job;
import org.knowm.sundial.annotations.SimpleTrigger;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SimpleTrigger(repeatInterval = 30, timeUnit = TimeUnit.SECONDS, isConcurrencyAllowed = true)
public class SampleJob7 extends Job {

  private final Logger logger = LoggerFactory.getLogger(SampleJob7.class);

  @Override
  public void doRun() throws JobInterruptException {

    logger.info("Running SampleJob7.");

    // Do something interesting...
  }
}
