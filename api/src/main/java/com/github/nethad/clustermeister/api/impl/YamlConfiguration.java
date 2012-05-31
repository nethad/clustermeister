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
package com.github.nethad.clustermeister.api.impl;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.commons.configuration.tree.ConfigurationNodeVisitor;
import org.apache.commons.configuration.tree.DefaultExpressionEngine;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author thomas
 */
public class YamlConfiguration extends HierarchicalConfiguration {
//    private Map<String, Object> document;
    
    public YamlConfiguration() {
        this.setExpressionEngine(new DefaultExpressionEngine());
    }

    public YamlConfiguration(String configFilePath) throws ConfigurationException {
        this.setExpressionEngine(new DefaultExpressionEngine());
        try {
            load(new FileReader(configFilePath));
        } catch (FileNotFoundException ex) {
            throw new ConfigurationException(ex);
        }
    }
    
    public final void load(Reader reader) {
        Yaml yaml = new Yaml();
        Map<String, Object> document = (Map<String, Object>)yaml.load(reader);
        this.setRootNode(new YamlConfigurationNode("", document));
    }
    
    public static String defaultConfiguration() {
        StringBuilder sb = new StringBuilder("# clustermeister default configuration\n");
        sb.append("# for more information on how to configure clustermeister, see https://github.com/nethad/clustermeister/wiki/Configuration\n")
            .append("\n")
            .append("# amazon:\n")
            .append("#   access_key_id: 00accesskey00\n")
            .append("#   secret_key: 00secretkey00\n")
            .append("#   keypairs:\n")
            .append("#     - my-keypair:\n")
            .append("#         user: ec2-user\n")
            .append("#         private_key: /home/user/path/to/key_private.pem\n")
            .append("#         public_key: /home/user/path/to/key_public.pub\n")
            .append("#   profiles:\n")
            .append("#     - micro-eu:\n")
            .append("#         type: t1.micro\n")
            .append("#         region: eu-west-1\n")
            .append("#         keypair: my-keypair\n")
            .append("#\n")
            .append("# torque:\n")
            .append("#   ssh_user: user\n")
            .append("#   ssh_privatekey: /home/user/.ssh/id_dsa\n")
            .append("#   ssh_host: host.example.org\n")
            .append("#   ssh_port: 22\n")
            .append("#   email_notify: user@example.org\n")
            .append("#   queue_name: queuename\n")
            .append("#\n")
            .append("# preload:\n")
            .append("#   poms:\n")
            .append("#     - /home/user/path/to/my/pom.xml\n")
            .append("#   excludes:\n")
            .append("#     - \"org.example.groupids.to.exclude:artifactid\"\n")
            .append("#   maven_repositories:\n")
            .append("#     - typesafe:\n")
            .append("#         layout: default\n")
            .append("#         url: http://repo.typesafe.com/typesafe/releases/\n")
            .append("#   artifacts:\n")
            .append("#     - \"com.groupid:artifactid:1.0\"\n")
            .append("#\n")
            .append("# jvm_options:\n")
            .append("#   local_driver: \"-Xmx500m\"\n")
            .append("#   node: \"-Xmx300m\"\n")
            .append("#\n");
        return sb.toString();
    }
    
    public class YamlConfigurationNode implements ConfigurationNode {
        
        private Map<String, Object> root = new HashMap<String, Object>();
        private String name;
        private Object value;
        private YamlConfigurationNode parent;
        private List<ConfigurationNode> children = new ArrayList<ConfigurationNode>();
        
        public YamlConfigurationNode(String name, Object root) {
            if (root instanceof Map) {
                this.root = (Map<String, Object>) root;
            } else {
                this.value = root;
            }
            this.name = name;
        }
        
        @Override
        public Object clone() {
            throw new RuntimeException("Clone not yet implemented.");
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setName(String name) {
            this.name = name;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public void setValue(Object val) {
            this.value = val;
        }

        @Override
        public Object getReference() {
            return root;
        }

        @Override
        public void setReference(Object ref) {
            this.root = (Map<String, Object>) ref;
        }

        @Override
        public ConfigurationNode getParentNode() {
            return parent;
        }

        @Override
        public void setParentNode(ConfigurationNode parent) {
            this.parent = (YamlConfigurationNode) parent;
        }

        @Override
        public void addChild(ConfigurationNode node) {
            this.children.add(node);
        }

        @Override
        public List<ConfigurationNode> getChildren() {
            return children;
        }

        @Override
        public int getChildrenCount() {
            return children.size();
        }

        @Override
        public List<ConfigurationNode> getChildren(String name) {
            List<ConfigurationNode> nodes = new ArrayList<ConfigurationNode>();
            nodes.add(new YamlConfigurationNode(name, root.get(name)));
            return nodes;
        }

        @Override
        public int getChildrenCount(String name) {
            return 1;
        }

        @Override
        public ConfigurationNode getChild(int index) {
            if (value instanceof List) {
                Object lval = ((List)value).get(index);
                return new YamlConfigurationNode("", lval);
            } else {
                throw new RuntimeException("Not yet implemented");
            }
        }

        @Override
        public boolean removeChild(ConfigurationNode child) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean removeChild(String childName) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void removeChildren() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isAttribute() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setAttribute(boolean f) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<ConfigurationNode> getAttributes() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getAttributeCount() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<ConfigurationNode> getAttributes(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getAttributeCount(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ConfigurationNode getAttribute(int index) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean removeAttribute(ConfigurationNode node) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean removeAttribute(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void removeAttributes() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void addAttribute(ConfigurationNode attr) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isDefined() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void visit(ConfigurationNodeVisitor visitor) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("[YamlConfigurationNode: ");
            sb.append("name: ").append(this.name).append("; ")
            .append("children: ").append(this.children).append("; ")
            .append("parent: ").append(this.parent).append("; ")
            .append("root: ").append(this.root).append("; ")
            .append("value: ").append(this.value).append("; ");
            return sb.toString();
        }
        
        
        
    }
    
}
