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
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.util.Iterator;
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
 * Manages credentials for the Amazon Provisioning provider.
 * 
 * Manages credentials configured in the Clustermeister Configuration file as 
 * well as Credentials that are generated at runtime or generated Credentials 
 * that have been persisted by the Amazon provisioning provider.
 *
 * @author daniel
 */
public class CredentialsManager {
    private final static Logger logger = LoggerFactory.getLogger(Loggers.PROVISIONING);
    
    private final Set<Credentials> configuredCredentials;
    
    private final ContextManager contextManager;

    /**
     * Initializes the Credentials Manager.
     * 
     * @param configuration 
     *      The Clustermeister configuration containing keypair configurations.
     * @param contextManager 
     *      The {@link ContextManager} for this provisioning context.
     */
    public CredentialsManager(Configuration configuration, ContextManager contextManager) {
        this.configuredCredentials = new AmazonConfigurationLoader(configuration).
                getConfiguredCredentials();
        this.contextManager = contextManager;
    }
    
    /**
     * Returns a sorted set with all configured, persisted and runtime credentials.
     * 
     * Note: When calling this method for the first time there may be a slight delay.
     * 
     * @return a newly created sorted set containing all currently known keypairs.
     */
    public SortedSet<Credentials> getAllCredentials() {
        SortedSet<Credentials> sortedSet = Sets.newTreeSet();
        sortedSet.addAll(ImmutableSet.copyOf(configuredCredentials));
        sortedSet.addAll(ImmutableSet.copyOf(getNodeKeyPairs()));
        
        return sortedSet;
    }
    
    /**
     * Get {@link Credentials} by name.
     * 
     * @param name  the name of the credentials (e.g. the configured name).
     * @return the credentials with the corresponding name or null if none is found.
     */
    public Credentials getCredentials(final String name) {
        SetView<Credentials> union = Sets.union(configuredCredentials, getNodeKeyPairs());
        Credentials result = Iterables.find(union, new Predicate<Credentials>() {
            @Override
            public boolean apply(Credentials input) {
                return input.getName().equals(name);
            }
        }, null);
        
        return result;
    }
    
    private Set<Credentials> getNodeKeyPairs() {
        String container = CredentialsBlobStoreContextBuilder.CREDENTIALS_STORE;
        BlobStore credentialsStore = contextManager.getCredentialsContext().getBlobStore();
        Set<Credentials> nodeCredentials = Sets.newHashSet();
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
                nodeCredentials.add(new AmazonGeneratedKeyPairCredentials(
                        storageMetadata.getName(), credentialsBlob.user, 
                        credentialsBlob.privateKey));
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
        
        /**
         * Check if both, {@code user} and {@code privateKey} are set.
         * 
         * @return true if all checked 
         */
        boolean isComplete() {
            return user != null && !user.isEmpty() && privateKey != null && !privateKey.isEmpty();
        }
    }
}
