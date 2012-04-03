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

import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author thomas
 */
public class AssertionsTest {

    @Test
    public void assertIntegers() {
        Assertions.assertEquals(Integer.MIN_VALUE, Integer.MIN_VALUE, "");
        Assertions.assertEquals(0, 0, "");
        Assertions.assertEquals(Integer.MAX_VALUE, Integer.MAX_VALUE, "");
    }
    
    @Test
    public void assertIntegers_exception() {
        try {
            Assertions.assertEquals(-1, 0, "");
            fail("No AssertionError thrown.");
        } catch (AssertionError ae) {
            // expected
        }
    }
    
    @Test
    public void assertStrings() {
        Assertions.assertEquals("", "", "");
        Assertions.assertEquals("test", "test", "");
    }
    
    @Test
    public void assertStrings_exception() {
        try {
            Assertions.assertEquals("", "", "");
            fail("No AssertionError thrown.");
        } catch (AssertionError ae) {
            // expected
        }
    }
}
