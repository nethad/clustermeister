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
package com.github.nethad.clustermeister.provisioning.ec2;

import com.github.nethad.clustermeister.api.Node;
import com.github.nethad.clustermeister.api.NodeCapabilities;
import com.github.nethad.clustermeister.api.NodeType;
import com.github.nethad.clustermeister.provisioning.CommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.*;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class AmazonCommandLineEvaluation implements CommandLineEvaluation {
    private final Logger logger = LoggerFactory.getLogger(AmazonCommandLineEvaluation.class);
    
    private final AmazonNodeManager nodeManager;
    private final CommandLineHandle handle;
    private Map<String, String> commandHelp = new HashMap<String, String>();

    public AmazonCommandLineEvaluation(AmazonNodeManager nodeManager, CommandLineHandle handle) {
        this.nodeManager = nodeManager;
        this.handle = handle;
        buildCommandHelp();
    }
    
    private void buildCommandHelp() {
        commandHelp.put(CommandLineEvaluation.COMMAND_ADDNODES, "[number of nodes]  [processing threads per node]");
    }
    
    public String[] commands() {
        return commandHelp.keySet().toArray(new String[]{});
    }
    
    @Override
    public void addNodes(StringTokenizer tokenizer, String driverHost) {
        if (tokenizer.countTokens() != 2) {
            handle.expectedArguments(new String[]{"number of nodes", "processing threads per node"});
            return;
        }
        final int numberOfNodes = Integer.parseInt(tokenizer.nextToken());
        final int numberOfCpusPerNode = Integer.parseInt(tokenizer.nextToken());
                final AmazonNodeConfiguration amazonNodeConfiguration = new AmazonNodeConfiguration();
        amazonNodeConfiguration.setDriverAddress("localhost");
        amazonNodeConfiguration.setNodeCapabilities(new NodeCapabilities() {
            @Override
            public int getNumberOfProcessors() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public int getNumberOfProcessingThreads() {
                return numberOfCpusPerNode;
            }

            @Override
            public String getJppfConfig() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
        amazonNodeConfiguration.setNodeType(NodeType.NODE);
        amazonNodeConfiguration.setRegion("eu-west-1c");
        
        logger.info("Starting {} nodes.", numberOfNodes);
        List<ListenableFuture<? extends Node>> futures = 
                new ArrayList<ListenableFuture<? extends Node>>(numberOfNodes);
        for (int i = 0; i < numberOfNodes; i++) {
            futures.add(nodeManager.addNode(amazonNodeConfiguration, 
                    Optional.<String>absent()));
        }
        try {
            List<? extends Node> startedNodes = Futures.successfulAsList(futures).get();
            int failedNodes = Iterables.frequency(startedNodes, null);
            if(failedNodes > 0) {
                logger.warn("{} nodes failed to start.", failedNodes);
            }
        } catch (InterruptedException ex) {
            logger.warn("Interrupted while waiting for nodes to start. Nodes may not be started properly.", ex);
        } catch (ExecutionException ex) {
            logger.warn("Failed to wait for nodes to start.", ex);
        }
    }

    @Override
    public void state(StringTokenizer tokenizer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void shutdown(StringTokenizer tokenizer) {
        nodeManager.removeAllNodes(AmazonInstanceShutdownMethod.TERMINATE);
        nodeManager.close();
    }

    @Override
    public void help(StringTokenizer tokenizer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void handleCommand(String command, StringTokenizer tokenizer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String helpText(String command) {
        if (commandHelp.containsKey(command)) {
            return commandHelp.get(command);
        } else {
            return "!command unknown!";
        }
    }
    
}
