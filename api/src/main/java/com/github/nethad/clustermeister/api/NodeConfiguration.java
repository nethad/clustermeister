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
package com.github.nethad.clustermeister.api;

import com.google.common.base.Optional;
import java.io.File;
import java.util.Collection;

/**
 *
 * @author daniel
 */
public interface NodeConfiguration {
	public NodeType getType();
	
	public String getDriverAddress();
	
	public boolean isDriverDeployedLocally();
        
	public Collection<File> getArtifactsToPreload();
        
        public Optional<String> getJvmOptions();
        
        /**
         * Specifies the SLF4J log level for logging output on this Node.
         * 
         * @return the log level.
         */
        public Optional<LogLevel> getLogLevel();
        
        /**
         * Whether to use remote logging.
         * 
         * @return true means to use remote logging for this node, false means not to use it.
         */
        public Optional<Boolean> isRemoteLoggingActivataed();
        
        /**
         * Returns the TCP port to send remote logging events to.
         */
        public Optional<Integer> getRemoteLoggingPort();
}
