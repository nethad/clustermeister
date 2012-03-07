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

import java.util.Properties;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
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
    static final String CRC32_FILE_DRIVER = "jppf-driver-crc-32";
    static final String START_SCRIPT = "/jppf-driver/startDriver.sh";

    private final static Logger logger =
            LoggerFactory.getLogger(AmazonEC2JPPFDriverDeployer.class);

    public AmazonEC2JPPFDriverDeployer(ComputeServiceContext context,
            NodeMetadata metadata, LoginCredentials credentials,
            AmazonNodeConfiguration nodeConfiguration) {
        super(credentials, context, metadata, nodeConfiguration);
    }

    @Override
    public void run() {
        final String publicIp = getPublicIp();
        final String privateIp = getPrivateIp();
        logger.info("Deploying JPPF-Driver to {} ({}).", metadata.getId(), publicIp);
        Properties nodeProperties = getSettings(privateIp, 
                nodeConfiguration.getManagementPort());

        SshClient client = getSSHClient();
        client.connect();
        try {
            final String folderName = getFolderName();
            final String crcFile = CLUSTERMEISTER_BIN + "/" + CRC32_FILE_DRIVER;
            long checksum = getChecksum(DRIVER_ZIP_FILE);
            
            if(getUploadNecessary(crcFile, client, checksum)) {
                uploadAndSetup(folderName, crcFile, checksum, DRIVER_ZIP_FILE, START_SCRIPT);
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
        } finally {
            if (client != null) {
                client.disconnect();
            }
        }
        logger.info("JPPF-Driver deployed on {}.", metadata.getId());
    }

    private Properties getSettings(String managementHost, int managementPort) {
        Properties nodeProperties = getPropertiesFromStream(
                this.getClass().getResourceAsStream("jppf-driver.properties"));
        nodeProperties.setProperty("jppf.management.host", managementHost);
        nodeProperties.setProperty("jppf.management.port", String.valueOf(managementPort));
        return nodeProperties;
    }
}