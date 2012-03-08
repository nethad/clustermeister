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

/**
 *
 * @author thomas
 */
public class CommandLineTextBuilder {
    private static final String TAB = "\t";
    private static final String NEWLINE = "\n";
    
    private StringBuilder sb;

    public CommandLineTextBuilder(String titleText) {
        sb = new StringBuilder(titleText);
        sb.append(NEWLINE).append(NEWLINE);
    }
    
    public void addLine(String left, Object right) {
        sb.append(left).append(TAB).append(right.toString()).append(NEWLINE);
    }

    @Override
    public String toString() {
        return sb.toString();
    }
    
    public void print() {
        System.out.println(sb.toString());
    }
    
    
    
}
