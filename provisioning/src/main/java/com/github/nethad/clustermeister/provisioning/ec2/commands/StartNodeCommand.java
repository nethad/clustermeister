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
import com.github.nethad.clustermeister.api.NodeCapabilities;
import com.github.nethad.clustermeister.api.NodeType;
import com.github.nethad.clustermeister.api.impl.KeyPairCredentials;
import com.github.nethad.clustermeister.provisioning.CommandLineArguments;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.ec2.AWSInstanceProfile;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonCommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonInstanceManager;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNodeConfiguration;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNodeManager;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;
import org.jclouds.compute.domain.Processor;

/**
 *
 * @author daniel
 */
public class StartNodeCommand extends AbstractAmazonExecutableCommand {

    private static final String[] ARGUMENTS = new String[]{"instance ID", "keypair name"};
    
    private static final String HELP_TEXT = "Start a JPPF-Node on an AWS E2 instance.";
    
    private static final String NAME = "startnode";

    public StartNodeCommand(AmazonCommandLineEvaluation commandLineEvaluation) {
        super(NAME, ARGUMENTS, HELP_TEXT, commandLineEvaluation);
    }

    @Override
    public void execute(CommandLineArguments arguments) {
        CommandLineHandle handle = getCommandLineHandle();
        AmazonNodeManager nodeManager = getNodeManager();
        AmazonInstanceManager instanceManager = nodeManager.getInstanceManager();
        
        
        if (this.isArgumentsCountFalse(arguments)) {
            return;
        }
        
        Scanner scanner = arguments.asScanner();
        
        String instanceId = scanner.next();
        String keypairName = scanner.next();
        Credentials configuredCredentials = 
                instanceManager.getConfiguredCredentials(keypairName);
        if(configuredCredentials == null || 
                !(configuredCredentials instanceof KeyPairCredentials)) {
            handle.print(String.format(
                    "No configured keypair credentials found for keypair %s.", 
                    keypairName));
            return;
        }
        
        final NodeMetadata instanceMetadata = 
                instanceManager.getInstanceMetadata(instanceId);
        
        NodeState state = instanceMetadata.getState();
        if(state == NodeState.RUNNING || state == NodeState.SUSPENDED) {
            final AmazonNodeConfiguration amazonNodeConfiguration = 
                    AmazonNodeConfiguration.fromInstanceProfile(
                    AWSInstanceProfile.fromInstanceMetadata(instanceMetadata));
            amazonNodeConfiguration.setDriverAddress("localhost");

            int numberOfCores = 0;
            for(Processor processor : instanceMetadata.getHardware().getProcessors()) {
                numberOfCores += (int) processor.getCores();
            }

            final int numberOfProcessingThreads = numberOfCores;

            amazonNodeConfiguration.setNodeCapabilities(new NodeCapabilities() {
                @Override
                public int getNumberOfProcessors() {
                    return instanceMetadata.getHardware().getProcessors().size();
                }

                @Override
                public int getNumberOfProcessingThreads() {
                    return numberOfProcessingThreads;
                }

                @Override
                public String getJppfConfig() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            });
            amazonNodeConfiguration.setNodeType(NodeType.NODE);

            amazonNodeConfiguration.setCredentials(configuredCredentials);

            logger.info("Starting node on {}", instanceId);
            ListenableFuture<? extends Node> future =
                    nodeManager.addNode(amazonNodeConfiguration,
                    Optional.of(instanceId));
            try {
                Node node = future.get();
                if(node != null) {
                    logger.info("Node started on {}", instanceId);
                } else {
                    logger.info("Failed to start node on {}.", instanceId);
                }
            } catch (InterruptedException ex) {
                logger.warn("Interrupted.", ex);
            } catch (ExecutionException ex) {
                logger.warn("Execution exception.", ex);
            }
        } else {
            handle.print(String.format(
                    "Can not start node on instance in state %s.", state));
        }
        
    }
}
