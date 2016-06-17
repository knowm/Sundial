package org.knowm.sundial;

import org.quartz.exceptions.UnableToInterruptJobException;

/** Base class that interrupts blocking operations when the job is stopped.
 *
 * This class can be used as the base class for jobs that should interrupt
 * blocking operations when the job is stopped, as opposed to completing them
 * first.
 *
 * @see Thread#interrupt()
 */
public abstract class InterruptingJob extends Job {
    private Thread executingThread;

    public synchronized void setup() {
        executingThread = Thread.currentThread();
    }

    public synchronized void cleanup() {
        executingThread = null;
    }

    public void interrupt() throws UnableToInterruptJobException {
        synchronized (this) {
            if (executingThread != null) {
                executingThread.interrupt();
            }
        }
        super.interrupt();
    }
}
