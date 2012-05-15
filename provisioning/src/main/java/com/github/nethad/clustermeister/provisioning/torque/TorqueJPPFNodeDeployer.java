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
import com.github.nethad.clustermeister.provisioning.utils.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.InetAddresses;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class TorqueJPPFNodeDeployer implements TorqueNodeDeployment, PublicIpNotifier {
    
    Logger logger = LoggerFactory.getLogger(TorqueJPPFNodeDeployer.class);
            
    private String host;
    private int port;
    private SSHClient sshClient;
    private String user;
    private boolean isInfrastructureDeployed;
    private AtomicInteger currentNodeNumber;
    private final long sessionId;
    private final TorqueConfiguration configuration;
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

    @VisibleForTesting
    void doPublicIpRequest() throws SSHClientException {
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

    /**
     * Deploy infrastructure via SSH and connect to it if necessary. This method is idempotent.
     * @param artifactsToPreload files to be deployed, e.g. libraries as jar files.
     * @throws SSHClientException 
     */
    public synchronized void prepareAndDeployInfrastructure(Collection<File> artifactsToPreload) throws SSHClientException {
        if (isInfrastructureDeployed) {
            return;
        }
        if (!sshClient.isConnected()) {
            connectToSSH();
        }
        deployInfrastructure(artifactsToPreload);

        isInfrastructureDeployed = true;
    }

    @VisibleForTesting
    void deployInfrastructure(Collection<File> artifactsToPreload) {
        InfrastructureDeployer infrastructureDeployer = new InfrastructureDeployer(sshClient);
        infrastructureDeployer.deployInfrastructure(artifactsToPreload);
    }

    /**
     * Deploys a new node to Torque and deploys infrastructure beforehand if necessary.
     * @param nodeConfiguration deployment configuration
     * @return
     * @throws SSHClientException 
     */
    public void deployNewNode(TorqueNodeConfiguration nodeConfiguration) throws SSHClientException {
        if (!isInfrastructureDeployed) {
            prepareAndDeployInfrastructure(nodeConfiguration.getArtifactsToPreload());
        }
        NodeDeployTask nodeDeployTask = 
                new NodeDeployTask(this, currentNodeNumber.getAndIncrement(), nodeConfiguration, configuration);
        nodeDeployTask.execute();
    }
    
    private void loadConfiguration() {     
          host = configuration.getSshHost();
          port = configuration.getSshPort();
          user = configuration.getSshUser();
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
