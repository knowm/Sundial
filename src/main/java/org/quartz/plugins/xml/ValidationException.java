package org.quartz.plugins.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Reports JobSchedulingDataLoader validation exceptions.
 *
 * @author <a href="mailto:bonhamcm@thirdeyeconsulting.com">Chris Bonham</a>
 */
class ValidationException extends Exception {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private Collection validationExceptions = new ArrayList();

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /** Constructor for ValidationException. */
  public ValidationException() {

    super();
  }

  /**
   * Constructor for ValidationException.
   *
   * @param message exception message.
   */
  private ValidationException(String message) {

    super(message);
  }

  /**
   * Constructor for ValidationException.
   *
   * @param message exception message.
   * @param errors collection of validation exceptions.
   */
  ValidationException(String message, Collection<Exception> errors) {

    this(message);
    this.validationExceptions = Collections.unmodifiableCollection(validationExceptions);
    initCause(errors.iterator().next());
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * Returns collection of errors.
   *
   * @return collection of errors.
   */
  public Collection getValidationExceptions() {

    return validationExceptions;
  }

  /**
   * Returns the detail message string.
   *
   * @return the detail message string.
   */
  @Override
  public String getMessage() {

    if (getValidationExceptions().size() == 0) {
      return super.getMessage();
    }

    StringBuffer sb = new StringBuffer();

    boolean first = true;

    for (Iterator iter = getValidationExceptions().iterator(); iter.hasNext(); ) {
      Exception e = (Exception) iter.next();

      if (!first) {
        sb.append('\n');
        first = false;
      }

      sb.append(e.getMessage());
    }

    return sb.toString();
  }
}
