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
package com.github.nethad.clustermeister.provisioning.torque.commands;

import com.github.nethad.clustermeister.api.NodeInformation;
import com.github.nethad.clustermeister.provisioning.CommandLineArguments;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author thomas
 */
public class RemoveNodeCommand extends AbstractTorqueExecutableCommand {
    
    private static final String COMMAND = "removenode";
    private static final String[] ARGUMENTS = new String[]{"node IDs..."};
    private static final String HELP_TEXT = "Remove node from the cluster";

    public RemoveNodeCommand(TorqueCommandLineEvaluation commandLineEvaluation) {
        super(COMMAND, ARGUMENTS, HELP_TEXT, commandLineEvaluation);
    }

    @Override
    public void execute(CommandLineArguments arguments) {
        Collection<NodeInformation> allNodes = getRmiServerForApi().getAllNodes();
        LinkedList<String> nodeUuids = new LinkedList<String>();
        
        Scanner scanner = arguments.asScanner();
        
        while (scanner.hasNext()) {
            final String currentId = scanner.next();
            boolean validId = Iterables.any(allNodes, new Predicate<NodeInformation>() {
                              @Override
                              public boolean apply(NodeInformation input) {
                                  return input.getID().equalsIgnoreCase(currentId);
                              }
                          });
            if (validId) {
                nodeUuids.add(currentId);
            }
        }
        ListenableFuture<Void> removeNodesFuture = getNodeManager().removeNodes(nodeUuids);
        try {
            removeNodesFuture.get();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } catch (ExecutionException ex) {
            throw new RuntimeException(ex);
        }
        
    }
    
}
