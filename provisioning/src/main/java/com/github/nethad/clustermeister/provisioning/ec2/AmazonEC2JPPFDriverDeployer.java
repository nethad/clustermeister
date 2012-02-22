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

import java.util.Properties;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.ssh.SshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
public class AmazonEC2JPPFDriverDeployer extends AmazonEC2JPPFDeployer {

	private final static Logger logger = 
			LoggerFactory.getLogger(AmazonEC2JPPFDriverDeployer.class);
	
	public AmazonEC2JPPFDriverDeployer(ComputeServiceContext context, 
			NodeMetadata metadata, LoginCredentials credentials) {
		super(credentials, context, metadata);
	}
	
	@Override
	public void run() {
		final String publicIp = metadata.getPublicAddresses().iterator().next();
		final String privateIp = metadata.getPrivateAddresses().iterator().next();
		logger.info("Deploying JPPF-Driver to {} ({}).", metadata.getId(), publicIp);
		Properties nodeProperties = getSettings(
				privateIp);
		
		SshClient client = context.utils().sshForNode().apply(
				NodeMetadataBuilder.fromNodeMetadata(metadata).
				credentials(loginCredentials).build());
		client.connect();
		try {
			execute("rm -rf jppf-driver*", client);
			upload(client, getClass().getResourceAsStream("jppf-driver.zip"), 
					"/home/ec2-user/jppf-driver.zip");
			execute("unzip jppf-driver.zip", client);
			execute("chmod +x jppf-driver/startDriver.sh", client);
			upload(client, getRunningConfig(nodeProperties), 
					"jppf-driver/config/jppf-driver.properties");
			
			logger.info("Starting JPPF-Driver on {}...", metadata.getId());
			final String script = "cd /home/ec2-user/jppf-driver\nnohup ./startDriver.sh > nohup.out 2>&1";
			RunScriptOptions options = new RunScriptOptions().
					overrideLoginPrivateKey(loginCredentials.getPrivateKey()).
					overrideLoginUser(loginCredentials.getUser()).
					blockOnComplete(false).
					runAsRoot(false).
					nameTask("jppf-driver-start");
			logExecResponse(context.getComputeService().
					runScriptOnNode(metadata.getId(), script, options));
			logger.info("JPPF-Driver started.");
		} finally {
			if (client != null) {
				client.disconnect();
			}
		}
		logger.info("JPPF-Driver deployed on {}.", metadata.getId());
	}
	
	private Properties getSettings(String managementHost) {
		Properties nodeProperties = getPropertiesFromStream(
				this.getClass().getResourceAsStream("jppf-driver.properties"));
		nodeProperties.setProperty("jppf.management.host", managementHost);
		return nodeProperties;
	}
}