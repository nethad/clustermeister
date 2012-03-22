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
package com.github.nethad.clustermeister.node;

import org.jppf.node.NodeRunner;
import org.jppf.node.event.NodeLifeCycleEvent;
import org.jppf.node.event.NodeLifeCycleListener;

/**
 * Custom Clustermeister JPPF Node Life Cycle Listener.
 *
 * @author daniel
 */
public class ClustermeisterNodeLifeCycleListener implements NodeLifeCycleListener {

    @Override
    public void nodeStarting(NodeLifeCycleEvent event) {
        //make sure the UUID is printed to standard out in a well defined format.
        System.out.println(ClustermeisterLauncher.UUID_PREFIX + NodeRunner.getUuid());
    }

    @Override
    public void nodeEnding(NodeLifeCycleEvent event) {
        //nop
    }

    @Override
    public void jobStarting(NodeLifeCycleEvent event) {
        //nop
    }

    @Override
    public void jobEnding(NodeLifeCycleEvent event) {
        //nop
    }
}
