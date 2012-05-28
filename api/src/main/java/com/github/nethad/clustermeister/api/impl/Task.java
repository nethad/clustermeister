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

import java.io.Serializable;
import org.jppf.server.protocol.JPPFTask;

/**
 *
 * @author thomas
 */
public abstract class Task<T> implements Serializable{
    private JPPFTask jppfTask;

    public Task() {
        jppfTask = new JPPFTask() {
            @Override
            public void run() {
                try {
                    T result = execute();
                    setResult(result);
                } catch (Exception ex) {
                    setException(ex);
                }
            }
        };
    }
    
    JPPFTask getJppfTask() {
        return jppfTask;
    }
    
    public Object getValue(String key) throws Exception {
        if (jppfTask == null) {
            return "key not found";
        }
        return jppfTask.getDataProvider().getValue(key);
    }
    
    public abstract T execute() throws Exception;
    
}
 