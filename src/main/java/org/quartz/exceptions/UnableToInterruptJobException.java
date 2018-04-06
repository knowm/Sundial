package org.quartz.exceptions;

/**
 * An exception that is thrown to indicate that a call to InterruptableJob.interrupt() failed without interrupting the Job.
 * 
 * @see org.quartz.jobs.InterruptableJob#interrupt()
 * @author James House
 */
public class UnableToInterruptJobException extends SchedulerException {

}
