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

import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.AbstractExecutableCommand;
import com.github.nethad.clustermeister.provisioning.rmi.RmiServerForApi;
import com.github.nethad.clustermeister.provisioning.torque.TorqueNodeManager;
import java.util.StringTokenizer;

/**
 *
 * @author thomas
 */
public abstract class AbstractTorqueExecutableCommand extends AbstractExecutableCommand {
    
    private TorqueCommandLineEvaluation commandLineEvaluation;
  
    public AbstractTorqueExecutableCommand(String commandName, String[] arguments, 
            String helpText, TorqueCommandLineEvaluation commandLineEvaluation) {
        super(commandName, arguments, helpText);
        this.commandLineEvaluation = commandLineEvaluation;
    }
    
    protected CommandLineHandle getCommandLineHandle() {
        return commandLineEvaluation.getCommandLineHandle();
    }
    
    protected TorqueNodeManager getNodeManager() {
        return commandLineEvaluation.getNodeManager();
    }
    
    protected RmiServerForApi getRmiServerForApi() {
        return commandLineEvaluation.getRmiServerForApi();
    }
    
    protected boolean isArgumentsCountFalse(StringTokenizer tokenizer) {
        if (tokenizer.countTokens() != this.getArgumentCount()) {
            getCommandLineHandle().expectedArguments(this.getArguments());
            return true;
        } else {
            return false;
        }
    }
        
}
