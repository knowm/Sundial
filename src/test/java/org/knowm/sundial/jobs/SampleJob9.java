package org.knowm.sundial.jobs;

import org.knowm.sundial.Job;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleJob9 extends Job {

  private final Logger logger = LoggerFactory.getLogger(SampleJob9.class);

  @Override
  public void doRun() throws JobInterruptException {

    logger.info("Running SampleJob9.");

    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    do {
      checkTerminated();
    } while (true);
  }

  //  @Override
  //  public void cleanup() {
  //    System.out.println("CLEAN");
  //  }
}
