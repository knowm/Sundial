/**
 * Copyright 2015 Knowm Inc. (http://knowm.org) and contributors.
 * Copyright 2013-2015 Xeiam LLC (http://xeiam.com) and contributors.
 * Copyright 2001-2011 Terracotta Inc. (http://terracotta.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quartz.core;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.quartz.exceptions.SchedulerConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This is class is a simple implementation of a thread pool, based on the <code>{@link org.quartz.core.ThreadPool}</code> interface.
 * </p>
 * <p>
 * <CODE>Runnable</CODE> objects are sent to the pool with the <code>{@link #runInThread(Runnable)}</code> method, which blocks until a
 * <code>Thread</code> becomes available.
 * </p>
 * <p>
 * The pool has a fixed number of <code>Thread</code>s, and does not grow or shrink based on demand.
 * </p>
 * 
 * @author James House
 * @author Juergen Donnerstag
 */
public class SimpleThreadPool implements ThreadPool {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private int count = -1;

  private int prio = Thread.NORM_PRIORITY;

  private boolean isShutdown = false;
  private boolean handoffPending = false;

  private boolean inheritLoader = false;

  private boolean inheritGroup = true;

  private boolean makeThreadsDaemons = false;

  private ThreadGroup threadGroup;

  private final Object nextRunnableLock = new Object();

  private List<WorkerThread> workers;
  private LinkedList<WorkerThread> availWorkers = new LinkedList<WorkerThread>();
  private LinkedList<WorkerThread> busyWorkers = new LinkedList<WorkerThread>();

  private String threadNamePrefix;

  private final Logger log = LoggerFactory.getLogger(getClass());

  private String schedulerInstanceName;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Create a new (unconfigured) <code>SimpleThreadPool</code>.
   * </p>
   * 
   * @see #setThreadCount(int)
   * @see #setThreadPriority(int)
   */
  public SimpleThreadPool() {

  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  public Logger getLog() {

    return log;
  }

  @Override
  public int getPoolSize() {

    return getThreadCount();
  }

  /**
   * <p>
   * Set the number of worker threads in the pool - has no effect after <code>initialize()</code> has been called.
   * </p>
   */
  public void setThreadCount(int count) {

    this.count = count;
  }

  /**
   * <p>
   * Get the number of worker threads in the pool.
   * </p>
   */
  public int getThreadCount() {

    return count;
  }

  /**
   * <p>
   * Set the thread priority of worker threads in the pool - has no effect after <code>initialize()</code> has been called.
   * </p>
   */
  public void setThreadPriority(int prio) {

    this.prio = prio;
  }

  /**
   * <p>
   * Get the thread priority of worker threads in the pool.
   * </p>
   */
  public int getThreadPriority() {

    return prio;
  }

  public void setThreadNamePrefix(String prfx) {

    this.threadNamePrefix = prfx;
  }

  public String getThreadNamePrefix() {

    if (threadNamePrefix == null) {
      threadNamePrefix = schedulerInstanceName + "-SimpleThreadPoolWorker";
    }
    return threadNamePrefix;
  }

  /**
   * @return Returns the threadsInheritContextClassLoaderOfInitializingThread.
   */
  public boolean isThreadsInheritContextClassLoaderOfInitializingThread() {

    return inheritLoader;
  }

  /**
   * @param inheritLoader The threadsInheritContextClassLoaderOfInitializingThread to set.
   */
  public void setThreadsInheritContextClassLoaderOfInitializingThread(boolean inheritLoader) {

    this.inheritLoader = inheritLoader;
  }

  public boolean isThreadsInheritGroupOfInitializingThread() {

    return inheritGroup;
  }

  public void setThreadsInheritGroupOfInitializingThread(boolean inheritGroup) {

    this.inheritGroup = inheritGroup;
  }

  /**
   * @return Returns the value of makeThreadsDaemons.
   */
  public boolean isMakeThreadsDaemons() {

    return makeThreadsDaemons;
  }

  /**
   * @param makeThreadsDaemons The value of makeThreadsDaemons to set.
   */
  public void setMakeThreadsDaemons(boolean makeThreadsDaemons) {

    this.makeThreadsDaemons = makeThreadsDaemons;
  }

  public void setInstanceId(String schedInstId) {

  }

  @Override
  public void initialize() throws SchedulerConfigException {

    if (workers != null && workers.size() > 0) {
      return;
    }

    if (count <= 0) {
      throw new SchedulerConfigException("Thread count must be > 0");
    }
    if (prio <= 0 || prio > 9) {
      throw new SchedulerConfigException("Thread priority must be > 0 and <= 9");
    }

    if (isThreadsInheritGroupOfInitializingThread()) {
      threadGroup = Thread.currentThread().getThreadGroup();
    } else {
      // follow the threadGroup tree to the root thread group.
      threadGroup = Thread.currentThread().getThreadGroup();
      ThreadGroup parent = threadGroup;
      while (!parent.getName().equals("main")) {
        threadGroup = parent;
        parent = threadGroup.getParent();
      }
      threadGroup = new ThreadGroup(parent, schedulerInstanceName + "-SimpleThreadPool");
      if (isMakeThreadsDaemons()) {
        threadGroup.setDaemon(true);
      }
    }

    if (isThreadsInheritContextClassLoaderOfInitializingThread()) {
      getLog().info("Job execution threads will use class loader of thread: " + Thread.currentThread().getName());
    }

    // create the worker threads and start them
    Iterator workerThreads = createWorkerThreads(count).iterator();
    while (workerThreads.hasNext()) {
      WorkerThread wt = (WorkerThread) workerThreads.next();
      wt.start();
      availWorkers.add(wt);
    }
  }

  private List<WorkerThread> createWorkerThreads(int count) {

    workers = new LinkedList<WorkerThread>();
    for (int i = 1; i <= count; ++i) {
      WorkerThread wt = new WorkerThread(this, threadGroup, getThreadNamePrefix() + "-" + i, getThreadPriority(), isMakeThreadsDaemons());
      if (isThreadsInheritContextClassLoaderOfInitializingThread()) {
        wt.setContextClassLoader(Thread.currentThread().getContextClassLoader());
      }
      workers.add(wt);
    }

    return workers;
  }

  /**
   * <p>
   * Terminate any worker threads in this thread group.
   * </p>
   * <p>
   * Jobs currently in progress will complete.
   * </p>
   */
  public void shutdown() {

    shutdown(true);
  }

  /**
   * <p>
   * Terminate any worker threads in this thread group.
   * </p>
   * <p>
   * Jobs currently in progress will complete.
   * </p>
   */
  @Override
  public void shutdown(boolean waitForJobsToComplete) {

    synchronized (nextRunnableLock) {
      isShutdown = true;

      if (workers == null) {
        return;
      }

      // signal each worker thread to shut down
      Iterator workerThreads = workers.iterator();
      while (workerThreads.hasNext()) {
        WorkerThread wt = (WorkerThread) workerThreads.next();
        wt.shutdown();
        availWorkers.remove(wt);
      }

      // Give waiting (wait(1000)) worker threads a chance to shut down.
      // Active worker threads will shut down after finishing their
      // current job.
      nextRunnableLock.notifyAll();

      if (waitForJobsToComplete == true) {

        // wait for hand-off in runInThread to complete...
        while (handoffPending) {
          try {
            nextRunnableLock.wait(100);
          } catch (Throwable t) {
          }
        }

        // Wait until all worker threads are shut down
        while (busyWorkers.size() > 0) {
          WorkerThread wt = busyWorkers.getFirst();
          try {
            getLog().debug("Waiting for thread " + wt.getName() + " to shut down");

            // note: with waiting infinite time the
            // application may appear to 'hang'.
            nextRunnableLock.wait(2000);
          } catch (InterruptedException ex) {
          }
        }

        getLog().debug("shutdown complete");
      }
    }
  }

  /**
   * <p>
   * Run the given <code>Runnable</code> object in the next available <code>Thread</code>. If while waiting the thread pool is asked to shut down, the
   * Runnable is executed immediately within a new additional thread.
   * </p>
   * 
   * @param runnable the <code>Runnable</code> to be added.
   */
  @Override
  public boolean runInThread(Runnable runnable) {

    if (runnable == null) {
      return false;
    }

    synchronized (nextRunnableLock) {

      handoffPending = true;

      // Wait until a worker thread is available
      while ((availWorkers.size() < 1) && !isShutdown) {
        try {
          nextRunnableLock.wait(500);
        } catch (InterruptedException ignore) {
        }
      }

      if (!isShutdown) {
        WorkerThread wt = availWorkers.removeFirst();
        busyWorkers.add(wt);
        wt.run(runnable);
      } else {
        // If the thread pool is going down, execute the Runnable
        // within a new additional worker thread (no thread from the pool).
        WorkerThread wt = new WorkerThread(this, threadGroup, "WorkerThread-LastJob", prio, isMakeThreadsDaemons(), runnable);
        busyWorkers.add(wt);
        workers.add(wt);
        wt.start();
      }
      nextRunnableLock.notifyAll();
      handoffPending = false;
    }

    return true;
  }

  @Override
  public int blockForAvailableThreads() {

    synchronized (nextRunnableLock) {

      while ((availWorkers.size() < 1 || handoffPending) && !isShutdown) {
        try {
          nextRunnableLock.wait(500);
        } catch (InterruptedException ignore) {
        }
      }

      return availWorkers.size();
    }
  }

  private void makeAvailable(WorkerThread wt) {

    synchronized (nextRunnableLock) {
      if (!isShutdown) {
        availWorkers.add(wt);
      }
      busyWorkers.remove(wt);
      nextRunnableLock.notifyAll();
    }
  }

  private void clearFromBusyWorkersList(WorkerThread wt) {

    synchronized (nextRunnableLock) {
      busyWorkers.remove(wt);
      nextRunnableLock.notifyAll();
    }
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ WorkerThread Class.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * A Worker loops, waiting to execute tasks.
   * </p>
   */
  private class WorkerThread extends Thread {

    // A flag that signals the WorkerThread to terminate.
    private boolean run = true;

    private SimpleThreadPool tp;

    private Runnable runnable = null;

    private boolean runOnce = false;

    /**
     * <p>
     * Create a worker thread and start it. Waiting for the next Runnable, executing it, and waiting for the next Runnable, until the shutdown flag is
     * set.
     * </p>
     */
    private WorkerThread(SimpleThreadPool tp, ThreadGroup threadGroup, String name, int prio, boolean isDaemon) {

      this(tp, threadGroup, name, prio, isDaemon, null);
    }

    /**
     * <p>
     * Create a worker thread, start it, execute the runnable and terminate the thread (one time execution).
     * </p>
     */
    private WorkerThread(SimpleThreadPool tp, ThreadGroup threadGroup, String name, int prio, boolean isDaemon, Runnable runnable) {

      super(threadGroup, name);
      this.tp = tp;
      this.runnable = runnable;
      if (runnable != null) {
        runOnce = true;
      }
      setPriority(prio);
      setDaemon(isDaemon);
    }

    /**
     * <p>
     * Signal the thread that it should terminate.
     * </p>
     */
    void shutdown() {

      synchronized (this) {
        run = false;
      }
    }

    public void run(Runnable newRunnable) {

      synchronized (this) {
        if (runnable != null) {
          throw new IllegalStateException("Already running a Runnable!");
        }

        runnable = newRunnable;
        this.notifyAll();
      }
    }

    /**
     * <p>
     * Loop, executing targets as they are received.
     * </p>
     */
    @Override
    public void run() {

      boolean ran = false;
      boolean shouldRun = false;
      synchronized (this) {
        shouldRun = run;
      }

      while (shouldRun) {
        try {
          synchronized (this) {
            while (runnable == null && run) {
              this.wait(500);
            }

            if (runnable != null) {
              ran = true;
              runnable.run();
            }
          }
        } catch (InterruptedException unblock) {
          // do nothing (loop will terminate if shutdown() was called
          try {
            getLog().error("Worker thread was interrupt()'ed.", unblock);
          } catch (Exception e) {
            // ignore to help with a tomcat glitch
          }
        } catch (Throwable exceptionInRunnable) {
          try {
            getLog().error("Error while executing the Runnable: ", exceptionInRunnable);
          } catch (Exception e) {
            // ignore to help with a tomcat glitch
          }
        } finally {
          synchronized (this) {
            runnable = null;
          }
          // repair the thread in case the runnable mucked it up...
          if (getPriority() != tp.getThreadPriority()) {
            setPriority(tp.getThreadPriority());
          }

          if (runOnce) {
            synchronized (this) {
              run = false;
            }
            clearFromBusyWorkersList(this);
          } else if (ran) {
            ran = false;
            makeAvailable(this);
          }

        }

        // read value of run within synchronized block to be
        // sure of its value
        synchronized (this) {
          shouldRun = run;
        }
      }

      // if (log.isDebugEnabled())
      try {
        getLog().debug("WorkerThread is shut down.");
      } catch (Exception e) {
        // ignore to help with a tomcat glitch
      }
    }
  }
}
