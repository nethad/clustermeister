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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.io.Closeable;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jclouds.aws.ec2.reference.AWSEC2Constants;
import org.jclouds.compute.ComputeServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
public class ComputeContextManager implements Closeable {
    private final static Logger logger =
            LoggerFactory.getLogger(ComputeContextManager.class);
    
    private ListenableFuture<ComputeServiceContext> eagerContext = null;
    private final Lock eagerLock = new ReentrantLock();
    private ListenableFuture<ComputeServiceContext> lazyContext = null;
    private final Lock lazyLock = new ReentrantLock();
    
    private final ListeningExecutorService executorService;
    private final String accessKeyId;
    private final String secretKey;
    
    public ComputeContextManager(String accessKeyId, String secretKey, 
            ListeningExecutorService executorService) {
        this.accessKeyId = accessKeyId;
        this.secretKey = secretKey;
        this.executorService = executorService;
    }
    
    public ComputeServiceContext getEagerContext() {
        eagerLock.lock();
        try {
            if(eagerContext == null) {
                eagerContext = executorService.submit(
                        new AmazonContextBuilder(accessKeyId, secretKey,
                        Optional.<Properties>absent()));
            }
            return valueOrNotReady(eagerContext);
        } finally {
            eagerLock.unlock();
        }
    }
    
    public ComputeServiceContext getLazyContext() {
        //Optimization: lazy image fetching
        //set AMI queries to nothing
        Properties properties = new Properties();
        properties.setProperty(AWSEC2Constants.PROPERTY_EC2_AMI_QUERY, "");
        properties.setProperty(AWSEC2Constants.PROPERTY_EC2_CC_AMI_QUERY, "");
        lazyLock.lock();
        try {
            if(lazyContext == null) {
                lazyContext = executorService.submit(
                        new AmazonContextBuilder(accessKeyId, secretKey,
                        Optional.fromNullable(properties)));
            }
            return valueOrNotReady(lazyContext);
        } finally {
            lazyLock.unlock();
        }
    }
    
    @Override
    public void close() {
        try {
            closeContext(eagerContext, eagerLock);
        } catch(Throwable ex) {
            logger.error("Can not close eager context.", ex);
        }
        try {
            closeContext(lazyContext, lazyLock);
        } catch(Throwable ex) {
            logger.error("Can not close lazy context.", ex);
        }
    }
    
    private void closeContext(ListenableFuture<ComputeServiceContext> context, Lock lock) {
        lock.lock();
        try {
            if(context != null) {
                valueOrNotReady(context).close();
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Retrieves future value or throws IllegalStateException if the future
     * value can not be retrieved anymore.
     *
     * @return
     */
    private <T> T valueOrNotReady(Future<T> future) {
        try {
            return future.get();
        } catch (Exception ex) {
            throw new IllegalStateException("ComputeContext is not ready.", ex);
        }
    }
}
