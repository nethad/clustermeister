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
import com.github.nethad.clustermeister.provisioning.utils.FileUtils;
import com.github.nethad.clustermeister.provisioning.utils.PublicIp;
import com.github.nethad.clustermeister.provisioning.utils.SSHClient;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientExcpetion;
import com.google.common.base.Charsets;
import java.io.*;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class TorqueJPPFNodeDeployer implements TorqueNodeDeployment {
    
    Logger logger = LoggerFactory.getLogger(TorqueJPPFNodeDeployer.class);

//	private static final int DEFAULT_MANAGEMENT_PORT = 11198;
//	private static final String DEPLOY_BASE_NAME = "jppf-node";
    private static final String DEPLOY_ZIP = DEPLOY_BASE_NAME + ".zip";
    private static final String AKKA_ZIP = "akka-libs.zip";
    private static final String AKKA_REMOTE_ZIP_PATH = DEPLOY_BASE_NAME + "/lib/" + AKKA_ZIP;
    private static final String CRC32_FILE = DEPLOY_BASE_NAME + "/CRC32";
    private static final String AKKA_CRC32_FILE = DEPLOY_BASE_NAME + "/AKKA_CRC32";
//	private static final String DEPLOY_CONFIG_SUFFIX = ".properties";
//    private static final String DEPLOY_PROPERTIES = DEPLOY_BASE_NAME + DEPLOY_CONFIG_SUFFIX;
//	private static final String DEPLOY_QSUB = "qsub-node.sh";
//	private static final String PATH_TO_QSUB_SCRIPT = "./" + DEPLOY_BASE_NAME + "/" + DEPLOY_QSUB;
    private String host;
    private String localIp;
    private int port;
    private SSHClient sshClient;
    private String user;
    private String privateKeyFilePath;
//    private String passphrase;
    private boolean isInfrastructureDeployed;
    private AtomicInteger currentNodeNumber;
    private final long sessionId;
    private long deployZipCRC32;
    private long akkaLibsZipCRC32;
    

    public TorqueJPPFNodeDeployer() {
        isInfrastructureDeployed = false;
        currentNodeNumber = new AtomicInteger(0);
        sessionId = System.currentTimeMillis();
    }

//    private void execute(int numberOfNodes) {
//        sshClient = null;
//
//        try {
//            loadConfiguration();
//
//            sshClient = new SSHClient(privateKeyFilePath);
//            sshClient.connect(user, host, port);
//
//
//            // remove previously uploaded files (might be outdated/not necessary)
//            executeAndSysout("rm -rf " + DEPLOY_BASE_NAME + "*");
//
//            uploadResources();
//
//            prepareLocalIP();
//
//            submitJobs_massSubmission(numberOfNodes);
////	    resolveNodeIpAddresses();
//        } catch (SSHClientExcpetion ex) {
//            System.out.println(ex.getClass().getName() + ": " + ex.getMessage());
//        } finally {
//            if (sshClient != null) {
//                sshClient.disconnect();
//            }
//        }
//    }

    public synchronized void deployInfrastructure() throws SSHClientExcpetion {
        if (isInfrastructureDeployed) {
            return;
        }
        loadConfiguration();

        sshClient = new SSHClient(privateKeyFilePath);
        sshClient.connect(user, host, port);
        try {
            deployZipCRC32 = FileUtils.getCRC32ForFile(new File(getResourcePath(DEPLOY_ZIP)));
            akkaLibsZipCRC32 = FileUtils.getCRC32ForFile(new File(getResourcePath(AKKA_ZIP)));
        } catch (IOException ex) {
            //TODO: logger
            logger.error("Can not read from File.");
        }
        
        // delete config files (separate config files for each node are generated)
        executeAndSysout("rm -rf " + DEPLOY_BASE_NAME + "/config/*.properties");

        if (!isResourceAlreadyDeployedAndUpToDate()) {
            logger.info("Resource is not up to date.");
            System.out.println("Resource is not up to date.");
            // remove previously uploaded files (might be outdated/not necessary)
            executeAndSysout("rm -rf " + DEPLOY_BASE_NAME + "*");

            uploadResources();

            final String makeQsubScriptExecutable = "chmod +x " + PATH_TO_QSUB_SCRIPT + ";";
            sshClient.executeWithResult(makeQsubScriptExecutable);
        } else {
            logger.info("Resource is up to date.");
            System.out.println("Resource is up to date.");
        }
        
        prepareLocalIP();
        isInfrastructureDeployed = true;
    }

    private void uploadResources() throws SSHClientExcpetion {
        // upload zip archive with all files, unpack it
        sshClient.sftpUpload(getResourcePath(DEPLOY_ZIP), DEPLOY_ZIP);
        executeAndSysout("unzip " + DEPLOY_ZIP);
        
        // upload akka libraries
        // TODO deactivated akka upload for testing purposes
//        sshClient.sftpUpload(getResourcePath(AKKA_ZIP), AKKA_REMOTE_ZIP_PATH);
//        executeAndSysout("cd "+DEPLOY_BASE_NAME+"/lib/ && unzip " + AKKA_ZIP);
        
        sshClient.sftpUpload(getResourcePath(DEPLOY_QSUB), DEPLOY_BASE_NAME + "/" + DEPLOY_QSUB);
        sshClient.sftpUpload(new ByteArrayInputStream(
                String.valueOf(deployZipCRC32).getBytes(Charsets.UTF_8)), CRC32_FILE);
        sshClient.sftpUpload(new ByteArrayInputStream(
                String.valueOf(akkaLibsZipCRC32).getBytes(Charsets.UTF_8)), AKKA_CRC32_FILE);
    }

    public TorqueNode submitJob(NodeConfiguration nodeConfiguration, TorqueNodeManagement torqueNodeManagement) throws SSHClientExcpetion {
        if (!isInfrastructureDeployed) {
            deployInfrastructure();
        }
        NodeDeployTask nodeDeployTask = new NodeDeployTask(this, currentNodeNumber.getAndIncrement(), nodeConfiguration);
        final TorqueNode torqueNode = nodeDeployTask.execute();
        torqueNodeManagement.addManagedNode(torqueNode);
        return torqueNode;
    }

    /*
     * private void resolveNodeIpAddresses() throws SSHClientExcpetion { for (String jobId : jobIdList) {
     * waitForJobToBeRunning(jobId); String nodeIp = waitForNodeIpToResolve(jobId); nodeIpMap.put(jobId, nodeIp);
     * System.out.println(jobId + ": " + nodeIp); } }
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
        StringBuilder executeString = new StringBuilder();
        for (int nodeNumber = 0; nodeNumber < numberOfNodes; nodeNumber++) {
            System.out.println("Current node number: " + nodeNumber);
            nodeName = nodeNameBase + "_" + nodeNumber;
            nodeConfigFileName = configFileName();
            uploadNodeConfiguration(nodeConfigFileName, localIp);
//			final String makeQsubScriptExecutable = "chmod +x " + pathToQsubScript + ";";
            final String submitJobToQsub = pathToQsubScript + " " + nodeName + " " + nodeConfigFileName + "|qsub";
            executeString.append(submitJobToQsub + ";");
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
            logger.error("Could not read property file.", ex);
        } catch (SSHClientExcpetion ex) {
            logger.error("SSH excpetion", ex);
        }
    }

    private void prepareLocalIP() {
        //		localHost = InetAddress.getLocalHost().getHostAddress();
        localIp = PublicIp.getPublicIp();
        logger.info("localIp = " + localIp);
    }

    private String configFileName() {
        return DEPLOY_BASE_NAME + "-" + currentNodeNumber + DEPLOY_CONFIG_SUFFIX;

    }

    private String getResourcePath(String resource) {
        return TorqueJPPFDriverDeployer.class.getResource(resource).getPath();
    }

    @Deprecated
    private void executeAndSysout(String command) throws SSHClientExcpetion {
        sshClient.sshExec(command, System.err);
    }

    @Deprecated
    private String executeWithResult(String command) throws SSHClientExcpetion {
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
        sshClient = null;
    }

    private boolean isResourceAlreadyDeployedAndUpToDate() {
        if (!doesFileExistOnRemote(CRC32_FILE) || !doesFileExistOnRemote(AKKA_CRC32_FILE)) {
            logger.info("At least one CRC32 file is missing.");
            return false;
        }
        try {
            String md5sumDeployZipRemote = sshClient.executeWithResult("cat " + CRC32_FILE);
            String md5sumAkkaRemote = sshClient.executeWithResult("cat " + AKKA_CRC32_FILE);
            return md5sumDeployZipRemote.equals(String.valueOf(deployZipCRC32))
                   && md5sumAkkaRemote.equals(String.valueOf(akkaLibsZipCRC32));
        } catch (SSHClientExcpetion ex) {
            logger.error("SSH exception", ex);
        }
        return false;
    }

    private boolean doesFileExistOnRemote(String filePath) {
        try {
            String command = FileUtils.getFileExistsShellCommand(filePath);
            final String result = sshClient.executeWithResult(command);
            return Boolean.parseBoolean(result);
        } catch (SSHClientExcpetion ex) {
            logger.error("SSH exception", ex);
        }
        return false;
    }
}
