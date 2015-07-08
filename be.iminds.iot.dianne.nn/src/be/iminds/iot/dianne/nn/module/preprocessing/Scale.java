package be.iminds.iot.dianne.nn.module.preprocessing;

import java.util.UUID;

import be.iminds.iot.dianne.api.dataset.Dataset;
import be.iminds.iot.dianne.api.nn.module.AbstractModule;
import be.iminds.iot.dianne.api.nn.module.Preprocessor;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorFactory;

public class Scale extends AbstractModule {

	// dims of the scaled tensor
	private int[] targetDims;
	
	public Scale(TensorFactory factory){
		super(factory);
	}
	
	public Scale(TensorFactory factory, UUID id){
		super(factory, id);
	}
	
	public Scale(TensorFactory factory, UUID id, int[] dims){
		super(factory, id);
		this.targetDims = dims;
	}

	@Override
	protected void forward() {
		output = factory.getTensorMath().scale2D(output, input, targetDims);
	}

	@Override
	protected void backward() {
		// not implemented
	}

}
