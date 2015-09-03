package be.iminds.iot.dianne.rl.command;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import be.iminds.iot.dianne.api.rl.Agent;

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

	private Agent agent;
	
	public void act(String nnName, String environment){
		act(nnName, environment, null);
	}
	
	public void act(String nnName, String environment, String experiencePool, String... properties){
		try {
			agent.act(nnName, environment, experiencePool, createConfig(properties));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void stopAct(){
		agent.stop();
	}
	
	private Map<String, String> createConfig(String[] properties){
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
	public void setAgent(Agent agent){
		this.agent = agent;
	}
}
