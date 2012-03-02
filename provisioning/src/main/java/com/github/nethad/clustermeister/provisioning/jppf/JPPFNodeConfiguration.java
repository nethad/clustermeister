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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author thomas
 */
public class JPPFNodeConfiguration {
    
    Properties properties = new Properties();

    public JPPFNodeConfiguration() {
        properties.setProperty("jppf.management.enabled", "true");
        properties.setProperty("jppf.discovery.enabled", "false");
        properties.setProperty("reconnect.max.time", "60");
        
        properties.setProperty("jppf.jvm.options", "-Xmx128m -Djava.util.logging.config.file=config/logging-driver.properties");
        properties.setProperty("jppf.classloader.delegation", "parent");
    }
    
    public JPPFNodeConfiguration setProperty(String key, String value) {
        properties.setProperty(key, value);
        return this;
    }
    
    public InputStream getPropertyStream() throws IOException {
//        System.out.println("READING CUSTOM CONFIGURATION, inputstream");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        properties.store(baos, "");
        return new ByteArrayInputStream(baos.toByteArray());
    }

	public String getProperty(String key) {
		return properties.getProperty(key);
	}
    
}
