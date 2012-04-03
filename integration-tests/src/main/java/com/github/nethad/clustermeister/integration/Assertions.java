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
package com.github.nethad.clustermeister.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thomas
 */
public class Assertions {
    
    private static final Logger logger = LoggerFactory.getLogger(Assertions.class);
    
    public static void assertEquals(int expected, int actual, String message) {
        assertCondition(expected == actual, message);
    }
    
    public static void assertEquals(String expected, String actual, String message) {
        assertCondition(expected.equals(actual), message);
    }
    
    private static void assertCondition(boolean assertion, String message) {
        if (!assertion) {
            violation(message);
        }
    }
    
    private static void violation(String message) {
        logger.warn("ASSERTION VIOLATED! {}", message);
        throw new AssertionError(message);
    }
    
}
