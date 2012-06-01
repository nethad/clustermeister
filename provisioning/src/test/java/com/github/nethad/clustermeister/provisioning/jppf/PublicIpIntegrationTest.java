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
package com.github.nethad.clustermeister.provisioning.jppf;

import com.github.nethad.clustermeister.provisioning.torque.TorqueConfiguration;
import com.github.nethad.clustermeister.provisioning.torque.TorqueJPPFNodeDeployer;
import com.github.nethad.clustermeister.provisioning.torque.TorqueNodeManager;
import com.github.nethad.clustermeister.provisioning.utils.SSHClient;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;

import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.*;

/**
 *
 * @author thomas
 */
public class PublicIpIntegrationTest {
    protected static TorqueJPPFNodeDeployer nodeDeployer;

    @Before
    public void setup() throws Exception {
        SSHClient sshClientMock = mock(SSHClient.class);
        when(sshClientMock.executeWithResult(contains("$SSH_CLIENT"))).thenReturn("1.2.3.5 1234 4321");
        TorqueConfiguration torqueConfiguration = new TorqueConfiguration("user", "", null, 22, null, null);
        nodeDeployer = new TorqueJPPFNodeDeployer(torqueConfiguration, sshClientMock);
    }

    @Test
    public void getPublicIpFromSSHServerEnvAndNotifyObservers() {
        Observer observerMock = mock(Observer.class);
        nodeDeployer.addListener(observerMock);
        verify(observerMock).update(null, "1.2.3.5");
    }
    
    @Test
    public void testDriverBlockingUntilPublicIpAvailable() throws InterruptedException {
        final Lock lock = new ReentrantLock();
        final Condition condition = lock.newCondition();
        final AtomicBoolean isBlocking = new AtomicBoolean(false);
        final JPPFLocalDriver driver = new JPPFLocalDriver(null);
        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    isBlocking.set(true);
                    condition.signal();
                } finally {
                    lock.unlock();
                }
                driver.getIpAddress();
                
            }
        }).start();
        lock.lock();
        try {
            while(!isBlocking.get()) {
                condition.await();
                Thread.sleep(100);
            }
        } finally {
            lock.unlock();
        }
        nodeDeployer.addListener(driver);
        assertThat(driver.getIpAddress(), is("1.2.3.5"));
    }
}
