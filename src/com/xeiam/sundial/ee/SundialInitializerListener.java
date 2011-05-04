/*
 * Copyright 2001-2010 Terracotta, Inc.
 * Copyright 2011 Xeiam LLC
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

package com.xeiam.sundial.ee;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A ServletContextListner that can be used to initialize Quartz.
 * </p>
 * <p>
 * The init parameter 'quartz:shutdown-on-unload' can be used to specify whether you want scheduler.shutdown() called when the listener is unloaded (usually when the application server is being shutdown). Possible values are "true" or "false". The
 * default is "true".
 * </p>
 * <p>
 * The init parameter 'quartz:wait-on-shutdown' has effect when 'quartz:shutdown-on-unload' is specified "true", and indicates whether you want scheduler.shutdown(true) called when the listener is unloaded (usually when the application server is
 * being shutdown). Passing "true" to the shutdown() call causes the scheduler to wait for existing jobs to complete. Possible values are "true" or "false". The default is "false".
 * </p>
 * <p>
 * The init parameter 'quartz:start-on-load' can be used to specify whether you want the scheduler.start() method called when the listener is first loaded. If set to false, your application will need to call the start() method before the scheduler
 * begins to run and process jobs. Possible values are "true" or "false". The default is "true", which means the scheduler is started.
 * </p>
 * <p>
 * The init parameter 'quartz:start-delay-seconds' can be used to specify the amount of time to wait after initializing the scheduler before scheduler.start() is called.
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

    private Scheduler scheduler = null;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        logger.info("Quartz Initializer Servlet loaded, initializing Scheduler...");

        ServletContext servletContext = sce.getServletContext();
        StdSchedulerFactory factory;
        try {

            String shutdownPref = servletContext.getInitParameter("shutdown-on-unload");

            if (shutdownPref != null) {
                performShutdown = Boolean.valueOf(shutdownPref).booleanValue();
            }
            String shutdownWaitPref = servletContext.getInitParameter("wait-on-shutdown");
            if (shutdownPref != null) {
                waitOnShutdown = Boolean.valueOf(shutdownWaitPref).booleanValue();
            }

            factory = new StdSchedulerFactory();

            // Always want to get the scheduler, even if it isn't starting,
            // to make sure it is both initialized and registered.
            scheduler = factory.getScheduler();

            // Should the Scheduler being started now or later
            String startOnLoad = servletContext.getInitParameter("start-scheduler-on-load");

            int startDelay = 0;
            String startDelayS = servletContext.getInitParameter("start-delay-seconds");

            try {
                if (startDelayS != null && startDelayS.trim().length() > 0) {
                    startDelay = Integer.parseInt(startDelayS);
                }
            } catch (Exception e) {
                logger.error("Cannot parse value of 'start-delay-seconds' to an integer: " + startDelayS + ", defaulting to 5 seconds.");
                startDelay = 5;
            }

            /*
             * If the "quartz:start-on-load" init-parameter is not specified, the scheduler will be started. This is to maintain backwards compatability.
             */
            if (startOnLoad == null || (Boolean.valueOf(startOnLoad).booleanValue())) {
                if (startDelay <= 0) {
                    // Start now
                    scheduler.start();
                    logger.info("Scheduler has been started...");
                } else {
                    // Start delayed
                    scheduler.startDelayed(startDelay);
                    logger.info("Scheduler will start in " + startDelay + " seconds.");
                }
            } else {
                logger.info("Scheduler has not been started. Use scheduler.start()");
            }

        } catch (Exception e) {
            logger.error("Quartz Scheduler failed to initialize: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

        if (!performShutdown) {
            return;
        }

        try {
            if (scheduler != null) {
                scheduler.shutdown(waitOnShutdown);
            }
        } catch (Exception e) {
            logger.error("Quartz Scheduler failed to shutdown cleanly: " + e.toString());
            e.printStackTrace();
        }

        logger.info("Quartz Scheduler successful shutdown.");
    }

}
