package org.quartz.listeners;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

/**
 * Holds a List of references to SchedulerListener instances and broadcasts all
 * events to them (in order).
 *
 * <p>This may be more convenient than registering all of the listeners
 * directly with the Scheduler, and provides the flexibility of easily changing
 * which listeners get notified.</p>
 *
 * @see #addListener(org.quartz.SchedulerListener)
 * @see #removeListener(org.quartz.SchedulerListener)
 *
 * @author James House (jhouse AT revolition DOT net)
 */
public class BroadcastSchedulerListener implements SchedulerListener {

    private List<SchedulerListener> listeners;

    public BroadcastSchedulerListener() {
        listeners = new LinkedList();
    }

    /**
     * Construct an instance with the given List of listeners.
     *
     * @param listeners the initial List of SchedulerListeners to broadcast to.
     */
    public BroadcastSchedulerListener(List listeners) {
        this();
        this.listeners.addAll(listeners);
    }


    public void addListener(SchedulerListener listener) {
        listeners.add(listener);
    }

    public boolean removeListener(SchedulerListener listener) {
        return listeners.remove(listener);
    }

    public List<SchedulerListener> getListeners() {
        return java.util.Collections.unmodifiableList(listeners);
    }

	public void jobAdded(JobDetail jobDetail) {
        Iterator itr = listeners.iterator();
        while(itr.hasNext()) {
            SchedulerListener l = (SchedulerListener) itr.next();
            l.jobAdded(jobDetail);
        }
	}

	public void jobDeleted(JobKey jobKey) {
        Iterator itr = listeners.iterator();
        while(itr.hasNext()) {
            SchedulerListener l = (SchedulerListener) itr.next();
            l.jobDeleted(jobKey);
        }
	}
    
    public void jobScheduled(Trigger trigger) {
        Iterator itr = listeners.iterator();
        while(itr.hasNext()) {
            SchedulerListener l = (SchedulerListener) itr.next();
            l.jobScheduled(trigger);
        }
    }

    public void jobUnscheduled(TriggerKey triggerKey) {
        Iterator itr = listeners.iterator();
        while(itr.hasNext()) {
            SchedulerListener l = (SchedulerListener) itr.next();
            l.jobUnscheduled(triggerKey);
        }
    }

    public void triggerFinalized(Trigger trigger) {
        Iterator itr = listeners.iterator();
        while(itr.hasNext()) {
            SchedulerListener l = (SchedulerListener) itr.next();
            l.triggerFinalized(trigger);
        }
    }

    public void triggerPaused(TriggerKey key) {
        Iterator itr = listeners.iterator();
        while(itr.hasNext()) {
            SchedulerListener l = (SchedulerListener) itr.next();
            l.triggerPaused(key);
        }
    }

    public void triggersPaused(String triggerGroup) {
        Iterator itr = listeners.iterator();
        while(itr.hasNext()) {
            SchedulerListener l = (SchedulerListener) itr.next();
            l.triggersPaused(triggerGroup);
        }
    }

    public void triggerResumed(TriggerKey key) {
        Iterator itr = listeners.iterator();
        while(itr.hasNext()) {
            SchedulerListener l = (SchedulerListener) itr.next();
            l.triggerResumed(key);
        }
    }

    public void triggersResumed(String triggerGroup) {
        Iterator itr = listeners.iterator();
        while(itr.hasNext()) {
            SchedulerListener l = (SchedulerListener) itr.next();
            l.triggersResumed(triggerGroup);
        }
    }
    
    public void schedulingDataCleared() {
        Iterator itr = listeners.iterator();
        while(itr.hasNext()) {
            SchedulerListener l = (SchedulerListener) itr.next();
            l.schedulingDataCleared();
        }
    }

    
    public void jobPaused(JobKey key) {
        Iterator itr = listeners.iterator();
        while(itr.hasNext()) {
            SchedulerListener l = (SchedulerListener) itr.next();
            l.jobPaused(key);
        }
    }

    public void jobsPaused(String jobGroup) {
        Iterator itr = listeners.iterator();
        while(itr.hasNext()) {
            SchedulerListener l = (SchedulerListener) itr.next();
            l.jobsPaused(jobGroup);
        }
    }

    public void jobResumed(JobKey key) {
        Iterator itr = listeners.iterator();
        while(itr.hasNext()) {
            SchedulerListener l = (SchedulerListener) itr.next();
            l.jobResumed(key);
        }
    }

    public void jobsResumed(String jobGroup) {
        Iterator itr = listeners.iterator();
        while(itr.hasNext()) {
            SchedulerListener l = (SchedulerListener) itr.next();
            l.jobsResumed(jobGroup);
        }
    }
    
    public void schedulerError(String msg, SchedulerException cause) {
        Iterator itr = listeners.iterator();
        while(itr.hasNext()) {
            SchedulerListener l = (SchedulerListener) itr.next();
            l.schedulerError(msg, cause);
        }
    }

    public void schedulerStarted() {
        Iterator itr = listeners.iterator();
        while(itr.hasNext()) {
            SchedulerListener l = (SchedulerListener) itr.next();
            l.schedulerStarted();
        }
    }
    
    public void schedulerInStandbyMode() {
        Iterator itr = listeners.iterator();
        while(itr.hasNext()) {
            SchedulerListener l = (SchedulerListener) itr.next();
            l.schedulerInStandbyMode();
        }
    }
    
    public void schedulerShutdown() {
        Iterator itr = listeners.iterator();
        while(itr.hasNext()) {
            SchedulerListener l = (SchedulerListener) itr.next();
            l.schedulerShutdown();
        }
    }
    
    public void schedulerShuttingdown() {
        Iterator itr = listeners.iterator();
        while(itr.hasNext()) {
            SchedulerListener l = (SchedulerListener) itr.next();
            l.schedulerShuttingdown();
        }
    }
    
}
