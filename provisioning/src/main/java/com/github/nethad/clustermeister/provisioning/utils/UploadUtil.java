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
package com.github.nethad.clustermeister.provisioning.utils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class UploadUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(UploadUtil.class);
    
    private static final String RESOURCES_DIR = ".cm-resources/";
    
    private static final String JPPF_NODE_DIR = "jppf-node";
    private static final String JPPF_NODE_ZIP_NAME = JPPF_NODE_DIR + ".zip";
    private static final String LOCAL_JPPF_NODE_ZIP_PATH = "/" + JPPF_NODE_ZIP_NAME;
    private static final String REMOTE_LIB_DIR = JPPF_NODE_DIR + "/lib/";
    
    private static final String CRC32_DIR = RESOURCES_DIR;
    private static final String CRC32_SUFFIX = ".crc";
    private static final String JPPF_NODE_CRC32_FILE = CRC32_DIR + JPPF_NODE_ZIP_NAME + CRC32_SUFFIX;
    
    private Collection<File> artifactsToPreload;
    private String deployZipCRC32;
    private HashMap<String, String> artifactCrc32Map = new HashMap<String, String>();
    private final SSHClient sshClient;

    
    public UploadUtil(SSHClient sshClient) {
        this.sshClient = sshClient;
    }

    public void deployInfrastructure(Collection<File> artifactsToPreload) {
        this.artifactsToPreload = artifactsToPreload;
        
        computeCrc();
        deleteConfigurationFiles();
        // remove previously uploaded files (might be outdated/not necessary)
        try {
            sshClient.executeAndSysout("rm -rf " + JPPF_NODE_DIR + "*");
            if (!areAllResourcesAlreadyDeployedAndUpToDate()) {
                logger.info("Resource is not up to date.");
                uploadResources();
            }
        } catch (SSHClientException ex) {
            logger.warn("SSH Exception", ex);
        }
        unpackZipsAndCopyArtifacts();
    }
    
    private void computeCrc() {
        try {
            for (File artifact : artifactsToPreload) {
                artifactCrc32Map.put(crc32PathForFile(artifact), String.valueOf(FileUtils.getCRC32ForFile(artifact)));
            }
            deployZipCRC32 = String.valueOf(FileUtils.getCRC32(getResourceStream(LOCAL_JPPF_NODE_ZIP_PATH)));
            artifactCrc32Map.put(JPPF_NODE_CRC32_FILE, deployZipCRC32);
        } catch (IOException ex) {
            logger.warn("Exception while computing CRC sum.");
        }
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
    
    private String crc32PathForFile(File file) {
        return String.format("%s%s%s", CRC32_DIR, file.getName(), CRC32_SUFFIX);
    }
    
    private boolean areAllResourcesAlreadyDeployedAndUpToDate() {       
        try {
            for (Map.Entry<String, String> entry : artifactCrc32Map.entrySet()) {
                if (!doesFileExistOnRemote(entry.getKey())) {
                    logger.info("CRC32 {} is missing.", entry.getKey());
                    return false;
                }
                String remoteCrc32 = sshClient.executeWithResultSilent("cat "+entry.getKey());
                if (!entry.getValue().equals(remoteCrc32)) {
                    logger.info("CRC32 for {} does not match.", entry.getKey());
                    return false;
                }
            }
        } catch (SSHClientException ex) {
            logger.error("SSH exception", ex);
        }
        return true;
    }
    
    private boolean doesFileExistOnRemote(String filePath) {
        try {
            String command = FileUtils.getFileExistsShellCommand(filePath);
            final String result = sshClient.executeWithResultSilent(command);
            return Boolean.parseBoolean(result);
        } catch (SSHClientException ex) {
            logger.error("SSH exception", ex);
        }
        return false;
    }
    
    private void uploadResources() throws SSHClientException {
        
        sshClient.executeAndSysout("mkdir -p " + CRC32_DIR);
        sshClient.sftpUpload(getResourceStream(LOCAL_JPPF_NODE_ZIP_PATH), RESOURCES_DIR + JPPF_NODE_ZIP_NAME);
        sshClient.sftpUpload(new ByteArrayInputStream(deployZipCRC32.getBytes(Charsets.UTF_8)), JPPF_NODE_CRC32_FILE);
//        sshClient.executeAndSysout("unzip " + JPPF_NODE_ZIP_NAME);

        uploadArtifacts();
    }
        
    private void uploadArtifacts() throws SSHClientException {
        for (File artifact : artifactsToPreload) {
            if (isResourceDeployedAndUpToDate(artifact)) {
                continue;
            }
            try {
                String remoteArtifactPath = RESOURCES_DIR + artifact.getName();
                logger.info("Uploading to {}", remoteArtifactPath);
                sshClient.sftpUpload(new FileInputStream(artifact), remoteArtifactPath);

                String remoteCrc32Path = crc32PathForFile(artifact);
                String crc32 = artifactCrc32Map.get(remoteCrc32Path);
                logger.info("Uploading CRC32 to {}", remoteCrc32Path);
                sshClient.sftpUpload(new ByteArrayInputStream(crc32.getBytes(Charsets.UTF_8)), remoteCrc32Path);
            } catch (FileNotFoundException ex) {
                logger.warn("Artifact file {} does not exist.", artifact.getAbsolutePath(), ex);
            } catch (IOException ex) {
                logger.warn("Exception while computing CRC32 for file {}", artifact.getAbsolutePath(), ex);
            }
        }
    }
    
    private boolean isResourceDeployedAndUpToDate(File file) {
        boolean fileExistsOnRemote = doesFileExistOnRemote(RESOURCES_DIR + file.getName());
        if (fileExistsOnRemote) {
            String remoteCrc32Path = crc32PathForFile(file);
            String localCrc32 = artifactCrc32Map.get(remoteCrc32Path);
            String remoteCrc32;
            try {
                remoteCrc32 = sshClient.executeWithResultSilent("cat "+remoteCrc32Path);
            } catch (SSHClientException ex) {
                logger.warn("Could not check remote CRC32 file.", ex);
                return false;
            }
            return localCrc32.equals(remoteCrc32);
        } else {
            return false;
        }
    }
    
    private void unpackZipsAndCopyArtifacts() {
        try {
            sshClient.executeAndSysout("unzip -o " + RESOURCES_DIR + JPPF_NODE_ZIP_NAME);
            logger.info("Copying " + artifactsToPreload.size() + " artifacts...");
            for (File file : artifactsToPreload) {
                sshClient.executeWithResult("cp " + RESOURCES_DIR + file.getName() + " " + REMOTE_LIB_DIR);
            }
        } catch (SSHClientException ex) {
            logger.warn("Exception while unpacking and copying resources.", ex);
        }
    }
    
    private InputStream getResourceStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }

    @VisibleForTesting
    Collection<File> getArtifactsToPreload() {
        return this.artifactsToPreload;
    }
    
}
