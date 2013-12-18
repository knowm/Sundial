/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

package org.quartz;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * <code>DateBuilder</code> is used to conveniently create <code>java.util.Date</code> instances that meet particular criteria.
 * <p>
 * Quartz provides a builder-style API for constructing scheduling-related entities via a Domain-Specific Language (DSL). The DSL can best be utilized through the usage of static imports of the
 * methods on the classes <code>TriggerBuilder</code>, <code>JobBuilder</code>, <code>DateBuilder</code>, <code>JobKey</code>, <code>TriggerKey</code> and the various <code>ScheduleBuilder</code>
 * implementations.
 * </p>
 * <p>
 * Client code can then use the DSL to write code such as this:
 * </p>
 * 
 * <pre>
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * JobDetail job = newJob(MyJob.class).withIdentity(&quot;myJob&quot;).build();
 * 
 * Trigger trigger = newTrigger().withIdentity(triggerKey(&quot;myTrigger&quot;, &quot;myTriggerGroup&quot;)).withSchedule(simpleSchedule().withIntervalInHours(1).repeatForever()).startAt(futureDate(10, MINUTES)).build();
 * 
 * scheduler.scheduleJob(job, trigger);
 * 
 * <pre>
 * 
 * @see TriggerBuilder
 * @see JobBuilder
 */
public class DateBuilder {

  public enum IntervalUnit {
    MILLISECOND, SECOND, MINUTE, HOUR, DAY, WEEK, MONTH, YEAR
  };

  public static final long MILLISECONDS_IN_MINUTE = 60l * 1000l;

  public static final long MILLISECONDS_IN_HOUR = 60l * 60l * 1000l;

  private int month;
  private int day;
  private int year;
  private int hour;
  private int minute;
  private int second;
  private TimeZone tz;
  private Locale lc;

  /**
   * Create a DateBuilder, with initial settings for the current date and time in the system default timezone.
   */
  private DateBuilder() {

    Calendar cal = Calendar.getInstance();

    month = cal.get(Calendar.MONTH);
    day = cal.get(Calendar.DAY_OF_MONTH);
    year = cal.get(Calendar.YEAR);
    hour = cal.get(Calendar.HOUR_OF_DAY);
    minute = cal.get(Calendar.MINUTE);
    second = cal.get(Calendar.SECOND);
  }

  /**
   * Create a DateBuilder, with initial settings for the current date and time in the given timezone.
   */
  private DateBuilder(TimeZone tz) {

    Calendar cal = Calendar.getInstance(tz);

    this.tz = tz;
    month = cal.get(Calendar.MONTH);
    day = cal.get(Calendar.DAY_OF_MONTH);
    year = cal.get(Calendar.YEAR);
    hour = cal.get(Calendar.HOUR_OF_DAY);
    minute = cal.get(Calendar.MINUTE);
    second = cal.get(Calendar.SECOND);
  }

  /**
   * Create a DateBuilder, with initial settings for the current date and time in the given locale.
   */
  private DateBuilder(Locale lc) {

    Calendar cal = Calendar.getInstance(lc);

    this.lc = lc;
    month = cal.get(Calendar.MONTH);
    day = cal.get(Calendar.DAY_OF_MONTH);
    year = cal.get(Calendar.YEAR);
    hour = cal.get(Calendar.HOUR_OF_DAY);
    minute = cal.get(Calendar.MINUTE);
    second = cal.get(Calendar.SECOND);
  }

  /**
   * Create a DateBuilder, with initial settings for the current date and time in the given timezone and locale.
   */
  private DateBuilder(TimeZone tz, Locale lc) {

    Calendar cal = Calendar.getInstance(tz, lc);

    this.tz = tz;
    this.lc = lc;
    month = cal.get(Calendar.MONTH);
    day = cal.get(Calendar.DAY_OF_MONTH);
    year = cal.get(Calendar.YEAR);
    hour = cal.get(Calendar.HOUR_OF_DAY);
    minute = cal.get(Calendar.MINUTE);
    second = cal.get(Calendar.SECOND);
  }

  private static final int MAX_YEAR = Calendar.getInstance().get(Calendar.YEAR) + 100;

}
