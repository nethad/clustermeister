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

import com.github.nethad.clustermeister.api.utils.NodeManagementConnector;
import com.github.nethad.clustermeister.node.ClustermeisterLauncher;
import com.github.nethad.clustermeister.provisioning.utils.*;
import java.util.concurrent.TimeoutException;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class JPPFLocalDriver {
    private ClustermeisterLauncher launcher = null;
    
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(JPPFLocalDriver.class);

    public static final int SERVER_PORT = 11111;
    public static final int MANAGEMENT_PORT = 11198;
    private String publicIp;

    public void execute() {
        localSetupAndRun();
    }

    private void localSetupAndRun() {
        launcher = JPPFConfiguratedComponentFactory.getInstance().
                createLocalDriver(SERVER_PORT, MANAGEMENT_PORT);
    }

    public String getIpAddress() {
        if (publicIp == null) {
            publicIp = PublicIp.getPublicIp();
            logger.info("public IP = "+publicIp);
        }
        return publicIp;
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

}
