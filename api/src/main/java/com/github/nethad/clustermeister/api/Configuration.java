/*
 * Copyright 2012 University of Zurich.
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
package com.github.nethad.clustermeister.api;

/**
 *
 * @author thomas
 */
public interface Configuration {
    public static final String TORQUE_SSH_USER = "torque.ssh_user";
    public static final String TORQUE_SSH_PRIVATEKEY = "torque.ssh_privatekey";
    public static final String TORQUE_SSH_HOST = "torque.ssh_host";
    public static final String TORQUE_SSH_PORT = "torque.ssh_port";
    public static final String TORQUE_EMAIL_NOTIFY = "torque.email_notify";
    
    public String getString(String key, String defaultValue);
    public int getInt(String key, int defaultValue);
    
}
