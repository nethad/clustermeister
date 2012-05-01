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

import com.google.common.base.Objects;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.lang3.builder.EqualsBuilder;

/**
 * Represents a resource that can be uploaded to and deployed on remote 
 * Clustermeister instances.
 * 
 * Typical examples are library jars or zip files that are needed to set up a 
 * remote JPPF node.
 * 
 * A resource defines a checksum test to check if an upload is necessary and a 
 * deployment method that defines how and where a resource is deployed on a 
 * remote instance.
 *
 * @author daniel
 */
public abstract class Resource {
    private final String name;
    private final String remoteDeploymentDirectory;
    private boolean uploaded = false;
    private boolean deployed = false;
    private boolean unzipContents = false;

    /**
     * Creates a new resource.
     * 
     * @param name  The name of this resource. Typically the name of the file.
     * @param remoteDeploymentDirectory 
     *      Where to deploy this resource on a remote Clustermeister instance. 
     *      The directory can be absolute or relative to the home directory of 
     *      the configured SSH user.
     */
    public Resource(String name, String remoteDeploymentDirectory) {
        this.name = name;
        this.remoteDeploymentDirectory = remoteDeploymentDirectory;
    }

    /**
     * Opens an input stream containing the resources data.
     * 
     * The input stream should be opened at the time of calling this method and 
     * will be closed by the {@link RemoteResourceManager} after use.
     * 
     * @return an InputStream containing the resource data.
     * @throws IOException If the InputStream can not be created or read from.
     */
    public abstract InputStream getResourceData() throws IOException;
    
    /**
     * Returns a CRC checksum of this resources data. This is used to decide 
     * whether a resource already uploaded to a remote instance is up to date 
     * or a new upload is needed.
     * 
     * @return  the CRC checksum.
     * @throws IOException When the resource can not be read from.
     */
    public abstract long getResourceChecksum() throws IOException;

    /**
     * Returns the resources name.
     * 
     * @return  the resource's name, typically the file name. 
     */
    public String getName() {
        return name;
    }

    /**
     * The directory on the remote instance where this resource is to be deployed.
     * 
     * The directory can be absolute or relative to the home directory of 
     * the configured SSH user.
     * 
     * @return the path to the directory where the resource is to be deployed.
     */
    public String getRemoteDeploymentDirectory() {
        return remoteDeploymentDirectory;
    }

    /**
     * Set whether to unzip or copy the resource to its deployment directory.
     * 
     * @param unzipContents 
     *      If true, the resource is unzipped to its deployment directory using 
     *      the 'unzip' command. If false (default), the resource is copied to 
     *      its deployment directory.
     */
    public void setUnzipContents(boolean unzipContents) {
        this.unzipContents = unzipContents;
    }
    
    /**
     * Checks whether a resource is configured to be unzipped or copied to its 
     * deployment directory.
     * 
     * @return true if the resource is configured to be unzipped, false otherwise.
     */
    public boolean isUnzipContents() {
        return unzipContents;
    }

    /**
     * Sets the uploaded flag on this resource that marks that it has 
     * been uploaded successfully.
     * 
     * NOTE: for internal use only.
     * 
     * @param uploaded true to mark the resource as uploaded.
     */
    void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }

    /**
     * Checks whether the resource is marked as uploaded.
     * 
     * NOTE: for internal use only.
     * 
     * @return true if the resource is marked as uploaded, false otherwise.
     */
    boolean isUploaded() {
        return uploaded;
    }

    /**
     * Sets the deployed flag on this resource that marks that it has been 
     * deployed successfully.
     * 
     * NOTE: for internal use only.
     * 
     * @param deployed true to mark the resource as deployed.
     */
    void setDeployed(boolean deployed) {
        this.deployed = deployed;
    }

    /**
     * Checks whether the resource is marked as deployed.
     * 
     * NOTE: for internal use only.
     * 
     * @return true if the resource is marked as deployed, false otherwise.
     */
    boolean isDeployed() {
        return deployed;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != (getClass())) {
            return false;
        }
        Resource otherResource = (Resource) obj;
        return new EqualsBuilder().
                append(name, otherResource.name).
                append(remoteDeploymentDirectory, 
                otherResource.remoteDeploymentDirectory).isEquals();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, remoteDeploymentDirectory);
    }
    
}
