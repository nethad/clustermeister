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

import com.github.nethad.clustermeister.provisioning.CommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.ec2.commands.AbstractExecutableCommand;
import com.github.nethad.clustermeister.provisioning.rmi.RmiServerForApi;
import com.github.nethad.clustermeister.provisioning.torque.TorqueNodeManager;
import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.LinkedList;
import java.util.StringTokenizer;
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
//    Map<String, String> commandHelp = new HashMap<String, String>();
    private final RmiServerForApi rmiServerForApi;
    
    private Collection<AbstractExecutableCommand> commands = new LinkedList<AbstractExecutableCommand>();

    public TorqueCommandLineEvaluation(TorqueNodeManager nodeManager, CommandLineHandle handle, RmiServerForApi rmiServerForApi) {
        this.nodeManager = nodeManager;
        this.handle = handle;
        this.rmiServerForApi = rmiServerForApi;
        buildCommandHelp();
    }
    
    private void buildCommandHelp() {
        AbstractExecutableCommand addNodesCommand = new AddNodesCommand(
                                                      new String[]{"number of nodes", "processing threads per node"}, 
                                                      "Add nodes (= torque job) to the cluster.", 
                                                      this);
        addAndRegisterCommand(addNodesCommand);
        AbstractExecutableCommand removeNodeCommand = new RemoveNodeCommand(
                                                          new String[]{"node ID"}, 
                                                          "Remove node from the cluster.", 
                                                          this);
        addAndRegisterCommand(removeNodeCommand);
    }
    
    private void addAndRegisterCommand(AbstractExecutableCommand command) {
        handle.getCommandRegistry().registerCommand(command);
        commands.add(command);
    }
    
    @Override
    public void state(StringTokenizer tokenizer) {
        new StateCommand(null, null, this).execute(tokenizer);
//        Collection<NodeInformation> allNodes = rmiServerForApi.getAllNodes();
//        handle.print("running nodes: %d", allNodes.size());
//        
//        for (NodeInformation nodeInformation : allNodes) {
//            String id = nodeInformation.getID();
//            String processingThreads = nodeInformation.getJPPFSystemInformation().getJppf().getProperty(JPPFProperties.PROCESSING_THREADS);
//            handle.print("node %s: %s processing threads.", id, processingThreads);
//        }
    }

    @Override
    public void shutdown(StringTokenizer tokenizer) {
        nodeManager.removeAllNodes();
        nodeManager.shutdown();
    }

    @Override
    public void handleCommand(String command, StringTokenizer tokenizer) {
        for (AbstractExecutableCommand executableCommand : commands) {
            if (executableCommand.getCommandName().equals(command)) {
                executableCommand.execute(tokenizer);
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
