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

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 *
 * @author thomas
 */
public class NodeDeployTaskTest {
    private NodeDeployTask nodeDeployTask;
    

    @Before
    public void setup() {
        nodeDeployTask = new NodeDeployTask(null, 0, null, null);
    }
    
    @Test
    public void base64Encode() {
        String toEncode = "Hello World";
        String result = nodeDeployTask.base64Encode(toEncode);
        assertEquals("SGVsbG8gV29ybGQ=", result);
    }
    
    public void isValidEmail() {
        assertThat(nodeDeployTask.isValidEmail(""), is(false));
    }
}
