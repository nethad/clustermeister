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

import com.google.common.util.concurrent.Monitor;
import org.jppf.utils.JPPFConfiguration.ConfigurationSource;

/**
 * Creates instances that need a custom JPPF ConfigurationSource.
 * 
 * This class coordinates the System property settings between concurrent calls.
 * 
 * This class is a singleton.
 *
 * @author daniel
 */
public class JPPFConfiguratedComponentFactory {
	/**
	 * The "jppf.config.plugin" property.
	 */
	protected static final String JPPF_CONFIG_PLUGIN = "jppf.config.plugin";
	
	private final Monitor configPropertyMonitor = new Monitor(false);
	
	private JPPFConfiguratedComponentFactory() {
		//private default constructor
	}
	
	/**
	 * Get the only instance of JPPFConfiguratedComponentFactory.
	 * 
	 * @return the JPPFConfiguratedComponentFactory instance.
	 */
	public static JPPFConfiguratedComponentFactory getInstance() {
		return NewSingletonHolder.INSTANCE;
	}
	
	/**
	 * Create a JPPFManagementByJobsClient instance.
	 * 
	 * @param configurationSource	the JPPF configuration source.
	 * @return	the JPPFManagementByJobsClient instance.
	 */
	public JPPFManagementByJobsClient createManagementByJobsClient(ConfigurationSource configurationSource) {
		
		configPropertyMonitor.enter();
		try {
			setConfigProperty(configurationSource);
			return new JPPFManagementByJobsClient();
		} finally {
			configPropertyMonitor.leave();
		}
	}

	private void setConfigProperty(ConfigurationSource configurationSource) {
		System.setProperty(JPPF_CONFIG_PLUGIN, 
				configurationSource.getClass().getCanonicalName());
	}
	
	private static class NewSingletonHolder {
		private static final JPPFConfiguratedComponentFactory INSTANCE = 
				new JPPFConfiguratedComponentFactory();
	}
}
