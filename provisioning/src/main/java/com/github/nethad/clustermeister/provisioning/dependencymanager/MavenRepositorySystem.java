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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.repository.internal.ArtifactDescriptorUtils;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.repository.internal.MavenServiceLocator;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.resolution.DependencyResult;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;

/**
 *
 * @author daniel
 */
public class MavenRepositorySystem {
    private final static Logger logger =
            LoggerFactory.getLogger(MavenRepositorySystem.class);
    
    private RepositorySystem repositorySystem;
    private RemoteRepository centralRepository;
    private LocalRepository localRepository;
    private List<RemoteRepository> additionalRepositories;
    private Settings settings;

    public MavenRepositorySystem() {
        this.repositorySystem = initRepositorySystem();
        this.settings = buildMavenSettings();
        this.localRepository = new LocalRepository(getDefaultLocalRepository(settings));
        this.centralRepository = createCentralRepository();
        this.additionalRepositories = new ArrayList<RemoteRepository>();
    }
    
    public List<File> resolveDependencies(String groupId, String artifactId, String version) 
            throws DependencyResolutionException {
        Artifact artifact = new DefaultArtifact(groupId, artifactId, "", "jar", version);
        return resolveDependencies(artifact, Collections.EMPTY_LIST);
    }
    
    public List<File> resolveDependencies(String coords) throws DependencyResolutionException {
        Artifact artifact = new DefaultArtifact(coords);
        return resolveDependencies(artifact, Collections.EMPTY_LIST);
    }
    
    public List<File> resolveDependenciesFromPom(File pom) throws DependencyResolutionException {
        Model model = getEffectiveModel(pom);
        HashSet<File> files = new HashSet<File>();
        for(org.apache.maven.model.Dependency dependency : model.getDependencies()) {
            Artifact artifact = new DefaultArtifact(dependency.getGroupId(), 
                    dependency.getArtifactId(), dependency.getType(), 
                    dependency.getVersion());
            files.addAll(resolveDependencies(artifact, model.getRepositories()));
        }
        
        return new ArrayList<File>(files);
    }
    
    protected List<File> resolveDependencies(Artifact artifact, List<Repository> repositories) 
            throws DependencyResolutionException {
        
        RepositorySystemSession session = createSession();
        Dependency dependency = new Dependency(artifact, "runtime");
        
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        for(RemoteRepository repository : getRepositories(toRemoteRepositories(repositories))) {
            collectRequest.addRepository(repository);
        }
        DependencyRequest dependencyRequest = new DependencyRequest();
        dependencyRequest.setCollectRequest(collectRequest);
        DependencyResult result = repositorySystem.
                resolveDependencies(session, dependencyRequest);
        
        PreorderNodeListGenerator listGen = new PreorderNodeListGenerator();
        result.getRoot().accept(listGen);
        
        return listGen.getFiles();
    }
    
    public void addRepository(RemoteRepository repository) {
        this.additionalRepositories.add(repository);
    }
    
    public void removeRepository(RemoteRepository repository) {
        this.additionalRepositories.remove(repository);
    }
    
    protected Model getEffectiveModel(File pom) {
        ModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.setProcessPlugins(false);
        req.setPomFile(pom);
        req.setModelResolver(new SimpleModelResolver(repositorySystem, 
                createSession(), getRepositories(Collections.EMPTY_LIST)));
        req.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        
        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        try {
            return builder.build(req).getEffectiveModel();
        } catch(ModelBuildingException ex) {
            logger.warn("Could not build maven model.", ex);
        }
        
        return new Model();
    }

    public List<RemoteRepository> getRepositories(List<RemoteRepository> repositories) {
        int size = additionalRepositories.size() + repositories.size() + 1;
        List<RemoteRepository> remoteRepositories = new ArrayList<RemoteRepository>(
                size);
        HashSet<RemoteRepository> set = new HashSet<RemoteRepository>(size);
        set.add(centralRepository);
        remoteRepositories.add(centralRepository);
        for(RemoteRepository repository : additionalRepositories) {
            if(set.add(repository)) {
                remoteRepositories.add(repository);
            }
        }
        for(RemoteRepository repository : repositories) {
            if(set.add(repository)) {
                remoteRepositories.add(repository);
            }
        }
        
        set.clear();
        
        return Collections.unmodifiableList(remoteRepositories);
    }
    
    protected RepositorySystemSession createSession() {
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();
        session.setLocalRepositoryManager(
                repositorySystem.newLocalRepositoryManager(localRepository));
        session.setTransferListener(
                new LoggingTransferListener(LogLevel.TRACE, LogLevel.TRACE));
        session.setRepositoryListener(
                new LoggingRepositoryListener(LogLevel.TRACE, LogLevel.TRACE));
        return session;
    }
    
    private RepositorySystem initRepositorySystem() {
        MavenServiceLocator locator = new MavenServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
        locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
        locator.setServices(WagonProvider.class, new SimpleWagonProvider());

        return locator.getService(RepositorySystem.class);
    }
    
    private Settings buildMavenSettings() {
        try {
            File settingsXml = new File(
                    new File(System.getProperty("user.home"), ".m2"), 
                    "settings.xml");
            if(settingsXml.canRead()) {
                SettingsBuilder settingsBuilder = new DefaultSettingsBuilderFactory().newInstance();
                SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
                request.setSystemProperties(System.getProperties());
                request.setUserSettingsFile(settingsXml);

                return settingsBuilder.build(request).getEffectiveSettings();
            }
        } catch (SettingsBuildingException ex) {
            logger.warn("Could not build settings from user settings.xml.", ex);
        }
        
        return new Settings();
    }
    
    private File getDefaultLocalRepository(Settings settings) {
        String repoPath = settings.getLocalRepository();
        if(repoPath == null || repoPath.isEmpty()) {
            return new File(new File(System.getProperty("user.home"), ".m2"), "repository");
        } else {
            return new File(repoPath);
        }
    }
    
    protected RemoteRepository createCentralRepository() {
        return createRemoteRepository("central", "default", "http://repo1.maven.org/maven2/");
    }
    
    protected RemoteRepository createRemoteRepository(String id, String type, String url) {
        return new RemoteRepository(id, type, url);
    }
    
    private List<RemoteRepository> toRemoteRepositories(List<Repository> repositories) {
        List<RemoteRepository> remoteRepositories = 
                new ArrayList<RemoteRepository>(repositories.size());
        for(Repository repository : repositories) {
            remoteRepositories.add(ArtifactDescriptorUtils.toRemoteRepository(repository));
        }
        
        return remoteRepositories;
    }
}
