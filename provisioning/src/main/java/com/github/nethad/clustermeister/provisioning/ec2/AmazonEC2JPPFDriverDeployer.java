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

import com.github.nethad.clustermeister.provisioning.FileConfiguration;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.domain.Credentials;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.io.Payloads;
import org.jclouds.net.IPSocket;
import org.jclouds.predicates.InetSocketAddressConnect;
import org.jclouds.predicates.RetryablePredicate;
import org.jclouds.ssh.SshClient;

/**
 *
 * @author daniel
 */
public class AmazonEC2JPPFDriverDeployer {

	public static void main(String... args) throws FileNotFoundException, IOException, TimeoutException {
		String userName = "ec2-user";

		FileConfiguration config = new FileConfiguration("/home/daniel/clustermeister-amazonapi.properties");
		AmazonAPIManageNodes nodeManager = new AmazonAPIManageNodes(config);

		nodeManager.init();
		
		nodeManager.resumeNode("eu-west-1/i-28b4f361");
		
		NodeMetadata metadata = nodeManager.getContext().getComputeService().
				getNodeMetadata("eu-west-1/i-28b4f361");
//		String publicIP = metadata.getPublicAddresses().iterator().next();


//		RetryablePredicate<IPSocket> socketTester =
//				new RetryablePredicate<IPSocket>(
//				new InetSocketAddressConnect(), 300, 1, TimeUnit.SECONDS);
//		System.out.printf("%d: %s awaiting ssh service to start%n", System.currentTimeMillis(), publicIP);
//		if (!socketTester.apply(new IPSocket(publicIP, 22))) {
//			throw new TimeoutException("timeout waiting for ssh to start: " + publicIP);
//		}

//		System.out.printf("%d: %s ssh service started%n", System.currentTimeMillis(), publicIP);

		BufferedReader reader = new BufferedReader(new FileReader("/home/daniel/Desktop/EC2/EC2_keypair.pem"));
		StringBuilder sb = new StringBuilder();

		String line;
		while ((line = reader.readLine()) != null) {
			sb.append(line);
			sb.append("\n");
		}
		reader.close();


		SshClient client = nodeManager.getContext().utils().sshForNode().apply(
				NodeMetadataBuilder.fromNodeMetadata(metadata).credentials(
                       new LoginCredentials(userName, null, sb.toString().trim(), true)).build());
		client.connect();
		try {
			ExecResponse res = client.exec("rm -rf jppf-driver*");
			System.out.println("Result: " + res.toString());
			InputStream is = AmazonEC2JPPFDriverDeployer.class.getResourceAsStream("jppf-driver.zip");
			client.put("/home/ec2-user/jppf-driver.zip", Payloads.newInputStreamPayload(is));
			if(is != null) {
				try {
					is.close();
				} catch (IOException ex) {
					//ignore
				}
			}
			ExecResponse res2 = client.exec("unzip jppf-driver.zip");
			System.out.println("Result: " + res2.toString());
			res2 = client.exec("chmod +x jppf-driver/startDriver.sh");
			System.out.println("Result: " + res2.toString());
			is = AmazonEC2JPPFDriverDeployer.class.getResourceAsStream("jppf-driver.properties");
			client.put("jppf-driver/config/jppf-driver.properties", Payloads.newInputStreamPayload(is));
			if(is != null) {
				try {
					is.close();
				} catch (IOException ex) {
					//ignorel
				}
			}
//			String command = "cd jppf-driver;nohup ./startDriver.sh > nohup.out 2>&1 &";
////			String command = "cd jppf-driver;nohup ./startDriver.sh";
//			System.out.println("command = " + command);
//			ExecResponse res3 = client.exec(command);
//			System.out.println("Result: " + res3.toString());
			ExecResponse res4 = nodeManager.getContext().getComputeService().
					runScriptOnNode("eu-west-1/i-28b4f361", 
					"cd /home/ec2-user/jppf-driver\nnohup ./startDriver.sh > nohup.out 2>&1", 
					new RunScriptOptions().overrideLoginPrivateKey(sb.toString().trim()).
					overrideLoginUser(userName).blockOnComplete(false).runAsRoot(false).nameTask("jppf-driver-start"));
			System.out.println(res4);
		} finally {
			if (client != null) {
				client.disconnect();
			}
		}


//		SSHClient sshUtil = null;
//		try {
//			String privateKeyFile = "/home/daniel/Desktop/EC2/EC2_keypair.pem";
////			String host = "ec2-176-34-200-11.eu-west-1.compute.amazonaws.com";
//			int port = 22;
//			
//			
//			sshUtil = new SSHClient(privateKeyFile);
//			sshUtil.connect(userName, publicIP, port);
//			String result = sshUtil.sshExec("rm -rf jppf-driver*", System.err);
//			System.out.println("Result: " + result);
//			sshUtil.sftpUpload(AmazonEC2JPPFDriverDeployer.class.getResource("jppf-driver.zip").getPath(), "jppf-driver.zip");
////			sshUtil.sftpUpload(AmazonEC2JPPFDriverDeployer.class.getResource("jppf-driver").getPath(), "jppf-driver");
//			result = sshUtil.sshExec("unzip jppf-driver.zip", System.err);
//			System.out.println("Result: " + result);
//			sshUtil.sftpUpload(AmazonEC2JPPFDriverDeployer.class.getResource("jppf-driver.properties").getPath(), "jppf-driver/config/jppf-driver.properties");
//			result = sshUtil.sshExec("chmod +x jppf-driver/startDriver.sh", System.err);
//			System.out.println("Result: " + result);
//			result = sshUtil.sshExec("cd jppf-driver;nohup ./startDriver.sh >>nohup.out 2>&1 &", System.err);
//			System.out.println("Result: " + result);
//		} catch (SSHClientExcpetion ex) {
//			ex.printStackTrace();
//		} finally {
//			if(sshUtil != null) {
//				sshUtil.disconnect();
//				System.out.println("HERE");
//			}
//		}
//		nodeManager.suspendNode("eu-west-1/i-28b4f361");
	}
}