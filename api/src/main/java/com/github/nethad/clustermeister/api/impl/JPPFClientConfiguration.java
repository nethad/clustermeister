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

import com.github.nethad.clustermeister.api.JPPFConstants;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.jppf.utils.JPPFConfiguration;

/**
 *
 * @author thomas
 */
public class JPPFClientConfiguration extends AbstractJPPFConfiguration {

    private Properties properties = new Properties();

    public JPPFClientConfiguration() {
        String driverName = "driver";
        properties.setProperty(JPPFConstants.DRIVERS, driverName);
        properties.setProperty(String.format(JPPFConstants.DRIVER_SERVER_HOST_PATTERN, driverName), "localhost");
        properties.setProperty(String.format(JPPFConstants.DRIVER_SERVER_PORT_PATTERN, driverName), "11111");
//        properties.setProperty("reconnect.max.time", "-1");
        properties.setProperty(JPPFConstants.DISCOVERY_ENABLED, "false");
    }

    @Override
    protected Properties getProperties() {
        return properties;
    }
}
