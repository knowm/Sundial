package org.quartz.exceptions;

import org.quartz.core.Scheduler;

/**
 * Base class for exceptions thrown by the Quartz <code>{@link Scheduler}</code>.
 *
 * <p><code>SchedulerException</code>s may contain a reference to another <code>Exception</code>,
 * which was the underlying cause of the <code>SchedulerException</code>.
 *
 * @author James House
 */
public class SchedulerException extends Exception {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  public SchedulerException() {

    super();
  }

  public SchedulerException(String msg) {

    super(msg);
  }

  SchedulerException(Throwable cause) {

    super(cause);
  }

  public SchedulerException(String msg, Throwable cause) {

    super(msg, cause);
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * Return the exception that is the underlying cause of this exception.
   *
   * <p>This may be used to find more detail about the cause of the error.
   *
   * @return the underlying exception, or <code>null</code> if there is not one.
   */
  public Throwable getUnderlyingException() {

    return super.getCause();
  }

  @Override
  public String toString() {

    Throwable cause = getUnderlyingException();
    if (cause == null || cause == this) {
      return super.toString();
    } else {
      return super.toString() + " [See nested exception: " + cause + "]";
    }
  }
}
