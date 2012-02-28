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
package com.github.nethad.clustermeister.provisioning.torque;

import com.github.nethad.clustermeister.api.Node;
import com.github.nethad.clustermeister.api.NodeType;
import java.util.Set;

/**
 * TODO: lightweight class with information about created nodes.
 * - should not perform any web requests/long running operations
 * - a node is a JPPF node or a JPPF driver, not directly related to TOQUE nodes. 
 * - Multiple JPPF nodes may run on the same TORQUE node.
 *
 * @author daniel
 */
public class TorqueNode implements Node {
	private final NodeType nodeType;

	public TorqueNode(NodeType nodeType) {
		this.nodeType = nodeType;
	}

	@Override
	public String getID() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public NodeType getType() {
		return nodeType;
	}

	@Override
	public Set<String> getPublicAddresses() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Set<String> getPrivateAddresses() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
}
