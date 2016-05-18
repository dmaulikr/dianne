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
package be.iminds.iot.dianne.nn.learn.processors;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import be.iminds.iot.dianne.api.nn.learn.GradientProcessor;
import be.iminds.iot.dianne.tensor.Tensor;

/**
 * Additional learning techniques like Momentum can be implemented as a Processor decorator
 */
public class MomentumProcessor extends GradientProcessor {

	private final float rate;
	
	private final Map<UUID, Tensor> momentum = new HashMap<>();
	
	public MomentumProcessor(GradientProcessor p, float rate) {
		super(p);
		this.rate = rate;
	}
	
	@Override
	public void updateDelta(long i) {
		nn.getTrainables().values().stream().forEach(m -> {
			// Get the gradients and momentum
			Tensor deltaParams = m.getDeltaParameters();
			Tensor momentum = this.momentum.computeIfAbsent(m.getId(), k -> {
				Tensor t = factory.createTensor(deltaParams.dims());
				t.fill(0.0f);
				return t;
			});
			
			// Update momentum
			factory.getTensorMath().add(deltaParams, deltaParams, rate, momentum);
			deltaParams.copyInto(momentum);
			
			// Set DeltaParameters to be sure in case of remote module instance
			m.setDeltaParameters(deltaParams);
		});
	}
}
