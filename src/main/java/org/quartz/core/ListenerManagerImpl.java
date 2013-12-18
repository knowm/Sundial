package org.quartz.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.ListenerManager;
import org.quartz.Matcher;
import org.quartz.SchedulerListener;
import org.quartz.TriggerKey;
import org.quartz.TriggerListener;
import org.quartz.impl.matchers.EverythingMatcher;

public class ListenerManagerImpl implements ListenerManager {

  private HashMap<String, JobListener> globalJobListeners = new HashMap<String, JobListener>(10);

  private HashMap<String, TriggerListener> globalTriggerListeners = new HashMap<String, TriggerListener>(10);

  private HashMap<String, List<Matcher<JobKey>>> globalJobListenersMatchers = new HashMap<String, List<Matcher<JobKey>>>(10);

  private HashMap<String, List<Matcher<TriggerKey>>> globalTriggerListenersMatchers = new HashMap<String, List<Matcher<TriggerKey>>>(10);

  private ArrayList<SchedulerListener> schedulerListeners = new ArrayList<SchedulerListener>(10);

  @Override
  public List<Matcher<JobKey>> getJobListenerMatchers(String listenerName) {

    synchronized (globalJobListeners) {
      List<Matcher<JobKey>> matchers = globalJobListenersMatchers.get(listenerName);
      if (matchers == null) {
        return null;
      }
      return Collections.unmodifiableList(matchers);
    }
  }

  @Override
  public List<JobListener> getJobListeners() {

    synchronized (globalJobListeners) {
      return java.util.Collections.unmodifiableList(new LinkedList<JobListener>(globalJobListeners.values()));
    }
  }

  @Override
  public void addTriggerListener(TriggerListener triggerListener, Matcher<TriggerKey>... matchers) {

    addTriggerListener(triggerListener, Arrays.asList(matchers));
  }

  @Override
  public void addTriggerListener(TriggerListener triggerListener, List<Matcher<TriggerKey>> matchers) {

    if (triggerListener.getName() == null || triggerListener.getName().length() == 0) {
      throw new IllegalArgumentException("TriggerListener name cannot be empty.");
    }

    synchronized (globalTriggerListeners) {
      globalTriggerListeners.put(triggerListener.getName(), triggerListener);

      LinkedList<Matcher<TriggerKey>> matchersL = new LinkedList<Matcher<TriggerKey>>();
      if (matchers != null && matchers.size() > 0) {
        matchersL.addAll(matchers);
      }
      else {
        matchersL.add(EverythingMatcher.allTriggers());
      }

      globalTriggerListenersMatchers.put(triggerListener.getName(), matchersL);
    }
  }

  @Override
  public List<Matcher<TriggerKey>> getTriggerListenerMatchers(String listenerName) {

    synchronized (globalTriggerListeners) {
      List<Matcher<TriggerKey>> matchers = globalTriggerListenersMatchers.get(listenerName);
      if (matchers == null) {
        return null;
      }
      return Collections.unmodifiableList(matchers);
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
