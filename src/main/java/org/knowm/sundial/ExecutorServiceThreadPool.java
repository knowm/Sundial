package org.knowm.sundial;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.quartz.core.JobRunShell;
import org.quartz.core.ThreadPool;
import org.quartz.exceptions.SchedulerConfigException;

/**
 * A {@link ThreadPool} implementation backed by a standard {@link ExecutorService}.
 *
 * <p>Two usage modes:
 *
 * <ul>
 *   <li><b>Sundial-managed pool</b>: constructed with a thread count. Sundial creates and owns a
 *       fixed-size thread pool internally and shuts it down when the scheduler stops.
 *   <li><b>User-managed pool</b>: constructed with an existing {@link ExecutorService}. The caller
 *       is responsible for the executor's lifecycle — Sundial will not shut it down.
 * </ul>
 */
public class ExecutorServiceThreadPool implements ThreadPool {

  private static final AtomicInteger poolCounter = new AtomicInteger(1);

  private final ExecutorService executorService;
  private final int poolSize;
  private final Semaphore semaphore;
  private final boolean ownsExecutorService;

  /**
   * Create a thread pool backed by an internally-managed fixed-size {@link ExecutorService}.
   * Sundial owns the executor and will shut it down when the scheduler stops.
   *
   * @param poolSize the number of concurrent worker threads
   */
  public ExecutorServiceThreadPool(int poolSize) {
    this.poolSize = poolSize;
    int n = poolCounter.getAndIncrement();
    AtomicInteger threadCount = new AtomicInteger(1);
    this.executorService =
        Executors.newFixedThreadPool(
            poolSize,
            r -> {
              Thread t = new Thread(r, "Sundial-Worker-" + n + "-" + threadCount.getAndIncrement());
              t.setDaemon(false);
              return t;
            });
    this.semaphore = new Semaphore(poolSize);
    this.ownsExecutorService = true;
  }

  /**
   * Wrap an existing {@link ExecutorService}. Sundial will not shut the executor down when the
   * scheduler stops — that is the caller's responsibility.
   *
   * <p>Use this to reuse an application-wide thread pool or a framework-managed executor (e.g.
   * Spring, Jakarta EE).
   *
   * @param executorService the executor to delegate job execution to
   */
  public ExecutorServiceThreadPool(ExecutorService executorService) {
    this.poolSize = Integer.MAX_VALUE;
    this.executorService = executorService;
    this.semaphore = null; // caller manages their executor's capacity
    this.ownsExecutorService = false;
  }

  /**
   * Blocks until at least one thread is available, then returns the number of currently available
   * threads.
   *
   * <p>For a Sundial-managed pool, this blocks on the internal {@link Semaphore}. For a
   * user-managed pool, this returns {@code 1} immediately since the external executor has its own
   * queuing.
   */
  @Override
  public int blockForAvailableThreads() {
    if (semaphore != null) {
      semaphore.acquireUninterruptibly();
      semaphore.release();
      return semaphore.availablePermits();
    }
    return 1;
  }

  @Override
  public boolean runInThread(JobRunShell runnable) {
    if (semaphore != null) {
      semaphore.acquireUninterruptibly();
    }
    try {
      executorService.execute(
          () -> {
            try {
              runnable.run();
            } finally {
              if (semaphore != null) {
                semaphore.release();
              }
            }
          });
    } catch (Exception e) {
      if (semaphore != null) {
        semaphore.release();
      }
      return false;
    }
    return true;
  }

  @Override
  public void initialize() throws SchedulerConfigException {
    // nothing to initialize — ExecutorService is ready at construction time
  }

  /**
   * Shuts down the executor if Sundial owns it. If the executor was provided by the caller, this
   * method does nothing — the caller is responsible for shutting it down.
   */
  @Override
  public void shutdown() {
    if (ownsExecutorService) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (InterruptedException e) {
        executorService.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public int getPoolSize() {
    return poolSize;
  }
}
