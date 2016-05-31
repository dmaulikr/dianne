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
package be.iminds.iot.dianne.nn.learn;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.util.promise.Promise;

import be.iminds.iot.dianne.api.nn.learn.Learner;
import be.iminds.iot.dianne.api.nn.module.Trainable;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorOps;

@Component(service=Learner.class, 
	property={"aiolos.unique=true",
			  "dianne.learner.category=FF"})
public class FeedForwardLearner extends AbstractLearner {
	
	protected int batchSize = 10;
	protected boolean batchAverage = true;
	
	// Current batch
	// Each lists consists of noBatches + 1 tensors: 0 = batch tensor, 1..noBatch+1 = selects per index
	private List<Tensor> batch = null;
	private List<Tensor> target = null;

	
	// For loading next batch while processing current batch
	private List<Tensor> nextBatch = null;
	private List<Tensor> nextTarget = null;
	
	protected void loadConfig(Map<String, String> config){
		super.loadConfig(config);
		
		if(config.containsKey("batchSize"))
			batchSize = Integer.parseInt(config.get("batchSize"));
		
		if(config.containsKey("batchAverage"))
			batchAverage = Boolean.parseBoolean(config.get("batchAverage"));
		
		System.out.println("* batchSize = " +batchSize);
		System.out.println("* batchAverage = " + batchAverage);
		
		// Reset next batch
		batch = null;
		target = null;
		nextBatch = null;
		nextTarget = null;
	}
	
	private void loadBatch(){
		for(int k=0;k<batchSize;k++){
			// Select new sample
			int index = sampling.next();
			
			// Preallocate batch if necessary
			if(nextBatch == null){
				int[] inputDims = dataset.inputDims();
				int[] batchInputDims = new int[inputDims.length+1];
				batchInputDims[0] = batchSize;
				for(int i=0;i<inputDims.length;i++){
					batchInputDims[i+1] = inputDims[i];
				}
				nextBatch = new ArrayList<>(batchSize+1);
				Tensor b = new Tensor(batchInputDims);
				nextBatch.add(b);
				for(int i=0;i<batchSize;i++){
					nextBatch.add(b.select(0, i));
				}
				
				int[] targetDims = dataset.outputDims();
				int[] batchTargetDims = new int[targetDims.length+1];
				batchTargetDims[0] = batchSize;
				for(int i=0;i<targetDims.length;i++){
					batchTargetDims[i+1] = targetDims[i];
				}
				nextTarget = new ArrayList<>(batchSize+1);
				Tensor o = new Tensor(batchTargetDims);
				nextTarget.add(o);
				for(int i=0;i<batchSize;i++){
					nextTarget.add(o.select(0, i));
				}
			}
			
			//Fetch sample
			dataset.getInputSample(index, nextBatch.get(k+1));
			dataset.getOutputSample(index, nextTarget.get(k+1));
		}
	}
	
	protected float process(long i){
		// Clear delta params
		nn.getTrainables().values().stream().forEach(Trainable::zeroDeltaParameters);
		
		// Use a placeholder for error
		final float[] error = new float[1];
		
		// Load batch if necessary
		if(nextBatch==null)
			loadBatch();
		
		// Flip current/next
		List<Tensor> temp = batch;
		batch = nextBatch;
		nextBatch = temp;
		
		temp = target;
		target = nextTarget;
		nextTarget = temp;
		
		// Forward/backward pass - executed asynchronously
		Promise result = nn.forward(null, null, batch.get(0)).then(
				p -> {
					// Forward
					Tensor output = p.getValue().tensor;
					
					// Error
					error[0] += criterion.error(output, target.get(0)).get(0);

					// Error gradient
					Tensor gradOut = criterion.grad(output, target.get(0));
					
					// Backward
					return nn.backward(null, null, gradOut);
				}).then(		
				p -> {	
					// Accumulate gradient weights
					nn.getTrainables().values().stream().forEach(Trainable::accGradParameters);
					
					if(batchAverage) {
						nn.getTrainables().values().stream().forEach(m -> {
							Tensor deltaParams = m.getDeltaParameters();
				
							TensorOps.div(deltaParams, deltaParams, batchSize);
									
							// Set DeltaParameters to be sure in case of remote module instance
							m.setDeltaParameters(deltaParams);
						});
						
						error[0] /= batchSize;
					}
					
					// Run gradient processors
					gradientProcessor.calculateDelta(i);
					
					// Update parameters
					nn.getTrainables().values().stream().forEach(Trainable::updateParameters);
					
					return p;
				});
		
		// Load next batch while processing previous one
		loadBatch();
		
		try {
			result.getValue();
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
		
		return error[0];
	}
	
}
