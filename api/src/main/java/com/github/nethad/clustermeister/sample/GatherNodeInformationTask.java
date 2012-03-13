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

import com.github.nethad.clustermeister.api.utils.NodeManagementConnector;
import java.util.concurrent.TimeoutException;
import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.TypedProperties;

/**
 *
 * @author thomas
 */
public class GatherNodeInformationTask extends JPPFTask {

    private final int myPort;

    public GatherNodeInformationTask(int myPort) {
        this.myPort = myPort;
    }

    @Override
    public void run() {
        JMXNodeConnectionWrapper wrapper = null;
        try {
            wrapper = NodeManagementConnector.openNodeConnection("localhost", myPort);
            JPPFSystemInformation sysInfo = wrapper.systemInformation();
            StringBuilder sb = new StringBuilder("node info:\n");
            addLine(sb, "jppf.uuid", sysInfo.getUuid());
            addLine(sb, "processsing.threads", sysInfo.getJppf());
            addLine(sb, "ipv4.addresses", sysInfo.getNetwork());
            addLine(sb, "os.name", sysInfo.getSystem());
            addLine(sb, "totalMemory", sysInfo.getRuntime());
            addLine(sb, "availableProcessors", sysInfo.getRuntime());
            
            sb.append("[[ env ]] \n\n").append(sysInfo.getEnv().asString()).append("\n");
            sb.append("[[ storage ]] \n\n").append(sysInfo.getStorage().asString()).append("\n");
            
            String nodeInfo = sb.toString();
            setResult(nodeInfo);
        } catch (TimeoutException ex) {
            setException(ex);
        } catch (Exception ex) {
            setException(ex);
        } finally {
            if (wrapper != null) {
                try {
                    wrapper.close();
                } catch (Exception ex) {
                    setException(ex);
                }
            }
        }
    }
    
    private void addLine(StringBuilder sb, String key, TypedProperties properties) {
        sb.append(key + " = ").append(properties.getProperties(key)).append("\n");
    }

    @Override
    public String toString() {
        return super.toString() + "; port: "+myPort;
    }
    
    
}
