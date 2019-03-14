package org.quartz.plugins.xml;

import org.quartz.core.Scheduler;
import org.quartz.exceptions.SchedulerException;
import org.quartz.plugins.SchedulerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This plugin loads XML file(s) to add jobs and schedule them with triggers as the scheduler is
 * initialized, and can optionally periodically scan the file for changes.
 *
 * @author James House
 * @author Pierre Awaragi
 * @author pl47ypus
 */
public class XMLSchedulingDataProcessorPlugin implements SchedulerPlugin {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private Scheduler scheduler;

  private boolean failOnFileNotFound = true;

  private String fileName = XMLSchedulingDataProcessor.QUARTZ_XML_DEFAULT_FILE_NAME;

  /**
   * Constructor
   *
   * @param classLoadHelper
   */
  public XMLSchedulingDataProcessorPlugin() {}

  /** Get this plugin's <code>Scheduler</code>. Set as part of initialize(). */
  private Scheduler getScheduler() {

    return scheduler;
  }

  /** Comma separated list of file names (with paths) to the XML files that should be read. */
  public String getFileNames() {

    return fileName;
  }

  /**
   * Whether or not initialization of the plugin should fail (throw an exception) if the file cannot
   * be found. Default is <code>true</code>.
   */
  public boolean isFailOnFileNotFound() {

    return failOnFileNotFound;
  }

  /**
   * Whether or not initialization of the plugin should fail (throw an exception) if the file cannot
   * be found. Default is <code>true</code>.
   */
  public void setFailOnFileNotFound(boolean failOnFileNotFound) {

    this.failOnFileNotFound = failOnFileNotFound;
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ SchedulerPlugin Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * Called during creation of the <code>Scheduler</code> in order to give the <code>SchedulerPlugin
   * </code> a chance to initialize.
   *
   * @throws org.quartz.exceptions.SchedulerConfigException if there is an error initializing.
   */
  @Override
  public void initialize(String name, final Scheduler scheduler) throws SchedulerException {

    logger.info("Initializing XMLSchedulingDataProcessorPlugin Plug-in.");

    this.scheduler = scheduler;
  }

  @Override
  public void start() {

    try {
      XMLSchedulingDataProcessor processor =
          new XMLSchedulingDataProcessor(scheduler.getCascadingClassLoadHelper());
      processor.processFile(
          XMLSchedulingDataProcessor.QUARTZ_XML_DEFAULT_FILE_NAME, failOnFileNotFound);
      processor.scheduleJobs(getScheduler());

    } catch (Exception e) {
      logger.error("Error scheduling jobs: " + e.getMessage(), e);
    }
  }

  /**
   * Overridden to ignore <em>wrapInUserTransaction</em> because shutdown() does not interact with
   * the <code>Scheduler</code>.
   */
  @Override
  public void shutdown() {

    // Since we have nothing to do, override base shutdown so don't
    // get extraneous UserTransactions.
  }
}
