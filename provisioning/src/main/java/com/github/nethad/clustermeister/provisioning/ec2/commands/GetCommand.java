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

import com.github.nethad.clustermeister.provisioning.ec2.AmazonCommandLineEvaluation;

/**
 * Registers all get * commands.
 *
 * @author daniel
 */
public class GetCommand extends AbstractCompositeCommand {
    private static final String[] ARGUMENTS = new String[]{"subcommand"};

    private static final String HELP_TEXT = "Retrieve information.";
    
    private static final String NAME = "get";

    /**
     * Creates a new command with a command line evaluation reference for access 
     * to the Clustermeister provisioning infrastructure.
     * 
     * <p>
     * This constructor registers GetInstancesCommand, GetKeypairsCommand and 
     * GetProfilesCommand as subcommands.
     * </p>
     * 
     * @param commandLineEvaluation the command line evaluation instance reference.
     */
    public GetCommand(AmazonCommandLineEvaluation commandLineEvaluation) {
        super(NAME, ARGUMENTS, HELP_TEXT, commandLineEvaluation);
        
        registerCommand(new GetInstancesCommand(commandLineEvaluation));
        registerCommand(new GetKeypairsCommand(commandLineEvaluation));
        registerCommand(new GetProfilesCommand(commandLineEvaluation));
    }
}
