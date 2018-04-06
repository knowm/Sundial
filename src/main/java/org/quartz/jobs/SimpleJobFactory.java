package org.quartz.jobs;

import org.quartz.core.Scheduler;
import org.quartz.core.TriggerFiredBundle;
import org.quartz.exceptions.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default JobFactory used by Quartz - simply calls <code>newInstance()</code> on the job class.
 *
 * @see JobFactory
 * @see PropertySettingJobFactory
 * @author jhouse
 */
public class SimpleJobFactory implements JobFactory {

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public Job newJob(TriggerFiredBundle bundle, Scheduler Scheduler) throws SchedulerException {

    JobDetail jobDetail = bundle.getJobDetail();
    Class<? extends Job> jobClass = jobDetail.getJobClass();
    try {
      if (log.isDebugEnabled()) {
        log.debug("Producing instance of Job '" + jobDetail.getName() + "', class=" + jobClass.getName());
      }

      return jobClass.newInstance();
    } catch (Exception e) {
      SchedulerException se = new SchedulerException("Problem instantiating class '" + jobDetail.getJobClass().getName() + "'", e);
      throw se;
    }
  }

}
