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
package com.github.nethad.clustermeister.provisioning.torque;

import com.github.nethad.clustermeister.provisioning.utils.SSHClient;

/**
 *
 * @author thomas
 */
public interface TorqueNodeDeployment {

	public static final int DEFAULT_MANAGEMENT_PORT = 11198;
	public static final String DEPLOY_BASE_NAME = "jppf-node";
	public static final String DEPLOY_CONFIG_SUFFIX = ".properties";
	public static final String DEPLOY_QSUB = "qsub-node.sh";
	public static final String PATH_TO_QSUB_SCRIPT = "./" + DEPLOY_BASE_NAME + "/" + DEPLOY_QSUB;

	public String getDriverAddress();

	public String getSessionId();
	
	public SSHClient sshClient();
}
