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

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ComparisonChain;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationScope;

/**
 *
 * @author daniel
 */
public class AWSInstanceProfile implements Comparable<AWSInstanceProfile> {
    private final String profileName;
    private final String location;
    private final Optional<String> amiId;

    public static AWSInstanceProfile fromInstanceMetadata(NodeMetadata instanceMetadata) {
        //TODO: region/zone support
        String region = instanceMetadata.getLocation().getId();
        if(instanceMetadata.getLocation().getScope() == LocationScope.ZONE) {
            region = instanceMetadata.getLocation().getParent().getId();
        }
        return new AWSInstanceProfile("<generated for >" + instanceMetadata.getId(), 
                region, instanceMetadata.getImageId());
    }
    
    public AWSInstanceProfile(String profileName, String location, String amiId) {
        profileName = getCheckedString(profileName, "Invalid profile name.");
        this.profileName = profileName;
        location = getCheckedString(location, "Invalid %s for profile '%s'.", 
                AmazonConfigurationLoader.LOCATION, profileName);
        this.location = location;
        amiId = getCheckedString(amiId, "Invalid %s for profile '%s'.", 
                AmazonConfigurationLoader.AMI_ID, profileName);
        this.amiId = Optional.of(amiId);
    }
    
    public String getProfileName() {
        return profileName;
    }
    
    public Optional<String> getAmiId() {
        return amiId;
    }

    public String getLocation() {
        return location;
    }
    
    private String getCheckedString(String string, String message, Object... messageArgs) {
        checkArgument(string != null, message, messageArgs);
        string = string.trim();
        checkArgument(!string.isEmpty(), message, messageArgs);
        
        return string;
    }
    
    @Override
    public String toString() {
        ToStringHelper helper = Objects.toStringHelper(profileName).
                                     add("location", location);
        if(amiId.isPresent()) {
            helper.add("AMI ID", amiId.get());
        }
                
        return helper.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != (getClass())) {
            return false;
        }
        AWSInstanceProfile otherProfile = (AWSInstanceProfile) obj;
        return new EqualsBuilder().append(profileName, otherProfile.profileName).
                isEquals();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(profileName);
    }

    @Override
    public int compareTo(AWSInstanceProfile that) {
        return ComparisonChain.start().
                compare(this.profileName, that.profileName).
                compare(this.location, that.location).
                compare(this.amiId.orNull(), that.amiId.orNull()).
                result();
    }
}
