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
import com.github.nethad.clustermeister.api.NodeConfiguration;
import com.github.nethad.clustermeister.api.NodeType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;

/**
 *
 * @author daniel
 */
public class AmazonNodeManager {
	
	AmazonInstanceManager amazonInstanceManager;
	Configuration configuration;

	public AmazonNodeManager(Configuration configuration) {
		this.configuration = configuration;
		this.amazonInstanceManager = new AmazonInstanceManager(configuration);
		amazonInstanceManager.init();
	}
	
	public List<? extends Node> getNodes() {
		List<AmazonNode> nodes = new ArrayList<AmazonNode>();
		Iterator<? extends ComputeMetadata> it = amazonInstanceManager.getInstances();
		while (it.hasNext()) {
			final NodeMetadata metadata = 
					amazonInstanceManager.getInstanceMetadata(it.next().getId());
			for(Map.Entry<String, String> entry : 
					metadata.getUserMetadata().entrySet()) {
				if(entry.getKey().equalsIgnoreCase("jppf-node") && 
						entry.getValue().equalsIgnoreCase("true")) {
					nodes.add(getNode(NodeType.NODE, metadata));
				}
				if(entry.getKey().equalsIgnoreCase("jppf-driver") && 
						entry.getValue().equalsIgnoreCase("true")) {
					nodes.add(getNode(NodeType.DRIVER, metadata));
				}
			}
		}
		
		return nodes;
	}
	
	public Node addNode(NodeConfiguration nodeConfiguration) {
		Map<String, String> tags = new HashMap<String, String>();
		switch(nodeConfiguration.getType()) {
			case NODE: {
				tags.put("JPPF-node", "true");
				return createNewInstance(nodeConfiguration, tags);
			}
			case DRIVER:  {
				tags.put("JPPF-driver", "true");
				return createNewInstance(nodeConfiguration, tags);
			}
			default: {
				return null;
			}
		}
	}
	
	public void close() {
		if(amazonInstanceManager != null) {
			amazonInstanceManager.close();
		}
	}
	
	private Node createNewInstance(NodeConfiguration config, Map<String, String> tags) {
		NodeMetadata metadata = amazonInstanceManager.createInstance(tags);
		return getNode(config.getType(), metadata);
	}
	
	private AmazonNode getNode(NodeType type, NodeMetadata metadata) {
		AmazonNode node = new AmazonNode();
		node.setId(metadata.getId());
		node.setStatus(metadata.getState().toString());
		node.setType(type);
		node.setPrivateAddresses(metadata.getPrivateAddresses());
		node.setPublicAddresses(metadata.getPublicAddresses());
		node.setPort(-1);
		
		return node;
	}
}
