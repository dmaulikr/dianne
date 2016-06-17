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
package be.iminds.iot.dianne.nn.learn.criterion;

import be.iminds.iot.dianne.api.nn.learn.Criterion;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorOps;

/**
 * Mean squared error criterion
 * 
 * @author tverbele
 *
 */
public class MSECriterion implements Criterion {

	protected Tensor diff;
	protected Tensor error;
	protected Tensor grad;
	
	public MSECriterion() {
		this.error = new Tensor(1);
	}
	
	@Override
	public Tensor error(final Tensor output, final Tensor target) {
		diff = TensorOps.sub(diff, output, target);
		error.set(TensorOps.dot(diff, diff) / (output.dim() == 2 ? output.size(1) : output.size(0)), 0);
		return error;
	}

	@Override
	public Tensor grad(final Tensor output, final Tensor target) {
		grad = TensorOps.mul(grad, diff, 2.0f / (output.dim() == 2 ? output.size(1) : output.size(0)));
		return grad;
	}

}
