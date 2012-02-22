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
import java.util.Set;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.compute.domain.Template;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.ec2.compute.options.EC2TemplateOptions;
import org.jclouds.ec2.domain.InstanceType;
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
	
    ComputeServiceContext context;
    private Template template;
	
	private Configuration configuration;

	AmazonInstanceManager(Configuration config) {
		this.configuration = config;
	}
    
    void init() {
		logger.info("Loading Configuration...");
        loadConfiguration();
		logger.info("Creating Context...");
		context = new ComputeServiceContextFactory().
				createContext("aws-ec2", accessKeyId, secretKey, 
				ImmutableSet.of(new JschSshClientModule(), 
				new SLF4JLoggingModule(), new EnterpriseConfigurationModule()));
        buildTemplate();
		logger.info("Initialization complete.");
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
			if(userMetaData != null) {
				template.getOptions().userMetadata(userMetaData);
			}
			Set<? extends NodeMetadata> instances = 
					context.getComputeService().createNodesInGroup(
						GROUP_NAME, 1, template);
			Iterator<? extends NodeMetadata> it = instances.iterator();
			if(it.hasNext()) {
				return instances.iterator().next();
			}
		} catch (RunNodesException ex) {
			logger.error("Exception while creating new instance.", ex);
		}
		return null;
	}
	
	AmazonNode deploy(NodeMetadata instanceMetadata, NodeConfiguration nodeConfig) {
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
		wrapper.connectAndWait(100);
		while (!wrapper.isConnected()) {
			wrapper.connectAndWait(100);
		}
		logger.info("Connected to Management.");
		
		String uuid;
		try {
			uuid = wrapper.systemInformation().getUuid().getProperty("jppf.uuid");
		} catch (Exception ex) {
			logger.error("Could not get UUID for {}", wrapper.getId());
			throw new RuntimeException(ex);
		}
		logger.info("Got UUID: {}", uuid);
		
		return new AmazonNode(uuid, nodeConfig.getType(), instanceMetadata);
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
        accessKeyId = configuration.getString("accessKeyId", "");
        secretKey = configuration.getString("secretKey", "");
        keyPair = configuration.getString("keyPair", "");
        locationId = configuration.getString("locationId", "");
    }


    private void buildTemplate() {
		logger.info("Building Template...");
		template = context.getComputeService()
							.templateBuilder()
								.locationId(locationId)
								.hardwareId(InstanceType.T1_MICRO)
								.osFamily(OsFamily.AMZN_LINUX).build();

		template.getOptions().as(EC2TemplateOptions.class).
				inboundPorts(22, 11111, 11112, 11113, 11198, 12198);

		// specify your own keypair for use in creating nodes
		template.getOptions().as(EC2TemplateOptions.class).keyPair(keyPair);
		logger.info("Template built.");
    }
}
