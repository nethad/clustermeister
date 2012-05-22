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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.inject.Module;
import java.util.Properties;
import java.util.concurrent.Callable;
import org.jclouds.blobstore.InputStreamMap;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.enterprise.config.EnterpriseConfigurationModule;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.rest.config.CredentialStoreModule;
import org.jclouds.ssh.jsch.config.JschSshClientModule;

/**
 *
 * @author daniel
 */
public class AmazonComputeContextBuilder implements Callable<ComputeServiceContext> {
    
    private final String accessKeyId;
    private final Builder<Module> modulesBuilder = ImmutableSet.builder();
    private final String secretKey;
    private final Properties overrides;

    public AmazonComputeContextBuilder(String accessKeyId, String secretKey,
            Optional<Properties> overrides, Optional<InputStreamMap> credentialsMap) {
        this.accessKeyId = accessKeyId;
        this.secretKey = secretKey;
        if (overrides.isPresent()) {
            this.overrides = overrides.get();
        } else {
            this.overrides = new Properties();
        }
        modulesBuilder.add(new JschSshClientModule()).
                add(new SLF4JLoggingModule()).
                add(new EnterpriseConfigurationModule());
        if(credentialsMap.isPresent()) {
            modulesBuilder.add(new CredentialStoreModule(credentialsMap.get()));
        }
    }

    @Override
    public ComputeServiceContext call() throws Exception {
        return new ComputeServiceContextFactory().createContext("aws-ec2", 
                accessKeyId, secretKey, modulesBuilder.build(), overrides);
    }
}
