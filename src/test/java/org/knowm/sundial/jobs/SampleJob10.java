package org.knowm.sundial.jobs;

import org.knowm.sundial.InterruptingJob;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demonstrates InterruptingJob: a job that blocks on Thread.sleep() and can be immediately
 * interrupted via SundialJobScheduler.stopJob("SampleJob10").
 *
 * <p>Unlike the checkTerminated() approach, there is no need to poll — the blocking call itself
 * is unblocked by Thread.interrupt() when the job is stopped.
 */
public class SampleJob10 extends InterruptingJob {

  private final Logger logger = LoggerFactory.getLogger(SampleJob10.class);

  @Override
  public void doRun() throws JobInterruptException {

    logger.info("SampleJob10 starting — will sleep for 60 seconds.");

    try {
      Thread.sleep(60_000);
      logger.info("SampleJob10 completed normally.");
    } catch (InterruptedException e) {
      // Re-interrupt so callers can observe the interrupt status
      Thread.currentThread().interrupt();
      logger.info("SampleJob10 was interrupted before sleep completed.");
    }
  }
}
