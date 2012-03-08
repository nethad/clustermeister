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
import com.github.nethad.clustermeister.api.impl.KeyPairCredentials;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Monitor;
import com.google.common.util.concurrent.SettableFuture;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.jclouds.aws.ec2.reference.AWSEC2Constants;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.ec2.compute.options.EC2TemplateOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages AWS EC2 Instances.
 *
 * Instances are not equal to nodes in the context of Clustermeister. Nodes are
 * JPPF Nodes or Drivers while Instances are AWS EC2 (virtual) machines.
 * Multiple nodes can be deployed on a single instance (e.g. a JPPF driver and a
 * JPPF node).
 *
 * This class is designed to be thread-safe.
 *
 * @author thomas, daniel
 */
public class AmazonInstanceManager {

    /**
     * jClouds group name.
     *
     * AWS EC2 instances will be named GROUP_NAME-xxxxxxxx (e.g.
     * clustermeister-1c045955)
     */
    static final String GROUP_NAME = "clustermeister";
    private final static Logger logger =
            LoggerFactory.getLogger(AmazonInstanceManager.class);
    private String accessKeyId;
    private String secretKey;
    private String keyPair;
    private String locationId;
    private String imageId;
    private final ListenableFuture<ComputeServiceContext> contextFuture;
    private final SettableFuture<Template> templateFuture;
    private final ListeningExecutorService executorService;
    private final Monitor portCounterMonitor = new Monitor(false);
    private final Map<String, Integer> instanceToPortCounter =
            new HashMap<String, Integer>();

    /**
     * Creates a new AmazonInstanceManager.
     *
     * This class is designed to be used by the AmazonNodeManager and each
     * AmazonNodeManager should use only a single instance.
     *
     * @param config The configuration containing AWS credentials.
     * @param executorService The ExecutorService used to perform asynchronous
     * tasks, such as context set-up and template building.
     */
    AmazonInstanceManager(Configuration config, ListeningExecutorService executorService) {
        this.executorService = executorService;
        loadConfiguration(config);
        templateFuture = SettableFuture.create();
        //from here the configuration must be loaded.
        contextFuture = createContext();
        //after the context is ready, build the template.
        Futures.addCallback(contextFuture, new FutureCallback<ComputeServiceContext>() {

            @Override
            public void onSuccess(ComputeServiceContext result) {
                templateFuture.set(buildTemplate(result));
            }

            @Override
            public void onFailure(Throwable t) {
                templateFuture.setException(t);
                throw new RuntimeException(t);
            }
        }, executorService);
    }

    /**
     * Release resources used by the instance manager.
     */
    void close() {
        try {
            ComputeServiceContext context = contextFuture.get();
            logger.debug("Closing context...");
            context.close();
            logger.debug("Context Closed.");
        } catch (Exception ex) {
            //do nothing, instance manager is in corrupt state and 
            //context could not be created.
        }
    }

    /**
     * Performs an Amazon API call to retrieve all AWS EC2 instances.
     *
     * @return A set containing all registered AWS EC2 instances regardless
     * of state.
     */
    Set<? extends ComputeMetadata> getInstances() {
        return valueOrNotReady(contextFuture).getComputeService().listNodes();
    }

    /**
     * Get meta data for a given instance.
     *
     * @param id	The jClouds node ID.
     * @return	jClouds node meta data object.
     */
    NodeMetadata getInstanceMetadata(String id) {
        return valueOrNotReady(contextFuture).getComputeService().getNodeMetadata(id);
    }

    /**
     * Create a new instance.
     *
     * @param userMetaData A map containing optional user-defined tags.
     * @return	Meta data object for the created instance.
     *
     * @throws RunNodesException	If the instance could not be started.
     */
    NodeMetadata createInstance(Optional<Map<String, String>> userMetaData) throws RunNodesException {
        logger.info("Creating a new instance...");
        ComputeServiceContext context = valueOrNotReady(contextFuture);
        Template template = valueOrNotReady(templateFuture);

        if (userMetaData.isPresent()) {
            template.getOptions().userMetadata(userMetaData.get());
        }
        template.getOptions().
                inboundPorts(
                AmazonNodeManager.DEFAULT_SSH_PORT, 
                AmazonNodeManager.DEFAULT_SERVER_PORT, 
                AmazonNodeManager.DEFAULT_SERVER_CLIENT_PORT, 
                AmazonNodeManager.DEFAULT_SERVER_NODE_PORT, //TODO: may not be needed
                AmazonNodeManager.DEFAULT_MANAGEMENT_PORT, 
                11199, 11200, 11201, 11202, 11203, //TODO: excess management ports, remove need for these
                AmazonNodeManager.DEFAULT_MANAGEMENT_RMI_PORT);

        // specify your own keypair for use in creating nodes
        //TODO: remove need to specify keypair in AWS.
        template.getOptions().as(EC2TemplateOptions.class).keyPair(keyPair);
        Set<? extends NodeMetadata> instances = context.getComputeService().
                createNodesInGroup(GROUP_NAME, 1, template);

        NodeMetadata metadata = Iterables.getOnlyElement(instances);
        logger.info("Instance {} created.", metadata.getId());
        return metadata;
    }

    /**
     * Deploy JPPF node (or driver) on an instance.
     *
     * @param instanceMetadata Meta data object identifying the instance.
     * @param nodeConfig Node configuration specifying the node to deploy.
     * @return The created node handle.
     *
     * @throws TimeoutException When
     */
    AmazonNode deploy(NodeMetadata instanceMetadata, AmazonNodeConfiguration nodeConfig)
            throws TimeoutException {

        ComputeServiceContext context = valueOrNotReady(contextFuture);

        int managementPort;
        switch (nodeConfig.getType()) {
            case NODE: {
                managementPort = getNextNodeManagementPort(instanceMetadata);
                nodeConfig.setManagementPort(managementPort);
                AmazonEC2JPPFDeployer deployer =
                        new AmazonEC2JPPFNodeDeployer(context, instanceMetadata,
                        getLoginCredentials(nodeConfig), nodeConfig);
                deployer.deploy();
                break;
            }
            case DRIVER: {
                managementPort = AmazonNodeManager.DEFAULT_MANAGEMENT_PORT;
                nodeConfig.setManagementPort(managementPort);
                AmazonEC2JPPFDeployer deployer =
                        new AmazonEC2JPPFDriverDeployer(context, instanceMetadata,
                        getLoginCredentials(nodeConfig), nodeConfig);
                deployer.deploy();

                break;
            }
            default: {
                throw new IllegalArgumentException("Invalid Node Type.");
            }
        }

        AmazonNode node = new AmazonNode(getId(instanceMetadata, managementPort),
                nodeConfig, instanceMetadata);
        return node;
    }

    private String getId(NodeMetadata instanceMetadata, int managementPort) {
        return instanceMetadata.getId() + ":" + String.valueOf(managementPort);
    }

    private int getNextNodeManagementPort(NodeMetadata instanceMetadata) {
        portCounterMonitor.enter();
        try {
            Integer portCounter = instanceToPortCounter.get(instanceMetadata.getId());
            if (portCounter == null) {
                portCounter = new Integer(AmazonNodeManager.DEFAULT_MANAGEMENT_PORT + 1);
                instanceToPortCounter.put(instanceMetadata.getId(), portCounter);
            } else {
                portCounter += 1;
            }
            return portCounter;
        } finally {
            portCounterMonitor.leave();
        }
    }

    private void decrementAndManagePortCounter(String instanceId) {
        portCounterMonitor.enter();
        try {
            Integer portCounter = instanceToPortCounter.get(instanceId);
            if (portCounter != null) {
                portCounter -= 1;
                if (portCounter <= AmazonNodeManager.DEFAULT_MANAGEMENT_PORT) {
                    instanceToPortCounter.remove(instanceId);
                }
            }
        } finally {
            portCounterMonitor.leave();
        }
    }

    /**
     * Suspend (stop) an instance.
     *
     * Shuts down the instance but the instance stays available for resuming.
     *
     * @param instanceId	jClouds node ID.
     */
    void suspendInstance(String instanceId) {
        logger.info("Suspending instance {}.", instanceId);
        valueOrNotReady(contextFuture).getComputeService().suspendNode(instanceId);
        decrementAndManagePortCounter(instanceId);
        AmazonEC2JPPFDeployer.removeDriverMonitor(instanceId);
        AmazonEC2JPPFDeployer.removeNodeMonitor(instanceId);
        logger.info("Instance {} suspended.", instanceId);
    }

    /**
     * Terminate (destroy) an instance.
     *
     * Shuts down and discards the instance.
     *
     * @param instanceId	jClouds node ID.
     */
    void terminateInstance(String instanceId) {
        logger.info("Terminating instance {}.", instanceId);
        valueOrNotReady(contextFuture).getComputeService().destroyNode(instanceId);
        decrementAndManagePortCounter(instanceId);
        AmazonEC2JPPFDeployer.removeDriverMonitor(instanceId);
        AmazonEC2JPPFDeployer.removeNodeMonitor(instanceId);
        logger.info("Instance {} terminated.", instanceId);
    }

    /**
     * Resume (start) an instance.
     *
     * @param instanceId	jClouds node ID.
     */
    void resumeInstance(String instanceId) {
        logger.info("Resuming instance {}.", instanceId);
        valueOrNotReady(contextFuture).getComputeService().resumeNode(instanceId);
        logger.info("Instance {} resumed.", instanceId);
    }

    private LoginCredentials getLoginCredentials(AmazonNodeConfiguration config) {
        KeyPairCredentials credentials = config.getCredentials().get();
        
        try {
            String privateKey = credentials.getPrivateKey();
            return new LoginCredentials(credentials.getUser(), null, privateKey, true);
        } catch(IOException ex) {
            logger.warn("Can not get private key.");
            throw new IllegalStateException(ex);
        }
    }

    private ListenableFuture<ComputeServiceContext> createContext() {
        logger.debug("Creating Context...");

        Optional<Properties> overrides;
        if (isImageIdSet()) {
            //Optimization: lazy image fetching
            //set AMI queries to nothing
            Properties properties = new Properties();
            properties.setProperty(AWSEC2Constants.PROPERTY_EC2_AMI_QUERY, "");
            properties.setProperty(AWSEC2Constants.PROPERTY_EC2_CC_AMI_QUERY, "");
            overrides = Optional.of(properties);
        } else {
            overrides = Optional.absent();
        }
        return executorService.submit(
                new AmazonContextBuilder(accessKeyId, secretKey, overrides));

    }

    private void loadConfiguration(Configuration configuration) {
        logger.debug("Loading Configuration...");
        accessKeyId = configuration.getString("accessKeyId", "").trim();
        secretKey = configuration.getString("secretKey", "").trim();
        keyPair = configuration.getString("keyPair", "").trim();
        locationId = configuration.getString("locationId", "").trim();
        imageId = configuration.getString("imageId", "").trim();
    }

    private Template buildTemplate(ComputeServiceContext context) {
        AmazonTemplateBuilder templateBuilder;
        if (isImageIdSet()) {
            templateBuilder = new AmazonImageIdTemplateBuilder(context, imageId);
        } else {
            templateBuilder = new AmazonT1MicroTemplateBuilder(context, locationId);
        }
        return templateBuilder.buildTemplate();
    }

    private boolean isImageIdSet() {
        return (imageId != null && !imageId.isEmpty());
    }

    /**
     * Retrieves future value or throws IllegalStateException if the future
     * value can not be retrieved anymore.
     *
     * @return
     */
    private <T> T valueOrNotReady(Future<T> future) {
        try {
            return future.get();
        } catch (Exception ex) {
            throw new IllegalStateException("InstanceManager is not ready.", ex);
        }
    }
}
