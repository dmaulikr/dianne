/*******************************************************************************
 * DIANNE  - Framework for distributed artificial neural networks
 * Copyright (C) 2015  iMinds - IBCN - UGent
 *
 * This file is part of DIANNE.
 *
 * DIANNE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Tim Verbelen, Steven Bohez
 *******************************************************************************/
package be.iminds.iot.dianne.rl.command;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import be.iminds.iot.dianne.api.nn.module.dto.NeuralNetworkInstanceDTO;
import be.iminds.iot.dianne.api.nn.platform.DiannePlatform;
import be.iminds.iot.dianne.api.rl.agent.Agent;

/**
 * Separate component for rl commands ... should be moved to the command bundle later on
 */
@Component(
		service=Object.class,
		property={"osgi.command.scope=dianne",
				  "osgi.command.function=act",
				  "osgi.command.function=stopAct"},
		immediate=true)
public class DianneRLAgentCommands {

	private DiannePlatform platform;
	
	private Agent agent;
	
	public void act(String nnName, String environment){
		act(nnName, environment, null);
	}
	
	public void act(String nnName, String environment, String experiencePool, String... properties){
		try {
			NeuralNetworkInstanceDTO nni = platform.deployNeuralNetwork(nnName);
			
			agent.act(experiencePool, createAgentConfig(properties), nni, environment);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void stopAct(){
		agent.stop();
	}
	
	static Map<String, String> createAgentConfig(String[] properties){
		Map<String, String> config = new HashMap<String, String>();
		for(String property : properties){
			String[] p = property.split("=");
			if(p.length==2){
				config.put(p[0].trim(), p[1].trim());
			}
		}
		
		return config;
	}
	
	@Reference
	void setAgent(Agent agent){
		this.agent = agent;
	}
	
	@Reference
	void setDiannePlatform(DiannePlatform p){
		this.platform = p;
	}
}
