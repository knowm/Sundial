package org.knowm.sundial;

import java.util.concurrent.TimeUnit;

/** @author timmolter */
public class SampleRun {

  /** Make sure jobs.xml is on the classpath! */
  public static void main(String[] args) {

    SundialJobScheduler.startScheduler("org.knowm.sundial.jobs"); // package with annotated Jobs

    SundialJobScheduler.addJob("SampleJob1", "org.knowm.sundial.jobs.SampleJob1");

    SundialJobScheduler.addCronTrigger("SampleJob1-Cron-Trigger", "SampleJob1", "0/10 * * * * ?");

    SundialJobScheduler.addSimpleTrigger(
        "SampleJob1-Simple-Trigger", "SampleJob1", -1, TimeUnit.SECONDS.toMillis(3));
  }
}
