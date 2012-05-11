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

import com.github.nethad.clustermeister.api.Node;
import com.github.nethad.clustermeister.api.utils.NodeManagementConnector;
import com.github.nethad.clustermeister.provisioning.CommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFManagementByJobsClient;
import com.github.nethad.clustermeister.provisioning.rmi.RmiInfrastructure;
import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Monitor;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import org.apache.commons.configuration.Configuration;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
public class AmazonNodeManager {

    public static final int DEFAULT_SSH_PORT = 22;
    
    private final static Logger logger =
            LoggerFactory.getLogger(AmazonNodeManager.class);
    
    final AmazonInstanceManager amazonInstanceManager;
    
    final Configuration configuration;
    
    RmiInfrastructure rmiInfrastructure = null;
    
    JPPFManagementByJobsClient managementClient = null;
    
    //TODO: make sure this will not cause a memory leak
//    Map<AmazonNode, JPPFManagementByJobsClient> managementClients =
//            Collections.synchronizedMap(new HashMap<AmazonNode, JPPFManagementByJobsClient>());
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
    
    public static CommandLineEvaluation commandLineEvaluation(Configuration configuration, 
            CommandLineHandle handle, RmiInfrastructure rmiInfrastructure) {
        AmazonNodeManager amazonNodeManager = new AmazonNodeManager(configuration);
        amazonNodeManager.registerRmiInfrastructure(rmiInfrastructure);
        CommandLineEvaluation commandLineEvaluation = 
                amazonNodeManager.getCommandLineEvaluation(handle);
        
        return commandLineEvaluation;
    }
    
    public CommandLineEvaluation getCommandLineEvaluation(CommandLineHandle commandLineHandle) {
        return new AmazonCommandLineEvaluation(this, commandLineHandle);
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

    public AmazonInstanceManager getInstanceManager() {
        return amazonInstanceManager;
    }

    public ListenableFuture<? extends Node> addNode(AmazonNodeConfiguration nodeConfiguration,
            Optional<String> instanceId) {
        return executorService.submit(new AmazonNodeManager.AddNodeTask(nodeConfiguration, instanceId));
    }
    
    /**
     *
     * @param node
     * @param shutdownState 
     * @return	The future returns null upon successful completion.
     */
    public ListenableFuture<Void> removeNode(AmazonNode node, 
            AmazonInstanceShutdownState shutdownState) {
        return executorService.submit(
                new AmazonNodeManager.RemoveNodeTask(node, shutdownState, 
                amazonInstanceManager));
        
    }
    
    /**
     *
     * @param node
     * @return	The future returns null upon successful completion.
     */
    public ListenableFuture<Void> removeNode(AmazonNode node) {
        return executorService.submit(
                new AmazonNodeManager.RemoveNodeTask(node, 
                node.getInstanceShutdownState(), amazonInstanceManager));
    }
    
    public void registerManagementClient(JPPFManagementByJobsClient client) {
        this.managementClient = client;
    }
    
    public void registerRmiInfrastructure(RmiInfrastructure rmiInfrastructure) {
        this.rmiInfrastructure = rmiInfrastructure;
    }
    
//    public void removeAllNodes(AmazonInstanceShutdownState shutdownMethod) {
//        try {
//            managementClient.shutdownAllNodes();
//            managedNodesMonitor.enter();
//            try {
//                for(AmazonNode node : nodes) {
//                    switch(shutdownMethod) {
//                        case SUSPENDED: {
//                            amazonInstanceManager.suspendInstance(node.getInstanceId());
//                            break;
//                        }
//                        case TERMINATED: {
//                            amazonInstanceManager.terminateInstance(node.getInstanceId());
//                            break;
//                        }
//                    }
//                }
//                nodes.clear();
//            } finally {
//                managedNodesMonitor.leave();
//            }
//        } catch (Exception ex) {
//            logger.warn("Failed to shut down all nodes.", ex);
//        }
//    }
    
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
//                    String publicIp = Iterables.getFirst(node.getPublicAddresses(), null);
//                    managementClients.put(node, JPPFConfiguratedComponentFactory.getInstance().
//                            createManagementByJobsClient(publicIp, AmazonNodeManager.DEFAULT_SERVER_PORT));
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
    
    private AmazonNode getDriverForNode(final AmazonNode node) {
        managedNodesMonitor.enter();
        try {
            return Iterables.find(drivers, new Predicate<AmazonNode>() {
                @Override
                public boolean apply(AmazonNode driver) {
                    String driverAddress = Iterables.getFirst(driver.getPrivateAddresses(), null);
                    checkNotNull(driverAddress);
                    return node.getDriverAddress().equals(driverAddress);
                }
            }, null);
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
                    instanceMetadata = amazonInstanceManager.createInstance(
                            nodeConfiguration, noMap);
                } catch (RunNodesException ex) {
                    logger.warn("Failed to create instance.", ex);
                    return null;
                }
            } else {
                instanceMetadata = amazonInstanceManager.getInstanceMetadata(instanceId.get());
                if(instanceMetadata.getState() == NodeState.SUSPENDED) {
                    amazonInstanceManager.resumeInstance(instanceMetadata.getId());
                    instanceMetadata = amazonInstanceManager.getInstanceMetadata(instanceId.get());
                }
            }
            AmazonNode node;
            try {
                logger.info("Deploying JPPF-{} on {}", nodeConfiguration.getType().toString(), 
                        instanceMetadata.getId());
                node = amazonInstanceManager.deploy(instanceMetadata, nodeConfiguration);
                node.setDriver(getDriverForNode(node));
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
        private final AmazonInstanceShutdownState shutdownState;
        private final AmazonInstanceManager instanceManager;

        public RemoveNodeTask(AmazonNode node, AmazonInstanceShutdownState shutdownMethod,
                AmazonInstanceManager instanceManager) {
            this.node = node;
            this.shutdownState = shutdownMethod;
            this.instanceManager = instanceManager;
        }

        @Override
        public Void call() throws Exception {
            String publicIp = Iterables.getFirst(node.getPublicAddresses(), null);
            checkNotNull(publicIp, "Can not get public IP of node " + node + ".");
            switch (node.getType()) {
                case DRIVER: {
//                    JPPFManagementByJobsClient client = managementClients.remove(node);
//                    if(client != null) {
//                        client.close();
//                    }
                    driverShutdown(publicIp);
                    break;
                }
                case NODE: {
                    nodeShutdown(node);
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Invalid node type");
                }
            }
            
            switch (shutdownState) {
                case SUSPENDED: {
                    instanceManager.suspendInstance(node.getInstanceId());
                    break;
                }
                case TERMINATED: {
                    instanceManager.terminateInstance(node.getInstanceId());
                    break;
                }
                case RUNNING: {
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

        private void nodeShutdown(AmazonNode node) {
            if(managementClient != null) {
                logger.info("Shutting down node {}.", node);
                try {
                    managementClient.shutdownNode(node.getID());
                } catch(Exception ex) {
                    logger.warn("Failed to shut down {}.\n{}", node, ex.getMessage());
                }
            } else {
                logger.warn("Can not shut down {}. No management client registered.", node);
            }
        }
    }
}
