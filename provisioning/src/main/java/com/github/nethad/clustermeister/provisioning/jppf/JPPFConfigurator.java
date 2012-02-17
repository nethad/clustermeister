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
import org.jppf.utils.JPPFConfiguration;

/**
 *
 * @author daniel
 */
public class JPPFConfigurator implements JPPFConfiguration.ConfigurationSource {
	public static String host = "localhost";
	
	Properties properties = new Properties();

	public JPPFConfigurator() {
		properties.setProperty("jppf.discovery.enabled", "false");
		properties.setProperty("jppf.discovery.group", "230.0.0.1");
		properties.setProperty("jppf.discovery.port", "11111");
		properties.setProperty("jppf.drivers", "driver-1");
		properties.setProperty("driver-1.jppf.server.host", host);
		properties.setProperty("driver-1.jppf.server.port", "11111");
	}
	
	@Override
	public InputStream getPropertyStream() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		properties.store(baos, "");
		return new ByteArrayInputStream(baos.toByteArray());
	}
	
}
