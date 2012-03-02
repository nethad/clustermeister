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

import com.github.nethad.clustermeister.provisioning.jppf.managementtasks.JPPFConfigReaderTask;
import com.github.nethad.clustermeister.provisioning.jppf.managementtasks.JPPFShutdownTask;
import com.github.nethad.clustermeister.provisioning.jppf.managementtasks.ShutdownSingleNodeTask;
import com.github.nethad.clustermeister.provisioning.utils.NodeManagementConnector;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.policy.Equal;
import org.jppf.server.protocol.JPPFTask;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
public class JPPFManagementByJobsClient {

    private final static org.slf4j.Logger logger =
            LoggerFactory.getLogger(JPPFManagementByJobsClient.class);
    
    private final JPPFClient jPPFClient;

    /**
     * Default constructor: intended to be used only by JPPFConfiguratedComponentFactory.
     */
    JPPFManagementByJobsClient() {
        jPPFClient = new JPPFClient();
    }

    public Properties getJPPFConfig(String host, int port) {
        JPPFJob job = new JPPFJob();
        try {
            job.addTask(new JPPFConfigReaderTask(), host, port);
        } catch (JPPFException ex) {
            throw new RuntimeException(ex);
        }
        job.getSLA().setMaxNodes(1);
        job.setBlocking(true);
        job.getSLA().setSuspended(false);
        List<JPPFTask> results = Collections.EMPTY_LIST;
        try {
            results = jPPFClient.submit(job);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        JPPFTask task = Iterables.getOnlyElement(results);
        return (Properties) task.getResult();
    }

    /**
     * Shuts a single node (not driver) down.
     */
    public boolean shutdownNode(String nodeHost, int managementPort) {
        JPPFJob job = new JPPFJob();
        try {
            job.addTask(new ShutdownSingleNodeTask(), nodeHost, managementPort);
            job.getSLA().setMaxNodes(1);
            job.setBlocking(false);
            job.getSLA().setSuspended(false);
            jPPFClient.submit(job);
        } catch (Exception ex) {
            logger.warn("Failed to shut down node.", ex);
            return false;
        }

        return true;
    }

    public void shutdownAllNodes(String driverHost, int managementPort) {
        try {
            JMXDriverConnectionWrapper wrapper = 
                    NodeManagementConnector.openDriverConnection(driverHost, managementPort);
            List<String> sockets = new ArrayList<String>();
            for (JPPFManagementInfo nodeInfo : wrapper.nodesInformation()) {
                sockets.add(nodeInfo.getHost() + ":" + nodeInfo.getPort());
            }

            JPPFJob job = new JPPFJob();
            job.setName("Shutdown Job");
            String[] split = sockets.get(0).split(":");
            int port = Integer.parseInt(split[1]);
            logger.debug("Chose node with port {} as executor.", port);
            job.addTask(new JPPFShutdownTask(), sockets, port);
            job.getSLA().setMaxNodes(1);
            job.getSLA().setExecutionPolicy(new Equal("jppf.management.port", port));

            job.setBlocking(false);
            jPPFClient.submit(job);

            logger.debug("Waiting for all nodes to shut down...");
            while (wrapper.nodesInformation().size() > 0) {
                Thread.sleep(1000);
            }
            logger.debug("All nodes are shut down. Canceling Job: {}", job.getName());
            wrapper.cancelJob(job.getUuid());
            wrapper.close();
        } catch (Exception e) {
            logger.warn("Failed to shut down all nodes.", e);
        } finally {
            if (jPPFClient != null) {
                jPPFClient.close();
            }
        }
    }

    public void shutdownDriver(String driverHost, int managementPort) {
        try {
            JMXDriverConnectionWrapper wrapper = 
                    NodeManagementConnector.openDriverConnection(driverHost, managementPort);
            wrapper.restartShutdown(0L, -1L);
        } catch (TimeoutException ex) {
            logger.warn("Timed out waiting for driver management connection.", ex);
        } catch (Exception ex) {
            logger.warn("Could not shut down driver.", ex);
        }
    }

    public void close() {
        if (jPPFClient != null) {
            jPPFClient.close();
        }
    }
}
