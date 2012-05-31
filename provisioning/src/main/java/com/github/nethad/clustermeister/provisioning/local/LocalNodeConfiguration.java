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
package com.github.nethad.clustermeister.provisioning.local;

import com.github.nethad.clustermeister.api.LogLevel;
import com.github.nethad.clustermeister.api.NodeConfiguration;
import com.github.nethad.clustermeister.api.NodeType;
import com.google.common.base.Optional;
import java.io.File;
import java.util.Collection;

/**
 *
 * @author thomas
 */
public class LocalNodeConfiguration implements NodeConfiguration {
    
    private Collection<File> artifactsToPreload;
    private final Optional<String> jvmOptions;
    private final Optional<LogLevel> logLevel;
    private final Optional<Boolean> remoteLoggingActivated;
    private final int numberOfProcessingThreads;
    
    public static LocalNodeConfiguration configurationFor(
            Collection<File> artifactsToPreload, String jvmOptions, 
            String logLevel, boolean activateRemoteLogging, 
            int numberOfProcessingThreads) {
        
        return new LocalNodeConfiguration(artifactsToPreload, jvmOptions, 
                logLevel, activateRemoteLogging, numberOfProcessingThreads);
    }

    private LocalNodeConfiguration(Collection<File> artifactsToPreload, 
            String jvmOptions, String logLevel, boolean activateRemoteLogging, 
            int numberOfProcessingThreads) {
        
        this.artifactsToPreload = artifactsToPreload;
        this.jvmOptions = Optional.fromNullable(jvmOptions);
        this.logLevel = Optional.fromNullable(LogLevel.valueOf(logLevel.toUpperCase()));
        this.remoteLoggingActivated = Optional.fromNullable(activateRemoteLogging);
        this.numberOfProcessingThreads = numberOfProcessingThreads;
    }

    @Override
    public NodeType getType() {
        return NodeType.NODE;
    }

    @Override
    public String getDriverAddress() {
        return "127.0.0.1";
    }

    @Override
    public boolean isDriverDeployedLocally() {
        return true;
    }

    @Override
    public Collection<File> getArtifactsToPreload() {
        return artifactsToPreload;
    }

    @Override
    public Optional<String> getJvmOptions() {
        return jvmOptions;
    }

    /**
     * @return the numberOfProcessingThreads
     */
    public int getNumberOfProcessingThreads() {
        return numberOfProcessingThreads;
    }

    @Override
    public Optional<LogLevel> getLogLevel() {
        return logLevel;
    }

    @Override
    public Optional<Boolean> isRemoteLoggingActivataed() {
        return remoteLoggingActivated;
    }
}
