package org.quartz.core;

import org.quartz.exceptions.SchedulerConfigException;

/**
 * <p>
 * The interface to be implemented by classes that want to provide a thread pool for the <code>{@link org.quartz.QuartzScheduler}</code>'s use.
 * </p>
 * <p>
 * <code>ThreadPool</code> implementation instances should ideally be made for the sole use of Quartz. Most importantly, when the method
 * <code>blockForAvailableThreads()</code> returns a value of 1 or greater, there must still be at least one available thread in the pool when the
 * method <code>runInThread(Runnable)</code> is called a few moments (or many moments) later. If this assumption does not hold true, it may result in
 * extra JobStore queries and updates, and if clustering features are being used, it may result in greater imballance of load.
 * </p>
 * 
 * @see org.quartz.QuartzScheduler
 * @author James House
 */
public interface ThreadPool {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Execute the given <code>{@link java.lang.Runnable}</code> in the next available <code>Thread</code>.
   * </p>
   * <p>
   * The implementation of this interface should not throw exceptions unless there is a serious problem (i.e. a serious misconfiguration). If there
   * are no immediately available threads <code>false</code> should be returned.
   * </p>
   * 
   * @return true, if the runnable was assigned to run on a Thread.
   */
  boolean runInThread(Runnable runnable);

  /**
   * <p>
   * Determines the number of threads that are currently available in in the pool. Useful for determining the number of times
   * <code>runInThread(Runnable)</code> can be called before returning false.
   * </p>
   * <p>
   * The implementation of this method should block until there is at least one available thread.
   * </p>
   * 
   * @return the number of currently available threads
   */
  int blockForAvailableThreads();

  /**
   * <p>
   * Must be called before the <code>ThreadPool</code> is used, in order to give the it a chance to initialize.
   * </p>
   * <p>
   * Typically called by the <code>SchedulerFactory</code>.
   * </p>
   */
  void initialize() throws SchedulerConfigException;

  /**
   * <p>
   * Called by the QuartzScheduler to inform the <code>ThreadPool</code> that it should free up all of it's resources because the scheduler is
   * shutting down.
   * </p>
   */
  void shutdown(boolean waitForJobsToComplete);

  /**
   * <p>
   * Get the current number of threads in the <code>ThreadPool</code>.
   * </p>
   */
  int getPoolSize();

}
