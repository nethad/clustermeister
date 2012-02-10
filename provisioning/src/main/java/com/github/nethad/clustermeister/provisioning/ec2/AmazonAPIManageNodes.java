/*
 * Copyright 2012 University of Zurich.
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

//import com.google.common.collect.ImmutableSet;

//import org.jclouds.aws.ec2.AWSEC2Client;

//import com.google.common.collect.Iterables;
//import com.google.inject.Module;
import com.github.nethad.clustermeister.provisioning.Configuration;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.compute.domain.Template;
import org.jclouds.ec2.compute.options.EC2TemplateOptions;
import org.jclouds.ec2.domain.InstanceType;
import org.jclouds.enterprise.config.EnterpriseConfigurationModule;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.ssh.jsch.config.JschSshClientModule;

//import org.jclouds.logging.log4j.config.Log4JLoggingModule;
//import org.jclouds.ssh.jsch.config.JschSshClientModule;

/**
 *
 * @author thomas
 */
public class AmazonAPIManageNodes {
    public static final String CONFIG_FILE_PATH = ".clustermeister/configuration.properties";
    private String accessKeyId;
    private String secretKey;
    private String securityGroup;
    private String keyPair;
    private String locationId;
    
    private ComputeServiceContext context;
    private Template template;
	
	private Configuration configuration;

	public AmazonAPIManageNodes(Configuration config) {
		this.configuration = config;
	}
    
//    public static void main(String... args) {
//        new AmazonAPIManageNodes().execute();
//    }
    
    public void init() {
        loadConfiguration();
		context = new ComputeServiceContextFactory().createContext("aws-ec2", accessKeyId, secretKey,
                                        ImmutableSet.of(new JschSshClientModule(), new SLF4JLoggingModule(), new EnterpriseConfigurationModule()));
//		context = new ComputeServiceContextFactory().createContext("aws-ec2", accessKeyId, secretKey,
//                                        ImmutableSet.of(new SshjSshClientModule(), new SLF4JLoggingModule(), new EnterpriseConfigurationModule()));
//        context = new ComputeServiceContextFactory().createContext("aws-ec2", accessKeyId, secretKey);
        buildTemplate();
	}
	
	public ComputeServiceContext getContext() {
		return context;
		
	}
	
    public void suspendNode(String nodeId) {
		context.getComputeService().suspendNode(nodeId);
	}
	
    public NodeMetadata resumeNode(String nodeId) {
        
//        listNodes();

//        String nodeId = "eu-west-1/i-14c98e5d";

        
//        System.out.println("create new node");
//        
//        Set<? extends NodeMetadata> newNodes = null;
        
//        try {
             context.getComputeService().resumeNode(nodeId);
            // context.getComputeService().runNodesWithTag("jclouds", 1);
//            newNodes = context.getComputeService().createNodesInGroup("jclouds-test", 1, template);
//        } catch (RunNodesException ex) {
//            System.err.println(ex.getMessage());
//        }

        // when you need access to very ec2-specific features, use the provider-specific context
//        AWSEC2Client ec2Client = AWSEC2Client.class.cast(context.getProviderSpecificContext().getApi());
        
        return context.getComputeService().getNodeMetadata(nodeId);
//
////        NodeMetadata node = Iterables.get(nodes, 0);
//        
//        System.out.println("old node");
//        
//        printMetadata(node);
        
//        System.out.println("new node");
//        
//        printMetadata(newNodes.iterator().next());

//        context.close();
    }

    private void loadConfiguration() {
//        String homeDir = System.getProperty("user.home");
//        String configFilePath = homeDir + File.separator + CONFIG_FILE_PATH;
//        FileConfiguration config = new FileConfiguration(configFilePath);
		
        accessKeyId = configuration.getString("accessKeyId", "");
        secretKey = configuration.getString("secretKey", "");
        securityGroup = configuration.getString("securityGroup", "");
        keyPair = configuration.getString("keyPair", "");
        locationId = configuration.getString("locationId", "");
    }

    private void buildTemplate() {
        template = context.getComputeService()
                            .templateBuilder()
                                .locationId(locationId)
                                .hardwareId(InstanceType.T1_MICRO)
                                .osFamily(OsFamily.AMZN_LINUX).build();
        

        // specify your own groups which already have the correct rules applied
        template.getOptions().as(EC2TemplateOptions.class).securityGroups(securityGroup);

        // specify your own keypair for use in creating nodes
        template.getOptions().as(EC2TemplateOptions.class).keyPair(keyPair);
    }

    private void listNodes() {
        Set<? extends ComputeMetadata> listNodes = context.getComputeService().listNodes();        
        printSet(listNodes);
    }
    
    private void printSet(Set<?> set) {
        for (Object object : set) {
            System.out.println(object);
        }
    }
    
    private void printMetadata(NodeMetadata node) {
                System.out.println("id = "+node.getId());
        
        System.out.println("name = "+node.getName());
        
        System.out.println("private addresses");
        
        for (String address : node.getPrivateAddresses()) {
            System.out.println("address = " + address);
        }
        
        System.out.println("public addresses");
        
        for (String address : node.getPublicAddresses()) {
            System.out.println("address = " + address);
        }
        
        System.out.println("state = "+node.getState().toString());
        
        System.out.println("URI = "+node.getUri());
    }
    
}
