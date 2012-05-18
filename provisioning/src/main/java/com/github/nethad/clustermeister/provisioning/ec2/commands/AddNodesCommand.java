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

import com.github.nethad.clustermeister.api.Credentials;
import com.github.nethad.clustermeister.api.Node;
import com.github.nethad.clustermeister.api.NodeType;
import com.github.nethad.clustermeister.provisioning.CommandLineArguments;
import com.github.nethad.clustermeister.provisioning.ec2.AWSInstanceProfile;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonCommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonInstanceManager;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNodeConfiguration;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNodeManager;
import com.github.nethad.clustermeister.provisioning.ec2.CredentialsManager;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Creates new EC2 instances and deploys JPPF-Nodes on them.
 * 
 * @author daniel
 */
public class AddNodesCommand extends AbstractAmazonExecutableCommand {
    
    private static final String[] ARGUMENTS = 
            new String[]{"number of nodes", "profile"};

    private static final String HELP_TEXT = "Create new EC2 instances and deploys JPPF-Nodes on them.";
    
    private static final String NAME = "addnodes";
    
    /**
     * Creates a new command with a command line evaluation reference for access 
     * to the Clustermeister provisioning infrastructure.
     * 
     * @param commandLineEvaluation the command line evaluation instance reference.
     */
    public AddNodesCommand(AmazonCommandLineEvaluation commandLineEvaluation) {
        super(NAME, ARGUMENTS, HELP_TEXT, commandLineEvaluation);
    }

    @Override
    public void execute(CommandLineArguments arguments) {
        if (isArgumentsCountFalse(arguments)) {
            return;
        }
        
        AmazonNodeManager nodeManager = getNodeManager();
        AmazonInstanceManager instanceManager = nodeManager.getInstanceManager();
        CredentialsManager credentialsManager = nodeManager.getCredentialsManager();
        
        Scanner scanner = arguments.asScanner();
        
        final int numberOfNodes = scanner.nextInt();
        final String profileName = scanner.next();
        AWSInstanceProfile profile = instanceManager.getConfiguredProfile(profileName);
        if(profile == null) {
            getCommandLineHandle().print("Unknown profile '%s'.", profileName);
            return;
        }
        
        final AmazonNodeConfiguration nodeConfiguration = 
                AmazonNodeConfiguration.fromInstanceProfile(profile);
        nodeConfiguration.setDriverAddress("localhost");
        nodeConfiguration.setNodeType(NodeType.NODE);
        
        if(profile.getKeyPairName().isPresent()) {
            String keyPairName = profile.getKeyPairName().get();
            Credentials credentials;
            if((credentials = credentialsManager.getCredentials(keyPairName)) != null) {
                nodeConfiguration.setCredentials(credentials);
            } else {
                logger.error(
                        "Keypair {} configured in profile but not found in keypairs configuration.", 
                        keyPairName);
                return;
            }
        }
        
        getCommandLineHandle().print("Starting %d nodes.", numberOfNodes);
        List<ListenableFuture<? extends Object>> futures = 
                new ArrayList<ListenableFuture<? extends Object>>(numberOfNodes);
        for (int i = 0; i < numberOfNodes; i++) {
            ListenableFuture<? extends Node> future = 
                    nodeManager.addNode(nodeConfiguration, Optional.<String>absent());
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
