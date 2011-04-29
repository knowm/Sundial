
/* 
 * Copyright 2001-2009 Terracotta, Inc. 
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

package org.quartz.impl.triggers;

import java.util.Calendar;
import java.util.Date;

import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.CalendarIntervalTrigger;
import org.quartz.CronTrigger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerUtils;
import org.quartz.DateBuilder.IntervalUnit;


/**
 * <p>A concrete <code>{@link Trigger}</code> that is used to fire a <code>{@link org.quartz.JobDetail}</code>
 * based upon repeating calendar time intervals.</p>
 * 
 * <p>The trigger will fire every N (see {@link #setRepeatInterval(int)} ) units of calendar time
 * (see {@link #setRepeatIntervalUnit(IntervalUnit)}) as specified in the trigger's definition.  
 * This trigger can achieve schedules that are not possible with {@link SimpleTrigger} (e.g 
 * because months are not a fixed number of seconds) or {@link CronTrigger} (e.g. because
 * "every 5 months" is not an even divisor of 12).</p>
 * 
 * <p>If you use an interval unit of <code>MONTH</code> then care should be taken when setting
 * a <code>startTime</code> value that is on a day near the end of the month.  For example,
 * if you choose a start time that occurs on January 31st, and have a trigger with unit
 * <code>MONTH</code> and interval <code>1</code>, then the next fire time will be February 28th, 
 * and the next time after that will be March 28th - and essentially each subsequent firing will 
 * occur on the 28th of the month, even if a 31st day exists.  If you want a trigger that always
 * fires on the last day of the month - regardless of the number of days in the month, 
 * you should use <code>CronTrigger</code>.</p> 
 * 
 * @see Trigger
 * @see CronTrigger
 * @see SimpleTrigger
 * @see NthIncludedDayTrigger
 * @see TriggerUtils
 * 
 * @since 1.7
 * 
 * @author James House
 */
public class CalendarIntervalTriggerImpl extends AbstractTrigger implements CalendarIntervalTrigger, CoreTrigger {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constants.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private static final long serialVersionUID = -2635982274232850343L;

    
    private static final int YEAR_TO_GIVEUP_SCHEDULING_AT = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + 100;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    
    private Date startTime = null;

    private Date endTime = null;

    private Date nextFireTime = null;

    private Date previousFireTime = null;

    private  int repeatInterval = 0;
    
    private IntervalUnit repeatIntervalUnit = IntervalUnit.DAY;

    private int timesTriggered = 0;

    private boolean complete = false;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Create a <code>DateIntervalTrigger</code> with no settings.
     * </p>
     */
    public CalendarIntervalTriggerImpl() {
        super();
    }

    /**
     * <p>
     * Create a <code>DateIntervalTrigger</code> that will occur immediately, and
     * repeat at the the given interval.
     * </p>
     */
    public CalendarIntervalTriggerImpl(String name, IntervalUnit intervalUnit,  int repeatInterval) {
        this(name, null, intervalUnit, repeatInterval);
    }

    /**
     * <p>
     * Create a <code>DateIntervalTrigger</code> that will occur immediately, and
     * repeat at the the given interval.
     * </p>
     */
    public CalendarIntervalTriggerImpl(String name, String group, IntervalUnit intervalUnit,
            int repeatInterval) {
        this(name, group, new Date(), null, intervalUnit, repeatInterval);
    }
    
    /**
     * <p>
     * Create a <code>DateIntervalTrigger</code> that will occur at the given time,
     * and repeat at the the given interval until the given end time.
     * </p>
     * 
     * @param startTime
     *          A <code>Date</code> set to the time for the <code>Trigger</code>
     *          to fire.
     * @param endTime
     *          A <code>Date</code> set to the time for the <code>Trigger</code>
     *          to quit repeat firing.
     * @param intervalUnit
     *          The repeat interval unit (minutes, days, months, etc).
     * @param repeatInterval
     *          The number of milliseconds to pause between the repeat firing.
     */
    public CalendarIntervalTriggerImpl(String name, Date startTime,
            Date endTime, IntervalUnit intervalUnit,  int repeatInterval) {
        this(name, null, startTime, endTime, intervalUnit, repeatInterval);
    }
    
    /**
     * <p>
     * Create a <code>DateIntervalTrigger</code> that will occur at the given time,
     * and repeat at the the given interval until the given end time.
     * </p>
     * 
     * @param startTime
     *          A <code>Date</code> set to the time for the <code>Trigger</code>
     *          to fire.
     * @param endTime
     *          A <code>Date</code> set to the time for the <code>Trigger</code>
     *          to quit repeat firing.
     * @param intervalUnit
     *          The repeat interval unit (minutes, days, months, etc).
     * @param repeatInterval
     *          The number of milliseconds to pause between the repeat firing.
     */
    public CalendarIntervalTriggerImpl(String name, String group, Date startTime,
            Date endTime, IntervalUnit intervalUnit,  int repeatInterval) {
        super(name, group);

        setStartTime(startTime);
        setEndTime(endTime);
        setRepeatIntervalUnit(intervalUnit);
        setRepeatInterval(repeatInterval);
    }

    /**
     * <p>
     * Create a <code>DateIntervalTrigger</code> that will occur at the given time,
     * fire the identified <code>Job</code> and repeat at the the given
     * interval until the given end time.
     * </p>
     * 
     * @param startTime
     *          A <code>Date</code> set to the time for the <code>Trigger</code>
     *          to fire.
     * @param endTime
     *          A <code>Date</code> set to the time for the <code>Trigger</code>
     *          to quit repeat firing.
     * @param intervalUnit
     *          The repeat interval unit (minutes, days, months, etc).
     * @param repeatInterval
     *          The number of milliseconds to pause between the repeat firing.
     */
    public CalendarIntervalTriggerImpl(String name, String group, String jobName,
            String jobGroup, Date startTime, Date endTime,  
            IntervalUnit intervalUnit,  int repeatInterval) {
        super(name, group, jobName, jobGroup);

        setStartTime(startTime);
        setEndTime(endTime);
        setRepeatIntervalUnit(intervalUnit);
        setRepeatInterval(repeatInterval);
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Get the time at which the <code>DateIntervalTrigger</code> should occur.
     * </p>
     */
    @Override
    public Date getStartTime() {
        if(startTime == null)
            startTime = new Date();
        return startTime;
    }

    /**
     * <p>
     * Set the time at which the <code>DateIntervalTrigger</code> should occur.
     * </p>
     * 
     * @exception IllegalArgumentException
     *              if startTime is <code>null</code>.
     */
    @Override
    public void setStartTime(Date startTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }

        Date eTime = getEndTime();
        if (eTime != null && startTime != null && eTime.before(startTime)) {
            throw new IllegalArgumentException(
                "End time cannot be before start time");    
        }

        this.startTime = startTime;
    }

    /**
     * <p>
     * Get the time at which the <code>DateIntervalTrigger</code> should quit
     * repeating.
     * </p>
     * 
     * @see #getFinalFireTime()
     */
    @Override
    public Date getEndTime() {
        return endTime;
    }

    /**
     * <p>
     * Set the time at which the <code>DateIntervalTrigger</code> should quit
     * repeating (and be automatically deleted).
     * </p>
     * 
     * @exception IllegalArgumentException
     *              if endTime is before start time.
     */
    @Override
    public void setEndTime(Date endTime) {
        Date sTime = getStartTime();
        if (sTime != null && endTime != null && sTime.after(endTime)) {
            throw new IllegalArgumentException(
                    "End time cannot be before start time");
        }

        this.endTime = endTime;
    }

    /* (non-Javadoc)
     * @see org.quartz.DateIntervalTriggerI#getRepeatIntervalUnit()
     */
    public IntervalUnit getRepeatIntervalUnit() {
        return repeatIntervalUnit;
    }

    /**
     * <p>Set the interval unit - the time unit on with the interval applies.</p>
     */
    public void setRepeatIntervalUnit(IntervalUnit intervalUnit) {
        this.repeatIntervalUnit = intervalUnit;
    }

    /* (non-Javadoc)
     * @see org.quartz.DateIntervalTriggerI#getRepeatInterval()
     */
    public int getRepeatInterval() {
        return repeatInterval;
    }

    /**
     * <p>
     * set the the time interval that will be added to the <code>DateIntervalTrigger</code>'s
     * fire time (in the set repeat interval unit) in order to calculate the time of the 
     * next trigger repeat.
     * </p>
     * 
     * @exception IllegalArgumentException
     *              if repeatInterval is < 1
     */
    public void setRepeatInterval( int repeatInterval) {
        if (repeatInterval < 0) {
            throw new IllegalArgumentException(
                    "Repeat interval must be >= 1");
        }

        this.repeatInterval = repeatInterval;
    }

    /* (non-Javadoc)
     * @see org.quartz.DateIntervalTriggerI#getTimesTriggered()
     */
    public int getTimesTriggered() {
        return timesTriggered;
    }

    /**
     * <p>
     * Set the number of times the <code>DateIntervalTrigger</code> has already
     * fired.
     * </p>
     */
    public void setTimesTriggered(int timesTriggered) {
        this.timesTriggered = timesTriggered;
    }

    @Override
    protected boolean validateMisfireInstruction(int misfireInstruction) {
        if (misfireInstruction < MISFIRE_INSTRUCTION_SMART_POLICY) {
            return false;
        }

        if (misfireInstruction > MISFIRE_INSTRUCTION_DO_NOTHING) {
            return false;
        }

        return true;
    }


    /**
     * <p>
     * Updates the <code>DateIntervalTrigger</code>'s state based on the
     * MISFIRE_INSTRUCTION_XXX that was selected when the <code>DateIntervalTrigger</code>
     * was created.
     * </p>
     * 
     * <p>
     * If the misfire instruction is set to MISFIRE_INSTRUCTION_SMART_POLICY,
     * then the following scheme will be used: <br>
     * <ul>
     * <li>The instruction will be interpreted as <code>MISFIRE_INSTRUCTION_FIRE_ONCE_NOW</code>
     * </ul>
     * </p>
     */
    @Override
    public void updateAfterMisfire(org.quartz.Calendar cal) {
        int instr = getMisfireInstruction();

        if(instr == Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY)
            return;

        if (instr == MISFIRE_INSTRUCTION_SMART_POLICY) {
            instr = MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
        }

        if (instr == MISFIRE_INSTRUCTION_DO_NOTHING) {
            Date newFireTime = getFireTimeAfter(new Date());
            while (newFireTime != null && cal != null
                    && !cal.isTimeIncluded(newFireTime.getTime())) {
                newFireTime = getFireTimeAfter(newFireTime);
            }
            setNextFireTime(newFireTime);
        } else if (instr == MISFIRE_INSTRUCTION_FIRE_ONCE_NOW) { 
            // fire once now...
            setNextFireTime(new Date());
            // the new fire time afterward will magically preserve the original  
            // time of day for firing for day/week/month interval triggers, 
            // because of the way getFireTimeAfter() works - in its always restarting
            // computation from the start time.
        }
    }

    /**
     * <p>
     * Called when the <code>{@link Scheduler}</code> has decided to 'fire'
     * the trigger (execute the associated <code>Job</code>), in order to
     * give the <code>Trigger</code> a chance to update itself for its next
     * triggering (if any).
     * </p>
     * 
     * @see #executionComplete(JobExecutionContext, JobExecutionException)
     */
    @Override
    public void triggered(org.quartz.Calendar calendar) {
        timesTriggered++;
        previousFireTime = nextFireTime;
        nextFireTime = getFireTimeAfter(nextFireTime);

        while (nextFireTime != null && calendar != null
                && !calendar.isTimeIncluded(nextFireTime.getTime())) {
            
            nextFireTime = getFireTimeAfter(nextFireTime);

            if(nextFireTime == null)
                break;
            
            //avoid infinite loop
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.setTime(nextFireTime);
            if (c.get(java.util.Calendar.YEAR) > YEAR_TO_GIVEUP_SCHEDULING_AT) {
                nextFireTime = null;
            }
        }
    }


    /**
     *  
     * @see org.quartz.Trigger#updateWithNewCalendar(org.quartz.Calendar, long)
     */
    @Override
    public void updateWithNewCalendar(org.quartz.Calendar calendar, long misfireThreshold)
    {
        nextFireTime = getFireTimeAfter(previousFireTime);

        if (nextFireTime == null || calendar == null) {
            return;
        }
        
        Date now = new Date();
        while (nextFireTime != null && !calendar.isTimeIncluded(nextFireTime.getTime())) {

            nextFireTime = getFireTimeAfter(nextFireTime);

            if(nextFireTime == null)
                break;
            
            //avoid infinite loop
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.setTime(nextFireTime);
            if (c.get(java.util.Calendar.YEAR) > YEAR_TO_GIVEUP_SCHEDULING_AT) {
                nextFireTime = null;
            }

            if(nextFireTime != null && nextFireTime.before(now)) {
                long diff = now.getTime() - nextFireTime.getTime();
                if(diff >= misfireThreshold) {
                    nextFireTime = getFireTimeAfter(nextFireTime);
                }
            }
        }
    }

    /**
     * <p>
     * Called by the scheduler at the time a <code>Trigger</code> is first
     * added to the scheduler, in order to have the <code>Trigger</code>
     * compute its first fire time, based on any associated calendar.
     * </p>
     * 
     * <p>
     * After this method has been called, <code>getNextFireTime()</code>
     * should return a valid answer.
     * </p>
     * 
     * @return the first time at which the <code>Trigger</code> will be fired
     *         by the scheduler, which is also the same value <code>getNextFireTime()</code>
     *         will return (until after the first firing of the <code>Trigger</code>).
     *         </p>
     */
    @Override
    public Date computeFirstFireTime(org.quartz.Calendar calendar) {
        nextFireTime = getStartTime();

        while (nextFireTime != null && calendar != null
                && !calendar.isTimeIncluded(nextFireTime.getTime())) {
            
            nextFireTime = getFireTimeAfter(nextFireTime);
            
            if(nextFireTime == null)
                break;

            //avoid infinite loop
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.setTime(nextFireTime);
            if (c.get(java.util.Calendar.YEAR) > YEAR_TO_GIVEUP_SCHEDULING_AT) {
                return null;
            }
        }
        
        return nextFireTime;
    }

    /**
     * <p>
     * Returns the next time at which the <code>Trigger</code> is scheduled to fire. If
     * the trigger will not fire again, <code>null</code> will be returned.  Note that
     * the time returned can possibly be in the past, if the time that was computed
     * for the trigger to next fire has already arrived, but the scheduler has not yet
     * been able to fire the trigger (which would likely be due to lack of resources
     * e.g. threads).
     * </p>
     *
     * <p>The value returned is not guaranteed to be valid until after the <code>Trigger</code>
     * has been added to the scheduler.
     * </p>
     */
    @Override
    public Date getNextFireTime() {
        return nextFireTime;
    }

    /**
     * <p>
     * Returns the previous time at which the <code>DateIntervalTrigger</code> 
     * fired. If the trigger has not yet fired, <code>null</code> will be
     * returned.
     */
    @Override
    public Date getPreviousFireTime() {
        return previousFireTime;
    }

    /**
     * <p>
     * Set the next time at which the <code>DateIntervalTrigger</code> should fire.
     * </p>
     * 
     * <p>
     * <b>This method should not be invoked by client code.</b>
     * </p>
     */
    public void setNextFireTime(Date nextFireTime) {
        this.nextFireTime = nextFireTime;
    }

    /**
     * <p>
     * Set the previous time at which the <code>DateIntervalTrigger</code> fired.
     * </p>
     * 
     * <p>
     * <b>This method should not be invoked by client code.</b>
     * </p>
     */
    public void setPreviousFireTime(Date previousFireTime) {
        this.previousFireTime = previousFireTime;
    }

    /**
     * <p>
     * Returns the next time at which the <code>DateIntervalTrigger</code> will
     * fire, after the given time. If the trigger will not fire after the given
     * time, <code>null</code> will be returned.
     * </p>
     */
    public Date getFireTimeAfter(Date afterTime) {
        return getFireTimeAfter(afterTime, false);
    }
    
    protected Date getFireTimeAfter(Date afterTime, boolean ignoreEndTime) {
        if (complete) {
            return null;
        }

        // increment afterTme by a second, so that we are 
        // comparing against a time after it!
        if (afterTime == null) {
            afterTime = new Date(System.currentTimeMillis() + 1000L);
        }
        else {
            afterTime = new Date(afterTime.getTime() + 1000L);
        }

        long startMillis = getStartTime().getTime();
        long afterMillis = afterTime.getTime();
        long endMillis = (getEndTime() == null) ? Long.MAX_VALUE : getEndTime()
                .getTime();

        if (!ignoreEndTime && (endMillis <= afterMillis)) {
            return null;
        }

        if (afterMillis < startMillis) {
            return new Date(startMillis);
        }

        
        long secondsAfterStart = (afterMillis - startMillis) / 1000L;

        Date time = null;
        long repeatLong = getRepeatInterval();
        
        Calendar aTime = Calendar.getInstance();
        aTime.setTime(afterTime);

        Calendar sTime = Calendar.getInstance();
        sTime.setTime(getStartTime());
        sTime.setLenient(true);
        
        if(getRepeatIntervalUnit().equals(IntervalUnit.SECOND)) {
            long jumpCount = secondsAfterStart / repeatLong;
            if(secondsAfterStart % repeatLong != 0)
                jumpCount++;
            sTime.add(Calendar.SECOND, getRepeatInterval() * (int)jumpCount);
            time = sTime.getTime();
        }
        else if(getRepeatIntervalUnit().equals(IntervalUnit.MINUTE)) {
            long jumpCount = secondsAfterStart / (repeatLong * 60L);
            if(secondsAfterStart % (repeatLong * 60L) != 0)
                jumpCount++;
            sTime.add(Calendar.MINUTE, getRepeatInterval() * (int)jumpCount);
            time = sTime.getTime();
        }
        else if(getRepeatIntervalUnit().equals(IntervalUnit.HOUR)) {
            long jumpCount = secondsAfterStart / (repeatLong * 60L * 60L);
            if(secondsAfterStart % (repeatLong * 60L * 60L) != 0)
                jumpCount++;
            sTime.add(Calendar.HOUR_OF_DAY, getRepeatInterval() * (int)jumpCount);
            time = sTime.getTime();
        }
        else if(getRepeatIntervalUnit().equals(IntervalUnit.DAY)) {
            sTime.setLenient(true);
            
            // Because intervals greater than an hour have an non-fixed number 
            // of seconds in them (due to daylight savings, variation number of 
            // days in each month, leap year, etc. ) we can't jump forward an
            // exact number of seconds to calculate the fire time as we can
            // with the second, minute and hour intervals.   But, rather
            // than slowly crawling our way there by iteratively adding the 
            // increment to the start time until we reach the "after time",
            // we can first make a big leap most of the way there...
            
            long jumpCount = secondsAfterStart / (repeatLong * 24L * 60L * 60L);
            // if we need to make a big jump, jump most of the way there, 
            // but not all the way because in some cases we may over-shoot or under-shoot
            if(jumpCount > 20) {
                if(jumpCount < 50)
                    jumpCount = (long) (jumpCount * 0.80);
                else if(jumpCount < 500)
                    jumpCount = (long) (jumpCount * 0.90);
                else
                    jumpCount = (long) (jumpCount * 0.95);
                sTime.add(java.util.Calendar.DAY_OF_YEAR, (int) (getRepeatInterval() * jumpCount));
            }
            
            // now baby-step the rest of the way there...
            while(sTime.getTime().before(afterTime) && 
                    (sTime.get(java.util.Calendar.YEAR) < YEAR_TO_GIVEUP_SCHEDULING_AT)) {            
                sTime.add(java.util.Calendar.DAY_OF_YEAR, getRepeatInterval());
            }
            time = sTime.getTime();
        }
        else if(getRepeatIntervalUnit().equals(IntervalUnit.WEEK)) {
            sTime.setLenient(true);

            // Because intervals greater than an hour have an non-fixed number 
            // of seconds in them (due to daylight savings, variation number of 
            // days in each month, leap year, etc. ) we can't jump forward an
            // exact number of seconds to calculate the fire time as we can
            // with the second, minute and hour intervals.   But, rather
            // than slowly crawling our way there by iteratively adding the 
            // increment to the start time until we reach the "after time",
            // we can first make a big leap most of the way there...
            
            long jumpCount = secondsAfterStart / (repeatLong * 7L * 24L * 60L * 60L);
            // if we need to make a big jump, jump most of the way there, 
            // but not all the way because in some cases we may over-shoot or under-shoot
            if(jumpCount > 20) {
                if(jumpCount < 50)
                    jumpCount = (long) (jumpCount * 0.80);
                else if(jumpCount < 500)
                    jumpCount = (long) (jumpCount * 0.90);
                else
                    jumpCount = (long) (jumpCount * 0.95);
                sTime.add(java.util.Calendar.WEEK_OF_YEAR, (int) (getRepeatInterval() * jumpCount));
            }
            
            while(sTime.getTime().before(afterTime) && 
                    (sTime.get(java.util.Calendar.YEAR) < YEAR_TO_GIVEUP_SCHEDULING_AT)) {            
                sTime.add(java.util.Calendar.WEEK_OF_YEAR, getRepeatInterval());
            }
            time = sTime.getTime();
        }
        else if(getRepeatIntervalUnit().equals(IntervalUnit.MONTH)) {
            sTime.setLenient(true);

            // because of the large variation in size of months, and 
            // because months are already large blocks of time, we will
            // just advance via brute-force iteration.
            
            while(sTime.getTime().before(afterTime) && 
                    (sTime.get(java.util.Calendar.YEAR) < YEAR_TO_GIVEUP_SCHEDULING_AT)) {            
                sTime.add(java.util.Calendar.MONTH, getRepeatInterval());
            }
            time = sTime.getTime();
        }
        else if(getRepeatIntervalUnit().equals(IntervalUnit.YEAR)) {

            while(sTime.getTime().before(afterTime) && 
                    (sTime.get(java.util.Calendar.YEAR) < YEAR_TO_GIVEUP_SCHEDULING_AT)) {            
                sTime.add(java.util.Calendar.YEAR, getRepeatInterval());
            }
            time = sTime.getTime();
        }

        if (!ignoreEndTime && (endMillis <= time.getTime())) {
            return null;
        }

        return time;
    }

    /**
     * <p>
     * Returns the final time at which the <code>DateIntervalTrigger</code> will
     * fire, if there is no end time set, null will be returned.
     * </p>
     * 
     * <p>
     * Note that the return time may be in the past.
     * </p>
     */
    public Date getFinalFireTime() {
        if (complete || getEndTime() == null) {
            return null;
        }

        // back up a second from end time
        Date fTime = new Date(getEndTime().getTime() - 1000L);
        // find the next fire time after that
        fTime = getFireTimeAfter(fTime, true);
        
        // the the trigger fires at the end time, that's it!
        if(fTime.equals(getEndTime()))
            return fTime;
        
        // otherwise we have to back up one interval from the fire time after the end time
        
        Calendar lTime = Calendar.getInstance();
        lTime.setTime(fTime);
        lTime.setLenient(true);
        
        if(getRepeatIntervalUnit().equals(IntervalUnit.SECOND)) {
            lTime.add(java.util.Calendar.SECOND, -1 * getRepeatInterval());
        }
        else if(getRepeatIntervalUnit().equals(IntervalUnit.MINUTE)) {
            lTime.add(java.util.Calendar.MINUTE, -1 * getRepeatInterval());
        }
        else if(getRepeatIntervalUnit().equals(IntervalUnit.HOUR)) {
            lTime.add(java.util.Calendar.HOUR_OF_DAY, -1 * getRepeatInterval());
        }
        else if(getRepeatIntervalUnit().equals(IntervalUnit.DAY)) {
            lTime.add(java.util.Calendar.DAY_OF_YEAR, -1 * getRepeatInterval());
        }
        else if(getRepeatIntervalUnit().equals(IntervalUnit.WEEK)) {
            lTime.add(java.util.Calendar.WEEK_OF_YEAR, -1 * getRepeatInterval());
        }
        else if(getRepeatIntervalUnit().equals(IntervalUnit.MONTH)) {
            lTime.add(java.util.Calendar.MONTH, -1 * getRepeatInterval());
        }
        else if(getRepeatIntervalUnit().equals(IntervalUnit.YEAR)) {
            lTime.add(java.util.Calendar.YEAR, -1 * getRepeatInterval());
        }

        return lTime.getTime();
    }

    /**
     * <p>
     * Determines whether or not the <code>DateIntervalTrigger</code> will occur
     * again.
     * </p>
     */
    public boolean mayFireAgain() {
        return (getNextFireTime() != null);
    }

    /**
     * <p>
     * Validates whether the properties of the <code>JobDetail</code> are
     * valid for submission into a <code>Scheduler</code>.
     * 
     * @throws IllegalStateException
     *           if a required property (such as Name, Group, Class) is not
     *           set.
     */
    public void validate() throws SchedulerException {
        super.validate();
        
        if (repeatInterval < 1) {
            throw new SchedulerException("Repeat Interval cannot be zero.");
        }
    }

    /**
     * Get a {@link ScheduleBuilder} that is configured to produce a 
     * schedule identical to this trigger's schedule.
     * 
     * @see #getTriggerBuilder()
     */
    public ScheduleBuilder<CalendarIntervalTrigger> getScheduleBuilder() {
        
        CalendarIntervalScheduleBuilder cb = CalendarIntervalScheduleBuilder.calendarIntervalSchedule()
                .withInterval(getRepeatInterval(), getRepeatIntervalUnit());
            
        switch(getMisfireInstruction()) {
            case MISFIRE_INSTRUCTION_DO_NOTHING : cb.withMisfireHandlingInstructionDoNothing();
            break;
            case MISFIRE_INSTRUCTION_FIRE_ONCE_NOW : cb.withMisfireHandlingInstructionFireAndProceed();
            break;
        }
        
        return cb;
    }

    public boolean hasAdditionalProperties() {
        return false;
    }
}
