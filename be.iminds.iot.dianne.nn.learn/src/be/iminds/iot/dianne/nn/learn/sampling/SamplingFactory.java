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
 *     Tim Verbelen, Steven Bohez, Elias De Coninck
 *******************************************************************************/
package be.iminds.iot.dianne.nn.learn.sampling;

import java.util.Map;

import be.iminds.iot.dianne.api.dataset.Dataset;
import be.iminds.iot.dianne.api.nn.learn.SamplingStrategy;
import be.iminds.iot.dianne.nn.learn.sampling.config.WeightedUniformConfig;
import be.iminds.iot.dianne.nn.util.DianneConfigHandler;

public class SamplingFactory {
	
	public enum SamplingConfig {
		UNIFORM,
		WEIGHTED,
		SEQUENTIAL,
		PERMUTATION
	}
	
	public static SamplingStrategy createSamplingStrategy(SamplingConfig strategy, Dataset d, Map<String, String> config){
		SamplingStrategy sampling = null;

		switch(strategy) {
		case SEQUENTIAL:
			sampling = new SequentialSamplingStrategy(d);
			break;
		case PERMUTATION:
			sampling = new PermutationSamplingStrategy(d);
			break;
		case WEIGHTED:
			sampling = new WeightedUniformSamplingStrategy(d, DianneConfigHandler.getConfig(config, WeightedUniformConfig.class));
		default:
			sampling = new UniformSamplingStrategy(d);
		}
		
		return sampling;
	}
}
