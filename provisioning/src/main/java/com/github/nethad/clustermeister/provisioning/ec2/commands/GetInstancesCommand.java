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
package com.github.nethad.clustermeister.provisioning.ec2.commands;

import com.github.nethad.clustermeister.provisioning.CommandLineHandle;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonCommandLineEvaluation;
import com.github.nethad.clustermeister.provisioning.ec2.AmazonInstanceManager;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.OperatingSystem;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationScope;

/**
 *
 * @author daniel
 */
public class GetInstancesCommand extends AbstractExecutableCommand {

    public static final String[] ARG_DESCRIPTIONS = new String[]{"-v   print instance details (verbose)"};
    
    /**
     * Command name.
     */
    public static final String NAME = "getinstances";
    
    public static final String VERBOSE = "-v";
    
    private static final String SEPARATOR_LINE = "-------------------------------------------------";
    
    public GetInstancesCommand(String[] arguments, String helpText, 
            AmazonCommandLineEvaluation commandLineEvaluation) {
        super(NAME, arguments, helpText, commandLineEvaluation);
    }
    
    @Override
    public void execute(StringTokenizer tokenizer) {
        AmazonInstanceManager instanceManager = 
                commandLineEvaluation.getNodeManager().getInstanceManager();
        CommandLineHandle handle = commandLineEvaluation.getHandle();
        
        boolean verbose = false;
        if (tokenizer.countTokens() >= ARG_DESCRIPTIONS.length) {
            if(tokenizer.nextToken().equals(VERBOSE)) {
                verbose = true;
            } else {
                handle.expectedArguments(ARG_DESCRIPTIONS);
                return;
            }
        }
        
        StringBuilder output = new StringBuilder("AWS EC2 Instances for this account:\n");
        output.append(SEPARATOR_LINE).append("\n");
        Set<? extends ComputeMetadata> instances = instanceManager.getInstances();
        if(instances.isEmpty()) {
            output.append("No instances found.\n");
        } else {
            Regions regions = new Regions();
            buildLocationTree(instances, regions);
            createLocationTreeOutput(regions, output, verbose);
        }
        output.append(SEPARATOR_LINE);
        handle.print(output.toString());
    }

    private void createLocationTreeOutput(Regions regions, StringBuilder output, boolean verbose) {
        for (Map.Entry<String, Zones> regionEntry : regions.entrySet()) {
            String regionId = regionEntry.getKey();
            Zones zones = regionEntry.getValue();

            output.append("Region=").append(regionId).append(" {\n");
            
            for (Map.Entry<String, SortedSet<ComputeMetadata>> zoneEntry : zones.entrySet()) {
                String zoneId = zoneEntry.getKey();
                SortedSet<ComputeMetadata> zoneInstances = zoneEntry.getValue();
                
                output.append("\tZone=").append(zoneId).append(" {\n");
                createInstanceOutput(zoneInstances, output, verbose);
                output.append("\t}\n");
            }
            
            output.append("}\n");
        }
    }

    private void createInstanceOutput(SortedSet<ComputeMetadata> zoneInstances, 
            StringBuilder output, boolean verbose) {
        
        for (ComputeMetadata computeMetadata : zoneInstances) {
            if(verbose) {
                output.append("\t\t").append(computeMetadata.getId()).
                        append(" {\n").
                        append("\t\t\tName=").
                        append(computeMetadata.getName());

                if (computeMetadata instanceof NodeMetadata) {
                    NodeMetadata nodeMetadata = (NodeMetadata) computeMetadata;
                    createNodeMetadataOutput(output, nodeMetadata);
                }

                createMapOutput(output, computeMetadata.getUserMetadata(), 
                        "UserMetaData", "\t\t\t");

                output.append("\n\t\t}\n");
            } else {
                output.append("\t\t").append(computeMetadata.getId()).append("\n");
            }
        }
    }
    
    private void createMapOutput(StringBuilder output, 
            Map<? extends Object, ? extends Object> map, 
            String mapName, String indent) {
        if(map.isEmpty()) {
            return;
        }
        output.append("\n").append(indent).append(mapName).append("={\n");
        boolean first = true;
        for (Map.Entry<? extends Object, ? extends Object> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if(first) {
                first = false;
            } else {
                output.append(",\n");
            }
            output.append(indent).append("\t").append(key.toString()).
                    append("=").append(value.toString());
        }
        output.append("\n").append(indent).append("}");
    }

    private void createNodeMetadataOutput(StringBuilder output, NodeMetadata nodeMetadata) {
        output.append("\n\t\t\tState=").
                append(nodeMetadata.getState());
        output.append("\n\t\t\tAMI=").
                append(nodeMetadata.getImageId()).
                append("\n\t\t\tPublicAddresses=").
                append(nodeMetadata.getPublicAddresses()).
                append("\n\t\t\tPrivateAddresses=").
                append(nodeMetadata.getPrivateAddresses());
        createOperatingSystemOutput(output, nodeMetadata.getOperatingSystem(), 
                "OS", "\t\t\t");
        createHardwareOutput(output, nodeMetadata.getHardware(), "Hardware", 
                "\t\t\t");
    }
    
    private void createOperatingSystemOutput(StringBuilder output, 
            OperatingSystem os, String name, String indent) {
        
        output.append("\n").append(indent).append(name).append("={\n");
        if(os.getName() != null && !os.getName().isEmpty()) {
            
            output.append(indent).append("\t").append("Name=").
                    append(os.getName()).append(",\n");
        }
        if(os.getFamily() != null && os.getFamily() != OsFamily.UNRECOGNIZED) {
            output.append(indent).append("\t").append("Family=").
                    append(os.getFamily()).append(",\n");
        }
        output.append(indent).append("\t").append("Description=").
                append(os.getDescription()).append(",\n");
        if(os.getVersion() != null && !os.getVersion().isEmpty()) {
            output.append(indent).append("\t").append("Version=").
                    append(os.getVersion()).append(",\n");
        }
        output.append(indent).append("\t").append("Arch=").
                append(os.is64Bit() ? "64bit" : "32bit").append("\n");
        output.append(indent).append("}");
    }
    
    private void createHardwareOutput(StringBuilder output, 
            Hardware hw, String name, String indent) {
        
        output.append("\n").append(indent).append(name).append("={\n");
        output.append(indent).append("\t").append("ID=").
                append(hw.getId()).append(",\n");
        output.append(indent).append("\t").append("Processors=").
                append(hw.getProcessors()).append(",\n");
        output.append(indent).append("\t").append("RAM=").
                append(hw.getRam()).append("\n");
        output.append(indent).append("}");
    }

    private void buildLocationTree(Set<? extends ComputeMetadata> instances, 
            Regions regions) {
        for (ComputeMetadata computeMetadata : instances) {
            Location loc = computeMetadata.getLocation();
            if (loc.getScope() == LocationScope.ZONE) {
                if (loc.getParent().getScope() == LocationScope.REGION) {
                    Zones zones = processRegion(regions, loc.getParent());
                    SortedSet<ComputeMetadata> zoneInstances;
                    if (!zones.containsKey(loc.getId())) {
                        zoneInstances = new TreeSet<ComputeMetadata>(
                                new ComputeMetadataIdComparator());
                        zones.put(loc.getId(), zoneInstances);
                    } else {
                        zoneInstances = zones.get(loc.getId());
                    }
                    zoneInstances.add(computeMetadata);
                }
            }
        }
    }

    private Zones processRegion(Regions regions, Location loc) {
        if(!regions.containsKey(loc.getId())) {
            Zones zones = new Zones();
            regions.put(loc.getId(), zones);
            return zones;
        } else {
            return regions.get(loc.getId());
        }
    }
    
    private class Zones extends TreeMap<String, SortedSet<ComputeMetadata>> {}

    private class Regions extends TreeMap<String, Zones> {}
    
    
    private class ComputeMetadataIdComparator implements Comparator<ComputeMetadata> {

        @Override
        public int compare(ComputeMetadata o1, ComputeMetadata o2) {
            if(o1 == null) {
                return -1;
            }
            if(o2 == null) {
                return 1;
            }
            
            String o1Id = o1.getId();
            if(o1Id == null) {
                return -1;
            }
            
            String o2Id = o2.getId();
            if(o2Id == null) {
                return 1;
            }
            
            return o1Id.compareTo(o2Id);
        }
    }
}
