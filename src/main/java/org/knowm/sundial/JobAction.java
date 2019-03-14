package org.knowm.sundial;

/**
 * A JobAction encapsulates business logic that can be reused in more than one place. Extenders of
 * JobAction have access to the JobContext and Job logging functions.
 *
 * @author timothy.molter
 */
public abstract class JobAction extends JobContainer {

  /** Call this method to start the Action */
  public void run() {

    doRun();
    cleanup();
  }

  /**
   * Override and place any code in here that should be called no matter what after the Job runs or
   * throws an exception.
   */
  public void cleanup() {}

  /** Implement this method. Don't not call it directly. */
  public abstract void doRun();
}
