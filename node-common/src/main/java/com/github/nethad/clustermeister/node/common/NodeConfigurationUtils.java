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
package com.github.nethad.clustermeister.node.common;

import java.util.Properties;

/**
 *
 * @author daniel
 */
public class NodeConfigurationUtils {
    
    /**
     * Returns JPPF node configuration properties.
     * 
     * @return the log4j configuration.
     */
    public static Properties getLog4JConfiguration() {
        Properties properties = new Properties();
        properties.setProperty("log4j.appender.JPPF", "org.apache.log4j.FileAppender");
        properties.setProperty("log4j.appender.JPPF.File", "jppf-node.log");
        properties.setProperty("log4j.appender.JPPF.Append", "false");
        properties.setProperty("log4j.appender.JPPF.layout", "org.apache.log4j.PatternLayout");
        properties.setProperty("log4j.appender.JPPF.layout.ConversionPattern", "%d [%-5p][%c.%M(%L)]: %m\n");
        
        properties.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        properties.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
        properties.setProperty("log4j.appender.stdout.layout.ConversionPattern", "%d [%-5p][%c.%M(%L)]: %m\n");
        
        //TODO: make node log level configurable
        properties.setProperty("log4j.rootLogger", "INFO, JPPF, stdout");
//        properties.setProperty("log4j.rootLogger", "INFO, JPPF");
        
        return properties;
    }
}
