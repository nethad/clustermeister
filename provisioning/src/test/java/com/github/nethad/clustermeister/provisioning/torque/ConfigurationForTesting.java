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
package com.github.nethad.clustermeister.provisioning.torque;

import com.github.nethad.clustermeister.api.Configuration;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author thomas
 */
public class ConfigurationForTesting implements Configuration {

    Map<String, Object> configValues = new HashMap<String, Object>();

    public ConfigurationForTesting(Map<String, Object> configValues) {
        this.configValues = configValues;
    }
    
    @Override
    public String getString(String key, String defaultValue) {
        if (configValues.containsKey(key)) {
            return (String) configValues.get(key);
        } else {
            return defaultValue;
        }
    }

    @Override
    public int getInt(String key, int defaultValue) {
        if (configValues.containsKey(key)) {
            return (Integer) configValues.get(key);
        } else {
            return defaultValue;
        }
    }
}
