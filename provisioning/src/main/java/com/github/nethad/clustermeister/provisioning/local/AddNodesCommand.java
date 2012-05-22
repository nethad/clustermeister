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

import com.github.nethad.clustermeister.api.Loggers;
import com.github.nethad.clustermeister.provisioning.CommandLineArguments;
import com.github.nethad.clustermeister.provisioning.ConfigurationKeys;
import com.github.nethad.clustermeister.provisioning.dependencymanager.DependencyConfigurationUtil;
import com.google.common.collect.Iterators;
import java.io.File;
import java.util.Collection;
import java.util.Scanner;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class AddNodesCommand extends AbstractLocalExecutableCommand {
//    public static final String JVM_OPTIONS_NODE = "jvm_options.node";
    private static final Logger logger = LoggerFactory.getLogger(Loggers.PROVISIONING);
    
    private static final String COMMAND_NAME = "addnodes";
    private static final String[] ARGUMENTS = new String[]{"number of nodes", "processing threads per node"};
    private static final String HELP_TEXT = "adds nodes on the local machine";
    
    JPPFLocalNode node;

    public AddNodesCommand(LocalCommandLineEvaluation commandLineEvaluation) {
        super(COMMAND_NAME, ARGUMENTS, HELP_TEXT, commandLineEvaluation);
    }

    @Override
    public void execute(CommandLineArguments arguments) {
        logger.info("AddNodesCommand local execute.");
        if (isArgumentsCountFalse(arguments)) {
            return;
        }
        
        Scanner scanner = arguments.asScanner();
        
        int numberOfNodes = scanner.nextInt();
        int numberOfCpusPerNode = scanner.nextInt();
        final Configuration configuration = getNodeManager().getConfiguration();
        
        Collection<File> artifactsToPreload = DependencyConfigurationUtil.getConfiguredDependencies(configuration);
        
        String jvmOptions = configuration.getString(ConfigurationKeys.JVM_OPTIONS_NODE, ConfigurationKeys.DEFAULT_JVM_OPTIONS_NODE);
        
        final LocalNodeConfiguration nodeConfiguration = LocalNodeConfiguration.configurationFor(
                artifactsToPreload, jvmOptions, numberOfCpusPerNode);
        
        for (int i=0; i<numberOfNodes; i++) {
            getNodeManager().addNode(nodeConfiguration);
        }
    }
    
}
