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

import com.github.nethad.clustermeister.api.NodeType;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.hamcrest.CoreMatchers.*;

/**
 *
 * @author thomas
 */
public class TorqueNodeConfigurationTest {

    @Before
    public void setup() {
    }

    @Test
    public void factoryMethodDriver_local() {
        TorqueNodeConfiguration configurationForDriver = TorqueNodeConfiguration.configurationForDriver(true);
        assertThat(configurationForDriver.isDriverDeployedLocally(), is(true));
        assertThat(configurationForDriver.getDriverAddress(), is(""));
        assertThat(configurationForDriver.getNumberOfCpus(), is(1));
        assertThat(configurationForDriver.getType(), is(NodeType.DRIVER));
    }

    @Test
    public void factoryMethodDriver_notLocal() {
        TorqueNodeConfiguration configurationForDriver = TorqueNodeConfiguration.configurationForDriver(false);
        assertThat(configurationForDriver.isDriverDeployedLocally(), is(false));
        assertThat(configurationForDriver.getDriverAddress(), is(""));
        assertThat(configurationForDriver.getNumberOfCpus(), is(1));
        assertThat(configurationForDriver.getType(), is(NodeType.DRIVER));
    }

    @Test
    public void factoryMethodNode() {
        TorqueNodeConfiguration configurationForDriver = TorqueNodeConfiguration.configurationForNode("driverIp", 3);
        assertThat(configurationForDriver.getDriverAddress(), is("driverIp"));
        assertThat(configurationForDriver.getNumberOfCpus(), is(3));
        assertThat(configurationForDriver.getType(), is(NodeType.NODE));
    }
}
