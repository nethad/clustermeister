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

import com.github.nethad.clustermeister.api.Credentials;
import com.github.nethad.clustermeister.api.JPPFConstants;
import com.github.nethad.clustermeister.api.LogLevel;
import com.github.nethad.clustermeister.api.NodeConfiguration;
import com.github.nethad.clustermeister.api.NodeType;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;

/**
 *
 * @author daniel
 */
public class AmazonNodeConfiguration implements NodeConfiguration {
    
    private AWSInstanceProfile profile = null;
    private NodeType nodeType = NodeType.NODE;
    private Optional<Credentials> credentials = Optional.absent();
    private String driverAddress = "";
    private boolean driverDeployedLocally = false;
    private int managementPort = JPPFConstants.DEFAULT_MANAGEMENT_PORT;
    private Collection<File> artifactsToPreload = Collections.EMPTY_LIST;
    private Optional<String> jvmOptions = Optional.absent();
    private Optional<LogLevel> logLevel = Optional.absent();
    private Optional<Boolean> remoteLoggingActivated = Optional.absent();
    private Optional<Integer> remoteLoggingPort = Optional.absent();

    public static AmazonNodeConfiguration fromInstanceProfile(
            AWSInstanceProfile instanceProfile) {
        
        return new AmazonNodeConfiguration(instanceProfile);
    }

    private AmazonNodeConfiguration(AWSInstanceProfile instanceProfile) {
        this.profile = instanceProfile;
    }
    
    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    @Override
    public NodeType getType() {
        return nodeType;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = Optional.fromNullable(credentials);
    }

    public Optional<Credentials> getCredentials() {
        return credentials;
    }
    
    public void setDriverAddress(String driverAddress) {
        this.driverAddress = driverAddress;
    }

    @Override
    public String getDriverAddress() {
        return driverAddress;
    }

    public void setDriverDeployedLocally(boolean driverDeployedLocally) {
        this.driverDeployedLocally = driverDeployedLocally;
    }

    @Override
    public boolean isDriverDeployedLocally() {
        return driverDeployedLocally;
    }

    public AWSInstanceProfile getProfile() {
        return profile;
    }
    
    void setManagementPort(int managementPort) {
        this.managementPort = managementPort;
    }

    int getManagementPort() {
        return managementPort;
    }

    Template getTemplate(TemplateBuilder templateBuilder) {
        templateBuilder.hardwareId(profile.getType());
        templateBuilder.locationId(profile.getZone().or(profile.getRegion()));
        
        if(profile.getAmiId().isPresent()) {
            String jCloudsImageId = Joiner.on('/').join(profile.getRegion(), 
                    profile.getAmiId().get());
            templateBuilder.imageId(jCloudsImageId);
        } else {
            //fallback
            //this will launch the highest version of the AMZN_LINUX template
            templateBuilder.osFamily(OsFamily.AMZN_LINUX);
        }
        
        return templateBuilder.build();
    }

    public void setArtifactsToPreload(Collection<File> artifacts) {
        artifactsToPreload = artifacts;
    }
    
    @Override
    public Collection<File> getArtifactsToPreload() {
        return artifactsToPreload;
    }

    public void setJvmOptions(String jvmOptions) {
        this.jvmOptions = Optional.fromNullable(jvmOptions);
    }

    @Override
    public Optional<String> getJvmOptions() {
        return jvmOptions;
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = Optional.fromNullable(logLevel);
    }

    @Override
    public Optional<LogLevel> getLogLevel() {
        return logLevel;
    }

    public void setRemoteLoggingActivated(Boolean remoteLoggingActivated) {
        this.remoteLoggingActivated = Optional.fromNullable(remoteLoggingActivated);
    }
    
    @Override
    public Optional<Boolean> isRemoteLoggingActivataed() {
        return remoteLoggingActivated;
    }

    public void setRemoteLoggingPort(Integer remoteLoggingPort) {
        this.remoteLoggingPort = Optional.fromNullable(remoteLoggingPort);
    }

    @Override
    public Optional<Integer> getRemoteLoggingPort() {
        return remoteLoggingPort;
    }
}
