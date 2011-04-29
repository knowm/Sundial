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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.AttributeList;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.UnableToInterruptJobException;
import org.quartz.Trigger.TriggerState;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.JobFactory;

/**
 * <p>
 * An implementation of the <code>Scheduler</code> interface that remotely
 * proxies all method calls to the equivalent call on a given <code>QuartzScheduler</code>
 * instance, via JMX.
 * </p>
 * 
 * <p>
 * A user must create a subclass to implement the actual connection to the remote 
 * MBeanServer using their application specific connector.
 * For example <code>{@link org.quartz.ee.jmx.jboss.JBoss4RMIRemoteMBeanScheduler}</code>.
 * </p>
 * @see org.quartz.Scheduler
 * @see org.quartz.core.QuartzScheduler
 * @see org.quartz.core.SchedulingContext
 */
public abstract class RemoteMBeanScheduler implements Scheduler {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private ObjectName schedulerObjectName;
    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public RemoteMBeanScheduler() { 
    }
    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Properties.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    
    /**
     * Get the name under which the Scheduler MBean is registered on the
     * remote MBean server.
     */
    protected ObjectName getSchedulerObjectName() {
        return schedulerObjectName;
    }

    /**
     * Set the name under which the Scheduler MBean is registered on the
     * remote MBean server.
     */
    public void setSchedulerObjectName(String schedulerObjectName)  throws SchedulerException {
        try {
            this.schedulerObjectName = new ObjectName(schedulerObjectName);
        } catch (MalformedObjectNameException e) {
            throw new SchedulerException("Failed to parse Scheduler MBean name: " + schedulerObjectName, e);
        }
    }

    /**
     * Set the name under which the Scheduler MBean is registered on the
     * remote MBean server.
     */
    public void setSchedulerObjectName(ObjectName schedulerObjectName)  throws SchedulerException {
        this.schedulerObjectName = schedulerObjectName;
    }

    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Abstract methods.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * Initialize this RemoteMBeanScheduler instance, connecting to the
     * remote MBean server.
     */
    public abstract void initialize() throws SchedulerException;

    /**
     * Get the given attribute of the remote Scheduler MBean.
     */
    protected abstract Object getAttribute(
            String attribute) throws SchedulerException;
        
    /**
     * Get the given attributes of the remote Scheduler MBean.
     */
    protected abstract AttributeList getAttributes(String[] attributes)
        throws SchedulerException;
    
    /**
     * Invoke the given operation on the remote Scheduler MBean.
     */
    protected abstract Object invoke(
        String operationName,
        Object[] params,
        String[] signature) throws SchedulerException;
        

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Returns the name of the <code>Scheduler</code>.
     * </p>
     */
    public String getSchedulerName() throws SchedulerException {
        return (String)getAttribute("schedulerName");
    }

    /**
     * <p>
     * Returns the instance Id of the <code>Scheduler</code>.
     * </p>
     */
    public String getSchedulerInstanceId() throws SchedulerException {
        return (String)getAttribute("schedulerInstanceId");
    }

    public SchedulerMetaData getMetaData() throws SchedulerException {
        AttributeList attributeList = 
            getAttributes(
                new String[] {
                    "schedulerName",
                    "schedulerInstanceId",
                    "inStandbyMode",
                    "shutdown",
                    "jobStoreClass",
                    "threadPoolClass",
                    "threadPoolSize",
                    "version"
                });
        
        return new SchedulerMetaData(
                (String)attributeList.get(0),
                (String)attributeList.get(1),
                getClass(), true, isStarted(), 
                ((Boolean)attributeList.get(2)).booleanValue(), 
                ((Boolean)attributeList.get(3)).booleanValue(), 
                (Date)invoke("runningSince", new Object[] {}, new String[] {}), 
                ((Integer)invoke("numJobsExecuted", new Object[] {}, new String[] {})).intValue(),
                (Class)attributeList.get(4),
                ((Boolean)invoke("supportsPersistence", new Object[] {}, new String[] {})).booleanValue(),
                ((Boolean)invoke("isClustered", new Object[] {}, new String[] {})).booleanValue(),
                (Class)attributeList.get(5),
                ((Integer)attributeList.get(6)).intValue(),
                (String)attributeList.get(7));
    }

    /**
     * <p>
     * Returns the <code>SchedulerContext</code> of the <code>Scheduler</code>.
     * </p>
     */
    public SchedulerContext getContext() throws SchedulerException {
        return (SchedulerContext)getAttribute("schedulerContext");
    }

    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Schedululer State Management Methods
    ///
    ///////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void start() throws SchedulerException {
        invoke("start", new Object[] {}, new String[] {});
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void startDelayed(int seconds) throws SchedulerException {
        invoke("startDelayed", new Object[] {Integer.valueOf(seconds)}, new String[] {int.class.getName()});
    }
    
    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void standby() throws SchedulerException {
        invoke("standby", new Object[] {}, new String[] {});
    }

    /**
     * Whether the scheduler has been started.  
     * 
     * <p>
     * Note: This only reflects whether <code>{@link #start()}</code> has ever
     * been called on this Scheduler, so it will return <code>true</code> even 
     * if the <code>Scheduler</code> is currently in standby mode or has been 
     * since shutdown.
     * </p>
     * 
     * @see #start()
     * @see #isShutdown()
     * @see #isInStandbyMode()
     */    
    public boolean isStarted() throws SchedulerException {
        return (invoke("runningSince", new Object[] {}, new String[] {}) != null);
    }
    
    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public boolean isInStandbyMode() throws SchedulerException {
        return ((Boolean)getAttribute("inStandbyMode")).booleanValue();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void shutdown() throws SchedulerException {
        // Have to get the scheduler name before we actually call shutdown.
        String schedulerName = getSchedulerName();
        
        invoke("shutdown", new Object[] {}, new String[] {});
        SchedulerRepository.getInstance().remove(schedulerName);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void shutdown(boolean waitForJobsToComplete)
        throws SchedulerException {
        // Have to get the scheduler name before we actually call shutdown.
        String schedulerName = getSchedulerName();
        
        invoke(
            "shutdown", 
            new Object[] { toBoolean(waitForJobsToComplete) }, 
            new String[] { boolean.class.getName() });
        
        SchedulerRepository.getInstance().remove(schedulerName);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public boolean isShutdown() throws SchedulerException {
        return ((Boolean)getAttribute("shutdown")).booleanValue();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public List<JobExecutionContext> getCurrentlyExecutingJobs() throws SchedulerException {
        return (List)invoke("getCurrentlyExecutingJobs", new Object[] {}, new String[] {});
    }

    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Scheduling-related Methods
    ///
    ///////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public Date scheduleJob(JobDetail jobDetail, Trigger trigger)
        throws SchedulerException {
        return (Date)invoke(
                "scheduleJob", 
                new Object[] { jobDetail, trigger }, 
                new String[] { JobDetail.class.getName(), Trigger.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public Date scheduleJob(Trigger trigger) throws SchedulerException {
        return (Date)invoke(
                "scheduleJob", 
                new Object[] { trigger }, 
                new String[] { Trigger.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void addJob(JobDetail jobDetail, boolean replace)
        throws SchedulerException {
        invoke(
            "addJob", 
            new Object[] { jobDetail, toBoolean(replace) }, 
            new String[] { JobDetail.class.getName(), boolean.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public boolean deleteJob(JobKey jobKey)
        throws SchedulerException {
        return ((Boolean)invoke(
                "deleteJob", 
                new Object[] { jobKey }, 
                new String[] { JobKey.class.getName() })).booleanValue();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public boolean unscheduleJob(TriggerKey triggerKey)
        throws SchedulerException {
        return ((Boolean)invoke(
                "unscheduleJob", 
                new Object[] { triggerKey }, 
                new String[] { TriggerKey.class.getName() })).booleanValue();
    }


    public boolean deleteJobs(List<JobKey> jobKeys) throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }

    public void scheduleJobs(Map<JobDetail, List<Trigger>> triggersAndJobs, boolean replace) throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }

    public boolean unscheduleJobs(List<TriggerKey> triggerKeys) throws SchedulerException {
        throw new SchedulerException("Operation not supported for remote schedulers.");
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public Date rescheduleJob(TriggerKey triggerKey,
            Trigger newTrigger) throws SchedulerException {
        return (Date)invoke(
                "unscheduleJob", 
                new Object[] { triggerKey, newTrigger}, 
                new String[] { TriggerKey.class.getName(), Trigger.class.getName() });
    }
    
    
    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void triggerJob(JobKey jobKey)
        throws SchedulerException {
        triggerJob(jobKey, null);
    }
    
    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void triggerJob(JobKey jobKey, JobDataMap data)
        throws SchedulerException {
        invoke(
            "triggerJob", 
            new Object[] { jobKey, data}, 
            new String[] { JobKey.class.getName(), JobDataMap.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void pauseTrigger(TriggerKey triggerKey)
        throws SchedulerException {
        invoke(
            "pauseTrigger", 
            new Object[] { triggerKey }, 
            new String[] { TriggerKey.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void pauseTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
        invoke(
            "pauseTriggers",
            new Object[] { matcher },
            new String[] { GroupMatcher.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void pauseJob(JobKey jobKey)
        throws SchedulerException {
        invoke(
            "pauseJob", 
            new Object[] { jobKey }, 
            new String[] { JobKey.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void pauseJobs(GroupMatcher<JobKey> matcher) throws SchedulerException {
        invoke(
            "pauseJobs",
            new Object[] { matcher },
            new String[] { GroupMatcher.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void resumeTrigger(TriggerKey triggerKey)
        throws SchedulerException {
        invoke(
            "resumeTrigger", 
            new Object[] { triggerKey }, 
            new String[] { TriggerKey.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void resumeTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
        invoke(
            "resumeTriggers",
            new Object[] { matcher },
            new String[] { GroupMatcher.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void resumeJob(JobKey jobKey)
        throws SchedulerException {
        invoke(
            "resumeJob", 
            new Object[] { jobKey }, 
            new String[] { JobKey.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void resumeJobs(GroupMatcher<JobKey> matcher) throws SchedulerException {
        invoke(
            "resumeJobs",
            new Object[] { matcher },
            new String[] { GroupMatcher.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void pauseAll() throws SchedulerException {
        invoke(
            "pauseAll", 
            new Object[] { }, 
            new String[] { });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void resumeAll() throws SchedulerException {
        invoke(
            "resumeAll", 
            new Object[] { }, 
            new String[] { });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public List<String> getJobGroupNames() throws SchedulerException {
        return (List<String>) invoke(
                "getJobGroupNames", 
                new Object[] { }, 
                new String[] { });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher) throws SchedulerException {
        Set<JobKey> keys = (Set<JobKey>)invoke(
                "getJobNames", 
                new Object[] { matcher },
                new String[] { GroupMatcher.class.getName() });
        
        return keys;
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public List<Trigger> getTriggersOfJob(JobKey jobKey)
        throws SchedulerException {
        return (List<Trigger>)invoke(
                "getTriggersOfJob", 
                new Object[] { jobKey }, 
                new String[] { JobKey.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public List<String> getTriggerGroupNames() throws SchedulerException {
        return (List<String>)invoke(
                "getTriggerGroupNames", 
                new Object[] { }, 
                new String[] { });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
        Set<TriggerKey> keys =  (Set<TriggerKey>)invoke(
                "getTriggerKeys",
                new Object[] { matcher },
                new String[] { GroupMatcher.class.getName() });

        return keys;
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public JobDetail getJobDetail(JobKey jobKey)
        throws SchedulerException {
        return (JobDetail)invoke(
                "getJobDetail", 
                new Object[] { jobKey }, 
                new String[] { JobKey.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public Trigger getTrigger(TriggerKey triggerKey)
        throws SchedulerException {
        return (Trigger)invoke(
                "getTrigger", 
                new Object[] { triggerKey }, 
                new String[] { TriggerKey.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public boolean checkExists(JobKey jobKey) throws SchedulerException {
        return (Boolean)invoke(
                "checkExists", 
                new Object[] { jobKey }, 
                new String[] { JobKey.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public boolean checkExists(TriggerKey triggerKey) throws SchedulerException {
        return (Boolean)invoke(
                "checkExists", 
                new Object[] { triggerKey }, 
                new String[] { TriggerKey.class.getName() });
    }
    
    public void clear() throws SchedulerException {
        invoke(
                "clear", 
                new Object[] {  }, 
                new String[] {  });
    }


    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public TriggerState getTriggerState(TriggerKey triggerKey)
        throws SchedulerException {
        return (TriggerState)invoke(
                "getTriggerState", 
                new Object[] { triggerKey }, 
                new String[] { TriggerKey.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public void addCalendar(String calName, Calendar calendar, boolean replace, boolean updateTriggers)
        throws SchedulerException {
        invoke(
            "addCalendar", 
            new Object[] { calName, calendar, toBoolean(replace), toBoolean(updateTriggers) }, 
            new String[] { String.class.getName(), 
                    Calendar.class.getName(), boolean.class.getName(), boolean.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public boolean deleteCalendar(String calName) throws SchedulerException {
        return ((Boolean)invoke(
                "getTriggerState", 
                new Object[] { calName }, 
                new String[] { String.class.getName() })).booleanValue();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public Calendar getCalendar(String calName) throws SchedulerException {
        return (Calendar)invoke(
                "getCalendar", 
                new Object[] { calName }, 
                new String[] { String.class.getName() });
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>,
     * passing the <code>SchedulingContext</code> associated with this
     * instance.
     * </p>
     */
    public List<String> getCalendarNames() throws SchedulerException {
        return (List<String>)invoke(
                "getCalendarNames", 
                new Object[] { }, 
                new String[] { });
    }

    /** 
     * @see org.quartz.Scheduler#getPausedTriggerGroups()
     */
    public Set<String> getPausedTriggerGroups() throws SchedulerException {
        return (Set<String>)invoke(
                "getPausedTriggerGroups", 
                new Object[] { }, 
                new String[] { });
    }

    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Other Methods
    ///
    ///////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public ListenerManager getListenerManager() throws SchedulerException {
        throw new SchedulerException(
                "Operation not supported for remote schedulers.");
    }

    /**
     * @see org.quartz.Scheduler#interrupt(JobKey)
     */
    public boolean interrupt(JobKey jobKey) throws UnableToInterruptJobException  {
        try {
            return ((Boolean)invoke(
                    "interrupt", 
                    new Object[] { jobKey }, 
                    new String[] { JobKey.class.getName() })).booleanValue();
        } catch (SchedulerException se) {
            throw new UnableToInterruptJobException(se);
        }
    }

    /**
     * @see org.quartz.Scheduler#setJobFactory(org.quartz.spi.JobFactory)
     */
    public void setJobFactory(JobFactory factory) throws SchedulerException {
        throw new SchedulerException(
                "Operation not supported for remote schedulers.");
    }
    
    protected Boolean toBoolean(boolean bool) {
        return (bool) ? Boolean.TRUE : Boolean.FALSE;
    }
}
