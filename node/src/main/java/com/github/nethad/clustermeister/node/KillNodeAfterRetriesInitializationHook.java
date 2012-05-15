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
package com.github.nethad.clustermeister.node;

import org.jppf.node.initialization.InitializationHook;
import org.jppf.utils.UnmodifiableTypedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * This InitializationHook kills the node after the configured period of unsuccessful attempts to connect to a driver.
 * 
 * @author thomas
 */
public class KillNodeAfterRetriesInitializationHook implements InitializationHook {
    
    private static final Logger logger = LoggerFactory.getLogger(KillNodeAfterRetriesInitializationHook.class);
    
    private boolean firstStart = true;
    
    @Override
    public void initializing(UnmodifiableTypedProperties initialConfiguration) {
        logger.info("KillNodeAfterRetriesInitializationHook initializing.");
        logger.warn("Multiple servers in the configuration file are ignored at the moment. If the first server "
                + "is not reachable, the node shuts down itself.");
        if (firstStart) {
            logger.info("First start, try to connect to server.");
        } else {
            shutdown();
        }
        firstStart = false;
    }

    private void shutdown() {
        logger.info("Could not connect to server, shutting down.");
        System.exit(0);
    }
    
}
