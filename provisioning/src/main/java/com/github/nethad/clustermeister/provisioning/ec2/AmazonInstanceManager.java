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

import com.github.nethad.clustermeister.api.Credentials;
import com.github.nethad.clustermeister.api.JPPFConstants;
import com.github.nethad.clustermeister.api.impl.AmazonConfiguredKeyPairCredentials;
import com.github.nethad.clustermeister.api.impl.KeyPairCredentials;
import com.github.nethad.clustermeister.api.impl.PasswordCredentials;
import com.github.nethad.clustermeister.provisioning.dependencymanager.DependencyConfigurationUtil;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonEC2JPPFDeployer.Event;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientImpl;
import com.github.nethad.clustermeister.provisioning.utils.SocksTunnel;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Monitor;
import com.google.common.util.concurrent.SettableFuture;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.apache.commons.configuration.Configuration;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
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
    private Collection<File> artifactsToPreload = null;
    private final ListenableFuture<ComputeServiceContext> contextFuture;
    private final SettableFuture<TemplateBuilder> templateBuilderFuture;
    private final ListeningExecutorService executorService;
    private final Monitor portCounterMonitor = new Monitor(false);
    private final Map<String, Integer> instanceToPortCounter =
            new HashMap<String, Integer>();
    
    //TODO: make sure this does not cause memory leak
    private final Monitor reverseTunnelMonitor = new Monitor(false);
    private final Map<String, SocksTunnel> instanceToReverseTunnel =
            new HashMap<String, SocksTunnel>();

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
        templateBuilderFuture = SettableFuture.create();
        //from here the configuration must be loaded.
        contextFuture = createContext();
        Futures.addCallback(contextFuture, new FutureCallback<ComputeServiceContext>() {

            @Override
            public void onSuccess(ComputeServiceContext context) {
                templateBuilderFuture.set(context.getComputeService().templateBuilder());
            }

            @Override
            public void onFailure(Throwable t) {
                templateBuilderFuture.setException(t);
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
            logger.warn("Failed to close {}", getClass().getSimpleName());
        }
    }

    /**
     * Performs an Amazon API call to retrieve all AWS EC2 instances.
     *
     * @return A set containing all registered AWS EC2 instances regardless
     * of state.
     */
    public Set<? extends ComputeMetadata> getInstances() {
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
    NodeMetadata createInstance(AmazonNodeConfiguration nodeConfiguration, 
            Optional<Map<String, String>> userMetaData) throws RunNodesException {
        logger.info("Creating a new instance...");
        ComputeServiceContext context = valueOrNotReady(contextFuture);
        Template template = nodeConfiguration.getTemplate(
                valueOrNotReady(templateBuilderFuture));

        if (userMetaData.isPresent()) {
            template.getOptions().userMetadata(userMetaData.get());
        }
        template.getOptions().
                inboundPorts(
                AmazonNodeManager.DEFAULT_SSH_PORT, 
                JPPFConstants.DEFAULT_SERVER_PORT, 
                JPPFConstants.DEFAULT_MANAGEMENT_PORT,
                JPPFConstants.DEFAULT_MANAGEMENT_PORT + 1,
                JPPFConstants.DEFAULT_MANAGEMENT_RMI_PORT);
        
        setLoginCredentials(template, nodeConfiguration);
        
        Set<? extends NodeMetadata> instances = context.getComputeService().
                createNodesInGroup(GROUP_NAME, 1, template);

        NodeMetadata metadata = Iterables.getOnlyElement(instances);
        if(!nodeConfiguration.getCredentials().isPresent()) {
            //TODO: generated credentials need to be persited somehow for re-use
            nodeConfiguration.setCredentials(new AmazonGeneratedKeyPairCredentials(
                    AmazonConfiguredKeyPairCredentials.DEFAULT_USER, 
                    metadata.getCredentials().getPrivateKey()));
        }
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
    AmazonNode deploy(final NodeMetadata instanceMetadata, final AmazonNodeConfiguration nodeConfig)
            throws TimeoutException {

        ComputeServiceContext context = valueOrNotReady(contextFuture);

        nodeConfig.setArtifactsToPreload(artifactsToPreload);
        
        int managementPort;
        String uuid = null;
        switch (nodeConfig.getType()) {
            case NODE: {
                managementPort = getNextNodeManagementPort(instanceMetadata);
                nodeConfig.setManagementPort(managementPort);
                AmazonEC2JPPFDeployer deployer =
                        new AmazonEC2JPPFNodeDeployer(context, instanceMetadata,
                        buildLoginCredentials(nodeConfig), nodeConfig);
                Observer sshConnectionCallback = new Observer() {
                    @Override
                    public void update(Observable arg0, Object event) {
                        if(event == Event.DEPLOYMENT_PREPARED) {
                            openReverseChannel(instanceMetadata, nodeConfig);
                        }
                    }
                };
                deployer.addObserver(sshConnectionCallback);
                uuid = deployer.deploy();
                deployer.deleteObserver(sshConnectionCallback);
                break;
            }
            case DRIVER: {
                managementPort = JPPFConstants.DEFAULT_MANAGEMENT_PORT;
                nodeConfig.setManagementPort(managementPort);
                AmazonEC2JPPFDeployer deployer =
                        new AmazonEC2JPPFDriverDeployer(context, instanceMetadata,
                        buildLoginCredentials(nodeConfig), nodeConfig);
                uuid = deployer.deploy();

                break;
            }
            default: {
                throw new IllegalArgumentException("Invalid Node Type.");
            }
        }

        checkState(uuid != null && !uuid.isEmpty());
        AmazonNode node = new AmazonNode(uuid, nodeConfig, instanceMetadata);
        return node;
    }

    private void openReverseChannel(NodeMetadata instanceMetadata, AmazonNodeConfiguration nodeConfig) {
        reverseTunnelMonitor.enter();
        try {
            if(!instanceToReverseTunnel.containsKey(instanceMetadata.getId())) {
                SSHClientImpl sshClientForReversePort = new SSHClientImpl();
                Credentials credentials = nodeConfig.getCredentials().get();
                try {
                    if(credentials instanceof KeyPairCredentials) {
                        sshClientForReversePort.addIdentity(instanceMetadata.getId(), 
                                credentials.as(KeyPairCredentials.class).getPrivateKey().
                                getBytes(Charsets.UTF_8));
                        String publicIp = Iterables.getFirst(instanceMetadata.getPublicAddresses(), null);
                        sshClientForReversePort.connect(credentials.getUser(), publicIp, 
                                instanceMetadata.getLoginPort());
                        SocksTunnel socksReverseTunnel = sshClientForReversePort.getSocksReverseTunnel();
                        instanceToReverseTunnel.put(instanceMetadata.getId(), socksReverseTunnel);
                        socksReverseTunnel.openTunnel(
                                JPPFConstants.DEFAULT_SERVER_PORT, "localhost", 
                                JPPFConstants.DEFAULT_SERVER_PORT);
                    } else {
                        //TODO: add support for password credentials
                        throw new IllegalStateException("Unsupported Credentials.");
                    }
                } catch (Exception ex) {
                    logger.warn("Could not open reverse channel.", ex);
                }
            }
        } finally {
            reverseTunnelMonitor.leave();
        }
    }

    private int getNextNodeManagementPort(NodeMetadata instanceMetadata) {
        portCounterMonitor.enter();
        try {
            Integer portCounter = instanceToPortCounter.get(instanceMetadata.getId());
            if (portCounter == null) {
                portCounter = new Integer(JPPFConstants.DEFAULT_MANAGEMENT_PORT + 1);
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
                if (portCounter <= JPPFConstants.DEFAULT_MANAGEMENT_PORT) {
                    instanceToPortCounter.remove(instanceId);
                }
            }
        } finally {
            portCounterMonitor.leave();
        }
    }
    
    private void removeSocksTunnel(String instanceId) {
        reverseTunnelMonitor.enter();
        try {
            SocksTunnel tunnel = instanceToReverseTunnel.remove(instanceId);
            if(tunnel != null) {
                tunnel.closeTunnel();
                tunnel.getSshClient().disconnect();
            }
        } finally {
            reverseTunnelMonitor.leave();
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
        removeSocksTunnel(instanceId);
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
        removeSocksTunnel(instanceId);
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
    
    private void setLoginCredentials(Template template, AmazonNodeConfiguration nodeConfiguration) {
        final EC2TemplateOptions awsOptions = 
                template.getOptions().as(EC2TemplateOptions.class);
        if(nodeConfiguration.getCredentials().isPresent()) {
            Credentials credentials = nodeConfiguration.getCredentials().get();
            if(credentials instanceof AmazonConfiguredKeyPairCredentials) {
                awsOptions.keyPair(credentials.as(AmazonConfiguredKeyPairCredentials.class).
                        getAmazonKeyPairName());
            } else if(credentials instanceof KeyPairCredentials){
                KeyPairCredentials keyPairCredentials = credentials.as(KeyPairCredentials.class);
                try {
                    Optional<String> publicKey = keyPairCredentials.getPublicKey();
                    if(publicKey.isPresent()) {
                        awsOptions.authorizePublicKey(publicKey.get());
                    } else {
                        awsOptions.dontAuthorizePublicKey();
                    }
                } catch(IOException ex) {
                    logger.warn("Can not authorize public key.", ex);
                }
                awsOptions.overrideLoginCredentials(buildLoginCredentials(nodeConfiguration));
            } else {
                awsOptions.overrideLoginCredentials(buildLoginCredentials(nodeConfiguration));
            }
        } else {
            //TODO: ???
        }
    }

    private LoginCredentials buildLoginCredentials(AmazonNodeConfiguration config) {
        Credentials credentials = config.getCredentials().get();
        if(credentials instanceof KeyPairCredentials) {
            try {
                String privateKey = credentials.as(KeyPairCredentials.class).getPrivateKey();
                return new LoginCredentials(credentials.getUser(), null, privateKey, true);
            } catch(IOException ex) {
                logger.warn("Can not get private key.");
                throw new IllegalStateException("Can not get private key.", ex);
            }
        } else if(credentials instanceof PasswordCredentials) {
                String password = credentials.as(PasswordCredentials.class).getPassword();
            return new LoginCredentials(credentials.getUser(), password, null, true);
        } else {
            throw new IllegalStateException("Unsupported credentials.");
        }
    }

    private ListenableFuture<ComputeServiceContext> createContext() {
        logger.debug("Creating Context...");

        //TODO: how to enable lazy image fetching?
//        //Optimization: lazy image fetching
//        //set AMI queries to nothing
//        Properties properties = new Properties();
//        properties.setProperty(AWSEC2Constants.PROPERTY_EC2_AMI_QUERY, "");
//        properties.setProperty(AWSEC2Constants.PROPERTY_EC2_CC_AMI_QUERY, "");
        return executorService.submit(
                new AmazonContextBuilder(accessKeyId, secretKey, 
                Optional.<Properties>absent()));

    }

    private void loadConfiguration(Configuration configuration) {
        logger.debug("Loading Configuration...");
        accessKeyId = checkNotNull(
                configuration.getString("amazon.accessKeyId", null), 
                "No Amazon access key ID configured.").trim();
        secretKey = checkNotNull(
                configuration.getString("amazon.secretKey", null), 
                "No Amazon secret key configured.").trim();
        artifactsToPreload = DependencyConfigurationUtil.getConfiguredDependencies(configuration);
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
