package org.quartz.exceptions;

/**
 * An exception that is thrown to indicate that there has been a failure in the scheduler's
 * underlying persistence mechanism.
 *
 * @author James House
 */
public class JobPersistenceException extends SchedulerException {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /** Create a <code>JobPersistenceException</code> with the given message. */
  public JobPersistenceException(String msg) {

    super(msg);
  }
}
