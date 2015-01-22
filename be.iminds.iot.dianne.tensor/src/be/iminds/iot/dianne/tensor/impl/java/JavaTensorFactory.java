package be.iminds.iot.dianne.tensor.impl.java;

import org.osgi.service.component.annotations.Component;

import be.iminds.iot.dianne.tensor.TensorFactory;
import be.iminds.iot.dianne.tensor.TensorMath;

@Component
public class JavaTensorFactory implements TensorFactory<JavaTensor>{

	private final JavaTensorMath math = new JavaTensorMath(this);
	
	@Override
	public JavaTensor createTensor(int... d) {
		return new JavaTensor(d);
	}

	@Override
	public TensorMath<JavaTensor> getTensorMath() {
		return math;
	}

}
