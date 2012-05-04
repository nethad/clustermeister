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

import com.google.common.annotations.VisibleForTesting;
import static com.google.common.base.Preconditions.*;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyResolutionException;

/**
 * Extracts dependency preload configurations from a {@link Configuration} and 
 * resolves them using {@link MavenRepositorySystem}.
 *
 * @author daniel
 */
public class DependencyConfigurationUtil {
    
    /**
     * A Maven repository configuration.
     * <p>
     *  Sometimes additional Maven repositories need to be configured in order 
     *  to resolve artifacts.
     * </p>
     * <p>
     *  Format: &lt;id&gt;|&lt;repoLayout&gt;|&lt;url&gt;
     *  <br/>
     *  Example: central|default|http://repo1.maven.org/maven2/
     * </p>
     * <p>
     *  NOTE: The Maven central repository is already pre-configured by default.
     * </p>
     */
    public static final String MAVEN_REPOSITORIES = "preload.maven_repositories";
    
    /**
     * A Maven artifact specification that will be preloaded on to JPPF Nodes 
     * when they are deployed.
     * <p>
     *  The artifact and all its runtime dependencies are resolved preloaded.
     * </p>
     * <p>
     *  The artifact specification uses the Maven/Aether artifact coords syntax:
     * </p>
     * <p>
     *  Format: &lt;groupId&gt;:&lt;artifactId&gt;[:&lt;extension&gt;[:&lt;classifier&gt;]]:&lt;version&gt;
     * <br/>
     *  Example: my.group:my.artifact:1.0-SNAPSHOT
     * </p>
     * 
     * @see #PRELOAD_EXCLUDE
     * @see #PRELOAD_POM
     */
    public static final String PRELOAD_ARTIFACTS = "preload.artifacts";
    
    /**
     * A Maven artifact specification that will be excluded from dependency 
     * resolution.
     * 
     * <p>
     *  This is useful if you want to exclude problematic dependencies to be 
     *  uploaded.
     * </p>
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
     * @see #PRELOAD_ARTIFACT
     * @see #PRELOAD_POM
     */
    public static final String PRELOAD_EXCLUDES = "preload.excludes";
    
    /**
     * A path to a pom.xml file that will be used to preload all dependencies
     * specified in it.
     * 
     * <p>
     *  NOTE: For each dependency specified in the pom all runtime dependencies 
     *  are resolved as well.
     * </p>
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
     * @see #PRELOAD_ARTIFACT
     * @see #PRELOAD_EXCLUDE
     */
    public static final String PRELOAD_POMS = "preload.poms";
    
    private final static Logger logger =
            LoggerFactory.getLogger(DependencyConfigurationUtil.class);
    
    /**
     * The repository system instance to use.
     */
    @VisibleForTesting
    static MavenRepositorySystem repositorySystem = null;

    
    /**
     * Resolves preload dependencies from a {@link Configuration} and resolves 
     * them using {@link MavenRepositorySystem}.
     * 
     * @param configuration The configuration specifying preload dependencies.
     * @return All resolved dependencies as files.
     */
    public static Collection<File> getConfiguredDependencies(Configuration configuration) {
        if(repositorySystem == null) {
            repositorySystem = new MavenRepositorySystem();
        }
        
        List<File> artifactsToPreload = new LinkedList<File>();
        
        addDefaultGlobalExclusions();
        
        List<Object> excludePatterns = configuration.getList(PRELOAD_EXCLUDES, 
                Collections.EMPTY_LIST);
        for (Object excludePattern : excludePatterns) {
            String excludePatternString = excludePattern.toString();
            if(excludePattern != null && !excludePatternString.isEmpty()) {
                logger.info("Excluding {} from dependency resolution.", excludePattern);
                repositorySystem.addGlobalExclusion(excludePatternString);
            }
        }
        
        List<Object> repositories = configuration.getList(MAVEN_REPOSITORIES, 
                Collections.EMPTY_LIST);
        for (Object repositorySpecification : repositories) {
            try {
                String[] repositoryInfo = repositorySpecification.toString().split("\\|");
                String helpMessage = "A repository specification needs at least three parts: id|repoLayout|url.";
                checkArgument(repositoryInfo.length >= 3, helpMessage);
                RemoteRepository repo = repositorySystem.createRemoteRepository(
                      repositoryInfo[0], repositoryInfo[1], repositoryInfo[2]);
                repositorySystem.addRepository(repo);
                logger.info("Adding repository for dependency resolution: {}.", repo);
            } catch (Exception ex) {
                logger.warn("Could not process repository specification.", ex);
            }
        }
        
        List<Object> artifacts = configuration.getList(PRELOAD_ARTIFACTS, 
                Collections.EMPTY_LIST);
        for (Object artifactSpecification : artifacts) {
            logger.info("Resolving artifact {}.", artifactSpecification);
            try {
                List<File> dependencies = repositorySystem.
                        resolveDependencies(artifactSpecification.toString());
                addToListIfUnique(dependencies, artifactsToPreload);
                logger.debug("{} resolved to {}.", artifactSpecification, dependencies);
            } catch (DependencyResolutionException ex) {
                logger.warn("Could not resolve artifact {}.", artifactSpecification, ex);
            }
        }
        
        List<Object> poms = configuration.getList(PRELOAD_POMS, 
                Collections.EMPTY_LIST);
        for (Object pomPath : poms) {
            logger.info("Resolving artifacts from POM file: {}.", pomPath);
            try {
                List<File> dependencies = repositorySystem.
                        resolveDependenciesFromPom(new File(pomPath.toString()));
                addToListIfUnique(dependencies, artifactsToPreload);
                logger.debug("{} resolved to {}.", pomPath, dependencies);
            } catch (DependencyResolutionException ex) {
                logger.warn("Could not resolve artifacts from {}.", pomPath, ex);
            }
        }
        
        repositorySystem = null;
        
        return artifactsToPreload;
    }

    private static void addToListIfUnique(List<File> dependencies, List<File> artifactsToPreload) {
        for(File artifact : dependencies) {
            if(!artifactsToPreload.contains(artifact)) {
                artifactsToPreload.add(artifact);
            }
        }
    }

    private static void addDefaultGlobalExclusions() {
        //dependencies known to be present in jppf node or known to cause problems
        MavenRepositorySystem rs = repositorySystem;
        rs.addGlobalExclusion("org.jppf");
        rs.addGlobalExclusion("com.github.nethad.clustermeister:clustermeister");
        rs.addGlobalExclusion("com.github.nethad.clustermeister:provisioning");
        rs.addGlobalExclusion("com.github.nethad.clustermeister:api");
        rs.addGlobalExclusion("com.github.nethad.clustermeister:cli");
        rs.addGlobalExclusion("com.github.nethad.clustermeister:common-node");
        rs.addGlobalExclusion("com.github.nethad.clustermeister:node");
        rs.addGlobalExclusion("com.github.nethad.clustermeister:driver");
        rs.addGlobalExclusion("org.jvnet.opendmk:jmxremote_optional");
        rs.addGlobalExclusion("log4j");
        rs.addGlobalExclusion("slf4j");
    }
}
