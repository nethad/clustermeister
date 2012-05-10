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
import com.github.nethad.clustermeister.provisioning.*;
import com.github.nethad.clustermeister.provisioning.ec2.commands.AddNodesCommand;
import com.github.nethad.clustermeister.provisioning.ec2.commands.GetInstancesCommand;
import com.github.nethad.clustermeister.provisioning.ec2.commands.GetKeypairsCommand;
import com.github.nethad.clustermeister.provisioning.ec2.commands.GetProfilesCommand;
import com.github.nethad.clustermeister.provisioning.ec2.commands.ShutdownCommand;
import com.github.nethad.clustermeister.provisioning.ec2.commands.StartNodeCommand;
import com.github.nethad.clustermeister.provisioning.ec2.commands.StateCommand;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFConfiguratedComponentFactory;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFManagementByJobsClient;
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
    private ShutdownCommand shutdownCommand;
    private StateCommand stateCommand;
    
    public AmazonCommandLineEvaluation(AmazonNodeManager nodeManager, CommandLineHandle handle) {
        this.nodeManager = nodeManager;
        this.handle = handle;
        this.amazonManagementClient = JPPFConfiguratedComponentFactory.getInstance().
                createManagementByJobsClient("localhost", JPPFConstants.DEFAULT_SERVER_PORT);
        nodeManager.registerManagementClient(amazonManagementClient);
        registerCommands();
    }

    @Override
    public void state(CommandLineArguments arguments) {
        stateCommand.execute(arguments);
    }

    @Override
    public void shutdown(CommandLineArguments arguments) {
        shutdownCommand.execute(arguments);
    }

    @Override
    public void handleCommand(String commandName, CommandLineArguments arguments) {
        AbstractExecutableCommand command = getCommand(commandName);
        if (command != null) {
            command.execute(arguments);
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
        stateCommand = new StateCommand(this);
        shutdownCommand = new ShutdownCommand(this);
        handle.getCommandRegistry().registerCommand(new AddNodesCommand(this));
        handle.getCommandRegistry().registerCommand(new GetInstancesCommand(this));
        handle.getCommandRegistry().registerCommand(new StartNodeCommand(this));
        handle.getCommandRegistry().registerCommand(new GetKeypairsCommand(this));
        handle.getCommandRegistry().registerCommand(new GetProfilesCommand(this));
    }
    
    public AmazonNodeManager getNodeManager() {
        return nodeManager;
    }

    @Override
    public CommandLineHandle getCommandLineHandle() {
        return handle;
    }

}
