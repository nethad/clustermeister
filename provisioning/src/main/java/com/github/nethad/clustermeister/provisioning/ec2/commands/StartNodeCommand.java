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
import com.github.nethad.clustermeister.api.impl.KeyPairCredentials;
import com.github.nethad.clustermeister.provisioning.CommandLineArguments;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.ec2.AWSInstanceProfile;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonCommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNodeConfiguration;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNodeManager;
import com.github.nethad.clustermeister.provisioning.ec2.AwsEc2Facade;
import com.github.nethad.clustermeister.provisioning.ec2.CredentialsManager;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;

/**
 * Start a JPPF-Node on an AWS EC2 instance.
 *
 * @author daniel
 */
public class StartNodeCommand extends AbstractAmazonExecutableCommand {

    private static final String[] ARGUMENTS = new String[]{"instance ID", "keypair name"};
    
    private static final String HELP_TEXT = "Start a JPPF-Node on an AWS EC2 instance.";
    
    private static final String NAME = "startnode";

    /**
     * Creates a new command with a command line evaluation reference for access 
     * to the Clustermeister provisioning infrastructure.
     * 
     * @param commandLineEvaluation the command line evaluation instance reference.
     */
    public StartNodeCommand(AmazonCommandLineEvaluation commandLineEvaluation) {
        super(NAME, ARGUMENTS, HELP_TEXT, commandLineEvaluation);
    }

    @Override
    public void execute(CommandLineArguments arguments) {
        CommandLineHandle console = getCommandLineHandle();
        AmazonNodeManager nodeManager = getNodeManager();
        CredentialsManager credentialsManager = nodeManager.getCredentialsManager();
        AwsEc2Facade ec2Facade = nodeManager.getEc2Facade();
        
        
        if (this.isArgumentsCountFalse(arguments)) {
            return;
        }
        
        Scanner scanner = arguments.asScanner();
        
        String instanceId = scanner.next();
        String keypairName = scanner.next();
        Credentials credentials = 
                credentialsManager.getCredentials(keypairName);
        if(credentials == null || !(credentials instanceof KeyPairCredentials)) {
            console.print(String.format("No keypair credentials found for credentials '%s'.", 
                    credentials));
            return;
        }
        
        final NodeMetadata instanceMetadata = 
                ec2Facade.getInstanceMetadata(instanceId);
        
        NodeState state = instanceMetadata.getState();
        if(state == NodeState.RUNNING || state == NodeState.SUSPENDED) {
            final AmazonNodeConfiguration amazonNodeConfiguration = 
                    AmazonNodeConfiguration.fromInstanceProfile(
                    AWSInstanceProfile.fromInstanceMetadata(instanceMetadata));
            
            amazonNodeConfiguration.setDriverAddress("localhost");
            amazonNodeConfiguration.setNodeType(NodeType.NODE);
            amazonNodeConfiguration.setCredentials(credentials);

            console.print("Starting node on %s", instanceId);
            ListenableFuture<? extends Node> future =
                    nodeManager.addNode(amazonNodeConfiguration,
                    Optional.of(instanceId));
            try {
                Node node = future.get();
                if(node != null) {
                    console.print("Node started on %s", instanceId);
                } else {
                    console.print("Failed to start node on %s.", instanceId);
                }
            } catch (InterruptedException ex) {
                logger.warn("Interrupted while waiting for node to start.", ex);
            } catch (ExecutionException ex) {
                logger.warn("Could not wait for node to start.", ex);
            }
        } else {
            console.print(String.format(
                    "Can not start node on %s because the instance is in state '%s'.", 
                    instanceId, state));
        }
        
    }
}
