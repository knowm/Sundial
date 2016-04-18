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
package org.quartz.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class ListenerManagerImpl implements ListenerManager {

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
