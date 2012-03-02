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

import com.github.nethad.clustermeister.api.Configuration;
import com.github.nethad.clustermeister.api.NodeConfiguration;
import com.github.nethad.clustermeister.api.impl.FileConfiguration;
import com.github.nethad.clustermeister.provisioning.jppf.JPPFNodeConfiguration;
import com.github.nethad.clustermeister.provisioning.utils.PublicIp;
import com.github.nethad.clustermeister.provisioning.utils.SSHClient;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientExcpetion;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author thomas
 */
public class TorqueJPPFNodeDeployer implements TorqueNodeDeployment {

//	private static final int DEFAULT_MANAGEMENT_PORT = 11198;
//	private static final String DEPLOY_BASE_NAME = "jppf-node";
	private static final String DEPLOY_ZIP = DEPLOY_BASE_NAME + ".zip";
//	private static final String DEPLOY_CONFIG_SUFFIX = ".properties";
//    private static final String DEPLOY_PROPERTIES = DEPLOY_BASE_NAME + DEPLOY_CONFIG_SUFFIX;
//	private static final String DEPLOY_QSUB = "qsub-node.sh";
//	private static final String PATH_TO_QSUB_SCRIPT = "./" + DEPLOY_BASE_NAME + "/" + DEPLOY_QSUB;

	private static final int POLLING_INTERVAL = 1000;
	private String host;
	private List<String> jobIdList;
	private String localIp;
	private Map<String, String> nodeIpMap;
	private int port;


	private SSHClient sshClient;
	private String user;
	private String privateKeyFilePath;
//    private String passphrase;
	private boolean isInfrastructureDeployed;
	private AtomicInteger currentNodeNumber;
	private final long sessionId;

	public TorqueJPPFNodeDeployer() {
		isInfrastructureDeployed = false;
		currentNodeNumber = new AtomicInteger(0);
		nodeIpMap = new HashMap<String, String>();
		jobIdList = new ArrayList<String>();
		sessionId = System.currentTimeMillis();
	}

	public void execute(int numberOfNodes) {
		sshClient = null;

		try {
			loadConfiguration();

			sshClient = new SSHClient(privateKeyFilePath);
			sshClient.connect(user, host, port);
			

			// remove previously uploaded files (might be outdated/not necessary)
			executeAndSysout("rm -rf " + DEPLOY_BASE_NAME + "*");

			uploadResources();

			prepareLocalIP();

			submitJobs_massSubmission(numberOfNodes);
//	    resolveNodeIpAddresses();
		} catch (SSHClientExcpetion ex) {
			System.out.println(ex.getClass().getName() + ": "+ex.getMessage());
		} finally {
			if (sshClient != null) {
				sshClient.disconnect();
			}
		}
	}

	public synchronized void deployInfrastructure() throws SSHClientExcpetion {
		if (isInfrastructureDeployed) {
			return;
		}
		loadConfiguration();

		sshClient = new SSHClient(privateKeyFilePath);
		sshClient.connect(user, host, port);

		// remove previously uploaded files (might be outdated/not necessary)
		executeAndSysout("rm -rf " + DEPLOY_BASE_NAME + "*");

		uploadResources();
		
		final String makeQsubScriptExecutable = "chmod +x " + PATH_TO_QSUB_SCRIPT + ";";
		sshClient.executeWithResult(makeQsubScriptExecutable);
		
		prepareLocalIP();
		isInfrastructureDeployed = true;
	}

	private void uploadResources() throws SSHClientExcpetion {
		// upload zip archive with all files, unpack it
		sshClient.sftpUpload(getResourcePath(DEPLOY_ZIP), DEPLOY_ZIP);
		executeAndSysout("unzip " + DEPLOY_ZIP);
		sshClient.sftpUpload(getResourcePath(DEPLOY_QSUB), DEPLOY_BASE_NAME + "/" + DEPLOY_QSUB);
	}

	/*
	private String resolveNodeIpAddresses(String jobId) throws SSHClientExcpetion {
//		for (String jobId : jobIdList) {
		waitForJobToBeRunning(jobId);
		String nodeIp = waitForNodeIpToResolve(jobId);
		nodeIpMap.put(jobId, nodeIp);
		System.out.println(jobId + ": " + nodeIp);
		return nodeIp;
//		}
	}
	*/

	public TorqueNode submitJob(NodeConfiguration nodeConfiguration, TorqueNodeManagement torqueNodeManagement) throws SSHClientExcpetion {
		if (!isInfrastructureDeployed) {
			deployInfrastructure();
		}
		NodeDeployTask nodeDeployTask = new NodeDeployTask(this, currentNodeNumber.getAndIncrement(), nodeConfiguration);
		final TorqueNode torqueNode = nodeDeployTask.execute();
		torqueNodeManagement.addManagedNode(torqueNode);
		return torqueNode;
//		String nodeNameBase = "CMNode" + sessionId;
//		String nodeName = nodeNameBase + "_" + currentNodeNumber;
//		String nodeConfigFileName = configFileName();
//		uploadNodeConfiguration(nodeConfigFileName, nodeConfiguration.getDriverAddress());
//		final String makeQsubScriptExecutable = "chmod +x " + PATH_TO_QSUB_SCRIPT + ";";
//		final String submitJobToQsub = PATH_TO_QSUB_SCRIPT + " " + nodeName + " " + nodeConfigFileName + "|qsub";
//		String currentJobId = executeWithResult(makeQsubScriptExecutable + submitJobToQsub);
//		jobIdList.add(currentJobId);
//
//		TorqueNode torqueNode = new TorqueNode(NodeType.NODE);
//		currentNodeNumber.incrementAndGet();
//		return torqueNode;
	}

	/*
	private void resolveNodeIpAddresses() throws SSHClientExcpetion {
		for (String jobId : jobIdList) {
			waitForJobToBeRunning(jobId);
			String nodeIp = waitForNodeIpToResolve(jobId);
			nodeIpMap.put(jobId, nodeIp);
			System.out.println(jobId + ": " + nodeIp);
		}
	}
	*/ 

	private void submitJobs_massSubmission(int numberOfNodes) throws SSHClientExcpetion {
		// assume java is installed (installed in ~/jdk-1.7)
		// executeAndSysout("cp -R /home/user/dspicar/jdk-1.7 ~/jdk-1.7");

		// execute qsub helper script and pipe it into qsub (job submission)
		String pathToQsubScript = "./" + DEPLOY_BASE_NAME + "/" + DEPLOY_QSUB;

		String nodeNameBase = "Node" + sessionId;
		String nodeName;
		String nodeConfigFileName;
//		String currentJobId;
		nodeIpMap = new HashMap<String, String>();
		jobIdList = new ArrayList<String>();
		StringBuilder executeString = new StringBuilder();
		for (int nodeNumber = 0; nodeNumber < numberOfNodes; nodeNumber++) {
			System.out.println("Current node number: " + nodeNumber);
			nodeName = nodeNameBase + "_" + nodeNumber;
			nodeConfigFileName = configFileName();
			uploadNodeConfiguration(nodeConfigFileName, localIp);
//			final String makeQsubScriptExecutable = "chmod +x " + pathToQsubScript + ";";
			final String submitJobToQsub = pathToQsubScript + " " + nodeName + " " + nodeConfigFileName + "|qsub";
			executeString.append(submitJobToQsub +";");
//			currentJobId = executeWithResult(makeQsubScriptExecutable + submitJobToQsub);
//			jobIdList.add(currentJobId);
//			executeAndSysout("uname -r");
			currentNodeNumber.incrementAndGet();
		}
		final String makeQsubScriptExecutable = "chmod +x " + pathToQsubScript;
		executeAndSysout(makeQsubScriptExecutable);
	
//		executeAndSysout("uname -r");
		
		String jobIdsFromOutput = executeWithResult(executeString.toString());
		String[] jobIdArray = jobIdsFromOutput.split("\\\n");
		System.out.println("jobIdArray size = " + jobIdArray.length);
		//		executeAndSysout(executeString.toString());
		//		System.out.println("jobIdsFromOutput = " + jobIdsFromOutput);
	}

	private void uploadNodeConfiguration(String nodeConfigFileName, String driverIpAddress) throws SSHClientExcpetion {
		
				// generate properties file from configuration class and attach
		// the local ip address as the driver's IP target address.
		
		int managementPort = DEFAULT_MANAGEMENT_PORT + currentNodeNumber.intValue();
		String driverIp;
		if (driverIpAddress == null) {
			driverIp = driverIpAddress;
		} else {
			driverIp = localIp;
		}
		try {
			InputStream propertyStream = new JPPFNodeConfiguration()
					.setProperty("jppf.server.host", driverIp)
					.setProperty("jppf.management.port", String.valueOf(managementPort))
					.setProperty("jppf.resource.cache.dir", "/tmp/.jppf/node-" + sessionId + "_" + currentNodeNumber)
					.getPropertyStream();
			sshClient.sftpUpload(propertyStream, DEPLOY_BASE_NAME + "/config/" + nodeConfigFileName);
		} catch (IOException ex) {
			Logger.getLogger(TorqueJPPFNodeDeployer.class.getName()).log(Level.SEVERE, null, ex);
		} catch (SSHClientExcpetion ex) {
			Logger.getLogger(TorqueJPPFNodeDeployer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void prepareLocalIP() {
		//		localHost = InetAddress.getLocalHost().getHostAddress();
		localIp = PublicIp.getPublicIp();
		System.out.println("localIp = " + localIp);
//		URL whatismyip;
//		BufferedReader in = null;
//		try {
//			whatismyip = new URL("http://automation.whatismyip.com/n09230945.asp");
//			in = new BufferedReader(new InputStreamReader(
//					whatismyip.openStream()));
//			localIp = in.readLine(); //you get the IP as a String
//			System.out.println("localIp = " + localIp);
//		} catch (MalformedURLException ex) {
//			Logger.getLogger(TorqueJPPFNodeDeployer.class.getName()).log(Level.SEVERE, null, ex);
//		} catch (IOException ex) {
//			Logger.getLogger(TorqueJPPFNodeDeployer.class.getName()).log(Level.SEVERE, null, ex);
//		} finally {
//			try {
//				in.close();
//			} catch (IOException ex) {
//				Logger.getLogger(TorqueJPPFNodeDeployer.class.getName()).log(Level.SEVERE, null, ex);
//			}
//		}
	}

	private String configFileName() {
		return DEPLOY_BASE_NAME + "-" + currentNodeNumber + DEPLOY_CONFIG_SUFFIX;

	}

	/*
	private String waitForNodeIpToResolve(String jobId) throws SSHClientExcpetion {
		String nodeIp;
		do {
			waitForJobToBeInList(jobId);
			String command = "pbsnodes | grep 'status ='|grep " + jobId + "|grep -o -P 'uname=\\w+\\s\\w+\\s'|awk {'print $2'}|xargs -n 1 host -W 10 |awk {'print $4'}";
			nodeIp = executeWithResult(command);
		} while (nodeIp.isEmpty());
		return nodeIp;
	} 

	private void waitForJobToBeInList(String jobId) throws SSHClientExcpetion, NumberFormatException {
		String jobInListCommand = "pbsnodes | grep 'status ='|grep " + jobId + "|wc -l";
		int jobInList;
		int timeoutCounter = 0;
		int timeout = 20;
		do {
			try {
				Thread.sleep(POLLING_INTERVAL * 3);
			} catch (InterruptedException ex) {
				// do nothing
			}
			jobInList = Integer.parseInt(executeWithResult(jobInListCommand));
			timeoutCounter++;
		} while (jobInList < 1 && timeoutCounter < timeout);
	}
	*/ 

	private String getResourcePath(String resource) {
		return TorqueJPPFDriverDeployer.class.getResource(resource).getPath();
	}

	private void executeAndSysout(String command) throws SSHClientExcpetion {
		System.out.println("$ " + command);
		String result = sshClient.sshExec(command, System.err);
		System.out.println("Result: " + result);
	}

	private String executeWithResult(String command) throws SSHClientExcpetion {
		System.out.println("$ " + command);
		return sshClient.sshExec(command, System.err);
	}

	private void loadConfiguration() {
		String home = System.getProperty("user.home");
		String separator = System.getProperty("file.separator");
		Configuration config = new FileConfiguration(home + separator + ".clustermeister" + separator + "torque.properties");

		host = getStringDefaultEmpty(config, "host");
		port = config.getInt("port", 22);
		user = getStringDefaultEmpty(config, "user");
		privateKeyFilePath = getStringDefaultEmpty(config, "privateKey");
//        passphrase = getStringDefaultempty(config, "passphrase");

	}

	private String getStringDefaultEmpty(Configuration config, String key) {
		return config.getString(key, "");
	}

	private void waitForJobToBeRunning(String jobId) throws SSHClientExcpetion {
		String jobIdNumber = jobId.split("\\.")[0];
		String state;
		do {
			try {
				Thread.sleep(POLLING_INTERVAL);
			} catch (InterruptedException ex) {
				// do nothing
			}
			String command = "qstat -u " + user + "|awk {'print $1,$10'}|grep -P '^[\\d]+'|grep " + jobIdNumber;
			String result = executeWithResult(command);
			executeAndSysout(command);
			state = result.split(" ")[1];
		} while (checkIfStillHaveToWait(state));
	}

	private boolean checkIfStillHaveToWait(String state) {
		if (state.equals("Q")) {
			return true; // job is queued
		} else if (state.equals("R")) {
			return false; // job is running
		} else if (state.equals("C")) {
			throw new IllegalStateException("Job is already completed.");
		} else {
			throw new IllegalStateException("Job has errors");
		}
	}

	@Override
	public String getDriverAddress() {
		return localIp;
	}

	@Override
	public String getSessionId() {
		return String.valueOf(sessionId);
	}

	@Override
	public SSHClient sshClient() {
		return sshClient;
	}

	public void disconnectSshConnection() {
		sshClient.disconnect();
	}

}
