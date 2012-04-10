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
package com.github.nethad.clustermeister.node.common;

/**
 * Launches a JPPF-Node in a new spawned process (JVM)
 *
 * @author daniel
 */
public class ClustermeisterNodeLauncher extends ClustermeisterLauncher {

    /**
     * JPPF Class to use for launching the node.
     */
    protected static final String NODE_RUNNER = "org.jppf.node.NodeRunner";
    
    /**
     * Start a JPPF-node.
     * 
     * The main method will spawn a new process for the JPPF-node.
     * If an independent process is launched it will kill this JVM as soon as it 
     * obtained the UUID. Otherwise it will block until the child process dies.
     * 
     * @param args 
     *      the first argument "false" to launch an independent process or 
     *      "true" to launch a dependent child process. If no argument is set, 
     *      "true" is assumed.
     *      The second argument is "true" if the UUID should be printed to stdout 
     *      or "false" otherwise. If it is not set, "false" is assumed.
     */
    public static void main(String... args) {
        boolean launchAsChildProcess = getBooleanArgument(args, 0, true);
        boolean printUUID = getBooleanArgument(args, 1, false);
        ClustermeisterLauncher launcher = new ClustermeisterNodeLauncher();
        launcher.setPrintUUIDtoStdOut(printUUID);
        launcher.doLaunch(launchAsChildProcess);
        
        if(!launchAsChildProcess) {
            //Exit from this JVM. The spawned process continues to run.
            System.exit(0);
        }
    }
    
    @Override
    protected String getRunner() {
        return NODE_RUNNER;
    }
}
