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
import com.github.nethad.clustermeister.provisioning.utils.*;
import com.google.common.annotations.VisibleForTesting;
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
    protected File akkaZipFile;
    
    Logger logger = LoggerFactory.getLogger(TorqueJPPFNodeDeployer.class);

//	private static final int DEFAULT_MANAGEMENT_PORT = 11198;
//	private static final String DEPLOY_BASE_NAME = "jppf-node";
    private static final String DEPLOY_ZIP_NAME = DEPLOY_BASE_NAME + ".zip";
    private static final String LOCAL_DEPLOY_ZIP_PATH = "/" + DEPLOY_ZIP_NAME;
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
    private final TorqueConfiguration configuration;
    private String email;
    private String akkaZip;
    

    public TorqueJPPFNodeDeployer(TorqueConfiguration configuration, SSHClient sshClient) {
        this.configuration = configuration;
        isInfrastructureDeployed = false;
        currentNodeNumber = new AtomicInteger(0);
        sessionId = System.currentTimeMillis();
        loadConfiguration();
        this.sshClient = sshClient;
    }
    
    @VisibleForTesting
    void setSshClient(SSHClient sshClient) {
        this.sshClient = sshClient;
    }
    
        @VisibleForTesting
    boolean isInfrastructureDeployed() {
        return isInfrastructureDeployed;
    }

    public synchronized void deployInfrastructure() throws SSHClientException {
        if (isInfrastructureDeployed) {
            return;
        }
        
        sshClient.connect(user, host, port);
        try {
            deployZipCRC32 = FileUtils.getCRC32(getResourceStream(LOCAL_DEPLOY_ZIP_PATH));
            akkaZipFile = new File(akkaZip);
            if (akkaZipFile.exists()) {
                logger.info("akka libs zip exists.");
                akkaLibsZipCRC32 = FileUtils.getCRC32ForFile(akkaZipFile);
            } else {
                logger.info("akka libs zip does NOT exist. {}", akkaZip);
                akkaLibsZipCRC32 = 0L;
            }
        } catch (IOException ex) {
            logger.error("Can not read resource.", ex);
        }
        
        // delete config files (separate config files for each node are generated)
        sshClient.executeAndSysout("rm -rf " + DEPLOY_BASE_NAME + "/config/*.properties");

        if (!isResourceAlreadyDeployedAndUpToDate()) {
            logger.info("Resource is not up to date.");
            // remove previously uploaded files (might be outdated/not necessary)
            sshClient.executeAndSysout("rm -rf " + DEPLOY_BASE_NAME + "*");

            uploadResources();
        } else {
            logger.info("Resource is up to date.");
        }
        
        prepareLocalIP();
        isInfrastructureDeployed = true;
    }

    private void uploadResources() throws SSHClientException {
        // upload zip archive with all files, unpack it
        sshClient.sftpUpload(getResourceStream(LOCAL_DEPLOY_ZIP_PATH), DEPLOY_ZIP_NAME);
        sshClient.executeAndSysout("unzip " + DEPLOY_ZIP_NAME);
        
        // upload akka libraries
        // TODO deactivated akka upload for testing purposes
//        sshClient.sftpUpload(getResourcePath(AKKA_ZIP), AKKA_REMOTE_ZIP_PATH);
        if (akkaZipFile.exists()) {
            logger.info("upload akka zip.");
            sshClient.sftpUpload(akkaZipFile.getAbsolutePath(), AKKA_REMOTE_ZIP_PATH);
            sshClient.executeAndSysout("cd "+DEPLOY_BASE_NAME+"/lib/ && unzip " + AKKA_ZIP +" > unzip.log");
            sshClient.executeAndSysout("cd "+DEPLOY_BASE_NAME+"/lib/ && cat unzip.log");
        }
        
//        sshClient.sftpUpload(getResourceStream(DEPLOY_QSUB), DEPLOY_BASE_NAME + "/" + DEPLOY_QSUB);
        sshClient.sftpUpload(new ByteArrayInputStream(
                String.valueOf(deployZipCRC32).getBytes(Charsets.UTF_8)), CRC32_FILE);
        sshClient.sftpUpload(new ByteArrayInputStream(
                String.valueOf(akkaLibsZipCRC32).getBytes(Charsets.UTF_8)), AKKA_CRC32_FILE);
    }

    public TorqueNode submitJob(TorqueNodeConfiguration nodeConfiguration, TorqueNodeManagement torqueNodeManagement) throws SSHClientException {
        if (!isInfrastructureDeployed) {
            deployInfrastructure();
        }
        NodeDeployTask nodeDeployTask = new NodeDeployTask(this, currentNodeNumber.getAndIncrement(), nodeConfiguration, email);
        final TorqueNode torqueNode = nodeDeployTask.execute();
        torqueNodeManagement.addManagedNode(torqueNode);
        return torqueNode;
    }

    private void prepareLocalIP() {
        //		localHost = InetAddress.getLocalHost().getHostAddress();
        localIp = PublicIp.getPublicIp();
        logger.info("localIp = " + localIp);
    }
    
    private void loadConfiguration() {     
          host = configuration.getSshHost();
          port = configuration.getSshPort();
          user = configuration.getSshUser();
          privateKeyFilePath = configuration.getPrivateKeyPath();
          email = configuration.getEmailNotify();
          akkaZip = System.getProperty("user.home")+"/.clustermeister/akka-libs.zip";
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

    @VisibleForTesting
    boolean isResourceAlreadyDeployedAndUpToDate() {
        if (!doesFileExistOnRemote(CRC32_FILE) || !doesFileExistOnRemote(AKKA_CRC32_FILE)) {
            logger.info("At least one CRC32 file is missing.");
            return false;
        }
        try {
            String md5sumDeployZipRemote = sshClient.executeWithResult("cat " + CRC32_FILE);
            String md5sumAkkaRemote = sshClient.executeWithResult("cat " + AKKA_CRC32_FILE);
            return md5sumDeployZipRemote.equals(String.valueOf(deployZipCRC32))
                   && md5sumAkkaRemote.equals(String.valueOf(akkaLibsZipCRC32));
        } catch (SSHClientException ex) {
            logger.error("SSH exception", ex);
        }
        return false;
    }

    private boolean doesFileExistOnRemote(String filePath) {
        try {
            String command = FileUtils.getFileExistsShellCommand(filePath);
            final String result = sshClient.executeWithResult(command);
            return Boolean.parseBoolean(result);
        } catch (SSHClientException ex) {
            logger.error("SSH exception", ex);
        }
        return false;
    }

    InputStream getResourceStream(String resource) {
        return TorqueJPPFDriverDeployer.class.getResourceAsStream(resource);
    }
}
