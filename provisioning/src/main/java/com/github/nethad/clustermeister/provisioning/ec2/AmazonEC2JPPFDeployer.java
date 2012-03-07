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
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Iterables;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.io.Payloads;
import org.jclouds.ssh.SshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
public abstract class AmazonEC2JPPFDeployer implements Runnable {

    private final static Logger logger =
            LoggerFactory.getLogger(AmazonEC2JPPFDeployer.class);
    
    static final String CLUSTERMEISTER_BIN = "clustermeister-bin";
    
    final LoginCredentials loginCredentials;
    final ComputeServiceContext context;
    final NodeMetadata metadata;
    final AmazonNodeConfiguration nodeConfiguration;

    private SshClient sshClient = null;
    
    public AmazonEC2JPPFDeployer(LoginCredentials loginCredentials,
            ComputeServiceContext context, NodeMetadata metadata,
            AmazonNodeConfiguration nodeConfiguration) {
        this.loginCredentials = loginCredentials;
        this.context = context;
        this.metadata = metadata;
        this.nodeConfiguration = nodeConfiguration;
    }

    protected ExecResponse execute(String command, SshClient client) {
        logger.info("Executing {}", command);
        ExecResponse response = client.exec(command);
        logExecResponse(response);
        return response;
    }

    protected void logExecResponse(ExecResponse response) {
        logger.info("Exit Code: {}", response.getExitCode());
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

    protected void upload(SshClient client, InputStream source, String to) {
        logger.info("Uploading {}", to);
        client.put(to, Payloads.newInputStreamPayload(source));
        if (source != null) {
            try {
                source.close();
            } catch (IOException ex) {
                logger.warn("Could not close inputstream. Continuing...", ex);
            }
        }
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

    protected ByteArrayInputStream getRunningConfig(Properties properties) {
        ByteArrayOutputStream runningConfig = new ByteArrayOutputStream();
        try {
            properties.store(runningConfig, "Running Config");
        } catch (IOException ex) {
            logger.warn("Can not write running property configuration.", ex);
        }

        return new ByteArrayInputStream(runningConfig.toByteArray());
    }
    
    protected String getFolderName() {
        return "jppf-" + nodeConfiguration.getType().toString().toLowerCase() + "-" + 
                metadata.getId().replace("/", "_") + "_" + nodeConfiguration.getManagementPort();
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
        Long checksum = null;
        final InputStream driverPackage = getClass().getResourceAsStream(filePath);
        try {
            checksum = FileUtils.getCRC32(driverPackage);
            return checksum.longValue();
        } catch (IOException ex) {
            logger.warn("Can not compute CRC32 checksum.", ex);
            checkNotNull(checksum, "Checksum is null.");
            return checksum.longValue();
        } finally {
            try {
                driverPackage.close();
            } catch (IOException ex) {
                logger.warn("Can not close Inputstream.", ex);
            }
        }
    }

    protected boolean getUploadNecessary(final String crcFile, SshClient client, 
            long checksum) throws NumberFormatException {
        boolean crcFileExists = getBoolResult(execute(
                FileUtils.getFileExistsShellCommand(crcFile), client));
        boolean uploadDriver = true;
        if (crcFileExists) {
            long remoteChecksum = Long.parseLong(getStringResult(
                    execute("cat " + crcFile, client)));
            uploadDriver = (remoteChecksum != checksum);
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
}
