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
package org.knowm.sundial;

import java.util.concurrent.TimeUnit;

import org.knowm.sundial.SundialJobScheduler;

/**
 * @author timmolter
 */
public class SampleRun {

  /**
   * Make sure jobs.xml is on the classpath!
   */
  public static void main(String[] args) {

    SundialJobScheduler.startScheduler("org.knowm.sundial.jobs"); // package with annotated Jobs

    SundialJobScheduler.addJob("SampleJob1", "org.knowm.sundial.jobs.SampleJob1");

    SundialJobScheduler.addCronTrigger("SampleJob1-Cron-Trigger", "SampleJob1", "0/10 * * * * ?");

    SundialJobScheduler.addSimpleTrigger("SampleJob1-Simple-Trigger", "SampleJob1", -1, TimeUnit.SECONDS.toMillis(3));

  }
}
