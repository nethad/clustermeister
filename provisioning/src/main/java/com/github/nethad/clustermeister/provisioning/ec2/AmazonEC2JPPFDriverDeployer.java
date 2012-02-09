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

import com.github.nethad.clustermeister.provisioning.AmazonAPIManageNodes;
import com.github.nethad.clustermeister.provisioning.FileConfiguration;
import com.github.nethad.clustermeister.provisioning.utils.SSHClient;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientExcpetion;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.net.IPSocket;
import org.jclouds.predicates.InetSocketAddressConnect;
import org.jclouds.predicates.RetryablePredicate;

/**
 *
 * @author daniel
 */
public class AmazonEC2JPPFDriverDeployer {

	public static void main(String... args) throws FileNotFoundException, IOException, TimeoutException {

		FileConfiguration config = new FileConfiguration("/home/daniel/clustermeister-amazonapi.properties");
		AmazonAPIManageNodes nodeManager = new AmazonAPIManageNodes(config);

		nodeManager.init();
		NodeMetadata metadata = nodeManager.getContext().getComputeService().
				getNodeMetadata("eu-west-1/i-28b4f361");
		String publicIP = metadata.getPublicAddresses().iterator().next();


		RetryablePredicate<IPSocket> socketTester =
				new RetryablePredicate<IPSocket>(
				new InetSocketAddressConnect(), 300, 1, TimeUnit.SECONDS);
		System.out.printf("%d: %s awaiting ssh service to start%n", System.currentTimeMillis(), publicIP);
		if (!socketTester.apply(new IPSocket(publicIP, 22))) {
			throw new TimeoutException("timeout waiting for ssh to start: " + publicIP);
		}

		System.out.printf("%d: %s ssh service started%n", System.currentTimeMillis(), publicIP);

//		BufferedReader reader = new BufferedReader(new FileReader("/home/daniel/Desktop/EC2/EC2_keypair.pem"));
//		StringBuilder sb = new StringBuilder();
//
//		String line;
//		while ((line = reader.readLine()) != null) {
//			sb.append(line);
//			sb.append("\n");
//		}
//		reader.close();

//		System.out.println(sb.toString().trim());

//		SshClient client = nodeManager.getContext().utils().sshForNode().apply(
//				NodeMetadataBuilder.fromNodeMetadata(metadata).credentials(
//                       new LoginCredentials("ec2-user", null, sb.toString().trim(), true)).build());
//		try {
//			InputStream is = AmazonEC2JPPFDriverDeployer.class.getResourceAsStream("jppf-driver.zip");
//			client.connect();
//			client.put("/home/ec2-user/", Payloads.newInputStreamPayload(is));
//			if(is != null) {
//				try {
//					is.close();
//				} catch (IOException ex) {
//					//ignore
//				}
//			}
//			ExecResponse res = client.exec("rm -rf jppf-driver*");
//			System.out.println("Result: " + res.toString());
//			res = client.exec("unzip jppf-driver.zip");
//			System.out.println("Result: " + res.toString());
//			is = AmazonEC2JPPFDriverDeployer.class.getResourceAsStream("jppf-driver.properties");
//			client.put("jppf-driver/config/", Payloads.newInputStreamPayload(is));
//			if(is != null) {
//				try {
//					is.close();
//				} catch (IOException ex) {
//					//ignore
//				}
//			}
//			res = client.exec("nohup jppf-driver/startDriver.sh &");
//			System.out.println("Result: " + res.toString());
//		} finally {
//			if (client != null) {
//				client.disconnect();
//			}
//		}


		SSHClient sshUtil = null;
		try {
			String privateKeyFile = "/home/daniel/Desktop/EC2/EC2_keypair.pem";
			String userName = "ec2-user";
//			String host = "ec2-176-34-200-11.eu-west-1.compute.amazonaws.com";
			int port = 22;
			
			
			sshUtil = new SSHClient(privateKeyFile);
			sshUtil.connect(userName, publicIP, port);
			String result = sshUtil.sshExec("rm -rf jppf-driver*", System.err);
			System.out.println("Result: " + result);
			sshUtil.sftpUpload(AmazonEC2JPPFDriverDeployer.class.getResource("jppf-driver.zip").getPath(), "jppf-driver.zip");
//			sshUtil.sftpUpload(AmazonEC2JPPFDriverDeployer.class.getResource("jppf-driver").getPath(), "jppf-driver");
			result = sshUtil.sshExec("unzip jppf-driver.zip", System.err);
			System.out.println("Result: " + result);
			sshUtil.sftpUpload(AmazonEC2JPPFDriverDeployer.class.getResource("jppf-driver.properties").getPath(), "jppf-driver/config/jppf-driver.properties");
			result = sshUtil.sshExec("chmod +x jppf-driver/startDriver.sh", System.err);
			System.out.println("Result: " + result);
			result = sshUtil.sshExec("cd jppf-driver;nohup ./startDriver.sh >>nohup.out 2>&1 &", System.err);
			System.out.println("Result: " + result);
		} catch (SSHClientExcpetion ex) {
			ex.printStackTrace();
		} finally {
			if(sshUtil != null) {
				sshUtil.disconnect();
			}
		}
//		nodeManager.suspendNode("eu-west-1/i-28b4f361");
	}
}