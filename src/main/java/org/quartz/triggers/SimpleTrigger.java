package org.quartz.triggers;

import org.quartz.core.Calendar;
import org.quartz.core.Scheduler;

/**
 * A <code>{@link Trigger}</code> that is used to fire a <code>Job</code> at a given moment in time,
 * and optionally repeated at a specified interval.
 *
 * @author James House
 * @author contributions by Lieven Govaerts of Ebitec Nv, Belgium.
 */
public interface SimpleTrigger extends Trigger {

  public static final long serialVersionUID = -3735980074222850397L;

  /**
   * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire situation, the <code>
   * {@link SimpleTrigger}</code> wants to be fired now by <code>Scheduler</code>.
   *
   * <p><i>NOTE:</i> This instruction should typically only be used for 'one-shot' (non-repeating)
   * Triggers. If it is used on a trigger with a repeat count > 0 then it is equivalent to the
   * instruction <code>{@link #MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT}
   * </code>.
   */
  public static final int MISFIRE_INSTRUCTION_FIRE_NOW = 1;

  /**
   * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire situation, the <code>
   * {@link SimpleTrigger}</code> wants to be re-scheduled to 'now' (even if the associated <code>
   * {@link Calendar}</code> excludes 'now') with the repeat count left as-is. This does obey the
   * <code>Trigger</code> end-time however, so if 'now' is after the end-time the <code>Trigger
   * </code> will not fire again.
   *
   * <p><i>NOTE:</i> Use of this instruction causes the trigger to 'forget' the start-time and
   * repeat-count that it was originally setup with (this is only an issue if you for some reason
   * wanted to be able to tell what the original values were at some later time).
   */
  public static final int MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT = 2;

  /**
   * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire situation, the <code>
   * {@link SimpleTrigger}</code> wants to be re-scheduled to 'now' (even if the associated <code>
   * {@link Calendar}</code> excludes 'now') with the repeat count set to what it would be, if it
   * had not missed any firings. This does obey the <code>Trigger</code> end-time however, so if
   * 'now' is after the end-time the <code>Trigger</code> will not fire again.
   *
   * <p><i>NOTE:</i> Use of this instruction causes the trigger to 'forget' the start-time and
   * repeat-count that it was originally setup with. Instead, the repeat count on the trigger will
   * be changed to whatever the remaining repeat count is (this is only an issue if you for some
   * reason wanted to be able to tell what the original values were at some later time).
   *
   * <p><i>NOTE:</i> This instruction could cause the <code>Trigger</code> to go to the 'COMPLETE'
   * state after firing 'now', if all the repeat-fire-times where missed.
   */
  public static final int MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT = 3;

  /**
   * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire situation, the <code>
   * {@link SimpleTrigger}</code> wants to be re-scheduled to the next scheduled time after 'now' -
   * taking into account any associated <code>{@link Calendar}</code>, and with the repeat count set
   * to what it would be, if it had not missed any firings.
   *
   * <p><i>NOTE/WARNING:</i> This instruction could cause the <code>Trigger</code> to go directly to
   * the 'COMPLETE' state if all fire-times where missed.
   */
  public static final int MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT = 4;

  /**
   * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire situation, the <code>
   * {@link SimpleTrigger}</code> wants to be re-scheduled to the next scheduled time after 'now' -
   * taking into account any associated <code>{@link Calendar}</code>, and with the repeat count
   * left unchanged.
   *
   * <p><i>NOTE/WARNING:</i> This instruction could cause the <code>Trigger</code> to go directly to
   * the 'COMPLETE' state if the end-time of the trigger has arrived.
   */
  public static final int MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT = 5;

  /**
   * Used to indicate the 'repeat count' of the trigger is indefinite. Or in other words, the
   * trigger should repeat continually until the trigger's ending timestamp.
   */
  public static final int REPEAT_INDEFINITELY = -1;

  /**
   * Get the the number of times the <code>SimpleTrigger</code> should repeat, after which it will
   * be automatically deleted.
   *
   * @see #REPEAT_INDEFINITELY
   */
  public int getRepeatCount();

  /**
   * Get the the time interval (in milliseconds) at which the <code>SimpleTrigger</code> should
   * repeat.
   */
  public long getRepeatInterval();

  /** Get the number of times the <code>SimpleTrigger</code> has already fired. */
  public int getTimesTriggered();
}
