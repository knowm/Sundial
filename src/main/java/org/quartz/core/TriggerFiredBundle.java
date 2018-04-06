package org.quartz.core;

import java.util.Date;

import org.quartz.jobs.JobDetail;
import org.quartz.triggers.OperableTrigger;

/**
 * <p>
 * A simple class (structure) used for returning execution-time data from the JobStore to the <code>QuartzSchedulerThread</code>.
 * </p>
 * 
 * @see org.quartz.QuartzScheduler
 * @author James House
 */
public class TriggerFiredBundle implements java.io.Serializable {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private JobDetail job;

  private OperableTrigger trigger;

  private Calendar cal;

  private boolean jobIsRecovering;

  private Date fireTime;

  private Date scheduledFireTime;

  private Date prevFireTime;

  private Date nextFireTime;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  public TriggerFiredBundle(JobDetail job, OperableTrigger trigger, Calendar cal, boolean jobIsRecovering, Date fireTime, Date scheduledFireTime,
      Date prevFireTime, Date nextFireTime) {

    this.job = job;
    this.trigger = trigger;
    this.cal = cal;
    this.jobIsRecovering = jobIsRecovering;
    this.fireTime = fireTime;
    this.scheduledFireTime = scheduledFireTime;
    this.prevFireTime = prevFireTime;
    this.nextFireTime = nextFireTime;
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  public JobDetail getJobDetail() {

    return job;
  }

  public OperableTrigger getTrigger() {

    return trigger;
  }

  public Calendar getCalendar() {

    return cal;
  }

  public boolean isRecovering() {

    return jobIsRecovering;
  }

  /**
   * @return Returns the fireTime.
   */
  public Date getFireTime() {

    return fireTime;
  }

  /**
   * @return Returns the nextFireTime.
   */
  public Date getNextFireTime() {

    return nextFireTime;
  }

  /**
   * @return Returns the prevFireTime.
   */
  public Date getPrevFireTime() {

    return prevFireTime;
  }

  /**
   * @return Returns the scheduledFireTime.
   */
  public Date getScheduledFireTime() {

    return scheduledFireTime;
  }

}