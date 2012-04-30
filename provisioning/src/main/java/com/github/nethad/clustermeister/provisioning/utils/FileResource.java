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
package com.github.nethad.clustermeister.provisioning.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author daniel
 */
public class FileResource extends Resource {
    final File resource;

    public FileResource(File resource, String remoteDeploymentDirectory) {
        super(resource.getName(), remoteDeploymentDirectory);
        this.resource = resource;
    }

    @Override
    public InputStream getResourceData() throws IOException {
        return new FileInputStream(resource);
    }

    @Override
    public long getResourceChecksum() throws IOException {
        return FileUtils.getCRC32ForFile(resource);
    }
    
}
