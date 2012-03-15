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

import java.util.List;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author thomas
 */
public class GatherNodeInformationTest {

    private GatherNodeInformation gatherNodeInformation;
    private String ipAddresses;

    @Before
    public void setup() {
        gatherNodeInformation = new GatherNodeInformation(null);
        ipAddresses = "192.168.1.107|192.168.1.107 node01.comp.uzh.ch|130.23.42.76 localhost|127.0.0.1";
    }

    @Test
    public void extractAddressesFromString() {
        List<String> addresses = gatherNodeInformation.extractAddressesFromString(ipAddresses);
        assertEquals(3, addresses.size());
        assertEquals("192.168.1.107", addresses.get(0));
        assertEquals("130.23.42.76", addresses.get(1));
        assertEquals("127.0.0.1", addresses.get(2));
    }

    @Test
    public void getAllPrivateAddresses() {
        Set<String> allPrivateAddresses = gatherNodeInformation.getAllPrivateAddresses(gatherNodeInformation.extractAddressesFromString(ipAddresses));
        assertEquals(1, allPrivateAddresses.size());
        assertEquals("192.168.1.107", allPrivateAddresses.iterator().next());
    }

    @Test
    public void getAllPublicAddresses() {
        Set<String> allPublicAddresses = gatherNodeInformation.getAllPublicAddresses(gatherNodeInformation.extractAddressesFromString(ipAddresses));
        assertEquals(1, allPublicAddresses.size());
        assertEquals("130.23.42.76", allPublicAddresses.iterator().next());
    }
}
