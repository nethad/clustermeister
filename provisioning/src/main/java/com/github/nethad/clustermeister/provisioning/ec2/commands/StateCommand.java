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
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNodeManager;
import java.util.Collection;

/**
 * Show all registered JPPF Nodes.
 *
 * @author daniel
 */
public class StateCommand extends AbstractAmazonExecutableCommand {
    
    private static final String[] ARGUMENTS = null;
    
    private static final String HELP_TEXT = "Show all registered JPPF Nodes.";
    
    private static final String NAME = "state";

    /**
     * Creates a new command with a command line evaluation reference for access 
     * to the Clustermeister provisioning infrastructure.
     * 
     * @param commandLineEvaluation the command line evaluation instance reference.
     */
    public StateCommand(AmazonCommandLineEvaluation commandLineEvaluation) {
        super(NAME, ARGUMENTS, HELP_TEXT, commandLineEvaluation);
    }

    @Override
    public void execute(CommandLineArguments arguments) {
        AmazonNodeManager nodeManager = getNodeManager();
        CommandLineHandle commandLineHandle = getCommandLineHandle();
        Collection<? extends Node> nodes = nodeManager.getNodes();
        if(nodes == null || nodes.isEmpty()) {
            commandLineHandle.print("No nodes registered.");
        }
        
        for(Node node : nodes) {
            commandLineHandle.print(node.toString());
        }
    }
    
}
