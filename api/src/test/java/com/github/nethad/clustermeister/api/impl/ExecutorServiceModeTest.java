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

import org.jppf.client.concurrent.JPPFExecutorService;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author thomas
 */
public class ExecutorServiceModeTest {
    private JPPFExecutorService executorService;


    @Before
    public void setup() throws Exception {
        executorService = mock(JPPFExecutorService.class);
    }

    @Test
    public void standardMode() {
        ExecutorServiceMode standard = ExecutorServiceMode.standard();
        
        standard.configureJppfExecutorService(executorService);
        
        verify(executorService, never()).setBatchTimeout(anyLong());
        verify(executorService, never()).setBatchSize(anyInt());
    }
    
    @Test
    public void timeConstraint() {
        ExecutorServiceMode timeConstraint = ExecutorServiceMode.timeConstraint(4242L);
        
        timeConstraint.configureJppfExecutorService(executorService);
        
        verify(executorService, only()).setBatchTimeout(eq(4242L));
    }
    
    @Test
    public void batchConstraint() {
        ExecutorServiceMode batchSizeContraint = ExecutorServiceMode.batchSizeContraint(23);
        
        batchSizeContraint.configureJppfExecutorService(executorService);
        
        verify(executorService, only()).setBatchSize(eq(23));
    }
    
    @Test
    public void timeAndBatchConstraint() {
        ExecutorServiceMode timeoutAndBatchSizeContraint = ExecutorServiceMode.timeoutAndBatchSizeContraint(2323L, 42);
        
        timeoutAndBatchSizeContraint.configureJppfExecutorService(executorService);
        
        verify(executorService).setBatchTimeout(eq(2323L));
        verify(executorService).setBatchSize(eq(42));
    }
}
