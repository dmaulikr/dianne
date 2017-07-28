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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.osgi.service.component.annotations.Component;
import org.osgi.util.promise.Promise;

import be.iminds.iot.dianne.api.rl.environment.Environment;
import be.iminds.iot.dianne.nn.util.DianneConfigHandler;
import be.iminds.iot.dianne.rl.environment.kuka.api.KukaEnvironment;
import be.iminds.iot.dianne.rl.environment.kuka.config.ReacherConfig;
import be.iminds.iot.dianne.rl.environment.kuka.config.ReacherConfig.Mode;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorOps;
import be.iminds.iot.robot.api.JointDescription;
import be.iminds.iot.robot.api.JointState;
import be.iminds.iot.robot.api.arm.Arm;


@Component(immediate = true,
	service = {Environment.class, KukaEnvironment.class},
	property = { "name="+ReacherEnvironment.NAME, 
				 "aiolos.unique=true",
				 "aiolos.combine=*",
				 "osgi.command.scope=reacher",
				 "osgi.command.function=start",
				 "osgi.command.function=stop",
				 "osgi.command.function=pause",
				 "osgi.command.function=resume",
				 "osgi.command.function=reward",
				 "osgi.command.function=resetCan"})
public class ReacherEnvironment extends FetchCanEnvironment {
	
	public static final String NAME = "Reacher";
	
	protected ReacherConfig config;
	private float min = -1.0f;
	private float max = 1.0f;
	
	@Override
	public String getName(){
		return NAME;
	}

	@Override
	public int[] actionDims() {
		return new int[]{6};
	}
	
	@Override
	public int[] observationDims() {
		int[] dims =  super.observationDims();
		int size = 0;
		for (int i : dims) {
			size *= i;
		}
		List<JointState> state = this.kukaArm.getState();
		dims = new int[]{size + state.size() * 2};
		return dims;
	}
	
	@Override
	protected void updateObservation(){
		super.updateObservation();
		int jointLength = this.kukaArm.getState().size();
		float[] data = this.observation.get();
		int totalLength = data.length + jointLength * 2;
		float[] result = Arrays.copyOf(data, totalLength);
		int offset = data.length;
		JointDescription joint = null;
		JointState state = null;
		float minPos, maxPos, minV, maxV;
		for (int i=0; i<jointLength; i++) {
			state = this.kukaArm.getState().get(i);
			joint = this.kukaArm.getJoints().get(i);
			minPos = joint.getPositionMin();
			maxPos = joint.getPositionMax();
			result[offset] = (state.position - minPos)/(maxPos - minPos); // tranform to new range [0,1].
			maxV = (float) Math.PI/2+1; // should be set in rososgi (PID and torque control are different) 2.5rad/s is for torque control
			minV = -maxV;
			result[offset + jointLength] = (state.velocity - minV)/(maxV - minV); // tranform to new range [0,1].
			offset += 1;
		}
		observation = new Tensor(result, totalLength);
	}
	
	final float middlePos = 2.92510465f;
	final float minPos = middlePos - ((float)Math.PI/2);
	final float maxPos = minPos + (float)Math.PI;
	
	@Override
	protected void executeAction(Tensor t) throws Exception {
		float[] f = Arrays.copyOf(t.get(), t.get().length + 1);
		if (config.gripperFixed)
			f[f.length - 2] = min;
		else 
			f[f.length - 2] = f[f.length - 2] >= 0 ? max : min; 
		f[f.length - 1] = f[f.length - 2] * (config.mode == Mode.POSITION ? 1 : -1);
		
		List<JointDescription> joints = kukaArm.getJoints();
		float a,b;
		JointDescription joint;
		for (int i=0; i < f.length; i++) {
			joint = joints.get(i);
			switch (config.mode) {
			case POSITION:
				a = joint.getPositionMin();
				b = joint.getPositionMax();
				break;
			case VELOCITY:
				a = joint.getVelocityMin();
				b = joint.getVelocityMax();
				break;
			case TORQUE:
				a = joint.getTorqueMin();
				b = joint.getTorqueMax();
				break;
			default:
				throw new Exception("Unsupported mode for reacher.");
			}
			a *= config.outputScaleFactor;
			b *= config.outputScaleFactor;
			f[i] = (f[i]-min)/(max - min)*(b-a) + a; // tranform from [min,max] to new range.
		}
		switch (config.mode) {
		case POSITION:
			kukaArm.setPositions(f);
			break;
		case VELOCITY:
			if (config.limitFirstJoint) {
				// Clamp the first joints' movement between -Math.PI, Math.PI
				int jointNr = 0;
				float joint0Pos = this.kukaArm.getState().get(jointNr).position;
				if (joint0Pos <= minPos) {
					this.kukaArm.setPosition(jointNr, minPos);
					f[0]=0; // set joint `jointNr` speed to 0
				} else if (joint0Pos >= maxPos) {
					this.kukaArm.setPosition(jointNr, maxPos);
					f[0]=0; // set joint `jointNr` speed to 0
				}
			}
			kukaArm.setVelocities(f);
			break;
		case TORQUE:
			kukaArm.setTorques(f);
			break;
		}
	
		// simulate an iteration further
		if(simulator != null){
			for(int i=0;i<=super.config.skip;i++){
				simulator.tick();
			}
		}
	}
	
	@Override
	protected float calculateEnergy(Tensor a) throws Exception {
		if (config.mode == Mode.TORQUE)
			return TensorOps.dot(a, a); // normalized torques
		else 
			return super.calculateEnergy(a);
	}
	
	@Override
	protected float calculateVelocity(Tensor a) throws Exception {
		int nrJoints = this.kukaArm.getState().size();
		Tensor v = this.observation.narrow(0, this.observation.size() - nrJoints, nrJoints); // normalized velocities
		return TensorOps.dot(v, v);
	}
	
	@Override
	public void configure(Map<String, String> config) {
		this.config = DianneConfigHandler.getConfig(config, ReacherConfig.class);
		
		super.configure(config);
	}
	
	@Override
	protected boolean checkCollisions(){
		if(simulator == null)
			return false;
		
		return simulator.checkCollisions("Border") 
				|| simulator.checkCollisions("BorderArm") 
				|| simulator.checkCollisions("SelfCollision")
				|| simulator.checkCollisions("Floor")
				|| simulator.checkCollisions("Gripper");
	}
	
	@Override
	protected void initAction(){
		// reset arm to candle
		Promise<Arm> p = kukaArm.setPositions(middlePos, 1.103709733f, -2.478948503f, 1.72566195f, 2.765485f);
		// simulate an iteration further
		while(!p.isDone() && active) {
			try {
				simulator.tick();
			} catch(TimeoutException e){}
		}
	}
}
