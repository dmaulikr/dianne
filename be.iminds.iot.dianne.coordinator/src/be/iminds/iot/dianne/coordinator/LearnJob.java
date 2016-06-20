package be.iminds.iot.dianne.coordinator;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.osgi.framework.ServiceRegistration;

import be.iminds.iot.dianne.api.coordinator.Job.LearnCategory;
import be.iminds.iot.dianne.api.coordinator.Job.Type;
import be.iminds.iot.dianne.api.coordinator.LearnResult;
import be.iminds.iot.dianne.api.nn.learn.LearnProgress;
import be.iminds.iot.dianne.api.nn.learn.Learner;
import be.iminds.iot.dianne.api.nn.learn.LearnerListener;
import be.iminds.iot.dianne.api.nn.module.dto.NeuralNetworkDTO;
import be.iminds.iot.dianne.api.nn.module.dto.NeuralNetworkInstanceDTO;

public class LearnJob extends AbstractJob<LearnResult> implements LearnerListener {

	private ServiceRegistration reg;
	
	private long maxIterations = 10000;
	
	private LearnResult result = new LearnResult();
	
	private Map<UUID, Learner> learners = new HashMap<>();
	
	private Map<UUID, NeuralNetworkInstanceDTO> nnis2 = new HashMap<>();
	
	public LearnJob(DianneCoordinatorImpl coord, 
			NeuralNetworkDTO nn,
			String d,
			Map<String, String> c){
		super(coord, Type.LEARN, nn, d, c);
		
		if(c.containsKey("environment") || coord.isExperiencePool(d)){
			category = LearnCategory.RL;
		} else if(coord.isRecurrent(nn)){
			category = LearnCategory.RNN;
		} else {
			category = LearnCategory.FF;
		}
	}
		
	@Override
	public void execute() throws Exception {
		
		if(coordinator.isExperiencePool(dataset)){
			// DeepQ learner also requires target nni
			nnis2 = new HashMap<>();
			for(UUID target : targets){
				NeuralNetworkInstanceDTO nni = coordinator.platform.deployNeuralNetwork(nn.name, "Dianne Coordinator LearnJob "+jobId, target);
				nnis2.put(target, nni);
			}
		}
		
		if(config.containsKey("maxIterations")){
			maxIterations = Long.parseLong(config.get("maxIterations"));
		}
		
		Dictionary<String, Object> props = new Hashtable();
		String[] t = targets.stream().map(uuid -> uuid.toString()).collect(Collectors.toList()).toArray(new String[targets.size()]);
		props.put("targets", t);
		props.put("aiolos.unique", true);
		reg = coordinator.context.registerService(LearnerListener.class, this, props);
		
		// TODO deploy dataset?
		
		// set training set
		final Map<String, String> learnConfig = new HashMap<>(config);
		learnConfig.put("range", config.get("trainSet"));

		System.out.println("Start Learn Job");
		System.out.println("===============");
		System.out.println("* nn: "+nn.name);
		System.out.println("* dataset: "+dataset);
		System.out.println("* maxIterations: "+maxIterations);
		System.out.println("---");
		
		// start learning on each learner
		for(UUID target : targets){
			Learner learner = coordinator.learners.get(category.toString()).get(target);
			learners.put(target, learner);
			// if nnis2 is unused, this will give null, but doesnt matter?
			learner.learn(dataset, learnConfig, nnis.get(target), nnis2.get(target));
		}
	}
	
	@Override
	public void onProgress(UUID learnerId, LearnProgress progress) {
		if(deferred.getPromise().isDone()){
			return;
		}
		
		result.progress.add(progress);
		
		coordinator.sendLearnProgress(this.jobId, progress);
		
		// maxIterations stop condition
		// what in case of multiple learners?!
		boolean stop = progress.iteration >= maxIterations;
				
		// TODO other stop conditions
		// - check error rate evolution (on train and/or validationSet)
		// - stop at a certain error rate
		// - stop after certain time
		// ...
				
		// if stop ... assemble result object and resolve
		if(stop){
			for(Learner learner : learners.values()){
				learner.stop();
			}
		}
	}

	@Override
	public void onException(UUID learnerId, Throwable e) {
		if(deferred.getPromise().isDone()){
			return;
		}
		done(e);
	}

	@Override
	public void onFinish(UUID learnerId, LearnProgress p) {
		result.progress.add(p);
		done(result);
	}

	@Override
	public void cleanup() {
		if(reg!=null)
			reg.unregister();

		for(Learner learner : learners.values()){
			learner.stop();
		}
		
		// these are used in case of deep q learning
		for(NeuralNetworkInstanceDTO nni : nnis2.values()){
			coordinator.platform.undeployNeuralNetwork(nni);
		}
	}

	@Override
	public LearnResult getProgress() {
		return result;
	}


	@Override
	public void stop() throws Exception{
		if(started > 0){
			done(result);
		} else {
			done(new Exception("Job "+this.jobId+" cancelled."));
		}
	}

}
