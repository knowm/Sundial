package com.xeiam.sundial.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xeiam.sundial.SundialJobScheduler;

/**
 * A RuntimeException that wraps some checked Exceptions in the SundialScheduler class.
 *
 * @author timmolter
 */
public class SundialSchedulerException extends RuntimeException {

  /** slf4J logger wrapper */
  static Logger logger = LoggerFactory.getLogger(SundialJobScheduler.class);

  /**
   * Constructor
   *
   * @param msg
   * @param cause
   */
  public SundialSchedulerException(String msg, Throwable cause) {

    super(msg, cause);
    logger.error(msg, cause);
  }

}
