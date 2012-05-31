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

import java.util.concurrent.Callable;
import org.jppf.client.JPPFJob;

/**
 * A Job is a collection of {@link Task}s.
 * Jobs are usually created with {@link JobFactory#create(java.lang.String, java.util.Map) }.
 * It is generally a good idea to conflate tasks in a job, as it often leads to a significant execution speed-up
 * compared to executing them as single {@link Callable}s.
 * @author thomas
 */
public abstract class Job<T> {
        
    /**
     * Adds a task to the job.
     * @param task the task to add
     * @throws Exception 
     */
    public abstract void addTask(Task<T> task) throws Exception;
    
    /**
     * Exposes the underlying JPPF Job. Any direct modification of the JPPF Job is discouraged.
     * @return the native JPPF Job.
     */
    protected abstract JPPFJob getJppfJob();

    protected abstract void setBlocking(boolean blocking);
    
    protected abstract FutureResultCollector resultCollector();
    
}
