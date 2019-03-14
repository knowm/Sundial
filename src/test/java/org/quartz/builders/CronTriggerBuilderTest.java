package org.quartz.builders;

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;
import org.junit.Test;
import org.quartz.jobs.JobDataMap;
import org.quartz.triggers.OperableTrigger;

public class CronTriggerBuilderTest {
  @Test
  public void shouldBeAbleToCallMethodsInAnyOrder() throws ParseException {
    OperableTrigger trigger =
        CronTriggerBuilder.cronTriggerBuilder("0/5 * * * * ?")
            .withIdentity("id")
            .withDescription("description")
            .withPriority(1)
            .modifiedByCalendar("foo")
            .startNow()
            .startAt(new Date())
            .endAt(new Date())
            .forJob("job1")
            .usingJobData(new JobDataMap())
            .inTimeZone(TimeZone.getDefault())
            .withMisfireHandlingInstructionDoNothing()
            .withMisfireHandlingInstructionFireAndProceed()
            .build();
  }
}
