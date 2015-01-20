package be.iminds.iot.dianne.nn.train;

import be.iminds.iot.dianne.tensor.Tensor;

public interface Criterion {

	public Tensor forward(final Tensor output, final Tensor target);
	
	public Tensor backward(final Tensor output, final Tensor target);
	
}
