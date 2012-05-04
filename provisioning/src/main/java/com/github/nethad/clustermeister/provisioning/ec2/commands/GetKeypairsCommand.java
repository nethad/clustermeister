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

import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonCommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonInstanceManager;
import java.util.Set;
import java.util.StringTokenizer;

/**
 *
 * @author daniel
 */
public class GetKeypairsCommand extends AbstractExecutableCommand {

    /**
     * Command name.
     */
    public static final String NAME = "getkeypairs";

    private static final String SEPARATOR_LINE = "-------------------------------------------------";
    
    public GetKeypairsCommand(String[] arguments, String helpText, 
            AmazonCommandLineEvaluation commandLineEvaluation) {
        super(NAME, arguments, helpText, commandLineEvaluation);
    }
    
    @Override
    public void execute(StringTokenizer tokenizer) {
        AmazonInstanceManager amazonInstanceManager = 
                commandLineEvaluation.getNodeManager().getInstanceManager();
        CommandLineHandle handle = commandLineEvaluation.getHandle();
        
        handle.print("Configured keypair names:");
        handle.print(SEPARATOR_LINE);
        Set<String> configuredKeypairNames = 
                amazonInstanceManager.getConfiguredKeypairNames();
        
        if(configuredKeypairNames.isEmpty()) {
            handle.print("No keypairs configured.");
        } else {
            for(String keypairName : configuredKeypairNames) {
                handle.print(keypairName);
            }
        }
        handle.print(SEPARATOR_LINE);
    }
}
