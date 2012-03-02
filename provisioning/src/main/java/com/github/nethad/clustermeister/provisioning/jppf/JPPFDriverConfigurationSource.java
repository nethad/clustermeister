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
 * @author thomas
 */
public class JPPFDriverConfigurationSource implements JPPFConfiguration.ConfigurationSource {

    public static String host = "localhost";
    Properties properties = new Properties();

    public JPPFDriverConfigurationSource() {
//        properties.setProperty("jppf.discovery.enabled", "false");
//        properties.setProperty("jppf.discovery.group", "230.0.0.1");
//        properties.setProperty("jppf.discovery.port", "11111");
        
        properties.setProperty("jppf.server.host", "localhost");
        
        properties.setProperty("jppf.peer.server_1.server.host", "host_1");
        properties.setProperty("jppf.peer.server_1.server.port", "11111");
        properties.setProperty("jppf.peer.server_2.server.host", "host_2");
        properties.setProperty("jppf.peer.server_2.server.port", "11111");
        properties.setProperty("jppf.load.balancing.algorithm", "proportional");
        properties.setProperty("jppf.load.balancing.strategy", "test");
        properties.setProperty("strategy.manual.size", "1");
        properties.setProperty("strategy.autotuned.size", "5");
        properties.setProperty("strategy.autotuned.minSamplesToAnalyse", "100");
        properties.setProperty("strategy.autotuned.minSamplesToCheckConvergence", "50");
        properties.setProperty("strategy.autotuned.maxDeviation", "0.2");
        properties.setProperty("strategy.autotuned.maxGuessToStable", "50");
        properties.setProperty("strategy.autotuned.sizeRatioDeviation", "1.5");
        properties.setProperty("strategy.autotuned.decreaseRatio", "0.2");
        properties.setProperty("strategy.proportional.size", "5");
        properties.setProperty("strategy.proportional.performanceCacheSize", "300");
        properties.setProperty("strategy.proportional.proportionalityFactor", "1");
        properties.setProperty("strategy.rl.performanceCacheSize", "1000");
        properties.setProperty("strategy.rl.performanceVariationThreshold", "0.001");
        properties.setProperty("strategy.rl.maxActionRange", "10");
        properties.setProperty("strategy.nodethreads.multiplicator", "1");
        properties.setProperty("strategy.test.size", "1");
        properties.setProperty("strategy.test.minSamplesToAnalyse", "100");
        properties.setProperty("strategy.test.minSamplesToCheckConvergence", "50");
        properties.setProperty("strategy.test.maxDeviation", "0.2");
        properties.setProperty("strategy.test.maxGuessToStable", "50");
        properties.setProperty("strategy.test.sizeRatioDeviation", "1.5");
        properties.setProperty("strategy.test.decreaseRatio", "0.2");
        properties.setProperty("strategy.test.performanceCacheSize", "1000");
        properties.setProperty("strategy.test.proportionalityFactor", "1");
        properties.setProperty("strategy.test.increaseRate", "0.03");
        properties.setProperty("strategy.test.rateOfChange", "0.9");
        properties.setProperty("strategy.test.discountFactor", "0.2");
        properties.setProperty("strategy.test.performanceVariationThreshold", "0.001");
        properties.setProperty("strategy.test.maxActionRange", "10");
        properties.setProperty("strategy.test.multiplicator", "1");

        
//        properties.setProperty("jppf.server.port", "11198");
        
//        properties.setProperty("jppf.management.port", "11111");
        
        properties.setProperty("jppf.jvm.options", 
                "-Xmx256m "
                + "-Dlog4j.configuration=log4j-driver.properties"
                + "-Djava.util.logging.config.file=config/logging-driver.properties "
                + "-Djppf.config.plugin="+JPPFDriverConfigurationSource.class.getCanonicalName());
        
//        properties.setProperty("jppf.debug.enabled", "true");

//        properties.setProperty("jppf.server.host", "localhost");
//        properties.setProperty("jppf.debug.enabled", "true");
    }

    @Override
    public InputStream getPropertyStream() throws IOException {
//        System.out.println("READING CUSTOM CONFIGURATION, inputstream");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        properties.store(baos, "");
        return new ByteArrayInputStream(baos.toByteArray());
    }
}

