package org.quartz.simpl;

import org.quartz.SchedulerException;
import org.quartz.spi.InstanceIdGenerator;

/**
 * InstanceIdGenerator that will use a {@link SystemPropertyInstanceIdGenerator#SYSTEM_PROPERTY system property}
 * to configure the scheduler.
 * If no value set for the property, a {@link org.quartz.SchedulerException} is thrown
 *
 * @author Alex Snaps
 */
public class SystemPropertyInstanceIdGenerator implements InstanceIdGenerator {

  /**
   * System property to read the instanceId from
   */
  public static final String SYSTEM_PROPERTY = "org.quartz.scheduler.instanceId";

  /**
   * Returns the cluster wide value for this scheduler instance's id, based on a system property
   * @return the value of the {@link SystemPropertyInstanceIdGenerator#SYSTEM_PROPERTY system property}
   * @throws SchedulerException Shouldn't a value be found
   */
  public String generateInstanceId() throws SchedulerException {
    String property = System.getProperty(SYSTEM_PROPERTY);
    if(property == null) {
      throw new SchedulerException("No value for '" + SYSTEM_PROPERTY
                                   + "' system property found, please configure your environment accordingly!");
    }
    return property;
  }
}
