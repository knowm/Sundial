package org.knowm.sundial;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.quartz.exceptions.SchedulerException;

/** This doesn't really test much. But at least it allows us to inspect the job output on errors. */
public class BadJobTest {
  public static class BadJob extends Job {

    @Override
    public void doRun() throws JobInterruptException {
      throw new RuntimeException("I'm bad to the bone");
    }
  }

  @BeforeClass
  public void setup() {
    SundialJobScheduler.startScheduler(1, null); // null -> don't load anything
    List<String> names = SundialJobScheduler.getAllJobNames();

    // We get the jobs from the XML for free
    // assertTrue( names.isEmpty() );
  }

  @AfterClass
  public static void shutdownScheduler() {
    SundialJobScheduler.shutdown();
  }

  @Test
  public void testJobsNeverFail() throws InterruptedException, SchedulerException {
    BadJob bj = new BadJob();
    SundialJobScheduler.addJob(BadJob.class.getSimpleName(), BadJob.class);
    SundialJobScheduler.addSimpleTrigger("bj-trigger", BadJob.class.getSimpleName(), 0, 1);
    List<String> names = SundialJobScheduler.getAllJobNames();
    Thread.sleep(100);
  }
}
