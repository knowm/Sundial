
/* 
 * Copyright 2001-2009 Terracotta, Inc. 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 * 
 */

package org.quartz.impl;

import java.util.Date;
import java.util.HashMap;

import org.quartz.Calendar;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.spi.TriggerFiredBundle;


public class JobExecutionContextImpl implements java.io.Serializable, JobExecutionContext {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
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
    
    private HashMap data = new HashMap();

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Create a JobExcecutionContext with the given context data.
     * </p>
     */
    public JobExecutionContextImpl(Scheduler scheduler,
            TriggerFiredBundle firedBundle, Job job) {
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
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /* (non-Javadoc)
     * @see org.quartz.JobExecutionContext#getScheduler()
     */
    public Scheduler getScheduler() {
        return scheduler;
    }

    /* (non-Javadoc)
     * @see org.quartz.JobExecutionContext#getTrigger()
     */
    public Trigger getTrigger() {
        return trigger;
    }

    /* (non-Javadoc)
     * @see org.quartz.JobExecutionContext#getCalendar()
     */
    public Calendar getCalendar() {
        return calendar;
    }

    /* (non-Javadoc)
     * @see org.quartz.JobExecutionContext#isRecovering()
     */
    public boolean isRecovering() {
        return recovering;
    }

    public void incrementRefireCount() {
        numRefires++;
    }

    /* (non-Javadoc)
     * @see org.quartz.JobExecutionContext#getRefireCount()
     */
    public int getRefireCount() {
        return numRefires;
    }

    /* (non-Javadoc)
     * @see org.quartz.JobExecutionContext#getMergedJobDataMap()
     */
    public JobDataMap getMergedJobDataMap() {
        return jobDataMap;
    }

    /* (non-Javadoc)
     * @see org.quartz.JobExecutionContext#getJobDetail()
     */
    public JobDetail getJobDetail() {
        return jobDetail;
    }

    /* (non-Javadoc)
     * @see org.quartz.JobExecutionContext#getJobInstance()
     */
    public Job getJobInstance() {
        return job;
    }

    /* (non-Javadoc)
     * @see org.quartz.JobExecutionContext#getFireTime()
     */
    public Date getFireTime() {
        return fireTime;
    }

    /* (non-Javadoc)
     * @see org.quartz.JobExecutionContext#getScheduledFireTime()
     */
    public Date getScheduledFireTime() {
        return scheduledFireTime;
    }

    /* (non-Javadoc)
     * @see org.quartz.JobExecutionContext#getPreviousFireTime()
     */
    public Date getPreviousFireTime() {
        return prevFireTime;
    }

    /* (non-Javadoc)
     * @see org.quartz.JobExecutionContext#getNextFireTime()
     */
    public Date getNextFireTime() {
        return nextFireTime;
    }

    public String toString() {
        return "JobExecutionContext:" + " trigger: '"
                + getTrigger().getKey() + " job: "
                + getJobDetail().getKey() + " fireTime: '" + getFireTime()
                + " scheduledFireTime: " + getScheduledFireTime()
                + " previousFireTime: '" + getPreviousFireTime()
                + " nextFireTime: " + getNextFireTime() + " isRecovering: "
                + isRecovering() + " refireCount: " + getRefireCount();
    }

    /* (non-Javadoc)
     * @see org.quartz.JobExecutionContext#getResult()
     */
    public Object getResult() {
        return result;
    }
    
    /* (non-Javadoc)
     * @see org.quartz.JobExecutionContext#setResult(java.lang.Object)
     */
    public void setResult(Object result) {
        this.result = result;
    }
    
    /* (non-Javadoc)
     * @see org.quartz.JobExecutionContext#getJobRunTime()
     */
    public long getJobRunTime() {
        return jobRunTime;
    }
    
    /**
     * @param jobRunTime The jobRunTime to set.
     */
    public void setJobRunTime(long jobRunTime) {
        this.jobRunTime = jobRunTime;
    }

    /* (non-Javadoc)
     * @see org.quartz.JobExecutionContext#put(java.lang.Object, java.lang.Object)
     */
    public void put(Object key, Object value) {
        data.put(key, value);
    }
    
    /* (non-Javadoc)
     * @see org.quartz.JobExecutionContext#get(java.lang.Object)
     */
    public Object get(Object key) {
        return data.get(key);
    }
}
