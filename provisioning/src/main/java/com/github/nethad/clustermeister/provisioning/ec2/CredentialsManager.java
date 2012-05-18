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

import com.github.nethad.clustermeister.api.Credentials;
import com.github.nethad.clustermeister.api.Loggers;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import org.apache.commons.configuration.Configuration;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import static org.jclouds.blobstore.options.ListContainerOptions.Builder.*;
import org.jclouds.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
public class CredentialsManager {
    private final static Logger logger = LoggerFactory.getLogger(Loggers.PROVISIONING);
    
    private final Map<String, Credentials> configuredCredentials;
    
    private final ContextManager contextManager;

    public CredentialsManager(Configuration configuration, ContextManager contextManager) {
        this.configuredCredentials = Collections.synchronizedMap(
                new AmazonConfigurationLoader(configuration).getConfiguredCredentials());
        this.contextManager = contextManager;
    }
    
    @Deprecated
    public Set<String> getConfiguredKeypairNames() {
        SortedSet<String> sortedSet = Sets.newTreeSet();
        SetView<String> union = Sets.union(configuredCredentials.keySet(), 
                getNodeKeyPairs().keySet());
        return ImmutableSet.copyOf(union.copyInto(sortedSet));
    }
    
    @Deprecated
    public Credentials getConfiguredCredentials(String keypairName) {
        Credentials result = configuredCredentials.get(keypairName);
        if(result == null) {
            result = getNodeKeyPairs().get(keypairName);
        }
        return result;
    }
    
    private Map<String, Credentials> getNodeKeyPairs() {
        String container = CredentialsBlobStoreContextBuilder.CREDENTIALS_STORE;
        BlobStore credentialsStore = contextManager.getCredentialsContext().getBlobStore();
        Map<String, Credentials> nodeCredentials = Maps.newHashMap();
        Json json = contextManager.getCredentialsContext().utils().getJson();
        
        PageSet<? extends StorageMetadata> list = credentialsStore.list(container, recursive());
        for (Iterator<? extends StorageMetadata> it = list.iterator(); it.hasNext();) {
            StorageMetadata storageMetadata = it.next();
            CredentialsBlob credentialsBlob;
            try {
                credentialsBlob = getCredentialsBlob(credentialsStore, container, 
                        storageMetadata, json);
            } catch (IOException ex) {
                logger.error("Could not read credentials from blobstore.", ex);
                continue;
            }
            if(credentialsBlob != null && credentialsBlob.isComplete()) {
                nodeCredentials.put(storageMetadata.getName(), 
                        new AmazonGeneratedKeyPairCredentials(storageMetadata.getName(),
                        credentialsBlob.user, credentialsBlob.privateKey));
            }
        }
        
        return nodeCredentials;
    }

    private CredentialsBlob getCredentialsBlob(BlobStore credentialsStore, String container, 
            StorageMetadata storageMetadata, Json json) throws IOException {
        
        Blob blob = credentialsStore.getBlob(container, storageMetadata.getName());
        byte[] bytes = ByteStreams.toByteArray(blob.getPayload());
        String privateKey = new String(bytes, Charsets.UTF_8);
        return json.fromJson(privateKey, CredentialsBlob.class);
    }
    
    /**
     * Support class to help parse JSON serialized Credentials.
     */
    class CredentialsBlob {
        private String user;
        private String privateKey;

        /**
         * Default Constructor.
         */
        CredentialsBlob() {}
        
        boolean isComplete() {
            return user != null && !user.isEmpty() && privateKey != null && !privateKey.isEmpty();
        }
    }
}
