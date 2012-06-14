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
package com.github.nethad.clustermeister.node.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.StringTokenizer;
import org.jppf.utils.JPPFConfiguration;

/**
 * Provides the properties configured in the {@link Constants#JPPF_JVM_OPTIONS} 
 * system property as configuration source for JPPF.
 *
 * @author daniel
 */
public class JvmOptionsConfigurationSource implements JPPFConfiguration.ConfigurationSource {

    @Override
    public InputStream getPropertyStream() throws IOException {
        String jvmOptions = System.getProperty(Constants.JPPF_JVM_OPTIONS);
        Properties properties = new Properties();
        if(jvmOptions != null) {
            StringTokenizer tokenizer = new StringTokenizer(jvmOptions);
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                if(token.startsWith("-D") && token.contains("=")) {
                    String[] property = token.substring(2).split("=", 2);
                    properties.setProperty(property[0], property[1]);
                }
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        properties.store(baos, null);
                    System.out.println("HMM: " + new String(baos.toByteArray()));
        return new ByteArrayInputStream(baos.toByteArray());
    }
    
}
