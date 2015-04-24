package be.iminds.iot.dianne.tensor.impl.nd4j;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import be.iminds.iot.dianne.tensor.TensorFactory;
import be.iminds.iot.dianne.tensor.TensorMath;

public class ND4JTensorFactory implements TensorFactory<ND4JTensor>{

	public ND4JTensorFactory() {
		Nd4j.factory().setOrder('c');
	}
	
	@Override
	public ND4JTensor createTensor(int... d) {
		INDArray nd = Nd4j.create(d);
		return new ND4JTensor(nd);
	}

	@Override
	public ND4JTensor createTensor(float[] data, int... d) {
		INDArray nd = Nd4j.create(data, d);
		return new ND4JTensor(nd);
	}

	@Override
	public TensorMath<ND4JTensor> getTensorMath() {
		return new ND4JTensorMath();
	}

}
