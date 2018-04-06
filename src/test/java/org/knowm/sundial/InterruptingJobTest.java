package org.knowm.sundial;

import java.util.concurrent.Semaphore;

import org.junit.Test;
import org.quartz.core.JobExecutionContext;
import org.quartz.core.JobExecutionContextImpl;
import org.quartz.core.TriggerFiredBundle;
import org.quartz.jobs.JobDetail;
import org.quartz.jobs.JobDetailImpl;
import org.quartz.triggers.OperableTrigger;
import org.quartz.triggers.SimpleTriggerImpl;

public class InterruptingJobTest {
    @Test
    public void shouldInterruptThread() throws Exception {
        final Semaphore sem = new Semaphore(0);
        final WaitingJob job = new WaitingJob(sem);

        final JobDetail detail = new JobDetailImpl();
        final OperableTrigger trigger = new SimpleTriggerImpl();
        final TriggerFiredBundle bundle = new TriggerFiredBundle(detail, trigger, null, true, null, null, null, null);
        final JobExecutionContext context = new JobExecutionContextImpl(null, bundle, job);

        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    job.execute(context);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        t.start();

        synchronized (job) {
            while (!job.aboutToSleep) {
                job.wait();
            }
        }
        while (!sem.hasQueuedThreads()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        job.interrupt();

        t.join();
    }

    static class WaitingJob extends InterruptingJob {
        public boolean aboutToSleep = false;
        final Semaphore sem;
        public WaitingJob(Semaphore sem) {
            this.sem = sem;
        }
        @Override
        public void doRun() {
            synchronized (this) {
                aboutToSleep = true;
                notify();
            }
            try {
                sem.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
