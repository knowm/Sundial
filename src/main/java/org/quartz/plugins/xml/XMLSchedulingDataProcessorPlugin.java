/* 
 * Copyright 2001-2010 Terracotta, Inc. 
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

package org.quartz.plugins.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import org.quartz.Scheduler;
import org.quartz.exceptions.SchedulerException;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerPlugin;
import org.quartz.xml.XMLSchedulingDataProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This plugin loads XML file(s) to add jobs and schedule them with triggers as the scheduler is initialized, and can optionally periodically scan the file for changes.
 * <p>
 * The XML schema definition can be found here: http://www.quartz-scheduler.org/xml/job_scheduling_data_2_0.xsd
 * </p>
 * 
 * @see org.quartz.xml.XMLSchedulingDataProcessor
 * @author James House
 * @author Pierre Awaragi
 * @author pl47ypus
 */
public class XMLSchedulingDataProcessorPlugin implements SchedulerPlugin {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private String name;

  private Scheduler scheduler;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */
  private static final String JOB_INITIALIZATION_PLUGIN_NAME = "JobSchedulingDataLoaderPlugin";

  private boolean failOnFileNotFound = true;

  private String mFileName = XMLSchedulingDataProcessor.QUARTZ_XML_DEFAULT_FILE_NAME;

  private JobFile jobFile;

  private long scanInterval = 0;

  private boolean started = false;

  private ClassLoadHelper classLoadHelper = null;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  public XMLSchedulingDataProcessorPlugin() {

  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

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

    return mFileName;
  }

  /**
   * The interval (in seconds) at which to scan for changes to the file. If the file has been changed, it is re-loaded and parsed. The default value for the interval is 0, which disables scanning.
   * 
   * @return Returns the scanInterval.
   */
  public long getScanInterval() {

    return scanInterval / 1000;
  }

  /**
   * The interval (in seconds) at which to scan for changes to the file. If the file has been changed, it is re-loaded and parsed. The default value for the interval is 0, which disables scanning.
   * 
   * @param scanInterval The scanInterval to set.
   */
  public void setScanInterval(long pScanInterval) {

    scanInterval = pScanInterval * 1000;
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
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ SchedulerPlugin Interface. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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

    this.name = name;
    this.scheduler = scheduler;

    classLoadHelper = new CascadingClassLoadHelper();
    classLoadHelper.initialize();

    jobFile = new JobFile(XMLSchedulingDataProcessor.QUARTZ_XML_DEFAULT_FILE_NAME);

    log.info("Initializing XMLSchedulingDataProcessorPlugin Plug-in.");

  }

  @Override
  public void start() {

    processFile(jobFile);

    started = true;
  }

  /**
   * Overridden to ignore <em>wrapInUserTransaction</em> because shutdown() does not interact with the <code>Scheduler</code>.
   */
  @Override
  public void shutdown() {

    // Since we have nothing to do, override base shutdown so don't
    // get extraneous UserTransactions.
  }

  private void processFile(JobFile jobFile) {

    if (jobFile == null || !jobFile.getFileFound()) {
      return;
    }

    try {
      XMLSchedulingDataProcessor processor = new XMLSchedulingDataProcessor(this.classLoadHelper);

      processor.addJobGroupToNeverDelete(JOB_INITIALIZATION_PLUGIN_NAME);
      processor.addTriggerGroupToNeverDelete(JOB_INITIALIZATION_PLUGIN_NAME);

      processor.processFileAndScheduleJobs(jobFile.getFileName(), jobFile.getFileName(), // systemId
          getScheduler());
    } catch (Exception e) {
      log.error("Error scheduling jobs: " + e.getMessage(), e);
    }
  }

  private class JobFile {

    private String fileName;

    // These are set by initialize()
    private String filePath;
    private String fileBasename;
    private boolean fileFound;

    protected JobFile(String fileName) throws SchedulerException {

      this.fileName = fileName;
      initialize();
    }

    protected String getFileName() {

      return fileName;
    }

    protected boolean getFileFound() {

      return fileFound;
    }

    private void initialize() throws SchedulerException {

      InputStream f = null;
      try {
        String furl = null;

        File file = new File(getFileName()); // files in filesystem
        if (!file.exists()) {
          URL url = classLoadHelper.getResource(getFileName());
          if (url != null) {
            try {
              furl = URLDecoder.decode(url.getPath(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
              furl = url.getPath();
            }
            file = new File(furl);
            try {
              f = url.openStream();
            } catch (IOException ignor) {
              // Swallow the exception
            }
          }
        }
        else {
          try {
            f = new java.io.FileInputStream(file);
          } catch (FileNotFoundException e) {
            // ignore
          }
        }

        if (f == null) {
          if (isFailOnFileNotFound()) {
            throw new SchedulerException("File named '" + getFileName() + "' does not exist.");
          }
          else {
            log.warn("File named '" + getFileName() + "' does not exist. This is OK if you don't want to use an XML job config file.");
          }
        }
        else {
          fileFound = true;
          filePath = (furl != null) ? furl : file.getAbsolutePath();
          fileBasename = file.getName();
        }
      } finally {
        try {
          if (f != null) {
            f.close();
          }
        } catch (IOException ioe) {
          log.warn("Error closing jobs file " + getFileName(), ioe);
        }
      }
    }
  }

}
