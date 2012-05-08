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

import com.github.nethad.clustermeister.api.JPPFConstants;
import com.github.nethad.clustermeister.provisioning.Command;
import com.github.nethad.clustermeister.provisioning.CommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.AbstractExecutableCommand;
import com.github.nethad.clustermeister.provisioning.ec2.commands.AddNodesCommand;
import com.github.nethad.clustermeister.provisioning.ec2.commands.GetInstancesCommand;
import com.github.nethad.clustermeister.provisioning.ec2.commands.GetKeypairsCommand;
import com.github.nethad.clustermeister.provisioning.ec2.commands.ShutdownCommand;
import com.github.nethad.clustermeister.provisioning.ec2.commands.StartNodeCommand;
import com.github.nethad.clustermeister.provisioning.ec2.commands.StateCommand;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFConfiguratedComponentFactory;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFManagementByJobsClient;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas, daniel
 */
public class AmazonCommandLineEvaluation implements CommandLineEvaluation {
    private final Logger logger = LoggerFactory.getLogger(AmazonCommandLineEvaluation.class);
    
    private final AmazonNodeManager nodeManager;
    private final CommandLineHandle handle;
    private final JPPFManagementByJobsClient amazonManagementClient;
    
    public AmazonCommandLineEvaluation(AmazonNodeManager nodeManager, CommandLineHandle handle) {
        this.nodeManager = nodeManager;
        this.handle = handle;
        this.amazonManagementClient = JPPFConfiguratedComponentFactory.getInstance().
                createManagementByJobsClient("localhost", JPPFConstants.DEFAULT_SERVER_PORT);
        nodeManager.registerManagementClient(amazonManagementClient);
        registerCommands();
    }

    @Override
    public void state(StringTokenizer tokenizer) {
        new StateCommand(new String[]{}, "", this).execute(tokenizer);
    }

    @Override
    public void shutdown(StringTokenizer tokenizer) {
        new ShutdownCommand(new String[]{}, "", this).execute(tokenizer);
    }

    @Override
    public void handleCommand(String commandName, StringTokenizer tokenizer) {
        AbstractExecutableCommand command = getCommand(commandName);
        if (command != null) {
            command.execute(tokenizer);
        }
    }

    public JPPFManagementByJobsClient getAmazonManagementClient() {
        return amazonManagementClient;
    }
    
    private AbstractExecutableCommand getCommand(String commandName) {
        Command command = handle.getCommandRegistry().getCommand(commandName);
        if(command instanceof AbstractExecutableCommand) {
            return (AbstractExecutableCommand) command;
        } else {
            return null;
        }
    }

    private void registerCommands() {
        handle.getCommandRegistry().registerCommand(new AddNodesCommand(
                AddNodesCommand.ARG_DESCRIPTIONS, "Add nodes to the cluster.", this));
        handle.getCommandRegistry().registerCommand(new GetInstancesCommand(
                GetInstancesCommand.ARG_DESCRIPTIONS, 
                "Get configured instances and their state from the configure AWS Account.", 
                this));
        handle.getCommandRegistry().registerCommand(new StartNodeCommand(
                StartNodeCommand.ARG_DESCRIPTIONS, 
                "Start a JPPF-Node on an AWS E2 instance.", 
                this));
        handle.getCommandRegistry().registerCommand(new GetKeypairsCommand(
                null, 
                "Get all configured keypair names.", 
                this));
    }
    
    public AmazonNodeManager getNodeManager() {
        return nodeManager;
    }

    @Override
    public CommandLineHandle getCommandLineHandle() {
        return handle;
    }
}
