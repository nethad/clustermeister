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
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 *
 * @author daniel
 */
public abstract class Resource {
    private final String name;
    private final String remoteDeploymentDirectory;
    private boolean uploaded = false;
    private boolean deployed = false;
    private boolean unzipContents = false;

    public Resource(String name, String remoteDeploymentDirectory) {
        this.name = name;
        this.remoteDeploymentDirectory = remoteDeploymentDirectory;
    }

    public abstract InputStream getResourceData() throws IOException;

    public abstract long getResourceChecksum() throws IOException;

    public String getName() {
        return name;
    }

    public String getRemoteDeploymentDirectory() {
        return remoteDeploymentDirectory;
    }

    public void setUnzipContents(boolean unzipContents) {
        this.unzipContents = unzipContents;
    }

    public boolean isUnzipContents() {
        return unzipContents;
    }

    void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }

    boolean isUploaded() {
        return uploaded;
    }

    void setDeployed(boolean deployed) {
        this.deployed = deployed;
    }

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
        return new EqualsBuilder().append(name, otherResource.name).append(remoteDeploymentDirectory, otherResource.remoteDeploymentDirectory).isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append(name).append("deploymentDirectory", remoteDeploymentDirectory).toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, remoteDeploymentDirectory);
    }
    
}
