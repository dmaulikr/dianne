package be.iminds.iot.dianne.nn.train;

import java.util.List;

import be.iminds.iot.dianne.nn.module.Trainable;
import be.iminds.iot.dianne.nn.module.io.Input;
import be.iminds.iot.dianne.nn.module.io.Output;

public interface Trainer {

	// TODO better specify the neural network to train?
	public void train(Input input, Output output, List<Trainable> module, Criterion criterion, Dataset data);
	
}
