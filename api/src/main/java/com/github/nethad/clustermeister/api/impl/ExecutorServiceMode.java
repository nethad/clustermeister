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
package com.github.nethad.clustermeister.api.impl;

import com.google.common.base.Optional;
import java.util.concurrent.ExecutorService;
import org.jppf.client.concurrent.JPPFExecutorService;

/**
 * The ExecutorServiceMode influences the {@link ExecutorService} task scheduling.
 * Two parameters can be set: a timeout and a batch size.
 * <ul>
 * <li>If no parameter is set, every task is sent immediately (standard)</li>
 * <li>If the timeout is set, an ExecutorService waits for the timeout to send the tasks to the server.</li>
 * <li>If the batch size is set to N, an ExecutorService waits for N tasks before it sends them to the server.</li>
 * <li>If the timeout AND batch size is set, an ExecutorService send the tasks to the server if a timeout occurs
 * or the number of tasks is reached, whatever happens first.</li>
 * </ul>
 * 
 * @author thomas
 */
public abstract class ExecutorServiceMode {
    
    abstract protected void configureJppfExecutorService(JPPFExecutorService executorService);
    
    public static ExecutorServiceMode standard() {
        return new GenericExecutorServiceMode(Optional.<Long>absent(), Optional.<Integer>absent());
    }
    
    public static ExecutorServiceMode timeConstraint(long timeout) {
        return new GenericExecutorServiceMode(Optional.fromNullable(timeout), Optional.<Integer>absent());
    }
    
    public static ExecutorServiceMode batchSizeContraint(int batchSize) {
        return new GenericExecutorServiceMode(Optional.<Long>absent(), Optional.fromNullable(batchSize));
    }
    
    public static ExecutorServiceMode timeoutAndBatchSizeContraint(long timeout, int batchSize) {
        return new GenericExecutorServiceMode(Optional.fromNullable(timeout), Optional.fromNullable(batchSize));
    }
    
    static class GenericExecutorServiceMode extends ExecutorServiceMode {
        private final Optional<Long> timeout;
        private final Optional<Integer> batchSize;

        GenericExecutorServiceMode(Optional<Long> timeout, Optional<Integer> batchSize) {
            this.timeout = timeout;
            this.batchSize = batchSize;
        }

        @Override
        protected void configureJppfExecutorService(JPPFExecutorService executorService) {
            if (timeout.isPresent()) {
                executorService.setBatchTimeout(timeout.get());
            }
            if (batchSize.isPresent()) {
                executorService.setBatchSize(batchSize.get());
            }
        }
        
    }

}
