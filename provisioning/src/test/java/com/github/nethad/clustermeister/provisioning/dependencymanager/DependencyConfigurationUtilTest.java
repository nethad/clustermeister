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
import java.util.List;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hamcrest.Matcher;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import static org.mockito.Matchers.*;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyResolutionException;

/**
 * Tests for DependencyConfigurationUtil.
 *
 * @author daniel
 */
@RunWith(MockitoJUnitRunner.class)
public class DependencyConfigurationUtilTest {
    private static final String EXAMPLE_POM_PATH = "testProject/pom.xml";
    private static final String EXAMPLE_POM2_PATH = "testProject/test/pom.xml";
    
    private static final String EXAMPLE_REPO_ID = "test";
    private static final String EXAMPLE_REPO_TYPE = "default";
    private static final String EXAMPLE_REPO_URL = "http://example.org/maven/repo";
    private static final String EXAMPLE_MAVEN_REPO = String.format("%s|%s|%s", 
            EXAMPLE_REPO_ID, EXAMPLE_REPO_TYPE, EXAMPLE_REPO_URL);
    
    private static final String EXAMPLE_REPO2_ID = "test2";
    private static final String EXAMPLE_REPO2_TYPE = "legacy";
    private static final String EXAMPLE_REPO2_URL = "http://example.org/maven/repo2";
    private static final String EXAMPLE_MAVEN_REPO2 = String.format("%s|%s|%s", 
            EXAMPLE_REPO2_ID, EXAMPLE_REPO2_TYPE, EXAMPLE_REPO2_URL);
    
    private static final String EXAMPLE_APP_GROUPID = "org.example";
    private static final String EXAMPLE_APP_ARTIFACTID = "best.app.ever";
    private static final String EXAMPLE_APP_VERSION = "1.0";
    private static final String EXAMPLE_APP_COORDS = String.format("%s:%s:%s", 
            EXAMPLE_APP_GROUPID, EXAMPLE_APP_ARTIFACTID, EXAMPLE_APP_VERSION);
    
    private static final String EXAMPLE_APP2_GROUPID = "org.example";
    private static final String EXAMPLE_APP2_ARTIFACTID = "second.best.app.ever";
    private static final String EXAMPLE_APP2_VERSION = "1.5-SNAPSHOT";
    private static final String EXAMPLE_APP2_COORDS = String.format("%s:%s:%s", 
            EXAMPLE_APP2_GROUPID, EXAMPLE_APP2_ARTIFACTID, EXAMPLE_APP2_VERSION);
    
    @Mock
    private MavenRepositorySystem mavenRepositorySystem;
    
    @Before
    public void setUp() throws DependencyResolutionException {
        when(mavenRepositorySystem.resolveDependencies(anyString())).thenCallRealMethod();
        when(mavenRepositorySystem.createRemoteRepository(
                anyString(), anyString(), anyString())).thenCallRealMethod();
        
        DependencyConfigurationUtil.repositorySystem = mavenRepositorySystem;
    }

    @Test
    public void testPreloadArtifact() throws DependencyResolutionException {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.addProperty(DependencyConfigurationUtil.PRELOAD_ARTIFACTS, 
                EXAMPLE_APP_COORDS);
        
        DependencyConfigurationUtil.getConfiguredDependencies(configuration);
        
        ArgumentCaptor<Artifact> artifact = ArgumentCaptor.forClass(Artifact.class);
        
        verify(mavenRepositorySystem, times(1)).resolveDependencies(artifact.capture(), 
                argThat(isEmptyList()), argThat(isEmptyList()));
        assertThat(artifact.getValue().getGroupId(), is(equalTo(EXAMPLE_APP_GROUPID)));
        assertThat(artifact.getValue().getArtifactId(), is(equalTo(EXAMPLE_APP_ARTIFACTID)));
        assertThat(artifact.getValue().getVersion(), is(equalTo(EXAMPLE_APP_VERSION)));
    }
    
    @Test
    public void testPreloadMultipleArtifacts() throws DependencyResolutionException {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.addProperty(DependencyConfigurationUtil.PRELOAD_ARTIFACTS, 
                EXAMPLE_APP_COORDS);
        configuration.addProperty(DependencyConfigurationUtil.PRELOAD_ARTIFACTS, 
                EXAMPLE_APP2_COORDS);
        
        DependencyConfigurationUtil.getConfiguredDependencies(configuration);
        
        verify(mavenRepositorySystem, times(1)).resolveDependencies(EXAMPLE_APP_COORDS);
        verify(mavenRepositorySystem, times(1)).resolveDependencies(EXAMPLE_APP2_COORDS);
    }
    
    @Test
    public void testMavenRepository() throws DependencyResolutionException {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.addProperty(DependencyConfigurationUtil.MAVEN_REPOSITORIES, 
                EXAMPLE_MAVEN_REPO);
        configuration.addProperty(DependencyConfigurationUtil.MAVEN_REPOSITORIES, 
                EXAMPLE_MAVEN_REPO2);
        
        DependencyConfigurationUtil.getConfiguredDependencies(configuration);
        
        ArgumentCaptor<RemoteRepository> repo = ArgumentCaptor.forClass(RemoteRepository.class);
        
        verify(mavenRepositorySystem, times(2)).addRepository(repo.capture());
        assertThat(repo.getAllValues().get(0).getId(), is(equalTo(EXAMPLE_REPO_ID)));
        assertThat(repo.getAllValues().get(0).getContentType(), is(equalTo(EXAMPLE_REPO_TYPE)));
        assertThat(repo.getAllValues().get(0).getUrl(), is(equalTo(EXAMPLE_REPO_URL)));
        assertThat(repo.getAllValues().get(1).getId(), is(equalTo(EXAMPLE_REPO2_ID)));
        assertThat(repo.getAllValues().get(1).getContentType(), is(equalTo(EXAMPLE_REPO2_TYPE)));
        assertThat(repo.getAllValues().get(1).getUrl(), is(equalTo(EXAMPLE_REPO2_URL)));
    }
    
    @Test
    public void testExclusion() throws DependencyResolutionException {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.addProperty(DependencyConfigurationUtil.PRELOAD_EXCLUDES, 
                EXAMPLE_APP_COORDS);
        configuration.addProperty(DependencyConfigurationUtil.PRELOAD_EXCLUDES, 
                EXAMPLE_APP2_COORDS);
        
        DependencyConfigurationUtil.getConfiguredDependencies(configuration);
        
        verify(mavenRepositorySystem).addGlobalExclusion(eq(EXAMPLE_APP_COORDS));
        verify(mavenRepositorySystem).addGlobalExclusion(eq(EXAMPLE_APP2_COORDS));
    }
    
    @Test
    public void testPom() throws DependencyResolutionException {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        String pomPath = getClass().getResource(EXAMPLE_POM_PATH).getPath();
        String pom2Path = getClass().getResource(EXAMPLE_POM2_PATH).getPath();
        configuration.addProperty(DependencyConfigurationUtil.PRELOAD_POMS, pomPath);
        configuration.addProperty(DependencyConfigurationUtil.PRELOAD_POMS, pom2Path);
        
        DependencyConfigurationUtil.getConfiguredDependencies(configuration);
        
        verify(mavenRepositorySystem, times(1)).resolveDependenciesFromPom(eq(new File(pomPath)));
        verify(mavenRepositorySystem, times(1)).resolveDependenciesFromPom(eq(new File(pom2Path)));
    }
    
    private Matcher<List> isEmptyList() {
        return new IsEmptyList();
    }
    
    private class IsEmptyList extends ArgumentMatcher<List> {
        @Override
        public boolean matches(Object argument) {
            return ((List) argument).isEmpty();
        }
    }
}
