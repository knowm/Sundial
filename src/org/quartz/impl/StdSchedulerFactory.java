/*
 * Copyright 2001-2009 Terracotta, Inc.
 * Copyright 2011 Xeiam, LLC.
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
import java.security.AccessControlException;
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
import org.quartz.spi.InstanceIdGenerator;
import org.quartz.spi.JobFactory;
import org.quartz.spi.JobStore;
import org.quartz.spi.SchedulerPlugin;
import org.quartz.spi.ThreadPool;
import org.quartz.utils.DBConnectionManager;
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

    public static final String PROPERTIES_FILE = "org.quartz.properties";

    public static final String PROP_SCHED_INSTANCE_NAME = "org.quartz.scheduler.instanceName";

    public static final String PROP_SCHED_INSTANCE_ID = "org.quartz.scheduler.instanceId";

    public static final String PROP_SCHED_INSTANCE_ID_GENERATOR_PREFIX = "org.quartz.scheduler.instanceIdGenerator";

    public static final String PROP_SCHED_INSTANCE_ID_GENERATOR_CLASS = PROP_SCHED_INSTANCE_ID_GENERATOR_PREFIX + ".class";

    public static final String PROP_SCHED_THREAD_NAME = "org.quartz.scheduler.threadName";

    public static final String PROP_SCHED_SKIP_UPDATE_CHECK = "org.quartz.scheduler.skipUpdateCheck";

    public static final String PROP_SCHED_BATCH_TIME_WINDOW = "org.quartz.scheduler.batchTriggerAcquisitionFireAheadTimeWindow";

    public static final String PROP_SCHED_MAX_BATCH_SIZE = "org.quartz.scheduler.batchTriggerAcquisitionMaxCount";

    public static final String PROP_SCHED_JMX_EXPORT = "org.quartz.scheduler.jmx.export";

    public static final String PROP_SCHED_JMX_OBJECT_NAME = "org.quartz.scheduler.jmx.objectName";

    public static final String PROP_SCHED_JMX_PROXY = "org.quartz.scheduler.jmx.proxy";

    public static final String PROP_SCHED_JMX_PROXY_CLASS = "org.quartz.scheduler.jmx.proxy.class";

    public static final String PROP_SCHED_RMI_EXPORT = "org.quartz.scheduler.rmi.export";

    public static final String PROP_SCHED_RMI_PROXY = "org.quartz.scheduler.rmi.proxy";

    public static final String PROP_SCHED_RMI_HOST = "org.quartz.scheduler.rmi.registryHost";

    public static final String PROP_SCHED_RMI_PORT = "org.quartz.scheduler.rmi.registryPort";

    public static final String PROP_SCHED_RMI_SERVER_PORT = "org.quartz.scheduler.rmi.serverPort";

    public static final String PROP_SCHED_RMI_CREATE_REGISTRY = "org.quartz.scheduler.rmi.createRegistry";

    public static final String PROP_SCHED_RMI_BIND_NAME = "org.quartz.scheduler.rmi.bindName";

    public static final String PROP_SCHED_WRAP_JOB_IN_USER_TX = "org.quartz.scheduler.wrapJobExecutionInUserTransaction";

    public static final String PROP_SCHED_USER_TX_URL = "org.quartz.scheduler.userTransactionURL";

    public static final String PROP_SCHED_IDLE_WAIT_TIME = "org.quartz.scheduler.idleWaitTime";

    public static final String PROP_SCHED_DB_FAILURE_RETRY_INTERVAL = "org.quartz.scheduler.dbFailureRetryInterval";

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

    public static final String PROP_DATASOURCE_DRIVER = "driver";

    public static final String PROP_DATASOURCE_URL = "URL";

    public static final String PROP_DATASOURCE_USER = "user";

    public static final String PROP_DATASOURCE_PASSWORD = "password";

    public static final String PROP_DATASOURCE_MAX_CONNECTIONS = "maxConnections";

    public static final String PROP_DATASOURCE_VALIDATION_QUERY = "validationQuery";

    public static final String PROP_DATASOURCE_JNDI_URL = "jndiURL";

    public static final String PROP_DATASOURCE_JNDI_ALWAYS_LOOKUP = "jndiAlwaysLookup";

    public static final String PROP_DATASOURCE_JNDI_INITIAL = "java.naming.factory.initial";

    public static final String PROP_DATASOURCE_JNDI_PROVDER = "java.naming.provider.url";

    public static final String PROP_DATASOURCE_JNDI_PRINCIPAL = "java.naming.security.principal";

    public static final String PROP_DATASOURCE_JNDI_CREDENTIALS = "java.naming.security.credentials";

    public static final String PROP_PLUGIN_PREFIX = "org.quartz.plugin";

    public static final String PROP_PLUGIN_CLASS = "class";

    public static final String PROP_JOB_LISTENER_PREFIX = "org.quartz.jobListener";

    public static final String PROP_TRIGGER_LISTENER_PREFIX = "org.quartz.triggerListener";

    public static final String PROP_LISTENER_CLASS = "class";

    public static final String DEFAULT_INSTANCE_ID = "NON_CLUSTERED";

    public static final String AUTO_GENERATE_INSTANCE_ID = "AUTO";
    public static final String SYSTEM_PROPERTY_AS_INSTANCE_ID = "SYS_PROP";

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private SchedulerException initException = null;

    private String propSrc = null;

    private PropertiesParser cfg;

    private final Logger log = LoggerFactory.getLogger(getClass());

    // private Scheduler scheduler;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * Create an uninitialized StdSchedulerFactory.
     */
    public StdSchedulerFactory() {
    }

    /**
     * Create a StdSchedulerFactory that has been initialized via <code>{@link #initialize(Properties)}</code>.
     * 
     * @see #initialize(Properties)
     */
    public StdSchedulerFactory(Properties props) throws SchedulerException {
        initialize(props);
    }

    /**
     * Create a StdSchedulerFactory that has been initialized via <code>{@link #initialize(String)}</code>.
     * 
     * @see #initialize(String)
     */
    public StdSchedulerFactory(String fileName) throws SchedulerException {
        initialize(fileName);
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public Logger getLog() {
        return log;
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
        if (cfg != null) {
            return;
        }
        if (initException != null) {
            throw initException;
        }

        String requestedFile = System.getProperty(PROPERTIES_FILE);
        String propFileName = requestedFile != null ? requestedFile : "quartz.properties";
        File propFile = new File(propFileName);

        Properties props = new Properties();

        InputStream in = null;

        try {
            if (propFile.exists()) {
                try {
                    if (requestedFile != null) {
                        propSrc = "specified file: '" + requestedFile + "'";
                    } else {
                        propSrc = "default file in current working dir: 'quartz.properties'";
                    }

                    in = new BufferedInputStream(new FileInputStream(propFileName));
                    props.load(in);

                } catch (IOException ioe) {
                    initException = new SchedulerException("Properties file: '" + propFileName + "' could not be read.", ioe);
                    throw initException;
                }
            } else if (requestedFile != null) {
                in = Thread.currentThread().getContextClassLoader().getResourceAsStream(requestedFile);

                if (in == null) {
                    initException = new SchedulerException("Properties file: '" + requestedFile + "' could not be found.");
                    throw initException;
                }

                propSrc = "specified file: '" + requestedFile + "' in the class resource path.";

                in = new BufferedInputStream(in);
                try {
                    props.load(in);
                } catch (IOException ioe) {
                    initException = new SchedulerException("Properties file: '" + requestedFile + "' could not be read.", ioe);
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

        initialize(overrideWithSysProps(props));
    }

    /**
     * Add all System properties to the given <code>props</code>. Will override any properties that already exist in the given <code>props</code>.
     */
    private Properties overrideWithSysProps(Properties props) {
        Properties sysProps = null;
        try {
            sysProps = System.getProperties();
        } catch (AccessControlException e) {
            getLog().warn(
                    "Skipping overriding quartz properties with System properties " + "during initialization because of an AccessControlException.  " + "This is likely due to not having read/write access for "
                            + "java.util.PropertyPermission as required by java.lang.System.getProperties().  " + "To resolve this warning, either add this permission to your policy file or "
                            + "use a non-default version of initialize().", e);
        }

        if (sysProps != null) {
            props.putAll(sysProps);
        }

        return props;
    }

    /**
     * <p>
     * Initialize the <code>{@link org.quartz.SchedulerFactory}</code> with the contents of the <code>Properties</code> file with the given name.
     * </p>
     */
    public void initialize(String filename) throws SchedulerException {
        // short-circuit if already initialized
        if (cfg != null) {
            return;
        }

        if (initException != null) {
            throw initException;
        }

        InputStream is = null;
        Properties props = new Properties();

        is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);

        try {
            if (is != null) {
                is = new BufferedInputStream(is);
                propSrc = "the specified file : '" + filename + "' from the class resource path.";
            } else {
                is = new BufferedInputStream(new FileInputStream(filename));
                propSrc = "the specified file : '" + filename + "'";
            }
            props.load(is);
        } catch (IOException ioe) {
            initException = new SchedulerException("Properties file: '" + filename + "' could not be read.", ioe);
            throw initException;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignore) {
                }
            }
        }

        initialize(props);
    }

    /**
     * <p>
     * Initialize the <code>{@link org.quartz.SchedulerFactory}</code> with the contents of the <code>Properties</code> file opened with the given <code>InputStream</code>.
     * </p>
     */
    public void initialize(InputStream propertiesStream) throws SchedulerException {
        // short-circuit if already initialized
        if (cfg != null) {
            return;
        }

        if (initException != null) {
            throw initException;
        }

        Properties props = new Properties();

        if (propertiesStream != null) {
            try {
                props.load(propertiesStream);
                propSrc = "an externally opened InputStream.";
            } catch (IOException e) {
                initException = new SchedulerException("Error loading property data from InputStream", e);
                throw initException;
            }
        } else {
            initException = new SchedulerException("Error loading property data from InputStream - InputStream is null.");
            throw initException;
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

        this.cfg = new PropertiesParser(props);
    }

    private Scheduler instantiate() throws SchedulerException {
        if (cfg == null) {
            initialize();
        }

        if (initException != null) {
            throw initException;
        }

        JobStore js = null;
        ThreadPool tp = null;
        QuartzScheduler qs = null;
        DBConnectionManager dbMgr = null;
        String instanceIdGeneratorClass = null;
        Properties tProps = null;
        String userTXLocation = null;
        boolean wrapJobInTx = false;
        boolean autoId = false;
        long idleWaitTime = -1;
        long dbFailureRetry = -1;
        String classLoadHelperClass;
        String jobFactoryClass;

        SchedulerRepository schedRep = SchedulerRepository.getInstance();

        // Get Scheduler Properties
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        String schedName = cfg.getStringProperty(PROP_SCHED_INSTANCE_NAME, "QuartzScheduler");

        String threadName = cfg.getStringProperty(PROP_SCHED_THREAD_NAME, schedName + "_QuartzSchedulerThread");

        String schedInstId = cfg.getStringProperty(PROP_SCHED_INSTANCE_ID, DEFAULT_INSTANCE_ID);

        if (schedInstId.equals(AUTO_GENERATE_INSTANCE_ID)) {
            autoId = true;
            instanceIdGeneratorClass = cfg.getStringProperty(PROP_SCHED_INSTANCE_ID_GENERATOR_CLASS, "org.quartz.simpl.SimpleInstanceIdGenerator");
        } else if (schedInstId.equals(SYSTEM_PROPERTY_AS_INSTANCE_ID)) {
            autoId = true;
            instanceIdGeneratorClass = "org.quartz.simpl.SystemPropertyInstanceIdGenerator";
        }

        userTXLocation = cfg.getStringProperty(PROP_SCHED_USER_TX_URL, userTXLocation);
        if (userTXLocation != null && userTXLocation.trim().length() == 0) {
            userTXLocation = null;
        }

        classLoadHelperClass = cfg.getStringProperty(PROP_SCHED_CLASS_LOAD_HELPER_CLASS, "org.quartz.simpl.CascadingClassLoadHelper");
        wrapJobInTx = cfg.getBooleanProperty(PROP_SCHED_WRAP_JOB_IN_USER_TX, wrapJobInTx);

        jobFactoryClass = cfg.getStringProperty(PROP_SCHED_JOB_FACTORY_CLASS, null);

        idleWaitTime = cfg.getLongProperty(PROP_SCHED_IDLE_WAIT_TIME, idleWaitTime);
        dbFailureRetry = cfg.getLongProperty(PROP_SCHED_DB_FAILURE_RETRY_INTERVAL, dbFailureRetry);

        boolean makeSchedulerThreadDaemon = cfg.getBooleanProperty(PROP_SCHED_MAKE_SCHEDULER_THREAD_DAEMON);

        boolean threadsInheritInitalizersClassLoader = cfg.getBooleanProperty(PROP_SCHED_SCHEDULER_THREADS_INHERIT_CONTEXT_CLASS_LOADER_OF_INITIALIZING_THREAD);

        boolean skipUpdateCheck = cfg.getBooleanProperty(PROP_SCHED_SKIP_UPDATE_CHECK, false);
        long batchTimeWindow = cfg.getLongProperty(PROP_SCHED_BATCH_TIME_WINDOW, 0L);
        int maxBatchSize = cfg.getIntProperty(PROP_SCHED_MAX_BATCH_SIZE, 1);

        boolean interruptJobsOnShutdown = cfg.getBooleanProperty(PROP_SCHED_INTERRUPT_JOBS_ON_SHUTDOWN, false);
        boolean interruptJobsOnShutdownWithWait = cfg.getBooleanProperty(PROP_SCHED_INTERRUPT_JOBS_ON_SHUTDOWN_WITH_WAIT, false);

        boolean jmxExport = cfg.getBooleanProperty(PROP_SCHED_JMX_EXPORT);
        String jmxObjectName = cfg.getStringProperty(PROP_SCHED_JMX_OBJECT_NAME);

        boolean rmiExport = cfg.getBooleanProperty(PROP_SCHED_RMI_EXPORT, false);
        boolean rmiProxy = cfg.getBooleanProperty(PROP_SCHED_RMI_PROXY, false);
        String rmiHost = cfg.getStringProperty(PROP_SCHED_RMI_HOST, "localhost");
        int rmiPort = cfg.getIntProperty(PROP_SCHED_RMI_PORT, 1099);
        int rmiServerPort = cfg.getIntProperty(PROP_SCHED_RMI_SERVER_PORT, -1);
        String rmiCreateRegistry = cfg.getStringProperty(PROP_SCHED_RMI_CREATE_REGISTRY, QuartzSchedulerResources.CREATE_REGISTRY_NEVER);
        String rmiBindName = cfg.getStringProperty(PROP_SCHED_RMI_BIND_NAME);

        Properties schedCtxtProps = cfg.getPropertyGroup(PROP_SCHED_CONTEXT_PREFIX, true);

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

            tProps = cfg.getPropertyGroup(PROP_SCHED_JOB_FACTORY_PREFIX, true);
            try {
                setBeanProps(jobFactory, tProps);
            } catch (Exception e) {
                initException = new SchedulerException("JobFactory class '" + jobFactoryClass + "' props could not be configured.", e);
                throw initException;
            }
        }

        InstanceIdGenerator instanceIdGenerator = null;
        if (instanceIdGeneratorClass != null) {
            try {
                instanceIdGenerator = (InstanceIdGenerator) loadHelper.loadClass(instanceIdGeneratorClass).newInstance();
            } catch (Exception e) {
                throw new SchedulerConfigException("Unable to instantiate InstanceIdGenerator class: " + e.getMessage(), e);
            }

            tProps = cfg.getPropertyGroup(PROP_SCHED_INSTANCE_ID_GENERATOR_PREFIX, true);
            try {
                setBeanProps(instanceIdGenerator, tProps);
            } catch (Exception e) {
                initException = new SchedulerException("InstanceIdGenerator class '" + instanceIdGeneratorClass + "' props could not be configured.", e);
                throw initException;
            }
        }

        // Get ThreadPool Properties
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        String tpClass = cfg.getStringProperty(PROP_THREAD_POOL_CLASS, SimpleThreadPool.class.getName());

        if (tpClass == null) {
            initException = new SchedulerException("ThreadPool class not specified. ");
            throw initException;
        }

        try {
            tp = (ThreadPool) loadHelper.loadClass(tpClass).newInstance();
        } catch (Exception e) {
            initException = new SchedulerException("ThreadPool class '" + tpClass + "' could not be instantiated.", e);
            throw initException;
        }
        tProps = cfg.getPropertyGroup(PROP_THREAD_POOL_PREFIX, true);
        try {
            setBeanProps(tp, tProps);
        } catch (Exception e) {
            initException = new SchedulerException("ThreadPool class '" + tpClass + "' props could not be configured.", e);
            throw initException;
        }

        // Get JobStore Properties
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        String jsClass = cfg.getStringProperty(PROP_JOB_STORE_CLASS, RAMJobStore.class.getName());

        if (jsClass == null) {
            initException = new SchedulerException("JobStore class not specified. ");
            throw initException;
        }

        try {
            js = (JobStore) loadHelper.loadClass(jsClass).newInstance();
        } catch (Exception e) {
            initException = new SchedulerException("JobStore class '" + jsClass + "' could not be instantiated.", e);
            throw initException;
        }

        SchedulerDetailsSetter.setDetails(js, schedName, schedInstId);

        tProps = cfg.getPropertyGroup(PROP_JOB_STORE_PREFIX, true, new String[] { PROP_JOB_STORE_LOCK_HANDLER_PREFIX });
        try {
            setBeanProps(js, tProps);
        } catch (Exception e) {
            initException = new SchedulerException("JobStore class '" + jsClass + "' props could not be configured.", e);
            throw initException;
        }

        // if (js instanceof JobStoreSupport) {
        // // Install custom lock handler (Semaphore)
        // String lockHandlerClass = cfg.getStringProperty(PROP_JOB_STORE_LOCK_HANDLER_CLASS);
        // if (lockHandlerClass != null) {
        // try {
        // Semaphore lockHandler = (Semaphore) loadHelper.loadClass(lockHandlerClass).newInstance();
        //
        // tProps = cfg.getPropertyGroup(PROP_JOB_STORE_LOCK_HANDLER_PREFIX, true);
        //
        // // If this lock handler requires the table prefix, add it to its properties.
        // if (lockHandler instanceof TablePrefixAware) {
        // tProps.setProperty(PROP_TABLE_PREFIX, ((JobStoreSupport) js).getTablePrefix());
        // tProps.setProperty(PROP_SCHED_NAME, schedName);
        // }
        //
        // try {
        // setBeanProps(lockHandler, tProps);
        // } catch (Exception e) {
        // initException = new SchedulerException("JobStore LockHandler class '" + lockHandlerClass + "' props could not be configured.", e);
        // throw initException;
        // }
        //
        // ((JobStoreSupport) js).setLockHandler(lockHandler);
        // getLog().info("Using custom data access locking (synchronization): " + lockHandlerClass);
        // } catch (Exception e) {
        // initException = new SchedulerException("JobStore LockHandler class '" + lockHandlerClass + "' could not be instantiated.", e);
        // throw initException;
        // }
        // }
        // }

        // Set up any DataSources
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // String[] dsNames = cfg.getPropertyGroups(PROP_DATASOURCE_PREFIX);
        // for (int i = 0; i < dsNames.length; i++) {
        // PropertiesParser pp = new PropertiesParser(cfg.getPropertyGroup(
        // PROP_DATASOURCE_PREFIX + "." + dsNames[i], true));
        //
        // String cpClass = pp.getStringProperty(PROP_CONNECTION_PROVIDER_CLASS, null);
        //
        // // custom connectionProvider...
        // if(cpClass != null) {
        // ConnectionProvider cp = null;
        // try {
        // cp = (ConnectionProvider) loadHelper.loadClass(cpClass).newInstance();
        // } catch (Exception e) {
        // initException = new SchedulerException("ConnectionProvider class '" + cpClass
        // + "' could not be instantiated.", e);
        // throw initException;
        // }
        //
        // try {
        // // remove the class name, so it isn't attempted to be set
        // pp.getUnderlyingProperties().remove(
        // PROP_CONNECTION_PROVIDER_CLASS);
        //
        // setBeanProps(cp, pp.getUnderlyingProperties());
        // } catch (Exception e) {
        // initException = new SchedulerException("ConnectionProvider class '" + cpClass
        // + "' props could not be configured.", e);
        // throw initException;
        // }
        //
        // dbMgr = DBConnectionManager.getInstance();
        // dbMgr.addConnectionProvider(dsNames[i], cp);
        // } else {
        // String dsJndi = pp.getStringProperty(PROP_DATASOURCE_JNDI_URL, null);
        //
        // if (dsJndi != null) {
        // boolean dsAlwaysLookup = pp.getBooleanProperty(
        // PROP_DATASOURCE_JNDI_ALWAYS_LOOKUP);
        // String dsJndiInitial = pp.getStringProperty(
        // PROP_DATASOURCE_JNDI_INITIAL);
        // String dsJndiProvider = pp.getStringProperty(
        // PROP_DATASOURCE_JNDI_PROVDER);
        // String dsJndiPrincipal = pp.getStringProperty(
        // PROP_DATASOURCE_JNDI_PRINCIPAL);
        // String dsJndiCredentials = pp.getStringProperty(
        // PROP_DATASOURCE_JNDI_CREDENTIALS);
        // Properties props = null;
        // if (null != dsJndiInitial || null != dsJndiProvider
        // || null != dsJndiPrincipal || null != dsJndiCredentials) {
        // props = new Properties();
        // if (dsJndiInitial != null) {
        // props.put(PROP_DATASOURCE_JNDI_INITIAL,
        // dsJndiInitial);
        // }
        // if (dsJndiProvider != null) {
        // props.put(PROP_DATASOURCE_JNDI_PROVDER,
        // dsJndiProvider);
        // }
        // if (dsJndiPrincipal != null) {
        // props.put(PROP_DATASOURCE_JNDI_PRINCIPAL,
        // dsJndiPrincipal);
        // }
        // if (dsJndiCredentials != null) {
        // props.put(PROP_DATASOURCE_JNDI_CREDENTIALS,
        // dsJndiCredentials);
        // }
        // }
        // JNDIConnectionProvider cp = new JNDIConnectionProvider(dsJndi,
        // props, dsAlwaysLookup);
        // dbMgr = DBConnectionManager.getInstance();
        // dbMgr.addConnectionProvider(dsNames[i], cp);
        // } else {
        // String dsDriver = pp.getStringProperty(PROP_DATASOURCE_DRIVER);
        // String dsURL = pp.getStringProperty(PROP_DATASOURCE_URL);
        // String dsUser = pp.getStringProperty(PROP_DATASOURCE_USER, "");
        // String dsPass = pp.getStringProperty(PROP_DATASOURCE_PASSWORD, "");
        // int dsCnt = pp.getIntProperty(PROP_DATASOURCE_MAX_CONNECTIONS, 10);
        // String dsValidation = pp.getStringProperty(PROP_DATASOURCE_VALIDATION_QUERY);
        //
        // if (dsDriver == null) {
        // initException = new SchedulerException(
        // "Driver not specified for DataSource: "
        // + dsNames[i]);
        // throw initException;
        // }
        // if (dsURL == null) {
        // initException = new SchedulerException(
        // "DB URL not specified for DataSource: "
        // + dsNames[i]);
        // throw initException;
        // }
        // try {
        // PoolingConnectionProvider cp = new PoolingConnectionProvider(
        // dsDriver, dsURL, dsUser, dsPass, dsCnt,
        // dsValidation);
        // dbMgr = DBConnectionManager.getInstance();
        // dbMgr.addConnectionProvider(dsNames[i], cp);
        // } catch (SQLException sqle) {
        // initException = new SchedulerException(
        // "Could not initialize DataSource: " + dsNames[i],
        // sqle);
        // throw initException;
        // }
        // }
        //
        // }
        //
        // }

        // Set up any SchedulerPlugins
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        String[] pluginNames = cfg.getPropertyGroups(PROP_PLUGIN_PREFIX);
        SchedulerPlugin[] plugins = new SchedulerPlugin[pluginNames.length];
        for (int i = 0; i < pluginNames.length; i++) {
            Properties pp = cfg.getPropertyGroup(PROP_PLUGIN_PREFIX + "." + pluginNames[i], true);

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
        String[] jobListenerNames = cfg.getPropertyGroups(PROP_JOB_LISTENER_PREFIX);
        JobListener[] jobListeners = new JobListener[jobListenerNames.length];
        for (int i = 0; i < jobListenerNames.length; i++) {
            Properties lp = cfg.getPropertyGroup(PROP_JOB_LISTENER_PREFIX + "." + jobListenerNames[i], true);

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

        String[] triggerListenerNames = cfg.getPropertyGroups(PROP_TRIGGER_LISTENER_PREFIX);
        TriggerListener[] triggerListeners = new TriggerListener[triggerListenerNames.length];
        for (int i = 0; i < triggerListenerNames.length; i++) {
            Properties lp = cfg.getPropertyGroup(PROP_TRIGGER_LISTENER_PREFIX + "." + triggerListenerNames[i], true);

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

            // if (userTXLocation != null) {
            // UserTransactionHelper.setUserTxLocation(userTXLocation);
            // }
            //
            // if (wrapJobInTx) {
            // jrsf = new JTAJobRunShellFactory();
            // } else {
            jrsf = new StandardJobRunShellFactory();
            // }

            if (autoId) {
                try {
                    schedInstId = DEFAULT_INSTANCE_ID;
                    if (js.isClustered()) {
                        schedInstId = instanceIdGenerator.generateInstanceId();
                    }
                } catch (Exception e) {
                    getLog().error("Couldn't generate instance Id!", e);
                    throw new IllegalStateException("Cannot run without an instance id.");
                }
            }

            if (js.getClass().getName().startsWith("org.terracotta.quartz")) {
                try {
                    String uuid = (String) js.getClass().getMethod("getUUID").invoke(js);
                    if (schedInstId.equals(DEFAULT_INSTANCE_ID)) {
                        schedInstId = "TERRACOTTA_CLUSTERED,node=" + uuid;
                        if (jmxObjectName == null) {
                            jmxObjectName = QuartzSchedulerResources.generateJMXObjectName(schedName, schedInstId);
                        }
                    } else if (jmxObjectName == null) {
                        jmxObjectName = QuartzSchedulerResources.generateJMXObjectName(schedName, schedInstId + ",node=" + uuid);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Problem obtaining node id from TerracottaJobStore.", e);
                }

                if (null == cfg.getStringProperty(PROP_SCHED_JMX_EXPORT)) {
                    jmxExport = true;
                }
            }

            // if (js instanceof JobStoreSupport) {
            // JobStoreSupport jjs = (JobStoreSupport) js;
            // jjs.setDbRetryInterval(dbFailureRetry);
            // if (threadsInheritInitalizersClassLoader) {
            // jjs.setThreadsInheritInitializersClassLoadContext(threadsInheritInitalizersClassLoader);
            // }
            // }

            QuartzSchedulerResources rsrcs = new QuartzSchedulerResources();
            rsrcs.setName(schedName);
            rsrcs.setThreadName(threadName);
            rsrcs.setInstanceId(schedInstId);
            rsrcs.setJobRunShellFactory(jrsf);
            rsrcs.setMakeSchedulerThreadDaemon(makeSchedulerThreadDaemon);
            rsrcs.setThreadsInheritInitializersClassLoadContext(threadsInheritInitalizersClassLoader);
            rsrcs.setRunUpdateCheck(!skipUpdateCheck);
            rsrcs.setBatchTimeWindow(batchTimeWindow);
            rsrcs.setMaxBatchSize(maxBatchSize);
            rsrcs.setInterruptJobsOnShutdown(interruptJobsOnShutdown);
            rsrcs.setInterruptJobsOnShutdownWithWait(interruptJobsOnShutdownWithWait);
            rsrcs.setJMXExport(jmxExport);
            rsrcs.setJMXObjectName(jmxObjectName);

            if (rmiExport) {
                rsrcs.setRMIRegistryHost(rmiHost);
                rsrcs.setRMIRegistryPort(rmiPort);
                rsrcs.setRMIServerPort(rmiServerPort);
                rsrcs.setRMICreateRegistryStrategy(rmiCreateRegistry);
                rsrcs.setRMIBindName(rmiBindName);
            }

            SchedulerDetailsSetter.setDetails(tp, schedName, schedInstId);

            rsrcs.setThreadPool(tp);
            if (tp instanceof SimpleThreadPool) {
                ((SimpleThreadPool) tp).setThreadNamePrefix(schedName + "_Worker");
                if (threadsInheritInitalizersClassLoader) {
                    ((SimpleThreadPool) tp).setThreadsInheritContextClassLoaderOfInitializingThread(threadsInheritInitalizersClassLoader);
                }
            }
            tp.initialize();
            tpInited = true;

            rsrcs.setJobStore(js);

            // add plugins
            for (int i = 0; i < plugins.length; i++) {
                rsrcs.addSchedulerPlugin(plugins[i]);
            }

            qs = new QuartzScheduler(rsrcs, idleWaitTime, dbFailureRetry);
            qsInited = true;

            // Create Scheduler ref...
            Scheduler scheduler = instantiate(rsrcs, qs);

            // set job factory if specified
            if (jobFactory != null) {
                qs.setJobFactory(jobFactory);
            }

            // Initialize plugins now that we have a Scheduler instance.
            for (int i = 0; i < plugins.length; i++) {
                plugins[i].initialize(pluginNames[i], scheduler);
            }

            // add listeners
            for (int i = 0; i < jobListeners.length; i++) {
                qs.getListenerManager().addJobListener(jobListeners[i], EverythingMatcher.allJobs());
            }
            for (int i = 0; i < triggerListeners.length; i++) {
                qs.getListenerManager().addTriggerListener(triggerListeners[i], EverythingMatcher.allTriggers());
            }

            // set scheduler context data...
            for (Object key : schedCtxtProps.keySet()) {
                String val = schedCtxtProps.getProperty((String) key);

                scheduler.getContext().put(key, val);
            }

            // fire up job store, and runshell factory

            js.setInstanceId(schedInstId);
            js.setInstanceName(schedName);
            js.initialize(loadHelper, qs.getSchedulerSignaler());
            js.setThreadPoolSize(tp.getPoolSize());

            jrsf.initialize(scheduler);

            qs.initialize();

            getLog().info("Quartz scheduler '" + scheduler.getSchedulerName() + "' initialized from " + propSrc);

            getLog().info("Quartz scheduler version: " + qs.getVersion());

            // prevents the repository from being garbage collected
            qs.addNoGCObject(schedRep);
            // prevents the db manager from being garbage collected
            if (dbMgr != null) {
                qs.addNoGCObject(dbMgr);
            }

            schedRep.bind(scheduler);
            return scheduler;
        } catch (SchedulerException e) {
            if (qsInited) {
                qs.shutdown(false);
            } else if (tpInited) {
                tp.shutdown(false);
            }
            throw e;
        } catch (RuntimeException re) {
            if (qsInited) {
                qs.shutdown(false);
            } else if (tpInited) {
                tp.shutdown(false);
            }
            throw re;
        } catch (Error re) {
            if (qsInited) {
                qs.shutdown(false);
            } else if (tpInited) {
                tp.shutdown(false);
            }
            throw re;
        }
    }

    protected Scheduler instantiate(QuartzSchedulerResources rsrcs, QuartzScheduler qs) {

        Scheduler scheduler = new StdScheduler(qs);
        return scheduler;
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
                    refProps = cfg;
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
        return cfg.getStringProperty(PROP_SCHED_INSTANCE_NAME, "QuartzScheduler");
    }

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
        if (cfg == null) {
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
     * Returns a handle to the default Scheduler, creating it if it does not yet exist.
     * </p>
     * 
     * @see #initialize()
     */
    public static Scheduler getDefaultScheduler() throws SchedulerException {
        StdSchedulerFactory fact = new StdSchedulerFactory();

        return fact.getScheduler();
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
