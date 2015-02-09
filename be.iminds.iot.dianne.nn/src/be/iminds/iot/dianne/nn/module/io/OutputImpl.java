package be.iminds.iot.dianne.nn.module.io;

import java.util.UUID;

import be.iminds.iot.dianne.nn.module.AbstractModule;
import be.iminds.iot.dianne.nn.module.Module;
import be.iminds.iot.dianne.nn.module.Output;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorFactory;

public class OutputImpl extends AbstractModule implements Output {

	public OutputImpl(TensorFactory factory) {
		super(factory);
	}

	public OutputImpl(TensorFactory factory, UUID id) {
		super(factory, id);
	}
	
	@Override
	public Tensor getOutput(){
		return output;
	}
	
	@Override
	public void backpropagate(Tensor gradOutput) {
		backward(this.id, gradOutput);
	}
	
	@Override
	protected void forward(UUID from) {
		output = input;
	}

	@Override
	protected void backward(UUID from) {
		gradInput = gradOutput;
	}
	
	@Override
	public void setNext(final Module... next) {
		System.out.println("Output cannot have next modules");
	}
}
