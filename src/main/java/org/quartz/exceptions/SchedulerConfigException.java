package org.quartz.exceptions;

/**
 * An exception that is thrown to indicate that there is a misconfiguration of the <code>
 * SchedulerFactory</code>- or one of the components it configures.
 *
 * @author James House
 */
public class SchedulerConfigException extends SchedulerException {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /** Create a <code>JobPersistenceException</code> with the given message. */
  public SchedulerConfigException(String msg) {

    super(msg);
  }
}
