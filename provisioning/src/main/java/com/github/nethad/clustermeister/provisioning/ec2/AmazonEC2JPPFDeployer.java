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

import com.github.nethad.clustermeister.provisioning.FileResource;
import com.github.nethad.clustermeister.provisioning.InputStreamResource;
import com.github.nethad.clustermeister.provisioning.RemoteResourceManager;
import com.github.nethad.clustermeister.provisioning.Resource;
import com.github.nethad.clustermeister.provisioning.utils.JCloudsSshClientWrapper;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientException;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Monitor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Properties;
import java.util.regex.Matcher;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.domain.Processor;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.io.Payloads;
import org.jclouds.ssh.SshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
public abstract class AmazonEC2JPPFDeployer extends Observable {

    protected int getNumberOfProcessingThreads() {
        int numberOfCores = 0;
        for (Processor processor : metadata.getHardware().getProcessors()) {
            numberOfCores += (int) processor.getCores();
        }
        return numberOfCores;
    }
    public static enum Event{DEPLOYMENT_PREPARED, 
            RESOURCES_PRELOADED, JPPF_CONFIGURATED, DEPLOYMENT_FINISHED};
    
    protected static final String UUID_PREFIX = "UUID=";

    protected final static Logger logger =
            LoggerFactory.getLogger(AmazonEC2JPPFDeployer.class);
    
    protected static final String INIT_LOG = "init.log";
    protected static final String CLUSTERMEISTER_BIN = "clustermeister-bin";
    
    private static final Monitor driverMM = new Monitor(false);
    private static final Monitor nodeMM = new Monitor(false);
    //TODO: make sure this does not cause a memory leak.
    protected static final Map<String, Monitor> instanceDriverMonitors = new HashMap<String, Monitor>();
    protected static final Map<String, Monitor> instanceNodeMonitors = new HashMap<String, Monitor>();
    
    protected final String zipFile;
    protected final String crc32File;
    protected final String propertyFile;
    protected final String startScript;
    protected final String startScriptArguments;
    protected final String jppfFolder;
    
    protected final LoginCredentials loginCredentials;
    protected final ComputeServiceContext context;
    protected final NodeMetadata metadata;
    protected final AmazonNodeConfiguration nodeConfiguration;

    private SshClient sshClient = null;
    private String directoryName = null;
    
    static protected Monitor getDriverMonitor(NodeMetadata metadata) {
        driverMM.enter();
        try {
            Monitor driverMonitor = instanceDriverMonitors.get(metadata.getId());
            if (driverMonitor == null) {
                driverMonitor = new Monitor(false);
                instanceDriverMonitors.put(metadata.getId(), driverMonitor);
            }
            return driverMonitor;
        } finally {
            driverMM.leave();
        }
    }
    
    static protected Monitor getNodeMonitor(NodeMetadata metadata) {
        nodeMM.enter();
        try {
            Monitor nodeMonitor = instanceNodeMonitors.get(metadata.getId());
            if (nodeMonitor == null) {
                nodeMonitor = new Monitor(false);
                instanceNodeMonitors.put(metadata.getId(), nodeMonitor);
            }
            return nodeMonitor;
        } finally {
            nodeMM.leave();
        }
    }
    
    static void removeDriverMonitor(String instanceId) {
        driverMM.enter();
        try {
            instanceDriverMonitors.remove(instanceId);
        } finally {
            driverMM.leave();
        }
    }
    
    static void removeNodeMonitor(String instanceId) {
        nodeMM.enter();
        try {
            instanceNodeMonitors.remove(instanceId);
        } finally {
            nodeMM.leave();
        }
    }
    
    public AmazonEC2JPPFDeployer(LoginCredentials loginCredentials,
            ComputeServiceContext context, NodeMetadata metadata,
            AmazonNodeConfiguration nodeConfiguration, String zipFile, 
            String crc32File, String propertyFile, String startScript, 
            String startScriptArguments, String jppfFolder) {
        this.loginCredentials = loginCredentials;
        this.context = context;
        this.metadata = metadata;
        this.nodeConfiguration = nodeConfiguration;
        this.zipFile = zipFile;
        this.crc32File = crc32File;
        this.propertyFile = propertyFile;
        this.startScript = startScript;
        this.startScriptArguments = startScriptArguments;
        this.jppfFolder = jppfFolder;
    }
    
    protected abstract void checkPrecondition() throws Throwable;

    protected abstract Properties getSettings();
    
    protected abstract Monitor getMonitor();
    
    public String deploy() {
        String uuid = null;
        SshClient ssh = getSSHClient();
        ssh.connect();
        JCloudsSshClientWrapper jCloudsSshClientWrapper = 
                new JCloudsSshClientWrapper(sshClient, metadata.getLoginPort());
        RemoteResourceManager remoteResourceManager = 
                new RemoteResourceManager(jCloudsSshClientWrapper);
        Resource jppfZipResource = new InputStreamResource(
                String.format("/%s", zipFile), this.getClass(), zipFile, getDirectoryName());
        jppfZipResource.setUnzipContents(true);
        remoteResourceManager.addResource(jppfZipResource);
        try {
            checkPrecondition();
            final String nodeTypeStr = nodeConfiguration.getType().toString();
            logger.debug("Deploying JPPF-{} to {} ({}).", 
                    new Object[]{nodeTypeStr, metadata.getId(), getPublicIp()});

            prepareJPPF(remoteResourceManager);
            sendEvent(Event.DEPLOYMENT_PREPARED);
            Monitor monitor = getMonitor();
            monitor.enter();
            try {
                remoteResourceManager.uploadResources();
                
                for(File artifact : nodeConfiguration.getArtifactsToPreload()) {
                    remoteResourceManager.addResource(
                            new FileResource(artifact, getDirectoryName() + jppfFolder + "lib"));
                }
                remoteResourceManager.uploadResources();
                remoteResourceManager.deployResources();
                sendEvent(Event.RESOURCES_PRELOADED);
            } finally {
                monitor.leave();
            }
            setupJPPF();
            uploadConfiguration(getSettings());
            sendEvent(Event.JPPF_CONFIGURATED);

            logger.debug("Starting JPPF-{} on {}...", nodeTypeStr, metadata.getId());
            startJPPF();
            uuid = getUUID();
            sendEvent(Event.DEPLOYMENT_FINISHED);
            logger.debug("JPPF-{} deployed on {}.", nodeTypeStr, metadata.getId());
        } catch(Throwable ex){
            logger.debug("Deployment of JPPF-{} failed.", nodeConfiguration.getType().toString());
            throw new IllegalStateException(ex);
        } finally {
            if(ssh != null) {
                ssh.disconnect();
            }
        }
        return uuid;
    }

    protected void sendEvent(Event event) {
        setChanged();
        notifyObservers(event);
    }
    
    protected void prepareJPPF(RemoteResourceManager remoteResourceManager) {
        execute(String.format("rm -rf %s", getDirectoryName()));
        try {
            remoteResourceManager.prepareResourceDirectory();
        } catch (SSHClientException ex) {
            logger.error("Could not prepare resource manager.", ex);
        }
    }

    protected ExecResponse execute(String command) {
        logger.trace("Executing {}", command);
        ExecResponse response = getSSHClient().exec(command);
        return logExecResponse(response);
    }

    protected ExecResponse logExecResponse(ExecResponse response) {
        logger.trace("Exit Code: {}", response.getExitStatus());
        if (response.getError() != null && !response.getError().isEmpty()) {
            logger.warn("Execution error: {}.", response.getError());
        }
        
        return response;
    }
    
    protected String getStringResult(ExecResponse response) {
        return response.getOutput().trim();
    }
    
    protected void setupJPPF() {
        execute("chmod +x " + getDirectoryName() + jppfFolder + startScript);
    }

    protected void upload(InputStream source, String to) {
        logger.debug("Uploading {}", to);
        getSSHClient().put(to, Payloads.newInputStreamPayload(source));
    }

    protected Properties getPropertiesFromStream(InputStream properties) {
        Properties nodeProperties = new Properties();
        try {
            nodeProperties.load(properties);
        } catch (IOException ex) {
            logger.warn("Can not read properties file.", ex);
        }
        return nodeProperties;
    }

    protected InputStream getRunningConfig(Properties properties) {
        ByteArrayOutputStream runningConfig = new ByteArrayOutputStream();
        try {
            properties.store(runningConfig, "Running Config");
        } catch (IOException ex) {
            logger.warn("Can not write running property configuration.", ex);
        }

        return new ByteArrayInputStream(runningConfig.toByteArray());
    }
    
    protected String getDirectoryName() {
        if(directoryName != null) {
            return directoryName;
        }
        directoryName = "jppf-" + nodeConfiguration.getType().toString().toLowerCase() + "-" + 
                metadata.getId().replace("/", "_") + "_" + nodeConfiguration.getManagementPort();
        return directoryName;
    }
    
    protected String getPrivateIp() {
        String privateIp = Iterables.getFirst(metadata.getPrivateAddresses(), null);
        checkState(privateIp != null, "No private IP set.");
        return privateIp;
    }

    protected String getPublicIp() {
        String publicIp = Iterables.getFirst(metadata.getPublicAddresses(), null);
        checkState(publicIp != null, "No public IP set.");
        return publicIp;
    }

    protected SshClient getSSHClient() {
        if(sshClient == null) {
            sshClient = context.utils().sshForNode().apply(
                NodeMetadataBuilder.fromNodeMetadata(metadata).
                credentials(loginCredentials).build());
        }
        return sshClient;
    }

    protected void uploadConfiguration(Properties nodeProperties) {
        logger.debug("Uploading config {}.", propertyFile);
        upload(getRunningConfig(nodeProperties), getDirectoryName() + propertyFile);
    }

    protected String startJPPF() {
        final StringBuilder script = new StringBuilder("cd /home/ec2-user/").
                append(getDirectoryName()).
                append(jppfFolder).
                append(" && nohup ./").
                append(startScript);
        if(startScriptArguments != null && !startScriptArguments.isEmpty()) {
            script.append(" ").append(startScriptArguments);
        }
        script.append(" > ").
                append(INIT_LOG).
                append(" 2>&1");
        return execute(script.toString()).getOutput();
    }

    protected void closeInputstream(final InputStream in) {
        try {
            in.close();
        } catch (IOException ex) {
            logger.warn("Could not close Inputstream.", ex);
        }
    }
    
    protected String getUUID() {
        logger.debug("Fetching UUID from {}", INIT_LOG);
        String output = getStringResult(execute("cat " + getDirectoryName() + 
                jppfFolder + INIT_LOG + " | grep " + UUID_PREFIX));
        checkNotNull(output);
        checkState(output.contains(UUID_PREFIX));
        int beginIndex = output.indexOf(UUID_PREFIX) + UUID_PREFIX.length();
        int endIndex = beginIndex + 32;
        String uuid = output.substring(beginIndex, endIndex);
        logger.debug("Got UUID {}.", uuid);
        
        return uuid;
    }
}
