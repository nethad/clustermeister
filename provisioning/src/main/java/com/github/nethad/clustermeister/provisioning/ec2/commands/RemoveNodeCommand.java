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
import com.github.nethad.clustermeister.provisioning.ec2.AmazonInstanceShutdownState;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNode;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNodeManager;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author daniel
 */
public class RemoveNodeCommand extends AbstractAmazonExecutableCommand {

    private static final String[] ARGUMENTS = 
            new String[]{"shutdown state (running|suspended|terminated)", "node IDs..."};

    private static final String HELP_TEXT = "Removes JPPF-Nodes.";
    
    private static final String NAME = "removenode";

    public RemoveNodeCommand(AmazonCommandLineEvaluation commandLineEvaluation) {
        super(NAME, ARGUMENTS, HELP_TEXT, commandLineEvaluation);
    }
    
    @Override
    public void execute(CommandLineArguments arguments) {
        CommandLineHandle console = getCommandLineHandle();
        if (arguments.argumentCount() < 2) {
            console.expectedArguments(getArguments());
            return;
        }
        
        AmazonNodeManager nodeManager = getNodeManager();
        Scanner scanner = arguments.asScanner();
        
        AmazonInstanceShutdownState shutdownState;  
        String shutdownStateArgument = scanner.next();
        try {
            shutdownState = AmazonInstanceShutdownState.valueOf(shutdownStateArgument.toUpperCase());
        } catch (IllegalArgumentException ex) {
            console.print("Unknown shutdown state '%s'.", shutdownStateArgument);
            return;
        }
        
        List<AmazonNode> nodesToShutdown = Lists.newLinkedList();
        while(scanner.hasNext()) {
            final String nodeId = scanner.next();
            Node node = Iterables.find(getNodeManager().getNodes(), new Predicate<Node>() {
                @Override
                public boolean apply(Node input) {
                    return input.getID().equalsIgnoreCase(nodeId);
                }
            }, null);
            if(node != null) {
                nodesToShutdown.add(node.as(AmazonNode.class));
            } else {
                console.print("Unknown node ID: %s. Skipping.", nodeId);
            }
        }
        
        console.print("Shutting down %d nodes", nodesToShutdown.size());
        List<ListenableFuture<? extends Object>> futures = 
                new ArrayList<ListenableFuture<? extends Object>>(nodesToShutdown.size());
        for (AmazonNode amazonNode : nodesToShutdown) {
            ListenableFuture<Boolean> removeNode = nodeManager.removeNode(amazonNode, shutdownState);
            futures.add(removeNode);
        }
        
        waitForFuturesToComplete(futures, 
                "Interrupted while waiting for nodes to shut down. Nodes may not all be stopped properly.", 
                "Failed to wait for nodes to stop.", "{} nodes failed to shut down.");
        console.print("Shutdown completed.");
        
    }
    
}
