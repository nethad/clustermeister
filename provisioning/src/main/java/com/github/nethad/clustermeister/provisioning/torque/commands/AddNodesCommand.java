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

import com.github.nethad.clustermeister.api.Node;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.dependencymanager.DependencyConfigurationUtil;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonCommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.AbstractExecutableCommand;
import com.github.nethad.clustermeister.provisioning.CommandLineArguments;
import com.github.nethad.clustermeister.provisioning.torque.TorqueNodeConfiguration;
import com.github.nethad.clustermeister.provisioning.torque.TorqueNodeManager;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.util.Collection;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.logging.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class AddNodesCommand extends AbstractTorqueExecutableCommand {
    private static final String COMMAND = "addnodes";
    private static final String[] ARGUMENTS = new String[]{"number of nodes", "processing threads per node"};
    private static final String HELP_TEXT = "Add nodes (= torque job) to the cluster.";
    
    private static final Logger logger = LoggerFactory.getLogger(AddNodesCommand.class);

    public AddNodesCommand(TorqueCommandLineEvaluation commandLineEvaluation) {
        super(COMMAND, ARGUMENTS, HELP_TEXT, commandLineEvaluation);
    }
    
    @Override
    public void execute(CommandLineArguments arguments) {
        if (isArgumentsCountFalse(arguments)) {
            return;
        }
        
        Scanner scanner = arguments.asScanner();
        
        int numberOfNodes = scanner.nextInt();
        int numberOfCpusPerNode = scanner.nextInt();
        
        Collection<File> artifactsToPreload = DependencyConfigurationUtil.getConfiguredDependencies(getNodeManager().getConfiguration());
        
        final TorqueNodeConfiguration torqueNodeConfiguration =
                TorqueNodeConfiguration.configurationForNode(numberOfCpusPerNode, artifactsToPreload);
                
        ListenableFuture<? extends Node> lastNode = null;
        for (int i = 0; i < numberOfNodes; i++) {
            lastNode = getNodeManager().addNode(torqueNodeConfiguration);
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
    
}
