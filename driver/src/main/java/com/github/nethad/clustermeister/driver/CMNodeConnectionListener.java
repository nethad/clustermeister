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

import com.github.nethad.clustermeister.driver.rmi.IRmiServerForDriver;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import org.jppf.server.event.NodeConnectionEvent;
import org.jppf.server.event.NodeConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class CMNodeConnectionListener implements NodeConnectionListener  {
    private final Logger logger = LoggerFactory.getLogger(CMNodeConnectionListener.class);
    
    private Registry registry;
    private IRmiServerForDriver server;
    private boolean successfulInit = false;

    public CMNodeConnectionListener() {
        logger.info("Initialize CMNodeConnectionListener");
        try {
            registry = LocateRegistry.getRegistry("localhost", 61111);
            logger.info("registry = "+Arrays.toString(registry.list()));
            server = (IRmiServerForDriver) registry.lookup(IRmiServerForDriver.NAME);
            successfulInit = true;
        } catch (NotBoundException ex) {
            logger.error("", ex);
        } catch (AccessException ex) {
            logger.error("", ex);
        } catch (RemoteException ex) {
            logger.error("", ex);
        }
    }

    @Override
    public void nodeConnected(NodeConnectionEvent event) {
        if (successfulInit) {
            try {
                server.onNodeConnected(event.getNodeInformation());
            } catch (RemoteException ex) {
                logger.error("Could not send node connected message to server.", ex);
            }
        }
    }

    @Override
    public void nodeDisconnected(NodeConnectionEvent event) {
        // do nothing
    }

}
