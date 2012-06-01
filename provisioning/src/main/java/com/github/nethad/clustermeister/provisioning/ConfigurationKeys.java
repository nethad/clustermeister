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

/**
 *
 * @author thomas
 */
public class ConfigurationKeys {
    
    public static final String JVM_OPTIONS_LOCAL_DRIVER = "jvm_options.local_driver";
    
    public static final String JVM_OPTIONS_NODE = "jvm_options.node";
    public static final String DEFAULT_JVM_OPTIONS_NODE = "-Xmx32m";
    
    public static final String LOGGING_NODE_LEVEL = "logging.node.level";
    public static final String LOGGING_NODE_REMOTE = "logging.node.remote";
    public static final String LOGGING_NODE_REMOTE_PORT = "logging.node.remote_port";
    public static final String DEFAULT_LOGGING_NODE_LEVEL = "INFO";
    public static final Boolean DEFAULT_LOGGING_NODE_REMOTE = Boolean.FALSE;
    public static final int DEFAULT_LOGGING_NODE_REMOTE_PORT = 52321;
    
}
