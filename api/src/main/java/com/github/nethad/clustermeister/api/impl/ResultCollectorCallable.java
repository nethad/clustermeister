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

import java.util.List;
import java.util.concurrent.Callable;
import org.jppf.client.JPPFResultCollector;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.TypedProperties;

/**
 *
 * @author thomas
 */
class ResultCollectorCallable<T> implements Callable<T> {
    private JPPFResultCollector collector;

    public ResultCollectorCallable(JPPFResultCollector collector) {
        this.collector = collector;
    }

    @Override
    public T call() throws Exception {
        List<JPPFTask> tasks = collector.waitForResults();
        if (tasks.size() != 1) {
            throw new Exception("There should only be 1 task.");
        }
        JPPFTask task = tasks.get(0);
        if (task.getException() != null) {
            throw new Exception(task.getException());
        }
        return (T) task.getResult();
    }
    
}
