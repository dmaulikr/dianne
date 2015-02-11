package be.iminds.iot.dianne.nn.module.conv;

import java.util.UUID;

import be.iminds.iot.dianne.nn.module.AbstractTrainableModule;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorFactory;

public class SpatialConvolution extends AbstractTrainableModule {

	private int noInputPlanes;
	private int noOutputPlanes;
	private int kernelWidth;
	private int kernelHeight;
	
	public SpatialConvolution(TensorFactory factory,
			int noInputPlanes, int noOutputPlanes, 
			int kernelWidth, int kernelHeight){
		super(factory);
		init(noInputPlanes, noOutputPlanes, kernelWidth, kernelHeight);
	}
	
	public SpatialConvolution(TensorFactory factory, UUID id,
			int noInputPlanes, int noOutputPlanes, 
			int kernelWidth, int kernelHeight){
		super(factory, id);
		init(noInputPlanes, noOutputPlanes, kernelWidth, kernelHeight);
	}
	
	// TODO strides support?
	protected void init(int noInputPlanes, int noOutputPlanes, 
			int kernelWidth, int kernelHeight){
		this.noInputPlanes = noInputPlanes;
		this.noOutputPlanes = noOutputPlanes;
		this.kernelWidth = kernelWidth;
		this.kernelHeight = kernelHeight;
		
		parameters = factory.createTensor(noOutputPlanes, noInputPlanes, kernelWidth, kernelHeight);
		
		parameters.randn();
	}
	
	@Override
	protected void forward() {
		int[] outDims = new int[3];
		outDims[0] = noOutputPlanes;
		if(input.dim()==2){
			outDims[1] = input.size(0) - kernelHeight + 1;
			outDims[2] = input.size(1) - kernelWidth + 1;
		} else if(input.dim()==3){
			outDims[1] = input.size(1) - kernelHeight + 1;
			outDims[2] = input.size(2) - kernelWidth + 1;
		} // else error?
		if(output==null || !output.hasDim(outDims)){
			output = factory.createTensor(outDims);
		}
		// TODO check input planes dim? // check kernel sizes?
	
		// TODO create subtensors once and reuse?
		Tensor temp = null;
		
		for(int i=0;i<noOutputPlanes;i++){
			Tensor planeKernels = parameters.select(0, i);
			Tensor outputPlane = output.select(0, i);
			outputPlane.fill(0.0f);
			for(int j=0;j<noInputPlanes;j++){
				Tensor kernel = planeKernels.select(0, j);
				
				// TODO convadd operation to avoid temp?
				temp = factory.getTensorMath().convolution2D(temp,
						noInputPlanes== 1 ? input : input.select(0, j), kernel);
				factory.getTensorMath().add(outputPlane, outputPlane, temp);
			}
		}
	}

	@Override
	protected void backward() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void accGradParameters() {
		// TODO Auto-generated method stub
		
	}

}
