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
package com.github.nethad.clustermeister.provisioning.ec2;

import com.github.nethad.clustermeister.api.impl.FileConfiguration;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import java.util.Properties;
import java.util.concurrent.Callable;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.BlobStoreContextFactory;
import org.jclouds.filesystem.reference.FilesystemConstants;

/**
 *
 * @author daniel
 */
public class CredentialsBlobStoreContextBuilder implements Callable<BlobStoreContext> {
    public static final String CREDENTIALS_STORE = "credentials";

    private final Properties overrides;

    public CredentialsBlobStoreContextBuilder(Optional<Properties> overrides) {
        if (overrides.isPresent()) {
            this.overrides = overrides.get();
        } else {
            this.overrides = new Properties();
        }
        //TODO: CM_HOME should be definedelsewhere
        this.overrides.setProperty(
                FilesystemConstants.PROPERTY_BASEDIR, FileConfiguration.CLUSTERMEISTER_HOME);
    }
    
    @Override
    public BlobStoreContext call() throws Exception {
        BlobStoreContext context = 
                new BlobStoreContextFactory().createContext("filesystem", "", "", 
                ImmutableSet.<Module>of(), overrides);
        context.getBlobStore().createContainerInLocation(null, CREDENTIALS_STORE);
        return context;
    }
}
