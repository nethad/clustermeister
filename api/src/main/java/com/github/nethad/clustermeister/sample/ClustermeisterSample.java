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
package com.github.nethad.clustermeister.sample;

import com.github.nethad.clustermeister.api.Clustermeister;
import com.github.nethad.clustermeister.api.ExecutorNode;
import com.github.nethad.clustermeister.api.impl.ClustermeisterFactory;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.server.protocol.JPPFTask;

/**
 *
 * @author thomas
 */
public class ClustermeisterSample implements Serializable {

    public static void main(String... args) {
        new ClustermeisterSample().execute();

    }

    private void execute() {
//        executorServiceApi();
//        jppfApi();
//        custom();
        gatherNodeInfo();
    }

    private void executorServiceApi() {
        Clustermeister clustermeister = ClustermeisterFactory.create();
        ExecutorService executorService = clustermeister.getExecutorService();
        List<Future<?>> results = new ArrayList<Future<?>>();
        for (int i = 0; i < 100; i++) {
            results.add(executorService.submit(new MyTask()));
        }
        try {
            for (Future<?> result : results) {
                Object resultObject = result.get(10, TimeUnit.SECONDS);
                System.out.println("resultObject = " + resultObject);
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } catch (ExecutionException ex) {
            throw new RuntimeException(ex);
        } catch (TimeoutException ex) {
            throw new RuntimeException(ex);
        }
        clustermeister.shutdown();
    }

    private void jppfApi() throws RuntimeException {
        Clustermeister clustermeister = ClustermeisterFactory.create();
        JPPFClient jppfClient = clustermeister.getJppfClient();
        JPPFJob job = new JPPFJob();
        job.setBlocking(true);
        try {
            job.addTask(new MyTask());
            List<JPPFTask> tasks = jppfClient.submit(job);
            for (JPPFTask task : tasks) {
                final Exception exception = task.getException();
                if (exception != null) {
                    System.out.println("Exception: " + exception);
                } else {
                    Object result = task.getResult();
                    System.out.println("result = " + result);
                }
            }
        } catch (JPPFException ex) {
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        System.exit(0);
    }

    private void gatherNodeInfo() {
        Clustermeister clustermeister = null;
        try {
            clustermeister = ClustermeisterFactory.create();
            Collection<ExecutorNode> nodes = clustermeister.getAllNodes();
            
            List<ListenableFuture<String>> list = new LinkedList<ListenableFuture<String>>();
            for (ExecutorNode executorNode : nodes) {
                System.out.println("executorNode " + executorNode.getID());
                System.out.println("executorNode processors = " + executorNode.getCapabilities().getNumberOfProcessors());
                System.out.println("executorNode processing threads = " + executorNode.getCapabilities().getNumberOfProcessingThreads());
//                System.out.println("executorNode jppf config\n" + executorNode.getCapabilities().getJppfConfig());
                ListenableFuture<String> future = executorNode.execute(new SampleCallable());
                list.add(future);
            }
            for (ListenableFuture<String> future : list) {
                try {
                    String get = future.get();
                    System.out.println("Yay! result = "+get);
                } catch (InterruptedException ex) {
                    System.out.println(ex.getMessage());
                } catch (ExecutionException ex) {
                    System.out.println(ex.getMessage());
                }
            }
            
        } finally {
            if (clustermeister != null) {
                clustermeister.shutdown();
            }
        }
    }
    
    private void readmeExample() {

    }

    private void custom() {
        // gather all node information
        // 1) ask drivers for NodeInformation list
        // 2) create job and for each node a task that gathers UUID and processors
        // 3) submit job and wait for results
        // 4) process result list and create local data structure for each node


        JPPFClient jppfClient = new JPPFClient("akkaClientUUID");
        try {
            JPPFJob job = new JPPFJob();
            job.setName("Template Job Id");
            try {
                job.addTask(new TemplateJPPFTask());
            } catch (JPPFException ex) {
                throw new RuntimeException(ex);
            }
            job.setBlocking(true);
            job.getSLA().setBroadcastJob(true);
            try {
                List<JPPFTask> results = jppfClient.submit(job);
                for (JPPFTask task : results) {
                    // if the task execution resulted in an exception
                    if (task.getException() != null) {
                        System.out.println("An exception was raised: " + task.getException().getMessage());
                    } else {
                        System.out.println("Execution result: " + task.getResult());
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } finally {
            if (jppfClient != null) {
                jppfClient.close();
            }
        }
    }
    
    public class SampleCallable implements Callable<String>, Serializable {

        @Override
        public String call() throws Exception {
            return "Hello world!";
        }
        
    }
    
}
