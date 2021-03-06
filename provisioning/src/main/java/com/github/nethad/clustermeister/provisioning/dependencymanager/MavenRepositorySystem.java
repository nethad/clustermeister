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

import com.github.nethad.clustermeister.api.LogLevel;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.Exclusion;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.resolution.DependencyResult;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;
import org.sonatype.aether.util.filter.DependencyFilterUtils;
import org.sonatype.aether.util.filter.PatternExclusionsDependencyFilter;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;

/**
 * Resolved dependencies from Maven style repositories.
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
    private Set<String> globalExclusions;

    /**
     * Creates a new MavenRepositorySystem.
     */
    public MavenRepositorySystem() {
        this.repositorySystem = initRepositorySystem();
        this.settings = buildMavenSettings();
        this.localRepository = new LocalRepository(getDefaultLocalRepository(settings));
        this.centralRepository = createCentralRepository();
        this.additionalRepositories = new ArrayList<RemoteRepository>();
        this.globalExclusions = new HashSet<String>();
    }
    
    /**
     * Resolve an artifact and all its runtime dependencies.
     * 
     * @param groupId       the artifact group ID.
     * @param artifactId    the artifact ID.
     * @param version       the artifact version.
     * @return 
     *      The artifact and all its runtime dependencies as files.
     * @throws DependencyResolutionException 
     *      If the artifact can not be properly resolved.
     */
    public List<File> resolveDependencies(String groupId, String artifactId, 
            String version) throws DependencyResolutionException {
        Artifact artifact = 
                new DefaultArtifact(groupId, artifactId, "", "jar", version);
        return resolveDependencies(artifact, Collections.EMPTY_LIST, 
                Collections.EMPTY_LIST);
    }
    
    /**
     * Resolve an artifact and all its runtime dependencies.
     * 
     * <p>
     *  The artifact specification uses the Maven/Aether artifact coords syntax:
     * </p>
     * <p>
     *  Format: &lt;groupId&gt;:&lt;artifactId&gt;[:&lt;extension&gt;[:&lt;classifier&gt;]]:&lt;version&gt;
     * <br/>
     *  Example: my.group:my.artifact:1.0-SNAPSHOT
     * </p>
     * 
     * @param coords the artifact specification.
     * @return 
     *      The artifact and all its runtime dependencies as files.
     * @throws DependencyResolutionException 
     *      If the artifact can not be properly resolved.
     */
    public List<File> resolveDependencies(String coords) 
            throws DependencyResolutionException {
        Artifact artifact = new DefaultArtifact(coords);
        return resolveDependencies(artifact, Collections.EMPTY_LIST, 
                Collections.EMPTY_LIST);
    }
    
    /**
     * Resolve all dependencies and transitive runtime dependencies specified in
     * a POM file.
     * 
     * <p>
     *  Additional information extracted from the POM:
     *  <ul> 
     *      <li>Dependency exclusions.</li>
     *      <li>Repository specifications.</li>
     *      <li>Parent POMs are parsed as well (if they can be found).</li>
     *  </ul> 
     * </p>
     * 
     * @param pom   the POM (pom.xml) file.
     * @return  
     *      All dependencies specified in the POM and their transitive runtime 
     *      dependencies as files.
     * @throws DependencyResolutionException 
     *      If some artifact can not be properly resolved.
     */
    public List<File> resolveDependenciesFromPom(File pom) 
            throws DependencyResolutionException {
        Model model = getEffectiveModel(pom);
        HashSet<File> files = new HashSet<File>();
        for(org.apache.maven.model.Dependency dependency : model.getDependencies()) {
            Artifact artifact = new DefaultArtifact(dependency.getGroupId(), 
                    dependency.getArtifactId(), dependency.getType(), 
                    dependency.getVersion());
            files.addAll(resolveDependencies(artifact, model.getRepositories(), 
                    getSonatypeExclusions(dependency)));
        }
        
        return new ArrayList<File>(files);
    }

    /**
     * Resolve an artifact and its transitive runtime dependencies given a list 
     * of repositories and artifact exclusions.
     * 
     * @param artifact  
     *      The artifact to resolve.
     * @param repositories  
     *      Additional repositories to use for dependency resolution.
     * @param exclusions
     *      Artifacts not to include in the final list of files.
     * @return 
     *      The artifact and its transitive runtime dependencies as files.
     * @throws DependencyResolutionException 
     *      If the artifact can not be properly resolved.
     */
    protected List<File> resolveDependencies(Artifact artifact, 
            List<Repository> repositories, List<Exclusion> exclusions) 
            throws DependencyResolutionException {
        
        RepositorySystemSession session = createSession();
        Dependency dependency = new Dependency(artifact, JavaScopes.RUNTIME, 
                false, exclusions);
        DependencyFilter classpathFilter = 
                DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME);
        
        PatternExclusionsDependencyFilter patternExclusionFilter = 
                new PatternExclusionsDependencyFilter(globalExclusions);
        DependencyFilter filter = DependencyFilterUtils.andFilter(classpathFilter, 
                patternExclusionFilter);
        
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        for(RemoteRepository repository : getRepositories(toRemoteRepositories(repositories))) {
            collectRequest.addRepository(repository);
        }
        DependencyRequest dependencyRequest = new DependencyRequest();
        dependencyRequest.setCollectRequest(collectRequest);
        dependencyRequest.setFilter(filter);
        DependencyResult result = repositorySystem.resolveDependencies(session, 
                dependencyRequest);
        
        PreorderNodeListGenerator listGen = new PreorderNodeListGenerator();
        result.getRoot().accept(listGen);
        
        return listGen.getFiles();
    }
    
    /**
     * Add an additional repository.
     * 
     * NOTE: the local repository and the Maven central repository are added 
     * by default.
     * 
     * @param repository The repository to use.
     */
    public void addRepository(RemoteRepository repository) {
        this.additionalRepositories.add(repository);
    }
    
    /**
     * Remove a repository.
     * 
     * @param repository the repository to remove.
     */
    public void removeRepository(RemoteRepository repository) {
        this.additionalRepositories.remove(repository);
    }
    
    /**
     * Specify a pattern that excludes all matching artifacts from any 
     * dependency resolution.
     * 
     * Each pattern segment is optional and supports full and partial * wildcards. 
     * An empty pattern segment is treated as an implicit wildcard.
     * 
     * For example, org.apache.* would match all artifacts whose group id started 
     * with org.apache. , and :::*-SNAPSHOT would match all snapshot artifacts.
     * 
     * @param pattern [groupId]:[artifactId]:[extension]:[version]
     */
    public void addGlobalExclusion(String pattern) {
        globalExclusions.add(pattern);
    }
    
    /**
     * Resolve the effective Maven model (pom) for a POM file.
     * 
     * This resolves the POM hierarchy (parents and modules) and creates an 
     * overall model.
     * 
     * @param pom the POM file to resolve.
     * @return the effective model.
     */
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

    /**
     * Get all configured repositories.
     * 
     * @param repositories additional repositories to include.
     * @return an unmodifyable list of repositories.
     */
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
    
    /**
     * Creates a repository session.
     * 
     * @return the repository session.
     */
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
    
    /**
     * Creates Sonatype exclusions from Maven dependency exclusions.
     * 
     * @param dependency the Maven dependency.
     * @return the Sonatype exclusion.
     */
    protected List<Exclusion> getSonatypeExclusions(org.apache.maven.model.Dependency dependency) {
        List<org.apache.maven.model.Exclusion> mavenExclusions = dependency.getExclusions();
        List<Exclusion> exclusions = new ArrayList<Exclusion>(mavenExclusions.size());
        for(org.apache.maven.model.Exclusion mavenExclusion : mavenExclusions) {
            exclusions.add(new Exclusion(stringOrWildcard(mavenExclusion.getGroupId()), 
                    stringOrWildcard(mavenExclusion.getArtifactId()), "*", "*"));
        }
        return exclusions;
    }
    
    private String stringOrWildcard(String string) {
        if(string == null || string.isEmpty()) {
            return "*";
        }
        return string;
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
    
    /**
     * Creates the Maven central repository specification.
     * 
     * @return the Maven central repository specification.
     */
    protected RemoteRepository createCentralRepository() {
        return createRemoteRepository("central", "default", "http://repo1.maven.org/maven2/");
    }
    
    /**
     * Creates a repository specification.
     * 
     * @param id    some user defined ID for the repository
     * @param type  the repository type. typically "default".
     * @param url   the repository URL.
     * @return the repository specification.
     */
    public RemoteRepository createRemoteRepository(String id, String type, String url) {
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
