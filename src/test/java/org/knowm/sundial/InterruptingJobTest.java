package org.knowm.sundial;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.quartz.exceptions.SchedulerException;

public class InterruptingJobTest {

  @BeforeClass
  public static void setup() {
    SundialJobScheduler.startScheduler(1, null);
  }

  @AfterClass
  public static void shutdownScheduler() {
    SundialJobScheduler.shutdown();
  }

  /**
   * A job that blocks indefinitely on a CountDownLatch. It records whether it was interrupted
   * before the latch was released.
   */
  public static class BlockingJob extends InterruptingJob {

    static final CountDownLatch jobStarted = new CountDownLatch(1);
    static final AtomicBoolean wasInterrupted = new AtomicBoolean(false);

    @Override
    public void doRun() throws JobInterruptException {
      jobStarted.countDown();
      try {
        new CountDownLatch(1).await(); // blocks forever unless interrupted
      } catch (InterruptedException e) {
        wasInterrupted.set(true);
        Thread.currentThread().interrupt();
      }
    }
  }

  @Test
  public void testStopJobInterruptsBlockingJob() throws InterruptedException, SchedulerException {
    SundialJobScheduler.addJob("BlockingJob", BlockingJob.class);
    SundialJobScheduler.addSimpleTrigger("blocking-trigger", "BlockingJob", 0, 1);

    // Wait until the job is actually running and blocking
    BlockingJob.jobStarted.await();

    // Give the job a moment to enter the blocking await()
    Thread.sleep(50);

    // Stop the job — should unblock it via Thread.interrupt()
    SundialJobScheduler.stopJob("BlockingJob");

    // Give it a moment to process the interrupt
    Thread.sleep(200);

    assertTrue("Job should have been interrupted", BlockingJob.wasInterrupted.get());
  }
}
