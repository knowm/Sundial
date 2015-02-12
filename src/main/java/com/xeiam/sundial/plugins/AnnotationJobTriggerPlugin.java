package com.xeiam.sundial.plugins;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.text.ParseException;
import java.util.Set;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.exceptions.SchedulerException;
import org.quartz.spi.SchedulerPlugin;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xeiam.sundial.Job;
import com.xeiam.sundial.Triggered;

/**
 * This plugin adds jobs and schedules them with triggers from annotated Job classes as the scheduler is initialized.
 *
 * @author timmolter
 */
public class AnnotationJobTriggerPlugin implements SchedulerPlugin {

  private final Logger logger = LoggerFactory.getLogger(AnnotationJobTriggerPlugin.class);

  private Scheduler scheduler;

  private static final String JOB_INITIALIZATION_PLUGIN_NAME = "AnnotationJobTriggerPlugin";

  private final String packageName;

  /**
   * Constructor
   *
   * @param packageName
   */
  public AnnotationJobTriggerPlugin(String packageName) {

    this.packageName = packageName;

  }

  /**
   * Get this plugin's <code>Scheduler</code>. Set as part of initialize().
   */
  private Scheduler getScheduler() {

    return scheduler;
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ SchedulerPlugin Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

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

      Reflections reflections = new Reflections(packageName, new SubTypesScanner());
      Set<Class<? extends Job>> scheduledClasses = reflections.getSubTypesOf(Job.class);

      for (Class<? extends Job> scheduledClass : scheduledClasses) {
        Triggered scheduleAnn = scheduledClass.getAnnotation(Triggered.class);
        if (scheduleAnn != null) {
          JobDetail job = newJob(scheduledClass).build();
          Trigger trigger;
          try {
            trigger = buildTrigger(scheduleAnn);
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

    //    try {
    //      XMLSchedulingDataProcessor processor = new XMLSchedulingDataProcessor();
    //      processor.addJobGroupToNeverDelete(JOB_INITIALIZATION_PLUGIN_NAME);
    //      processor.addTriggerGroupToNeverDelete(JOB_INITIALIZATION_PLUGIN_NAME);
    //      processor.processFile(XMLSchedulingDataProcessor.QUARTZ_XML_DEFAULT_FILE_NAME, failOnFileNotFound);
    //      processor.scheduleJobs(getScheduler());
    //
    //    } catch (Exception e) {
    //      logger.error("Error scheduling jobs: " + e.getMessage(), e);
    //    }
  }

  public static Trigger buildTrigger(Triggered ann) throws ParseException {
    TriggerBuilder<Trigger> trigger = newTrigger();

    if (ann.cron() != null && ann.cron().trim().length() > 0) {
      trigger.withSchedule(CronScheduleBuilder.cronSchedule(ann.cron()));
    } else {
      throw new IllegalArgumentException("One of 'cron', 'interval' is required for the @Scheduled annotation");
    }

    return trigger.build();
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
