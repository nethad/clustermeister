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

import com.github.nethad.clustermeister.api.Job;
import com.github.nethad.clustermeister.api.Loggers;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import org.jppf.JPPFException;
import org.jppf.client.JPPFJob;
import org.jppf.client.JPPFResultCollector;
import org.jppf.client.event.TaskResultEvent;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.DataProvider;
import org.jppf.task.storage.MemoryMapDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class JobImpl<T> implements Job<T> {
    public static final String DEFAULT_JOB_NAME = "Clustermeister Job";
    private static final Logger logger = LoggerFactory.getLogger(Loggers.API);
    
    private JPPFJob jppfJob;

    JobImpl(String name, Optional<Map<String, Object>> jobData) {
        if (jobData.isPresent()) {
            DataProvider dataProvider = dataProviderWith(jobData.get());
            jppfJob = new JPPFJob(dataProvider);
        } else {
            jppfJob = new JPPFJob();
        }
        jppfJob.setName(name);
    }
    
    private DataProvider dataProviderWith(Map<String, Object> jobData) {
        DataProvider dataProvider = new MemoryMapDataProvider();
        for (Map.Entry<String, Object> e : jobData.entrySet()) {
            try {
                dataProvider.setValue(e.getKey(), e.getValue());
            } catch (Exception ex) {
                logger.warn("Could not add job data '{}'.", ex);
            }
        }
        return dataProvider;
    }
    
    @Override
    public void addTask(final Task<T> task) throws Exception {
        try {
            jppfJob.addTask(task.getJppfTask());
        } catch (JPPFException ex) {
            throw new Exception(ex);
        }
    }

    @Override
    public JPPFJob getJppfJob() {
        return jppfJob;
    }

    @Override
    public void setBlocking(boolean blocking) {
        jppfJob.setBlocking(blocking);
    }
    
    @Override
    public FutureResultCollector resultCollector() {
        FutureResultCollector collector = new FutureResultCollector(jppfJob);
        jppfJob.setResultListener(collector);
        return collector;
    }

    public class FutureResultCollector extends JPPFResultCollector {
        
        List<SettableFuture<T>> futureResults = new ArrayList<SettableFuture<T>>();

        public FutureResultCollector(JPPFJob job) {
            super(job);
            for(int i=0; i<job.getTasks().size(); i++) {
                futureResults.add(SettableFuture.<T>create());
            }
        }
        
        @Override
        public synchronized void resultsReceived(TaskResultEvent event) {
            super.resultsReceived(event);
            //System.out.println("Received result, task list size = " + event.getTaskList().size());
            for (JPPFTask task : event.getTaskList()) {
                futureResults.get(task.getPosition()).set((T)task.getResult());
            }
        }

        public List<SettableFuture<T>> getFutureList() {
            return futureResults;
        }
    }
    
    
}
