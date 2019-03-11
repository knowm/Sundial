package org.knowm.sundial.jobs;

import org.knowm.sundial.Job;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleJob8 extends Job {

  private final Logger logger = LoggerFactory.getLogger(SampleJob8.class);

  @Override
  public void doRun() throws JobInterruptException {

    logger.info("Running SampleJob8.");

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
