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

import com.github.nethad.clustermeister.api.impl.ResultCollectorCallable;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.jppf.utils.TypedProperties;

/**
 *
 * @author thomas
 */
public class ThreadsExecutorService {
    private final ListeningExecutorService threadPoolExecutorService;

    public ThreadsExecutorService() {
        threadPoolExecutorService = MoreExecutors.listeningDecorator(
                Executors.newCachedThreadPool());
    }
    
    public <T> ListenableFuture<T> submit(Callable<T> callable) {
        return threadPoolExecutorService.submit(callable);
    }
    
    public void shutdown() {
        threadPoolExecutorService.shutdown();
    }
    
}
