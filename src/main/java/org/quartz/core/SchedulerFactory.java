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
package org.quartz.core;

import org.knowm.sundial.plugins.AnnotationJobTriggerPlugin;
import org.quartz.QuartzScheduler;
import org.quartz.exceptions.SchedulerException;
import org.quartz.plugins.management.ShutdownHookPlugin;
import org.quartz.plugins.xml.XMLSchedulingDataProcessorPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * An implementation of <code>{@link org.quartz.core.SchedulerFactory}</code> that does all of its work of creating a <code>QuartzScheduler</code>
 * instance.
 * </p>
 *
 * @author James House
 * @author Anthony Eden
 * @author Mohammad Rezaei
 * @author timmolter
 */
public class SchedulerFactory {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private QuartzScheduler quartzScheduler = null;

  private int threadPoolSize = 10; // default size is 10
  private String packageName = null;

  /**
   * @param threadPoolSize
   * @param String packageName
   * @return Returns a handle to the Scheduler produced by this factory. Initialized with given threadPoolSize and packageName where it looks for
   *         annotated Job classes
   * @throws SchedulerException
   */
  public Scheduler getScheduler(int threadPoolSize, String packageName) throws SchedulerException {

    this.threadPoolSize = threadPoolSize;
    this.packageName = packageName;

    return getScheduler();
  }

  /**
   * @param threadPoolSize
   * @return Returns a handle to the Scheduler produced by this factory. Initialized with given threadPoolSize
   * @throws SchedulerException
   */
  public Scheduler getScheduler(int threadPoolSize) throws SchedulerException {

    this.threadPoolSize = threadPoolSize;

    return getScheduler();
  }

  /**
   * <p>
   * Returns a handle to the Scheduler produced by this factory.
   * </p>
   * <p>
   * If one of the <code>initialize</code> methods has not be previously called, then the default (no-arg) <code>initialize()</code> method will be
   * called by this method.
   * </p>
   */
  public Scheduler getScheduler() throws SchedulerException {

    if (quartzScheduler != null) {
      return quartzScheduler;
    }

    return instantiate();
  }

  private Scheduler instantiate() throws SchedulerException {

    // Setup SimpleThreadPool
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //
    SimpleThreadPool threadPool = new SimpleThreadPool();
    threadPool.setThreadCount(threadPoolSize);

    // Setup RAMJobStore
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //
    JobStore jobstore = new RAMJobStore();

    // Set up any TriggerListeners
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    boolean tpInited = false;
    boolean qsInited = false;

    // Fire everything up
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    try {

      JobRunShellFactory jrsf = new StandardJobRunShellFactory(); // Create correct run-shell factory...

      QuartzSchedulerResources quartzSchedulerResources = new QuartzSchedulerResources();
      quartzSchedulerResources.setThreadName("Quartz Scheduler Thread");
      quartzSchedulerResources.setJobRunShellFactory(jrsf);
      quartzSchedulerResources.setMakeSchedulerThreadDaemon(false);
      quartzSchedulerResources.setThreadsInheritInitializersClassLoadContext(false);
      quartzSchedulerResources.setBatchTimeWindow(0L);
      quartzSchedulerResources.setMaxBatchSize(1);
      quartzSchedulerResources.setInterruptJobsOnShutdown(true);
      quartzSchedulerResources.setInterruptJobsOnShutdownWithWait(true);
      quartzSchedulerResources.setThreadPool(threadPool);
      threadPool.setThreadNamePrefix("Quartz_Scheduler_Worker");
      threadPool.initialize();
      tpInited = true;

      quartzSchedulerResources.setJobStore(jobstore);

      quartzScheduler = new QuartzScheduler(quartzSchedulerResources);
      qsInited = true;

      // Set up any SchedulerPlugins
      // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
      XMLSchedulingDataProcessorPlugin xmlSchedulingDataProcessorPlugin = new XMLSchedulingDataProcessorPlugin();
      xmlSchedulingDataProcessorPlugin.setFailOnFileNotFound(false);
      xmlSchedulingDataProcessorPlugin.setScanInterval(0);
      quartzSchedulerResources.addSchedulerPlugin(xmlSchedulingDataProcessorPlugin);

      ShutdownHookPlugin shutdownHookPlugin = new ShutdownHookPlugin();
      quartzSchedulerResources.addSchedulerPlugin(shutdownHookPlugin);

      AnnotationJobTriggerPlugin annotationJobTriggerPlugin = new AnnotationJobTriggerPlugin(packageName);
      quartzSchedulerResources.addSchedulerPlugin(annotationJobTriggerPlugin);

      // fire up job store, and runshell factory
      jobstore.initialize(quartzScheduler.getSchedulerSignaler());
      jobstore.setThreadPoolSize(threadPool.getPoolSize());

      // Initialize plugins now that we have a Scheduler instance.
      xmlSchedulingDataProcessorPlugin.initialize("XMLSchedulingDataProcessorPlugin", quartzScheduler);
      shutdownHookPlugin.initialize("ShutdownHookPlugin", quartzScheduler);
      annotationJobTriggerPlugin.initialize("AnnotationJobTriggerPlugin", quartzScheduler);

      jrsf.initialize(quartzScheduler);

      quartzScheduler.initialize(); // starts the thread

      return quartzScheduler;

    } catch (SchedulerException e) {
      if (qsInited) {
        quartzScheduler.shutdown(false);
      } else if (tpInited) {
        threadPool.shutdown(false);
      }
      throw e;
    } catch (RuntimeException re) {
      if (qsInited) {
        quartzScheduler.shutdown(false);
      } else if (tpInited) {
        threadPool.shutdown(false);
      }
      throw re;
    } catch (Error re) {
      if (qsInited) {
        quartzScheduler.shutdown(false);
      } else if (tpInited) {
        threadPool.shutdown(false);
      }
      throw re;
    }
  }
}
