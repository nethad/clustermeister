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

import com.github.nethad.clustermeister.api.Loggers;
import com.google.common.base.Optional;
import java.util.Map;
import org.jppf.JPPFException;
import org.jppf.client.JPPFJob;
import org.jppf.task.storage.DataProvider;
import org.jppf.task.storage.MemoryMapDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class JobImpl<T> extends Job<T> {
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
    protected JPPFJob getJppfJob() {
        return jppfJob;
    }

    @Override
    protected void setBlocking(boolean blocking) {
        jppfJob.setBlocking(blocking);
    }
    
    @Override
    protected FutureResultCollector<T> resultCollector() {
        FutureResultCollector<T> collector = new FutureResultCollector<T>(jppfJob);
        jppfJob.setResultListener(collector);
        return collector;
    }
    
    
}
