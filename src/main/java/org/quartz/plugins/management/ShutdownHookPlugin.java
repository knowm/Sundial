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
package org.quartz.plugins.management;

import org.quartz.core.Scheduler;
import org.quartz.exceptions.SchedulerConfigException;
import org.quartz.exceptions.SchedulerException;
import org.quartz.plugins.SchedulerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This plugin catches the event of the JVM terminating (such as upon a CRTL-C) and tells the scheduler to shutdown.
 * 
 * @see org.quartz.core.Scheduler#shutdown(boolean)
 * @author James House
 */
public class ShutdownHookPlugin implements SchedulerPlugin {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private boolean cleanShutdown = true;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  public ShutdownHookPlugin() {

  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * Determine whether or not the plug-in is configured to cause a clean shutdown of the scheduler.
   * <p>
   * The default value is <code>true</code>.
   * </p>
   * 
   * @see org.quartz.core.Scheduler#shutdown(boolean)
   */
  public boolean isCleanShutdown() {

    return cleanShutdown;
  }

  /**
   * Set whether or not the plug-in is configured to cause a clean shutdown of the scheduler.
   * <p>
   * The default value is <code>true</code>.
   * </p>
   * 
   * @see org.quartz.core.Scheduler#shutdown(boolean)
   */
  public void setCleanShutdown(boolean b) {

    cleanShutdown = b;
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
   * @throws SchedulerConfigException if there is an error initializing.
   */
  @Override
  public void initialize(String name, final Scheduler scheduler) throws SchedulerException {

    logger.info("Registering Quartz shutdown hook.");

    Thread t = new Thread("Quartz Shutdown-Hook") {

      @Override
      public void run() {

        logger.info("Shutting down Quartz...");
        try {
          scheduler.shutdown(isCleanShutdown());
        } catch (SchedulerException e) {
          logger.info("Error shutting down Quartz: " + e.getMessage(), e);
        }
      }
    };

    Runtime.getRuntime().addShutdownHook(t);
  }

  @Override
  public void start() {

    // do nothing.
  }

  /**
   * <p>
   * Called in order to inform the <code>SchedulerPlugin</code> that it should free up all of it's resources because the scheduler is shutting down.
   * </p>
   */
  @Override
  public void shutdown() {

    // nothing to do in this case (since the scheduler is already shutting
    // down)
  }

}
