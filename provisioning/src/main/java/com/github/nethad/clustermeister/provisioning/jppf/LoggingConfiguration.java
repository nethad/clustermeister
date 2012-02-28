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
package com.github.nethad.clustermeister.provisioning.jppf;

import com.github.nethad.clustermeister.provisioning.torque.TorqueJPPFTestSetup;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author thomas
 */
public class LoggingConfiguration {
    
    private InputStream getPropertiesInputStream_javaUtilLogging() {
        try {
            Properties properties = new Properties();
            String debugLevel = "FINEST";
            properties.setProperty("handlers", "java.util.logging.FileHandler");
            properties.setProperty(".level", debugLevel);

            properties.setProperty("java.util.logging.FileHandler.pattern", "logging-driver.log");
            properties.setProperty("java.util.logging.FileHandler.level", debugLevel);
            properties.setProperty("java.util.logging.FileHandler.count", "1");
            properties.setProperty("java.util.logging.FileHandler.formatter", "org.jppf.logging.jdk.JPPFLogFormatter");
            properties.setProperty("java.util.logging.FileHandler.append", "false");
            properties.setProperty("java.util.logging.ConsoleHandler.level", debugLevel);
            properties.setProperty("java.util.logging.ConsoleHandler.formatter", "java.util.logging.SimpleFormatter");
            properties.setProperty("org.jppf.logging.jdk.JmxHandler.level", debugLevel);
            properties.setProperty("org.jppf.logging.jdk.JmxHandler.formatter", "org.jppf.logging.jdk.JPPFLogFormatter");
            properties.setProperty("org.jppf.level", debugLevel);
            properties.setProperty("org.jppf.utils.level", debugLevel);
            properties.setProperty("org.jppf.comm.discovery.level", debugLevel);
            properties.setProperty("org.jppf.server.JPPFDriver", debugLevel);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            properties.store(baos, "");
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IOException ex) {
            Logger.getLogger(TorqueJPPFTestSetup.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
}
