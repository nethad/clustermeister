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
package com.github.nethad.clustermeister.provisioning;

import com.github.nethad.clustermeister.provisioning.utils.FileUtils;
import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a resource that can be uploaded to and deployed on remote 
 * Clustermeister instances.
 * 
 * This class loads a resource from the classpath using the 
 * {@link Class#getResourceAsStream(java.lang.String)} method.
 * 
 * Typical examples are library jars or zip files that are needed to set up a 
 * remote JPPF node.
 * 
 * A resource defines a checksum test to check if an upload is necessary and a 
 * deployment method that defines how and where a resource is deployed on a 
 * remote instance.
 *
 * @author daniel
 */
public class InputStreamResource extends Resource {
    private final String resource;
    private final Class clazz;

    /**
     * Creates a new InputStreamResource.
     * 
     * 
     * @param resourcePath  
     *      The path to the resource in the classpath, relative from 
     *      {@code clazz}. Can also be an absolute path starting at the root of 
     *      the classpath.
     * @param clazz 
     *      Defines the location from where to search for {@code resourcePath}.
     *      The resource is searched by invoking 
     *      {@link Class#getResourceAsStream(java.lang.String)} on {@code clazz}.
     * @param resourceName  
     *      A name for this resource. This is used as file-name on the remote 
     *      instance.
     * @param remoteDeploymentDirectory 
     *      Where to deploy this resource on a remote Clustermeister instance. 
     *      The directory can be absolute or relative to the home directory of 
     *      the configured SSH user.
     */
    public InputStreamResource(String resourcePath, Class clazz, String resourceName, String remoteDeploymentDirectory) {
        super(resourceName, remoteDeploymentDirectory);
        this.resource = resourcePath;
        this.clazz = clazz;
    }

    @Override
    public InputStream getResourceData() throws IOException {
        return clazz.getResourceAsStream(resource);
    }

    @Override
    public long getResourceChecksum() throws IOException {
        return FileUtils.getCRC32(getResourceData());
    }
    
}
