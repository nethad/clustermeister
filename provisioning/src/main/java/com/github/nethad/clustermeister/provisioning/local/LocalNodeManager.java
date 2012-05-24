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
package com.github.nethad.clustermeister.provisioning.local;

import com.github.nethad.clustermeister.api.Loggers;
import com.github.nethad.clustermeister.api.NodeConfiguration;
import com.github.nethad.clustermeister.provisioning.CommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFConfiguratedComponentFactory;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFLocalDriver;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFManagementByJobsClient;
import com.github.nethad.clustermeister.provisioning.rmi.RmiServerForApi;
import java.util.Collection;
import java.util.LinkedList;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class LocalNodeManager {
    
    private static final Logger logger = LoggerFactory.getLogger(Loggers.PROVISIONING);
    
    private final Configuration configuration;
    private Collection<JPPFLocalNode> localNodes;
//    private JPPFLocalNode node;
    
    private LocalNodeManager(Configuration configuration) {
        this.configuration = configuration;
        localNodes = new LinkedList<JPPFLocalNode>();
    }
    
    public static CommandLineEvaluation commandLineEvaluation(Configuration configuration, 
            CommandLineHandle commandLineHandle, RmiServerForApi rmiServerForApi) {
        LocalNodeManager nodeManager = new LocalNodeManager(configuration);
        return nodeManager.getCommandLineEvaluation(commandLineHandle, rmiServerForApi);
    }

    
    public CommandLineEvaluation getCommandLineEvaluation(CommandLineHandle commandLineHandle, RmiServerForApi rmiServerForApi) {
        return new LocalCommandLineEvaluation(this, commandLineHandle, rmiServerForApi);
    }
    
    public Configuration getConfiguration() {
        return configuration;
    }

    public void removeAllNodes() {
        logger.info("Remove all nodes.");

        String driverHost = "localhost";
        int serverPort = JPPFLocalDriver.SERVER_PORT;

        JPPFManagementByJobsClient client = null;
        try {
            client = JPPFConfiguratedComponentFactory.getInstance().createManagementByJobsClient(
                    driverHost, serverPort);
            try {
                client.shutdownAllNodes();
            } catch (Exception ex) {
                logger.warn("Not all nodes could be shut down.", ex);
            }
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    public void shutdown() {
        for (JPPFLocalNode node : localNodes) {
            node.cleanupAfterShutdown();
        }
    }

    public void addNode(LocalNodeConfiguration nodeConfiguration) {
        JPPFLocalNode node = new JPPFLocalNode();
        node.prepare(nodeConfiguration.getArtifactsToPreload());
        int numberOfProcessingThreads = nodeConfiguration.getNumberOfProcessingThreads();
        logger.info("Adding node with {} processing threads", numberOfProcessingThreads);
        node.startNewNode(nodeConfiguration);
        localNodes.add(node);
    }
    
}
