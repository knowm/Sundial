package org.knowm.sundial;

import org.knowm.sundial.exceptions.JobInterruptException;
import org.knowm.sundial.exceptions.RequiredParameterException;
import org.quartz.core.JobExecutionContext;
import org.quartz.exceptions.JobExecutionException;
import org.quartz.jobs.InterruptableJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author timmolter */
public abstract class Job extends JobContainer implements InterruptableJob {

  private final Logger logger = LoggerFactory.getLogger(Job.class);

  /** Required no-arg constructor */
  public Job() {}

  @Override
  public final void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

    // check for global lock
    if (SundialJobScheduler.getGlobalLock()) {
      logger.info("Global Lock in place! Job aborted.");
      return;
    }

    try {

      initContextContainer(jobExecutionContext);

      doRun();

    } catch (RequiredParameterException e) {
    } catch (JobInterruptException e) {
    } catch (Exception e) {
      logger.error(
          String.format(
              "Job [%s] aborted due to execution error: %s",
              jobExecutionContext.getJobDetail().getName(), e.getMessage()),
          e);
    } finally {
      cleanup();
      destroyContext(); // remove the JobContext from the ThreadLocal
    }
  }

  @Override
  public void interrupt() {

    logger.debug("Interrupt called!");

    setTerminate();
  }

  /**
   * Override and place any code in here that should be called no matter what after the Job runs or
   * throws an exception.
   */
  public void cleanup() {}

  public abstract void doRun() throws JobInterruptException;
}
