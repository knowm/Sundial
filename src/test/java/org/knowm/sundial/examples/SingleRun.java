package org.knowm.sundial.examples;

import org.knowm.sundial.SundialJobScheduler;

/** @author timmolter */
public class SingleRun {

  public static void main(String[] args) {

    SundialJobScheduler.startScheduler();

    SundialJobScheduler.addJob("SampleJob9", "org.knowm.sundial.jobs.SampleJob9");

    SundialJobScheduler.startJob("SampleJob9");

    //        try {
    //          Thread.sleep(1000);
    //        } catch (InterruptedException e) {
    //          // TODO Auto-generated catch block
    //          e.printStackTrace();
    //        }

    SundialJobScheduler.shutdown();
  }
}
