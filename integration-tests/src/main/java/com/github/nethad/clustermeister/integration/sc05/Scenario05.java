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
package com.github.nethad.clustermeister.integration.sc05;

import com.github.nethad.clustermeister.integration.AbstractScenario;
import java.io.IOException;

/**
 *
 * Scenario only usable if client code is run and the node itself is started externally.
 * 
 * @author thomas
 */
public class Scenario05 extends AbstractScenario {
    
    public static void main(String... args) throws InterruptedException {
        new Scenario05().withDriverOnly().execute();
    }

    @Override
    public void runScenario() throws Exception {
        final int seconds = 60;
        int counter = 0;
        
        while (counter < seconds) {
            System.out.println(String.format("Sleep... (%d)", counter));
            Thread.sleep(2000);
            counter++;
        }
        
//        System.out.print("Wait for user input (press ENTER)...");
//        try {
//            int read = System.in.read();
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
        System.out.println("Exit...");
    }
    
}
