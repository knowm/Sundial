package org.quartz.core;

import org.quartz.triggers.Trigger;

/**
 * An interface to be implemented by objects that define spaces of time during which an associated <code>{@link Trigger}</code> may (not) fire.
 * Calendars do not define actual fire times, but rather are used to limit a <code>Trigger</code> from firing on its normal schedule if necessary.
 * Most Calendars include all times by default and allow the user to specify times to exclude.
 * <p>
 * As such, it is often useful to think of Calendars as being used to <I>exclude</I> a block of time - as opposed to <I>include</I> a block of time.
 * (i.e. the schedule &quot;fire every five minutes except on Sundays&quot; could be implemented with a <code>SimpleTrigger</code> and a
 * <code>WeeklyCalendar</code> which excludes Sundays)
 * </p>
 * <p>
 * Implementations MUST take care of being properly <code>Cloneable</code> and <code>Serializable</code>.
 * </p>
 * 
 * @author James House
 * @author Juergen Donnerstag
 */
public interface Calendar extends java.io.Serializable, java.lang.Cloneable {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Determine whether the given time (in milliseconds) is 'included' by the Calendar.
   * </p>
   */
  boolean isTimeIncluded(long timeStamp);

  Object clone();
}
