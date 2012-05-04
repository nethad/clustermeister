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
import java.net.URISyntaxException;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import static org.hamcrest.Matchers.*;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyResolutionException;

/**
 * Tests for MavenRepositorySystem.
 *
 * @author daniel
 */
public class MavenRepositorySystemTest {

    private MavenRepositorySystem mavenRepositorySystem;
    
    @Before
    public void beforeTest() throws Exception {
        mavenRepositorySystem = new MavenRepositorySystem();
    }

    @After
    public void afterTest() throws Exception {
        mavenRepositorySystem = null;
    }
    
    @Test
    public void testEffectiveModel() throws URISyntaxException {
        Model model = mavenRepositorySystem.getEffectiveModel(getTestPom());
        assertThat(model.getDependencies(), allOf( 
                hasItem(new DependencyMatcher("akka-actor", null, null)),
                hasItem(new DependencyMatcher("akka-remote", null, null)),
                hasItem(new DependencyMatcher("server", null, null))
                ));
        assertThat(model.getDependencies().size(), is(equalTo(3)));
    }
    
    @Test
    public void testResolveDependenciesCoords() throws DependencyResolutionException {
        RemoteRepository ifiRepo = mavenRepositorySystem.createRemoteRepository(
                      "ifi", "default", "https://maven.ifi.uzh.ch/maven2/content/groups/public/");
        mavenRepositorySystem.addRepository(ifiRepo);
        List<File> dependencies = mavenRepositorySystem.resolveDependencies("org.jppf:common:3.0.1");
        assertThat(dependencies.size(), is(equalTo(1)));
        assertThat(dependencies.get(0).getName(), containsString("common"));
    }
    
    @Test
    public void testResolveDependenciesFromPom() throws URISyntaxException, DependencyResolutionException {
        List<File> dependencies = mavenRepositorySystem.resolveDependenciesFromPom(getTestPom());
        
        assertThat(dependencies, allOf( 
                hasItem(new FileNameMatcher("akka-actor-2.0.jar")),
                hasItem(new FileNameMatcher("scala-library-2.9.1-1.jar")),
                hasItem(new FileNameMatcher("akka-remote-2.0.jar")),
                hasItem(new FileNameMatcher("protobuf-java-2.4.1.jar")),
                hasItem(new FileNameMatcher("sjson_2.9.1-0.15.jar")),
                hasItem(new FileNameMatcher("dispatch-json_2.9.1-0.8.5.jar")),
                hasItem(new FileNameMatcher("httpclient-4.1.jar")),
                hasItem(new FileNameMatcher("httpcore-4.1.jar")),
                hasItem(new FileNameMatcher("commons-logging-1.1.1.jar")),
                hasItem(new FileNameMatcher("commons-codec-1.4.jar")),
                hasItem(new FileNameMatcher("objenesis-1.2.jar")),
                hasItem(new FileNameMatcher("commons-io-1.4.jar")),
                hasItem(new FileNameMatcher("h2-lzf-1.0.jar")),
                hasItem(new FileNameMatcher("server-3.0.1.jar"))
                ));
        assertThat(dependencies.size(), is(equalTo(14)));
    }
    
    @Test
    public void testResolveDependenciesFromPomWithGlobalExclusion() throws URISyntaxException, DependencyResolutionException {
        mavenRepositorySystem.addGlobalExclusion("voldemort.store.compress:h2-lzf::");
        List<File> dependencies = mavenRepositorySystem.resolveDependenciesFromPom(getTestPom());
        assertThat(dependencies, allOf( 
                hasItem(new FileNameMatcher("akka-actor-2.0.jar")),
                hasItem(new FileNameMatcher("scala-library-2.9.1-1.jar")),
                hasItem(new FileNameMatcher("akka-remote-2.0.jar")),
                hasItem(new FileNameMatcher("protobuf-java-2.4.1.jar")),
                hasItem(new FileNameMatcher("sjson_2.9.1-0.15.jar")),
                hasItem(new FileNameMatcher("dispatch-json_2.9.1-0.8.5.jar")),
                hasItem(new FileNameMatcher("httpclient-4.1.jar")),
                hasItem(new FileNameMatcher("httpcore-4.1.jar")),
                hasItem(new FileNameMatcher("commons-logging-1.1.1.jar")),
                hasItem(new FileNameMatcher("commons-codec-1.4.jar")),
                hasItem(new FileNameMatcher("objenesis-1.2.jar")),
                hasItem(new FileNameMatcher("commons-io-1.4.jar")),
                hasItem(new FileNameMatcher("server-3.0.1.jar"))
                ));
        assertThat(dependencies.size(), is(equalTo(13)));
    }

    private File getTestPom() throws URISyntaxException {
        return new File(getClass().getResource("testProject/test/pom.xml").toURI());
    }
    
    private class FileNameMatcher extends BaseMatcher<File> {
        private final String expectedName;

        public FileNameMatcher(String expectedName) {
            this.expectedName = expectedName;
        }

        @Override
        public boolean matches(Object item) {
            if(item == null) {
                return false;
            }
            if(item instanceof File) {
                File file = (File) item;
                boolean result = false;
                if(expectedName != null && !expectedName.isEmpty()) {
                    result = file.getName().equals(expectedName);
                }
                return result;
            } else {
                return false;
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(expectedName);
        }
    }
    
    private class DependencyMatcher extends BaseMatcher<Dependency> {
        
        private final String artifactId;
        private final String groupId;
        private final String version;

        public DependencyMatcher(String artifactId, String groupId, String version) {
            this.artifactId = artifactId;
            this.groupId = groupId;
            this.version = version;
        }
        
        @Override
        public boolean matches(Object item) {
            if(item == null) {
                return false;
            }
            if(item instanceof Dependency) {
                Dependency dep = (Dependency) item;
                boolean result = false;
                if(artifactId != null && !artifactId.isEmpty()) {
                    result = dep.getArtifactId().contains(artifactId);
                }
                if(groupId != null && !groupId.isEmpty()) {
                    result = dep.getGroupId().contains(groupId);
                }
                if(version != null && !version.isEmpty()) {
                    result = dep.getVersion().contains(version);
                }
                return result;
            } else {
                return false;
            }
        }

        @Override
        public void describeTo(Description description) {
            String desc = String.format("Dependency {groupId=%s, artifactId=%s, version=%s, type=jar}", 
                    groupId == null ? "*" : groupId,
                    artifactId == null ? "*" : artifactId,
                    version == null ? "*" : version);
            description.appendText(desc);
        }
        
    }
}
