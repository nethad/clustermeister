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

import com.github.nethad.clustermeister.api.NodeCapabilities;
import static com.google.common.base.Preconditions.*;
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
public class AmazonEC2JPPFNodeDeployer extends AmazonEC2JPPFDeployer {
    protected static final String JPPF_PROCESSING_THREADS = "processing.threads";
    protected static final String JPPF_SERVER_HOST = "jppf.server.host";
    private static final String ZIP_FILE = "jppf-node.zip";
    private static final String CRC32_FILE = CLUSTERMEISTER_BIN + "/jppf-node-crc-32";
    private static final String JPPF_FOLDER = "/jppf-node/";
    private static final String PROPERTY_FILE_NAME = "jppf-node.properties";
    private static final String PROPERTY_FILE_SUBPATH = JPPF_FOLDER + "config/" + PROPERTY_FILE_NAME;
    private static final String START_SCRIPT = "startNode.sh";
    private static final String START_SCRIPT_ARGUMENTS = "jppf-node.properties false";
    
    public AmazonEC2JPPFNodeDeployer(ComputeServiceContext context,
            NodeMetadata metadata, LoginCredentials credentials,
            AmazonNodeConfiguration nodeConfiguration) {
        super(credentials, context, metadata, nodeConfiguration, ZIP_FILE, 
                CRC32_FILE, PROPERTY_FILE_SUBPATH, START_SCRIPT, 
                START_SCRIPT_ARGUMENTS, JPPF_FOLDER);
    }

    @Override
    protected void checkPrecondition() throws Throwable {
        checkState(nodeConfiguration.getDriverAddress() != null, 
                "No driver address set.");
    }

    @Override
    protected Monitor getMonitor() {
        return getNodeMonitor(metadata);
    }

    @Override
    protected Properties getSettings() {
        final InputStream in = this.getClass().getResourceAsStream(PROPERTY_FILE_NAME);
        try {
            Properties nodeProperties = getPropertiesFromStream(in);
            nodeProperties.setProperty(JPPF_SERVER_HOST, nodeConfiguration.getDriverAddress());
            nodeProperties.setProperty(JPPF_MANAGEMENT_HOST, getPrivateIp());
            nodeProperties.setProperty(JPPF_MANAGEMENT_PORT, 
                    String.valueOf(nodeConfiguration.getManagementPort()));
            NodeCapabilities nodeCapabilities = nodeConfiguration.getNodeCapabilities();
            checkState(nodeCapabilities != null && !(nodeCapabilities.getNumberOfProcessingThreads() < 1),
                    "Invalid processing threads capability setting.");
            nodeProperties.setProperty(JPPF_PROCESSING_THREADS, 
                    String.valueOf(nodeCapabilities.getNumberOfProcessingThreads()));
            return nodeProperties;
        } finally {
            closeInputstream(in);
        }
    }
}
