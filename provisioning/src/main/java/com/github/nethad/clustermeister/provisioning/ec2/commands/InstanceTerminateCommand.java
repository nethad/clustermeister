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

import com.github.nethad.clustermeister.provisioning.CommandLineArguments;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonCommandLineEvaluation;
import java.util.Scanner;

/**
 * Shutdown and delete an AWS EC2 instance.
 *
 * @author daniel
 */
public class InstanceTerminateCommand extends AbstractAmazonExecutableCommand {

    private static final String[] ARGUMENTS = new String[]{"instance id"};

    private static final String HELP_TEXT = "Shutdown and delete an AWS EC2 instance.";
    
    private static final String NAME = "terminate";

    /**
     * Creates a new command with a command line evaluation reference for access 
     * to the Clustermeister provisioning infrastructure.
     * 
     * @param commandLineEvaluation the command line evaluation instance reference.
     */
    public InstanceTerminateCommand(AmazonCommandLineEvaluation commandLineEvaluation) {
        super(NAME, ARGUMENTS, HELP_TEXT, commandLineEvaluation);
    }
    
    @Override
    public void execute(CommandLineArguments arguments) {
        if (isArgumentsCountFalse(arguments)) {
            return;
        }
        
        Scanner scanner = arguments.asScanner();
        final String instanceId = scanner.next();
        
        getNodeManager().getEc2Facade().terminateInstance(instanceId);
    }
    
}
