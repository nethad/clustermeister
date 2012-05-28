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

import com.google.common.collect.Iterables;
import java.util.Set;
import org.jclouds.aws.ec2.AWSEC2Client;
import org.jclouds.aws.ec2.domain.PlacementGroup;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.domain.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel
 */
public class AwsEc2Facade {
    
    private final static Logger logger =
            LoggerFactory.getLogger(AmazonNodeManager.class);
    
    private final ContextManager contextManager;

    public AwsEc2Facade(ContextManager contextManager) {
        this.contextManager = contextManager;
    }
    
    /**
     * Performs an Amazon API call to retrieve all AWS EC2 instances.
     *
     * @return A set containing all registered AWS EC2 instances regardless
     * of state.
     */
    public Set<? extends ComputeMetadata> getInstances() {
        return contextManager.getEagerContext().getComputeService().listNodes();
    }
    
    /**
     * Get meta data for a given instance.
     *
     * @param id	The jClouds node ID.
     * @return	jClouds node meta data object.
     */
    public NodeMetadata getInstanceMetadata(String id) {
        return contextManager.getEagerContext().getComputeService().
                getNodeMetadata(id);
    }
    
    /**
     * Performs an Amazon API call to retrieve all AWS EC2 Locations 
     * (Zones and Regions).
     *
     * @return A set containing all registered AWS EC2 locations.
     */
    public Set<? extends Location> getLocations() {
        return contextManager.getEagerContext().getComputeService().
                listAssignableLocations();
    }
    
    /**
     * Create a placement group in a specified region.
     * 
     * @param region the AWS region for the placement group.
     * @param groupName the name of the placement group.
     */
    public void createPlavementGroupInRegion(String region, String groupName) {
        AWSEC2Client ec2Client = getEc2Client(contextManager.getEagerContext());
        ec2Client.getPlacementGroupServices().createPlacementGroupInRegion(
                region, groupName);
    }
    
    /**
     * Get information for a specified placement group.
     * 
     * @param region the AWS region for the placement group.
     * @param groupName the name of the placement group.
     * 
     * @return the placement group.
     */
    protected PlacementGroup getPlacementGroupDescription(String region, String groupName) {
        AWSEC2Client ec2Client = getEc2Client(contextManager.getEagerContext());
        Set<PlacementGroup> groups = ec2Client.getPlacementGroupServices().
                describePlacementGroupsInRegion(region, groupName);
        return Iterables.getOnlyElement(groups, null);
    }

    private AWSEC2Client getEc2Client(ComputeServiceContext context) {
        AWSEC2Client ec2Client = AWSEC2Client.class.cast(
                context.getProviderSpecificContext().getApi());
        return ec2Client;
    }
    
    /**
     * Suspend (stop) an instance.
     *
     * Shuts down the instance but the instance stays available for resuming.
     *
     * @param instanceId	jClouds node ID.
     */
    public void suspendInstance(String instanceId) {
        logger.info("Suspending instance {}.", instanceId);
        contextManager.getEagerContext().getComputeService().suspendNode(instanceId);
        logger.info("Instance {} suspended.", instanceId);
    }

    /**
     * Terminate (destroy) an instance.
     *
     * Shuts down and discards the instance.
     *
     * @param instanceId	jClouds node ID.
     */
    public void terminateInstance(String instanceId) {
        logger.info("Terminating instance {}.", instanceId);
        contextManager.getEagerContext().getComputeService().destroyNode(instanceId);
        logger.info("Instance {} terminated.", instanceId);
    }

    /**
     * Resume (start) an instance.
     *
     * @param instanceId	jClouds node ID.
     */
    public void resumeInstance(String instanceId) {
        logger.info("Resuming instance {}.", instanceId);
        contextManager.getEagerContext().getComputeService().resumeNode(instanceId);
        logger.info("Instance {} resumed.", instanceId);
    }
}
