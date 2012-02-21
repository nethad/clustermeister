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
package com.github.nethad.clustermeister.provisioning.ec2;

import com.github.nethad.clustermeister.api.Node;
import com.github.nethad.clustermeister.api.NodeType;
import java.util.Collections;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 *
 * @author daniel
 */
public class AmazonNode implements Node {
	String status;
	String id;
	NodeType type;
	Set<String> privateAddresses;
	Set<String> publicAddresses;
	int port;

	public void setStatus(String status) {
		this.status = status;
	}
	
	@Override
	public String getStatus() {
		return status;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	@Override
	public String getId() {
		return id;
	}

	public void setType(NodeType type) {
		this.type = type;
	}

	@Override
	public NodeType getType() {
		return type;
	}

	public void setPrivateAddresses(Set<String> privateAdresses) {
		this.privateAddresses = Collections.unmodifiableSet(privateAdresses);
	}

	@Override
	public Set<String> getPrivateAddresses() {
		return privateAddresses;
	}
	
	public void setPublicAddresses(Set<String> publicAdresses) {
		this.publicAddresses = Collections.unmodifiableSet(publicAdresses);
	}

	@Override
	public Set<String> getPublicAddresses() {
		return publicAddresses;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public int getPort() {
		return port;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).
				append(type).
				append(id).
				append(status).
				append("public", publicAddresses).
				append("private", privateAddresses).
				toString();
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
		AmazonNode otherNode = (AmazonNode) obj;
		return new EqualsBuilder().append(id, otherNode.id).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(3, 5).append(id).toHashCode();
	}
}
