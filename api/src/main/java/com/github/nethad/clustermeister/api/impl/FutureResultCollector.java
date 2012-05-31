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

import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.List;
import org.jppf.client.JPPFJob;
import org.jppf.client.JPPFResultCollector;
import org.jppf.client.event.TaskResultEvent;
import org.jppf.server.protocol.JPPFTask;

/**
 *
 * @author thomas
 */
public class FutureResultCollector<T> extends JPPFResultCollector {

    List<SettableFuture<T>> futureResults = new ArrayList<SettableFuture<T>>();

    protected FutureResultCollector(JPPFJob job) {
        super(job);
        for (int i = 0; i < job.getTasks().size(); i++) {
            futureResults.add(SettableFuture.<T>create());
        }
    }

    @Override
    public synchronized void resultsReceived(TaskResultEvent event) {
        super.resultsReceived(event);
        //System.out.println("Received result, task list size = " + event.getTaskList().size());
        for (JPPFTask task : event.getTaskList()) {
            futureResults.get(task.getPosition()).set((T) task.getResult());
        }
    }

    public List<SettableFuture<T>> getFutureList() {
        return futureResults;
    }
}
