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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.TypedProperties;

/**
 *
 * @author thomas
 */
public class GatherNodeInformationTaskInBroadcastJob extends JPPFTask {
    public static final String AVAILABLE_PROCESSORS = "availableProcessors";
    public static final String IPV4_ADDRESSES = "ipv4.addresses";
    private static final String LOCALHOST = "localhost";
    public static final String OS_NAME = "os.name";
    public static final String PROCESSING_THREADS = "processing.threads";
    public static final String TOTAL_MEMORY = "totalMemory";
    public static final String UUID = "jppf.uuid";

    private Map<Object, Object> propertiesMap;

    public GatherNodeInformationTaskInBroadcastJob() {
    }

    @Override
    public void run() {
        JMXNodeConnectionWrapper wrapper = null;
        try {
            wrapper = new JMXNodeConnectionWrapper();
            wrapper.connectAndWait(10000);
            JPPFSystemInformation sysInfo = wrapper.systemInformation();
            propertiesMap = new HashMap<Object, Object>();
            
            if (sysInfo == null) {
                setException(new Exception("Could not get system information."));
                return;
            }
            
            addProperty(UUID, sysInfo.getUuid());
            addProperty(PROCESSING_THREADS, sysInfo.getJppf());
            propertiesMap.put("jppfconfig", sysInfo.getJppf().asString());
            addProperty(IPV4_ADDRESSES, sysInfo.getNetwork());
            addProperty(OS_NAME, sysInfo.getSystem());
            addProperty(TOTAL_MEMORY, sysInfo.getRuntime());
            addProperty(AVAILABLE_PROCESSORS, sysInfo.getRuntime());
            
//            sb.append("[[ env ]] \n\n").append(sysInfo.getEnv().asString()).append("\n");
//            sb.append("[[ storage ]] \n\n").append(sysInfo.getStorage().asString()).append("\n");
            
//            String nodeInfo = sb.toString();
            TypedProperties typedProperties = new TypedProperties(propertiesMap);
            setResult(typedProperties);
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
    
    private void addProperty(String key, TypedProperties fromProperties) {
        propertiesMap.put(key, fromProperties.getProperty(key));
    }
    
    
}
