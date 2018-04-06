package org.quartz.plugins.management;

import org.quartz.core.Scheduler;
import org.quartz.exceptions.SchedulerConfigException;
import org.quartz.exceptions.SchedulerException;
import org.quartz.plugins.SchedulerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This plugin catches the event of the JVM terminating (such as upon a CRTL-C) and tells the
 * scheduler to shutdown.
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

  public ShutdownHookPlugin() {}

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * Determine whether or not the plug-in is configured to cause a clean shutdown of the scheduler.
   *
   * <p>The default value is <code>true</code>.
   *
   * @see org.quartz.core.Scheduler#shutdown(boolean)
   */
  public boolean isCleanShutdown() {

    return cleanShutdown;
  }

  /**
   * Set whether or not the plug-in is configured to cause a clean shutdown of the scheduler.
   *
   * <p>The default value is <code>true</code>.
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
   * Called during creation of the <code>Scheduler</code> in order to give the <code>SchedulerPlugin
   * </code> a chance to initialize.
   *
   * @throws SchedulerConfigException if there is an error initializing.
   */
  @Override
  public void initialize(String name, final Scheduler scheduler) throws SchedulerException {

    logger.info("Registering Quartz shutdown hook.");

    Thread t =
        new Thread("Quartz Shutdown-Hook") {

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
   * Called in order to inform the <code>SchedulerPlugin</code> that it should free up all of it's
   * resources because the scheduler is shutting down.
   */
  @Override
  public void shutdown() {

    // nothing to do in this case (since the scheduler is already shutting
    // down)
  }
}
