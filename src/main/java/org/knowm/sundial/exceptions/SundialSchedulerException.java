package org.knowm.sundial.exceptions;

import org.knowm.sundial.SundialJobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
