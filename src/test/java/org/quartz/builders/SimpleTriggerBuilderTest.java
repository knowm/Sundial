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
