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
package be.iminds.iot.dianne.nn.learn.strategy;

import java.util.Map;

import be.iminds.iot.dianne.api.dataset.Batch;
import be.iminds.iot.dianne.api.dataset.Dataset;
import be.iminds.iot.dianne.api.nn.NeuralNetwork;
import be.iminds.iot.dianne.api.nn.learn.Criterion;
import be.iminds.iot.dianne.api.nn.learn.GradientProcessor;
import be.iminds.iot.dianne.api.nn.learn.LearnProgress;
import be.iminds.iot.dianne.api.nn.learn.LearningStrategy;
import be.iminds.iot.dianne.api.nn.learn.SamplingStrategy;
import be.iminds.iot.dianne.nn.learn.criterion.CriterionFactory;
import be.iminds.iot.dianne.nn.learn.criterion.CriterionFactory.CriterionConfig;
import be.iminds.iot.dianne.nn.learn.processors.ProcessorFactory;
import be.iminds.iot.dianne.nn.learn.sampling.SamplingFactory;
import be.iminds.iot.dianne.nn.learn.strategy.config.GenerativeAdverserialConfig;
import be.iminds.iot.dianne.nn.util.DianneConfigHandler;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorOps;

/**
 * Generative Adversarial NN Learning strategy
 * 
 * Based on https://gist.github.com/lopezpaz/16a67948325fc97239775b09b261677a
 * 
 * @author tverbele
 *
 */
public class GenerativeAdverserialLearningStrategy implements LearningStrategy {

	protected Dataset dataset;
	
	protected NeuralNetwork generator;
	protected NeuralNetwork discriminator;
	
	protected GenerativeAdverserialConfig config;
	
	protected GradientProcessor gradientProcessorG;
	protected GradientProcessor gradientProcessorD;

	protected Criterion criterion;
	protected SamplingStrategy sampling;
	
	protected Batch batch;
	protected Tensor target;
	protected Tensor random;
	
	@Override
	public void setup(Map<String, String> config, Dataset dataset, NeuralNetwork... nns) throws Exception {
		this.dataset = dataset;
		
		this.generator = nns[0];
		this.discriminator = nns[1];
		
		this.config = DianneConfigHandler.getConfig(config, GenerativeAdverserialConfig.class);
		sampling = SamplingFactory.createSamplingStrategy(this.config.sampling, dataset, config);
		criterion = CriterionFactory.createCriterion(CriterionConfig.BCE);
		gradientProcessorG = ProcessorFactory.createGradientProcessor(this.config.method, generator, config);
		gradientProcessorD = ProcessorFactory.createGradientProcessor(this.config.method, discriminator, config);
		
		target = new Tensor(this.config.batchSize);
		random = new Tensor(this.config.batchSize, this.config.generatorDim);
	}

	@Override
	public LearnProgress processIteration(long i) throws Exception {
		// Clear delta params
		generator.zeroDeltaParameters();
		discriminator.zeroDeltaParameters();
		
		// First update the discriminator
		
		// Load minibatch of real data for the discriminator 
		batch = dataset.getBatch(batch, sampling.next(this.config.batchSize));
		// These should be classified as correct by discriminator
		target.fill(0.85f);
		
		Tensor output = discriminator.forward(batch.input);
		float error = criterion.error(output, target).get(0);
		Tensor gradOutput = criterion.grad(output, target);
		discriminator.backward(gradOutput);
		
		// Now sample a minibatch of generated data
		random.randn();
		// these should be classified as incorrect by discriminator
		target.fill(0.15f);
		Tensor generated = generator.forward(random);
		output = discriminator.forward(generated);
		error += criterion.error(output, target).get(0);
		gradOutput = criterion.grad(output, target);
		discriminator.backward(gradOutput);
		
		
		// Update discriminator weights
		discriminator.accGradParameters();
		
		// Run gradient processors
		gradientProcessorD.calculateDelta(i);
		
		// Batch done, calculate deltas
		if(config.batchAverage) {
			// Divide by batchSize in order to have learning rate independent of batchSize
			discriminator.getTrainables().values().stream().forEach(m -> {
				Tensor deltaParams = m.getDeltaParameters();
	
				TensorOps.div(deltaParams, deltaParams, config.batchSize);
		
				// Set DeltaParameters to be sure in case of remote module instance
				m.setDeltaParameters(deltaParams);
			});
			
			error /= config.batchSize;
		}
		
		discriminator.updateParameters();
		
		
		
		// Now update the generator
		// TODO do we need to do a new generation here?
		random.randn();
		// these should be classified as incorrect by discriminator
		target.fill(0.15f);
		generated = generator.forward(random);
		output = discriminator.forward(generated);
		float error2 = criterion.error(output, target).get(0);
		gradOutput = criterion.grad(output, target);
		Tensor gradInput = discriminator.backward(gradOutput);
		generator.backward(gradInput);
		
		
		// Update generator weights
		generator.accGradParameters();
		
		// Run gradient processors
		gradientProcessorG.calculateDelta(i);
		
		// Batch done, calculate deltas
		if(config.batchAverage) {
			// Divide by batchSize in order to have learning rate independent of batchSize
			generator.getTrainables().values().stream().forEach(m -> {
				Tensor deltaParams = m.getDeltaParameters();
	
				TensorOps.div(deltaParams, deltaParams, config.batchSize);
		
				// Set DeltaParameters to be sure in case of remote module instance
				m.setDeltaParameters(deltaParams);
			});
			
			error2 /= config.batchSize;
		}
		
		// Update parameters
		generator.updateParameters();
		
		
		return new LearnProgress(i, error);
	}

}
