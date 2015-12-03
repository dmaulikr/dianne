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
	T add(T res, final T tensor, final float value);
	
	/**
	 * Add tensor1 to tensor2 and put result into res. 
	 * The number of elements must match, but sizes do not matter.
	 */
	T add(T res, final T tensor1, final T tensor2);

	/**
	 * Multiply elements of tensor2 by the scalar value and add it to tensor1. 
	 * The number of elements must match, but sizes do not matter.
	 */
	T add(T res, final T tensor1, final float value, final T tensor2);
	
	/**
	 * Subract the given value of all elements in the T.
	 */
	T sub(T res, final T tensor, final float value);
	
	/**
	 * Subtract tensor2 from tensor1 and put result into res. 
	 * The number of elements must match, but sizes do not matter.
	 */
	T sub(T res, final T tensor1, final T tensor2);
	
	/**
	 * Multiply elements of tensor2 by the scalar value and subtract it from tensor1. 
	 * The number of elements must match, but sizes do not matter.
	 */
	T sub(T res, final T tensor1, final float value, final T tensor2);
	
	/**
	 * Multiply all elements in the T by the given value.
	 */
	T mul(T res, final T tensor1, final float value);
	
	/**
	 * Element-wise multiplication of tensor1 by tensor2. 
	 * The number of elements must match, but sizes do not matter.
	 */
	T cmul(T res, final T tensor1, final T tensor2);
	
	/**
	 * Divide all elements in the T by the given value.
	 */
	T div(T res, final T tensor1, final float value);
	
	/**
	 * Element-wise division of tensor1 by tensor2. 
	 * The number of elements must match, but sizes do not matter.
	 */
	T cdiv(T res, final T tensor1, final T tensor2);
	
	/**
	 * Performs the dot product between vec1 and vec2. 
	 * The number of elements must match: both Ts are seen as a 1D vector.
	 */
	float dot(final T vec1, final T vec2);
	
	/**
	 * Performs the matrix product between vec1 and vec2
	 * @param res placeholder
	 * @param vec1 vector of size m
	 * @param vec2 vector of size n
	 * @return resulting matrix of size mxn
	 */
	T vv(T res, final T vec1, final T vec2);
	
	/**
	 * Matrix vector product of mat and vec. 
	 * Sizes must respect the matrix-multiplication operation: 
	 * if mat is a n x m matrix, vec must be vector of size m and res must be a vector of size n.
	 */
	T mv(T res, final T mat, final T vec);
	
	/**
	 * Matrix vector product of transposed mat and vec. 
	 * Sizes must respect the matrix-multiplication operation: 
	 * if mat is a m x n matrix, vec must be vector of size m and res must be a vector of size n.
	 */
	T tmv(T res, final T mat, final T vec);
	
	/**
	 * Matrix matrix product of matensor1 and matensor2. If matensor1 is a n x m matrix, matensor2 a m x p matrix, 
	 * res must be a n x p matrix.
	 */
	T mm(T res, final T mat1, final T mat2);
	
	/**
	 * Performs the matrix product between vec1 and vec2 and adds this to mat
	 * @param res placeholder
	 * @param mat mxn matrix to add to result
	 * @param vec1 vector of size m
	 * @param vec2 vector of size n
	 * @return resulting matrix of size mxn
	 */
	T addvv(T res, final T mat, final T vec1, final T vec2);
	
	/**
	 * Performs a matrix-vector multiplication between mat (2D tensor) and vec (1D tensor) 
	 * and add it to vec1. In other words, res = vec1 + mat*vec2
	 */
	T addmv(T res, final T vec1, final T mat, final T vec2);

	/**
	 * Performs a matrix-vector multiplication between matensor1 (2D tensor) and matensor2 (2D tensor) 
	 * and add it to mat. In other words, res = mat + matensor1*matensor2
	 */
	T addmm(T res, final T mat, final T mat1, final T mat2);

	/**
	 * Calculates element-wise exp function
	 */
	T exp(T res, final T tensor);

	/**
	 * Calculates element-wise log function
	 */
	T log(T res, final T tensor);
	
	/**
	 * Calculates element-wise tanh function
	 */
	T tanh(T res, final T tensor);
	
	/**
	 * Calculates for each element (1-x^2)
	 */
	T dtanh(T res, final T tensor);
	
	/**
	 * Put the sigmoid of each element into res
	 */
	T sigmoid(T res, final T tensor);
	
	/**
	 * Calculates for each element x*(1-x)
	 */
	T dsigmoid(T res, final T tensor);
	
	/**
	 * All elements smaller than thresh are set to coeff * val + offset
	 */
	T thresh(T res, final T tensor, final float thresh, final float coeff, final float offset);
	
	/**
	 * All elements smaller than thresh are set to coeff * val + offset
	 */
	T thresh(T res, final T tensor, final T threshs, final T coeffs, final T offsets);
	
	/**
	 * All elements smaller than thresh are set to coeff, 1 otherwise
	 */
	T dthresh(T res, final T tensor, final float thresh, final float coeff);
	
	/**
	 * All elements smaller than thresh are set to coeff, 1 otherwise
	 */
	T dthresh(T res, final T tensor, final T threshs, final T coeffs);
	
	/**
	 * Calculates element-wise softmax function
	 */
	T softmax(T res, final T tensor);
	
	/**
	 * Return the sum of all elements
	 */
	float sum(final T tensor);
	
	/**
	 * Return the max of all elements
	 */
	float max(final T tensor);
	
	/**
	 * Return the min of all elements
	 */
	float min(final T tensor);
	
	/**
	 * Return the mean of all elements
	 */
	float mean(final T tensor);
	
	/**
	 * Return index of the max element (treats T as 1 dim vector)
	 */
	int argmax(final T tensor);
	
	/**
	 * Return index of the min element (treats T as 1 dim vector)
	 */
	int argmin(final T tensor);
	
	/**
	 * Calculate 2D convolution mat1 * mat2
	 * 
	 * mode is an integer:
	 * mode = 0 : valid convolution
	 * mode = 1 : full convolution
	 */
	T convolution2D(T res, final T mat1, final T mat2, final int sx, final int sy, final int mode, final boolean flip);

	/**
	 * Calculate 2D convolution mat1 * mat2 and add mat 
	 */
	T addconvolution2D(T res, final T add, final T mat1, final T mat2, final int sx, final int sy, final int mode, final boolean flip);

	/**
	 * Spatial convolution
	 * 
	 * Takes a 3D mat, a 4D k tensor, and outputs a 3D res
	 * 
	 * For each input plane j it convolves with k[i][j] to get the output plane i and add add[i], with strides sx,sy and padding px,py
	 */
	T spatialconvolve(T res, final T add, final T t, final T k, final int sx, final int sy, final int px, final int py);
	
	/**
	 * Add paddings to tensor t, set padding size for each dimension
	 */
	T zeropad(T res, final T t, int... paddings);
		
	/**
	 * Spatial max pool
	 * 
	 * Takes a 3D tensor input and results 3D tensor with each individual plane maxpooled2D
	 */
	T spatialmaxpool(T res, final T t, final int w, final int h, final int sx, final int sy);

	/**
	 * Spatial derivative of max pool
	 * 
	 * Calculate each max index in wxh block of t1, and 
	 * put value of subsampled t2 into res on that position
	 * 
	 * Takes a 3D tensor input and results 3D tensor with each individual plane maxpooled2D
	 */
	T spatialdmaxpool(T res, final T t2, final T t1, final int w, final int h, final int sx, final int sy);
	
	/**
	 * Scale (bilinear interpollate) in 2 dimensions
	 * In case of 3D tensor it will scale all 'channels'
	 */
	T scale2D(T res, final T t, final int... dims);
}

