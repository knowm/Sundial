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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;
import java.util.Properties;

import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.TriggerListener;
import org.quartz.core.JobRunShellFactory;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.core.StandardJobRunShellFactory;
import org.quartz.impl.matchers.EverythingMatcher;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.JobFactory;
import org.quartz.spi.JobStore;
import org.quartz.spi.SchedulerPlugin;
import org.quartz.spi.ThreadPool;
import org.quartz.utils.PropertiesParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * An implementation of <code>{@link org.quartz.SchedulerFactory}</code> that does all of its work of creating a <code>QuartzScheduler</code> instance based on the contents of a <code>Properties</code> file.
 * </p>
 * <p>
 * By default a properties file named "quartz.properties" is loaded from the 'current working directory'. If that fails, then the "quartz.properties" file located (as a resource) in the org/quartz package is loaded. If you wish to use a file other
 * than these defaults, you must define the system property 'org.quartz.properties' to point to the file you want.
 * </p>
 * <p>
 * Alternatively, you can explicitly initialize the factory by calling one of the <code>initialize(xx)</code> methods before calling <code>getScheduler()</code>.
 * </p>
 * <p>
 * See the sample properties files that are distributed with Quartz for information about the various settings available within the file. Full configuration documentation can be found at http://www.quartz-scheduler.org/docs/configuration/index.html
 * </p>
 * <p>
 * Instances of the specified <code>{@link org.quartz.spi.JobStore}</code>, <code>{@link org.quartz.spi.ThreadPool}</code>, and other SPI classes will be created by name, and then any additional properties specified for them in the config file will
 * be set on the instance by calling an equivalent 'set' method. For example if the properties file contains the property 'org.quartz.jobStore.myProp = 10' then after the JobStore class has been instantiated, the method 'setMyProp()' will be called
 * on it. Type conversion to primitive Java types (int, long, float, double, boolean, and String) are performed before calling the property's setter method.
 * </p>
 * <p>
 * One property can reference another property's value by specifying a value following the convention of "$@other.property.name", for example, to reference the scheduler's instance name as the value for some other property, you would use
 * "$@org.quartz.scheduler.instanceName".
 * </p>
 * 
 * @author James House
 * @author Anthony Eden
 * @author Mohammad Rezaei
 * @author timmolter
 */
public class StdSchedulerFactory implements SchedulerFactory {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constants. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public static final String PROP_SCHED_INSTANCE_NAME = "org.quartz.scheduler.instanceName";

    public static final String PROP_SCHED_INSTANCE_ID = "org.quartz.scheduler.instanceId";

    public static final String PROP_SCHED_THREAD_NAME = "org.quartz.scheduler.threadName";

    public static final String PROP_SCHED_BATCH_TIME_WINDOW = "org.quartz.scheduler.batchTriggerAcquisitionFireAheadTimeWindow";

    public static final String PROP_SCHED_MAX_BATCH_SIZE = "org.quartz.scheduler.batchTriggerAcquisitionMaxCount";

    public static final String PROP_SCHED_WRAP_JOB_IN_USER_TX = "org.quartz.scheduler.wrapJobExecutionInUserTransaction";

    public static final String PROP_SCHED_IDLE_WAIT_TIME = "org.quartz.scheduler.idleWaitTime";

    public static final String PROP_SCHED_MAKE_SCHEDULER_THREAD_DAEMON = "org.quartz.scheduler.makeSchedulerThreadDaemon";

    public static final String PROP_SCHED_SCHEDULER_THREADS_INHERIT_CONTEXT_CLASS_LOADER_OF_INITIALIZING_THREAD = "org.quartz.scheduler.threadsInheritContextClassLoaderOfInitializer";

    public static final String PROP_SCHED_CLASS_LOAD_HELPER_CLASS = "org.quartz.scheduler.classLoadHelper.class";

    public static final String PROP_SCHED_JOB_FACTORY_CLASS = "org.quartz.scheduler.jobFactory.class";

    public static final String PROP_SCHED_JOB_FACTORY_PREFIX = "org.quartz.scheduler.jobFactory";

    public static final String PROP_SCHED_INTERRUPT_JOBS_ON_SHUTDOWN = "org.quartz.scheduler.interruptJobsOnShutdown";

    public static final String PROP_SCHED_INTERRUPT_JOBS_ON_SHUTDOWN_WITH_WAIT = "org.quartz.scheduler.interruptJobsOnShutdownWithWait";

    public static final String PROP_SCHED_CONTEXT_PREFIX = "org.quartz.context.key";

    public static final String PROP_THREAD_POOL_PREFIX = "org.quartz.threadPool";

    public static final String PROP_THREAD_POOL_CLASS = "org.quartz.threadPool.class";

    public static final String PROP_JOB_STORE_PREFIX = "org.quartz.jobStore";

    public static final String PROP_JOB_STORE_LOCK_HANDLER_PREFIX = PROP_JOB_STORE_PREFIX + ".lockHandler";

    public static final String PROP_JOB_STORE_LOCK_HANDLER_CLASS = PROP_JOB_STORE_LOCK_HANDLER_PREFIX + ".class";

    public static final String PROP_TABLE_PREFIX = "tablePrefix";

    public static final String PROP_SCHED_NAME = "schedName";

    public static final String PROP_JOB_STORE_CLASS = "org.quartz.jobStore.class";

    public static final String PROP_JOB_STORE_USE_PROP = "org.quartz.jobStore.useProperties";

    public static final String PROP_DATASOURCE_PREFIX = "org.quartz.dataSource";

    public static final String PROP_CONNECTION_PROVIDER_CLASS = "connectionProvider.class";

    public static final String PROP_PLUGIN_PREFIX = "org.quartz.plugin";

    public static final String PROP_PLUGIN_CLASS = "class";

    public static final String PROP_JOB_LISTENER_PREFIX = "org.quartz.jobListener";

    public static final String PROP_TRIGGER_LISTENER_PREFIX = "org.quartz.triggerListener";

    public static final String PROP_LISTENER_CLASS = "class";

    public static final String DEFAULT_INSTANCE_ID = "NON_CLUSTERED";

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private SchedulerException initException = null;

    private String propSrc = null;

    private PropertiesParser mPropertiesParser;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * <p>
     * Returns a handle to the Scheduler produced by this factory.
     * </p>
     * <p>
     * If one of the <code>initialize</code> methods has not be previously called, then the default (no-arg) <code>initialize()</code> method will be called by this method.
     * </p>
     */
    @Override
    public Scheduler getScheduler() throws SchedulerException {

        if (mPropertiesParser == null) {
            initialize();
        }

        SchedulerRepository schedRep = SchedulerRepository.getInstance();

        Scheduler sched = schedRep.lookup(getSchedulerName());

        if (sched != null) {
            if (sched.isShutdown()) {
                schedRep.remove(getSchedulerName());
            } else {
                return sched;
            }
        }

        sched = instantiate();

        return sched;
    }

    /**
     * <p>
     * Initialize the <code>{@link org.quartz.SchedulerFactory}</code> with the contents of a <code>Properties</code> file and overriding System properties.
     * </p>
     * <p>
     * By default a properties file named "quartz.properties" is loaded from the 'current working directory'. If that fails, then the "quartz.properties" file located (as a resource) in the org/quartz package is loaded. If you wish to use a file
     * other than these defaults, you must define the system property 'org.quartz.properties' to point to the file you want.
     * </p>
     * <p>
     * System properties (environment variables, and -D definitions on the command-line when running the JVM) override any properties in the loaded file. For this reason, you may want to use a different initialize() method if your application
     * security policy prohibits access to <code>{@link java.lang.System#getProperties()}</code>.
     * </p>
     */
    public void initialize() throws SchedulerException {

        // short-circuit if already initialized
        if (mPropertiesParser != null) {
            return;
        }
        if (initException != null) {
            throw initException;
        }

        String propFileName = "quartz.properties";
        File propFile = new File(propFileName);

        Properties props = new Properties();

        InputStream in = null;

        try {
            if (propFile.exists()) {
                try {

                    propSrc = "default file in current working dir: 'quartz.properties'";

                    in = new BufferedInputStream(new FileInputStream(propFileName));
                    props.load(in);

                } catch (IOException ioe) {
                    initException = new SchedulerException("Properties file: '" + propFileName + "' could not be read.", ioe);
                    throw initException;
                }
            } else {
                propSrc = "default resource file in Quartz package: 'quartz.properties'";

                ClassLoader cl = getClass().getClassLoader();
                if (cl == null) {
                    cl = findClassloader();
                }
                if (cl == null) {
                    throw new SchedulerConfigException("Unable to find a class loader on the current thread or class.");
                }

                in = cl.getResourceAsStream("quartz.properties");

                if (in == null) {
                    in = cl.getResourceAsStream("/quartz.properties");
                }
                if (in == null) {
                    in = cl.getResourceAsStream("org/quartz/quartz.properties");
                }
                if (in == null) {
                    initException = new SchedulerException("Default quartz.properties not found in class path");
                    throw initException;
                }
                try {
                    props.load(in);
                } catch (IOException ioe) {
                    initException = new SchedulerException("Resource properties file: 'org/quartz/quartz.properties' " + "could not be read from the classpath.", ioe);
                    throw initException;
                }
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignore) { /* ignore */
                }
            }
        }

        initialize(props);
    }

    /**
     * <p>
     * Initialize the <code>{@link org.quartz.SchedulerFactory}</code> with the contents of the given <code>Properties</code> object.
     * </p>
     */
    public void initialize(Properties props) throws SchedulerException {

        if (propSrc == null) {
            propSrc = "an externally provided properties instance.";
        }

        mPropertiesParser = new PropertiesParser(props);
    }

    private Scheduler instantiate() throws SchedulerException {

        if (mPropertiesParser == null) {
            initialize();
        }

        if (initException != null) {
            throw initException;
        }

        JobStore jobstore = null;
        ThreadPool threadpool = null;
        QuartzScheduler lQuartzScheduler = null;
        Properties tProps = null;
        boolean wrapJobInTx = false;
        long idleWaitTime = -1;
        String classLoadHelperClass;
        String jobFactoryClass;

        SchedulerRepository schedRep = SchedulerRepository.getInstance();

        // Get Scheduler Properties
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        String schedName = mPropertiesParser.getStringProperty(PROP_SCHED_INSTANCE_NAME, "QuartzScheduler");

        String threadName = mPropertiesParser.getStringProperty(PROP_SCHED_THREAD_NAME, schedName + "_QuartzSchedulerThread");

        String schedInstId = mPropertiesParser.getStringProperty(PROP_SCHED_INSTANCE_ID, DEFAULT_INSTANCE_ID);

        classLoadHelperClass = mPropertiesParser.getStringProperty(PROP_SCHED_CLASS_LOAD_HELPER_CLASS, "org.quartz.simpl.CascadingClassLoadHelper");
        wrapJobInTx = mPropertiesParser.getBooleanProperty(PROP_SCHED_WRAP_JOB_IN_USER_TX, wrapJobInTx);

        jobFactoryClass = mPropertiesParser.getStringProperty(PROP_SCHED_JOB_FACTORY_CLASS, null);

        idleWaitTime = mPropertiesParser.getLongProperty(PROP_SCHED_IDLE_WAIT_TIME, idleWaitTime);

        boolean makeSchedulerThreadDaemon = mPropertiesParser.getBooleanProperty(PROP_SCHED_MAKE_SCHEDULER_THREAD_DAEMON);

        boolean threadsInheritInitalizersClassLoader = mPropertiesParser.getBooleanProperty(PROP_SCHED_SCHEDULER_THREADS_INHERIT_CONTEXT_CLASS_LOADER_OF_INITIALIZING_THREAD);

        long batchTimeWindow = mPropertiesParser.getLongProperty(PROP_SCHED_BATCH_TIME_WINDOW, 0L);
        int maxBatchSize = mPropertiesParser.getIntProperty(PROP_SCHED_MAX_BATCH_SIZE, 1);

        boolean interruptJobsOnShutdown = mPropertiesParser.getBooleanProperty(PROP_SCHED_INTERRUPT_JOBS_ON_SHUTDOWN, false);
        boolean interruptJobsOnShutdownWithWait = mPropertiesParser.getBooleanProperty(PROP_SCHED_INTERRUPT_JOBS_ON_SHUTDOWN_WITH_WAIT, false);

        Properties schedCtxtProps = mPropertiesParser.getPropertyGroup(PROP_SCHED_CONTEXT_PREFIX, true);

        // Create class load helper
        ClassLoadHelper loadHelper = null;
        try {
            loadHelper = (ClassLoadHelper) loadClass(classLoadHelperClass).newInstance();
        } catch (Exception e) {
            throw new SchedulerConfigException("Unable to instantiate class load helper class: " + e.getMessage(), e);
        }
        loadHelper.initialize();

        JobFactory jobFactory = null;
        if (jobFactoryClass != null) {
            try {
                jobFactory = (JobFactory) loadHelper.loadClass(jobFactoryClass).newInstance();
            } catch (Exception e) {
                throw new SchedulerConfigException("Unable to instantiate JobFactory class: " + e.getMessage(), e);
            }

            tProps = mPropertiesParser.getPropertyGroup(PROP_SCHED_JOB_FACTORY_PREFIX, true);
            try {
                setBeanProps(jobFactory, tProps);
            } catch (Exception e) {
                initException = new SchedulerException("JobFactory class '" + jobFactoryClass + "' props could not be configured.", e);
                throw initException;
            }
        }

        // Get ThreadPool Properties
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        String tpClass = mPropertiesParser.getStringProperty(PROP_THREAD_POOL_CLASS, SimpleThreadPool.class.getName());

        if (tpClass == null) {
            initException = new SchedulerException("ThreadPool class not specified. ");
            throw initException;
        }

        try {
            threadpool = (ThreadPool) loadHelper.loadClass(tpClass).newInstance();
        } catch (Exception e) {
            initException = new SchedulerException("ThreadPool class '" + tpClass + "' could not be instantiated.", e);
            throw initException;
        }
        tProps = mPropertiesParser.getPropertyGroup(PROP_THREAD_POOL_PREFIX, true);
        try {
            setBeanProps(threadpool, tProps);
        } catch (Exception e) {
            initException = new SchedulerException("ThreadPool class '" + tpClass + "' props could not be configured.", e);
            throw initException;
        }

        // Get JobStore Properties
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        String jsClass = mPropertiesParser.getStringProperty(PROP_JOB_STORE_CLASS, RAMJobStore.class.getName());

        if (jsClass == null) {
            initException = new SchedulerException("JobStore class not specified. ");
            throw initException;
        }

        try {
            jobstore = (JobStore) loadHelper.loadClass(jsClass).newInstance();
        } catch (Exception e) {
            initException = new SchedulerException("JobStore class '" + jsClass + "' could not be instantiated.", e);
            throw initException;
        }

        SchedulerDetailsSetter.setDetails(jobstore, schedName, schedInstId);

        tProps = mPropertiesParser.getPropertyGroup(PROP_JOB_STORE_PREFIX, true, new String[] { PROP_JOB_STORE_LOCK_HANDLER_PREFIX });
        try {
            setBeanProps(jobstore, tProps);
        } catch (Exception e) {
            initException = new SchedulerException("JobStore class '" + jsClass + "' props could not be configured.", e);
            throw initException;
        }

        // Set up any SchedulerPlugins
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        String[] pluginNames = mPropertiesParser.getPropertyGroups(PROP_PLUGIN_PREFIX);
        SchedulerPlugin[] plugins = new SchedulerPlugin[pluginNames.length];
        for (int i = 0; i < pluginNames.length; i++) {
            Properties pp = mPropertiesParser.getPropertyGroup(PROP_PLUGIN_PREFIX + "." + pluginNames[i], true);

            String plugInClass = pp.getProperty(PROP_PLUGIN_CLASS, null);

            if (plugInClass == null) {
                initException = new SchedulerException("SchedulerPlugin class not specified for plugin '" + pluginNames[i] + "'");
                throw initException;
            }
            SchedulerPlugin plugin = null;
            try {
                plugin = (SchedulerPlugin) loadHelper.loadClass(plugInClass).newInstance();
            } catch (Exception e) {
                initException = new SchedulerException("SchedulerPlugin class '" + plugInClass + "' could not be instantiated.", e);
                throw initException;
            }
            try {
                setBeanProps(plugin, pp);
            } catch (Exception e) {
                initException = new SchedulerException("JobStore SchedulerPlugin '" + plugInClass + "' props could not be configured.", e);
                throw initException;
            }

            plugins[i] = plugin;
        }

        // Set up any JobListeners
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        Class<?>[] strArg = new Class[] { String.class };
        String[] jobListenerNames = mPropertiesParser.getPropertyGroups(PROP_JOB_LISTENER_PREFIX);
        JobListener[] jobListeners = new JobListener[jobListenerNames.length];
        for (int i = 0; i < jobListenerNames.length; i++) {
            Properties lp = mPropertiesParser.getPropertyGroup(PROP_JOB_LISTENER_PREFIX + "." + jobListenerNames[i], true);

            String listenerClass = lp.getProperty(PROP_LISTENER_CLASS, null);

            if (listenerClass == null) {
                initException = new SchedulerException("JobListener class not specified for listener '" + jobListenerNames[i] + "'");
                throw initException;
            }
            JobListener listener = null;
            try {
                listener = (JobListener) loadHelper.loadClass(listenerClass).newInstance();
            } catch (Exception e) {
                initException = new SchedulerException("JobListener class '" + listenerClass + "' could not be instantiated.", e);
                throw initException;
            }
            try {
                Method nameSetter = listener.getClass().getMethod("setName", strArg);
                if (nameSetter != null) {
                    nameSetter.invoke(listener, new Object[] { jobListenerNames[i] });
                }
                setBeanProps(listener, lp);
            } catch (Exception e) {
                initException = new SchedulerException("JobListener '" + listenerClass + "' props could not be configured.", e);
                throw initException;
            }
            jobListeners[i] = listener;
        }

        // Set up any TriggerListeners
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        String[] triggerListenerNames = mPropertiesParser.getPropertyGroups(PROP_TRIGGER_LISTENER_PREFIX);
        TriggerListener[] triggerListeners = new TriggerListener[triggerListenerNames.length];
        for (int i = 0; i < triggerListenerNames.length; i++) {
            Properties lp = mPropertiesParser.getPropertyGroup(PROP_TRIGGER_LISTENER_PREFIX + "." + triggerListenerNames[i], true);

            String listenerClass = lp.getProperty(PROP_LISTENER_CLASS, null);

            if (listenerClass == null) {
                initException = new SchedulerException("TriggerListener class not specified for listener '" + triggerListenerNames[i] + "'");
                throw initException;
            }
            TriggerListener listener = null;
            try {
                listener = (TriggerListener) loadHelper.loadClass(listenerClass).newInstance();
            } catch (Exception e) {
                initException = new SchedulerException("TriggerListener class '" + listenerClass + "' could not be instantiated.", e);
                throw initException;
            }
            try {
                Method nameSetter = listener.getClass().getMethod("setName", strArg);
                if (nameSetter != null) {
                    nameSetter.invoke(listener, new Object[] { triggerListenerNames[i] });
                }
                setBeanProps(listener, lp);
            } catch (Exception e) {
                initException = new SchedulerException("TriggerListener '" + listenerClass + "' props could not be configured.", e);
                throw initException;
            }
            triggerListeners[i] = listener;
        }

        boolean tpInited = false;
        boolean qsInited = false;

        // Fire everything up
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        try {

            JobRunShellFactory jrsf = null; // Create correct run-shell factory...

            jrsf = new StandardJobRunShellFactory();

            QuartzSchedulerResources rsrcs = new QuartzSchedulerResources();
            rsrcs.setName(schedName);
            rsrcs.setThreadName(threadName);
            rsrcs.setInstanceId(schedInstId);
            rsrcs.setJobRunShellFactory(jrsf);
            rsrcs.setMakeSchedulerThreadDaemon(makeSchedulerThreadDaemon);
            rsrcs.setThreadsInheritInitializersClassLoadContext(threadsInheritInitalizersClassLoader);
            rsrcs.setBatchTimeWindow(batchTimeWindow);
            rsrcs.setMaxBatchSize(maxBatchSize);
            rsrcs.setInterruptJobsOnShutdown(interruptJobsOnShutdown);
            rsrcs.setInterruptJobsOnShutdownWithWait(interruptJobsOnShutdownWithWait);

            SchedulerDetailsSetter.setDetails(threadpool, schedName, schedInstId);

            rsrcs.setThreadPool(threadpool);
            if (threadpool instanceof SimpleThreadPool) {
                ((SimpleThreadPool) threadpool).setThreadNamePrefix(schedName + "_Worker");
                if (threadsInheritInitalizersClassLoader) {
                    ((SimpleThreadPool) threadpool).setThreadsInheritContextClassLoaderOfInitializingThread(threadsInheritInitalizersClassLoader);
                }
            }
            threadpool.initialize();
            tpInited = true;

            rsrcs.setJobStore(jobstore);

            // add plugins
            for (int i = 0; i < plugins.length; i++) {
                rsrcs.addSchedulerPlugin(plugins[i]);
            }

            lQuartzScheduler = new QuartzScheduler(rsrcs, idleWaitTime);
            qsInited = true;

            // // Create Scheduler ref...
            // Scheduler scheduler = instantiate(rsrcs, lQuartzScheduler);

            // set job factory if specified
            if (jobFactory != null) {
                lQuartzScheduler.setJobFactory(jobFactory);
            }

            // Initialize plugins now that we have a Scheduler instance.
            for (int i = 0; i < plugins.length; i++) {
                plugins[i].initialize(pluginNames[i], lQuartzScheduler);
            }

            // add listeners
            for (int i = 0; i < jobListeners.length; i++) {
                lQuartzScheduler.getListenerManager().addJobListener(jobListeners[i], EverythingMatcher.allJobs());
            }
            for (int i = 0; i < triggerListeners.length; i++) {
                lQuartzScheduler.getListenerManager().addTriggerListener(triggerListeners[i], EverythingMatcher.allTriggers());
            }

            // set scheduler context data...
            for (Object key : schedCtxtProps.keySet()) {
                String val = schedCtxtProps.getProperty((String) key);

                lQuartzScheduler.getContext().put(key, val);
            }

            // fire up job store, and runshell factory

            jobstore.setInstanceId(schedInstId);
            jobstore.setInstanceName(schedName);
            jobstore.initialize(loadHelper, lQuartzScheduler.getSchedulerSignaler());
            jobstore.setThreadPoolSize(threadpool.getPoolSize());

            jrsf.initialize(lQuartzScheduler);

            lQuartzScheduler.initialize();

            logger.info("Quartz scheduler '" + lQuartzScheduler.getSchedulerName() + "' initialized from " + propSrc);

            logger.info("Quartz scheduler version: " + lQuartzScheduler.getVersion());

            // prevents the repository from being garbage collected
            lQuartzScheduler.addNoGCObject(schedRep);

            schedRep.bind(lQuartzScheduler);

            return lQuartzScheduler;

        } catch (SchedulerException e) {
            if (qsInited) {
                lQuartzScheduler.shutdown(false);
            } else if (tpInited) {
                threadpool.shutdown(false);
            }
            throw e;
        } catch (RuntimeException re) {
            if (qsInited) {
                lQuartzScheduler.shutdown(false);
            } else if (tpInited) {
                threadpool.shutdown(false);
            }
            throw re;
        } catch (Error re) {
            if (qsInited) {
                lQuartzScheduler.shutdown(false);
            } else if (tpInited) {
                threadpool.shutdown(false);
            }
            throw re;
        }
    }

    // protected Scheduler instantiate(QuartzSchedulerResources rsrcs, QuartzScheduler qs) {
    //
    // Scheduler scheduler = new StdScheduler(qs);
    // return scheduler;
    // }

    private void setBeanProps(Object obj, Properties props) throws NoSuchMethodException, IllegalAccessException, java.lang.reflect.InvocationTargetException, IntrospectionException, SchedulerConfigException {

        props.remove("class");

        BeanInfo bi = Introspector.getBeanInfo(obj.getClass());
        PropertyDescriptor[] propDescs = bi.getPropertyDescriptors();
        PropertiesParser pp = new PropertiesParser(props);

        java.util.Enumeration<Object> keys = props.keys();
        while (keys.hasMoreElements()) {
            String name = (String) keys.nextElement();
            String c = name.substring(0, 1).toUpperCase(Locale.US);
            String methName = "set" + c + name.substring(1);

            java.lang.reflect.Method setMeth = getSetMethod(methName, propDescs);

            try {
                if (setMeth == null) {
                    throw new NoSuchMethodException("No setter for property '" + name + "'");
                }

                Class<?>[] params = setMeth.getParameterTypes();
                if (params.length != 1) {
                    throw new NoSuchMethodException("No 1-argument setter for property '" + name + "'");
                }

                // does the property value reference another property's value? If so, swap to look at its value
                PropertiesParser refProps = pp;
                String refName = pp.getStringProperty(name);
                if (refName != null && refName.startsWith("$@")) {
                    refName = refName.substring(2);
                    refProps = mPropertiesParser;
                } else {
                    refName = name;
                }

                if (params[0].equals(int.class)) {
                    setMeth.invoke(obj, new Object[] { Integer.valueOf(refProps.getIntProperty(refName)) });
                } else if (params[0].equals(long.class)) {
                    setMeth.invoke(obj, new Object[] { Long.valueOf(refProps.getLongProperty(refName)) });
                } else if (params[0].equals(float.class)) {
                    setMeth.invoke(obj, new Object[] { Float.valueOf(refProps.getFloatProperty(refName)) });
                } else if (params[0].equals(double.class)) {
                    setMeth.invoke(obj, new Object[] { Double.valueOf(refProps.getDoubleProperty(refName)) });
                } else if (params[0].equals(boolean.class)) {
                    setMeth.invoke(obj, new Object[] { Boolean.valueOf(refProps.getBooleanProperty(refName)) });
                } else if (params[0].equals(String.class)) {
                    setMeth.invoke(obj, new Object[] { refProps.getStringProperty(refName) });
                } else {
                    throw new NoSuchMethodException("No primitive-type setter for property '" + name + "'");
                }
            } catch (NumberFormatException nfe) {
                throw new SchedulerConfigException("Could not parse property '" + name + "' into correct data type: " + nfe.toString());
            }
        }
    }

    private java.lang.reflect.Method getSetMethod(String name, PropertyDescriptor[] props) {
        for (int i = 0; i < props.length; i++) {
            java.lang.reflect.Method wMeth = props[i].getWriteMethod();

            if (wMeth != null && wMeth.getName().equals(name)) {
                return wMeth;
            }
        }

        return null;
    }

    private Class<?> loadClass(String className) throws ClassNotFoundException, SchedulerConfigException {

        try {
            ClassLoader cl = findClassloader();
            if (cl != null) {
                return cl.loadClass(className);
            }
            throw new SchedulerConfigException("Unable to find a class loader on the current thread or class.");
        } catch (ClassNotFoundException e) {
            if (getClass().getClassLoader() != null) {
                return getClass().getClassLoader().loadClass(className);
            }
            throw e;
        }
    }

    private ClassLoader findClassloader() {
        // work-around set context loader for windows-service started jvms (QUARTZ-748)
        if (Thread.currentThread().getContextClassLoader() == null && getClass().getClassLoader() != null) {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        }
        return Thread.currentThread().getContextClassLoader();
    }

    private String getSchedulerName() {
        return mPropertiesParser.getStringProperty(PROP_SCHED_INSTANCE_NAME, "QuartzScheduler");
    }

    /**
     * <p>
     * Returns a handle to the Scheduler with the given name, if it exists (if it has already been instantiated).
     * </p>
     */
    @Override
    public Scheduler getScheduler(String schedName) throws SchedulerException {
        return SchedulerRepository.getInstance().lookup(schedName);
    }

    /**
     * <p>
     * Returns a handle to all known Schedulers (made by any StdSchedulerFactory instance.).
     * </p>
     */
    @Override
    public Collection<Scheduler> getAllSchedulers() throws SchedulerException {
        return SchedulerRepository.getInstance().lookupAll();
    }
}
