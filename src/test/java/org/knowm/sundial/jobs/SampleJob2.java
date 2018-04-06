package org.knowm.sundial.jobs;

import org.knowm.sundial.Job;
import org.knowm.sundial.JobContext;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
