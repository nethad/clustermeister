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
package com.github.nethad.clustermeister.provisioning.ec2;

import com.github.nethad.clustermeister.api.Configuration;
import com.github.nethad.clustermeister.api.Node;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFConfiguratedComponentFactory;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFManagementByJobsClient;
import com.github.nethad.clustermeister.provisioning.utils.NodeManagementConnector;
import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Monitor;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.JMXNodeConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
public class AmazonNodeManager {

    public static final int DEFAULT_MANAGEMENT_PORT = 11198;
    
    private final static Logger logger =
            LoggerFactory.getLogger(AmazonNodeManager.class);
    
    final AmazonInstanceManager amazonInstanceManager;
    
    final Configuration configuration;
    
    //TODO: make sure this will not cause a memory leak
    Map<AmazonNode, JPPFManagementByJobsClient> managementClients =
            Collections.synchronizedMap(new HashMap<AmazonNode, JPPFManagementByJobsClient>());
    private Set<AmazonNode> drivers = new HashSet<AmazonNode>();
    private Set<AmazonNode> nodes = new HashSet<AmazonNode>();
    
    private final Monitor managedNodesMonitor = new Monitor(false);
    
    private final ListeningExecutorService executorService;

    public AmazonNodeManager(Configuration configuration) {
        this.configuration = configuration;
        executorService = MoreExecutors.listeningDecorator(
                Executors.newCachedThreadPool());
        this.amazonInstanceManager =
                new AmazonInstanceManager(configuration, executorService);
    }

    public Collection<? extends Node> getNodes() {
        managedNodesMonitor.enter();
        try {
            List<AmazonNode> allNodes =
                    new ArrayList<AmazonNode>(drivers.size() + nodes.size());
            allNodes.addAll(nodes);
            allNodes.addAll(drivers);
            return Collections.unmodifiableCollection(allNodes);
        } finally {
            managedNodesMonitor.leave();
        }
    }

    public ListenableFuture<? extends Node> addNode(AmazonNodeConfiguration nodeConfiguration,
            Optional<String> instanceId) {
        return executorService.submit(new AddNodeTask(nodeConfiguration, instanceId));
    }

    /**
     *
     * @param node
     * @param shutdownMethod
     * @return	The future returns null upon successful completion.
     */
    public ListenableFuture<Void> removeNode(AmazonNode node,
            AmazonInstanceShutdownMethod shutdownMethod) {
        return executorService.submit(
                new RemoveNodeTask(node, shutdownMethod, amazonInstanceManager));
    }

    public void close() {
        if (amazonInstanceManager != null) {
            managedNodesMonitor.enter();
            try {
                drivers.clear();
                nodes.clear();
            } finally {
                managedNodesMonitor.leave();
            }
            amazonInstanceManager.close();
        }
    }

    private void addManagedNode(AmazonNode node) {
        managedNodesMonitor.enter();
        try {
            switch (node.getType()) {
                case NODE: {
                    nodes.add(node);
                    break;
                }
                case DRIVER: {
                    drivers.add(node);
                    String publicIp = Iterables.getFirst(node.getPublicAddresses(), null);
                    managementClients.put(node, JPPFConfiguratedComponentFactory.getInstance().
                            createManagementByJobsClient(publicIp, 11111));
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Invalid Node Type.");
                }
            }
        } finally {
            managedNodesMonitor.leave();
        }
    }

    private void removeManagedNode(AmazonNode node) {
        managedNodesMonitor.enter();
        try {
            switch (node.getType()) {
                case NODE: {
                    nodes.remove(node);
                    break;
                }
                case DRIVER: {
                    drivers.remove(node);
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Invalid Node Type.");
                }
            }
        } finally {
            managedNodesMonitor.leave();
        }
    }

    private class AddNodeTask implements Callable<AmazonNode> {

        private final AmazonNodeConfiguration nodeConfiguration;
        private final Optional<String> instanceId;

        public AddNodeTask(AmazonNodeConfiguration nodeConfiguration, Optional<String> instanceId) {
            this.nodeConfiguration = nodeConfiguration;
            this.instanceId = instanceId;
        }

        @Override
        public AmazonNode call() throws Exception {
            NodeMetadata instanceMetadata;
            if (!instanceId.isPresent()) {
                try {
                    Optional<Map<String, String>> noMap = Optional.absent();
                    instanceMetadata = amazonInstanceManager.createInstance(noMap);
                } catch (RunNodesException ex) {
                    logger.warn("Failed to create instance.", ex);
                    return null;
                }
            } else {
                instanceMetadata = amazonInstanceManager.getInstanceMetadata(instanceId.get());
                if(instanceMetadata.getState() == NodeState.SUSPENDED) {
                    amazonInstanceManager.resumeInstance(instanceMetadata.getId());
                }
            }
            AmazonNode node;
            try {
                logger.info("Deploying JPPF-{} on {}", nodeConfiguration.getType().toString(), 
                        instanceMetadata.getId());
                node = amazonInstanceManager.deploy(instanceMetadata, nodeConfiguration);
                logger.info("JPPF-{} deployed on {}.", nodeConfiguration.getType().toString(), 
                        instanceMetadata.getId());
            } catch (Throwable ex) {
                logger.warn("Failed to deploy node.", ex);
                if (instanceId.isPresent()) {
                    amazonInstanceManager.suspendInstance(instanceMetadata.getId());
                } else {
                    amazonInstanceManager.terminateInstance(instanceMetadata.getId());
                }
                return null;
            }

            addManagedNode(node);

            return node;
        }
    }

    private class RemoveNodeTask implements Callable<Void> {

        private final AmazonNode node;
        private final AmazonInstanceShutdownMethod shutdownMethod;
        private final AmazonInstanceManager instanceManager;

        public RemoveNodeTask(AmazonNode node, AmazonInstanceShutdownMethod shutdownMethod,
                AmazonInstanceManager instanceManager) {
            this.node = node;
            this.shutdownMethod = shutdownMethod;
            this.instanceManager = instanceManager;
        }

        @Override
        public Void call() throws Exception {

            String publicIp = Iterables.getFirst(node.getPublicAddresses(), null);
            checkNotNull(publicIp, "Can not get public IP of node " + node + ".");
            switch (node.getType()) {
                case DRIVER: {
                    JPPFManagementByJobsClient client = managementClients.remove(node);
                    if(client != null) {
                        client.close();
                    }
                    driverShutdown(publicIp);
                    break;
                }
                case NODE: {
                    nodeShutdown(publicIp);
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Invalid node type");
                }
            }

            switch (shutdownMethod) {
                case SHUTDOWN: {
                    instanceManager.suspendInstance(node.getInstanceId());
                    break;
                }
                case TERMINATE: {
                    instanceManager.terminateInstance(node.getInstanceId());
                    break;
                }
                case NO_SHUTDOWN: {
                    logger.info("{} specified. Instance continues running.", 
                            AmazonInstanceShutdownMethod.NO_SHUTDOWN.toString());
                    //do nothing
                    break;
                }
                default: {
                    logger.warn("Invalid shutdown method specified. Suspending instance...");
                    instanceManager.suspendInstance(node.getInstanceId());
                    break;
                }
            }

            removeManagedNode(node);

            return null;
        }

        private void driverShutdown(String publicIp) throws TimeoutException, Exception {
            JMXDriverConnectionWrapper wrapper =
                    new JMXDriverConnectionWrapper(publicIp, node.getManagementPort());
            NodeManagementConnector.connectToNodeManagement(wrapper);
            logger.info("Shutting driver node {}:{}.", publicIp, node.getManagementPort());
            wrapper.restartShutdown(0l, -1l);
            try {
                wrapper.close();
            } catch (Exception ex) {
                logger.warn("Could not close connection to node management.", ex);
            }
        }

        private void nodeShutdown(String publicIp) throws Exception, TimeoutException {
            JMXNodeConnectionWrapper wrapper =
                    new JMXNodeConnectionWrapper(publicIp, node.getManagementPort());
            NodeManagementConnector.connectToNodeManagement(wrapper);
            logger.info("Shutting down node {}:{}.", publicIp, node.getManagementPort());
            wrapper.shutdown();
            try {
                wrapper.close();
            } catch (Exception ex) {
                logger.warn("Could not close connection to node management.", ex);
            }
        }
    }
}
