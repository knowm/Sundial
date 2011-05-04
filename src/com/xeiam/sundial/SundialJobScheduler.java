/**
 * Copyright 2011 Xeiam LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xeiam.sundial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.exceptions.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry-point to the Sundial scheduler
 * 
 * @author timmolter
 */
public class SundialJobScheduler {

    /** slf4J logger wrapper */
    static Logger logger = LoggerFactory.getLogger(SundialJobScheduler.class);

    /** Quartz scheduler */
    private static Scheduler mScheduler = null;

    // /** ThreadPool Size */
    // private static int mThreadPoolSize = 10;

    /** global lock */
    private static boolean mGlobalLock = false;

    /**
     * Gets the underlying Sundial scheduler
     * 
     * @return
     */
    public static Scheduler getScheduler() {

        if (mScheduler == null) {
            mScheduler = createScheduler(10);
        }
        return mScheduler;
    }

    /**
     * Creates the Sundial Scheduler
     * 
     * @param pThreadPoolSize
     * @return
     */
    public static Scheduler createScheduler(int pThreadPoolSize) {

        if (mScheduler == null) {
            try {
                mScheduler = new StdSchedulerFactory().getScheduler(pThreadPoolSize);

            } catch (SchedulerException e) {
                logger.error("COULD NOT CREATE QUARTZ SCHEDULER!!!" + e);
            }
        }
        return mScheduler;
    }

    /**
     * Starts the Sundial Scheduler
     */
    public static void startScheduler() {
        try {
            getScheduler().start();
        } catch (SchedulerException e) {
            logger.error("COULD NOT START QUARTZ SCHEDULER!!!" + e);

        }
    }

    public static void toggleGlobalLock() {
        mGlobalLock = !mGlobalLock;
    }

    public static void lockScheduler() {
        mGlobalLock = true;
    }

    public static void unlockScheduler() {
        mGlobalLock = false;
    }

    public static boolean getGlobalLock() {
        return mGlobalLock;
    }

    /**
     * Starts a Job matching the the given Job Name found in jobs.xml
     * 
     * @param pJobName
     */
    public static void startJob(String pJobName) {

        try {
            JobKey jobKey = new JobKey(pJobName);
            getScheduler().triggerJob(jobKey);
        } catch (SchedulerException e) {
            logger.error("ERROR SCHEDULING FIRE ONCE JOB!!!", e);
        }

    }

    /**
     * Starts a Job matching the the given Job Name found in jobs.xml
     * 
     * @param pJobName
     */
    public static void startJob(String pJobName, Map<String, String> pParams) {

        try {

            JobDataMap lJobDataMap = new JobDataMap();
            for (String key : pParams.keySet()) {
                // logger.debug("key= " + key);
                // logger.debug("value= " + pParams.get(key));
                lJobDataMap.put(key, pParams.get(key));
            }
            JobKey jobKey = new JobKey(pJobName);
            getScheduler().triggerJob(jobKey, lJobDataMap);
        } catch (SchedulerException e) {
            logger.error("ERROR SCHEDULING FIRE ONCE JOB!!!", e);
        }

    }

    /**
     * Triggers a Job interrupt on all Jobs matching the given Job Name
     * 
     * @param pJobName
     */
    public static void stopJob(String pJobName) {

        try {
            List<JobExecutionContext> currentlyExecutingJobs = getScheduler().getCurrentlyExecutingJobs();
            for (JobExecutionContext lJobExecutionContext : currentlyExecutingJobs) {
                String currentlyExecutingJobName = lJobExecutionContext.getJobDetail().getKey().getName();
                if (currentlyExecutingJobName.equals(pJobName)) {
                    logger.debug("Matching Job found. Now Stopping!");
                    if (lJobExecutionContext.getJobInstance() instanceof Job) {
                        ((Job) lJobExecutionContext.getJobInstance()).interrupt();
                    } else {
                        logger.warn("CANNOT STOP NON-INTERRUPTABLE JOB!!!");
                    }
                } else {
                    logger.debug("Non-matching Job found. Not Stopping!");
                }
            }
        } catch (SchedulerException e) {
            logger.error("ERROR DURING STOP Job!!!" + e);
        }
    }

    /**
     * Triggers a Job interrupt on all Jobs matching the given Job Name, key and value
     * 
     * @param pJobName
     */
    public static void stopJob(String pJobName, String pKey, String pValue) {

        logger.debug("key= " + pKey);
        logger.debug("value= " + pValue);
        try {
            List<JobExecutionContext> currentlyExecutingJobs = getScheduler().getCurrentlyExecutingJobs();
            for (JobExecutionContext lJobExecutionContext : currentlyExecutingJobs) {
                String currentlyExecutingJobName = lJobExecutionContext.getJobDetail().getKey().getName();
                if (currentlyExecutingJobName.equals(pJobName)) {
                    if (lJobExecutionContext.getJobInstance() instanceof Job) {
                        JobDataMap lJobDataMap = lJobExecutionContext.getMergedJobDataMap();
                        String value = lJobDataMap.getString(pKey);
                        if (value != null & value.equalsIgnoreCase(pValue)) {
                            ((Job) lJobExecutionContext.getJobInstance()).interrupt();
                        }
                    } else {
                        logger.warn("CANNOT STOP NON-INTERRUPTABLE JOB!!!");
                    }
                } else {
                    logger.debug("Non-matching Job found. Not Stopping!");
                }
            }
        } catch (SchedulerException e) {
            logger.error("ERROR DURING STOP Job!!!" + e);
        }
    }

    /**
     * Generates an alphabetically sorted List of all Job names in the DEFAULT job group
     * 
     * @return
     */
    public static List<String> getAllJobNames() {

        List<String> lAllJobNames = new ArrayList<String>();
        try {
            Set<JobKey> allJobKeys = getScheduler().getJobKeys(null);
            for (JobKey jobKey : allJobKeys) {
                lAllJobNames.add(jobKey.getName());
            }
        } catch (SchedulerException e) {
            logger.error("COULD NOT GET JOB NAMES!!!" + e);
        }
        Collections.sort(lAllJobNames);

        return lAllJobNames;
    }

    /**
     * Generates a Map of all Job names with corresponding Triggers
     * 
     * @return
     */
    public static Map<String, List<Trigger>> getAllJobsAndTriggers() {

        Map<String, List<Trigger>> lAllJobsMap = new TreeMap<String, List<Trigger>>();
        try {
            Set<JobKey> allJobKeys = getScheduler().getJobKeys(null);
            for (JobKey lJobKey : allJobKeys) {
                List<Trigger> lTriggers = (List<Trigger>) getScheduler().getTriggersOfJob(lJobKey);
                lAllJobsMap.put(lJobKey.getName(), lTriggers);
            }

        } catch (SchedulerException e) {
            logger.error("COULD NOT GET JOB NAMES!!!" + e);
        }
        return lAllJobsMap;
    }

    public static boolean isJobRunning(String pJobName) {

        try {
            List<JobExecutionContext> currentlyExecutingJobs = getScheduler().getCurrentlyExecutingJobs();
            for (JobExecutionContext lJobExecutionContext : currentlyExecutingJobs) {
                String currentlyExecutingJobName = lJobExecutionContext.getJobDetail().getKey().getName();
                if (currentlyExecutingJobName.equals(pJobName)) {
                    logger.debug("Matching running Job found!");
                    return true;
                }
            }
        } catch (SchedulerException e) {
            logger.error("ERROR CHECKING RUNNING JOB!!!" + e);
        }
        logger.debug("Matching running NOT Job found!");

        return false;
    }

    /**
     * Halts the Scheduler's firing of Triggers, and cleans up all resources associated with the Scheduler.
     */
    public static void shutdown() {

        logger.debug("shutdown() called.");

        try {
            getScheduler().shutdown(true);
        } catch (Exception e) {
            logger.error("COULD NOT SHUTDOWN SCHEDULER!!!", e);
        }
    }
}
