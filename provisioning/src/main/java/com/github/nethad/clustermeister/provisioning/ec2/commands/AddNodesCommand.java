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
package com.github.nethad.clustermeister.provisioning.ec2.commands;

import com.github.nethad.clustermeister.api.Node;
import com.github.nethad.clustermeister.api.NodeCapabilities;
import com.github.nethad.clustermeister.api.NodeType;
import com.github.nethad.clustermeister.provisioning.CommandLineArguments;
import com.github.nethad.clustermeister.provisioning.ec2.AWSInstanceProfile;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonCommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonInstanceManager;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNodeConfiguration;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNodeManager;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author daniel
 */
public class AddNodesCommand extends AbstractAmazonExecutableCommand {
    
    private static final String[] ARGUMENTS = 
            new String[]{"number of nodes", "processing threads per node", "profile"};

    private static final String HELP_TEXT = "Add nodes to the cluster.";
    
    private static final String NAME = "addnodes";
    
    
    public AddNodesCommand(AmazonCommandLineEvaluation commandLineEvaluation) {
        super(NAME, ARGUMENTS, HELP_TEXT, commandLineEvaluation);
    }

    @Override
    public void execute(CommandLineArguments arguments) {
        AmazonNodeManager nodeManager = getNodeManager();
        AmazonInstanceManager instanceManager = nodeManager.getInstanceManager();
        
        if (isArgumentsCountFalse(arguments)) {
            return;
        }
        
        Scanner scanner = arguments.asScanner();
        
        final int numberOfNodes = scanner.nextInt();
        final int numberOfCpusPerNode = scanner.nextInt();
        final String profileName = scanner.next();
        AWSInstanceProfile profile = instanceManager.getConfiguredProfile(profileName);
        if(profile == null) {
            getCommandLineHandle().print("Unknown profile '%s'.", profileName);
            return;
        }
        
        final AmazonNodeConfiguration amazonNodeConfiguration = 
                AmazonNodeConfiguration.fromInstanceProfile(profile);
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
        
        logger.info("Starting {} nodes.", numberOfNodes);
        List<ListenableFuture<? extends Object>> futures = 
                new ArrayList<ListenableFuture<? extends Object>>(numberOfNodes);
        for (int i = 0; i < numberOfNodes; i++) {
            ListenableFuture<? extends Node> future = 
                    nodeManager.addNode(amazonNodeConfiguration, Optional.<String>absent());
            addFailureLogger(future);
            futures.add(future);
        }
        waitForFuturesToComplete(futures, 
                "Interrupted while waiting for nodes to start. Nodes may not be started properly.", 
                "Failed to wait for nodes to start.", "{} nodes failed to start.");
    }

    private void addFailureLogger(ListenableFuture<? extends Node> future) {
        Futures.addCallback(future, new FutureCallback<Node>() {
            @Override
            public void onSuccess(Node result) {
                //nop
            }

            @Override
            public void onFailure(Throwable t) {
                logger.warn("Node start failure.", t);
            }

        });
    }
    
}
