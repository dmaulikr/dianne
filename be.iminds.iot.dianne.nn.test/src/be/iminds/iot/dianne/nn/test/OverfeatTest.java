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
package be.iminds.iot.dianne.nn.test;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.UUID;

import org.osgi.framework.ServiceReference;

import be.iminds.iot.dianne.api.dataset.Dataset;
import be.iminds.iot.dianne.api.nn.module.ForwardListener;
import be.iminds.iot.dianne.api.nn.module.Module;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorOps;
import junit.framework.Assert;


public class OverfeatTest extends AbstractDianneTest {

	public final int TEST_SAMPLE = 2;

	private Dataset imagenet;
	
	public void setUp() throws Exception {
    	super.setUp();
    	
    	ServiceReference[] rds = context.getAllServiceReferences(Dataset.class.getName(), null);
    	for(ServiceReference rd : rds){
    		Dataset d = (Dataset) context.getService(rd);
    		if(d.getName().equals("ImageNet")){
    			imagenet = d;
    		}
    	}
    }
	
	public void testOverfeat() throws Exception {
		deployNN("../tools/nn/overfeat_fast_2/modules.txt");
		
		final Tensor sample = imagenet.getInputSample(TEST_SAMPLE);		
		final Tensor result = new Tensor(1000);
		
		// wait for output
		final Object lock = new Object();
		getOutput().addForwardListener(new ForwardListener() {
			
			@Override
			public void onForward(UUID moduleId, Tensor output, String... tags) {
				output.copyInto(result);
			
				synchronized(lock){
					lock.notifyAll();
				}
			}
		});

		// Write intermediate output to file
		for(Module m : getModules()){
			m.addForwardListener(new ForwardListener() {
				@Override
				public void onForward(UUID moduleId, Tensor output, String... tags) {
					try {
						File f = new File("out_"+m.getId()+".txt");
						PrintWriter writer = new PrintWriter(f);
						writer.println(Arrays.toString(output.dims()));
						
						float[] data = output.get();
						for(int i=0;i<data.length;i++){
							writer.write(data[i]+" ");
						}
						writer.close();
					} catch(Exception e){
					}
				}
			});
		}
		
		synchronized(lock){
			getInput().input(sample);
			lock.wait();
		}
		
		int index = TensorOps.argmax(result);
		System.out.println(getOutput().getOutputLabels()[index]+" "+result.get(index));
		
		int expected = TensorOps.argmax(imagenet.getOutputSample(TEST_SAMPLE));
		System.out.println("Expected: "+getOutput().getOutputLabels()[expected]);
		Assert.assertEquals(expected, index);
	}
}
