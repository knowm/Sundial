/*
 * Copyright 2001-2009 Terracotta, Inc.
 * Copyright 2011 Xeiam, LLC
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

package org.quartz.impl;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.core.JobRunShellFactory;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.core.StandardJobRunShellFactory;
import org.quartz.impl.matchers.EverythingMatcher;
import org.quartz.plugins.xml.XMLSchedulingDataProcessorPlugin;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xeiam.sundial.DefaultTriggerListener;

/**
 * <p>
 * An implementation of <code>{@link org.quartz.SchedulerFactory}</code> that does all of its work of creating a <code>QuartzScheduler</code> instance.
 * </p>
 * 
 * @author James House
 * @author Anthony Eden
 * @author Mohammad Rezaei
 * @author timmolter
 */
public class StdSchedulerFactory {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private SchedulerException initException = null;

    QuartzScheduler mQuartzScheduler = null;

    private int mThreadPoolSize = 10; // default size is 10

    /**
     * @param pThreadPoolSize
     * @return Returns a handle to the Scheduler produced by this factory. Initialized with given pThreadPoolSize
     * @throws SchedulerException
     */
    public Scheduler getScheduler(int pThreadPoolSize) throws SchedulerException {

        mThreadPoolSize = pThreadPoolSize;

        return getScheduler();
    }

    /**
     * <p>
     * Returns a handle to the Scheduler produced by this factory.
     * </p>
     * <p>
     * If one of the <code>initialize</code> methods has not be previously called, then the default (no-arg) <code>initialize()</code> method will be called by this method.
     * </p>
     */
    public Scheduler getScheduler() throws SchedulerException {

        if (mQuartzScheduler != null) {
            return mQuartzScheduler;
        }

        return instantiate();
    }

    private Scheduler instantiate() throws SchedulerException {

        if (initException != null) {
            throw initException;
        }

        JobStore jobstore = null;
        SimpleThreadPool threadpool = null;

        // Setup SimpleThreadPool
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        try {
            threadpool = new SimpleThreadPool();
        } catch (Exception e) {
            initException = new SchedulerException("SimpleThreadPool could not be instantiated.", e);
            throw initException;
        }
        threadpool.setThreadCount(mThreadPoolSize);

        // Setup RAMJobStore
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        //

        try {
            // jobstore = (JobStore) loadHelper.loadClass(jsClass).newInstance();
            jobstore = new RAMJobStore();
        } catch (Exception e) {
            initException = new SchedulerException("RAMJobStore could not be instantiated.", e);
            throw initException;
        }

        // Set up any SchedulerPlugins
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        XMLSchedulingDataProcessorPlugin lXMLSchedulingDataProcessorPlugin = new XMLSchedulingDataProcessorPlugin();
        lXMLSchedulingDataProcessorPlugin.setFailOnFileNotFound(true);
        lXMLSchedulingDataProcessorPlugin.setFileNames("jobs.xml");
        lXMLSchedulingDataProcessorPlugin.setScanInterval(0);

        // Set up any JobListeners
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // Set up any TriggerListeners
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        DefaultTriggerListener lDefaultTriggerListener = new DefaultTriggerListener();

        boolean tpInited = false;
        boolean qsInited = false;

        // Fire everything up
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        try {

            JobRunShellFactory jrsf = null; // Create correct run-shell factory...

            jrsf = new StandardJobRunShellFactory();

            QuartzSchedulerResources rsrcs = new QuartzSchedulerResources();
            rsrcs.setThreadName("Quartz Scheduler Thread");
            rsrcs.setJobRunShellFactory(jrsf);
            rsrcs.setMakeSchedulerThreadDaemon(false);
            rsrcs.setThreadsInheritInitializersClassLoadContext(false);
            rsrcs.setBatchTimeWindow(0L);
            rsrcs.setMaxBatchSize(1);
            rsrcs.setInterruptJobsOnShutdown(true);
            rsrcs.setInterruptJobsOnShutdownWithWait(true);

            rsrcs.setThreadPool(threadpool);
            if (threadpool instanceof SimpleThreadPool) {
                (threadpool).setThreadNamePrefix("Quartz_Scheduler_Worker");
            }
            threadpool.initialize();
            tpInited = true;

            rsrcs.setJobStore(jobstore);

            // add plugins
            rsrcs.addSchedulerPlugin(lXMLSchedulingDataProcessorPlugin);

            mQuartzScheduler = new QuartzScheduler(rsrcs);
            qsInited = true;

            // Initialize plugins now that we have a Scheduler instance.
            lXMLSchedulingDataProcessorPlugin.initialize("lXMLSchedulingDataProcessorPlugin", mQuartzScheduler);

            // add listeners
            mQuartzScheduler.getListenerManager().addTriggerListener(lDefaultTriggerListener, EverythingMatcher.allTriggers());

            // fire up job store, and runshell factory
            jobstore.initialize(mQuartzScheduler.getSchedulerSignaler());
            jobstore.setThreadPoolSize(threadpool.getPoolSize());

            jrsf.initialize(mQuartzScheduler);

            mQuartzScheduler.initialize();

            logger.info("Quartz scheduler version: " + mQuartzScheduler.getVersion());

            return mQuartzScheduler;

        } catch (SchedulerException e) {
            if (qsInited) {
                mQuartzScheduler.shutdown(false);
            } else if (tpInited) {
                threadpool.shutdown(false);
            }
            throw e;
        } catch (RuntimeException re) {
            if (qsInited) {
                mQuartzScheduler.shutdown(false);
            } else if (tpInited) {
                threadpool.shutdown(false);
            }
            throw re;
        } catch (Error re) {
            if (qsInited) {
                mQuartzScheduler.shutdown(false);
            } else if (tpInited) {
                threadpool.shutdown(false);
            }
            throw re;
        }
    }

}
