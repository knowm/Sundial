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
package org.quartz.plugins.xml;

import org.quartz.core.Scheduler;
import org.quartz.exceptions.SchedulerException;
import org.quartz.plugins.SchedulerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This plugin loads XML file(s) to add jobs and schedule them with triggers as the scheduler is initialized, and can optionally periodically scan the
 * file for changes.
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

  private long scanInterval = 0;

  /**
   * Constructor
   *
   * @param classLoadHelper
   */
  public XMLSchedulingDataProcessorPlugin() {

  }

  /**
   * Get this plugin's <code>Scheduler</code>. Set as part of initialize().
   */
  private Scheduler getScheduler() {

    return scheduler;
  }

  /**
   * Comma separated list of file names (with paths) to the XML files that should be read.
   */
  public String getFileNames() {

    return fileName;
  }

  /**
   * The interval (in seconds) at which to scan for changes to the file. If the file has been changed, it is re-loaded and parsed. The default value
   * for the interval is 0, which disables scanning.
   *
   * @return Returns the scanInterval.
   */
  public long getScanInterval() {

    return scanInterval / 1000;
  }

  /**
   * The interval (in seconds) at which to scan for changes to the file. If the file has been changed, it is re-loaded and parsed. The default value
   * for the interval is 0, which disables scanning.
   *
   * @param scanInterval The scanInterval to set.
   */
  public void setScanInterval(long scanInterval) {

    this.scanInterval = scanInterval * 1000;
  }

  /**
   * Whether or not initialization of the plugin should fail (throw an exception) if the file cannot be found. Default is <code>true</code>.
   */
  public boolean isFailOnFileNotFound() {

    return failOnFileNotFound;
  }

  /**
   * Whether or not initialization of the plugin should fail (throw an exception) if the file cannot be found. Default is <code>true</code>.
   */
  public void setFailOnFileNotFound(boolean failOnFileNotFound) {

    this.failOnFileNotFound = failOnFileNotFound;
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

    logger.info("Initializing XMLSchedulingDataProcessorPlugin Plug-in.");

    this.scheduler = scheduler;
  }

  @Override
  public void start() {

    try {
      XMLSchedulingDataProcessor processor = new XMLSchedulingDataProcessor(scheduler.getCascadingClassLoadHelper());
      processor.processFile(XMLSchedulingDataProcessor.QUARTZ_XML_DEFAULT_FILE_NAME, failOnFileNotFound);
      processor.scheduleJobs(getScheduler());

    } catch (Exception e) {
      logger.error("Error scheduling jobs: " + e.getMessage(), e);
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
