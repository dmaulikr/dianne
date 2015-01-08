package be.iminds.iot.dianne.tensor;

/**
 * Provides all supported Tensor operations. Each operation where a tensor is returned,
 * also has the argument res, in which one could provide a tensor in which the result
 * will be put and returned. This in order to save memory allocations. When res is null 
 * a new Tensor object will be created.
 * 
 * @author tverbele
 *
 */
public interface TensorMath<T extends Tensor<T>> {

	/**
	 * Add the given value to all elements in the T.
	 */
	public T add(final T res, final T T, final float value);
	
	/**
	 * Add T1 to T2 and put result into res. 
	 * The number of elements must match, but sizes do not matter.
	 */
	public T add(final T res, final T T1, final T T2);

	/**
	 * Multiply elements of T2 by the scalar value and add it to T1. 
	 * The number of elements must match, but sizes do not matter.
	 */
	public T add(final T res, final T T1, final float value, final T T2);
	
	/**
	 * Multiply all elements in the T by the given value.
	 */
	public T mul(final T res, final T T1, final float value);
	
	/**
	 * Element-wise multiplication of T1 by T2. 
	 * The number of elements must match, but sizes do not matter.
	 */
	public T cmul(final T res, final T T1, final T T2);
	
	/**
	 * Divide all elements in the T by the given value.
	 */
	public T div(final T res, final T T1, final float value);
	
	/**
	 * Element-wise division of T1 by T2. 
	 * The number of elements must match, but sizes do not matter.
	 */
	public T cdiv(final T res, final T T1, final T T2);
	
	/**
	 * Performs the dot product between vec1 and vec2. 
	 * The number of elements must match: both Ts are seen as a 1D vector.
	 */
	public float dot(final T vec1, final T vec2);
	
	/**
	 * Matrix vector product of mat and vec. 
	 * Sizes must respect the matrix-multiplication operation: 
	 * if mat is a n x m matrix, vec must be vector of size m and res must be a vector of size n.
	 */
	public T mv(final T res, final T mat, final T vec);
	
	/**
	 * Matrix matrix product of mat1 and mat2. If mat1 is a n x m matrix, mat2 a m x p matrix, 
	 * res must be a n x p matrix.
	 */
	public T mm(final T res, final T mat1, final T mat2);
	
}
