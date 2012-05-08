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
import com.github.nethad.clustermeister.provisioning.CommandLineArguments;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonCommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonInstanceShutdownMethod;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNode;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNodeManager;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFManagementByJobsClient;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.*;

/**
 *
 * @author daniel
 */
public class ShutdownCommand extends AbstractAmazonExecutableCommand {

    /**
     * Command name.
     */
    public static final String NAME = "shutdown";

    public ShutdownCommand(String[] arguments, String helpText, 
            AmazonCommandLineEvaluation commandLineEvaluation) {
        super(NAME, arguments, helpText, commandLineEvaluation);
    }

    @Override
    public void execute(CommandLineArguments arguments) {
        AmazonNodeManager nodeManager = commandLineEvaluation.getNodeManager();
        JPPFManagementByJobsClient amazonManagementClient = 
                commandLineEvaluation.getAmazonManagementClient();
        
        logger.info("Shutting down all nodes.");
        Collection<? extends Node> nodes = nodeManager.getNodes();
        List<ListenableFuture<? extends Object>> futures = 
                new ArrayList<ListenableFuture<? extends Object>>(nodes.size());
        for(Node node : nodes) {
            futures.add(nodeManager.removeNode((AmazonNode) node, 
                    AmazonInstanceShutdownMethod.TERMINATE));
        }
        waitForFuturesToComplete(futures, 
                "Interrupted while waiting for nodes to shut down. Nodes may not all be stopped properly.", 
                "Failed to wait for nodes to stop.", "{} nodes failed to shut down.");
        
        amazonManagementClient.close();
        nodeManager.close();
    }
    
}
