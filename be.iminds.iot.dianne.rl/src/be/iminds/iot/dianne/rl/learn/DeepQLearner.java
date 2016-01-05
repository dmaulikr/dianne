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
package be.iminds.iot.dianne.rl.learn;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import be.iminds.iot.dianne.api.log.DataLogger;
import be.iminds.iot.dianne.api.nn.Dianne;
import be.iminds.iot.dianne.api.nn.NeuralNetwork;
import be.iminds.iot.dianne.api.nn.learn.LearnProgress;
import be.iminds.iot.dianne.api.nn.learn.Processor;
import be.iminds.iot.dianne.api.nn.module.Module.Mode;
import be.iminds.iot.dianne.api.nn.module.Trainable;
import be.iminds.iot.dianne.api.nn.module.dto.NeuralNetworkInstanceDTO;
import be.iminds.iot.dianne.api.rl.dataset.ExperiencePool;
import be.iminds.iot.dianne.api.rl.learn.QLearner;
import be.iminds.iot.dianne.rl.learn.factory.QLearnerFactory;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorFactory;

@Component
public class DeepQLearner implements QLearner {

	protected UUID learnerId;

	private DataLogger logger;
	private String[] logLabels = new String[]{"minibatch time (ms)"};
	
	private TensorFactory factory;
	private Dianne dianne;
	private Map<String, ExperiencePool> pools = new HashMap<String, ExperiencePool>();

	private NeuralNetwork nn;
	private NeuralNetwork target;

	private Map<UUID, Tensor> previousParameters;
	
	private Thread learningThread;
	private volatile boolean learning;
	
	private Processor processor;
	private ExperiencePool pool; 

	private String tag = "learn";
	private int targetInterval = 1000;
	private int syncInterval = 0;
	private int storeInterval = 0;
	private String storeTag = "store";
	private int minSamples = 10000;
	private boolean clean = false;
	
	protected float error = 0;
	protected long i = 0;

	@Reference
	void setTensorFactory(TensorFactory factory) {
		this.factory = factory;
	}

	@Reference
	void setDianne(Dianne d){
		dianne = d;
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addExperiencePool(ExperiencePool pool, Map<String, Object> properties) {
		String name = (String) properties.get("name");
		this.pools.put(name, pool);
	}

	void removeExperiencePool(ExperiencePool pool, Map<String, Object> properties) {
		String name = (String) properties.get("name");
		this.pools.remove(name);
	}
	
	@Activate
	public void activate(BundleContext context){
		this.learnerId = UUID.fromString(context.getProperty(Constants.FRAMEWORK_UUID));
	}

	@Deactivate
	void deactivate() {
		if (learning)
			stop();
	}

	@Override
	public void learn(String experiencePool, Map<String, String> config, NeuralNetworkInstanceDTO... nni) throws Exception {
		if (learning)
			throw new Exception("Already running a Learner here");
		else if (experiencePool == null || !pools.containsKey(experiencePool))
			throw new Exception("ExperiencePool " + experiencePool + " is null or not available");

		if (config.containsKey("tag"))
			tag = config.get("tag");

		if (config.containsKey("targetInterval"))
			targetInterval = Integer.parseInt(config.get("targetInterval"));
		
		if (config.containsKey("syncInterval"))
			syncInterval = Integer.parseInt(config.get("syncInterval"));
		
		if (config.containsKey("storeInterval"))
			storeInterval = Integer.parseInt(config.get("storeInterval"));
		
		if (config.containsKey("storeTag"))
			storeTag = config.get("storeTag");
		
		if (config.containsKey("minSamples"))
			minSamples = Integer.parseInt(config.get("minSamples"));
		
		if (config.containsKey("clean"))
			clean = Boolean.parseBoolean(config.get("clean"));
		
		System.out.println("Learner Configuration");
		System.out.println("=====================");
		System.out.println("* tag = "+tag);
		System.out.println("* targetInterval = "+targetInterval);
		System.out.println("* syncInterval = "+syncInterval);
		System.out.println("* minSamples = "+minSamples);
		System.out.println("* clean = "+clean);
		System.out.println("---");
		
		try {
			nn = dianne.getNeuralNetwork(nni[0]).getValue();
		} catch(Exception e){}
		if (nn == null)
			throw new Exception("Network instance " + nni[0].id + " is not available");
		nn.getInput().setMode(EnumSet.of(Mode.BLOCKING));

		try {
			target = dianne.getNeuralNetwork(nni[1]).getValue();
		} catch(Exception e){}
		if (target == null)
			throw new Exception("Target instance " + nni[1].id + " is not available");
		target.getInput().setMode(EnumSet.of(Mode.BLOCKING));

		
		pool = pools.get(experiencePool);
		
		// store the labels
		nn.setOutputLabels(pool.getLabels());

		// create a Processor from config
		processor = QLearnerFactory.createProcessor(factory, nn, target, pool, config, logger);

		i = 0;
		error = 0;
		
		learning = true;
		learningThread = new Thread(new DeepQLearnerRunnable());
		learningThread.start();
	}

	@Override
	public void stop() {
		try {
			if (learningThread != null && learningThread.isAlive()) {
				learning = false;
				learningThread.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void loadParameters(NeuralNetwork... nns) {
		try {
			for(NeuralNetwork nn : nns){
				previousParameters = nn.loadParameters(tag);
			}
		} catch (Exception ex) {
			resetParameters();
		}
	}
	
	private void initializeParameters(){
		if(clean){
			// reset parameters
			resetParameters();
		} else {
			// load previous parameters
			loadParameters(nn, target);
		}
	}

	private void resetParameters(){
		nn.randomizeParameters();
		
		// store those parameters
		nn.storeParameters(tag);
		
		// copy to target
		target.setParameters(nn.getParameters());
		
		// copy to previousParameters
		previousParameters =  nn.getParameters().entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().copyInto(null)));
	}

	private class DeepQLearnerRunnable implements Runnable {

		private static final float alpha = 1e-2f;

		@Override
		public void run() {
			try {
				
				float err = 0;
				long timestamp = System.currentTimeMillis();
				
				initializeParameters();
				
				// wait until pool has some samples
				if(pool.size() < minSamples){
					System.out.println("Experience pool has too few samples, waiting a bit to start learning...");
					while(pool.size() < minSamples){
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
						}
					}
				}
				System.out.println("Start learning...");
				
				for (i = 1; learning; i++) {
					nn.getTrainables().values().stream().forEach(Trainable::zeroDeltaParameters);
					
					pool.lock();
					try {
						err = processor.processNext();
						if(Double.isInfinite(err) || Double.isNaN(err)){
							System.out.println(i+" ERROR IS "+err);
						}
						
					} finally {
						pool.unlock();
					}
					
					error = (1 - alpha) * error + alpha * err;
	
					if(logger!=null){
						long t = System.currentTimeMillis();
						logger.log("TIME", logLabels, (float)(t-timestamp));
						timestamp = t;
					}
	
					nn.getTrainables().values().stream().forEach(Trainable::updateParameters);
	
					if (targetInterval > 0 && i % targetInterval == 0) {
						nn.storeDeltaParameters(previousParameters, tag);
						loadParameters(nn, target);
						// also store these parameters tagged with batch number
						nn.storeParameters(""+i);
					} else if(syncInterval > 0 && i % syncInterval == 0){
						nn.storeDeltaParameters(previousParameters, tag);
						loadParameters(nn);
					}
					
					if(storeInterval > 0 && i % storeInterval == 0){
						nn.storeParameters(storeTag, ""+i);
					}
				}
	
				nn.storeDeltaParameters(previousParameters, tag);
			
			} catch(Throwable t){
				System.err.println("Error during learning");
				t.printStackTrace();
				learning = false;
			}
			System.out.println("Stopped learning");
		}

	}
	
	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	void setDataLogger(DataLogger l){
		this.logger = l;
	}

	@Override
	public UUID getLearnerId() {
		return learnerId;
	}

	@Override
	public LearnProgress getProgress() {
		return new LearnProgress(i, error);
	}

	@Override
	public boolean isBusy() {
		return learning;
	}
}
