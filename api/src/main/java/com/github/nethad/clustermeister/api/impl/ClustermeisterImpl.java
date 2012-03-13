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

import com.github.nethad.clustermeister.api.Clustermeister;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.jppf.client.JPPFClient;
import org.jppf.client.concurrent.JPPFExecutorService;

/**
 *
 * @author thomas
 */
public class ClustermeisterImpl implements Clustermeister {
    private JPPFExecutorService executorService;
    private JPPFClient jppfClient;

    public ClustermeisterImpl() {
        jppfClient = new JPPFClient("clustermeister_"+System.currentTimeMillis());
        executorService = new JPPFExecutorService(jppfClient);
    }
    
    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public JPPFClient getJppfClient() {
        return jppfClient;
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
        List<Runnable> runnables = executorService.shutdownNow();
        if (runnables != null) {
            System.out.println("Runnables, size = "+runnables.size());
        }
        jppfClient.close();
    }
    
}
