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
package com.github.nethad.clustermeister.provisioning.cli;

import com.github.nethad.clustermeister.api.Loggers;
import java.net.ServerSocket;
import java.net.Socket;
import org.apache.log4j.LogManager;
import org.apache.log4j.net.SocketNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
public class RemoteLoggingServer extends Thread {
    /**
     * Port to listen on for logging events.
     */
    public static final int PORT = 54321;
    
    private static final Logger logger = LoggerFactory.getLogger(Loggers.CLI);

    @Override
    public void run() {
        try {
            logger.info("Starting remote logging server on port {}.", PORT);
            ServerSocket serverSocket = new ServerSocket(PORT);
            while(true) {
                logger.info("Waiting for a client to connect.");
                Socket clientConnection = serverSocket.accept();
                logger.info("Client {} connected", clientConnection.getInetAddress());
                new Thread(new SocketNode(clientConnection, LogManager.getLoggerRepository()), 
                        String.format("[Remote: %s]", clientConnection.getRemoteSocketAddress(), 
                        clientConnection.getRemoteSocketAddress())).start();
            }
        } catch (Throwable t) {
            logger.error("Exception while waiting for client connections.", t);
        }
    }
}
