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
package com.github.nethad.clustermeister.provisioning.dependencymanager;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import org.apache.maven.model.Model;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author daniel
 */
public class MavenUtilsTest {
    
    public MavenUtilsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getDependencies method, of class MavenUtils.
     */
    @Test
    public void testGetDependencies() throws FileNotFoundException {
        MavenUtils.getDependencies(getClass().getResourceAsStream("test-pom.xml"));
    }
    
    @Test
    public void testEffectiveModel() throws URISyntaxException {
        Model model = MavenUtils.getEffectiveModel(new File(getClass().getResource("test-pom.xml").toURI()), 
                MavenUtils.newCentralRepository(), 
                MavenUtils.newRemoteRepository("typesafe", "default", "http://repo.typesafe.com/typesafe/releases/"), 
                MavenUtils.newRemoteRepository("ifi", "default", "https://maven.ifi.uzh.ch/maven2/content/groups/public/"));
        
        System.out.println(model.getName());
        System.out.println(model.getGroupId());
        System.out.println(model.getArtifactId());
        System.out.println(model.getDependencies());
    }
}
