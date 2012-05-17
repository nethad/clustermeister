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
import com.github.nethad.clustermeister.api.impl.NodeInformationImpl;
import com.github.nethad.clustermeister.driver.rmi.IRmiServerForDriver;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.management.JPPFSystemInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An RMI server/service for the JPPF driver to call on node (dis)connects.
 * @author thomas
 */
public class RmiServerForDriver implements IRmiServerForDriver {
    private final Logger logger = LoggerFactory.getLogger(Loggers.PROVISIONING);
    
    private NodeManager nodeManager;
    
    private List<NodeConnectionListener> listeners = new ArrayList<NodeConnectionListener>();

    /**
     * To be called when a node connects.
     * @param managementInfo
     * @param systemInformation 
     */
    @Override
    public void onNodeConnected(JPPFManagementInfo managementInfo, JPPFSystemInformation systemInformation) {
        logger.info("Node connected "+managementInfo.getId());
        NodeInformationImpl nodeInformation = new NodeInformationImpl(managementInfo.getId(), systemInformation);
        nodeManager.addNode(nodeInformation);
        notifyListenersConnected(managementInfo, systemInformation);
    }
    
    /**
     * To be called when a node disconnects.
     * @param managementInfo
     * @throws RemoteException 
     */
    @Override
    public void onNodeDisconnected(JPPFManagementInfo managementInfo) throws RemoteException {
        logger.info("Node disconnected "+managementInfo.getId());
        nodeManager.removeNode(managementInfo.getId());
        notifyListenersDisconnected(managementInfo);
    }
    
    public void setNodeManager(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }
    
    public void addNodeConnectionListener(NodeConnectionListener listener) {
        listeners.add(listener);
    }
    
    public void removeNodeConnectionListener(NodeConnectionListener listener) {
        listeners.remove(listener);
    }

    private void notifyListenersConnected(JPPFManagementInfo managementInfo, JPPFSystemInformation systemInformation) {
        for (NodeConnectionListener nodeConnectionListener : listeners) {
            nodeConnectionListener.onNodeConnected(managementInfo, systemInformation);
        }
    }
    
    private void notifyListenersDisconnected(JPPFManagementInfo managementInfo) {
        for (NodeConnectionListener nodeConnectionListener : listeners) {
            nodeConnectionListener.onNodeDisconnected(managementInfo);
        }
    }
}
