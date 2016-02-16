/**
 * Copyright 2015 Knowm Inc. (http://knowm.org) and contributors.
 * Copyright 2013-2015 Xeiam LLC (http://xeiam.com) and contributors.
 * Copyright 2001-2011 Terracotta Inc. (http://terracotta.org).
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
package org.knowm.sundial;

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

import org.knowm.sundial.exceptions.SundialSchedulerException;
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
  public static void startScheduler() throws SundialSchedulerException {

    startScheduler(10, null);
  }

  /**
   * Starts the Sundial Scheduler
   *
   * @param threadPoolSize
   */
  public static void startScheduler(int threadPoolSize) throws SundialSchedulerException {

    startScheduler(threadPoolSize, null);
  }

  /**
   * Starts the Sundial Scheduler
   *
   * @param annotatedJobsPackageName
   */
  public static void startScheduler(String annotatedJobsPackageName) throws SundialSchedulerException {

    startScheduler(10, annotatedJobsPackageName);
  }

  /**
   * Starts the Sundial Scheduler
   *
   * @param threadPoolSize
   * @param annotatedJobsPackageName
   */
  public static void startScheduler(int threadPoolSize, String annotatedJobsPackageName) throws SundialSchedulerException {

    try {
      createScheduler(threadPoolSize, annotatedJobsPackageName);
      getScheduler().start();
    } catch (SchedulerException e) {
      throw new SundialSchedulerException("COULD NOT START SUNDIAL SCHEDULER!!!", e);
    }
  }

  /**
   * Creates the Sundial Scheduler
   *
   * @param threadPoolSize the thread pool size used by the scheduler
   * @param annotatedJobsPackageName the package where trigger annotated Job calsses can be found
   * @return
   */
  public static Scheduler createScheduler(int threadPoolSize, String annotatedJobsPackageName) throws SundialSchedulerException {

    if (scheduler == null) {
      try {
        scheduler = new SchedulerFactory().getScheduler(threadPoolSize, annotatedJobsPackageName);

      } catch (SchedulerException e) {
        throw new SundialSchedulerException("COULD NOT CREATE SUNDIAL SCHEDULER!!!", e);
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
   * @param jobClass
   */
  public static void addJob(String jobName, Class<? extends Job> jobClass) throws SundialSchedulerException {

    addJob(jobName, jobClass, null, false);

  }

  /**
   * Adds a Job to the scheduler. Replaces a matching existing Job.
   *
   * @param jobName
   * @param jobClassName
   */
  public static void addJob(String jobName, String jobClassName) throws SundialSchedulerException {

    addJob(jobName, jobClassName, null, false);

  }

  /**
   * Adds a Job to the scheduler. Replaces a matching existing Job.
   *
   * @param jobName
   * @param jobClass
   * @param params Set this null if there are no params
   * @param isConcurrencyAllowed
   */
  public static void addJob(String jobName, Class<? extends Job> jobClass, Map<String, Object> params, boolean isConcurrencyAllowed)
      throws SundialSchedulerException {

    try {
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
      throw new SundialSchedulerException("ERROR ADDING JOB!!!", e);
    }
  }

  /**
   * Adds a Job to the scheduler. Replaces a matching existing Job.
   *
   * @param jobName
   * @param jobClassName
   * @param params Set this null if there are no params
   * @param isConcurrencyAllowed
   */
  public static void addJob(String jobName, String jobClassName, Map<String, Object> params, boolean isConcurrencyAllowed)
      throws SundialSchedulerException {

    try {
      addJob(jobName, getScheduler().getCascadingClassLoadHelper().loadClass(jobClassName), params, isConcurrencyAllowed);
    } catch (ClassNotFoundException e) {
      throw new SundialSchedulerException("ERROR ADDING JOB!!!", e);
    }
  }

  /**
   * Starts a Job matching the given Job Name
   *
   * @param jobName
   */
  public static void startJob(String jobName) throws SundialSchedulerException {

    try {
      getScheduler().triggerJob(jobName, null);
    } catch (SchedulerException e) {
      throw new SundialSchedulerException("ERROR STARTING JOB!!!", e);
    }
  }

  /**
   * Removes a Job matching the given Job Name
   *
   * @param jobName
   */
  public static void removeJob(String jobName) throws SundialSchedulerException {

    try {
      getScheduler().deleteJob(jobName);
    } catch (SchedulerException e) {
      throw new SundialSchedulerException("ERROR REMOVING JOB!!!", e);
    }

  }

  /**
   * Starts a Job matching the the given Job Name found in jobs.xml
   *
   * @param jobName
   */
  public static void startJob(String jobName, Map<String, Object> params) throws SundialSchedulerException {

    try {

      JobDataMap jobDataMap = new JobDataMap();
      for (String key : params.keySet()) {
        // logger.debug("key= " + key);
        // logger.debug("value= " + pParams.get(key));
        jobDataMap.put(key, params.get(key));
      }
      getScheduler().triggerJob(jobName, jobDataMap);
    } catch (SchedulerException e) {
      throw new SundialSchedulerException("ERROR STARTING JOB!!!", e);
    }

  }

  /**
   * Triggers a Job interrupt on all Jobs matching the given Job Name
   *
   * @param jobName The job name
   */
  public static void stopJob(String jobName) throws SundialSchedulerException {

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
      throw new SundialSchedulerException("ERROR STOPPING JOB!!!", e);
    }
  }

  /**
   * Triggers a Job interrupt on all Jobs matching the given Job Name, key and (String) value. Doesn't work if the value is not a String.
   *
   * @param jobName The job name
   * @param key The key in the job data map
   * @param pValue The value in the job data map
   */
  public static void stopJob(String jobName, String key, String pValue) throws SundialSchedulerException {

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
      throw new SundialSchedulerException("ERROR DURING STOP JOB!!!", e);
    }
  }

  // TRIGGERS /////////////////////////////////////////////

  /**
   * @param triggerName
   * @param jobName
   * @param cronExpression
   */
  public static void addCronTrigger(String triggerName, String jobName, String cronExpression) throws SundialSchedulerException {
    addCronTrigger(triggerName, jobName, cronExpression, null, null);
  }

  /**
   * @param triggerName
   * @param jobName
   * @param cronExpression
   * @param startTime - Trigger will NOT fire before this time, regardless of the Trigger's schedule.
   * @param endTime - Set the time at which the Trigger will no longer fire - even if it's schedule has remaining repeats. If null, the end time is
   *        indefinite.
   */
  public static void addCronTrigger(String triggerName, String jobName, String cronExpression, Date startTime, Date endTime)
      throws SundialSchedulerException {

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
      throw new SundialSchedulerException("ERROR ADDING CRON TRIGGER!!!", e);
    } catch (ParseException e) {
      throw new SundialSchedulerException("ERROR ADDING CRON TRIGGER!!!", e);
    }
  }

  /**
   * @param triggerName
   * @param jobName
   * @param repeatCount - set to -1 to repeat indefinitely
   * @param repeatInterval
   */
  public static void addSimpleTrigger(String triggerName, String jobName, int repeatCount, long repeatInterval) throws SundialSchedulerException {
    addSimpleTrigger(triggerName, jobName, repeatCount, repeatInterval, null, null);
  }

  /**
   * @param triggerName
   * @param jobName
   * @param repeatCount - set to -1 to repeat indefinitely
   * @param repeatInterval
   * @param startTime
   * @param endTime
   */
  public static void addSimpleTrigger(String triggerName, String jobName, int repeatCount, long repeatInterval, Date startTime, Date endTime)
      throws SundialSchedulerException {

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
      throw new SundialSchedulerException("ERROR ADDING CRON TRIGGER!!!", e);
    }
  }

  /**
   * Removes a Trigger matching the the given Trigger Name
   *
   * @param triggerName
   */
  public static void removeTrigger(String triggerName) throws SundialSchedulerException {

    try {
      getScheduler().unscheduleJob(triggerName);
    } catch (SchedulerException e) {
      throw new SundialSchedulerException("ERROR REMOVING TRIGGER!!!", e);
    }
  }

  /**
   * Generates an alphabetically sorted List of all Job names in the DEFAULT job group
   *
   * @return
   */
  public static List<String> getAllJobNames() throws SundialSchedulerException {

    List<String> allJobNames = new ArrayList<String>();
    try {
      Set<String> allJobKeys = getScheduler().getJobKeys();
      for (String jobKey : allJobKeys) {
        allJobNames.add(jobKey);
      }
    } catch (SchedulerException e) {
      throw new SundialSchedulerException("COULD NOT GET JOB NAMES!!!", e);
    }
    Collections.sort(allJobNames);

    return allJobNames;
  }

  /**
   * Generates a Map of all Job names with corresponding Triggers
   *
   * @return
   */
  public static Map<String, List<Trigger>> getAllJobsAndTriggers() throws SundialSchedulerException {

    Map<String, List<Trigger>> allJobsMap = new TreeMap<String, List<Trigger>>();
    try {
      Set<String> allJobKeys = getScheduler().getJobKeys();
      for (String jobKey : allJobKeys) {
        List<Trigger> triggers = getScheduler().getTriggersOfJob(jobKey);
        allJobsMap.put(jobKey, triggers);
      }

    } catch (SchedulerException e) {
      throw new SundialSchedulerException("COULD NOT GET JOB NAMES!!!", e);
    }
    return allJobsMap;
  }

  public static boolean isJobRunning(String jobName) throws SundialSchedulerException {

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
      throw new SundialSchedulerException("ERROR CHECKING RUNNING JOB!!!", e);
    }
    logger.debug("Matching running NOT Job found!");

    return false;
  }

  /**
   * Halts the Scheduler's firing of Triggers, and cleans up all resources associated with the Scheduler.
   */
  public static void shutdown() throws SundialSchedulerException {

    logger.debug("shutdown() called.");

    try {
      getScheduler().shutdown(true);
      scheduler = null;
    } catch (Exception e) {
      throw new SundialSchedulerException("COULD NOT SHUTDOWN SCHEDULER!!!", e);
    }
  }
}
