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
package com.github.nethad.clustermeister.provisioning.torque.commands;

import com.github.nethad.clustermeister.provisioning.AbstractExecutableCommand;
import com.github.nethad.clustermeister.provisioning.CommandLineArguments;
import com.github.nethad.clustermeister.provisioning.CommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.rmi.RmiServerForApi;
import com.github.nethad.clustermeister.provisioning.torque.TorqueNodeManager;
import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class TorqueCommandLineEvaluation implements CommandLineEvaluation {
    
    private static final Logger logger = LoggerFactory.getLogger(TorqueCommandLineEvaluation.class);
    
    private final TorqueNodeManager nodeManager;
    private final CommandLineHandle handle;
    @VisibleForTesting
    private final RmiServerForApi rmiServerForApi;
    
    private Collection<AbstractExecutableCommand> commands = new LinkedList<AbstractExecutableCommand>();

    public TorqueCommandLineEvaluation(TorqueNodeManager nodeManager, CommandLineHandle handle, RmiServerForApi rmiServerForApi) {
        this.nodeManager = nodeManager;
        this.handle = handle;
        this.rmiServerForApi = rmiServerForApi;
        buildCommandHelp();
    }
    
    private void buildCommandHelp() {
        addAndRegisterCommand(new AddNodesCommand(this));
        addAndRegisterCommand(new RemoveNodeCommand(this));
    }
    
    private void addAndRegisterCommand(AbstractExecutableCommand command) {
        handle.getCommandRegistry().registerCommand(command);
        commands.add(command);
    }
    
    @Override
    public void state(CommandLineArguments arguments) {
        new StateCommand(this).execute(arguments);
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
        return handle;
    }
    
    public TorqueNodeManager getNodeManager() {
        return nodeManager;
    }
    
    public RmiServerForApi getRmiServerForApi() {
        return rmiServerForApi;
    }
    
}
