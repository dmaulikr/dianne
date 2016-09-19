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

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import be.iminds.iot.dianne.api.rl.environment.Environment;
import be.iminds.iot.dianne.api.rl.environment.EnvironmentListener;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorOps;
import be.iminds.iot.robot.api.Arm;
import be.iminds.iot.robot.api.OmniDirectional;
import be.iminds.iot.sensor.api.LaserScanner;
import be.iminds.iot.simulator.api.Position;
import be.iminds.iot.simulator.api.Simulator;


@Component(immediate = true,
	property = { "name="+FetchCanEnvironment.NAME, "aiolos.unique=be.iminds.iot.dianne.api.rl.Environment" })
public class FetchCanEnvironment implements Environment {
	
	public static final String NAME = "Kuka";
	
	private Set<EnvironmentListener> listeners = Collections.synchronizedSet(new HashSet<>());
	
	private volatile boolean active = false;
	private boolean terminal = false;
	private Tensor observation;
	
	private OmniDirectional kukaPlatform;
	private Arm kukaArm;
	private LaserScanner rangeSensor;
	
	private Simulator simulator;
	
	private Random r = new Random(System.currentTimeMillis());
	
	@Override
	public int[] observationDims() {
		return new int[]{511};
	}

	@Override
	public int[] actionDims() {
		return new int[]{8};
	}
	
	@Override
	public float performAction(Tensor action) {
		if(!active)
			throw new RuntimeException("The Environment is not active!");
		
		
		// execute action and calculate reward
		int a = TensorOps.argmax(action);

		if(a==7){
			terminal=true;
		}
		
		executeAction(a);
		
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
		
		deinit();

		init();
		
		updateObservation();
		
		listeners.stream().forEach(l -> l.onAction(0, observation));
	}
	
	
	private void executeAction(int action){
		// TODO send command to robot
		System.out.println("DO ACTION "+action+" "+kukaArm+" "+kukaPlatform);
		switch(action){
		case 0:
			kukaPlatform.move(0f, 0.01f, 0f);
			break;
		case 1:
			kukaPlatform.move(0f, -0.01f, 0f);
			break;
		case 2:
			kukaPlatform.move(0.01f, 0f, 0f);
			break;
		case 3:
			kukaPlatform.move(-0.01f, 0f, 0f);
			break;
		case 4:
			kukaPlatform.move(0f, 0.f, 0f);
			break;
		case 5:
			kukaPlatform.move(0f, 0.f, 0.02f);
			break;
		case 6:
			kukaPlatform.move(0f, 0.f, -0.02f);
			break;	
		case 7:
			System.out.println("GRIP!");
			kukaPlatform.stop();	
			kukaArm.openGripper()
				.then(p -> kukaArm.setPosition(0, 2.92f))
				.then(p -> kukaArm.setPosition(4, 2.875f))
				.then(p -> kukaArm.setPositions(2.92f, 1.76f, -1.37f, 2.55f))
				.then(p -> kukaArm.closeGripper())
				.then(p -> kukaArm.setPositions(0.01f, 0.8f))
				.then(p -> kukaArm.setPositions(0.01f, 0.8f, -1f, 2.9f))
				.then(p -> kukaArm.openGripper())
				.then(p -> kukaArm.setPosition(1, -1.3f))
				.then(p -> kukaArm.reset());
			
			// keep on ticking to let the action complete
			for(int i=0;i<300;i++){
				simulator.tick();
			}
			
			break;
		}
		
		// simulate an iterations further
		simulator.tick();	
	
	}
	
	private float calculateReward(){
		// TODO calculate reward based on distance / succesful grip action
		float reward = 0.0f;
		
		Position p1 = simulator.getPosition("youBot");
		Position p2 = simulator.getPosition("Can0");
		
		double dx = p2.x - p1.x;
		double dy = p2.y - p1.y;
		
		System.out.println("REWARD IS "+reward+" "+dx+" "+dy);
		return reward;
	}
	
	private void updateObservation(){
		float[] data = rangeSensor.getValue().data;
		observation = new Tensor(data, data.length);
	}
	
	private void init(){
		Position p = simulator.getPosition("youBot");
		simulator.setPosition("youBot", new Position(0, 0, p.z));
		
		// set random can position
		p = simulator.getPosition("Can1");
		float x = r.nextFloat()-0.5f;
		float y = r.nextFloat()+0.5f;
		simulator.setPosition("Can1", new Position(x, y, p.z));
		
		simulator.start(true);
		
		// TODO we try here until we get a rangeSensor ref? should be better
		simulator.tick();
		while(rangeSensor==null
				|| kukaArm == null 
				|| kukaPlatform == null){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void deinit(){
		simulator.stop();
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
		this.kukaPlatform = null;
	}
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL, policy=ReferencePolicy.DYNAMIC)
	void setLaserScanner(LaserScanner l){
		this.rangeSensor = l;
	}
	
	void unsetLaserScanner(LaserScanner l){
		this.rangeSensor = null;
	}
	
	@Reference
	void setSimulator(Simulator s){
		this.simulator = s;
	}
	
	@Override
	public void setup(Map<String, String> config) {
		if(active)
			throw new RuntimeException("This Environment is already active");

		active = true;
		
		// configure the environment
		simulator.loadScene("youbot_fetch_can.ttt");
		
		reset();
	}
	
	@Override
	public void cleanup() {
		active = false;
		
		deinit();
	}
	
}
