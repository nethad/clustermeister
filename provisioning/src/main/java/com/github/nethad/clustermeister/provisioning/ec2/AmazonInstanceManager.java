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
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
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
	
    private final ListenableFuture<ComputeServiceContext> contextFuture;
    private final SettableFuture<Template> templateFuture;
	private final ListeningExecutorService executorService;

	AmazonInstanceManager(Configuration config, ListeningExecutorService executorService) {
		loadConfiguration(config);
		this.executorService = executorService;
		templateFuture = SettableFuture.create();
		contextFuture = createContext();
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
    
	void close() {
		try {
			ComputeServiceContext context = contextFuture.get();
			logger.info("Closing context...");
			context.close();
			logger.info("Context Closed.");
		} catch (Exception ex) {
			//do nothing
		}
	}
	
	Iterator<? extends ComputeMetadata> getInstances() {
		try {
			return contextFuture.get().getComputeService().listNodes().iterator();
		} catch (Exception ex) {
			return Collections.EMPTY_SET.iterator();
		}
	}
	
	NodeMetadata getInstanceMetadata(String id) {
		return valueOrNotReady(contextFuture).getComputeService().getNodeMetadata(id);
	}

    NodeMetadata createInstance(Map<String, String> userMetaData) throws RunNodesException {
		ComputeServiceContext context = valueOrNotReady(contextFuture);
		Template template = valueOrNotReady(templateFuture);

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
		
		return Iterables.getOnlyElement(instances);
	}
	
	AmazonNode deploy(NodeMetadata instanceMetadata, NodeConfiguration nodeConfig) 
			throws TimeoutException {
		
		ComputeServiceContext context = valueOrNotReady(contextFuture);
		
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
		valueOrNotReady(contextFuture).getComputeService().suspendNode(nodeId);
	}
	
	void terminateInstance(String nodeId) {
		valueOrNotReady(contextFuture).getComputeService().destroyNode(nodeId);
	}
	
    void resumeInstance(String nodeId) {
		valueOrNotReady(contextFuture).getComputeService().resumeNode(nodeId);
    }

	private LoginCredentials getLoginCredentials(NodeConfiguration config) {
		return new LoginCredentials(config.getUserName(), null, config.getPrivateKey(), true);
	}
	
	private ListenableFuture<ComputeServiceContext> createContext() {
		logger.info("Creating Context...");
		
		Optional<Properties> overrides;
		if(isImageIdSet()) {
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
		logger.info("Loading Configuration...");
        accessKeyId = configuration.getString("accessKeyId", "").trim();
        secretKey = configuration.getString("secretKey", "").trim();
        keyPair = configuration.getString("keyPair", "").trim();
        locationId = configuration.getString("locationId", "").trim();
        imageId = configuration.getString("imageId", "").trim();
    }

    private Template buildTemplate(ComputeServiceContext context) {
		AmazonTemplateBuilder templateBuilder;
		if(isImageIdSet()) {
			templateBuilder = new AmazonImageIdTemplateBuilder(context, imageId);
		} else {
			templateBuilder = new AmazonT1MicroTemplateBuilder(context, locationId);
		}
		return templateBuilder.buildTemplate();
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
	
	/**
	 * Retrieves future value or throws IllegalStateException if
	 * the future value can not be retrieved anymore.
	 * 
	 * @return 
	 */
	private <T> T valueOrNotReady(Future<T> future) {
		try {
			return future.get();
		} catch (Exception ex) {
			throw new IllegalStateException("InstanceManager is not ready.");
		}
	}
}
