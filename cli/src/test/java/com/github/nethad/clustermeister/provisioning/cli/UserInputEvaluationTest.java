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
package com.github.nethad.clustermeister.provisioning.cli;

import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.mockito.Mockito.*;

/**
 *
 * @author thomas
 */
public class UserInputEvaluationTest {
    private Provisioning provisioning;
    private UserInputEvaluation userInputEvaluation;

    @Before
    public void setup() {
        provisioning = mock(Provisioning.class);
        userInputEvaluation = new UserInputEvaluation(provisioning);
    }
    
    @Test
    public void addnodesCommand() {
        userInputEvaluation.evaluate("addnodes 10 23");
        verify(provisioning).addNodes(10, 23);
    }
    
    @Test
    public void addnodesCommand_fail() {
        userInputEvaluation.evaluate("addnodes 10");
        verify(provisioning, never()).addNodes(anyInt(), anyInt());
        
        userInputEvaluation.evaluate("addnodes");
        verify(provisioning, never()).addNodes(anyInt(), anyInt());
    }

    @Test
    public void stateCommand() {
        when(provisioning.getNumberOfRunningNodes()).thenReturn(0);
        when(provisioning.getProvider()).thenReturn(Provider.TORQUE);
        userInputEvaluation.evaluate("state");
        verify(provisioning).getNumberOfRunningNodes();
        verify(provisioning).getProvider();
    }
    
    @Test
    public void shutdownCommand() {
        userInputEvaluation.evaluate("shutdown");
        verify(provisioning).shutdown();
    }
    
        @Test
    public void unknownCommand() {
        userInputEvaluation.evaluate("");
        verifyZeroInteractions(provisioning);
    }
}
