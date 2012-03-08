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

import java.util.ArrayList;
import java.util.List;
import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.server.protocol.JPPFTask;

/**
 *
 * @author thomas
 */
public class GatherNodeInformationTask extends JPPFTask {
    private final List<Integer> ports;

    public GatherNodeInformationTask(List<Integer> ports) {
        this.ports = ports;
    }

    @Override
    public void run() {
        List<String> nodeInfos = new ArrayList<String>();
        for (Integer port : ports) {
            JMXNodeConnectionWrapper wrapper = new JMXNodeConnectionWrapper("localhost", port);
            wrapper.connectAndWait(10000);
            if(wrapper.isConnected()) {
                try {
                    String uuid = wrapper.systemInformation().getUuid().getProperty("jppf.uuid");
                    String cores = wrapper.systemInformation().getRuntime().getProperty("processing.threads");
                    nodeInfos.add(uuid +" "+cores);
                } catch (Exception ex) {
                    setException(ex);
                }
            } else {
                throw new IllegalStateException("Wrapper not connected!");
            }
        }
        setResult(nodeInfos);
    }
    
}
