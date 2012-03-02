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
package com.github.nethad.clustermeister.provisioning.utils;

import java.util.concurrent.TimeoutException;
import org.jppf.management.JMXConnectionWrapper;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.JMXNodeConnectionWrapper;

/**
 *
 * @author daniel
 */
public final class NodeManagementConnector {

    public static JMXNodeConnectionWrapper connectToNodeManagement_node(
            JMXNodeConnectionWrapper wrapper) throws TimeoutException {

        return (JMXNodeConnectionWrapper) connectToNodeManagement(wrapper);
    }

    public static JMXDriverConnectionWrapper connectToNodeManagement_driver(
            JMXDriverConnectionWrapper wrapper) throws TimeoutException {

        return (JMXDriverConnectionWrapper) connectToNodeManagement(wrapper);
    }

    public static JMXConnectionWrapper connectToNodeManagement(JMXConnectionWrapper wrapper)
            throws TimeoutException {
        wrapper.connectAndWait(3000);
        if (!wrapper.isConnected()) {
            /*
             * Optimization: sometimes it seems the backoff is too large. Now:
             * If timeout after 3 seconds. Try new connection with timeout 5
             * seconds. Good chance the new connection will succeed instantly or
             * quicker than waiting for long timeout on the first connection.
             * Fallback is waiting 2 minutes for timeout.
             */
            wrapper.connectAndWait(5000);
            if (!wrapper.isConnected()) {
                wrapper.connectAndWait(180000);
            }
        }
        if (wrapper.isConnected()) {
            return wrapper;
        } else {
            throw new TimeoutException("Timed out while for node JMX management to become available.");
        }
    }

    public static JMXNodeConnectionWrapper openNodeConnection(String host, int port) 
            throws TimeoutException {
        
        return connectToNodeManagement_node(new JMXNodeConnectionWrapper(host, port));
    }

    public static JMXDriverConnectionWrapper openDriverConnection(String host, int port) 
            throws TimeoutException {
        
        return connectToNodeManagement_driver(new JMXDriverConnectionWrapper(host, port));
    }
}
