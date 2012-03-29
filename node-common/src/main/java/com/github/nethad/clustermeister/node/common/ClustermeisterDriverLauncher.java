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
 * Launches a JPPF-Driver in a new spawned process (JVM).
 *
 * @author daniel
 */
public class ClustermeisterDriverLauncher extends ClustermeisterLauncher {

    /**
     * JPPF Class to use for launching the driver.
     */
    protected static final String DRIVER_RUNNER = "org.jppf.server.JPPFDriver";
    
    private final boolean useRmi;
    
    /**
     * Start a JPPF-driver.
     * 
     * The main method will spawn a new process for the JPPF-driver.
     * If an independent process is launched it will kill this JVM as soon as it 
     * obtained the UUID. Otherwise it will block until the child process dies.
     * 
     * @param args 
     *      the first argument "false" to launch an independent process or 
     *      "true" to launch a dependent child process. If no argument is set, 
     *      "true" is assumed.
     *      the second argument is "true" if RMI should be used (for local drivers) or
     *      "false" if not (default).
     */
    public static void main(String... args) {
        boolean launchAsChildProcess = true;
        boolean useRmi = false;
        if(args != null) {
            if (args.length >= 1) {
                launchAsChildProcess = Boolean.parseBoolean(args[0]);
            }
            if (args.length >= 2) {
                useRmi = Boolean.parseBoolean(args[1]);
            }
        }
        ClustermeisterLauncher launcher = new ClustermeisterDriverLauncher(useRmi);
        launcher.doLaunch(launchAsChildProcess);
        if(!launchAsChildProcess) {
            //Exit from this JVM. The spawned process continues to run.
            System.exit(0);
        }
    }

    public ClustermeisterDriverLauncher(boolean useRmi) {
        this.useRmi = useRmi;
    }
    
    public ClustermeisterDriverLauncher() {
        this.useRmi = false;
    }
    
    @Override
    protected String getRunner() {
        return DRIVER_RUNNER;
    }

    @Override
    protected ClustermeisterProcessLauncher createProcessLauncher() {
        ClustermeisterProcessLauncher launcher = super.createProcessLauncher();
        launcher.setUseRmi(useRmi);
        return launcher;
    }
    
    
}
