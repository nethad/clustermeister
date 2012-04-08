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

import com.github.nethad.clustermeister.provisioning.jppf.JPPFConstants;
import com.google.common.util.concurrent.Monitor;
import java.io.InputStream;
import java.util.Properties;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.domain.LoginCredentials;

/**
 * Do not reuse instances of this class.
 * 
 * @author daniel
 */
public class AmazonEC2JPPFDriverDeployer extends AmazonEC2JPPFDeployer {
    private static final String ZIP_FILE = "jppf-driver.zip";
    private static final String CRC32_FILE = CLUSTERMEISTER_BIN + "/jppf-driver-crc-32";
    private static final String JPPF_FOLDER = "/jppf-driver/";
    private static final String PROPERTY_FILE_NAME = "jppf-driver.properties";
    private static final String PROPERTY_FILE_SUBPATH = JPPF_FOLDER + "config/" + PROPERTY_FILE_NAME;
    private static final String START_SCRIPT = "startDriver.sh";
    private static final String START_SCRIPT_ARGUMENTS = "false";

    public AmazonEC2JPPFDriverDeployer(ComputeServiceContext context,
            NodeMetadata metadata, LoginCredentials credentials,
            AmazonNodeConfiguration nodeConfiguration) {
        super(credentials, context, metadata, nodeConfiguration, ZIP_FILE, 
                CRC32_FILE, PROPERTY_FILE_SUBPATH, START_SCRIPT, 
                START_SCRIPT_ARGUMENTS, JPPF_FOLDER);
    }

    @Override
    protected void checkPrecondition() throws Throwable {
        //no precondition
    }

    @Override
    protected Monitor getMonitor() {
        return getDriverMonitor(metadata);
    }

    @Override
    protected Properties getSettings() {
        final InputStream in = this.getClass().getResourceAsStream(PROPERTY_FILE_NAME);
        try {
            Properties nodeProperties = getPropertiesFromStream(in);
            nodeProperties.setProperty(JPPFConstants.DISCOVERY_ENABLED, "false");
            nodeProperties.setProperty(JPPFConstants.PEER_DISCOVERY_ENABLED, "false");
            nodeProperties.setProperty(JPPFConstants.MANAGEMENT_HOST, getPrivateIp());
            nodeProperties.setProperty(JPPFConstants.MANAGEMENT_PORT, 
                    String.valueOf(nodeConfiguration.getManagementPort()));
            return nodeProperties;
        } finally {
            closeInputstream(in);
        }
    }
}