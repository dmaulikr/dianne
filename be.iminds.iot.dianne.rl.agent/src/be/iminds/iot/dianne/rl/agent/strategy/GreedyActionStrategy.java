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
package be.iminds.iot.dianne.rl.agent.strategy;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import be.iminds.iot.dianne.api.log.DataLogger;
import be.iminds.iot.dianne.nn.util.DianneConfigHandler;
import be.iminds.iot.dianne.rl.agent.api.ExplorationController;
import be.iminds.iot.dianne.rl.agent.strategy.config.GreedyConfig;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorOps;

@Component(property={"strategy=GREEDY",
		"aiolos.proxy=false"})
public class GreedyActionStrategy implements ActionStrategy, ExplorationController {
	
	private GreedyConfig config;
	
	private DataLogger logger = null;
	private String[] loglabels = new String[]{"Q0", "Q1", "Q2", "epsilon"};
	
	public Tensor selectActionFromOutput(Tensor output, long i) {
		
		Tensor action = new Tensor(output.size());
		action.fill(-1);
		
		double epsilon = config.epsilonMin + (config.epsilonMax - config.epsilonMin) * Math.exp(-i * config.epsilonDecay);
		
		if(logger!=null){
			logger.log("AGENT", loglabels, output.get()[0],output.get()[1],output.get()[2], (float)epsilon);
		}
		
		if (Math.random() < epsilon) {
			action.set(1, (int) (Math.random() * action.size()));
		} else {
			action.set(1, TensorOps.argmax(output));
		}
		
		return action;
	}
	
	@Override
	public void configure(Map<String, String> config) {
		this.config = DianneConfigHandler.getConfig(config, GreedyConfig.class);
	}

	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	void setDataLogger(DataLogger l){
		this.logger = l;
		this.logger.setAlpha("epsilon", 1f);
		this.logger.setAlpha("Q0", 1f);
		this.logger.setAlpha("Q1", 1f);
		this.logger.setAlpha("Q2", 1f);
	}

	@Override
	public void setExploration(float exploration) {
		this.config.epsilonMax = exploration;
		this.config.epsilonMin = exploration;
	}

}
