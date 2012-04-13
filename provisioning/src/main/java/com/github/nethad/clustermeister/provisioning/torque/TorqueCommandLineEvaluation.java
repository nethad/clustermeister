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
package com.github.nethad.clustermeister.provisioning.torque;

import com.github.nethad.clustermeister.api.Node;
import com.github.nethad.clustermeister.provisioning.CommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class TorqueCommandLineEvaluation implements CommandLineEvaluation {
    private static final String COMMAND_ADDNODES = "addnodes";
    
    private final Logger logger = LoggerFactory.getLogger(TorqueCommandLineEvaluation.class);
    
    private final TorqueNodeManager nodeManager;
    private final CommandLineHandle handle;
    private Map<String, String> commandHelp = new HashMap<String, String>();

    public TorqueCommandLineEvaluation(TorqueNodeManager nodeManager, CommandLineHandle handle) {
        this.nodeManager = nodeManager;
        this.handle = handle;
        buildCommandHelp();
    }
    
    private void buildCommandHelp() {
        commandHelp.put(COMMAND_ADDNODES, "[number of nodes]  [processing threads per node]");
    }
    
    public String[] commands() {
        return new String[]{COMMAND_ADDNODES};
    }
    
    @Override
    public void state(StringTokenizer tokenizer) {
        handle.print("running nodes: %d", nodeManager.getNodes().size());
    }

    @Override
    public void addNodes(StringTokenizer tokenizer, String driverHost) {
        if (tokenizer.countTokens() != 2) {
            handle.expectedArguments(new String[]{"number of nodes", "processing threads per node"});
            return;
        }
        int numberOfNodes = Integer.parseInt(tokenizer.nextToken());
        int numberOfCpusPerNode = Integer.parseInt(tokenizer.nextToken());
        
        final TorqueNodeConfiguration torqueNodeConfiguration =
                TorqueNodeConfiguration.configurationForNode(driverHost, numberOfCpusPerNode);
                
        ListenableFuture<? extends Node> lastNode = null;
        for (int i = 0; i < numberOfNodes; i++) {
            lastNode = nodeManager.addNode(torqueNodeConfiguration);
        }
        try {
            lastNode.get();
        } catch (InterruptedException ex) {
            logger.warn("Waited for last node to start up", ex);
        } catch (ExecutionException ex) {
            logger.warn("Waited for last node to start up", ex);
//        } catch (TimeoutException ex) {
//            logger.warn("Waited for last node to start up", ex);
        }
    }

    @Override
    public void shutdown(StringTokenizer tokenizer) {
        nodeManager.removeAllNodes();
        nodeManager.shutdown();
    }

    @Override
    public void help(StringTokenizer tokenizer) {
        for (Map.Entry<String, String> entry : commandHelp.entrySet()) {
            handle.print("%s %s", entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void handleCommand(String command, StringTokenizer tokenizer) {
        handle.print("command %s unknown", command);
    }

    @Override
    public String helpText(String command) {
        if (commandHelp.containsKey(command)) {
            return commandHelp.get(command);
        } else {
            return "!command unknown!";
        }
    }
    
}
