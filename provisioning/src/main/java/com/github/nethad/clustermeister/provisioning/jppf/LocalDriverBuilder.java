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
import com.github.nethad.clustermeister.node.common.ClustermeisterProcessLauncher;
import com.github.nethad.clustermeister.node.common.builders.PluginConfiguratedJPPFComponentBuilder;
import com.github.nethad.clustermeister.provisioning.ConfigurationKeys;
import com.google.common.util.concurrent.Monitor;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds and launches a new local driver component.
 *
 * @author daniel
 */
public class LocalDriverBuilder extends PluginConfiguratedJPPFComponentBuilder<ClustermeisterLauncher> {
    private static final Logger logger = 
            LoggerFactory.getLogger(LocalDriverBuilder.class);
    
    private static final String DRIVER_THREAD_NAME = "CMLocalDriverThread";
    
    private final int serverPort;
    
    private final int managementPort;
    
    private final Configuration configuration;

    /**
     * Creates a new builder.
     * 
     * @param serverPort the server port
     * @param managementPort the management port
     * @param configuration the configuration
     */
    public LocalDriverBuilder(int serverPort, int managementPort, Configuration configuration) {
        this.serverPort = serverPort;
        this.managementPort = managementPort;
        this.configuration = configuration;
    }
    
    @Override
    protected ClustermeisterLauncher doBuild() {
        JPPFDriverConfigurationSource.serverPort = serverPort;
        JPPFDriverConfigurationSource.managementPort = managementPort;
        JPPFDriverConfigurationSource.jvmOptions = configuration.getString(ConfigurationKeys.JVM_OPTIONS_LOCAL_DRIVER, "");
        Map<String, String> loadBalancingConfigValues = new DriverLoadBalancing(configuration).getLoadBalancingConfigValues();
        if (loadBalancingConfigValues.isEmpty()) {
//                logger.info("No load balancing settings set.");
        } else {
            for (Map.Entry<String, String> entry : loadBalancingConfigValues.entrySet()) {
//                    logger.info("{} => {}", entry.getKey(), entry.getValue());
            }
        }
        JPPFDriverConfigurationSource.loadBalancing = new DriverLoadBalancing(configuration).getLoadBalancingConfigValues();
        final ClustermeisterLauncher launcher = new ClustermeisterDriverLauncher(true);
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
                    launcher.doLaunch(true, ClustermeisterProcessLauncher.StreamSink.LOG);
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
    }

    @Override
    protected String getConfigurationClassName() {
        return JPPFDriverConfigurationSource.class.getCanonicalName();
    }
}
