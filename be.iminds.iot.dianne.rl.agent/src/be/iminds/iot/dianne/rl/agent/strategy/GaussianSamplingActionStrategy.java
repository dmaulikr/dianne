package be.iminds.iot.dianne.rl.agent.strategy;

import java.util.Map;

import be.iminds.iot.dianne.api.nn.NeuralNetwork;
import be.iminds.iot.dianne.api.rl.agent.ActionStrategy;
import be.iminds.iot.dianne.api.rl.agent.AgentProgress;
import be.iminds.iot.dianne.api.rl.environment.Environment;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorOps;

public class GaussianSamplingActionStrategy implements ActionStrategy {

	private NeuralNetwork policy;
	
	private int actionDims;
	private Tensor action;
	
	@Override
	public void setup(Map<String, String> config, Environment env, NeuralNetwork... nns) throws Exception {
		this.policy = nns[0];
		this.actionDims = env.actionDims()[0];
		this.action = new Tensor(this.actionDims);
	}

	@Override
	public AgentProgress processIteration(long i, Tensor state) throws Exception {
		Tensor actionParams = policy.forward(state);
		
		Tensor means = actionParams.narrow(0, 0, actionDims);
		Tensor stdevs = actionParams.narrow(0, actionDims, actionDims);
		
		action.randn();
		
		TensorOps.cmul(action, action, stdevs);
		TensorOps.add(action, action, means);
		
		return new AgentProgress(i, action);
	}

}
