package org.quartz.core;

import java.util.Date;
import org.quartz.jobs.Job;
import org.quartz.jobs.JobDataMap;
import org.quartz.jobs.JobDetail;
import org.quartz.triggers.Trigger;

public class JobExecutionContextImpl implements java.io.Serializable, JobExecutionContext {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private transient Scheduler scheduler;

  private Trigger trigger;

  private JobDetail jobDetail;

  private JobDataMap jobDataMap;

  private transient Job job;

  private Calendar calendar;

  private boolean recovering = false;

  private int numRefires = 0;

  private Date fireTime;

  private Date scheduledFireTime;

  private Date prevFireTime;

  private Date nextFireTime;

  private long jobRunTime = -1;

  private Object result;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /** Create a JobExcecutionContext with the given context data. */
  public JobExecutionContextImpl(Scheduler scheduler, TriggerFiredBundle firedBundle, Job job) {

    this.scheduler = scheduler;
    this.trigger = firedBundle.getTrigger();
    this.calendar = firedBundle.getCalendar();
    this.jobDetail = firedBundle.getJobDetail();
    this.job = job;
    this.recovering = firedBundle.isRecovering();
    this.fireTime = firedBundle.getFireTime();
    this.scheduledFireTime = firedBundle.getScheduledFireTime();
    this.prevFireTime = firedBundle.getPrevFireTime();
    this.nextFireTime = firedBundle.getNextFireTime();

    this.jobDataMap = new JobDataMap();
    this.jobDataMap.putAll(jobDetail.getJobDataMap());
    this.jobDataMap.putAll(trigger.getJobDataMap());
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  @Override
  public Scheduler getScheduler() {

    return scheduler;
  }

  @Override
  public Trigger getTrigger() {

    return trigger;
  }

  @Override
  public Calendar getCalendar() {

    return calendar;
  }

  @Override
  public boolean isRecovering() {

    return recovering;
  }

  public void incrementRefireCount() {

    numRefires++;
  }

  @Override
  public int getRefireCount() {

    return numRefires;
  }

  @Override
  public JobDataMap getMergedJobDataMap() {

    return jobDataMap;
  }

  @Override
  public JobDetail getJobDetail() {

    return jobDetail;
  }

  @Override
  public Job getJobInstance() {

    return job;
  }

  @Override
  public Date getFireTime() {

    return fireTime;
  }

  @Override
  public Date getScheduledFireTime() {

    return scheduledFireTime;
  }

  @Override
  public Date getPreviousFireTime() {

    return prevFireTime;
  }

  @Override
  public Date getNextFireTime() {

    return nextFireTime;
  }

  @Override
  public String toString() {

    return "JobExecutionContext:"
        + " trigger: '"
        + getTrigger().getName()
        + " job: "
        + getJobDetail().getName()
        + " fireTime: '"
        + getFireTime()
        + " scheduledFireTime: "
        + getScheduledFireTime()
        + " previousFireTime: '"
        + getPreviousFireTime()
        + " nextFireTime: "
        + getNextFireTime()
        + " isRecovering: "
        + isRecovering()
        + " refireCount: "
        + getRefireCount();
  }

  @Override
  public Object getResult() {

    return result;
  }

  @Override
  public void setResult(Object result) {

    this.result = result;
  }

  @Override
  public long getJobRunTime() {

    return jobRunTime;
  }

  /**
   * @param jobRunTime The jobRunTime to set.
   */
  public void setJobRunTime(long jobRunTime) {

    this.jobRunTime = jobRunTime;
  }
}
