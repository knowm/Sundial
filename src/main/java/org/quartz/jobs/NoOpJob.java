package org.quartz.jobs;

import org.quartz.core.JobExecutionContext;
import org.quartz.exceptions.JobExecutionException;

/**
 * <p>
 * An implementation of Job, that does absolutely nothing - useful for system which only wish to use
 * <code>{@link org.quartz.listeners.TriggerListener}s</code> and <code>{@link org.quartz.listeners.JobListener}s</code>, rather than writing Jobs
 * that perform work.
 * </p>
 * 
 * @author James House
 */
public class NoOpJob implements Job {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Constructors.~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  public NoOpJob() {

  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Interface.~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Do nothing.
   * </p>
   */
  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {

  }

}