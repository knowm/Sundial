package org.knowm.sundial;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SundialJobSchedulerTest {
  public SundialJobSchedulerTest() {}

  @BeforeClass
  public static void createScheduler() {
    SundialJobScheduler.createScheduler(10, "com.example.empty");
  }

  @Test
  public void shouldBeAbleToAddJobsByName() {
    SundialJobScheduler.addJob("jobByName", "org.knowm.sundial.jobs.SampleJob1");
    Assert.assertTrue(SundialJobScheduler.getAllJobNames().contains("jobByName"));
  }

  @Test
  public void shouldBeAbleToAddJobsByClass() {
    SundialJobScheduler.addJob("jobByClass", org.knowm.sundial.jobs.SampleJob1.class);
    Assert.assertTrue(SundialJobScheduler.getAllJobNames().contains("jobByClass"));
  }
}
