package be.iminds.iot.dianne.nn.module.layer;

import java.util.UUID;

import be.iminds.iot.dianne.api.nn.module.AbstractTrainableModule;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorFactory;

public class SpatialConvolution extends AbstractTrainableModule {

	private int noInputPlanes;
	private int noOutputPlanes;
	private int kernelWidth;
	private int kernelHeight;
	private int strideX;
	private int strideY;
	private int padX = 0;
	private int padY = 0;
	
	// subtensors for weights / bias
	Tensor weights;
	Tensor deltaWeights;
	Tensor bias;
	Tensor deltaBias;
	
	public SpatialConvolution(TensorFactory factory,
			int noInputPlanes, int noOutputPlanes, 
			int kernelWidth, int kernelHeight,
			int strideX, int strideY, boolean pad){
		super(factory);
		init(noInputPlanes, noOutputPlanes, kernelWidth, kernelHeight, strideX, strideY, pad);
	}
	
	public SpatialConvolution(TensorFactory factory, UUID id,
			int noInputPlanes, int noOutputPlanes, 
			int kernelWidth, int kernelHeight,
			int strideX, int strideY, boolean pad){
		super(factory, id);
		init(noInputPlanes, noOutputPlanes, kernelWidth, kernelHeight, strideX, strideY, pad);
	}
	
	protected void init(int noInputPlanes, int noOutputPlanes, 
			int kernelWidth, int kernelHeight, int strideX, int strideY, boolean pad){
		this.noInputPlanes = noInputPlanes;
		this.noOutputPlanes = noOutputPlanes;
		this.kernelWidth = kernelWidth;
		this.kernelHeight = kernelHeight;
		this.strideX = strideX;
		this.strideY = strideY;
		if(pad){
			this.padX = (kernelWidth-1)/2;
			this.padY = (kernelHeight-1)/2;
		}
		
		parameters = factory.createTensor(noOutputPlanes*noInputPlanes*kernelWidth*kernelHeight+noOutputPlanes);
		weights = parameters.narrow(0, 0, noOutputPlanes*noInputPlanes*kernelWidth*kernelHeight);
		weights.reshape(noOutputPlanes, noInputPlanes, kernelWidth, kernelHeight);
		bias = parameters.narrow(0, noOutputPlanes*noInputPlanes*kernelWidth*kernelHeight, noOutputPlanes);
		
		deltaParameters = factory.createTensor(noOutputPlanes*noInputPlanes*kernelWidth*kernelHeight+noOutputPlanes);
		deltaWeights = deltaParameters.narrow(0, 0, noOutputPlanes*noInputPlanes*kernelWidth*kernelHeight);
		deltaWeights.reshape(noOutputPlanes, noInputPlanes, kernelWidth, kernelHeight);
		deltaBias = deltaParameters.narrow(0, noOutputPlanes*noInputPlanes*kernelWidth*kernelHeight, noOutputPlanes);
		
		
		// initialize weights uniform [-std, std] with std = 1/sqrt(kW*kH*noInputPlanes)  [from torch]
		parameters.rand();
		float std = (float) (1f/Math.sqrt(kernelWidth*kernelHeight*noInputPlanes));
		parameters = factory.getTensorMath().mul(parameters, parameters, 2*std);
		parameters = factory.getTensorMath().sub(parameters, parameters, std);
	}
	
	@Override
	protected void forward() {
		int[] outDims = new int[3];
		outDims[0] = noOutputPlanes;

		if(input.dim()==2){
			outDims[1] = (input.size(0) + 2*padY - kernelHeight)/strideY + 1;
			outDims[2] = (input.size(1) + 2*padX - kernelWidth)/strideX + 1;
		} else if(input.dim()==3){
			outDims[1] = (input.size(1) + 2*padY - kernelHeight )/strideY + 1;
			outDims[2] = (input.size(2) + 2*padX - kernelWidth )/strideX + 1;
		} // else error?
		if(output==null || !output.hasDim(outDims)){
			output = factory.createTensor(outDims);
		}
		// TODO check input planes dim? // check kernel sizes?
		
		output = factory.getTensorMath().spatialconvolve(output, bias, input, weights, strideX, strideY, padX, padY);
	}

	@Override
	protected void backward() {
		if(strideX!=1 || strideY!=1){
			// TODO also implement this for strides != 1
			throw new UnsupportedOperationException();
		}
		
		// backward based on http://andrew.gibiansky.com/blog/machine-learning/convolutional-neural-networks/
		if(gradInput == null || !gradInput.sameDim(input)){
			gradInput = factory.createTensor(input.dims());
		}
		
		// TODO create subtensors once and reuse?
		for(int i=0;i<noInputPlanes;i++){
			Tensor planeKernels = weights.select(1, i);
			Tensor gradInputPlane = noInputPlanes== 1 ? gradInput : gradInput.select(0, i);
			gradInputPlane.fill(0.0f);
			for(int j=0;j<noOutputPlanes;j++){
				Tensor kernel = planeKernels.select(0, j);
				
				// update gradInput
				// this should be "full" convolution and flipped kernel?
				factory.getTensorMath().addconvolution2D(gradInputPlane, gradInputPlane,
						gradOutput.select(0, j), kernel, 1, 1, 1, true);
			}
		}
	}

	@Override
	public void accGradParameters() {
		if(strideX!=1 || strideY!=1){
			// TODO also implement this for strides != 1
			throw new UnsupportedOperationException();
		}
		
		// calculate grad weights based on http://andrew.gibiansky.com/blog/machine-learning/convolutional-neural-networks/
		if(gradOutput!=null){
			for(int i=0;i<noOutputPlanes;i++){
				Tensor planeGradKernels = deltaWeights.select(0, i);
			
				for(int j=0;j<noInputPlanes;j++){
					Tensor gradKernel = planeGradKernels.select(0, j);
					
					//  update gradKernel
					factory.getTensorMath().addconvolution2D(gradKernel, gradKernel, 
							noInputPlanes== 1 ? input : input.select(0, j), gradOutput.select(0, i), 1, 1, 0, false);
				}
			}
			
			// grad bias
			for(int i=0;i<noOutputPlanes;i++){
				float sum = factory.getTensorMath().sum(gradOutput.select(0, i));
				deltaBias.set(deltaBias.get(i)+sum, i);
			}
		}
	}
}
