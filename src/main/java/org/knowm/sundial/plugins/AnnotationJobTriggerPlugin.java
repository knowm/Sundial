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
package org.knowm.sundial.plugins;

import static org.quartz.builders.CronTriggerBuilder.cronTriggerBuilder;
import static org.quartz.builders.JobBuilder.newJobBuilder;
import static org.quartz.builders.SimpleTriggerBuilder.simpleTriggerBuilder;

import java.text.ParseException;
import java.util.Set;
import java.util.TimeZone;

import org.knowm.sundial.Job;
import org.knowm.sundial.annotations.CronTrigger;
import org.knowm.sundial.annotations.SimpleTrigger;
import org.quartz.core.Scheduler;
import org.quartz.exceptions.SchedulerException;
import org.quartz.jobs.JobDataMap;
import org.quartz.jobs.JobDetail;
import org.quartz.plugins.SchedulerPlugin;
import org.quartz.triggers.OperableTrigger;
import org.quartz.triggers.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This plugin adds jobs and schedules them with triggers from annotated Job classes as the scheduler is initialized.
 *
 * @author timmolter
 */
public class AnnotationJobTriggerPlugin implements SchedulerPlugin {

  private final Logger logger = LoggerFactory.getLogger(AnnotationJobTriggerPlugin.class);

  private Scheduler scheduler;

  private final String packageName;

  private static final String SEPARATOR = ":";

  /**
   * Constructor
   *
   * @param packageName
   */
  public AnnotationJobTriggerPlugin(String packageName) {

    this.packageName = packageName;
  }

  /**
   * <p>
   * Called during creation of the <code>Scheduler</code> in order to give the <code>SchedulerPlugin</code> a chance to initialize.
   * </p>
   *
   * @throws org.quartz.exceptions.SchedulerConfigException if there is an error initializing.
   */
  @Override
  public void initialize(String name, final Scheduler scheduler) throws SchedulerException {

    logger.info("Initializing AnnotationJobTriggerPlugin Plug-in.");

    this.scheduler = scheduler;
  }

  @Override
  public void start() {

    logger.info("Loading annotated jobs from {}.", packageName);

    if (packageName != null) {

      Set<Class<? extends Job>> scheduledClasses = scheduler.getCascadingClassLoadHelper().getJobClasses(packageName);

      for (Class<? extends Job> jobClass : scheduledClasses) {

        CronTrigger cronTrigger = jobClass.getAnnotation(CronTrigger.class);
        if (cronTrigger != null) {

          JobDataMap jobDataMap = new JobDataMap();

          if (cronTrigger.jobDataMap() != null && cronTrigger.jobDataMap().length > 0) {
            addToJobDataMap(jobDataMap, cronTrigger.jobDataMap());
          }

          JobDetail jobDetail = newJobBuilder(jobClass).withIdentity(jobClass.getSimpleName())
              .isConcurrencyAllowed(cronTrigger.isConcurrencyAllowed()).usingJobData(jobDataMap).build();
          OperableTrigger trigger;
          try {
            trigger = buildCronTrigger(cronTrigger, jobClass.getSimpleName());
            scheduler.scheduleJob(jobDetail, trigger);
            logger.info("Scheduled job: {} with trigger: {}", jobDetail, trigger);
          } catch (Exception e) {
            logger.warn("ANNOTATED JOB + TRIGGER NOT ADDED!", e);
          }
        }
        SimpleTrigger simpleTrigger = jobClass.getAnnotation(SimpleTrigger.class);
        if (simpleTrigger != null) {

          JobDataMap jobDataMap = new JobDataMap();

          if (simpleTrigger.jobDataMap() != null && simpleTrigger.jobDataMap().length > 0) {
            addToJobDataMap(jobDataMap, simpleTrigger.jobDataMap());
          }

          JobDetail job = newJobBuilder(jobClass).withIdentity(jobClass.getSimpleName()).isConcurrencyAllowed(simpleTrigger.isConcurrencyAllowed())
              .usingJobData(jobDataMap).build();
          OperableTrigger trigger;
          try {
            trigger = buildSimpleTrigger(simpleTrigger, jobClass.getSimpleName());
            scheduler.scheduleJob(job, trigger);
            logger.info("Scheduled job {} with trigger {}", job, trigger);
          } catch (Exception e) {
            logger.warn("ANNOTATED JOB + TRIGGER NOT ADDED!", e);
          }
        }
      }
    } else {
      logger.info("Not loading any annotated Jobs. No package name provided. Use SundialJobScheduler.createScheduler() to set the package name.");
    }

  }

  public OperableTrigger buildCronTrigger(CronTrigger cronTrigger, String jobName) throws ParseException {

    if (cronTrigger.cron() != null && cronTrigger.cron().trim().length() > 0) {

      TimeZone tz = (cronTrigger.timeZone() == null || cronTrigger.timeZone().length() < 1) ? null : TimeZone.getTimeZone(cronTrigger.timeZone());

      return cronTriggerBuilder(cronTrigger.cron()).inTimeZone(tz).withIdentity(jobName + "-Trigger").forJob(jobName)
          .withPriority(Trigger.DEFAULT_PRIORITY).build();

    } else {
      throw new IllegalArgumentException("'cron' is required for the @CronTrigger annotation");
    }

  }

  public OperableTrigger buildSimpleTrigger(SimpleTrigger simpleTrigger, String jobName) {

    return simpleTriggerBuilder().withRepeatCount(simpleTrigger.repeatCount())
        .withIntervalInMilliseconds(simpleTrigger.timeUnit().toMillis(simpleTrigger.repeatInterval())).withIdentity(jobName + "-Trigger")
        .forJob(jobName).withPriority(Trigger.DEFAULT_PRIORITY).build();
  }

  private void addToJobDataMap(JobDataMap jobDataMap, String[] stringEncodedMap) {

    for (int i = 0; i < stringEncodedMap.length; i++) {

      String[] keyValue = stringEncodedMap[i].split(SEPARATOR);
      if (keyValue == null || keyValue.length != 2) {
        logger.warn(stringEncodedMap[i] + " was not parsable!!! Skipping.");
        continue;
      }

      jobDataMap.put(keyValue[0].trim(), keyValue[1].trim());
    }

  }

  /**
   * Overridden to ignore <em>wrapInUserTransaction</em> because shutdown() does not interact with the <code>Scheduler</code>.
   */
  @Override
  public void shutdown() {

    // Since we have nothing to do, override base shutdown so don't
    // get extraneous UserTransactions.
  }

}
