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
package com.github.nethad.clustermeister.node.common.builders;

import com.github.nethad.clustermeister.node.common.launchers.ClustermeisterJPPFServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configures and starts a new JPPF Driver/Server.
 *
 * @author daniel
 */
public class JPPFDriverBuilder extends PropertyConfiguratedJPPFComponentBuilder<ClustermeisterJPPFServer> {
    
    private final static Logger logger = LoggerFactory.getLogger("COMMON-NODE");
    
    @Override
    protected ClustermeisterJPPFServer doBuild() {
        ClustermeisterJPPFServer server = new ClustermeisterJPPFServer();
        synchronized(server.getMonitor()) {
            server.start();
                //wait for server to load configuration.
            try {
                while(!server.getMonitor().get()) {
                    server.getMonitor().wait();
                }
            } catch (InterruptedException ex) {
                logger.error("Interrupted while waiting for driver to initialize.");
            }
        }   
        return server;
    }
}
