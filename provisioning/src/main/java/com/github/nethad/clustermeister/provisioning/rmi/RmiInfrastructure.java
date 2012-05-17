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
package com.github.nethad.clustermeister.provisioning.rmi;

import com.github.nethad.clustermeister.api.Loggers;
import com.github.nethad.clustermeister.api.rmi.IRmiServerForApi;
import com.github.nethad.clustermeister.driver.rmi.IRmiServerForDriver;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Ranges;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class RmiInfrastructure {
    
    private static final int MIN_PORT = 1024;
    private static final int MAX_PORT = 65536;
    private static final int STANDARD_PORT = 61111;
    
    private final Logger logger = LoggerFactory.getLogger(Loggers.PROVISIONING);
//    @Inject
//    @Named(Loggers.PROVISIONING)
//    private Logger logger;
    
    private Registry registry;
    private int registryPort;
    private RmiServerForApi rmiServerForApi;
    private RmiServerForDriver rmiServerForDriver;
    private IRmiServerForApi serverForApiStub;
    private IRmiServerForDriver serverForDriverStub;

    public RmiInfrastructure() {
        this.registryPort = STANDARD_PORT;
    }
    
    /**
     * 
     * @param port has to be a port between 1024 and 65535
     */
    public RmiInfrastructure(int port) {
        if (isValidPort(port)) {
            this.registryPort = port;
        } else {
            throw new IllegalArgumentException("Port " + port + " is not between " + MIN_PORT + " and " + MAX_PORT + ".");
        }
    }

    private boolean isValidPort(int port) {
        return Ranges.closed(MIN_PORT, MAX_PORT).apply(port);
    }

    /**
     * Starts the RMI registry and registers services.
     */
    public void initialize() {
        final String policyUrl = RmiInfrastructure.class.getResource("/cm.policy").toString();
        logger.info("Policy file URL: {}", policyUrl);
        System.setProperty("java.security.policy", policyUrl);
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        registryPort = findAvailablePort();
        try {
            registry = LocateRegistry.createRegistry(registryPort);
            createRmiServerForDriver();
            logger.info("RmiServerForDriver bound");
            createRmiServerForApi();
            logger.info("RmiServerForApi bound");
            NodeManager nodeManager = new NodeManager();
            rmiServerForApi.setNodeManager(nodeManager);
            rmiServerForDriver.setNodeManager(nodeManager);
            
        } catch (Exception ex) {
            logger.error("RmiServerForDriver exception:", ex);
        }
    }
    
    /**
     * The RMI registry port.
     * @return 
     */
    public int getRegistryPort() {
        return registryPort;
    }
    
    @VisibleForTesting
    IRmiServerForApi getRmiServerForApi() {
        return serverForApiStub;
    }
    
    @VisibleForTesting
    IRmiServerForDriver getRmiServerForDriver() {
        return serverForDriverStub;
    }
    
    @VisibleForTesting
    public RmiServerForDriver getRmiServerForDriverObject() {
        return rmiServerForDriver;
    }
    
    public RmiServerForApi getRmiServerForApiObject() {
        return rmiServerForApi;
    }

    private void createRmiServerForDriver() throws RemoteException {
        final String name = IRmiServerForDriver.NAME;
        rmiServerForDriver = new RmiServerForDriver();
        serverForDriverStub = (IRmiServerForDriver) UnicastRemoteObject.exportObject(rmiServerForDriver, 0);
        registry.rebind(name, serverForDriverStub);
    }
    
    private void createRmiServerForApi() throws RemoteException {
        final String name = IRmiServerForApi.NAME;
        rmiServerForApi = new RmiServerForApi();
        serverForApiStub = (IRmiServerForApi) UnicastRemoteObject.exportObject(rmiServerForApi, 0);
        registry.rebind(name, serverForApiStub);
    }

    private int findAvailablePort() {
        int port = this.registryPort;
        while (!isAvailable(port)) {
            port++;
        }
        return port;
    }
    
    public boolean isAvailable(int port) {
        if (!isValidPort(port)) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }

        ServerSocket serverSocket = null;
        DatagramSocket datagramSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            datagramSocket = new DatagramSocket(port);
            datagramSocket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (datagramSocket != null) {
                datagramSocket.close();
            }
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    /*
                     * should not be thrown
                     */
                }
            }
        }
        return false;
    }

    void unregister() throws NoSuchObjectException {
        UnicastRemoteObject.unexportObject(rmiServerForApi, true);
        UnicastRemoteObject.unexportObject(rmiServerForDriver, true);
        UnicastRemoteObject.unexportObject(registry, true);
    }

}
