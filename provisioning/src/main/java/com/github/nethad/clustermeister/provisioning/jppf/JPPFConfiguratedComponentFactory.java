/*
 * Copyright 2012 The Clustermeister Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.nethad.clustermeister.provisioning.jppf;

import com.github.nethad.clustermeister.node.common.ClustermeisterDriverLauncher;
import com.github.nethad.clustermeister.node.common.ClustermeisterLauncher;
import com.google.common.util.concurrent.Monitor;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates instances that need a custom JPPF ConfigurationSource.
 *
 * This class coordinates the System property settings between concurrent calls.
 *
 * This class is a singleton.
 *
 * @author daniel
 */
public class JPPFConfiguratedComponentFactory {
    private static final String DRIVER_THREAD_NAME = "CMLocalDriverThread";

    
    /**
     * The "jppf.config.plugin" property.
     */
    protected static final String JPPF_CONFIG_PLUGIN = "jppf.config.plugin";
    
    private final static Logger logger = 
			LoggerFactory.getLogger(JPPFConfiguratedComponentFactory.class);

    private static int configClassId = 1;
    
    private final Monitor configPropertyMonitor = new Monitor(false);

    private JPPFConfiguratedComponentFactory() {
        //private default constructor
    }

    /**
     * Get the only instance of JPPFConfiguratedComponentFactory.
     *
     * @return the JPPFConfiguratedComponentFactory instance.
     */
    public static JPPFConfiguratedComponentFactory getInstance() {
        return NewSingletonHolder.INSTANCE;
    }

    private static String createConfigurationSource(Properties properties) {
        ClassPool classPool = ClassPool.getDefault();
        CtClass classPrototype = classPool.makeClass(
                "GenericClustermeisterConfigurationSource#" + configClassId++);
        try {
            classPrototype.setInterfaces(new CtClass[]{
                classPool.get("org.jppf.utils.JPPFConfiguration$ConfigurationSource")});
            StringBuilder method = new StringBuilder(
                    "public java.io.InputStream getPropertyStream() throws java.io.IOException { ");
            
            method.append("java.util.Properties properties = new java.util.Properties(); ");
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                method.append("properties.setProperty(\"");
                method.append(entry.getKey());
                method.append("\", \"");
                method.append(entry.getValue());
                method.append("\"); ");
            }
            method.append("java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(); ");
            method.append("properties.store(baos, \"Generic configuration\"); ");
            method.append("return new java.io.ByteArrayInputStream(baos.toByteArray());");
            method.append(" }");
            classPrototype.addMethod(CtMethod.make(method.toString(), classPrototype));
            Class clazz = classPrototype.toClass();
            return clazz.getCanonicalName();
        } catch (Exception ex) {
            logger.error("Can not create generic configuration source class.");
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Create a JPPFManagementByJobsClient instance.
     *
     * @return	the JPPFManagementByJobsClient instance.
     */
    public JPPFManagementByJobsClient createManagementByJobsClient(String host, int port) {
        Properties properties = new Properties();
        properties.setProperty("jppf.discovery.enabled", "false");
        properties.setProperty("jppf.drivers", "driver-1");
        properties.setProperty("driver-1.jppf.server.host", host);
        properties.setProperty("driver-1.jppf.server.port", String.valueOf(port));

        configPropertyMonitor.enter();
        try {
            setConfigProperty(createConfigurationSource(properties));
            return new JPPFManagementByJobsClient();
        } finally {
            configPropertyMonitor.leave();
        }
    }
	
    public ClustermeisterLauncher createLocalDriver(int serverPort, int managementPort) {
        configPropertyMonitor.enter();
        try {
            JPPFDriverConfigurationSource.serverPort = serverPort;
            JPPFDriverConfigurationSource.managementPort = managementPort;
            setConfigProperty(JPPFDriverConfigurationSource.class.getCanonicalName());
            final ClustermeisterLauncher launcher = new ClustermeisterDriverLauncher();
            final AtomicBoolean initialized = new AtomicBoolean(false);
            final Monitor initializationMonitor = new Monitor(false);
            final Monitor.Guard isInitialized = new Monitor.Guard(initializationMonitor) {
                @Override
                public boolean isSatisfied() {
                    return initialized.get();
                }
            };
            launcher.addObserver(new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    initializationMonitor.enter();
                    try {
                        initialized.set(true);
                    } finally {
                        initializationMonitor.leave();
                    }
                }
            });
            Thread driverThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        launcher.doLaunch(true);
                    } catch(Throwable ex) {
                        logger.warn("Execption from local driver thread.", ex);
                    }
                }
            });
            driverThread.setName(String.format("%s-%s", DRIVER_THREAD_NAME, driverThread.getId()));
            driverThread.start();
            
            //wait for driver to initialize.
            initializationMonitor.enter();
            try {
                try {
                    initializationMonitor.waitFor(isInitialized);
                } catch (InterruptedException ex) {
                    logger.warn("Interrupted while waiting for local driver to initialize! "
                            + "Initialization may not be complete.", ex);
                }
            } finally {
                initializationMonitor.leave();
            }
            return launcher;
        } finally {
            configPropertyMonitor.leave();
        }
    }

    private void setConfigProperty(String className) {
        System.setProperty(JPPF_CONFIG_PLUGIN, className);
    }

    private static class NewSingletonHolder {

        private static final JPPFConfiguratedComponentFactory INSTANCE =
                new JPPFConfiguratedComponentFactory();
    }
}
