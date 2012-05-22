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

import com.github.nethad.clustermeister.api.Loggers;
import com.github.nethad.clustermeister.api.utils.NodeManagementConnector;
import com.github.nethad.clustermeister.node.common.ClustermeisterLauncher;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.apache.commons.configuration.Configuration;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class JPPFLocalDriver implements Observer {
    private ClustermeisterLauncher launcher = null;
    
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(Loggers.PROVISIONING);

    public static final int SERVER_PORT = 11111;
    public static final int MANAGEMENT_PORT = 11198;
//    private String publicIp;
    private SettableFuture<String> publicIpFuture = SettableFuture.create();
    private final Configuration configuration;

    public JPPFLocalDriver(Configuration configuration) {
        this.configuration = configuration;
    }
    
    public void execute() {
        localSetupAndRun();
    }

    private void localSetupAndRun() {
        launcher = JPPFConfiguratedComponentFactory.getInstance().
                createLocalDriver(SERVER_PORT, MANAGEMENT_PORT, configuration);
    }

    public String getIpAddress() {
        try {
            return publicIpFuture.get();
        } catch (InterruptedException ex) {
            logger.warn("Interrupted.", ex);
        } catch (ExecutionException ex) {
            logger.warn("Exception raised while requesting public IP.", ex);
        }
        return null;
//        if (publicIp == null) {
//            publicIp = PublicIp.getInstance().getPublicIp();
//            logger.info("public IP = "+publicIp);
//        }
//        return publicIp;
    }
    
    public void shutdown() {
        if(launcher != null) {
            try {
                logger.info("Shutting down local driver.");
                launcher.shutdownProcess();
                logger.info("Shutdown complete.");
            } catch (Exception ex) {
                logger.error("Error while shutting down local driver.", ex);
            }
        }
    }

    private void shutdownWithJMXConnection() {
        JMXDriverConnectionWrapper wrapper = null;
        try {
            logger.info("Shutting down local driver.");
            wrapper =
                    NodeManagementConnector.openDriverConnection("localhost", MANAGEMENT_PORT);
            wrapper.restartShutdown(1 * 1000L, -1L);
            logger.info("Shutdown complete.");
        } catch (TimeoutException ex) {
            logger.error("Error while shutting down local driver.", ex);
        } catch (Exception ex) {
            logger.error("Error while shutting down local driver.", ex);
        } finally {
            if (wrapper != null) {
                try {
                    wrapper.close();
                } catch (Exception ex) {
                    logger.warn("Could not close JMX connection to driver.");
                }
            }
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof String) {
            this.publicIpFuture.set((String)arg);
        } else {
            this.publicIpFuture.setException(new IllegalArgumentException(String.format("String expected, but was (%s)", arg)));
        }
    }

}
