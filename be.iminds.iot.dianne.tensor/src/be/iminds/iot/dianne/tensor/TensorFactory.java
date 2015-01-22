package be.iminds.iot.dianne.tensor;


/**
 * Factory interface to create Tensors and get a suitable TensorMath object for these Tensors.
 * Within a single runtime, every Tensor should be created via the TensorFactory and only the
 * matching TensorMath object should be used. 
 * 
 * @author tverbele
 *
 */
public interface TensorFactory<T extends Tensor<T>> {
	
	public T createTensor(final int ... d);
	
	public TensorMath<T> getTensorMath();
	
}
