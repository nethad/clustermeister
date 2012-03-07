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

import com.google.common.base.Charsets;
import static com.google.common.base.Preconditions.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.ssh.SshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
public class AmazonEC2JPPFDriverDeployer extends AmazonEC2JPPFDeployer {
    static final String DRIVER_ZIP_FILE = "jppf-driver.zip";

    private final static Logger logger =
            LoggerFactory.getLogger(AmazonEC2JPPFDriverDeployer.class);

    public AmazonEC2JPPFDriverDeployer(ComputeServiceContext context,
            NodeMetadata metadata, LoginCredentials credentials,
            AmazonNodeConfiguration nodeConfiguration) {
        super(credentials, context, metadata, nodeConfiguration);
    }

    @Override
    public void run() {
        final String publicIp = metadata.getPublicAddresses().iterator().next();
        final String privateIp = metadata.getPrivateAddresses().iterator().next();
        logger.info("Deploying JPPF-Driver to {} ({}).", metadata.getId(), publicIp);
        Properties nodeProperties = getSettings(privateIp, 
                nodeConfiguration.getManagementPort());

        SshClient client = context.utils().sshForNode().apply(
                NodeMetadataBuilder.fromNodeMetadata(metadata).
                credentials(loginCredentials).build());
        client.connect();
        try {
            final String folderName = getFolderName();
            final String crcFile = CLUSTERMEISTER_BIN + "/" + CRC32_FILE_DRIVER;
            Long checksum = getChecksum(DRIVER_ZIP_FILE);
            checkNotNull(checksum, "Checksum is null.");
            
            if(getUploadNecessary(crcFile, client, checksum)) {
                uploadAndSetupDriver(folderName, client, crcFile, checksum);
            }
            logger.debug("Uploading driver config.");
            upload(client, getRunningConfig(nodeProperties),
                    folderName + "/jppf-driver/config/jppf-driver.properties");

            logger.info("Starting JPPF-Driver on {}...", metadata.getId());
            final String script = "cd /home/ec2-user/" + folderName + "/jppf-driver\nnohup ./startDriver.sh > nohup.out 2>&1";
            RunScriptOptions options = new RunScriptOptions().overrideLoginPrivateKey(
                    loginCredentials.getPrivateKey()).
                    overrideLoginUser(loginCredentials.getUser()).
                    blockOnComplete(false).
                    runAsRoot(false).
                    nameTask(folderName + "-start");
            logExecResponse(context.getComputeService().
                    runScriptOnNode(metadata.getId(), script, options));
            logger.info("JPPF-Driver started.");
        } catch(IOException ex) {
            logger.warn("Can not close input stream.", ex);
        } finally {
            if (client != null) {
                client.disconnect();
            }
        }
        logger.info("JPPF-Driver deployed on {}.", metadata.getId());
    }

    private void uploadAndSetupDriver(final String folderName, SshClient client, 
            String crcFile, long checksum) throws IOException {
        logger.debug("Uploading {}", DRIVER_ZIP_FILE);
        execute("rm -rf " + folderName + " && mkdir " + CLUSTERMEISTER_BIN, client);
        final InputStream driverPackage = 
                    getClass().getResourceAsStream(DRIVER_ZIP_FILE);
        upload(client, driverPackage, "/home/ec2-user/" + CLUSTERMEISTER_BIN + "/" + 
                DRIVER_ZIP_FILE);
        driverPackage.close();
        upload(client, new ByteArrayInputStream(
                String.valueOf(checksum).getBytes(Charsets.UTF_8)), crcFile);
        execute("unzip " + CLUSTERMEISTER_BIN + "/" + DRIVER_ZIP_FILE + " -d " + 
                folderName + " && chmod +x " + folderName + 
                "/jppf-driver/startDriver.sh", client);
    }

    private Properties getSettings(String managementHost, int managementPort) {
        Properties nodeProperties = getPropertiesFromStream(
                this.getClass().getResourceAsStream("jppf-driver.properties"));
        nodeProperties.setProperty("jppf.management.host", managementHost);
        nodeProperties.setProperty("jppf.management.port", String.valueOf(managementPort));
        return nodeProperties;
    }
}