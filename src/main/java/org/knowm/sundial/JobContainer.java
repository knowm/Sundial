package org.knowm.sundial;

import org.knowm.sundial.exceptions.JobInterruptException;
import org.quartz.core.JobExecutionContext;

/**
 * The highest class of the Job hierarchy which contains the ThreadLocal instance, the JobContext,
 * and handles terminating of Jobs.
 *
 * @author timmolter
 */
public abstract class JobContainer {

  /** ThreadLocal container */
  private static ThreadLocal<JobContext> contextContainer = new ThreadLocal<JobContext>();

  /** terminate flag */
  private boolean terminate = false;

  /**
   * Initialize the ThreadLocal with a JobExecutionContext object
   *
   * @param jobExecutionContext
   */
  protected void initContextContainer(JobExecutionContext jobExecutionContext) {

    JobContext jobContext = new JobContext();
    jobContext.addQuartzContext(jobExecutionContext);
    contextContainer.set(jobContext);
  }

  /** Empty the ThreadLocal container */
  protected void destroyContext() {

    contextContainer.remove();
  }

  /**
   * Get the JobContext object
   *
   * @return
   */
  protected JobContext getJobContext() {

    return JobContainer.getContext();
  }

  /**
   * Get the JobContext object
   *
   * @return
   */
  private static JobContext getContext() {

    return contextContainer.get();
  }

  /** Check if the terminate flag is true, and throw a JobInterruptException if it is. */
  public synchronized void checkTerminated() {

    if (terminate) {
      throw new JobInterruptException();
    }
  }

  /** Set the terminate flag to true. Client code should not call this. */
  protected void setTerminate() {

    terminate = true;
  }
}
