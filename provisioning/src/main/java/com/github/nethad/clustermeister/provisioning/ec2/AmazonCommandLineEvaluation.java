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
import com.github.nethad.clustermeister.provisioning.AbstractExecutableCommand;
import com.github.nethad.clustermeister.provisioning.Command;
import com.github.nethad.clustermeister.provisioning.CommandLineArguments;
import com.github.nethad.clustermeister.provisioning.CommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.ec2.commands.AddNodesCommand;
import com.github.nethad.clustermeister.provisioning.ec2.commands.GetCommand;
import com.github.nethad.clustermeister.provisioning.ec2.commands.InstanceCommand;
import com.github.nethad.clustermeister.provisioning.ec2.commands.RemoveNodeCommand;
import com.github.nethad.clustermeister.provisioning.ec2.commands.ShutdownCommand;
import com.github.nethad.clustermeister.provisioning.ec2.commands.StartNodeCommand;
import com.github.nethad.clustermeister.provisioning.ec2.commands.StateCommand;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFConfiguratedComponentFactory;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFManagementByJobsClient;

/**
 * @{inheritDoc}
 *
 * @author thomas, daniel
 */
public class AmazonCommandLineEvaluation implements CommandLineEvaluation {
    private final AmazonNodeManager nodeManager;
    private final CommandLineHandle handle;
    private final JPPFManagementByJobsClient managementClient;
    private ShutdownCommand shutdownCommand;
    private StateCommand stateCommand;
    
    /**
     * Creates a new command line evaluation for interpreting CLI commands for 
     * the Amazon provisioning provider.
     * 
     * @param nodeManager 
     *      The amazon provisioning provider for access to the provisioning context.
     * @param handle
     *      The command line handle for access to the command line context.
     */
    public AmazonCommandLineEvaluation(AmazonNodeManager nodeManager, CommandLineHandle handle) {
        this.nodeManager = nodeManager;
        this.handle = handle;
        //TODO: refactor initialization of this management client. Shouldn't be here!
        this.managementClient = JPPFConfiguratedComponentFactory.getInstance().
                createManagementByJobsClient("localhost", JPPFConstants.DEFAULT_SERVER_PORT);
        nodeManager.registerManagementClient(managementClient);
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

    /**
     * The management client issuing management tasks (such as shutdown or 
     * restart) to running JPPF nodes. 
     * 
     * @return the management client of the Amazon provisioning context.
     */
    public JPPFManagementByJobsClient getManagementClient() {
        return managementClient;
    }
    
    /**
     * The node manager provides access to the provisioning context.
     * 
     * @return the Amazon node manager.
     */
    public AmazonNodeManager getNodeManager() {
        return nodeManager;
    }

    @Override
    public CommandLineHandle getCommandLineHandle() {
        return handle;
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
        handle.getCommandRegistry().registerCommand(new GetCommand(this));
        handle.getCommandRegistry().registerCommand(new StartNodeCommand(this));
        handle.getCommandRegistry().registerCommand(new InstanceCommand(this));
        handle.getCommandRegistry().registerCommand(new RemoveNodeCommand(this));
    }
}
