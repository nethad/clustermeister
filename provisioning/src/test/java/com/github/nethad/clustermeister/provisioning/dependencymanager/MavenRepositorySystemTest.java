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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import static org.hamcrest.Matchers.*;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyResolutionException;

/**
 *
 * @author daniel
 */
public class MavenRepositorySystemTest {

    private static MavenRepositorySystem mavenRepositorySystem;
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        mavenRepositorySystem = new MavenRepositorySystem();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        mavenRepositorySystem = null;
    }
    
    @Test
    public void testEffectiveModel() throws URISyntaxException {
        Model model = mavenRepositorySystem.getEffectiveModel(getTestPom());
        
        assertTrue("Dependencies do not match.", 
                Iterables.all(model.getDependencies(), 
                    getContainsAllDependenciesPredicate(getExpectedSet(
                        "akka-actor",
                        "akka-remote",
                        "server"
                ))));
    }
    
    @Test
    public void testResolveDependenciesCoords() throws DependencyResolutionException {
        RemoteRepository ifiRepo = mavenRepositorySystem.createRemoteRepository(
                      "ifi", "default", "https://maven.ifi.uzh.ch/maven2/content/groups/public/");
        mavenRepositorySystem.addRepository(ifiRepo);
        List<File> dependencies = mavenRepositorySystem.resolveDependencies("org.jppf:common:3.0.1");
        assertThat("Amount of dependencies is wrong.", dependencies.size(), is(equalTo(1)));
        assertThat("Wrong dependency.", dependencies.get(0).getName(), 
                containsString("common"));
        mavenRepositorySystem.removeRepository(ifiRepo);
    }
    
    @Test
    public void testResolveDependenciesFromPom() throws URISyntaxException, DependencyResolutionException {
        List<File> dependencies = mavenRepositorySystem.resolveDependenciesFromPom(getTestPom());
        assertTrue("Dependencies do not match.", 
                Iterables.all(dependencies, getContainsAllFileNamesPredicate(getExpectedSet(
                    "akka-actor-2.0.jar",
                    "scala-library-2.9.1-1.jar",
                    "akka-remote-2.0.jar",
                    "netty-3.3.0.Final.jar",
                    "protobuf-java-2.4.1.jar",
                    "sjson_2.9.1-0.15.jar",
                    "dispatch-json_2.9.1-0.8.5.jar",
                    "httpclient-4.1.jar",
                    "httpcore-4.1.jar",
                    "commons-logging-1.1.1.jar",
                    "commons-codec-1.4.jar",
                    "objenesis-1.2.jar",
                    "commons-io-1.4.jar",
                    "h2-lzf-1.0.jar",
                    "server-3.0.1.jar"
                ))));
    }

    private File getTestPom() throws URISyntaxException {
        return new File(getClass().getResource("testProject/test/pom.xml").toURI());
    }
    
    private HashSet<String> getExpectedSet(String... expected) {
        return new HashSet<String>(Arrays.asList(expected));
    }
    
    private Predicate<File> getContainsAllFileNamesPredicate(final HashSet<String> expectedDependencies) {
        return new Predicate<File>() {
            @Override
            public boolean apply(File dep) {
                return expectedDependencies.contains(dep.getName());
            }
        };
    }
    
    private Predicate<Dependency> getContainsAllDependenciesPredicate(final HashSet<String> expectedDependencies) {
        return new Predicate<Dependency>() {
            @Override
            public boolean apply(Dependency dep) {
                return expectedDependencies.contains(dep.getArtifactId());
            }
        };
    }
}
