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
import java.util.Locale;
import java.util.Properties;

import org.quartz.Scheduler;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.core.JobRunShellFactory;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.core.StandardJobRunShellFactory;
import org.quartz.impl.matchers.EverythingMatcher;
import org.quartz.plugins.xml.XMLSchedulingDataProcessorPlugin;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.JobStore;
import org.quartz.utils.PropertiesParser;
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

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constants. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public static final String PROP_SCHED_MAKE_SCHEDULER_THREAD_DAEMON = "org.quartz.scheduler.makeSchedulerThreadDaemon";

    public static final String PROP_SCHED_SCHEDULER_THREADS_INHERIT_CONTEXT_CLASS_LOADER_OF_INITIALIZING_THREAD = "org.quartz.scheduler.threadsInheritContextClassLoaderOfInitializer";

    public static final String PROP_SCHED_CLASS_LOAD_HELPER_CLASS = "org.quartz.scheduler.classLoadHelper.class";

    public static final String PROP_SCHED_CONTEXT_PREFIX = "org.quartz.context.key";

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private SchedulerException initException = null;

    private String propSrc = null;

    private PropertiesParser mPropertiesParser;

    private final Logger logger = LoggerFactory.getLogger(getClass());

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

        if (mPropertiesParser == null) {
            initialize();
        }

        return instantiate();
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
        SimpleThreadPool threadpool = null;
        Properties tProps = null;
        String classLoadHelperClass;

        // Get Scheduler Properties
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        classLoadHelperClass = mPropertiesParser.getStringProperty(PROP_SCHED_CLASS_LOAD_HELPER_CLASS, "org.quartz.simpl.CascadingClassLoadHelper");

        boolean makeSchedulerThreadDaemon = mPropertiesParser.getBooleanProperty(PROP_SCHED_MAKE_SCHEDULER_THREAD_DAEMON);

        boolean threadsInheritInitalizersClassLoader = mPropertiesParser.getBooleanProperty(PROP_SCHED_SCHEDULER_THREADS_INHERIT_CONTEXT_CLASS_LOADER_OF_INITIALIZING_THREAD);

        // boolean interruptJobsOnShutdown = mPropertiesParser.getBooleanProperty(PROP_SCHED_INTERRUPT_JOBS_ON_SHUTDOWN, false);
        // boolean interruptJobsOnShutdownWithWait = mPropertiesParser.getBooleanProperty(PROP_SCHED_INTERRUPT_JOBS_ON_SHUTDOWN_WITH_WAIT, false);

        Properties schedCtxtProps = mPropertiesParser.getPropertyGroup(PROP_SCHED_CONTEXT_PREFIX, true);

        // Create class load helper
        ClassLoadHelper loadHelper = null;
        try {
            loadHelper = (ClassLoadHelper) loadClass(classLoadHelperClass).newInstance();
        } catch (Exception e) {
            throw new SchedulerConfigException("Unable to instantiate class load helper class: " + e.getMessage(), e);
        }
        loadHelper.initialize();

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
            rsrcs.setMakeSchedulerThreadDaemon(makeSchedulerThreadDaemon);
            rsrcs.setThreadsInheritInitializersClassLoadContext(threadsInheritInitalizersClassLoader);
            rsrcs.setBatchTimeWindow(0L);
            rsrcs.setMaxBatchSize(1);
            rsrcs.setInterruptJobsOnShutdown(true);
            rsrcs.setInterruptJobsOnShutdownWithWait(true);

            rsrcs.setThreadPool(threadpool);
            if (threadpool instanceof SimpleThreadPool) {
                (threadpool).setThreadNamePrefix("Quartz_Scheduler_Worker");
                if (threadsInheritInitalizersClassLoader) {
                    (threadpool).setThreadsInheritContextClassLoaderOfInitializingThread(threadsInheritInitalizersClassLoader);
                }
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

            // set scheduler context data...
            for (Object key : schedCtxtProps.keySet()) {
                String val = schedCtxtProps.getProperty((String) key);

                mQuartzScheduler.getContext().put(key, val);
            }

            // fire up job store, and runshell factory
            jobstore.initialize(loadHelper, mQuartzScheduler.getSchedulerSignaler());
            jobstore.setThreadPoolSize(threadpool.getPoolSize());

            jrsf.initialize(mQuartzScheduler);

            mQuartzScheduler.initialize();

            logger.info("Quartz scheduler initialized from " + propSrc);

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

}
