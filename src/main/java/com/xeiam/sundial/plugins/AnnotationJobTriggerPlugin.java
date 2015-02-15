package com.xeiam.sundial.plugins;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.text.ParseException;
import java.util.Set;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.exceptions.SchedulerException;
import org.quartz.spi.SchedulerPlugin;
import org.quartz.utils.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xeiam.sundial.Job;
import com.xeiam.sundial.annotations.CronTrigger;

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

      for (Class<? extends Job> scheduledClass : scheduledClasses) {
        CronTrigger cronTrigger = scheduledClass.getAnnotation(CronTrigger.class);
        if (cronTrigger != null) {

          JobDataMap jobDataMap = new JobDataMap();

          if (cronTrigger.jobDataMap() != null && cronTrigger.jobDataMap().length > 0) {
            addToJobDataMap(jobDataMap, cronTrigger.jobDataMap());
          }

          JobDetail job = newJob(scheduledClass).withIdentity(scheduledClass.getSimpleName(), Key.DEFAULT_GROUP).usingJobData(jobDataMap).build();
          Trigger trigger;
          try {
            trigger = buildTrigger(cronTrigger, scheduledClass.getSimpleName());
            scheduler.scheduleJob(job, trigger);
            logger.info("Scheduled job {} with trigger {}", job, trigger);
          } catch (Exception e) {
            logger.warn("ANNOTATED JOB+TRIGGER NOT ADDED!", e);
          }

        }
      }
    } else {
      logger.info("Not loading any annotated Jobs. No package name provided. Use SundialJobScheduler.createScheduler() to set the package name.");
    }

  }

  public Trigger buildTrigger(CronTrigger cronTrigger, String jobName) throws ParseException {

    TriggerBuilder<Trigger> trigger = newTrigger();

    if (cronTrigger.cron() != null && cronTrigger.cron().trim().length() > 0) {
      trigger.forJob(jobName, Key.DEFAULT_GROUP).withIdentity(jobName + "-Trigger", Key.DEFAULT_GROUP)
      .withSchedule(CronScheduleBuilder.cronSchedule(cronTrigger.cron()));
    } else {
      throw new IllegalArgumentException("One of 'cron', 'interval' is required for the @Scheduled annotation");
    }

    return trigger.build();
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
