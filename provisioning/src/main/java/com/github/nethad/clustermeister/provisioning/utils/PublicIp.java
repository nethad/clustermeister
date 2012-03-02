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
package com.github.nethad.clustermeister.provisioning.utils;

import com.github.nethad.clustermeister.provisioning.torque.TorqueJPPFNodeDeployer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author thomas
 */
public class PublicIp {
	public static final String WHATISMYIP_WEBSERVICE_URL = "http://automation.whatismyip.com/n09230945.asp";
	private static String publicIp;

	public static String getPublicIp() {
		if (publicIp != null) {
			return publicIp;
		}
		URL whatismyip;
		BufferedReader in = null;
		try {
			whatismyip = new URL(WHATISMYIP_WEBSERVICE_URL);
			in = new BufferedReader(new InputStreamReader(
					whatismyip.openStream()));
			publicIp = in.readLine(); //you get the IP as a String
		} catch (MalformedURLException ex) {
			Logger.getLogger(TorqueJPPFNodeDeployer.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(TorqueJPPFNodeDeployer.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				in.close();
			} catch (IOException ex) {
				Logger.getLogger(TorqueJPPFNodeDeployer.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		return publicIp;
	}
}
