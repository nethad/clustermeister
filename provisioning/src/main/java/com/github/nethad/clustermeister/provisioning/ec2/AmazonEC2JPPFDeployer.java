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

import com.github.nethad.clustermeister.provisioning.utils.FileUtils;
import com.google.common.base.Charsets;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Monitor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.io.Payloads;
import org.jclouds.ssh.SshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
public abstract class AmazonEC2JPPFDeployer {

    protected final static Logger logger =
            LoggerFactory.getLogger(AmazonEC2JPPFDeployer.class);
    
    protected static final String CLUSTERMEISTER_BIN = "clustermeister-bin";
    
    private static final Monitor driverMM = new Monitor(false);
    private static final Monitor nodeMM = new Monitor(false);
    //TODO: make sure this does not cause a memory leak.
    protected static final Map<String, Monitor> instanceDriverMonitors = new HashMap<String, Monitor>();
    protected static final Map<String, Monitor> instanceNodeMonitors = new HashMap<String, Monitor>();
    protected static final String JPPF_MANAGEMENT_HOST = "jppf.management.host";
    protected static final String JPPF_MANAGEMENT_PORT = "jppf.management.port";
    
    protected final String zipFile;
    protected final String crc32File;
    protected final String propertyFile;
    protected final String startScript;
    protected final String jppfFolder;
    
    protected final LoginCredentials loginCredentials;
    protected final ComputeServiceContext context;
    protected final NodeMetadata metadata;
    protected final AmazonNodeConfiguration nodeConfiguration;

    private SshClient sshClient = null;
    private Long localChecksum = null;
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
            String jppfFolder) {
        this.loginCredentials = loginCredentials;
        this.context = context;
        this.metadata = metadata;
        this.nodeConfiguration = nodeConfiguration;
        this.zipFile = zipFile;
        this.crc32File = crc32File;
        this.propertyFile = propertyFile;
        this.startScript = startScript;
        this.jppfFolder = jppfFolder;
    }
    
    protected abstract void checkPrecondition() throws Throwable;

    protected abstract Properties getSettings();
    
    protected abstract Monitor getMonitor();
    
    public void deploy() {
        SshClient ssh = getSSHClient();
        ssh.connect();
        try {
            checkPrecondition();
            final String nodeTypeStr = nodeConfiguration.getType().toString();
            logger.debug("Deploying JPPF-{} to {} ({}).", 
                    new Object[]{nodeTypeStr, metadata.getId(), getPublicIp()});

            prepareJPPF();
            Monitor monitor = getMonitor();
            monitor.enter();
            try {
                if(getUploadNecessary()) {
                    uploadJPPF();
                }
            } finally {
                monitor.leave();
            }
            setupJPPF();
            uploadConfiguration(getSettings());

            logger.debug("Starting JPPF-{} on {}...", nodeTypeStr, metadata.getId());
            startJPPF();
            logger.debug("JPPF-{} deployed on {}.", nodeTypeStr, metadata.getId());
        } catch(Throwable ex){
            logger.debug("Deployment of JPPF-{} failed.", nodeConfiguration.getType().toString());
            throw new IllegalStateException(ex);
        } finally {
            if(ssh != null) {
                ssh.disconnect();
            }
        }
    }
    
    protected void prepareJPPF() {
        execute("rm -rf " + getDirectoryName() + " && mkdir " + CLUSTERMEISTER_BIN);
    }

    protected ExecResponse execute(String command) {
        logger.trace("Executing {}", command);
        ExecResponse response = getSSHClient().exec(command);
        logExecResponse(response);
        return response;
    }

    protected void logExecResponse(ExecResponse response) {
        logger.trace("Exit Code: {}", response.getExitCode());
        if (response.getError() != null && !response.getError().isEmpty()) {
            logger.warn("Execution error: {}.", response.getError());
        }
    }
    
    protected String getStringResult(ExecResponse response) {
        return response.getOutput().trim();
    }
    
    protected boolean getBoolResult(ExecResponse response) {
        return Boolean.parseBoolean(response.getOutput().trim());
    }

    protected void setupJPPF() {
        execute("unzip " + CLUSTERMEISTER_BIN + "/" + zipFile + " -d " + 
                getDirectoryName() + " && chmod +x " + getDirectoryName() + 
                jppfFolder + startScript);
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

    protected long getChecksum(String filePath) {
        if(localChecksum != null) {
            return localChecksum.longValue();
        }
        final InputStream file = getClass().getResourceAsStream(filePath);
        try {
            localChecksum = FileUtils.getCRC32(file);
            return localChecksum.longValue();
        } catch (IOException ex) {
            logger.warn("Can not compute CRC32 checksum.", ex);
            checkNotNull(localChecksum, "Checksum is null.");
            return localChecksum.longValue();
        } finally {
            closeInputstream(file);
        }
    }

    protected boolean getUploadNecessary() {
        boolean crcFileExists = getBoolResult(execute(
                FileUtils.getFileExistsShellCommand(crc32File)));
        boolean uploadDriver = true;
        if (crcFileExists) {
            try {
                long remoteChecksum = Long.parseLong(getStringResult(
                        execute("cat " + crc32File)));
                uploadDriver = (remoteChecksum != getChecksum(zipFile));
            } catch (NumberFormatException ex) {
                logger.warn("Invalid remote checksum.", ex);
            }
        }
        return uploadDriver;
    }

    protected SshClient getSSHClient() {
        if(sshClient == null) {
            sshClient = context.utils().sshForNode().apply(
                NodeMetadataBuilder.fromNodeMetadata(metadata).
                credentials(loginCredentials).build());
        }
        return sshClient;
    }

    protected void uploadJPPF() {
        logger.debug("Uploading {}", zipFile);
        final InputStream file = getClass().getResourceAsStream(zipFile);
        try {
            upload(file, "/home/ec2-user/" + CLUSTERMEISTER_BIN + "/" + zipFile);
        } finally {
            closeInputstream(file);
        }
        upload(new ByteArrayInputStream(
                String.valueOf(getChecksum(zipFile)).getBytes(Charsets.UTF_8)), crc32File);
    }

    protected void uploadConfiguration(Properties nodeProperties) {
        logger.debug("Uploading config {}.", propertyFile);
        upload(getRunningConfig(nodeProperties), getDirectoryName() + propertyFile);
    }

    protected void startJPPF() {
        final String script = "cd /home/ec2-user/" + getDirectoryName() + jppfFolder +
                " && nohup ./" + startScript + " > nohup.out 2>&1";
        RunScriptOptions options = new RunScriptOptions().overrideLoginPrivateKey(
                loginCredentials.getPrivateKey()).overrideLoginUser(
                loginCredentials.getUser()).blockOnComplete(false).
                runAsRoot(false).nameTask(getDirectoryName() + "-start");
        logExecResponse(context.getComputeService().
                runScriptOnNode(metadata.getId(), script, options));
    }

    protected void closeInputstream(final InputStream in) {
        try {
            in.close();
        } catch (IOException ex) {
            logger.warn("Could not close Inputstream.", ex);
        }
    }
}
