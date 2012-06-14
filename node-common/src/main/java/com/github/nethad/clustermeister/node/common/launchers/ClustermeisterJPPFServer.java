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
package com.github.nethad.clustermeister.node.common.launchers;

/**
 * A JPPF Driver/Server.
 *
 * @author daniel
 */
public class ClustermeisterJPPFServer extends ClustermeisterJPPFThread {
    
    /**
     * FQCN of JPPF Driver runner.
     */
    protected static final String JPPF_DRIVER_CLASS = "org.jppf.server.JPPFDriver";

    /**
     * Create a JPPF Driver.
     */
    public ClustermeisterJPPFServer() {
        this("JPPF Driver");
    }

    /**
     * Create a JPPF Driver with a specified Thread Name.
     * 
     * @param name 
     */
    public ClustermeisterJPPFServer(String name) {
        super(name);
    }
    
    @Override
    public void run() {
        synchronized(monitorObject) {
            //notify waiting threads that config has been loaded
            monitorObject.set(true);
            monitorObject.notifyAll();
        }
        executeMain(JPPF_DRIVER_CLASS, NO_LAUNCHER_ARG);
    }
}
