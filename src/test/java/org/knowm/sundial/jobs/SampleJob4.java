package org.knowm.sundial.jobs;

import org.knowm.sundial.Job;
import org.knowm.sundial.annotations.CronTrigger;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CronTrigger(
    cron = "0/5 * * * * ?",
    jobDataMap = {"KEY_1:VALUE_1", "KEY_2:1000"})
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
