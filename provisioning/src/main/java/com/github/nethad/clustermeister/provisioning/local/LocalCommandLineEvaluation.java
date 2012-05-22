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
package com.github.nethad.clustermeister.provisioning.local;

import com.github.nethad.clustermeister.api.NodeInformation;
import com.github.nethad.clustermeister.provisioning.*;
import com.github.nethad.clustermeister.provisioning.rmi.RmiServerForApi;
import java.util.Collection;
import java.util.LinkedList;

/**
 *
 * @author thomas
 */
public class LocalCommandLineEvaluation implements CommandLineEvaluation {
    private final LocalNodeManager nodeManager;
    private final CommandLineHandle commandLineHandle;
    private final RmiServerForApi rmiServerForApi;
    
    private Collection<AbstractExecutableCommand> commands = new LinkedList<AbstractExecutableCommand>();

    LocalCommandLineEvaluation(LocalNodeManager nodeManager, CommandLineHandle commandLineHandle, RmiServerForApi rmiServerForApi) {
        this.nodeManager = nodeManager;
        this.commandLineHandle = commandLineHandle;
        this.rmiServerForApi = rmiServerForApi;
        registerCommands();
    }

    @Override
    public void state(CommandLineArguments arguments) {
        Collection<NodeInformation> allNodes = rmiServerForApi.getAllNodes();
        System.out.println("number of nodes: "+allNodes.size());
    }

    @Override
    public void shutdown(CommandLineArguments arguments) {
        nodeManager.removeAllNodes();
        nodeManager.shutdown();
    }

    @Override
    public void handleCommand(String command, CommandLineArguments arguments) {
        for (AbstractExecutableCommand executableCommand : commands) {
            if (executableCommand.getCommandName().equals(command)) {
                executableCommand.execute(arguments);
            }
        }
    }

    @Override
    public CommandLineHandle getCommandLineHandle() {
        return commandLineHandle;
    }

    LocalNodeManager getNodeManager() {
        return nodeManager;
    }

    private void registerCommands() {
        addAndRegisterCommand(new AddNodesCommand(this));
    }
    
    private void addAndRegisterCommand(AbstractExecutableCommand command) {
        commandLineHandle.getCommandRegistry().registerCommand(command);
        commands.add(command);
    }
    
}
