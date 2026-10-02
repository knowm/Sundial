package org.knowm.sundial;

/**
 * Base class for jobs that should interrupt blocking operations (e.g. I/O, sleep, semaphore
 * acquire) when the job is stopped, rather than waiting for them to complete naturally.
 *
 * <p>Extend this class instead of {@link Job} when your job may block indefinitely on operations
 * that respond to {@link Thread#interrupt()}. When {@link #interrupt()} is called (e.g. via {@link
 * SundialJobScheduler#stopJob(String)}), the executing thread will be interrupted immediately.
 *
 * <p>Note: you must still handle {@link InterruptedException} in your {@link #doRun()} and
 * re-interrupt the thread (i.e. call {@code Thread.currentThread().interrupt()}) so the interrupt
 * status is preserved.
 *
 * @see Job#interrupt()
 * @see Thread#interrupt()
 */
public abstract class InterruptingJob extends Job {

  private Thread executingThread;

  @Override
  public synchronized void setup() {
    executingThread = Thread.currentThread();
  }

  @Override
  public synchronized void cleanup() {
    executingThread = null;
  }

  @Override
  public void interrupt() {
    synchronized (this) {
      if (executingThread != null) {
        executingThread.interrupt();
      }
    }
    super.interrupt();
  }
}
