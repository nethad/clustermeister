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
package com.github.nethad.clustermeister.api.impl;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for reading values from configuration files.
 *
 * @author daniel
 */
public class ConfigurationUtil {
    
    /**
     * This helps reading lists form the configuration that consist of 
     * 'named objects', e.g. (YAML):
     * 
     * <pre>
     * list:
     *   - name1:
     *      key1: value
     *      key2: value2
     *   - name2:
     *      key1: value3
     *      key3: value4
     *   ...
     * </pre>
     * 
     * More specifically it reduces a List&lt;Map&lt;String, Map&lt;String, 
     * String&gt;&gt;&gt; as produced by above example to a Map&lt;String, 
     * Map&lt;String, String&gt;&gt; like this (Java syntax, referring to above 
     * example):
     * 
     * <pre>
     * [
     *   name1 => [ 
     *          key1 => value,
     *          key2 => value2 
     *   ],
     *   name2 => [
     *          key1 => value3,
     *          key3 => value4
     *   ],
     *   ...
     * ]
     * </pre>
     * 
     * @param list the list to reduce.
     * @param errorMessage  
     *          Custom error message to add to exception in case of the 
     *          list not being convertible.
     * @return A map reduced as described above.
     * @throws IllegalArgumentException if the list can not be converted in 
     *          this manner.
     */
    public static Map<String, Map<String, String>> reduceObjectList(List<Object> list, String errorMessage) {
        try {
            Map<String, Map<String, String>> result = Maps.newLinkedHashMap();
            List<Map<String, Map<String, String>>> mapList = Lists.transform(list, 
                    new Function<Object, Map<String, Map<String, String>>>() {
                        @Override
                        public Map apply(Object input) {
                            return (Map<String, Map<String, String>>) input;
                        }
            });
            for (Map<String, Map<String, String>> map : mapList) {
                for (Map.Entry<String, Map<String, String>> entry : map.entrySet()) {
                    String key = entry.getKey();
                    Map<String, String> value = entry.getValue();
                    for (Map.Entry<String, String> valueEntry : value.entrySet()) {
                        Object valueValue = valueEntry.getValue();
                        valueEntry.setValue(String.valueOf(valueValue));
                    }
                    result.put(key, value);
                }
            }

            return result;
            
        } catch(ClassCastException ex) {
            throw new IllegalArgumentException(errorMessage, ex);
        }
    }
    
    /**
     * Get a {@link File} from a path string.
     * 
     * <p>
     * This method checks if the file exists and is readable.
     * </p>
     * 
     * <p>
     * The error reporting pattern referred to in the parameter list is: 
     * <br/>
     * '&lt;key&gt; for &lt;listObjectCategory&gt; &lt;listObjectName&gt; can 
     * not be read from.'
     * </p>
     * <p>
     * Example:
     * <br/>
     * url for Maven Repository 'My Maven Repository' can not be read from.
     * </p>
     * @param path the path to the file.
     * @param key   
     *      Configuration key/property name, e.g. 'user_name' (like in: 
     *      'user_name = Hans Wurst'). This is used for error reporting.
     * @param listObjectCategory
     *      Configuration category (e.g. 'Maven Repository'). 
     *      Used for error reporting.
     * @param listObjectName
     *      Configuration category name (e.g. 'My Maven Repository').
     *      Used for error reporting.
     * @return the file.
     */
    public static File getCheckedFile(String path, String key, String listObjectCategory, String listObjectName) {
        File file = new File(path);
        checkArgument(file.isFile() && file.canRead(),
                "%s for %s '%s' can not be read from.", key, listObjectCategory, listObjectName);
        return file;
    }
    
    /**
     * Get a configuration value from a map.
     * 
     * <p>
     * This method checks that the key is contained in the map and the value is 
     * not null.
     * </p>
     * 
     * <p>
     * For details about the error reporting pattern referred to below check 
     * {@link #getCheckedFile(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     * </p>
     * @param key   
     *      Configuration key/property, e.g. 'user_name' (like in: 
     *      'user_name = Hans Wurst').
     * @param configMap the map containing the key/value pair.
     * @param listObjectCategory
     *      Configuration category (e.g. 'Maven Repository'). 
     *      Used for error reporting.
     * @param listObjectName
     *      Configuration category name (e.g. 'My Maven Repository').
     *      Used for error reporting.
     * @return the value.
     */
    public static String getCheckedConfigValue(String key, Map<String, String> configMap, 
            String listObjectCategory, String listObjectName) {
        checkArgument(configMap.containsKey(key),
                "No key '%s' found for %s '%s'.", key, listObjectCategory, listObjectName);
        String value = checkNotNull(configMap.get(key),
                "No value for key '%s' found for %s '%s'.", key, listObjectCategory, 
                listObjectName).trim();
        return value;
    }
}
