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
import com.github.nethad.clustermeister.api.impl.YamlConfiguration;
import java.io.StringReader;
import java.util.Map;
import org.apache.commons.configuration.Configuration;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 *
 * @author thomas
 */
public class DriverLoadBalancingTest {
    private DriverLoadBalancing driverLoadBalancing;
    
    private Configuration manualLoadBalancingConfig() {
        StringBuilder sb = new StringBuilder("load_balancing:\n");
            sb.append("  algorithm: manual\n")
              .append("  strategy: mystrategy\n")
              .append("  strategies:\n")
              .append("    - mystrategy:\n")
              .append("        size: 10\n");
        return readYamlConfig(sb);
    }
    
    private Configuration strategyNotInStrategiesConfig() {
        StringBuilder sb = new StringBuilder("load_balancing:\n");
            sb.append("  algorithm: manual\n")
              .append("  strategy: somestrategy\n")
              .append("  strategies:\n")
              .append("    - mystrategy:\n")
              .append("        size: 10\n");
        return readYamlConfig(sb);
    }
    
    private Configuration algorithmMissingConfig() {
        StringBuilder sb = new StringBuilder("load_balancing:\n");
            sb.append("  strategy: mystrategy\n")
              .append("  strategies:\n")
              .append("    - mystrategy:\n")
              .append("        size: 10\n");
        return readYamlConfig(sb);
    }
    
    private Configuration strategyMissingConfig() {
        StringBuilder sb = new StringBuilder("load_balancing:\n");
            sb.append("  algorithm: manual\n")
              .append("  strategy: mystrategy\n");
        return readYamlConfig(sb);
    }
    
    private Configuration readYamlConfig(StringBuilder sb) {
        YamlConfiguration config = new YamlConfiguration();
        config.load(new StringReader(sb.toString()));
        return config;
    }

    @Test
    public void manualStrategy() {
        driverLoadBalancing = new DriverLoadBalancing(manualLoadBalancingConfig());
        Map<String, String> values = driverLoadBalancing.getLoadBalancingConfigValues();
        
        assertThat(values.size(), is(3));
        assertThat(values.get(JPPFConstants.LOAD_BALANCING_ALGORITHM), equalTo("manual"));
        assertThat(values.get(JPPFConstants.LOAD_BALANCING_STRATEGY), equalTo("cmprofile"));
        assertThat(values.get("strategy.cmprofile.size"), equalTo("10"));
    }

    @Test
    public void strategyNotInStrategies() {
        driverLoadBalancing = new DriverLoadBalancing(strategyNotInStrategiesConfig());
        Map<String, String> values = driverLoadBalancing.getLoadBalancingConfigValues();
        
        assertThat(values.isEmpty(), is(true));
    }

    @Test
    public void algorithmMissing() {
        driverLoadBalancing = new DriverLoadBalancing(algorithmMissingConfig());
        Map<String, String> values = driverLoadBalancing.getLoadBalancingConfigValues();
        
        assertThat(values.isEmpty(), is(true));
    }

    @Test
    public void strategyMissing() {
        driverLoadBalancing = new DriverLoadBalancing(strategyMissingConfig());
        Map<String, String> values = driverLoadBalancing.getLoadBalancingConfigValues();
        
        assertThat(values.isEmpty(), is(true));
    }
    
    @Test
    public void nullConfiguration() {
        driverLoadBalancing = new DriverLoadBalancing(null);
        Map<String, String> values = driverLoadBalancing.getLoadBalancingConfigValues();

        assertThat(values.isEmpty(), is(true));
    }
}
