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
package com.github.nethad.clustermeister.provisioning;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author thomas
 */
public class FileConfiguration implements Configuration {
    private Properties properties;

    public FileConfiguration(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("File "+filePath+" does not exist.");
        }
        try {
            properties = new Properties();
            properties.load(new FileReader(file));
        } catch (IOException ex) {
            Logger.getLogger(FileConfiguration.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
}
