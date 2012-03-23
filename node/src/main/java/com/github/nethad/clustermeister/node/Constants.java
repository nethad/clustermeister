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
package com.github.nethad.clustermeister.node;

/**
 * Shared constants.
 *
 * @author daniel
 */
public class Constants {
    /**
     * UTF-8 charset string ("UTF-8").
     */
    public static final String UTF8 = "UTF-8";
    
    /**
     * Well-defined UUID prefix.
     */
    public static final String UUID_PREFIX = "UUID=";
    
    /**
     * Standard out log file name.
     */
    public static final String STDOUT_LOG = "stdout.log";
    
    /**
     * Standard err log file name.
     */
    public static final String STDERR_LOG = "stderr.log";
    
    /**
     * System property configuration indicating that stdout/stderr shall be 
     * diverted to loggers.
     */
    public static final String CLUSTERMEISTER_DIVERT_STREAMS_TO_LOG = 
            "com.github.nethad.clustermeister.divertStreamsToLog";
    /**
     * System property configuration indicating that stdout/stderr shall be 
     * diverted to files.
     */
    public static final String CLUSTERMEISTER_DIVERT_STREAMS_TO_FILE = 
            "com.github.nethad.clustermeister.divertStreamsToFile";
    
    /**
     * JPPF System property configuration for additional JVM options.
     */
    public static final String JPPF_JVM_OPTIONS = "jppf.jvm.options";
}
