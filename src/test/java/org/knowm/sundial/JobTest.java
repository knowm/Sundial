package org.knowm.sundial;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import org.junit.After;
import org.junit.Test;
import org.quartz.builders.JobBuilder;
import org.quartz.builders.SimpleTriggerBuilder;
import org.quartz.core.JobExecutionContext;
import org.quartz.core.JobExecutionContextImpl;
import org.quartz.core.TriggerFiredBundle;
import org.quartz.jobs.JobDetail;
import org.quartz.triggers.OperableTrigger;

public class JobTest {

  @After
  public void clearContext() {
    new ContextJob().destroyContext();
  }

  @Test
  public void testContextAvailableDuringCleanupAndRemovedAfterward() throws Exception {
    ContextJob job = new ContextJob();

    job.execute(contextFor(job));

    assertEquals("payload", job.cleanupContext.get("value"));
    assertNull(job.getJobContext());
  }

  @Test
  public void testContextRemovedWhenCleanupThrows() {
    assertContextRemovedAfterCleanupFailure(false, false);
  }

  @Test
  public void testContextRemovedWhenRunAndCleanupThrow() {
    assertContextRemovedAfterCleanupFailure(false, true);
  }

  @Test
  public void testContextRemovedWhenSetupAndCleanupThrow() {
    assertContextRemovedAfterCleanupFailure(true, false);
  }

  private void assertContextRemovedAfterCleanupFailure(boolean failSetup, boolean failRun) {
    ContextJob job = new ContextJob();
    job.failSetup = failSetup;
    job.failRun = failRun;
    job.cleanupFailure = new IllegalStateException("cleanup failed");

    IllegalStateException thrown =
        assertThrows(IllegalStateException.class, () -> job.execute(contextFor(job)));

    assertSame(job.cleanupFailure, thrown);
    assertEquals("payload", job.cleanupContext.get("value"));
    assertNull(job.getJobContext());
  }

  private JobExecutionContext contextFor(ContextJob job) {
    JobDetail detail = JobBuilder.newJobBuilder(ContextJob.class).withIdentity("context-job").build();
    detail.getJobDataMap().put("value", "payload");
    OperableTrigger trigger =
        SimpleTriggerBuilder.simpleTriggerBuilder()
            .withIdentity("context-trigger")
            .forJob("context-job")
            .build();
    TriggerFiredBundle bundle =
        new TriggerFiredBundle(detail, trigger, null, false, null, null, null, null);
    return new JobExecutionContextImpl(null, bundle, job);
  }

  public static class ContextJob extends Job {

    private boolean failSetup;
    private boolean failRun;
    private IllegalStateException cleanupFailure;
    private JobContext cleanupContext;

    @Override
    public void setup() {
      if (failSetup) {
        throw new IllegalStateException("setup failed");
      }
    }

    @Override
    public void doRun() {
      if (failRun) {
        throw new IllegalStateException("run failed");
      }
    }

    @Override
    public void cleanup() {
      cleanupContext = getJobContext();
      if (cleanupFailure != null) {
        throw cleanupFailure;
      }
    }
  }
}
