package org.quartz.builders;

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import org.quartz.jobs.JobDataMap;
import org.quartz.triggers.CronExpression;
import org.quartz.triggers.CronTrigger;
import org.quartz.triggers.CronTriggerImpl;
import org.quartz.triggers.OperableTrigger;

/**
 * <code>CronScheduleBuilder</code> is a {@link ScheduleBuilder} that defines {@link CronExpression}-based schedules for <code>Trigger</code>s.
 * <p>
 * Quartz provides a builder-style API for constructing scheduling-related entities via a Domain-Specific Language (DSL). The DSL can best be utilized
 * through the usage of static imports of the methods on the classes <code>TriggerBuilder</code>, <code>JobBuilder</code>, <code>DateBuilder</code>,
 * <code>JobKey</code>, <code>TriggerKey</code> and the various <code>ScheduleBuilder</code> implementations.
 * </p>
 * <p>
 * Client code can then use the DSL to write code such as this:
 * </p>
 *
 * <pre>
 *
 * JobDetail job = newJob(MyJob.class).withIdentity(&quot;myJob&quot;).build();
 * 
 * Trigger trigger = newTrigger().withIdentity(triggerKey(&quot;myTrigger&quot;, &quot;myTriggerGroup&quot;))
 *     .withSchedule(simpleSchedule().withIntervalInHours(1).repeatForever()).startAt(futureDate(10, MINUTES)).build();
 * 
 * scheduler.scheduleJob(job, trigger);
 *
 * </pre>
 */
public class CronTriggerBuilder extends TriggerBuilder {

  private String cronExpression;
  private TimeZone tz = null;
  private int misfireInstruction = CronTrigger.MISFIRE_INSTRUCTION_SMART_POLICY;

  /**
   * Constructor
   *
   * @param cronExpression
   */
  private CronTriggerBuilder(String cronExpression) {

    this.cronExpression = cronExpression;
  }

  /**
   * Create a CronScheduleBuilder with the given cron-expression.
   *
   * @param cronExpression the cron expression to base the schedule on.
   * @return the new CronScheduleBuilder
   * @throws ParseException
   * @see CronExpression
   */
  public static CronTriggerBuilder cronTriggerBuilder(String cronExpression) throws ParseException {

    CronExpression.validateExpression(cronExpression);
    return new CronTriggerBuilder(cronExpression);
  }

  /**
   * Build the actual Trigger -- NOT intended to be invoked by end users, but will rather be invoked by a TriggerBuilder which this ScheduleBuilder is
   * given to.
   */
  @Override
  public OperableTrigger instantiate() {

    CronTriggerImpl ct = new CronTriggerImpl();

    try {
      ct.setCronExpression(cronExpression);
    } catch (ParseException e) {
      // all methods of construction ensure the expression is valid by this point...
      throw new RuntimeException(
          "CronExpression '" + cronExpression + "' is invalid, which should not be possible, please report bug to Quartz developers.");
    }
    ct.setTimeZone(tz);
    ct.setMisfireInstruction(misfireInstruction);

    return ct;
  }

  /**
   * The <code>TimeZone</code> in which to base the schedule.
   *
   * @param tz the time-zone for the schedule.
   * @return the updated CronScheduleBuilder
   * @see CronExpression#getTimeZone()
   */
  public CronTriggerBuilder inTimeZone(TimeZone tz) {

    this.tz = tz;
    return this;
  }

  /**
   * If the Trigger misfires, use the {@link CronTrigger#MISFIRE_INSTRUCTION_DO_NOTHING} instruction.
   *
   * @return the updated CronScheduleBuilder
   * @see CronTrigger#MISFIRE_INSTRUCTION_DO_NOTHING
   */
  public CronTriggerBuilder withMisfireHandlingInstructionDoNothing() {

    misfireInstruction = CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING;
    return this;
  }

  /**
   * If the Trigger misfires, use the {@link CronTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW} instruction.
   *
   * @return the updated CronScheduleBuilder
   * @see CronTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW
   */
  public CronTriggerBuilder withMisfireHandlingInstructionFireAndProceed() {

    misfireInstruction = CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
    return this;
  }

  public CronTriggerBuilder withIdentity(String name) {
    return (CronTriggerBuilder)super.withIdentity(name);
  }

  public CronTriggerBuilder withDescription(String description) {
    return (CronTriggerBuilder)super.withDescription(description);
  }

  public CronTriggerBuilder withPriority(int priority) {
    return (CronTriggerBuilder)super.withPriority(priority);
  }

  public CronTriggerBuilder modifiedByCalendar(String calendarName) {
    return (CronTriggerBuilder)super.modifiedByCalendar(calendarName);
  }

  public CronTriggerBuilder startNow() {
    return (CronTriggerBuilder)super.startNow();
  }

  public CronTriggerBuilder startAt(Date startTime) {
    return (CronTriggerBuilder)super.startAt(startTime);
  }

  public CronTriggerBuilder endAt(Date endTime) {
    return (CronTriggerBuilder)super.endAt(endTime);
  }

  public CronTriggerBuilder forJob(String jobName) {
    return (CronTriggerBuilder)super.forJob(jobName);
  }

  public CronTriggerBuilder usingJobData(JobDataMap newJobDataMap) {
    return (CronTriggerBuilder)super.usingJobData(newJobDataMap);
  }
}
