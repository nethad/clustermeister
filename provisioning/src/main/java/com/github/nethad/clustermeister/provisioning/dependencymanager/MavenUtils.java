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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.repository.internal.MavenServiceLocator;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;

/**
 *
 * @author daniel
 */
public class MavenUtils {
    
    private static MavenXpp3Reader reader = null;
    private static RepositorySystem repositorySystem = null;
    private static RepositorySystemSession repositorySystemSession = null;

    public static void getDependencies(InputStream pom) {
        for(Dependency dependency : getSimpleModel(pom).getDependencies()) {
            System.out.println(dependency.getManagementKey());
            try {
                DependencyResolver.main(dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion());
            } catch (DependencyCollectionException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Returns the Model for a POM file.
     * 
     * This method only returns the content of the parsed POM file and does not 
     * take into account POM inheritance.
     * 
     * @param pom   the POM file.
     * @return  the POM Model.
     */
    protected static Model getSimpleModel(InputStream pom) {
        try {
            return getReader().read(pom);
        } catch (IOException ex) {
            throw new RuntimeException("Can not create model from POM.", ex);
        } catch (XmlPullParserException ex) {
            throw new RuntimeException("Can not create model from POM.", ex);
        }
    }
    
    protected static Model getEffectiveModel(File pom, RemoteRepository... remoteRepositories) {
        ModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.setProcessPlugins(false);
        req.setPomFile(pom);
        req.setModelResolver(new SimpleModelResolver(getRepositorySystem(), 
                getRepositorySystemSession(), remoteRepositories));
        req.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        
        Model model = null;
        ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        try {
            model = builder.build(req).getEffectiveModel();
        } catch(ModelBuildingException ex) {
            ex.printStackTrace();
        }
        
        return model;
    }
    
    /**
     * Returns the POM reader and initializes it if it has not been done before 
     * (lazy initialization).
     * 
     * @return an XML reader to read pom files.
     */
    protected static MavenXpp3Reader getReader() {
        if (reader == null) {
            reader = new MavenXpp3Reader();
        }

        return reader;
    }

    /**
     * Returns the RepositorySystem and initializes it if it has not been 
     * done before (lazy initialization).
     * 
     * @return an RepositorySystem.
     */
    public static RepositorySystem getRepositorySystem() {
        if (repositorySystem == null) {
            repositorySystem = newRepositorySystem();
        }

        return repositorySystem;
    }
    
    /**
     * Returns the RepositorySystemSession and initializes it if it has not been 
     * done before (lazy initialization).
     * 
     * @return an RepositorySystemSession.
     */
    protected static RepositorySystemSession getRepositorySystemSession() {
        if (repositorySystemSession == null) {
            repositorySystemSession = newRepositorySystemSession(getRepositorySystem());
        }

        return repositorySystemSession;
    }
    
    public static RepositorySystem newRepositorySystem() {
        MavenServiceLocator locator = new MavenServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
        locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
        locator.setServices(WagonProvider.class, new DependencyResolver.ManualWagonProvider());

        return locator.getService(RepositorySystem.class);
    }
    
    public static RepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();

        LocalRepository localRepo = new LocalRepository("target/local-repo");
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepo));

        session.setTransferListener(new DependencyResolver.ConsoleTransferListener());
        session.setRepositoryListener(new DependencyResolver.ConsoleRepositoryListener());

        // uncomment to generate dirty trees
        // session.setDependencyGraphTransformer( null );

        return session;
    }
    
    public static RemoteRepository newCentralRepository() {
        return newRemoteRepository("central", "default", "http://repo1.maven.org/maven2/");
    }
    
    public static RemoteRepository newRemoteRepository(String id, String type, String url) {
        return new RemoteRepository(id, type, url);
    }
}
