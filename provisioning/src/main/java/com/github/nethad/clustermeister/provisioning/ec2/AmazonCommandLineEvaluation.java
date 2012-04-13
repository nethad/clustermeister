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
package com.github.nethad.clustermeister.provisioning.ec2;

import com.github.nethad.clustermeister.provisioning.CommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import java.util.StringTokenizer;

/**
 *
 * @author thomas
 */
public class AmazonCommandLineEvaluation implements CommandLineEvaluation {
    private final AmazonNodeManager nodeManager;
    private final CommandLineHandle handle;

    public AmazonCommandLineEvaluation(AmazonNodeManager nodeManager, CommandLineHandle handle) {
        this.nodeManager = nodeManager;
        this.handle = handle;
    }
    
    @Override
    public void addNodes(StringTokenizer tokenizer, String driverHost) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void state(StringTokenizer tokenizer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void shutdown(StringTokenizer tokenizer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void help(StringTokenizer tokenizer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void handleCommand(String command, StringTokenizer tokenizer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String helpText(String command) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
