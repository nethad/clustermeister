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
package com.github.nethad.clustermeister.provisioning.cli;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author thomas
 */
@Ignore("Depends on local configuration")
public class CLISystemTest {
    private ProvisioningCLI provisioningCLI;
    
    @Before
    public void setup() {
        provisioningCLI = new ProvisioningCLI();
    }
    
    @Test
    public void startupThreeTorqueNodes() throws Exception {
        provisioningCLI.parseArguments(new String[]{"-n", "3"});
        Provisioning provisioning = new Provisioning("/home/thomas/.clustermeister/configuration.properties", Provider.TORQUE);
        provisioning.execute();
        Thread.sleep(10000);
    }
    
}
