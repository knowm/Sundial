package org.knowm.sundial.jobs;

import org.knowm.sundial.Job;
import org.knowm.sundial.JobContext;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleJob3 extends Job {

  private final Logger logger = LoggerFactory.getLogger(SampleJob3.class);

  @Override
  public void doRun() throws JobInterruptException {

    logger.info("Running SampleJob3.");

    JobContext context = getJobContext();

    context.put("MyValue", new Integer(123));

    new SampleJobAction().run();

  }
}
