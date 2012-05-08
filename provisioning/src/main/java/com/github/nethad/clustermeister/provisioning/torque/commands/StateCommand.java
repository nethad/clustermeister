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

import com.github.nethad.clustermeister.api.NodeInformation;
import com.github.nethad.clustermeister.api.utils.JPPFProperties;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.rmi.RmiServerForApi;
import java.util.Collection;
import java.util.StringTokenizer;

/**
 *
 * @author thomas
 */
public class StateCommand extends AbstractTorqueExecutableCommand {
    
    private static final String COMMAND = "state";
    private static final String[] ARGUMENTS = new String[]{};
    private static final String HELP_TEXT = "state";
    
//    private RmiServerForApi rmiServerForApi;
//    private CommandLineHandle handle;
    
    public StateCommand(TorqueCommandLineEvaluation commandLineEvaluation) {
        super(COMMAND, ARGUMENTS, HELP_TEXT, commandLineEvaluation);
    }
    
    @Override
    public void execute(StringTokenizer tokenizer) {
        Collection<NodeInformation> allNodes = getRmiServerForApi().getAllNodes();
        getCommandLineHandle().print("running nodes: %d", allNodes.size());
        
        for (NodeInformation nodeInformation : allNodes) {
            String id = nodeInformation.getID();
            String processingThreads = nodeInformation.getJPPFSystemInformation().getJppf().getProperty(JPPFProperties.PROCESSING_THREADS);
            getCommandLineHandle().print("node %s: %s processing threads.", id, processingThreads);
        }
    }
    
}
