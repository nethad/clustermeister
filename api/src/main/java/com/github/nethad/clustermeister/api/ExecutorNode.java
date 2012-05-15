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
package com.github.nethad.clustermeister.api;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Callable;

/**
 * 
 * @author thomas
 */
public interface ExecutorNode extends Node {
    
    /**
     * Returns this node's {@link NodeCapabilities}, 
     * which show attributes like the number of processors and processing threads.
     * @return this node's {@link NodeCapabilities}
     */
    public NodeCapabilities getCapabilities();
    
    /**
     * Executes code (as a {@link Callable}) on this node.
     * @param callable the code to execute
     * @return a {@link ListenableFuture} with the result from the {@link Callable}.
     */
    public <T> ListenableFuture<T> execute(Callable<T> callable);
    
}
