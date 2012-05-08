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

import com.github.nethad.clustermeister.provisioning.torque.TorqueNode;
import java.util.StringTokenizer;

/**
 *
 * @author thomas
 */
public class RemoveNodeCommand extends AbstractTorqueExecutableCommand {
    
    private static final String COMMAND = "removenode";

    public RemoveNodeCommand(String[] arguments, String helpText, TorqueCommandLineEvaluation commandLineEvaluation) {
        super(COMMAND, arguments, helpText, commandLineEvaluation);
    }

    @Override
    public void execute(StringTokenizer tokenizer) {
        getNodeManager().removeNodes(null);
    }
    
}
