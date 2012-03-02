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
import com.github.nethad.clustermeister.provisioning.ec2.AmazonNode;
import com.google.common.base.Objects;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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
	private String torqueJobId;
	private Set<String> publicAddresses = new HashSet<String>();
	private Set<String> privateAddresses = new HashSet<String>();
	private final int serverPort;
	private final int managementPort;

	public TorqueNode(NodeType nodeType, String torqueJobId, String publicAddress, String privateAddress, int serverPort, int managementPort) {
		this.nodeType = nodeType;
		this.torqueJobId = torqueJobId;
		this.publicAddresses.add(publicAddress);
		this.privateAddresses.add(privateAddress);
		this.serverPort = serverPort;
		this.managementPort = managementPort;
	}

	@Override
	public String getID() {
		return getTorqueJobId();
	}

	@Override
	public NodeType getType() {
		return nodeType;
	}

	@Override
	public Set<String> getPublicAddresses() {
		return publicAddresses;
	}

	@Override
	public Set<String> getPrivateAddresses() {
		return privateAddresses;
	}

	@Override
	public int getManagementPort() {
		return managementPort;
	}
	
	public String getTorqueJobId() {
		return torqueJobId;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).
				append(getType()).
				append(getTorqueJobId()).
				append("public", getPublicAddresses()).
				append("private", getPrivateAddresses()).
				append(getServerPort()).
				append(getManagementPort()).
				toString();
//		return "[TorqueNode; type="+getType()+",torqueJobId="+getTorqueJobId()+"]";
	}
	
		@Override
	public boolean equals(Object obj) {
		if(obj == null) {
			return false;
		}
		if(obj == this) {
			return true;
		}
		if(obj.getClass() != (getClass())) {
			return false;
		}
		TorqueNode otherNode = (TorqueNode) obj;
		return new EqualsBuilder().
				append(getID(), otherNode.getID()).
				isEquals();
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getID());
	}

	/**
	 * @return the serverPort
	 */
	public int getServerPort() {
		return serverPort;
	}
	
	
	
}
