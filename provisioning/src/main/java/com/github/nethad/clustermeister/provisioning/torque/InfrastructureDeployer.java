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

import com.github.nethad.clustermeister.provisioning.FileResource;
import com.github.nethad.clustermeister.provisioning.InputStreamResource;
import com.github.nethad.clustermeister.provisioning.RemoteResourceManager;
import com.github.nethad.clustermeister.provisioning.Resource;
import com.github.nethad.clustermeister.provisioning.utils.SSHClient;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientException;
import com.google.common.annotations.VisibleForTesting;
import java.io.*;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class InfrastructureDeployer {
    
    private static final Logger logger = LoggerFactory.getLogger(InfrastructureDeployer.class);
    
    private static final String RESOURCES_DIR = ".cm-resources";
    
    private static final String JPPF_NODE_DIR = "jppf-node";
    private static final String JPPF_NODE_ZIP_NAME = JPPF_NODE_DIR + ".zip";
    private static final String LOCAL_JPPF_NODE_ZIP_PATH = "/" + JPPF_NODE_ZIP_NAME;
    private static final String REMOTE_LIB_DIR = JPPF_NODE_DIR + "/lib/";
    
    private Collection<File> artifactsToPreload;
    private final SSHClient sshClient;
    @VisibleForTesting
    RemoteResourceManager remoteResourceManager;

    
    public InfrastructureDeployer(SSHClient sshClient) {       
        this(sshClient, new RemoteResourceManager(
                sshClient, "", RESOURCES_DIR, RemoteResourceManager.DEFAULT_REMOTE_SEPARATOR));
    }
    
    public InfrastructureDeployer(SSHClient sshClient, RemoteResourceManager remoteResourceManager) {
        this.sshClient = sshClient;
        
        this.remoteResourceManager = remoteResourceManager;
        Resource jppfZip = new InputStreamResource(
            LOCAL_JPPF_NODE_ZIP_PATH, getClass(), JPPF_NODE_ZIP_NAME, ".");
        jppfZip.setUnzipContents(true);
        this.remoteResourceManager.addResource(jppfZip);
    }

    public void deployInfrastructure(Collection<File> artifactsToPreload) {
        this.artifactsToPreload = artifactsToPreload;
        for (File artifact : artifactsToPreload) {
            remoteResourceManager.addResource(new FileResource(artifact, REMOTE_LIB_DIR));
        }
        
        deleteConfigurationFiles();
        try {
            // remove previously uploaded files (might be outdated/not necessary)
            sshClient.executeAndSysout("rm -rf " + JPPF_NODE_DIR + "*");
            remoteResourceManager.prepareResourceDirectory();
        } catch (SSHClientException ex) {
            logger.warn("SSH Exception", ex);
        }
        remoteResourceManager.uploadResources();
        remoteResourceManager.deployResources();
    }
    
    @VisibleForTesting
    void deleteConfigurationFiles() {
        try {
            // delete config files (separate config files for each node are generated)
            sshClient.executeAndSysout("rm -rf " + JPPF_NODE_DIR + "/config/jppf-node-*.properties");
        } catch (SSHClientException ex) {
            logger.warn("Exception while deleting (old) configuration files.", ex);
        }
    }
    
    @VisibleForTesting
    Collection<File> getArtifactsToPreload() {
        return this.artifactsToPreload;
    }
    
}
