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

import com.github.nethad.clustermeister.node.common.builders.JPPFClientBuilder;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFClientConnectionStatus;
import org.jppf.client.event.ClientConnectionStatusEvent;
import org.jppf.client.event.ClientConnectionStatusListener;
import org.jppf.client.event.ClientEvent;
import org.jppf.client.event.ClientListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to wait for a JPPFClient to connect to a JPPF Driver.
 *
 * @author daniel
 */
public class ClientConnectionAwaiter {
    private final static Logger logger = LoggerFactory.getLogger("COMMON-NODE");
    
    private final JPPFClient client;
    private final Lock initLock = new ReentrantLock();
    private final Condition clientConnected = initLock.newCondition();

    /**
     * Create a new ClientConnectionAwaiter for a driver connection specified by host and port.
     *
     * @param driverHost the driver host
     * @param driverPort the driver port
     */
    public ClientConnectionAwaiter(String driverHost, int driverPort) {
        JPPFClientBuilder clientBuilder = 
                new JPPFClientBuilder("initMonitorClient", new DriverConnectionListener());
        clientBuilder.setProperty("jppf.drivers", "startingDriver");
        clientBuilder.setProperty("startingDriver.jppf.server.host", driverHost);
        clientBuilder.setProperty("startingDriver.jppf.server.port", String.valueOf(driverPort));
        clientBuilder.setProperty("jppf.discovery.enabled", "false");
        clientBuilder.setProperty("jppf.management.enabled", "false");
        this.client = clientBuilder.build();
    }

    /**
     * Wait (non-busy) for the connection status to become AVAILABLE.
     */
    public void await() {
        initLock.lock();
        try {
            while (!client.hasAvailableConnection()) {
                try {
                    clientConnected.await();
                } catch (InterruptedException ex) {
                    logger.warn("Interrupted while waiting for driver to initialize.", ex);
                }
            }
        } finally {
            initLock.unlock();
            client.close();
        }
    }

    private class DriverConnectionListener implements ClientListener {

        @Override
        public void newConnection(ClientEvent event) {
            event.getConnection().addClientConnectionStatusListener(new DriverConnectionStatusListener());
        }

        @Override
        public void connectionFailed(ClientEvent event) {
            logger.warn("Connection to driver failed!");
        }
    }

    private class DriverConnectionStatusListener implements ClientConnectionStatusListener {

        @Override
        public void statusChanged(ClientConnectionStatusEvent event) {
            if (event.getClientConnectionStatusHandler().getStatus() == JPPFClientConnectionStatus.ACTIVE) {
                initLock.lock();
                try {
                    clientConnected.signalAll();
                } finally {
                    initLock.unlock();
                }
            }
        }
    }
}
