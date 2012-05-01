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
package com.github.nethad.clustermeister.provisioning;

import com.github.nethad.clustermeister.provisioning.utils.FileUtils;
import com.github.nethad.clustermeister.provisioning.utils.SSHClient;
import com.github.nethad.clustermeister.provisioning.utils.SSHClientException;
import com.google.common.base.Charsets;
import static com.google.common.base.Preconditions.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
public class RemoteResourceManager {
    
    private static final Logger logger = LoggerFactory.getLogger(RemoteResourceManager.class);
    
    public static final String DEFAULT_REMOTE_RESOURCES_DIR_NAME = ".cm-resources";
    
    public static final String DEFAULT_REMOTE_SEPARATOR = "/";
    
    protected static final String REMOTE_CRC_DIR_NAME = ".crc";
    
    protected static final String CRC_FILE_EXTENSION = ".crc";
    
    protected final SSHClient sshClient;
    
    protected final String remoteResourcesDir;
    
    protected final String remoteCrcDir;
    
    protected final String remoteSeparator;
    
    protected List<Resource> managedResources = new LinkedList<Resource>();
    
    public RemoteResourceManager(SSHClient sshClient, String remoteResourcesDirPath, 
            String remoteResourcesDirName, String remoteSeparator) {
        this.sshClient = sshClient;
        this.remoteSeparator = remoteSeparator;
        if(remoteResourcesDirPath == null || remoteResourcesDirPath.isEmpty()) {
            this.remoteResourcesDir = String.format("%s", remoteResourcesDirName);
        } else {
            this.remoteResourcesDir = String.format("%s%s%s", 
                    remoteResourcesDirPath, remoteSeparator, remoteResourcesDirName);
            
        }
        this.remoteCrcDir = String.format("%s%s%s", 
                remoteResourcesDir, remoteSeparator, REMOTE_CRC_DIR_NAME);
    }
    
    public void addResource(Resource resource) {
        managedResources.add(resource);
    }
    
    public void removeResource(Resource resource) {
        managedResources.remove(resource);
    }
    
    public void uploadResources() {
        for(Resource resource : managedResources) {
            try {
                uploadResource(resource);
            } catch (IOException ex) {
                logger.warn("Failed to upload {}.", resource.getName(), ex);
            } catch (SSHClientException ex) {
                logger.warn("Failed to upload {}.", resource.getName(), ex);
            }
        }
    }
    
    public void deployResources() {
        StringBuilder command = new StringBuilder();
        for(Resource resource : managedResources) {
            checkNotNull(resource);
            if(resource.isUploaded() && !resource.isDeployed()) {

                String remoteFile = getRemoteFile(resource);
                if(resource.isUnzipContents()) {
                    command.append("unzip ");
                    command.append(remoteFile);
                    command.append(" -d ");
                    command.append(resource.getRemoteDeploymentDirectory());
                    command.append(";");
                } else {
                    command.append("cp ");
                    command.append(remoteFile);
                    command.append(" ");
                    command.append(resource.getRemoteDeploymentDirectory());
                    command.append(";");
                }
                resource.setDeployed(true);
            }
        }
        
        try {
            logger.info("Deploying {}", managedResources);
            execute(command.toString());
        } catch(SSHClientException ex) {
            logger.warn("Error deploying resources.", ex);
        }
    }
    
    public void prepareResourceDirectory() throws SSHClientException {
        execute(String.format("mkdir -p %s", remoteCrcDir));
    }
    
    protected void uploadResource(Resource localResource) throws IOException, SSHClientException {
        checkNotNull(localResource);
        long localChecksum = localResource.getResourceChecksum();
        if(!localResource.isUploaded() && !isResourceUploadedAndUpToDate(localResource, localChecksum)) {
            String remoteFile = getRemoteFile(localResource);
            logger.info("Uploading {} to remote file {}.", 
                    localResource, remoteFile);
            InputStream resourceData = localResource.getResourceData();
            sshClient.sftpUpload(resourceData, remoteFile);
            resourceData.close();
            
            String remoteCrcFile = getRemoteCrcFile(localResource);
            logger.debug("Uploading CRC checksum {} to remote file {}.", 
                    localChecksum, remoteCrcFile);
            byte[] checksumBytes = String.valueOf(localChecksum).
                    getBytes(Charsets.UTF_8);
            sshClient.sftpUpload(new ByteArrayInputStream(checksumBytes), remoteCrcFile);
        }
        
        localResource.setUploaded(true);
    }
    
    protected boolean isResourceUploadedAndUpToDate(Resource localResource, long localChecksum) 
            throws SSHClientException, IOException {
        
        boolean fileExistsOnRemote = fileExistOnRemote(String.format("%s%s%s", 
                remoteResourcesDir, remoteSeparator, localResource.getName()));
        if (fileExistsOnRemote) {
            String remoteCrcFile = getRemoteCrcFile(localResource);
            long remoteCrc = Long.parseLong(execute(String.format("cat %s", remoteCrcFile)));
            
            return localChecksum == remoteCrc;
        } else {
            return false;
        }
    }
    
    protected boolean fileExistOnRemote(String filePath) throws SSHClientException {
        String command = FileUtils.getFileExistsShellCommand(filePath);
        final String result = execute(command);
        return Boolean.parseBoolean(result.trim());
    }
    
    protected String execute(String command) throws SSHClientException {
//        logger.info("Executing: {}", command);
        return sshClient.executeWithResultSilent(command);
    }
    
    protected String getRemoteCrcFile(Resource resource) {
        return String.format("%s%s%s%s", remoteCrcDir, remoteSeparator, 
                resource.getName(), CRC_FILE_EXTENSION);
    }
    
    public String getRemoteFile(Resource resource) {
        return String.format("%s%s%s", remoteResourcesDir, remoteSeparator, 
                resource.getName());
    }
    
}
