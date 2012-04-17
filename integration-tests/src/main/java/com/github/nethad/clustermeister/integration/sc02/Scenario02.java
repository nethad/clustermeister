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
package com.github.nethad.clustermeister.integration.sc02;

import akka.actor.ActorSystem;
import com.github.nethad.clustermeister.integration.AbstractScenario;
import com.github.nethad.clustermeister.integration.sc03.AkkaConfig;

/**
 *
 * @author thomas
 */
public class Scenario02 extends AbstractScenario {
    
    public static void main(String... args) throws InterruptedException {
        new Scenario02().execute();
    }

    @Override
    public void runScenario() throws Exception {
        System.out.println("Scenario 06 started...");
        System.out.println("Starting actor system ...");
        ActorSystem system = ActorSystem.create("RegistrarSystem",
                AkkaConfig.get());
        System.out.println("Actor system started.");
    }
    
}
