package be.iminds.iot.dianne.rl.learn.strategy;

import java.util.Map;
import java.util.UUID;

import be.iminds.iot.dianne.api.dataset.Dataset;
import be.iminds.iot.dianne.api.nn.NeuralNetwork;
import be.iminds.iot.dianne.api.nn.learn.Criterion;
import be.iminds.iot.dianne.api.nn.learn.GradientProcessor;
import be.iminds.iot.dianne.api.nn.learn.LearnProgress;
import be.iminds.iot.dianne.api.nn.learn.LearningStrategy;
import be.iminds.iot.dianne.api.nn.learn.SamplingStrategy;
import be.iminds.iot.dianne.api.nn.module.dto.ModuleInstanceDTO;
import be.iminds.iot.dianne.api.nn.module.dto.NeuralNetworkInstanceDTO;
import be.iminds.iot.dianne.api.rl.dataset.ExperiencePool;
import be.iminds.iot.dianne.api.rl.dataset.ExperiencePoolSample;
import be.iminds.iot.dianne.api.rl.learn.QLearnProgress;
import be.iminds.iot.dianne.nn.learn.criterion.CriterionFactory;
import be.iminds.iot.dianne.nn.learn.processors.ProcessorFactory;
import be.iminds.iot.dianne.nn.learn.sampling.SamplingFactory;
import be.iminds.iot.dianne.nn.util.DianneConfigHandler;
import be.iminds.iot.dianne.rl.learn.strategy.config.DeepQConfig;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorOps;

public class DeepDeterministicPolicyGradientStrategy implements LearningStrategy {

	protected DeepQConfig config;
	
	protected ExperiencePool pool;
	protected SamplingStrategy sampling;
	protected ExperiencePoolSample interaction;
	
	protected NeuralNetwork actor;
	protected NeuralNetwork targetActor;
	
	protected NeuralNetwork critic;
	protected NeuralNetwork targetCritic;
	
	protected Criterion criterion;
	protected GradientProcessor actorProcessor;
	protected GradientProcessor criticProcessor;
	
	protected UUID stateIn;
	protected UUID actionIn;
	protected UUID valueOut;
	
	protected UUID[] inputIds;
	protected UUID[] outputIds;
	
	protected Tensor stateBatch;
	protected Tensor actionBatch;
	protected Tensor targetValueBatch;
	
	@Override
	public void setup(Map<String, String> config, Dataset dataset, NeuralNetwork... nns) throws Exception {
		if(!(dataset instanceof ExperiencePool))
			throw new RuntimeException("Dataset is no experience pool");
		
		this.pool = (ExperiencePool) dataset;
		
		if(nns.length != 4)
			throw new RuntimeException("Invalid number of NN instances provided: "+nns.length+" (expected 4)");
		
		this.actor = nns[0];
		this.targetActor = nns[1];
		this.critic = nns[2];
		this.targetCritic = nns[3];
		
		this.config = DianneConfigHandler.getConfig(config, DeepQConfig.class);
		this.sampling = SamplingFactory.createSamplingStrategy(this.config.sampling, dataset, config);
		this.criterion = CriterionFactory.createCriterion(this.config.criterion, config);
		this.actorProcessor = ProcessorFactory.createGradientProcessor(this.config.method, actor, config);
		this.criticProcessor = ProcessorFactory.createGradientProcessor(this.config.method, critic, config);
		
		// Look for the critic inputs corresponding to state & action
		NeuralNetworkInstanceDTO nndto = this.critic.getNeuralNetworkInstance();
		for(UUID iid : this.critic.getInputs().keySet()) {
			ModuleInstanceDTO mdto = nndto.modules.get(iid);
			String mname = mdto.module.properties.get("name");
			
			if(mname.equalsIgnoreCase("state"))
				this.stateIn = iid;
			else if(mname.equalsIgnoreCase("action"))
				this.actionIn = iid;
		}
		this.valueOut = this.critic.getOutput().getId();
		
		if(stateIn == null || actionIn == null || valueOut == null)
			throw new RuntimeException("Unable to select correct Input modules from network " + nndto.name);
		
		this.inputIds = new UUID[]{this.stateIn, this.actionIn};
		this.outputIds = new UUID[]{this.valueOut};
		
		// Pre-allocate tensors for batch operations
		this.stateBatch = new Tensor(this.config.batchSize, this.pool.stateDims());
		this.actionBatch = new Tensor(this.config.batchSize, this.pool.actionDims());
		this.targetValueBatch = new Tensor(this.config.batchSize);
		
		// Wait for the pool to contain enough samples
		while(pool.size() < this.config.minSamples){
			System.out.println("Experience pool has too few samples, waiting a bit to start learning...");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				return;
			}
		}
		
		System.out.println("Start learning...");
	}

	@Override
	public LearnProgress processIteration(long i) throws Exception {
		// Reset the deltas
		actor.zeroDeltaParameters();
		critic.zeroDeltaParameters();
		
		// Fill in the batch
		for(int b = 0; b < config.batchSize; b++) {
			// Get a sample interaction
			int index = sampling.next();
			interaction = pool.getSample(interaction, index);
			
			// Get the data from the sample
			Tensor state = interaction.getState();
			Tensor action = interaction.getAction();
			Tensor nextState = interaction.getNextState();
			float reward = interaction.getReward();
			
			// Copy state & action into there respective batches
			state.copyInto(stateBatch.select(0, b));
			action.copyInto(actionBatch.select(0, b));
			
			// Calculate the target value
			if(!interaction.isTerminal) {
				// If the next state is not terminal, get the next value using the target actor and critic
				Tensor nextAction = targetActor.forward(nextState);
				Tensor nextValue = targetCritic.forward(inputIds, outputIds, new Tensor[]{nextState, nextAction}).getValue().tensor;
				
				// Set the target value using the Bellman equation
				targetValueBatch.set(reward + config.discount*nextValue.get(0), b);
			} else {
				// If the next state is terminal, the target value is equal to the reward
				targetValueBatch.set(reward, b);
			}
		}
		
		// Forward pass of the critic to get the current value estimate
		Tensor valueBatch = critic.forward(inputIds, outputIds, new Tensor[]{stateBatch, actionBatch}).getValue().tensor;
		
		// Get the total value for logging and calculate the MSE error and gradient with respect to the target value
		float value = TensorOps.sum(valueBatch)/config.batchSize;
		float loss = criterion.loss(valueBatch, targetValueBatch);
		Tensor criticGrad = criterion.grad(valueBatch, targetValueBatch);
		
		// Backward pass of the critic
		critic.backward(outputIds, inputIds, new Tensor[]{criticGrad}).getValue();
		critic.accGradParameters();
		
		// Get the actor action for the current state
		Tensor actionBatch = actor.forward(stateBatch);
		
		// Get the actor gradient by evaluating the critic and use it's gradient with respect to the action
		// Note: By default we're doing minimization, so set critic gradient to -1
		critic.forward(inputIds, outputIds, new Tensor[]{stateBatch, actionBatch}).getValue();
		criticGrad.fill(-1);
		Tensor actorGrad = critic.backward(outputIds, inputIds, new Tensor[]{criticGrad}).getValue().tensors.get(actionIn);
		
		// Backward pass of the actor
		actor.backward(actorGrad);
		actor.accGradParameters();
		
		// Call the processors to set the updates
		actorProcessor.calculateDelta(i);
		criticProcessor.calculateDelta(i);
		
		// Apply the updates
		// Note: target actor & critic get updated automatically by setting the syncInterval option
		actor.updateParameters();
		critic.updateParameters();
		
		return new QLearnProgress(i, loss, value);
	}

}
