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

import com.github.nethad.clustermeister.api.ExecutorNode;
import com.github.nethad.clustermeister.api.NodeInformation;
import com.google.common.collect.Collections2;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class NodeManager {
    private final Logger logger = LoggerFactory.getLogger(NodeManager.class);
    
    Map<String, NodeInformation> nodeMap;

    public NodeManager() {
        nodeMap = new HashMap<String, NodeInformation>();
    }
    
    public void addNode(NodeInformation nodeInformation) {
        String nodeId = nodeInformation.getID();
        if (!nodeMap.containsKey(nodeId)) {
            nodeMap.put(nodeId, nodeInformation);
        } else {
            logger.warn("Tried to add node "+nodeId+" but it was already present in local collection.");
        }
    }

    void removeNode(String nodeId) {
        if (nodeMap.remove(nodeId) == null) {
            logger.warn("Tried to remove node "+nodeId+" but it was not present in local collection.");
        }
    }
    
    public Collection<NodeInformation> getAllNodes() {
        return nodeMap.values();
    }
    
}
