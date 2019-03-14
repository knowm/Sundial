package org.quartz.triggers;

import java.util.Date;
import org.quartz.core.Calendar;
import org.quartz.core.JobExecutionContext;
import org.quartz.core.Scheduler;
import org.quartz.exceptions.JobExecutionException;
import org.quartz.exceptions.SchedulerException;

public interface OperableTrigger extends MutableTrigger {

  /**
   * This method should not be used by the Quartz client.
   *
   * <p>Called when the <code>{@link Scheduler}</code> has decided to 'fire' the trigger (execute
   * the associated <code>Job</code>), in order to give the <code>Trigger</code> a chance to update
   * itself for its next triggering (if any).
   *
   * @see #executionComplete(JobExecutionContext, JobExecutionException)
   */
  public void triggered(Calendar calendar);

  /**
   * This method should not be used by the Quartz client.
   *
   * <p>Called by the scheduler at the time a <code>Trigger</code> is first added to the scheduler,
   * in order to have the <code>Trigger</code> compute its first fire time, based on any associated
   * calendar.
   *
   * <p>After this method has been called, <code>getNextFireTime()</code> should return a valid
   * answer.
   *
   * @return the first time at which the <code>Trigger</code> will be fired by the scheduler, which
   *     is also the same value <code>getNextFireTime()</code> will return (until after the first
   *     firing of the <code>Trigger</code>).
   */
  public Date computeFirstFireTime(Calendar calendar);

  /**
   * This method should not be used by the Quartz client.
   *
   * <p>Called after the <code>{@link Scheduler}</code> has executed the <code>
   * {@link org.quartz.jobs.JobDetail}</code> associated with the <code>Trigger</code> in order to
   * get the final instruction code from the trigger.
   *
   * @param context is the <code>JobExecutionContext</code> that was used by the <code>Job</code>'s
   *     <code>execute(xx)</code> method.
   * @param result is the <code>JobExecutionException</code> thrown by the <code>Job</code>, if any
   *     (may be null).
   * @return one of the <code>CompletedExecutionInstruction</code> constants.
   * @see CompletedExecutionInstruction
   * @see #triggered(Calendar)
   */
  public CompletedExecutionInstruction executionComplete(
      JobExecutionContext context, JobExecutionException result);

  /**
   * This method should not be used by the Quartz client.
   *
   * <p>To be implemented by the concrete classes that extend this class.
   *
   * <p>The implementation should update the <code>Trigger</code>'s state based on the
   * MISFIRE_INSTRUCTION_XXX that was selected when the <code>Trigger</code> was created.
   */
  public void updateAfterMisfire(Calendar cal);

  /**
   * Validates whether the properties of the <code>JobDetail</code> are valid for submission into a
   * <code>Scheduler</code>.
   *
   * @throws IllegalStateException if a required property (such as Name, Group, Class) is not set.
   */
  public void validate() throws SchedulerException;

  /**
   * This method should not be used by the Quartz client.
   *
   * <p>Usable by <code>{@link org.quartz.core.JobStore}</code> implementations, in order to
   * facilitate 'recognizing' instances of fired <code>Trigger</code> s as their jobs complete
   * execution.
   */
  public void setFireInstanceId(String id);

  /** This method should not be used by the Quartz client. */
  public String getFireInstanceId();

  public void setNextFireTime(Date nextFireTime);

  public void setPreviousFireTime(Date previousFireTime);
}
