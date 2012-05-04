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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import java.io.StringReader;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import static org.hamcrest.Matchers.*;

/**
 *
 * @author thomas
 */
public class YamlConfigurationTest {
    private YamlConfiguration yamlConfiguration;
    
    private static final String DOCUMENT = 
              "amazon:\n"
            + "  imageid: micro\n"
            + "  location: euwest1\n"
            + "  profiles:\n"
            + "    - name: profile1\n"
            + "      key1: value1\n"
            + "      key2: value2\n"
            + "    - name: profile2\n"
            + "      key1: value3\n"
            + "      key2: value4\n"
            + "      otherkey: somevalue\n"
            + "torque: torquevalue\n"
            + "preload:\n"
            + "  artifacts:\n"
            + "    - artifact1\n"
            + "    - artifact2\n";
    
    @Before
    public void setup() {
        yamlConfiguration = new YamlConfiguration();
        yamlConfiguration.load(new StringReader(DOCUMENT));
    }
    
    @Test
    public void nested() {
        String value = yamlConfiguration.getString("amazon.imageid");
        assertThat(value, is("micro"));
        value = yamlConfiguration.getString("amazon.location");
        assertThat(value, is("euwest1"));
    }
    
    @Test
    public void simpleKeyValue() {
        String value = yamlConfiguration.getString("torque");
        assertThat(value, is("torquevalue"));
    }
    
    @Test
    public void nestedMap() {
        List<Object> rawprofiles = yamlConfiguration.getList("amazon.profiles");
        List<LinkedHashMap<String, String>> profiles = new ArrayList<LinkedHashMap<String, String>>();
        for (Object rawprofile : rawprofiles) {
            profiles.add((LinkedHashMap<String, String>)rawprofile);
        }
        assertThat(profiles.size(), is(2));
        
        LinkedHashMap<String, String> firstProfile = profiles.get(0);
        assertThat(firstProfile.size(), is(3));
        assertThat(firstProfile.get("name"), is("profile1"));
        assertThat(firstProfile.get("key1"), is("value1"));
        assertThat(firstProfile.get("key2"), is("value2"));
        
        LinkedHashMap<String, String> secondProfile = profiles.get(1);
        assertThat(secondProfile.size(), is(4));
        assertThat(secondProfile.get("name"), is("profile2"));
        assertThat(secondProfile.get("key1"), is("value3"));
        assertThat(secondProfile.get("key2"), is("value4"));
        assertThat(secondProfile.get("otherkey"), is("somevalue"));
    }
    
    @Test
    public void nestedList() {
        Collection<String> artifacts = Collections2.transform(yamlConfiguration.getList("preload.artifacts"), new Function<Object, String>(){
            @Override
            public String apply(Object input) { return input.toString(); }
        });
        assertThat(artifacts, hasItems("artifact1", "artifact2"));
    }
    
}
