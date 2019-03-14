package org.knowm.sundial.exceptions;

public class SchedulerStartupException extends RuntimeException {

  /** Constructor */
  public SchedulerStartupException(Throwable e) {

    super("Error starting scheduler!!!", e);
  }
}
