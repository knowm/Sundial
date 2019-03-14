package org.knowm.sundial.exceptions;

/**
 * This Exception is only used by the Job class to abort a running Job. Do not use this elsewhere.
 */
public class JobInterruptException extends RuntimeException {

  /**
   * Constructor
   *
   * @param message
   */
  private JobInterruptException(String message) {

    super(message);
  }

  /** Constructor */
  public JobInterruptException() {

    this("Job Interrupted!!!");
  }
}
