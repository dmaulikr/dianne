package be.iminds.iot.dianne.nn.module;

import be.iminds.iot.dianne.tensor.Tensor;

public interface Output extends Module {

	public Tensor getOutput();
	
	public void backpropagate(Tensor gradOutput);
	
	public void addOutputListener(OutputListener listener);
	
	public void removeOutputListener(OutputListener listener);
	
}
