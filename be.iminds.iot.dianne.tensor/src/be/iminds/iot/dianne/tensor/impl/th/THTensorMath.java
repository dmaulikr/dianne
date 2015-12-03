package be.iminds.iot.dianne.tensor.impl.th;

import be.iminds.iot.dianne.tensor.TensorMath;

public class THTensorMath implements TensorMath<THTensor> {

	@Override
	public THTensor add(THTensor res, THTensor tensor, float value) {
		long l = add(res==null ? 0 : res.address, tensor.address, value);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor add(THTensor res, THTensor tensor1, THTensor tensor2) {
		long l = add(res==null ? 0 : res.address, tensor1.address, tensor2.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor add(THTensor res, THTensor tensor1, float value,
			THTensor tensor2) {
		long l = add(res==null ? 0 : res.address, tensor1.address, value, tensor2.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor sub(THTensor res, THTensor tensor, float value) {
		long l = sub(res==null ? 0 : res.address, tensor.address, value);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor sub(THTensor res, THTensor tensor1, THTensor tensor2) {
		long l = sub(res==null ? 0 : res.address, tensor1.address, tensor2.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor sub(THTensor res, THTensor tensor1, float value,
			THTensor tensor2) {
		long l = sub(res==null ? 0 : res.address, tensor1.address, value, tensor2.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor mul(THTensor res, THTensor tensor1, float value) {
		long l = mul(res==null ? 0 : res.address, tensor1.address, value);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor cmul(THTensor res, THTensor tensor1, THTensor tensor2) {
		long l = cmul(res==null ? 0 : res.address, tensor1.address, tensor2.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor div(THTensor res, THTensor tensor1, float value) {
		long l = div(res==null ? 0 : res.address, tensor1.address, value);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor cdiv(THTensor res, THTensor tensor1, THTensor tensor2) {
		long l = cdiv(res==null ? 0 : res.address, tensor1.address, tensor2.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public float dot(THTensor vec1, THTensor vec2) {
		return dot(vec1.address, vec2.address);
	}

	@Override
	public THTensor vv(THTensor res, THTensor vec1, THTensor vec2) {
		long l = vv(res==null ? 0 : res.address, vec1.address, vec2.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor mv(THTensor res, THTensor mat, THTensor vec) {
		long l = mv(res==null ? 0 : res.address, mat.address, vec.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor tmv(THTensor res, THTensor mat, THTensor vec) {
		long l = tmv(res==null ? 0 : res.address, mat.address, vec.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor mm(THTensor res, THTensor mat1, THTensor mat2) {
		long l = mm(res==null ? 0 : res.address, mat1.address, mat2.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor addvv(THTensor res, THTensor mat, THTensor vec1,
			THTensor vec2) {
		long l = addvv(res==null ? 0 : res.address, mat.address, vec1.address, vec2.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor addmv(THTensor res, THTensor vec1, THTensor mat,
			THTensor vec2) {
		long l = addmv(res==null ? 0 : res.address, vec1.address, mat.address, vec2.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor addmm(THTensor res, THTensor mat, THTensor mat1,
			THTensor mat2) {
		long l = addmm(res==null ? 0 : res.address, mat.address, mat1.address, mat2.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor exp(THTensor res, THTensor tensor) {
		long l = exp(res==null ? 0 : res.address, tensor.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor log(THTensor res, THTensor tensor) {
		long l = log(res==null ? 0 : res.address, tensor.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor tanh(THTensor res, THTensor tensor) {
		long l = tanh(res==null ? 0 : res.address, tensor.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor dtanh(THTensor res, THTensor tensor) {
		long l = dtanh(res==null ? 0 : res.address, tensor.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor sigmoid(THTensor res, THTensor tensor) {
		long l = sigmoid(res==null ? 0 : res.address, tensor.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor dsigmoid(THTensor res, THTensor tensor) {
		long l = dsigmoid(res==null ? 0 : res.address, tensor.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor thresh(THTensor res, THTensor tensor, float thresh,
			float coeff, float offset) {
		long l = thresh(res==null ? 0 : res.address, tensor.address, thresh, coeff, offset);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor thresh(THTensor res, THTensor tensor, THTensor threshs,
			THTensor coeffs, THTensor offsets) {
		throw new UnsupportedOperationException();

//		long l = thresh(res==null ? 0 : res.address, tensor.address, threshs.address, coeffs.address, offsets.address);
//		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor dthresh(THTensor res, THTensor tensor, float thresh,
			float coeff) {
		long l = dthresh(res==null ? 0 : res.address, tensor.address, thresh, coeff);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor dthresh(THTensor res, THTensor tensor, THTensor threshs,
			THTensor coeffs) {
		throw new UnsupportedOperationException();

//		long l = dthresh(res==null ? 0 : res.address, tensor.address, threshs.address, coeffs.address);
//		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor softmax(THTensor res, THTensor tensor) {
		long l = softmax(res==null ? 0 : res.address, tensor.address);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public float sum(THTensor tensor) {
		return sum(tensor.address);
	}

	@Override
	public float max(THTensor tensor) {
		return max(tensor.address);
	}

	@Override
	public float min(THTensor tensor) {
		return min(tensor.address);
	}

	@Override
	public float mean(THTensor tensor) {
		return mean(tensor.address);
	}

	@Override
	public int argmax(THTensor tensor) {
		return argmax(tensor.address);
	}

	@Override
	public int argmin(THTensor tensor) {
		return argmin(tensor.address);
	}
	
	@Override
	public THTensor spatialconvolve(THTensor res, THTensor add, THTensor mat,
			THTensor k, int sx, int sy, int px, int py) {
		long l = spatialconvolve(res==null ? 0 : res.address, add.address, mat.address,
				k.address, sx, sy, px, py);
		return res==null ? new THTensor(l) : res;
	}
	
	@Override
	public THTensor spatialdinconvolve(THTensor res, THTensor g, THTensor k,
			int sx, int sy, int px, int py) {
		long l = spatialdinconvolve(res==null ? 0 : res.address, g.address, k.address,
				sx, sy, px, py);
		return res == null ? new THTensor(l) : res;
	}
	
	@Override
	public THTensor spatialdkerconvolve(THTensor res, THTensor add, THTensor g,
			THTensor k, int sx, int sy, int px, int py) {
		long l = spatialdkerconvolve(res == null ? 0 : res.address, add.address, g.address,
				k.address, sx, sy, px, py);
		return res == null ? new THTensor(l) : res;
	}

	@Override
	public THTensor zeropad(THTensor res, THTensor t, int... paddings) {
		long l = zeropad(res==null ? 0 : res.address, t.address, paddings);
		return res==null ? new THTensor(l) : res;
	}

	@Override
	public THTensor spatialmaxpool(THTensor res, THTensor t,
			int w, int h, int sx, int sy) {
		long l = spatialmaxpool(res==null ? 0 : res.address, t.address, w, h, sx, sy);
		return res==null ? new THTensor(l) : res;
	}
	
	@Override
	public THTensor spatialdmaxpool(THTensor res, THTensor t2, THTensor t1,
			int w, int h, int sx, int sy) {
		long l = spatialdmaxpool(res==null ? 0 : res.address, t2.address, t1.address, w, h, sx, sy);
		return res==null ? new THTensor(l) : res;
	}
	
	@Override
	public THTensor scale2D(THTensor res, THTensor t, int... dims) {
		long l = scale2d(res==null? 0 : res.address, t.address, dims);
		return res==null? new THTensor(l) : res;
	}
	
	private native long add(long res, long tensor, float value);

	private native long add(long res, long tensor1, long tensor2);

	private native long add(long res, long tensor1, float value, long tensor2);

	private native long sub(long res, long tensor, float value);

	private native long sub(long res, long tensor1, long tensor2);

	private native long sub(long res, long tensor1, float value, long tensor2);

	private native long mul(long res, long tensor1, float value);

	private native long cmul(long res, long tensor1, long tensor2);

	private native long div(long res, long tensor1, float value);

	private native long cdiv(long res, long tensor1, long tensor2);

	private native float dot(long vec1, long vec2);

	private native long vv(long res, long vec1, long vec2);

	private native long mv(long res, long mat, long vec);

	private native long tmv(long res, long mat, long vec);

	private native long mm(long res, long mat1, long mat2);

	private native long addvv(long res, long mat, long vec1, long vec2);

	private native long addmv(long res, long vec1, long mat, long vec2);

	private native long addmm(long res, long mat, long mat1, long mat2);

	private native long exp(long res, long tensor);

	private native long log(long res, long tensor);

	private native long tanh(long res, long tensor);

	private native long dtanh(long res, long tensor);

	private native long sigmoid(long res, long tensor);

	private native long dsigmoid(long res, long tensor);

	private native long thresh(long res, long tensor, float thresh,
			float coeff, float offset);

	private native long thresh(long res, long tensor, long threshs,
			long coeffs, long offsets);

	private native long dthresh(long res, long tensor, float thresh, float coeff);

	private native long dthresh(long res, long tensor, long threshs, long coeffs);

	private native long softmax(long res, long tensor);

	private native float sum(long tensor);

	private native float max(long tensor);

	private native float min(long tensor);

	private native float mean(long tensor);

	private native int argmax(long tensor);

	private native int argmin(long tensor);
	
	private native long spatialconvolve(long res, long add, long t,
			long k, int sx, int sy, int px, int py);
	
	private native long spatialdinconvolve(long res, long g, long k,
			int sx, int sy, int px, int py);
	
	private native long spatialdkerconvolve(long res, long add, long g,
			long t, int sx, int sy, int px, int py);

	private native long zeropad(long res, long t, int... paddings);

	private native long spatialmaxpool(long res, long t,
			int w, int h, int sx, int sy);

	private native long spatialdmaxpool(long res, long t2, long t1,
			int w, int h, int sx, int sy);
	
	private native long scale2d(long res, long t, int... dims);
}
