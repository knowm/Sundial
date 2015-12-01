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
package org.knowm.sundial.ee;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.knowm.sundial.SundialJobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A ServletContextListner that can be used to initialize Sundial.
 * </p>
 * <p>
 * The init parameter 'quartz:shutdown-on-unload' can be used to specify whether you want scheduler.shutdown() called when the listener is unloaded
 * (usually when the application server is being shutdown). Possible values are "true" or "false". The default is "true".
 * </p>
 * <p>
 * The init parameter 'quartz:wait-on-shutdown' has effect when 'quartz:shutdown-on-unload' is specified "true", and indicates whether you want
 * scheduler.shutdown(true) called when the listener is unloaded (usually when the application server is being shutdown). Passing "true" to the
 * shutdown() call causes the scheduler to wait for existing jobs to complete. Possible values are "true" or "false". The default is "false".
 * </p>
 * <p>
 * The init parameter 'quartz:start-on-load' can be used to specify whether you want the scheduler.start() method called when the listener is first
 * loaded. If set to false, your application will need to call the start() method before the scheduler begins to run and process jobs. Possible values
 * are "true" or "false". The default is "true", which means the scheduler is started.
 * </p>
 * <p>
 * The init parameter 'quartz:start-delay-seconds' can be used to specify the amount of time to wait after initializing the scheduler before
 * scheduler.start() is called.
 * </p>
 *
 * @author James House
 * @author Chuck Cavaness
 * @author John Petrocik
 * @author timmolter
 */
public class SundialInitializerListener implements ServletContextListener {

  private boolean performShutdown = true;

  private boolean waitOnShutdown = false;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {

    logger.info("Sundial Initializer Servlet loaded, initializing Scheduler...");

    ServletContext servletContext = servletContextEvent.getServletContext();
    try {

      String shutdownPrefString = servletContext.getInitParameter("shutdown-on-unload");

      if (shutdownPrefString != null) {
        performShutdown = Boolean.valueOf(shutdownPrefString).booleanValue();
      }
      String shutdownWaitPrefString = servletContext.getInitParameter("wait-on-shutdown");
      if (shutdownPrefString != null) {
        waitOnShutdown = Boolean.valueOf(shutdownWaitPrefString).booleanValue();
      }

      // THREAD POOL SIZE
      int threadPoolSize = 10; // ten is default
      String threadPoolSizeString = servletContext.getInitParameter("thread-pool-size");

      try {
        if (threadPoolSizeString != null && threadPoolSizeString.trim().length() > 0) {
          threadPoolSize = Integer.parseInt(threadPoolSizeString);
        }
      } catch (Exception e) {
        logger.error("Cannot parse value of 'thread-pool-size' to an integer: " + threadPoolSizeString + ", defaulting to 10 threads.");
      }

      // JOBS PACKAGE NAME
      String packageName = servletContext.getInitParameter("annotated-jobs-package-name");

      // Always want to get the scheduler, even if it isn't starting,
      // to make sure it is both initialized and registered.
      SundialJobScheduler.createScheduler(threadPoolSize, packageName);

      // Give a reference to the servletContext so jobs can access "global" webapp objects
      SundialJobScheduler.setServletContext(servletContext);

      // Should the Scheduler being started now or later
      String startOnLoadString = servletContext.getInitParameter("start-scheduler-on-load");

      int startDelay = 0;
      String startDelayString = servletContext.getInitParameter("start-delay-seconds");

      try {
        if (startDelayString != null && startDelayString.trim().length() > 0) {
          startDelay = Integer.parseInt(startDelayString);
        }
      } catch (Exception e) {
        logger.error("Cannot parse value of 'start-delay-seconds' to an integer: " + startDelayString + ", defaulting to 5 seconds.");
        startDelay = 5;
      }

      if (startOnLoadString == null || (Boolean.valueOf(startOnLoadString).booleanValue())) {
        if (startDelay <= 0) {
          // Start now
          SundialJobScheduler.getScheduler().start();
          logger.info("Sundial Scheduler has been started...");
        } else {
          // Start delayed
          SundialJobScheduler.getScheduler().startDelayed(startDelay);
          logger.info("Sundial Scheduler will start in " + startDelay + " seconds.");
        }
      } else {
        logger.info("Sundial Scheduler has not been started. Use scheduler.start()");
      }

      String globalLockOnLoadString = servletContext.getInitParameter("global-lock-on-load");
      boolean globalLockOnLoad = false;
      if (globalLockOnLoadString != null) {
        globalLockOnLoad = Boolean.valueOf(globalLockOnLoadString).booleanValue();
        if (globalLockOnLoad) {
          SundialJobScheduler.lockScheduler();
          logger.info("Sundial Scheduler has been locked.");
        }
      }

    } catch (Exception e) {
      logger.error("Sundial Scheduler failed to initialize: ", e);
    }

  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {

    if (!performShutdown) {
      return;
    }

    try {
      if (SundialJobScheduler.getScheduler() != null) {
        SundialJobScheduler.getScheduler().shutdown(waitOnShutdown);
      }
    } catch (Exception e) {
      logger.error("Sundial Scheduler failed to shutdown cleanly: ", e);
    }

    logger.info("Sundial Scheduler successful shutdown.");
  }

}
