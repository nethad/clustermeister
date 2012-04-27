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

import com.github.nethad.clustermeister.provisioning.ec2.AmazonInstanceManager;
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
 *
 * @author daniel
 */
public class DependencyManager {
    private final static Logger logger =
            LoggerFactory.getLogger(DependencyManager.class);
    
    public static Collection<File> processConfiguredDependencies(Configuration configuration) {
        List<File> artifactsToPreload = new LinkedList<File>();
        MavenRepositorySystem repositorySystem = new MavenRepositorySystem();
        List<Object> repositories = configuration.getList("maven.repository", Collections.EMPTY_LIST);
        for (Object repositorySpecification : repositories) {
            try {
                String[] repositoryInfo = repositorySpecification.toString().split("\\|");
                checkArgument(repositoryInfo.length >= 3, "repository specification needs at least three parts: id|repoLayout|url");
                RemoteRepository repo = repositorySystem.createRemoteRepository(
                      repositoryInfo[0], repositoryInfo[1], repositoryInfo[2]);
                repositorySystem.addRepository(repo);
                logger.debug("Processed repository: {}.", repo);
            } catch (Exception ex) {
                logger.warn("Could not process repository specification.", ex);
            }
        }
        
        List<Object> artifacts = configuration.getList("preload.artifact", Collections.EMPTY_LIST);
        for (Object artifactSpecification : artifacts) {
            logger.info("Resolving artifact {}.", artifactSpecification);
            try {
                List<File> dependencies = repositorySystem.resolveDependencies(artifactSpecification.toString());
                for(File artifact : dependencies) {
                    artifactsToPreload.add(artifact);
                }
                logger.debug("{} resolved to {}.", artifactSpecification, dependencies);
            } catch (DependencyResolutionException ex) {
                logger.warn("Could not resolve artifact {}.\n{}", artifactSpecification, ex.getMessage());
            }
        }
        
        List<Object> poms = configuration.getList("preload.pom", Collections.EMPTY_LIST);
        for (Object pomPath : poms) {
            logger.info("Resolving artifacts from POM file: {}.", pomPath);
            try {
                List<File> dependencies = 
                        repositorySystem.resolveDependenciesFromPom(new File(pomPath.toString()));
                for(File artifact : dependencies) {
                    artifactsToPreload.add(artifact);
                }
                logger.debug("{} resolved to {}.", pomPath, dependencies);
            } catch (DependencyResolutionException ex) {
                logger.warn("Could not resolve artifacts from {}.\n{}", pomPath, ex.getMessage());
            }
        }
        
        return artifactsToPreload;
    }
}
