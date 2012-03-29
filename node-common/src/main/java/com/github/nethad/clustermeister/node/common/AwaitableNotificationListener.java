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
package com.github.nethad.clustermeister.node.common;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.management.NotificationListener;

/**
 * A {@link NotificationListener} that allows waiting for a specified condition.
 *
 * @author daniel
 */
public abstract class AwaitableNotificationListener implements NotificationListener {
    /**
     * The lock that access to a condition is synchronized on.
     */
    protected final Lock lock = new ReentrantLock();
    
    /**
     * The awaitable condition associated to the lock.
     */
    protected final Condition conditionTrue = lock.newCondition();
    
    /**
     * The condition to wait to become true.
     */
    protected boolean condition = false;
    
    /**
     * Checks if the condition is satisfied and the waiting thread can return.
     * 
     * @return whether the 
     */
    public boolean isConditionSatisfied() {
        lock.lock();
        try {
            return condition;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Sets the condition to true and wakes up waiting threads.
     */
    public void setConditionSatisfied() {
        lock.lock();
        try {
            condition = true;
            conditionTrue.signalAll();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Wait for the job to be canceled.
     *
     * May also return when the waiting thread is interrupted.
     * 
     * @throws InterruptedException when the waiting thread is interrupted.
     */
    public void await() throws InterruptedException {
        lock.lock();
        try {
            while (!condition) {
                conditionTrue.await();
            }
        } finally {
            lock.unlock();
        }
    }
}
