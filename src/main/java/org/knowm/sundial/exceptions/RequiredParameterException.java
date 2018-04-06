package org.knowm.sundial.exceptions;

public class RequiredParameterException extends RuntimeException {

  /**
   * Constructor
   *
   * @param message
   */
  private RequiredParameterException(String message) {

    super(message);
  }

  /**
   * Constructor
   */
  public RequiredParameterException() {

    this("Required Value not found in Context! Job aborted!!!");
  }

}
