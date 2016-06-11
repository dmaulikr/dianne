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
package be.iminds.iot.dianne.nn.module.layer;

import org.junit.Test;

import be.iminds.iot.dianne.nn.module.ModuleTest;
import be.iminds.iot.dianne.tensor.Tensor;

public class VolumetricFullConvolutionTest extends ModuleTest{

	@Test
	public void testVolumetricFullConvolution1() throws InterruptedException {
		int noInputPlanes = 2;
		int noOutputPlanes = 3;
		int kernelWidth = 3;
		int kernelHeight = 3;
		int kernelDepth = 3;
		int stride = 1;
		int padding = 0;
		Convolution sc = new FullConvolution(noInputPlanes, noOutputPlanes, kernelWidth, kernelHeight, kernelDepth, stride, stride, stride, padding, padding, padding);
		
		Tensor input = new Tensor(2, 2, 2, 2);
		input.fill(2.0f);
		
		Tensor gradOutput = new Tensor(3, 4, 4, 4);
		gradOutput.fill(1.0f);
		
		Tensor params = new Tensor(noOutputPlanes*(noInputPlanes*kernelDepth*kernelHeight*kernelWidth+1));
		params.fill(1.0f);
		
		float[] expOutputData = new float[]{
			5.0f, 9.0f, 9.0f, 5.0f, 9.0f, 17.0f, 17.0f, 9.0f, 9.0f, 17.0f, 17.0f, 9.0f, 5.0f, 9.0f, 9.0f, 5.0f,
			9.0f, 17.0f, 17.0f, 9.0f, 17.0f, 33.0f, 33.0f, 17.0f, 17.0f, 33.0f, 33.0f, 17.0f, 9.0f, 17.0f, 17.0f,
			9.0f, 9.0f, 17.0f, 17.0f, 9.0f, 17.0f, 33.0f, 33.0f, 17.0f, 17.0f, 33.0f, 33.0f, 17.0f, 9.0f, 17.0f,
			17.0f, 9.0f, 5.0f, 9.0f, 9.0f, 5.0f, 9.0f, 17.0f, 17.0f, 9.0f, 9.0f, 17.0f, 17.0f, 9.0f, 5.0f, 9.0f,
			9.0f, 5.0f, 5.0f, 9.0f, 9.0f, 5.0f, 9.0f, 17.0f, 17.0f, 9.0f, 9.0f, 17.0f, 17.0f, 9.0f, 5.0f, 9.0f,
			9.0f, 5.0f, 9.0f, 17.0f, 17.0f, 9.0f, 17.0f, 33.0f, 33.0f, 17.0f, 17.0f, 33.0f, 33.0f, 17.0f, 9.0f,
			17.0f, 17.0f, 9.0f, 9.0f, 17.0f, 17.0f, 9.0f, 17.0f, 33.0f, 33.0f, 17.0f, 17.0f, 33.0f, 33.0f, 17.0f,
			9.0f, 17.0f, 17.0f, 9.0f, 5.0f, 9.0f, 9.0f, 5.0f, 9.0f, 17.0f, 17.0f, 9.0f, 9.0f, 17.0f, 17.0f, 9.0f,
			5.0f, 9.0f, 9.0f, 5.0f, 5.0f, 9.0f, 9.0f, 5.0f, 9.0f, 17.0f, 17.0f, 9.0f, 9.0f, 17.0f, 17.0f, 9.0f,
			5.0f, 9.0f, 9.0f, 5.0f, 9.0f, 17.0f, 17.0f, 9.0f, 17.0f, 33.0f, 33.0f, 17.0f, 17.0f, 33.0f, 33.0f,
			17.0f, 9.0f, 17.0f, 17.0f, 9.0f, 9.0f, 17.0f, 17.0f, 9.0f, 17.0f, 33.0f, 33.0f, 17.0f, 17.0f, 33.0f,
			33.0f, 17.0f, 9.0f, 17.0f, 17.0f, 9.0f, 5.0f, 9.0f, 9.0f, 5.0f, 9.0f, 17.0f, 17.0f, 9.0f, 9.0f, 17.0f,
			17.0f, 9.0f, 5.0f, 9.0f, 9.0f, 5.0f
		};
		Tensor expOutput = new Tensor(expOutputData, 3, 4, 4, 4);
		
		Tensor expGradInput = new Tensor(2, 2, 2, 2);
		expGradInput.fill(81.0f);

		Tensor expDelta = new Tensor(noOutputPlanes*(noInputPlanes*kernelDepth*kernelHeight*kernelWidth+1));
		expDelta.fill(16.0f);
		expDelta.set(64, expDelta.size()-3);
		expDelta.set(64, expDelta.size()-2);
		expDelta.set(64, expDelta.size()-1);
		
		testModule(sc, params, input, expOutput, gradOutput, expGradInput, expDelta);
	}
	
  }
