package org.quartz.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.quartz.JobListener;
import org.quartz.ListenerManager;
import org.quartz.SchedulerListener;
import org.quartz.TriggerListener;

class ListenerManagerImpl implements ListenerManager {

  private HashMap<String, JobListener> globalJobListeners = new HashMap<String, JobListener>(10);

  private HashMap<String, TriggerListener> globalTriggerListeners = new HashMap<String, TriggerListener>(10);

  private ArrayList<SchedulerListener> schedulerListeners = new ArrayList<SchedulerListener>(10);

  @Override
  public List<JobListener> getJobListeners() {

    synchronized (globalJobListeners) {
      return java.util.Collections.unmodifiableList(new LinkedList<JobListener>(globalJobListeners.values()));
    }
  }

  @Override
  public void addTriggerListener(TriggerListener triggerListener) {

    if (triggerListener.getName() == null || triggerListener.getName().length() == 0) {
      throw new IllegalArgumentException("TriggerListener name cannot be empty.");
    }

    synchronized (globalTriggerListeners) {

      globalTriggerListeners.put(triggerListener.getName(), triggerListener);

    }
  }

  @Override
  public List<TriggerListener> getTriggerListeners() {

    synchronized (globalTriggerListeners) {
      return java.util.Collections.unmodifiableList(new LinkedList<TriggerListener>(globalTriggerListeners.values()));
    }
  }

  @Override
  public List<SchedulerListener> getSchedulerListeners() {

    synchronized (schedulerListeners) {
      return java.util.Collections.unmodifiableList(new ArrayList<SchedulerListener>(schedulerListeners));
    }
  }

}
