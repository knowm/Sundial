package org.quartz.builders;

import java.util.Date;
import org.junit.Test;
import org.quartz.jobs.JobDataMap;
import org.quartz.builders.SimpleTriggerBuilder;
import org.quartz.triggers.OperableTrigger;

public class SimpleTriggerBuilderTest {
  @Test
  public void shouldBeAbleToCallMethodsInAnyOrder() {
    OperableTrigger trigger = SimpleTriggerBuilder.simpleTriggerBuilder()
      .withIdentity("id")
      .withDescription("description")
      .withPriority(1)
      .modifiedByCalendar("foo")
      .startNow()
      .startAt(new Date())
      .endAt(new Date())
      .forJob("job1")
      .usingJobData(new JobDataMap())
      .withIntervalInMilliseconds(1000)
      .withRepeatCount(10)
      .repeatForever()
      .withMisfireHandlingInstructionFireNow()
      .withMisfireHandlingInstructionNextWithExistingCount()
      .withMisfireHandlingInstructionNextWithRemainingCount()
      .withMisfireHandlingInstructionNowWithExistingCount()
      .withMisfireHandlingInstructionNowWithRemainingCount()
      .build();
  }
}
