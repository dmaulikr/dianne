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
package be.iminds.iot.dianne.rl.environment.kuka;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.promise.Promise;

import be.iminds.iot.dianne.api.rl.environment.Environment;
import be.iminds.iot.dianne.api.rl.environment.EnvironmentListener;
import be.iminds.iot.dianne.rl.environment.kuka.api.KukaEnvironment;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorOps;
import be.iminds.iot.robot.api.Arm;
import be.iminds.iot.robot.api.OmniDirectional;
import be.iminds.iot.sensor.api.LaserScanner;
import be.iminds.iot.simulator.api.Orientation;
import be.iminds.iot.simulator.api.Position;
import be.iminds.iot.simulator.api.Simulator;


@Component(immediate = true,
	property = { "name="+FetchCanEnvironment.NAME, 
				 "aiolos.unique=be.iminds.iot.dianne.api.rl.Environment",
				 "aiolos.combine=*",
				 "osgi.command.scope=kukaagent",
				 "osgi.command.function=rest",
				 "osgi.command.function=go",
				 "osgi.command.function=reward"})
public class FetchCanEnvironment implements Environment, KukaEnvironment {
	
	public static final String NAME = "Kuka";
	
	private Set<EnvironmentListener> listeners = Collections.synchronizedSet(new HashSet<>());
	
	private volatile boolean active = false;
	private boolean terminal = false;
	private Tensor observation;
	
	private OmniDirectional kukaPlatform;
	private Arm kukaArm;
	private LaserScanner rangeSensor;
	
	private Simulator simulator;
	
	private float reward = 0;
	private volatile boolean pause = false;
	
	private float speed = 0.1f;
	
	private Random r = new Random(System.currentTimeMillis());
	
	@Override
	public int[] observationDims() {
		return new int[]{512};
	}

	@Override
	public int[] actionDims() {
		return new int[]{7};
	}
	
	@Override
	public float performAction(Tensor action) {
		if(!active)
			throw new RuntimeException("The Environment is not active!");
		
		
		// execute action and calculate reward
		int a = TensorOps.argmax(action);

		try {
			executeAction(a);
		} catch (Exception e) {
			throw new RuntimeException("Failed executing action "+a);
		}
		
		float reward = calculateReward();
		
		updateObservation();

		synchronized(listeners){
			listeners.stream().forEach(l -> l.onAction(reward, observation));
		}
			
		return reward;
	}
	
	@Override
	public Tensor getObservation(Tensor t) {
		if(!active)
			throw new RuntimeException("The Environment is not active!");
		
		if(terminal){
			return null;
		}
		return observation.copyInto(t);
	}

	@Override
	public void reset() {
		if(!active)
			throw new RuntimeException("The Environment is not active!");
		
		terminal = false;
		
		// TODO handle failure here?
		try {
			deinit();

			init();
		} catch(Exception e){
			throw new RuntimeException("Failed to initialize the environment ...", e);
		}
		
		updateObservation();
		
		listeners.stream().forEach(l -> l.onAction(0, observation));
	}
	
	
	private void executeAction(int action) throws Exception {

		switch(action){
		case 0:
			kukaPlatform.move(0f, speed, 0f);
			break;
		case 1:
			kukaPlatform.move(0f, -speed, 0f);
			break;
		case 2:
			kukaPlatform.move(speed, 0f, 0f);
			break;
		case 3:
			kukaPlatform.move(-speed, 0f, 0f);
			break;
		case 4:
			kukaPlatform.move(0f, 0.f, 2*speed);
			break;
		case 5:
			kukaPlatform.move(0f, 0.f, -2*speed);
			break;	
		case 6:
			terminal = true;
			
			kukaPlatform.stop();	

			// stop early in simulator when we are nowhere 
			// near the pick location (further than 10cm)
			if(simulator != null){
				Position d = simulator.getPosition("Can1", "youBot");
				if(Math.abs(d.y) > 0.1 || Math.abs(d.z - 0.58) > 0.1){
					return;
				}
			}
			
			Promise<Arm> result = kukaArm.openGripper()
				.then(p -> kukaArm.setPosition(0, 2.92f))
				.then(p -> kukaArm.setPosition(4, 2.875f))
				.then(p -> kukaArm.setPositions(2.92f, 1.76f, -1.37f, 2.55f))
				.then(p -> kukaArm.closeGripper())
				.then(p -> kukaArm.setPositions(0.01f, 0.8f))
				.then(p -> kukaArm.setPositions(0.01f, 0.8f, -1f, 2.9f))
				.then(p -> kukaArm.openGripper())
				.then(p -> kukaArm.setPosition(1, -1.3f))
				.then(p -> kukaArm.reset());
			
			
			// in simulation keep on ticking to let the action complete
			if(simulator != null){
				for(int i=0;i<300;i++){
					simulator.tick();
					
					// stop when colliding
					if(simulator.checkCollisions("Border")){
						return;
					}
				}
			} else {
				// wait until grip is done
				result.getValue();
				
				// pause the agent
				pause = true;
				System.out.println("Enter your reward for this episode (type \"reward x\" in CLI with x your reward as floating point)");
			}
			
			return;
		}
		
		// simulate an iteration further
		if(simulator != null)
			simulator.tick();	
	
	}
	
	private float previousDistance = 0.0f;
	
	private float calculateReward(){
		
		// calculate reward based on simulator info
		if(simulator != null){
			
			// in case of collision, reward -1
			// in case of succesful grip, reward 1, insuccesful grip, -1
			// else, reward between 0 and 0.5 as one gets closer to the optimal grip point
			if(simulator.checkCollisions("Border")){
				terminal = true;
				return -1.0f;
			}
	
			Position d = simulator.getPosition("Can1", "youBot");
			// if terminal, check for grip success
			if(terminal){
				if(d.x > 0){
					// can is lifted, reward 1
					reward =  1.0f;
				} else {
					// else failed, no reward
					reward = 0.0f;
				}
			} else {
				float dx = d.y;
				float dy = d.z - 0.58f;
		
				// dy should come close to 0.58 for succesful grip
				// dx should come close to 0
				float d2 = dx*dx + dy*dy;
				float distance = (float)Math.sqrt(d2);
				
				// give reward based on whether one gets closer/further from target
				// rescale to get values (approx) between -0.25..0.25 
				reward = (previousDistance-distance)*10;
				
				previousDistance = distance;
			}
		} else {
			// return 0 reward by default ... 
			// unless one specifies his own reward during pause in waitForResume
			reward = 0.0f;
		}
		
		if(pause){
			kukaPlatform.stop();
			waitForResume();
		}
		
		return reward;
	}
	
	public void reward(float r){
		this.reward = r;
		go();
	}
	
	public void rest(){
		pause = true;
	}
	
	public void go(){
		pause = false;
		synchronized(this){
			this.notifyAll();
		}
	}
	
	private void waitForResume(){
		synchronized(this){
			if(pause){
				try {
					this.wait();
				} catch (InterruptedException e) {
					System.out.println("Environment interrupted!");
				}
				pause = false;
			}
		}
	}
	
	private void updateObservation(){
		float[] data = rangeSensor.getValue().data;
		observation = new Tensor(data, data.length);
	}
	
	private void init() throws Exception {
		// TODO also random init for youbot position and orientation?
		
		if(simulator == null) {
			// automatically pause the environment until the user resumes from CLI
			pause = true;
			System.out.println("Reset your environment and resume by typing the \"go\" command.");
			waitForResume();
			
		} else {
			// in simulation we can control the position of the youbot and can
			
			// always start the youbot in 0,0 for now
			Position p = simulator.getPosition("youBot");
			simulator.setPosition("youBot", new Position(0, 0, p.z));
			
			// set random can position, right now in front of the youbot
			p = simulator.getPosition("Can1");
			float x = (r.nextFloat()-0.5f)*0.55f;
			x = x > 0 ? x + 0.25f : x - 0.25f; 
			float y = r.nextFloat()*0.9f+0.4f;
			simulator.setPosition("Can1", new Position(x, y, 0.06f));
			simulator.setOrientation("Can1", new Orientation(0, 0 ,1.6230719f));
			
			simulator.start(true);
			
			// TODO there might be an issue with range sensor not coming online at all
			// should be fixed in robot project..
			long start = System.currentTimeMillis();
			int tries = 0;
			while(rangeSensor==null
					|| kukaArm == null 
					|| kukaPlatform == null){
				try {
					Thread.sleep(100);
					simulator.tick();
				} catch (InterruptedException|TimeoutException e) {
				}
				
				if(System.currentTimeMillis()-start > 20000){
					tries++;
					if(tries >= 3){
						throw new Exception("Failed to initialize Kuka environment");
					}
					
					System.out.println("Failed to initialize youbot/laserscanner in environment... Try again");
	
					// try again?
					simulator.stop();
					Thread.sleep(1000);
					simulator.start(true);
					start = System.currentTimeMillis();
				}
			}
			
			// calculate reward here to initialize previousDistance
			calculateReward();
		}
	}
	
	private void deinit(){
		if(simulator != null){
			simulator.stop();
		}
	}
	

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addEnvironmentListener(EnvironmentListener l, Map<String, Object> properties){
		String target = (String) properties.get("target");
		if(target==null || target.equals(NAME)){
			listeners.add(l);
		}
	}
	
	void removeEnvironmentListener(EnvironmentListener l){
		listeners.remove(l);
	}

	
	// TODO use target filters for these  (involves spawning environments from configadmin?)
	@Reference(cardinality=ReferenceCardinality.OPTIONAL, policy=ReferencePolicy.DYNAMIC)
	void setArm(Arm a){
		this.kukaArm = a;
	}
	
	void unsetArm(Arm a){
		this.kukaArm = null;
	}
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL, policy=ReferencePolicy.DYNAMIC)
	void setPlatform(OmniDirectional o){
		this.kukaPlatform = o;
	}
	
	void unsetPlatform(OmniDirectional o){
		if(this.kukaPlatform==o)
			this.kukaPlatform = null;
	}
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL, policy=ReferencePolicy.DYNAMIC)
	void setLaserScanner(LaserScanner l){
		this.rangeSensor = l;
	}
	
	void unsetLaserScanner(LaserScanner l){
		if(l == this.rangeSensor)
			this.rangeSensor = null;
	}
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL, policy=ReferencePolicy.DYNAMIC)
	void setSimulator(Simulator s){
		this.simulator = s;
	}
	
	void unsetSimulator(Simulator s){
		if(this.simulator == s){
			this.simulator = s;
		}
	}
	
	@Override
	public void setup(Map<String, String> config) {
		if(active)
			throw new RuntimeException("This Environment is already active");

		if(config.containsKey("speed")){
			this.speed = Float.parseFloat(config.get("speed"));
		}
		
		active = true;
		
		// configure the simulated environment
		if(simulator != null)
			simulator.loadScene("youbot_fetch_can.ttt");
		
		reset();
	}
	
	@Override
	public void cleanup() {
		active = false;
		
		deinit();
	}
	
}
