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
package com.github.nethad.clustermeister.provisioning.torque;

import com.github.nethad.clustermeister.api.NodeConfiguration;
import com.github.nethad.clustermeister.api.NodeType;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFNodeConfiguration;
import com.github.nethad.clustermeister.provisioning.utils.SSHClient;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientExcpetion;
import com.jcraft.jsch.SftpException;
import groovy.lang.Lazy;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
class NodeDeployTask {
	
	private Logger logger = LoggerFactory.getLogger(NodeDeployTask.class);
	
//	public static final int DEFAULT_MANAGEMENT_PORT = 11198;
//	private static final String DEPLOY_BASE_NAME = "jppf-node";
//	private static final String DEPLOY_CONFIG_SUFFIX = ".properties";
//	private static final String PATH_TO_QSUB_SCRIPT = "./" + DEPLOY_BASE_NAME + "/" + DEPLOY_QSUB;
	
	private int nodeNumber;
//	private final SSHClient sshClient;
	private final TorqueNodeDeployment torqueNodeDeployment;
	private final NodeConfiguration nodeConfiguration;

	public NodeDeployTask(TorqueNodeDeployment torqueNodeDeployment, int nodeNumber, NodeConfiguration nodeConfiguration) {
		this.torqueNodeDeployment = torqueNodeDeployment;
		this.nodeNumber = nodeNumber;
		this.nodeConfiguration = nodeConfiguration;
	}

	public TorqueNode execute() throws SSHClientExcpetion {
		String nodeNameBase = "CMNode" + torqueNodeDeployment.getSessionId();
		String nodeName = nodeNameBase + "_" + nodeNumber;
		String nodeConfigFileName = configFileName();
		uploadNodeConfiguration(nodeConfigFileName, driverAddress());
		
		final String submitJobToQsub = TorqueNodeDeployment.PATH_TO_QSUB_SCRIPT + " " + nodeName + " " + nodeConfigFileName + "|qsub";
		String jobId = sshClient().executeWithResult(submitJobToQsub);
//		outer.jobIdList.add(currentJobId);
		TorqueNode torqueNode = new TorqueNode(NodeType.NODE, jobId);
		
		return torqueNode;
	}
	
	private void uploadNodeConfiguration(String nodeConfigFileName, String driverIpAddress) throws SSHClientExcpetion {

		// generate properties file from configuration class and attach
		// the local ip address as the driver's IP target address.

		int managementPort = TorqueNodeDeployment.DEFAULT_MANAGEMENT_PORT + nodeNumber;
		try {
			InputStream propertyStream = new JPPFNodeConfiguration()
					.setProperty("jppf.server.host", driverIpAddress)
					.setProperty("jppf.management.port", String.valueOf(managementPort))
					.setProperty("jppf.resource.cache.dir", "/tmp/.jppf/node-" + torqueNodeDeployment.getSessionId() + "_" + nodeNumber)
					.getPropertyStream();
			sshClient().sftpUpload(propertyStream, TorqueNodeDeployment.DEPLOY_BASE_NAME + "/config/" + nodeConfigFileName);
		} catch (IOException ex) {
			logger.error(null, ex);
		}
	}
		
	private String configFileName() {
		return TorqueNodeDeployment.DEPLOY_BASE_NAME + "-" + nodeNumber + TorqueNodeDeployment.DEPLOY_CONFIG_SUFFIX;
	}
	
	private SSHClient sshClient() {
		return torqueNodeDeployment.sshClient();
	}
	
	private String driverAddress() {
		String nodeConfigurationAddress = nodeConfiguration.getDriverAddress();
		String torqueNodeDeploymentAddress = this.torqueNodeDeployment.getDriverAddress();
		if (nodeConfigurationAddress != null) {
			return nodeConfigurationAddress;
		} else if (torqueNodeDeploymentAddress != null) {
			return torqueNodeDeploymentAddress;
		} else {
			logger.warn("Could not find driver IP address, using localhost");
			return "localhost";
		}
		
	}
	
}
