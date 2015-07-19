package be.iminds.iot.dianne.nn.module.io;

import java.util.UUID;

import be.iminds.iot.dianne.api.nn.module.AbstractModule;
import be.iminds.iot.dianne.api.nn.module.Module;
import be.iminds.iot.dianne.api.nn.module.Output;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorFactory;

public class OutputImpl extends AbstractModule implements Output {

	protected String[] labels;
	
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
	public String[] getTags(){
		return tags;
	}
	
	@Override
	public void backpropagate(final Tensor gradOutput, final String... tags) {
		backward(this.id, gradOutput, tags);
	}
	
	@Override
	protected void forward() {
		output = input;
	}

	@Override
	protected void backward() {
		gradInput = gradOutput;
	}
	
	@Override
	public void setNext(final Module... next) {
		System.out.println("Output cannot have next modules");
	}

	@Override
	public String[] getOutputLabels() {
		return labels;
	}

	@Override
	public void setOutputLabels(String[] labels) {
		this.labels = labels;
	}
}
