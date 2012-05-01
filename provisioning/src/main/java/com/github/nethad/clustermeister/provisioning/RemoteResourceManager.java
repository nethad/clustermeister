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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages {@link Resource}s and their upload and deployment to remote 
 * Clustermeister instances.
 * 
 * This manager helps to upload files or libraries to remote Clustermeister 
 * instances by managing a resource directory on the remote instance where 
 * resources can be uploaded if they are missing or out of date. 
 * 
 * From the remote resource directory, resources can be deployed as defined by 
 * the resources themselves.
 * 
 * This class is not thread safe and remote file system access is not 
 * synchronized by this class.
 *
 * @author daniel
 */
public class RemoteResourceManager {
    
    private static final Logger logger = LoggerFactory.getLogger(RemoteResourceManager.class);
    
    /**
     * Default name of the remote resource directory.
     */
    public static final String DEFAULT_REMOTE_RESOURCES_DIR_NAME = ".cm-resources";
    
    /**
     * Default path separator used to build paths on remote instance.
     */
    public static final String DEFAULT_REMOTE_SEPARATOR = "/";
    
    /**
     * The name of the directory (located in the remote resource directory) 
     * where checksums are stored.
     */
    protected static final String REMOTE_CRC_DIR_NAME = ".crc";
    
    /**
     * The file extension of checksum files.
     */
    protected static final String CRC_FILE_EXTENSION = ".crc";
    
    /**
     * An instance of an {@link SSHClient} used to execute commands and upload 
     * resources.
     * 
     * This instance is expected to be connected already.
     */
    protected final SSHClient sshClient;
    
    /**
     * The remote resource directory where resources are uploaded to.
     */
    protected final String remoteResourcesDir;
    
    /**
     * The remote checksum directory where checksums of uploaded resources are stored.
     */
    protected final String remoteCrcDir;
    
    /**
     * The path separator used to build paths on the remote instance.
     */
    protected final String remoteSeparator;
    
    /**
     * All resources managed by this manager.
     */
    protected List<Resource> managedResources = new LinkedList<Resource>();
    
    /**
     * Creates a new RemoteResourceManager.
     * 
     * @param sshClient 
     *      An already connected instance of an {@link SSHClient} connected to 
     *      the desired Clustermeister instance.
     * @param remoteResourcesDirPath 
     *      The path to the directory containing remote resource directory. 
     *      The path can be absolute or relative. Relative paths start at the 
     *      home directory of the connected SSH user.
     * @param remoteResourcesDirName
     *      The name of the remote resource directory. The location of 
     *      this directory is defined by {@code remoteResourcesDirPath}.
     * @param remoteSeparator The path separator used on the remote instance.
     */
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
    
    /**
     * Creates a new RemoteResourceManager.
     * 
     * The remote resource directory is called '.cm-resources' and located in 
     * the home directory of the connected SSH user.
     * 
     * @param sshClient
     *      An already connected instance of an {@link SSHClient} connected to 
     *      the desired Clustermeister instance.
     */
    public RemoteResourceManager(SSHClient sshClient) {
        this(sshClient, "", DEFAULT_REMOTE_RESOURCES_DIR_NAME, DEFAULT_REMOTE_SEPARATOR);
    }
    
    /**
     * Manage a resource.
     * 
     * Managed resources can be uploaded and deployed to remote Clustermeister 
     * instances.
     * 
     * @param resource a resource to manage.
     */
    public void addResource(Resource resource) {
        managedResources.add(resource);
    }
    
    /**
     * Remove a managed resource.
     * 
     * Remove a resource from management. This does not delete the resource on 
     * the remote instance.
     * 
     * @param resource a managed resource. 
     */
    public void removeResource(Resource resource) {
        managedResources.remove(resource);
    }
    
    /**
     * Upload all managed resources to the resource directory on the remote instance.
     * 
     * Resources already uploaded by this manager instance are not re-uploaded.
     * Resources with the same file name already existing in the remote resource 
     * directory are only uploaded if their checksums do not match.
     */
    public void uploadResources() {
        for(Resource resource : managedResources) {
            try {
                uploadResource(resource);
            } catch (IOException ex) {
                logger.warn("Failed to upload {} to ssh://{}:{}/{}.", 
                        new Object[]{resource.getName(), sshClient.getHost(), 
                            sshClient.getPort(), remoteResourcesDir, ex});
            } catch (SSHClientException ex) {
                logger.warn("Failed to upload {} to ssh://{}:{}/{}.", 
                        new Object[]{resource.getName(), sshClient.getHost(), 
                            sshClient.getPort(), remoteResourcesDir, ex});
            }
        }
    }
    
    /**
     * Deploys all managed resources from the remote resource directory to the 
     * directory specified by the respective resource.
     * 
     * Resources already deployed by this manager instance are not re-deployed.
     * Resources already existing in the resource's deployment directory are 
     * overwritten.
     */
    public void deployResources() {
        StringBuilder command = new StringBuilder();
        List<Resource> deployedResources = 
                new ArrayList<Resource>(managedResources.size());
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
                deployedResources.add(resource);
            }
        }
        
        try {
            execute(command.toString());
            for(Resource resource : deployedResources) {
                logger.info("Deployed {} to ssh://{}:{}/{}", 
                        new Object[]{resource.getName(), sshClient.getHost(), 
                            sshClient.getPort(), resource.getRemoteDeploymentDirectory()});
                resource.setDeployed(true);
            }
        } catch(SSHClientException ex) {
            logger.warn("Error deploying resources {} to ssh://{}:{}.", 
                    new Object[]{deployedResources, sshClient.getHost(), 
                            sshClient.getPort(), ex});
        }
    }
    
    /**
     * Creates the configured resource directory on the remote instance.
     * 
     * @throws SSHClientException If there is a problem with the configured SSH client.
     */
    public void prepareResourceDirectory() throws SSHClientException {
        execute(String.format("mkdir -p %s", remoteCrcDir));
    }
    
    /**
     * Uploads a single resource to the remote Clustermeister instances 
     * resource directory.
     * 
     * @param localResource the locally available resource to upload.
     * @throws IOException  If the data from the resource can not be read.
     * @throws SSHClientException If there is a problem with the configured SSH client.
     */
    protected void uploadResource(Resource localResource) throws IOException, SSHClientException {
        checkNotNull(localResource);
        long localChecksum = localResource.getResourceChecksum();
        if(!localResource.isUploaded() && !isResourceUploadedAndUpToDate(localResource, localChecksum)) {
            String remoteFile = getRemoteFile(localResource);
            logger.info("Uploading {} to ssh://{}:{}/{}.", 
                    new Object[]{localResource, sshClient.getHost(), 
                            sshClient.getPort(), remoteFile});
            InputStream resourceData = localResource.getResourceData();
            sshClient.sftpUpload(resourceData, remoteFile);
            resourceData.close();
            
            String remoteCrcFile = getRemoteCrcFile(localResource);
            logger.debug("Uploading CRC checksum {} to ssh://{}:{}/{}.", 
                    new Object[]{localChecksum, sshClient.getHost(), 
                            sshClient.getPort(), remoteCrcFile});
            byte[] checksumBytes = String.valueOf(localChecksum).
                    getBytes(Charsets.UTF_8);
            sshClient.sftpUpload(new ByteArrayInputStream(checksumBytes), remoteCrcFile);
        }
        
        localResource.setUploaded(true);
    }
    
    /**
     * Checks if a resource exists in the remote resource directory and whether 
     * its checksum matches with the local resource.
     * 
     * @param localResource the local resource to check.
     * @param localChecksum the checksum of the local resource.
     * @return  
     *      True if the resource is exists in the remote resource directory and 
     *      its checksum matches the local resource's checksum. False otherwise.
     * @throws IOException  If the data from the resource can not be read.
     * @throws SSHClientException If there is a problem with the configured SSH client.
     */
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
    
    /**
     * Checks whether a file with specified path exists on the remote 
     * file system.
     * 
     * @param filePath 
     *      The location of the file on the remote instances file system. 
     * @return  
     *      True if the file exists on the remote instances file system, 
     *      false otherwise.
     * @throws SSHClientException If there is a problem with the configured SSH client.
     */
    protected boolean fileExistOnRemote(String filePath) throws SSHClientException {
        String command = FileUtils.getFileExistsShellCommand(filePath);
        final String result = execute(command);
        return Boolean.parseBoolean(result.trim());
    }
    
    /**
     * Execute a command on the remote instance.
     * 
     * @param command   the command to execute.
     * @return  the output of the command executed on the remote instance.
     * @throws SSHClientException If there is a problem with the configured SSH client.
     */
    protected String execute(String command) throws SSHClientException {
        return sshClient.executeWithResultSilent(command);
    }
    
    /**
     * Get the location on the remote instance of the checksum file for this 
     * resource. 
     * 
     * @param resource the resource
     * @return the path to the checksum file on the remote instances file system.
     */
    protected String getRemoteCrcFile(Resource resource) {
        return String.format("%s%s%s%s", remoteCrcDir, remoteSeparator, 
                resource.getName(), CRC_FILE_EXTENSION);
    }
    
    /**
     * Get the location on the remote instance of the file that this resource 
     * represents.
     * 
     * @param resource the resource
     * @return 
     *      The path to the file this resource represents on the remote 
     *      instances resource directory.
     */
    public String getRemoteFile(Resource resource) {
        return String.format("%s%s%s", remoteResourcesDir, remoteSeparator, 
                resource.getName());
    }
    
}
