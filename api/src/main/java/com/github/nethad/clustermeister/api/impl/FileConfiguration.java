/*
 * Copyright 2012 University of Zurich.
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
package com.github.nethad.clustermeister.api.impl;

import java.io.File;
import java.net.URL;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 *
 * @author thomas
 */
public class FileConfiguration extends PropertiesConfiguration {
    
    public static final String CONFIG_FILE_NAME = "configuration.yml";
    public static final String DEFAULT_CONFIG_FILE = System.getProperty("user.home") + "/.clustermeister/" + CONFIG_FILE_NAME;

    public FileConfiguration(File file) throws ConfigurationException {
        super(file);
    }

    public FileConfiguration(String fileName) throws ConfigurationException {
        super(fileName);
    }

    public FileConfiguration(URL url) throws ConfigurationException {
        super(url);
    }
}
