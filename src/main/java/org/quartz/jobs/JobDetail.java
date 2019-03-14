package org.quartz.jobs;

import java.io.Serializable;
import org.quartz.builders.JobBuilder;
import org.quartz.core.Scheduler;

/**
 * Conveys the detail properties of a given <code>Job</code> instance. JobDetails are to be
 * created/defined with {@link JobBuilder}.
 *
 * <p>Quartz does not store an actual instance of a <code>Job</code> class, but instead allows you
 * to define an instance of one, through the use of a <code>JobDetail</code>.
 *
 * <p><code>Job</code>s have a name associated with them, which should uniquely identify them within
 * a single <code>{@link Scheduler}</code>.
 *
 * <p><code>Trigger</code>s are the 'mechanism' by which <code>Job</code>s are scheduled. Many
 * <code>Trigger</code>s can point to the same <code>Job</code>, but a single <code>Trigger</code>
 * can only point to one <code>Job</code>.
 *
 * @author James House
 */
public interface JobDetail extends Serializable, Cloneable {

  public String getName();

  /**
   * Return the description given to the <code>Job</code> instance by its creator (if any).
   *
   * @return null if no description was set.
   */
  public String getDescription();

  /** Get the instance of <code>Job</code> that will be executed. */
  public Class<? extends Job> getJobClass();

  /** Get the <code>JobDataMap</code> that is associated with the <code>Job</code>. */
  public JobDataMap getJobDataMap();

  /**
   * The default behavior is to veto any job is currently running concurrent. However, concurrent
   * jobs can be created by setting the 'Concurrency' to true
   *
   * @return
   */
  public boolean isConcurrencyAllowed();

  public Object clone();

  /**
   * Get a {@link JobBuilder} that is configured to produce a <code>JobDetail</code> identical to
   * this one.
   */
  public JobBuilder getJobBuilder();
}
