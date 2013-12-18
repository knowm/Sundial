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
import java.util.Date;
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

  public static final int SUNDAY = 1;

  public static final int MONDAY = 2;

  public static final int TUESDAY = 3;

  public static final int WEDNESDAY = 4;

  public static final int THURSDAY = 5;

  public static final int FRIDAY = 6;

  public static final int SATURDAY = 7;

  public static final long MILLISECONDS_IN_MINUTE = 60l * 1000l;

  public static final long MILLISECONDS_IN_HOUR = 60l * 60l * 1000l;

  public static final long SECONDS_IN_MOST_DAYS = 24l * 60l * 60L;

  public static final long MILLISECONDS_IN_DAY = SECONDS_IN_MOST_DAYS * 1000l;

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

  /**
   * <p>
   * Returns a date that is rounded to the next even multiple of the given minute.
   * </p>
   * <p>
   * The rules for calculating the second are the same as those for calculating the minute in the method <code>getNextGivenMinuteDate(..)<code>.
   * </p>
   * 
   * @param date the Date to round, if <code>null</code> the current time will be used
   * @param secondBase the base-second to set the time on
   * @return the new rounded date
   * @see #nextGivenMinuteDate(Date, int)
   */
  public static Date nextGivenSecondDate(Date date, int secondBase) {

    if (secondBase < 0 || secondBase > 59) {
      throw new IllegalArgumentException("secondBase must be >=0 and <= 59");
    }

    if (date == null) {
      date = new Date();
    }

    Calendar c = Calendar.getInstance();
    c.setTime(date);
    c.setLenient(true);

    if (secondBase == 0) {
      c.set(Calendar.MINUTE, c.get(Calendar.MINUTE) + 1);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);

      return c.getTime();
    }

    int second = c.get(Calendar.SECOND);

    int arItr = second / secondBase;

    int nextSecondOccurance = secondBase * (arItr + 1);

    if (nextSecondOccurance < 60) {
      c.set(Calendar.SECOND, nextSecondOccurance);
      c.set(Calendar.MILLISECOND, 0);

      return c.getTime();
    }
    else {
      c.set(Calendar.MINUTE, c.get(Calendar.MINUTE) + 1);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);

      return c.getTime();
    }
  }

  public static void validateDayOfWeek(int dayOfWeek) {

    if (dayOfWeek < SUNDAY || dayOfWeek > SATURDAY) {
      throw new IllegalArgumentException("Invalid day of week.");
    }
  }

  public static void validateHour(int hour) {

    if (hour < 0 || hour > 23) {
      throw new IllegalArgumentException("Invalid hour (must be >= 0 and <= 23).");
    }
  }

  public static void validateMinute(int minute) {

    if (minute < 0 || minute > 59) {
      throw new IllegalArgumentException("Invalid minute (must be >= 0 and <= 59).");
    }
  }

  public static void validateSecond(int second) {

    if (second < 0 || second > 59) {
      throw new IllegalArgumentException("Invalid second (must be >= 0 and <= 59).");
    }
  }

  public static void validateDayOfMonth(int day) {

    if (day < 1 || day > 31) {
      throw new IllegalArgumentException("Invalid day of month.");
    }
  }

  public static void validateMonth(int month) {

    if (month < 1 || month > 12) {
      throw new IllegalArgumentException("Invalid month (must be >= 1 and <= 12.");
    }
  }

  private static final int MAX_YEAR = Calendar.getInstance().get(Calendar.YEAR) + 100;

  public static void validateYear(int year) {

    if (year < 0 || year > MAX_YEAR) {
      throw new IllegalArgumentException("Invalid year (must be >= 0 and <= " + MAX_YEAR);
    }
  }

}
