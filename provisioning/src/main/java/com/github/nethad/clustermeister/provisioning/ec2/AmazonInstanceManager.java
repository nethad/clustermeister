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
import com.github.nethad.clustermeister.provisioning.ec2.AmazonEC2JPPFDeployer.Event;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientImpl;
import com.github.nethad.clustermeister.provisioning.utils.SocksTunnel;
import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Monitor;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
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
    private final ContextManager contextManager;
    private final AwsEc2Facade ec2Facade;
    private final Monitor portCounterMonitor = new Monitor(false);
    private final Map<String, Integer> instanceToPortCounter =
            new HashMap<String, Integer>();
    
    //TODO: make sure this does not cause memory leak
    private final Monitor reverseTunnelMonitor = new Monitor(false);
    private final Map<String, SocksTunnel> instanceToReverseTunnel =
            new HashMap<String, SocksTunnel>();
    private final Collection<File> artifactsToPreload;
    private final Map<String, AWSInstanceProfile> profiles;

    
     /**
      * Creates a new AmazonInstanceManager.
      *
      * This class is designed to be used by the AmazonNodeManager and each
      * AmazonNodeManager should use only a single instance.
      *
      */
    AmazonInstanceManager(ContextManager contextManager, 
            AwsEc2Facade ec2Facade, 
            Map<String, AWSInstanceProfile> profiles, 
            Collection<File> artifactsToPreload) {
        this.contextManager = contextManager;
        this.ec2Facade = ec2Facade;
        this.profiles = profiles;
        this.artifactsToPreload = artifactsToPreload;
    }
    
    /**
     * Release resources used by the instance manager.
     */
    void close() {
        //TODO: release resources?
    }

    public Collection<AWSInstanceProfile> getConfiguredProfiles() {
        return ImmutableSet.copyOf(profiles.values());
    }
    
    public AWSInstanceProfile getConfiguredProfile(String profileName) {
        return profiles.get(profileName);
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
        ComputeService computeService = 
                contextManager.getEagerContext().getComputeService();
        Template template = nodeConfiguration.getTemplate(computeService.templateBuilder());

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
        
        Set<? extends NodeMetadata> instances = computeService.
                createNodesInGroup(GROUP_NAME, 1, template);

        NodeMetadata metadata = Iterables.getOnlyElement(instances);
        if(!nodeConfiguration.getCredentials().isPresent()) {
            AmazonGeneratedKeyPairCredentials credentials = 
                    new AmazonGeneratedKeyPairCredentials(
                        String.format("node#%s", metadata.getId()), 
                        metadata.getCredentials().getUser(), 
                        metadata.getCredentials().getPrivateKey());
            nodeConfiguration.setCredentials(credentials);
        }
        logger.info("Instance {} created.", metadata.getId());
        return metadata;
    }
    
    /**
     * Suspend (stop) an instance.
     *
     * Shuts down the instance but the instance stays available for resuming.
     *
     * @param instanceId	jClouds node ID.
     */
    void suspendInstance(String instanceId) {
        ec2Facade.suspendInstance(instanceId);
        decrementAndManagePortCounter(instanceId);
        removeSocksTunnel(instanceId);
        AmazonEC2JPPFDeployer.removeDriverMonitor(instanceId);
        AmazonEC2JPPFDeployer.removeNodeMonitor(instanceId);
    }

    /**
     * Terminate (destroy) an instance.
     *
     * Shuts down and discards the instance.
     *
     * @param instanceId	jClouds node ID.
     */
    void terminateInstance(String instanceId) {
        ec2Facade.terminateInstance(instanceId);
        decrementAndManagePortCounter(instanceId);
        removeSocksTunnel(instanceId);
        AmazonEC2JPPFDeployer.removeDriverMonitor(instanceId);
        AmazonEC2JPPFDeployer.removeNodeMonitor(instanceId);
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

        ComputeServiceContext context = contextManager.getEagerContext();

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
                        KeyPairCredentials keypair = credentials.as(KeyPairCredentials.class);
                        sshClientForReversePort.addIdentity(instanceMetadata.getId(), 
                                keypair.getPrivateKey().
                                getBytes(keypair.getKeySourceCharset()));
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
                        throw new IllegalStateException(
                                String.format("Unsupported Credentials: %s.", credentials));
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
            //amazon will generate its own keypair.
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
}
