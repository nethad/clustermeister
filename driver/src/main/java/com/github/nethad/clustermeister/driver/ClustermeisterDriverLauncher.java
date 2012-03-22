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
package com.github.nethad.clustermeister.driver;

import com.github.nethad.clustermeister.node.ClustermeisterLauncher;
import java.io.UnsupportedEncodingException;

/**
 * Launches a JPPF-Driver in a new spawned process (independent JVM) and returns
 * when the driver is initialized.
 *
 * @author daniel
 */
public class ClustermeisterDriverLauncher extends ClustermeisterLauncher {

    /**
     * JPPF Class to use for launching the driver.
     */
    protected static final String DRIVER_RUNNER = "org.jppf.server.JPPFDriver";
    
    /**
     * Start a JPPF-driver.
     * 
     * The main method will spawn a new process for the JPPF-node and return as 
     * soon as it obtained the UUID and initialization of JMX management is complete.
     * 
     * @param args not used.
     */
    public static void main(String... args) throws UnsupportedEncodingException {
        
        new ClustermeisterDriverLauncher().doLaunch();
        
        //Exit from this JVM. The spawned process continues to run.
        System.exit(0);
    }
    
    @Override
    protected String getRunner() {
        return DRIVER_RUNNER;
    }
}
