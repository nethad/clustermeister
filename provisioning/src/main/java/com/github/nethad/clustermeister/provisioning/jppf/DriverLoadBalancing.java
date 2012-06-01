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

import com.github.nethad.clustermeister.api.JPPFConstants;
import com.github.nethad.clustermeister.api.impl.ConfigurationUtil;
import java.util.*;
import org.apache.commons.configuration.Configuration;

/**
 *
 * @author thomas
 */
public class DriverLoadBalancing {
    public static final String ROOT_KEY = "load_balancing";
    public static final String ALGORITHM = "load_balancing.algorithm";
    public static final String STRATEGY = "load_balancing.strategy";
    public static final String STRATEGIES = "load_balancing.strategies";
    private String algorithm;
    
    private final Configuration configuration;
    private Map<String, Map<String, String>> strategies;
    private String strategy;

    public DriverLoadBalancing(Configuration configuration) {
        this.configuration = configuration;
    }
    
    public Map<String, String> getLoadBalancingConfigValues() {
        if (configuration == null) {
            return Collections.EMPTY_MAP;
        }
        
        LinkedHashMap<String, String> values = new LinkedHashMap<String, String>();
        algorithm = configuration.getString(ALGORITHM);
        strategy = configuration.getString(STRATEGY);
        strategies = getStrategies();
        if (isBrokenConfiguration()) {
            return Collections.EMPTY_MAP;
        }
        values.put(JPPFConstants.LOAD_BALANCING_ALGORITHM, algorithm);
        values.put(JPPFConstants.LOAD_BALANCING_STRATEGY, "cmprofile");
        Map<String, String> strategyValues = strategies.get(strategy);
        for (Map.Entry<String, String> entry : strategyValues.entrySet()) {
            values.put("strategy.cmprofile." + entry.getKey(), entry.getValue());
        }
        return values;
    }
    
    private Map<String, Map<String, String>> getStrategies() {
        try {
            return ConfigurationUtil.reduceObjectList(configuration.getList(STRATEGIES), 
                    "Strategies in load_balancing configuration are not well formatted.");
        } catch (IllegalArgumentException ex) {
            // do nothing, returns null below
        }
        return null;
    }
    
    private boolean isBrokenConfiguration() {
        return algorithm == null 
                || strategy == null 
                || strategies == null
                || strategies.isEmpty()
                || !strategies.containsKey(strategy);
    }
}
