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
 * This Server listens to Remote LOG4J Logging Events.
 * 
 * <p>
 * In order to receive logging events a remote node needs to configure a SocketAppender.
 * </p>
 * 
 * @see <a href="http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/net/SocketAppender.html">
 *  SocketAppender</a>
 *
 * @author daniel
 */
public class RemoteLoggingServer extends Thread {
    
    /**
     * TCP Port to listen on for logging events.
     */
    protected final int serverSocketPort;
    
    private static final Logger logger = LoggerFactory.getLogger(Loggers.CLI);

    /**
     * Create a new RemoteLOggingServer.
     * 
     * @param serverSocketPort the port to which the server will bind to to.
     */
    public RemoteLoggingServer(int serverSocketPort) {
        this.serverSocketPort = serverSocketPort;
        this.setName(String.format("%s-%d", getClass().getSimpleName(), getId()));
    }

    @Override
    public void run() {
        try {
            logger.info("Starting remote logging server on port {}.", serverSocketPort);
            ServerSocket serverSocket = new ServerSocket(serverSocketPort);
            while(true) {
                logger.debug("Waiting for a client to connect.");
                Socket clientConnection = serverSocket.accept();
                logger.debug("Client {} connected", clientConnection.getInetAddress());
                String threadName = String.format("[RemoteLoggingClient: %s]", 
                        clientConnection.getRemoteSocketAddress());
                new Thread(new SocketNode(clientConnection, 
                        LogManager.getLoggerRepository()), threadName).start();
            }
        } catch (Throwable t) {
            logger.error("Exception while waiting for client connections.", t);
        }
    }
}
