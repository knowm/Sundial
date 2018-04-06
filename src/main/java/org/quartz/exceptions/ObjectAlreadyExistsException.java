package org.quartz.exceptions;

import org.quartz.core.Calendar;
import org.quartz.core.Scheduler;
import org.quartz.jobs.JobDetail;
import org.quartz.triggers.Trigger;

/**
 * An exception that is thrown to indicate that an attempt to store a new object (i.e. <code>
 * {@link org.quartz.jobs.JobDetail}</code>, <code>{@link Trigger}</code> or <code>{@link Calendar}
 * </code>) in a <code>{@link Scheduler}</code> failed, because one with the same name & group
 * already exists.
 *
 * @author James House
 */
public class ObjectAlreadyExistsException extends JobPersistenceException {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * Create a <code>ObjectAlreadyExistsException</code> and auto-generate a message using the
   * name/group from the given <code>JobDetail</code>.
   *
   * <p>The message will read: <br>
   * "Unable to store Job with name: '__' and group: '__', because one already exists with this
   * identification."
   */
  public ObjectAlreadyExistsException(JobDetail offendingJob) {

    super(
        "Unable to store Job : '"
            + offendingJob.getName()
            + "', because one already exists with this identification.");
  }

  /**
   * Create a <code>ObjectAlreadyExistsException</code> and auto-generate a message using the
   * name/group from the given <code>Trigger</code>.
   *
   * <p>The message will read: <br>
   * "Unable to store Trigger with name: '__' and group: '__', because one already exists with this
   * identification."
   */
  public ObjectAlreadyExistsException(Trigger offendingTrigger) {

    super(
        "Unable to store Trigger with name: '"
            + offendingTrigger.getName()
            + "', because one already exists with this identification.");
  }
}
