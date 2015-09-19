/**
 * Copyright 2011 - 2013 Xeiam LLC.
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

import static org.quartz.builders.CronTriggerBuilder.cronTriggerBuilder;
import static org.quartz.builders.JobBuilder.newJobBuilder;
import static org.quartz.builders.SimpleTriggerBuilder.simpleTriggerBuilder;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletContext;

import org.quartz.builders.CronTriggerBuilder;
import org.quartz.builders.SimpleTriggerBuilder;
import org.quartz.core.JobExecutionContext;
import org.quartz.core.Scheduler;
import org.quartz.core.SchedulerFactory;
import org.quartz.exceptions.SchedulerException;
import org.quartz.jobs.JobDataMap;
import org.quartz.jobs.JobDetail;
import org.quartz.triggers.OperableTrigger;
import org.quartz.triggers.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xeiam.sundial.exceptions.SchedulerStartupException;

/**
 * Main entry-point to the Sundial scheduler
 *
 * @author timmolter
 */
public class SundialJobScheduler {

  /** slf4J logger wrapper */
  static Logger logger = LoggerFactory.getLogger(SundialJobScheduler.class);

  /** Quartz scheduler */
  private static Scheduler scheduler = null;

  /** global lock */
  private static boolean globalLock = false;

  private static ServletContext servletContext = null;

  /**
   * Starts the Sundial Scheduler
   */
  public static void startScheduler() {

    startScheduler(10, null);
  }

  /**
   * Starts the Sundial Scheduler
   *
   * @param threadPoolSize
   */
  public static void startScheduler(int threadPoolSize) {

    startScheduler(threadPoolSize, null);
  }

  /**
   * Starts the Sundial Scheduler
   *
   * @param annotatedJobsPackageName
   */
  public static void startScheduler(String annotatedJobsPackageName) {

    startScheduler(10, annotatedJobsPackageName);
  }

  /**
   * Starts the Sundial Scheduler
   *
   * @param threadPoolSize
   * @param annotatedJobsPackageName
   */
  public static void startScheduler(int threadPoolSize, String annotatedJobsPackageName) {

    try {
      createScheduler(threadPoolSize, annotatedJobsPackageName);
      getScheduler().start();
    } catch (SchedulerException e) {
      logger.error("COULD NOT START SUNDIAL SCHEDULER!!!", e);
      throw new SchedulerStartupException(e);
    }
  }

  /**
   * Creates the Sundial Scheduler
   *
   * @param threadPoolSize the thread pool size used by the scheduler
   * @param annotatedJobsPackageName the package where trigger annotated Job calsses can be found
   * @return
   * @throws SchedulerException 
   */
  public static Scheduler createScheduler(int threadPoolSize, String annotatedJobsPackageName) throws SchedulerException {

    if (scheduler == null) {
      try {
        scheduler = new SchedulerFactory().getScheduler(threadPoolSize, annotatedJobsPackageName);

      } catch (SchedulerException e) {
        logger.error("COULD NOT CREATE SUNDIAL SCHEDULER!!!", e);
        throw e;
      }
    }
    return scheduler;
  }

  /**
   * Gets the underlying Quartz scheduler
   *
   * @return
   */
  public static Scheduler getScheduler() {

    if (scheduler == null) {
      logger.warn("Scheduler has not yet been created!!! Call \"createScheduler\" first.");
    }
    return scheduler;
  }

  public static void toggleGlobalLock() {

    globalLock = !globalLock;
  }

  public static void lockScheduler() {

    globalLock = true;
  }

  public static void unlockScheduler() {

    globalLock = false;
  }

  public static boolean getGlobalLock() {

    return globalLock;
  }

  /**
   * @return the ServletContext
   */
  public static ServletContext getServletContext() {

    return servletContext;
  }

  /**
   * @param servletContext the ServletContext to set
   */
  public static void setServletContext(ServletContext servletContext) {

    SundialJobScheduler.servletContext = servletContext;
  }

  /**
   * Adds a Job to the scheduler. Replaces a matching existing Job.
   *
   * @param jobName
   * @param jobClassName
   * @throws SchedulerException 
   * @throws ClassNotFoundException 
   */
  public static void addJob(String jobName, String jobClassName) throws SchedulerException, ClassNotFoundException {

    addJob(jobName, jobClassName, null, false);

  }

  /**
   * Adds a Job to the scheduler. Replaces a matching existing Job.
   *
   * @param jobName
   * @param jobClassName
   * @param params Set this null if there are no params
   * @param isConcurrencyAllowed
   * @throws SchedulerException 
   * @throws ClassNotFoundException 
   */
  public static void addJob(String jobName, String jobClassName, Map<String, Object> params, boolean isConcurrencyAllowed) throws SchedulerException, ClassNotFoundException {

    try {

      Class<? extends Job> jobClass = getScheduler().getCascadingClassLoadHelper().loadClass(jobClassName);

      JobDataMap jobDataMap = new JobDataMap();
      if (params != null) {
        for (Entry<String, Object> entry : params.entrySet()) {
          jobDataMap.put(entry.getKey(), entry.getValue());
        }
      }

      JobDetail jobDetail = newJobBuilder(jobClass).withIdentity(jobName).usingJobData(jobDataMap).isConcurrencyAllowed(isConcurrencyAllowed).build();

      getScheduler().addJob(jobDetail);

    } catch (SchedulerException e) {
      logger.error("ERROR ADDING JOB!!!", e);
      throw e;
    } catch (ClassNotFoundException e) {
      logger.error("ERROR ADDING JOB!!!", e);
      throw e;
    }
  }

  /**
   * Starts a Job matching the given Job Name
   *
   * @param jobName
   * @throws SchedulerException 
   */
  public static void startJob(String jobName) throws SchedulerException {

    try {
      getScheduler().triggerJob(jobName, null);
    } catch (SchedulerException e) {
      logger.error("ERROR STARTING JOB!!!", e);
      throw e;
    }
  }

  /**
   * Removes a Job matching the given Job Name
   *
   * @param jobName
   * @throws SchedulerException 
   */
  public static void removeJob(String jobName) throws SchedulerException {

    try {
      getScheduler().deleteJob(jobName);
    } catch (SchedulerException e) {
      logger.error("ERROR REMOVING JOB!!!", e);
      throw e;
    }

  }

  /**
   * Starts a Job matching the the given Job Name found in jobs.xml
   *
   * @param jobName
   * @throws SchedulerException 
   */
  public static void startJob(String jobName, Map<String, Object> params) throws SchedulerException {

    try {

      JobDataMap jobDataMap = new JobDataMap();
      for (String key : params.keySet()) {
        // logger.debug("key= " + key);
        // logger.debug("value= " + pParams.get(key));
        jobDataMap.put(key, params.get(key));
      }
      getScheduler().triggerJob(jobName, jobDataMap);
    } catch (SchedulerException e) {
      logger.error("ERROR STARTING JOB!!!", e);
      throw e;
    }

  }

  /**
   * Triggers a Job interrupt on all Jobs matching the given Job Name
   *
   * @param jobName The job name
   * @throws SchedulerException 
   */
  public static void stopJob(String jobName) throws SchedulerException {

    try {
      List<JobExecutionContext> currentlyExecutingJobs = getScheduler().getCurrentlyExecutingJobs();
      for (JobExecutionContext jobExecutionContext : currentlyExecutingJobs) {
        String currentlyExecutingJobName = jobExecutionContext.getJobDetail().getName();
        if (currentlyExecutingJobName.equals(jobName)) {
          logger.debug("Matching Job found. Now Stopping!");
          if (jobExecutionContext.getJobInstance() instanceof Job) {
            ((Job) jobExecutionContext.getJobInstance()).interrupt();
          } else {
            logger.warn("CANNOT STOP NON-INTERRUPTABLE JOB!!!");
          }
        } else {
          logger.debug("Non-matching Job found. Not Stopping!");
        }
      }
    } catch (SchedulerException e) {
      logger.error("ERROR STOPPING JOB!!!", e);
      throw e;
    }
  }

  /**
   * Triggers a Job interrupt on all Jobs matching the given Job Name, key and (String) value. Doesn't work if the value is not a String.
   *
   * @param jobName The job name
   * @param key The key in the job data map
   * @param pValue The value in the job data map
   * @throws SchedulerException 
   */
  public static void stopJob(String jobName, String key, String pValue) throws SchedulerException {

    logger.debug("key= " + key);
    logger.debug("value= " + pValue);
    try {
      List<JobExecutionContext> currentlyExecutingJobs = getScheduler().getCurrentlyExecutingJobs();
      for (JobExecutionContext jobExecutionContext : currentlyExecutingJobs) {
        String currentlyExecutingJobName = jobExecutionContext.getJobDetail().getName();
        if (currentlyExecutingJobName.equals(jobName)) {
          if (jobExecutionContext.getJobInstance() instanceof Job) {
            JobDataMap jobDataMap = jobExecutionContext.getMergedJobDataMap();
            String value = jobDataMap.getString(key);
            if (value != null & value.equalsIgnoreCase(pValue)) {
              ((Job) jobExecutionContext.getJobInstance()).interrupt();
            }
          } else {
            logger.warn("CANNOT STOP NON-INTERRUPTABLE JOB!!!");
          }
        } else {
          logger.debug("Non-matching Job found. Not Stopping!");
        }
      }
    } catch (SchedulerException e) {
      logger.error("ERROR DURING STOP Job!!!", e);
      throw e;
    }
  }

  // TRIGGERS /////////////////////////////////////////////

  /**
   * @param triggerName
   * @param jobName
   * @param cronExpression
   * @throws ParseException 
   * @throws SchedulerException 
   */
  public static void addCronTrigger(String triggerName, String jobName, String cronExpression) throws SchedulerException, ParseException {
    addCronTrigger(triggerName, jobName, cronExpression, null, null);
  }

  /**
   * @param triggerName
   * @param jobName
   * @param cronExpression
   * @param startTime
   * @param endTime
   * @throws SchedulerException 
   * @throws ParseException 
   */
  public static void addCronTrigger(String triggerName, String jobName, String cronExpression, Date startTime, Date endTime) throws SchedulerException, ParseException {

    try {
      CronTriggerBuilder cronTriggerBuilder = cronTriggerBuilder(cronExpression);
      cronTriggerBuilder.withIdentity(triggerName).forJob(jobName).withPriority(Trigger.DEFAULT_PRIORITY);
      if (startTime != null) {
        cronTriggerBuilder.startAt(startTime);
      }
      if (endTime != null) {
        cronTriggerBuilder.endAt(endTime);
      }
      OperableTrigger trigger = cronTriggerBuilder.build();

      getScheduler().scheduleJob(trigger);
    } catch (SchedulerException e) {
      logger.error("ERROR ADDING CRON TRIGGER!!!", e);
      throw e;
    } catch (ParseException e) {
      logger.error("ERROR ADDING CRON TRIGGER!!!", e);
      throw e;
    }
  }

  /**
   * @param triggerName
   * @param jobName
   * @param repeatCount - set to -1 to repeat indefinitely
   * @param repeatInterval
   * @throws SchedulerException 
   */
  public static void addSimpleTrigger(String triggerName, String jobName, int repeatCount, long repeatInterval) throws SchedulerException {
    addSimpleTrigger(triggerName, jobName, repeatCount, repeatInterval, null, null);
  }

  /**
   * @param triggerName
   * @param jobName
   * @param repeatCount - set to -1 to repeat indefinitely
   * @param repeatInterval
   * @param startTime
   * @param endTime
   * @throws SchedulerException 
   */
  public static void addSimpleTrigger(String triggerName, String jobName, int repeatCount, long repeatInterval, Date startTime, Date endTime) throws SchedulerException {

    try {
      SimpleTriggerBuilder simpleTriggerBuilder = simpleTriggerBuilder();
      simpleTriggerBuilder.withRepeatCount(repeatCount).withIntervalInMilliseconds(repeatInterval).withIdentity(triggerName).forJob(jobName);
      if (startTime != null) {
        simpleTriggerBuilder.startAt(startTime);
      }
      if (endTime != null) {
        simpleTriggerBuilder.endAt(endTime);
      }
      OperableTrigger trigger = simpleTriggerBuilder.build();

      getScheduler().scheduleJob(trigger);

    } catch (SchedulerException e) {
      logger.error("ERROR ADDING CRON TRIGGER!!!", e);
      throw e;
    }
  }

  /**
   * Removes a Trigger matching the the given Trigger Name
   *
   * @param triggerName
   */
  public static void removeTrigger(String triggerName) {

    try {
      getScheduler().unscheduleJob(triggerName);
    } catch (SchedulerException e) {
      logger.error("ERROR REMOVING TRIGGER!!!", e);
    }
  }

  /**
   * Generates an alphabetically sorted List of all Job names in the DEFAULT job group
   *
   * @return
   */
  public static List<String> getAllJobNames() {

    List<String> allJobNames = new ArrayList<String>();
    try {
      Set<String> allJobKeys = getScheduler().getJobKeys();
      for (String jobKey : allJobKeys) {
        allJobNames.add(jobKey);
      }
    } catch (SchedulerException e) {
      logger.error("COULD NOT GET JOB NAMES!!!", e);
    }
    Collections.sort(allJobNames);

    return allJobNames;
  }

  /**
   * Generates a Map of all Job names with corresponding Triggers
   *
   * @return
   */
  public static Map<String, List<Trigger>> getAllJobsAndTriggers() {

    Map<String, List<Trigger>> allJobsMap = new TreeMap<String, List<Trigger>>();
    try {
      Set<String> allJobKeys = getScheduler().getJobKeys();
      for (String jobKey : allJobKeys) {
        List<Trigger> triggers = getScheduler().getTriggersOfJob(jobKey);
        allJobsMap.put(jobKey, triggers);
      }

    } catch (SchedulerException e) {
      logger.error("COULD NOT GET JOB NAMES!!!", e);
    }
    return allJobsMap;
  }

  public static boolean isJobRunning(String jobName) {

    try {
      List<JobExecutionContext> currentlyExecutingJobs = getScheduler().getCurrentlyExecutingJobs();
      for (JobExecutionContext jobExecutionContext : currentlyExecutingJobs) {
        String currentlyExecutingJobName = jobExecutionContext.getJobDetail().getName();
        if (currentlyExecutingJobName.equals(jobName)) {
          logger.debug("Matching running Job found!");
          return true;
        }
      }
    } catch (SchedulerException e) {
      logger.error("ERROR CHECKING RUNNING JOB!!!", e);
    }
    logger.debug("Matching running NOT Job found!");

    return false;
  }

  /**
   * Halts the Scheduler's firing of Triggers, and cleans up all resources associated with the Scheduler.
   * @throws Exception 
   */
  public static void shutdown() throws Exception {

    logger.debug("shutdown() called.");

    try {
      getScheduler().shutdown(true);
    } catch (Exception e) {
      logger.error("COULD NOT SHUTDOWN SCHEDULER!!!", e);
      throw e;
    }
  }
}
