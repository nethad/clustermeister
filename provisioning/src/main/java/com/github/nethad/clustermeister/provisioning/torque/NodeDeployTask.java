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

import com.github.nethad.clustermeister.api.JPPFConstants;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFLocalDriver;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFNodeConfiguration;
import com.github.nethad.clustermeister.provisioning.utils.SSHClient;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientException;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.bind.DatatypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
class NodeDeployTask {
	
	private static final Logger logger = LoggerFactory.getLogger(NodeDeployTask.class);
	private int managementPort;
	private int nodeNumber;
	private int serverPort;
	private final TorqueNodeDeployment torqueNodeDeployment;
	private final TorqueNodeConfiguration nodeConfiguration;
    private final String email;
    private final String queueName;

	public NodeDeployTask(TorqueNodeDeployment torqueNodeDeployment, int nodeNumber, 
            TorqueNodeConfiguration nodeConfiguration, TorqueConfiguration torqueConfiguration) {
		this.torqueNodeDeployment = torqueNodeDeployment;
		this.nodeNumber = nodeNumber;
		this.nodeConfiguration = nodeConfiguration;
		this.managementPort = TorqueNodeDeployment.DEFAULT_MANAGEMENT_PORT + nodeNumber;
        this.email = torqueConfiguration.getEmailNotify();
        this.queueName = torqueConfiguration.getQueueName();
	}

	public TorqueNode execute() throws SSHClientException {
		String nodeNameBase = "CMNode" + torqueNodeDeployment.getSessionId();
		String nodeName = nodeNameBase + "_" + nodeNumber;
		String nodeConfigFileName = configFileName();
		uploadNodeConfiguration(nodeConfigFileName, driverAddress());
        
        final String qsubScript = qsubScript(nodeName, nodeConfigFileName, 
                nodeConfiguration.getNumberOfCpus());
        final String base64EncodedQsubScript = base64Encode(qsubScript);
        String submitJobToQsub = "echo \""+base64EncodedQsubScript+"\"| base64 -d | qsub";
		String response = sshClient().executeWithResult(submitJobToQsub);
        logger.info("Started node, response: {}", response);
		TorqueNode torqueNode = new TorqueNode(response, null, null, serverPort, managementPort);
		return torqueNode;
	}
	
    @VisibleForTesting
	void uploadNodeConfiguration(String nodeConfigFileName, String driverIpAddress) throws SSHClientException {
		try {
			JPPFNodeConfiguration configuration = createNodeConfiguration(driverIpAddress);
			final String configServerPort = configuration.getProperty(JPPFConstants.SERVER_PORT);
			if (configServerPort == null) {
				serverPort = JPPFLocalDriver.SERVER_PORT;
			} else {
				serverPort = Integer.valueOf(configServerPort);
			}
			InputStream propertyStream = configuration.getPropertyStream();
			sshClient().sftpUpload(propertyStream, TorqueNodeDeployment.DEPLOY_BASE_NAME + "/config/" + nodeConfigFileName);
		} catch (IOException ex) {
			logger.error(null, ex);
		}
	}

    JPPFNodeConfiguration createNodeConfiguration(String driverIpAddress) {
        JPPFNodeConfiguration configuration = new JPPFNodeConfiguration()
                .setProperty(JPPFConstants.SERVER_HOST, driverIpAddress)
                .setProperty(JPPFConstants.MANAGEMENT_PORT, String.valueOf(managementPort))
                .setProperty(JPPFConstants.RESOURCE_CACHE_DIR, "/tmp/.jppf/node-" + torqueNodeDeployment.getSessionId() + "_" + nodeNumber)
                .setProperty(JPPFConstants.PROCESSING_THREADS, String.valueOf(nodeConfiguration.getNumberOfCpus()));
        return configuration;
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
            logger.info("Using driver address found in node configuration.");
			return nodeConfigurationAddress;
		} else if (torqueNodeDeploymentAddress != null) {
            logger.info("Using driver address found in torque node deployer.");
			return torqueNodeDeploymentAddress;
		} else {
            logger.info("Using driver address fallback 'localhost'.");
			logger.warn("Could not find driver IP address, using localhost");
			return "localhost";
		}
	}

    public String base64Encode(String toEncode) {
        return DatatypeConverter.printBase64Binary(toEncode.getBytes());
    }

    @VisibleForTesting
    String qsubScript(String nodeName, String nodeConfigFileName, int numberOfCpus) {
        StringBuilder sb = new StringBuilder();
        sb.append("#PBS -N ").append(nodeName).append("\n") // node name
            .append("#PBS -l nodes=1:ppn=").append(numberOfCpus).append("\n") // number of nodes, processors per node
            .append("#PBS -q ").append(queueName).append("\n") // queue name
            .append("#PBS -p 0\n") // priority, default: 0
            .append("#PBS -j oe\n") // join (o)output and (e)rror stream
            .append("#PBS -m b\n") // mail option: mail is sent when the job begins execution.
            .append("#PBS -m e\n") // mail option: mail is sent when the job terminates.
            .append("#PBS -m a\n") // mail option: mail is sent when the job is aborted by the batch system.
            .append("#PBS -V\n") // export environment variables to batch job
            .append("#PBS -o out/").append(nodeName).append(".out\n") // path to STDOUT file log
            .append("#PBS -e err/").append(nodeName).append(".err\n"); // path to STDERR file log
        if (isValidEmail(email)) {
            sb.append("#PBS -M ").append(email).append("\n"); // email address
        }
        // start script
            sb.append("\n")
            .append("workingDir=/home/torque/tmp/${USER}.${PBS_JOBID}\n")
            .append("cp -r ~/jppf-node $workingDir/jppf-node\n")
            .append("cd $workingDir/jppf-node\n")
            .append("chmod +x startNode.sh\n")
            .append("./startNode.sh ").append(nodeConfigFileName).append("\n");
        return sb.toString();
    }

    @VisibleForTesting
    boolean isValidEmail(String email) {
        return (email != null && email.matches(".*@.*\\..*"));
    }
	
}
