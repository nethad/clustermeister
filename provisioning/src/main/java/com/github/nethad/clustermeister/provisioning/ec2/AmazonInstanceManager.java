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
import com.github.nethad.clustermeister.api.NodeConfiguration;
import com.google.common.collect.ImmutableSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jclouds.aws.ec2.reference.AWSEC2Constants;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.ec2.compute.options.EC2TemplateOptions;
import org.jclouds.enterprise.config.EnterpriseConfigurationModule;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.ssh.jsch.config.JschSshClientModule;
import org.jppf.management.JMXConnectionWrapper;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.JMXNodeConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas, daniel
 */
public class AmazonInstanceManager {
	static final String GROUP_NAME = "clustermeister";
	
	private final static Logger logger = 
			LoggerFactory.getLogger(AmazonInstanceManager.class);
	
    private String accessKeyId;
    private String secretKey;
    private String keyPair;
    private String locationId;
    private String imageId;
	
    ComputeServiceContext context;
    private Future<Template> templateFuture;
	
	private Configuration configuration;

	AmazonInstanceManager(Configuration config) {
		this.configuration = config;
	}
    
    void init() {
		logger.info("Loading Configuration...");
        loadConfiguration();
		logger.info("Creating Context...");
		
		Properties overrides = new Properties();
		if(isImageIdSet()) {
			//Optimization: lazy image fetching
			//set AMI queries to nothing
			overrides.setProperty(AWSEC2Constants.PROPERTY_EC2_AMI_QUERY, "");
			overrides.setProperty(AWSEC2Constants.PROPERTY_EC2_CC_AMI_QUERY, "");
		}
		
		context = new ComputeServiceContextFactory().
				createContext("aws-ec2", accessKeyId, secretKey, 
				ImmutableSet.of(new JschSshClientModule(), 
				new SLF4JLoggingModule(), new EnterpriseConfigurationModule()),
				overrides);
		
        buildTemplate();
	}
	
	void close() {
		if(context != null) {
			logger.info("Closing context...");
			context.close();
			logger.info("closed.");
		}
	}
	
	Iterator<? extends ComputeMetadata> getInstances() {
		return context.getComputeService().listNodes().iterator();
	}
	
	NodeMetadata getInstanceMetadata(String id) {
		return context.getComputeService().getNodeMetadata(id);
	}

    NodeMetadata createInstance(Map<String, String> userMetaData) {
		try {
			//wait for template building to be finished.
			Template template = templateFuture.get(2l, TimeUnit.MINUTES);
			
			if(userMetaData != null) {
				template.getOptions().userMetadata(userMetaData);
			}
			template.getOptions().as(EC2TemplateOptions.class).
					inboundPorts(22, 11111, 11112, 11113, 11198, 12198);

			// specify your own keypair for use in creating nodes
			template.getOptions().as(EC2TemplateOptions.class).keyPair(keyPair);
			Set<? extends NodeMetadata> instances = 
					context.getComputeService().createNodesInGroup(
						GROUP_NAME, 1, template);
			Iterator<? extends NodeMetadata> it = instances.iterator();
			if(it.hasNext()) {
				return instances.iterator().next();
			}
		} catch (RunNodesException ex) {
			logger.error("Instance could not be started.", ex);
		} catch(TimeoutException ex) {
			logger.error("Timed out while retrieving machine image.", ex);
		} catch(InterruptedException ex) {
			logger.error("Interrupted while retrieving machine image.", ex);
		} catch(ExecutionException ex) {
			logger.error("Could not retrieve machine imgae.", ex.getCause());
		}
		return null;
	}
	
	AmazonNode deploy(NodeMetadata instanceMetadata, NodeConfiguration nodeConfig) 
			throws TimeoutException, IllegalStateException {
		
		JMXConnectionWrapper wrapper;
		String publicIp = instanceMetadata.getPublicAddresses().iterator().next();
		switch(nodeConfig.getType()) {
			case NODE: {
				AmazonEC2JPPFDeployer deployer = 
						new AmazonEC2JPPFNodeDeployer(context, instanceMetadata, 
						getLoginCredentials(nodeConfig), nodeConfig.getDriverAddress());
				deployer.run();
				
				//TODO: management port should be dynamic to allow multiple nodes per instance.
				wrapper = new JMXNodeConnectionWrapper(publicIp, 11198);
				
				
				break;
			}
			case DRIVER: {
				AmazonEC2JPPFDeployer deployer = 
						new AmazonEC2JPPFDriverDeployer(context, instanceMetadata, 
						getLoginCredentials(nodeConfig));
				deployer.run();
				
				//TODO: management port should be dynamic to allow multiple nodes per instance.
				wrapper = new JMXDriverConnectionWrapper(publicIp, 11198);
				break;
			}
			default: {
				throw new IllegalArgumentException("Invalid Node Type.");
			}
		}
		
		logger.info("Trying to connect to Management...");
		wrapper.connectAndWait(5000);
		if(!wrapper.isConnected()) {
			/*
			 * Optimization: sometimes it seems the backoff is too large.
			 * Now: If timeout after 5 seconds. Try new connection with timeout 2 min.
			 * Good chance the new connection will succeed instantly or quicker 
			 * than waiting for long timeout on the first connection.
			 */
			wrapper.connectAndWait(180000); 
		}
		if(wrapper.isConnected()) {
			logger.info("Connected to Management.");
			return new AmazonNode(getUUID(wrapper), nodeConfig.getType(), instanceMetadata);
		} else {
			throw new TimeoutException("Timed out while for node management to become available.");
		}
	}

	void suspendInstance(String nodeId) {
		context.getComputeService().suspendNode(nodeId);
	}
	
	void terminateInstance(String nodeId) {
		context.getComputeService().destroyNode(nodeId);
	}
	
    void resumeInstance(String nodeId) {
		context.getComputeService().resumeNode(nodeId);
    }

	private LoginCredentials getLoginCredentials(NodeConfiguration config) {
		return new LoginCredentials(config.getUserName(), null, config.getPrivateKey(), true);
	}
	
    private void loadConfiguration() {
        accessKeyId = configuration.getString("accessKeyId", "").trim();
        secretKey = configuration.getString("secretKey", "").trim();
        keyPair = configuration.getString("keyPair", "").trim();
        locationId = configuration.getString("locationId", "").trim();
        imageId = configuration.getString("imageId", "").trim();
    }

    private void buildTemplate() {
		AmazonTemplateBuilder templateBuilder;
		if(isImageIdSet()) {
			templateBuilder = new AmazonImageIdTemplateBuilder(context, imageId);
		} else {
			templateBuilder = new AmazonT1MicroTemplateBuilder(context, locationId);
		}
		templateFuture = Executors.newSingleThreadExecutor().submit(templateBuilder);
    }
	
	private String getUUID(JMXConnectionWrapper wrapper) throws RuntimeException {
		String uuid;
		try {
			uuid = wrapper.systemInformation().getUuid().getProperty("jppf.uuid");
		} catch (Exception ex) {
			logger.error("Could not get UUID for {}", wrapper.getId());
			throw new IllegalStateException(ex);
		}
		logger.info("Got UUID: {}", uuid);
		return uuid;
	}
	
	private boolean isImageIdSet() {
		return (imageId != null && !imageId.isEmpty());
	}
}
