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

import com.github.nethad.clustermeister.provisioning.jppf.PublicIpNotifier;
import com.github.nethad.clustermeister.provisioning.utils.FileUtils;
import com.github.nethad.clustermeister.provisioning.utils.PublicIp;
import com.github.nethad.clustermeister.provisioning.utils.SSHClient;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.net.InetAddresses;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class TorqueJPPFNodeDeployer implements TorqueNodeDeployment, PublicIpNotifier {
    protected File akkaZipFile;
    
    Logger logger = LoggerFactory.getLogger(TorqueJPPFNodeDeployer.class);

//	private static final int DEFAULT_MANAGEMENT_PORT = 11198;
//	private static final String DEPLOY_BASE_NAME = "jppf-node";
    private static final String DEPLOY_ZIP_NAME = DEPLOY_BASE_NAME + ".zip";
    private static final String LOCAL_DEPLOY_ZIP_PATH = "/" + DEPLOY_ZIP_NAME;
    private static final String AKKA_ZIP = "akka-libs.tar.bz2";
    private static final String AKKA_REMOTE_ZIP_PATH = DEPLOY_BASE_NAME + "/lib/" + AKKA_ZIP;
    private static final String CRC32_FILE = DEPLOY_BASE_NAME + "/CRC32";
    private static final String AKKA_CRC32_FILE = DEPLOY_BASE_NAME + "/AKKA_CRC32";
//	private static final String DEPLOY_CONFIG_SUFFIX = ".properties";
//    private static final String DEPLOY_PROPERTIES = DEPLOY_BASE_NAME + DEPLOY_CONFIG_SUFFIX;
//	private static final String DEPLOY_QSUB = "qsub-node.sh";
//	private static final String PATH_TO_QSUB_SCRIPT = "./" + DEPLOY_BASE_NAME + "/" + DEPLOY_QSUB;
    private String host;
//    private String localIp;
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
    private ArrayList<Observer> publicIpListener;
    private String publicIp;
    

    public TorqueJPPFNodeDeployer(TorqueConfiguration configuration, SSHClient sshClient) {
        this.configuration = configuration;
        isInfrastructureDeployed = false;
        currentNodeNumber = new AtomicInteger(0);
        sessionId = System.currentTimeMillis();
        loadConfiguration();
        this.sshClient = sshClient;
        publicIpListener = new ArrayList<Observer>();
    }
    
    @VisibleForTesting
    void setSshClient(SSHClient sshClient) {
        this.sshClient = sshClient;
    }
    
    @VisibleForTesting
    boolean isInfrastructureDeployed() {
        return isInfrastructureDeployed;
    }
    
    private void connectToSSH() throws SSHClientException {
        sshClient.connect(user, host, port);
    }

    private void doPublicIpRequest() throws SSHClientException {
        if (!sshClient.isConnected()) {
            connectToSSH();
        }

        String publicIpString = sshClient.executeWithResult("echo $SSH_CLIENT");
        String publicIpPart = publicIpString.split(" ")[0];
        if (InetAddresses.isInetAddress(publicIpPart)) {
            this.publicIp = publicIpPart;
            logger.info("Public IP request successful. ({})", publicIp);
        } else {
            this.publicIp = PublicIp.getInstance().getPublicIp();
            logger.warn("Error parsing public IP from \"{}\", used fallback {} instead ({})", new String[]{publicIpString, PublicIp.class.getName(), publicIp});
        }
        notifyPublicIp(publicIp);
    }

    public synchronized void deployInfrastructure() throws SSHClientException {
        if (isInfrastructureDeployed) {
            return;
        }
        
        if (!sshClient.isConnected()) {
            connectToSSH();
        }
        
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
        
//        prepareLocalIP();
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
            sshClient.executeAndSysout("cd "+DEPLOY_BASE_NAME+"/lib/ && tar xvf " + AKKA_ZIP);
//            sshClient.executeAndSysout("cd "+DEPLOY_BASE_NAME+"/lib/ && cat unzip.log");
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

//    private void prepareLocalIP() {
//        //		localHost = InetAddress.getLocalHost().getHostAddress();
//        localIp = PublicIp.getInstance().getPublicIp();
//        logger.info("localIp = " + localIp);
//    }
    
    private void loadConfiguration() {     
          host = configuration.getSshHost();
          port = configuration.getSshPort();
          user = configuration.getSshUser();
          privateKeyFilePath = configuration.getPrivateKeyPath();
          email = configuration.getEmailNotify();
          akkaZip = System.getProperty("user.home")+"/.clustermeister/"+AKKA_ZIP;
    }

    @Override
    public String getDriverAddress() {
        if (publicIp == null) {
            try {
                doPublicIpRequest();
            } catch (SSHClientException ex) {
                logger.warn("Request for public IP failed.", ex);
            }
        }
        return publicIp;
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
        return getClass().getResourceAsStream(resource);
    }

    @Override
    public void notifyPublicIp(String publicIp) {
        for (Observer observer : publicIpListener) {
            observer.update(null, publicIp);
        }
    }

    @Override
    public void addListener(Observer listener) {
        publicIpListener.add(listener);
        getDriverAddress();
    }

    @Override
    public void remoteListener(Observer listener) {
        publicIpListener.remove(listener);
    }
}
