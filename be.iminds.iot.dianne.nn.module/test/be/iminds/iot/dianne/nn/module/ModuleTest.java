package be.iminds.iot.dianne.nn.module;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;

import be.iminds.iot.dianne.api.nn.module.BackwardListener;
import be.iminds.iot.dianne.api.nn.module.ForwardListener;
import be.iminds.iot.dianne.api.nn.module.Module;
import be.iminds.iot.dianne.api.nn.module.Trainable;
import be.iminds.iot.dianne.tensor.NativeTensorLoader;
import be.iminds.iot.dianne.tensor.Tensor;

public class ModuleTest {
	
	@BeforeClass
	public static void setup() {
		NativeTensorLoader loader = new NativeTensorLoader();
		loader.activate();
	}
	
	protected void testModule(Module m, Tensor input, Tensor expOutput, Tensor gradOutput, 
			Tensor expGradInput) throws InterruptedException {
		try {
			Tensor output = new Tensor();
			Tensor gradInput = new Tensor();
			
			m.addForwardListener(new ForwardListener() {
				@Override
				public void onForward(UUID moduleId, Tensor o, String... tags) {
					o.copyInto(output);
					m.backward(UUID.randomUUID(), gradOutput);
				}
			});
			m.addBackwardListener(new BackwardListener() {
				@Override
				public void onBackward(UUID moduleId, Tensor gi, String... tags) {
					synchronized(m) {
						if(expGradInput!=null)
							gi.copyInto(gradInput);
						m.notify();
					}
				}
			});
			synchronized(m) {
				m.forward(UUID.randomUUID(), input);
				m.wait(1000);
			}
			
			Assert.assertTrue(expOutput.equals(output, 0.005f));
			if(expGradInput != null)
				Assert.assertTrue(expGradInput.equals(gradInput, 0.005f));

		} catch (UnsupportedOperationException ex) {
			Assume.assumeNoException("Method not implemented yet for current configuration.", ex);
		}
	}

	
	protected void testModule(Trainable m, Tensor params, Tensor input, Tensor expOutput, Tensor gradOutput, 
			Tensor expGradInput, Tensor expDeltaParameters) throws InterruptedException {
		try {
			Tensor output = new Tensor();
			Tensor gradInput = new Tensor();
			
			m.zeroDeltaParameters();
			m.setParameters(params);		
			
			m.addForwardListener(new ForwardListener() {
				@Override
				public void onForward(UUID moduleId, Tensor o, String... tags) {
					o.copyInto(output);
					m.backward(UUID.randomUUID(), gradOutput);
				}
			});
			m.addBackwardListener(new BackwardListener() {
				@Override
				public void onBackward(UUID moduleId, Tensor gi, String... tags) {
					synchronized(m) {
						gi.copyInto(gradInput);
						m.notify();
					}
				}
			});
			synchronized(m) {
				m.forward(UUID.randomUUID(), input);
				m.wait(1000);
				m.accGradParameters();
			}

			Assert.assertTrue(expOutput.equals(output, 0.001f));
			Assert.assertTrue(expGradInput.equals(gradInput, 0.001f));
			Assert.assertTrue(expDeltaParameters.equals(m.getDeltaParameters(), 0.001f));
			
		} catch (UnsupportedOperationException ex) {
			Assume.assumeNoException("Method not implemented yet for current configuration.", ex);
		} 
	}
}
