/*
 * Copyright 2012 Daniel Spicar.
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

import com.github.nethad.clustermeister.provisioning.utils.SSHClient;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientExcpetion;

/**
 *
 * @author daniel
 */
public class AmazonEC2JPPFDriverDeployer {
	
	public static void main(String... args) {
		SSHClient sshUtil = null;
		try {
			String privateKeyFile = "/home/daniel/Desktop/EC2/EC2_keypair.pem";
			String userName = "ec2-user";
			String host = "ec2-176-34-200-11.eu-west-1.compute.amazonaws.com";
			int port = 22;
			
			sshUtil = new SSHClient(privateKeyFile);
			sshUtil.connect(userName, host, port);
//			sshUtil.sftpUpload(AmazonEC2JPPFDriverDeployer.class.getResource("jppf-driver").getPath(), "jppf-driver");
			String result = sshUtil.sshExec("echo \"Hello World!\"", System.err);
			System.out.println("Result: " + result);
//			sshUtil.sftpUpload(AmazonEC2JPPFDriverDeployer.class.getResource("jppf-driver.properties").getPath(), "jppf-driver/config/jppf-driver.properties");
		} catch (SSHClientExcpetion ex) {
			ex.printStackTrace();
		} finally {
			if(sshUtil != null) {
				sshUtil.disconnect();
			}
		}
	}
}