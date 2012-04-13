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
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.aether.collection.DependencyCollectionException;

/**
 *
 * @author daniel
 */
public class MavenUtils {
    
    private static MavenXpp3Reader reader = null;

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
    
    protected static Model getEffectiveModel(InputStream pom) {
        try {
            return getReader().read(pom);
        } catch (IOException ex) {
            throw new RuntimeException("Can not create model from POM.", ex);
        } catch (XmlPullParserException ex) {
            throw new RuntimeException("Can not create model from POM.", ex);
        }
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
    
}
