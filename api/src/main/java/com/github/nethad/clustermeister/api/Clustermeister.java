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

import com.github.nethad.clustermeister.api.impl.ExecutorServiceMode;
import com.github.nethad.clustermeister.api.impl.Job;
import com.github.nethad.clustermeister.api.impl.JobFactory;
import com.github.nethad.clustermeister.api.impl.Task;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.jppf.client.JPPFClient;

/**
 * This is the entry point for the Clustermeister API. 
 * This object is usually instantiated via { @link ClustermeisterFactory#create() }
 * @author thomas
 */
public interface Clustermeister {

    /**
     * An {@link ExecutorService} executes code on any node. This method always returns a new {@link ExecutorService}.
     * @param executorServiceMode the {@link ExecutorServiceMode} for this {@link ExecutorService}, which can influence
     * task scheduling.
     * @return an ExecutorService for all nodes currently provisioned.
     */
    public ExecutorService getExecutorService(ExecutorServiceMode executorServiceMode);

    /**
     * The Clustermeister implementation is based on <a href="http://www.jppf.org/">JPPF</a>.
     * To execute code, the API will connect to a local driver via {@link JPPFClient}. This method provides
     * direct access to that object. However, its use is discouraged and nodes should be accessed via {@link #getAllNodes()}
     * or via {@link #getExecutorService()}.
     * @return an instance of the {@link JPPFClient} used to connect to a (local) JPPF driver.
     */
    public JPPFClient getJppfClient();
    
    /**
     * This method returns all nodes that are currently connected.
     * For information on how to execute code on the nodes, see {@link ExecutorNode}.
     * @return all nodes currently connected.
     */
    public Collection<ExecutorNode> getAllNodes();

    /**
     * Shuts down all currently running executions and closes the connection to the (local) JPPF driver.
     * This method is commonly called in a finally-block after all code has been executed on the nodes.
     */
    public void shutdown();
    
    /**
     * Execute a job (a list of tasks) on the nodes provisioned.
     * This method is blocking, see {@link #executeJobAsync(Job) } for a non-blocking 
     * implementation.
     * @param <T>
     * @param job the job to be executed.
     * @return a list of results from the task beloging to the job.
     * @throws Exception 
     */
    public <T> List<T> executeJob(Job<T> job) throws Exception;
    
    /**
     * Execute a job asynchronously. After all tasks are executed, this Future is complete. 
     * If task-results need to be retrieved independently and not as a whole, 
     * see {@link #executeJobAsyncTasks(Job) }.
     * @param <T>
     * @param job the job to be executed.
     * @return a Future which contains a list of results from task belonging to the job.
     * @throws Exception 
     */
    public <T> ListenableFuture<List<T>> executeJobAsync(Job<T> job) throws Exception;
    
    /**
     * Execute a job asynchronously and retrieve task results independently. Every task belonging to a job
     * has its own Future which completes as soon as the task result is retrieved.
     * @param <T>
     * @param job the job to be executed.
     * @return a list of Futures, where every Future is a task result.
     * @throws Exception 
     */
    public <T> List<ListenableFuture<T>> executeJobAsyncTasks(final Job<T> job) throws Exception;
    
}
