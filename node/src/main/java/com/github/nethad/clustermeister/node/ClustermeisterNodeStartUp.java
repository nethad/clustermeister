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
package com.github.nethad.clustermeister.node;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import org.jppf.startup.JPPFDriverStartupSPI;

/**
 * Custom Clustermeister JPPF-Node startup class.
 *
 * @author daniel
 */
public class ClustermeisterNodeStartUp implements JPPFDriverStartupSPI {

    @Override
    public void run() {
        try {
            System.out.println("OUTPUT SET");
            System.out.flush();
            System.setOut(new PrintStream(new FileOutputStream("lala.out")));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }
}
