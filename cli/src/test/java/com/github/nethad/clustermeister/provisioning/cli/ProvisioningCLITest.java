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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author thomas
 */
public class ProvisioningCLITest {
    private ProvisioningCLI provisioningCLI;
        
    @Before
    public void setUp() {
        provisioningCLI = new ProvisioningCLI();
    }
    
    @After
    public void tearDown() {
    }
    
    private String[] buildArgs(String argLine) {
        return argLine.split(" ");
    }

    /**
     * Test of main method, of class ProvisioningCLI.
     */
    @Test
    public void testMain() throws Exception {
        final String configPath = "/home/user/.clustermeister/configuration.properties";
        final String argLine = "--config "+configPath+" --provider amazon";
        provisioningCLI.parseArguments(buildArgs(argLine));
        assertEquals(configPath, provisioningCLI.getConfigFilePath());
        assertEquals(Provider.AMAZON, provisioningCLI.getProvider());
    }
}
