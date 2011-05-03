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
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerKey;
import org.quartz.UnableToInterruptJobException;
import org.quartz.core.QuartzScheduler;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.JobFactory;

/**
 * <p>
 * An implementation of the <code>Scheduler</code> interface that directly proxies all method calls to the equivalent call on a given <code>QuartzScheduler</code> instance.
 * </p>
 * 
 * @see org.quartz.Scheduler
 * @see org.quartz.core.QuartzScheduler
 * @see org.quartz.core.SchedulingContext
 * @author James House
 */
public class StdScheduler implements Scheduler {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private QuartzScheduler mQuartzScheduler;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Construct a <code>StdScheduler</code> instance to proxy the given <code>QuartzScheduler</code> instance, and with the given <code>SchedulingContext</code>.
     * </p>
     */
    public StdScheduler(QuartzScheduler sched) {
        this.mQuartzScheduler = sched;
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Returns the name of the <code>Scheduler</code>.
     * </p>
     */
    @Override
    public String getSchedulerName() {
        return mQuartzScheduler.getSchedulerName();
    }

    /**
     * <p>
     * Returns the instance Id of the <code>Scheduler</code>.
     * </p>
     */
    @Override
    public String getSchedulerInstanceId() {
        return mQuartzScheduler.getSchedulerInstanceId();
    }

    @Override
    public SchedulerMetaData getMetaData() {
        return new SchedulerMetaData(getSchedulerName(), getSchedulerInstanceId(), getClass(), isStarted(), isInStandbyMode(), isShutdown(), mQuartzScheduler.runningSince(), mQuartzScheduler.numJobsExecuted(), mQuartzScheduler.getJobStoreClass(),
                mQuartzScheduler.supportsPersistence(), mQuartzScheduler.isClustered(), mQuartzScheduler.getThreadPoolClass(), mQuartzScheduler.getThreadPoolSize(), mQuartzScheduler.getVersion());

    }

    /**
     * <p>
     * Returns the <code>SchedulerContext</code> of the <code>Scheduler</code>.
     * </p>
     */
    @Override
    public SchedulerContext getContext() throws SchedulerException {
        return mQuartzScheduler.getSchedulerContext();
    }

    // /////////////////////////////////////////////////////////////////////////
    // /
    // / Scheduler State Management Methods
    // /
    // /////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void start() throws SchedulerException {
        mQuartzScheduler.start();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void startDelayed(int seconds) throws SchedulerException {
        mQuartzScheduler.startDelayed(seconds);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void standby() {
        mQuartzScheduler.standby();
    }

    /**
     * Whether the scheduler has been started.
     * <p>
     * Note: This only reflects whether <code>{@link #start()}</code> has ever been called on this Scheduler, so it will return <code>true</code> even if the <code>Scheduler</code> is currently in standby mode or has been since shutdown.
     * </p>
     * 
     * @see #start()
     * @see #isShutdown()
     * @see #isInStandbyMode()
     */
    @Override
    public boolean isStarted() {
        return (mQuartzScheduler.runningSince() != null);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public boolean isInStandbyMode() {
        return mQuartzScheduler.isInStandbyMode();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void shutdown() {
        mQuartzScheduler.shutdown();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void shutdown(boolean waitForJobsToComplete) {
        mQuartzScheduler.shutdown(waitForJobsToComplete);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public boolean isShutdown() {
        return mQuartzScheduler.isShutdown();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public List<JobExecutionContext> getCurrentlyExecutingJobs() {
        return mQuartzScheduler.getCurrentlyExecutingJobs();
    }

    // /////////////////////////////////////////////////////////////////////////
    // /
    // / Scheduling-related Methods
    // /
    // /////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void clear() throws SchedulerException {
        mQuartzScheduler.clear();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public Date scheduleJob(JobDetail jobDetail, Trigger trigger) throws SchedulerException {
        return mQuartzScheduler.scheduleJob(jobDetail, trigger);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public Date scheduleJob(Trigger trigger) throws SchedulerException {
        return mQuartzScheduler.scheduleJob(trigger);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void addJob(JobDetail jobDetail, boolean replace) throws SchedulerException {
        mQuartzScheduler.addJob(jobDetail, replace);
    }

    @Override
    public boolean deleteJobs(List<JobKey> jobKeys) throws SchedulerException {
        return mQuartzScheduler.deleteJobs(jobKeys);
    }

    @Override
    public void scheduleJobs(Map<JobDetail, List<Trigger>> triggersAndJobs, boolean replace) throws SchedulerException {
        mQuartzScheduler.scheduleJobs(triggersAndJobs, replace);
    }

    @Override
    public boolean unscheduleJobs(List<TriggerKey> triggerKeys) throws SchedulerException {
        return mQuartzScheduler.unscheduleJobs(triggerKeys);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public boolean deleteJob(JobKey jobKey) throws SchedulerException {
        return mQuartzScheduler.deleteJob(jobKey);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public boolean unscheduleJob(TriggerKey triggerKey) throws SchedulerException {
        return mQuartzScheduler.unscheduleJob(triggerKey);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public Date rescheduleJob(TriggerKey triggerKey, Trigger newTrigger) throws SchedulerException {
        return mQuartzScheduler.rescheduleJob(triggerKey, newTrigger);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void triggerJob(JobKey jobKey) throws SchedulerException {
        triggerJob(jobKey, null);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void triggerJob(JobKey jobKey, JobDataMap data) throws SchedulerException {
        mQuartzScheduler.triggerJob(jobKey, data);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void pauseTrigger(TriggerKey triggerKey) throws SchedulerException {
        mQuartzScheduler.pauseTrigger(triggerKey);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void pauseTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
        mQuartzScheduler.pauseTriggers(matcher);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void pauseJob(JobKey jobKey) throws SchedulerException {
        mQuartzScheduler.pauseJob(jobKey);
    }

    /**
     * @see org.quartz.Scheduler#getPausedTriggerGroups()
     */
    @Override
    public Set getPausedTriggerGroups() throws SchedulerException {
        return mQuartzScheduler.getPausedTriggerGroups();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void pauseJobs(GroupMatcher<JobKey> matcher) throws SchedulerException {
        mQuartzScheduler.pauseJobs(matcher);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void resumeTrigger(TriggerKey triggerKey) throws SchedulerException {
        mQuartzScheduler.resumeTrigger(triggerKey);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void resumeTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
        mQuartzScheduler.resumeTriggers(matcher);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void resumeJob(JobKey jobKey) throws SchedulerException {
        mQuartzScheduler.resumeJob(jobKey);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void resumeJobs(GroupMatcher<JobKey> matcher) throws SchedulerException {
        mQuartzScheduler.resumeJobs(matcher);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void pauseAll() throws SchedulerException {
        mQuartzScheduler.pauseAll();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void resumeAll() throws SchedulerException {
        mQuartzScheduler.resumeAll();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public List<String> getJobGroupNames() throws SchedulerException {
        return mQuartzScheduler.getJobGroupNames();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public List<? extends Trigger> getTriggersOfJob(JobKey jobKey) throws SchedulerException {
        return mQuartzScheduler.getTriggersOfJob(jobKey);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher) throws SchedulerException {
        return mQuartzScheduler.getJobKeys(matcher);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public List<String> getTriggerGroupNames() throws SchedulerException {
        return mQuartzScheduler.getTriggerGroupNames();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
        return mQuartzScheduler.getTriggerKeys(matcher);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public JobDetail getJobDetail(JobKey jobKey) throws SchedulerException {
        return mQuartzScheduler.getJobDetail(jobKey);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public Trigger getTrigger(TriggerKey triggerKey) throws SchedulerException {
        return mQuartzScheduler.getTrigger(triggerKey);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public TriggerState getTriggerState(TriggerKey triggerKey) throws SchedulerException {
        return mQuartzScheduler.getTriggerState(triggerKey);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public void addCalendar(String calName, Calendar calendar, boolean replace, boolean updateTriggers) throws SchedulerException {
        mQuartzScheduler.addCalendar(calName, calendar, replace, updateTriggers);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public boolean deleteCalendar(String calName) throws SchedulerException {
        return mQuartzScheduler.deleteCalendar(calName);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public Calendar getCalendar(String calName) throws SchedulerException {
        return mQuartzScheduler.getCalendar(calName);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public List<String> getCalendarNames() throws SchedulerException {
        return mQuartzScheduler.getCalendarNames();
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public boolean checkExists(JobKey jobKey) throws SchedulerException {
        return mQuartzScheduler.checkExists(jobKey);
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    @Override
    public boolean checkExists(TriggerKey triggerKey) throws SchedulerException {
        return mQuartzScheduler.checkExists(triggerKey);
    }

    // /////////////////////////////////////////////////////////////////////////
    // /
    // / Other Methods
    // /
    // /////////////////////////////////////////////////////////////////////////

    /**
     * @see org.quartz.Scheduler#setJobFactory(org.quartz.spi.JobFactory)
     */
    @Override
    public void setJobFactory(JobFactory factory) throws SchedulerException {
        mQuartzScheduler.setJobFactory(factory);
    }

    /**
     * @see org.quartz.Scheduler#getListenerManager()
     */
    @Override
    public ListenerManager getListenerManager() throws SchedulerException {
        return mQuartzScheduler.getListenerManager();
    }

    @Override
    public boolean interrupt(JobKey jobKey) throws UnableToInterruptJobException {
        return mQuartzScheduler.interrupt(jobKey);
    }

}
