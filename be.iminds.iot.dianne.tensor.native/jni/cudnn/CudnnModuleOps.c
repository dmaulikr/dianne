/*******************************************************************************
 * DIANNE  - Framework for distributed artificial neural networks
 * Copyright (C) 2015  iMinds - IBCN - UGent
 *
 * This file is part of DIANNE.
 *
 * DIANNE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Tim Verbelen, Steven Bohez
 *******************************************************************************/
#include "be_iminds_iot_dianne_tensor_ModuleOps.h"
#include "TensorLoader.h"

#include "CudnnTensor.h"

#include <time.h>
clock_t start, end;
double cpu_time_used;


cudnnHandle_t cudnnHandle;

// optionally fix convolution algorithms
int convFwAlg = -1;
int convBwAlg = -1;
int convAgAlg = -1;


// to be used when we use a shared workspace (by default)
int workspaceLimit = -1;
int shareWorkspace = 1;
size_t workspaceSize = 0;
void* workspace;


float alpha = 1.0f, beta = 0.0f;

JNIEXPORT jobject JNICALL Java_be_iminds_iot_dianne_tensor_ModuleOps_spatialconvolve
  (JNIEnv * env, jclass cl, jobject out, jobject in, jobject ker, jobject b, jobject t1, jobject t2, jint kW, jint kH, jint dW, jint dH, jint pW, jint pH){
	THTensor* input = getTensor(env, in);
	THTensor* output = getTensor(env, out);
	THTensor* weight = getTensor(env, ker);
	THTensor* bias = getTensor(env, b);

//start = clock();

	// get input dimensions
	int n = input->nDimension == 4 ? input->size[0] : 1;
	int c = input->nDimension == 4 ? input->size[1] : input->size[0];
	int h = input->nDimension == 4 ? input->size[2] : input->size[1];
	int w =  input->nDimension == 4 ? input->size[3] : input->size[2];

	// declare all cudnn descriptors
	cudnnFilterDescriptor_t filterDescriptor;
	cudnnConvolutionDescriptor_t convDescriptor;
	cudnnConvolutionFwdAlgo_t algo;

	cudnnTensorDescriptor_t inputTensor;
	cudnnTensorDescriptor_t outputTensor;
	cudnnTensorDescriptor_t biasTensor;

	// create cudnn decriptors
	checkCUDNN(cudnnCreateFilterDescriptor(&filterDescriptor));
	checkCUDNN(cudnnCreateConvolutionDescriptor(&convDescriptor));

	checkCUDNN(cudnnCreateTensorDescriptor(&inputTensor));
	checkCUDNN(cudnnCreateTensorDescriptor(&outputTensor));
	checkCUDNN(cudnnCreateTensorDescriptor(&biasTensor));


	// set filter descriptor
    checkCUDNN(cudnnSetFilter4dDescriptor(filterDescriptor,
                                          CUDNN_DATA_FLOAT,
                                          CUDNN_TENSOR_NCHW,
                                          weight->size[0],
                                          weight->size[1]/(kW*kH),
                                          kH,
										  kW));

    // set convolution descriptor
    checkCUDNN(cudnnSetConvolution2dDescriptor(convDescriptor,
                                          pH, pW,
                                          dH, dW,
                                          1, 1,
										  CUDNN_CROSS_CORRELATION));

    // set tensor descriptors
    checkCUDNN(cudnnSetTensor4dDescriptor(inputTensor,
                                          CUDNN_TENSOR_NCHW,
                                          CUDNN_DATA_FLOAT,
                                          n, c, h, w));

    checkCUDNN(cudnnSetTensor4dDescriptor(biasTensor,
                                          CUDNN_TENSOR_NCHW,
                                          CUDNN_DATA_FLOAT,
                                          1, weight->size[0],
										  1, 1));

    // find dimension of convolution output and reshape output
    checkCUDNN(cudnnGetConvolution2dForwardOutputDim(convDescriptor,
                                                     inputTensor,
                                                     filterDescriptor,
													 &n, &c, &h, &w));
    if(input->nDimension == 3 && n == 1){
		THTensor_(resize3d)(
			state,
			output, c, h, w);
    } else {
		THTensor_(resize4d)(
			state,
			output, n, c, h, w);
    }

    checkCUDNN(cudnnSetTensor4dDescriptor(outputTensor,
                                                  CUDNN_TENSOR_NCHW,
                                                  CUDNN_DATA_FLOAT,
                                                  n, c,
												  h, w));

    // select convolution forward algorithm
    if(convFwAlg == -1){
    	checkCUDNN(cudnnGetConvolutionForwardAlgorithm(cudnnHandle,
                                                   inputTensor,
                                                   filterDescriptor,
                                                   convDescriptor,
                                                   outputTensor,
                                                   workspaceLimit == -1 ? CUDNN_CONVOLUTION_FWD_PREFER_FASTEST :
                                                		   workspaceLimit == 0 ? CUDNN_CONVOLUTION_FWD_NO_WORKSPACE :
                                                				   CUDNN_CONVOLUTION_FWD_SPECIFY_WORKSPACE_LIMIT
												   ,
                                                   workspaceLimit,
												   &algo));
    } else {
    	algo = static_cast<cudnnConvolutionFwdAlgo_t>(convFwAlg);;
    }

    // setup workspace
	size_t size = 0;
    checkCUDNN(cudnnGetConvolutionForwardWorkspaceSize(cudnnHandle,
                                                       inputTensor,
                                                       filterDescriptor,
                                                       convDescriptor,
                                                       outputTensor,
                                                       algo,
													   &size));

//    printf("CONV ALGORITHM %d - WORKSPACE SIZE %d \n", algo, size);

    void* ws;
    if(shareWorkspace > 0){
		// resize shared workspace if required
		if(size > workspaceSize){
			THCudaCheck(cudaFree(workspace));
			THCudaCheck(cudaMalloc(&workspace, size));
			workspaceSize = size;
		}

		ws = workspace;
    } else {
    	// use temp tensor to act as local workspace for this op
		THTensor* workspace = getTensor(env, t1);

		THTensor_(resize1d)(
			state,
			workspace, size);

		ws = THTensor_(data)(state, workspace);
    }

//end = clock();
//cpu_time_used = ((double) (end - start)) / CLOCKS_PER_SEC;
//printf("CPU TIME PREPARING DESCRIPTORS %f \n",cpu_time_used);


    // do convolution forward
	checkCUDNN(cudnnConvolutionForward(cudnnHandle, &alpha, inputTensor, THTensor_(data)(state, input),
											   filterDescriptor, THTensor_(data)(state, weight),
											   convDescriptor, algo,
											   ws, size, &beta,
	                                           outputTensor, THTensor_(data)(state, output)));

	// add bias
	checkCUDNN(cudnnAddTensor(cudnnHandle, &alpha, biasTensor, THTensor_(data)(state, bias),
								&alpha, outputTensor, THTensor_(data)(state, output)));


	// cleanup descriptors
	checkCUDNN(cudnnDestroyTensorDescriptor(inputTensor));
	checkCUDNN(cudnnDestroyTensorDescriptor(outputTensor));
	checkCUDNN(cudnnDestroyTensorDescriptor(biasTensor));

	checkCUDNN(cudnnDestroyConvolutionDescriptor(convDescriptor));
	checkCUDNN(cudnnDestroyFilterDescriptor(filterDescriptor));

	return out == NULL ? createTensorObject(env, output) : out;
}



JNIEXPORT jobject JNICALL Java_be_iminds_iot_dianne_tensor_ModuleOps_spatialconvolveGradIn
  (JNIEnv * env, jclass cl, jobject gradIn, jobject gradOut, jobject ker, jobject in, jobject t1, jobject t2, jint kW, jint kH, jint dW, jint dH, jint pW, jint pH){
	THTensor* gradInput = getTensor(env, gradIn);
	THTensor* gradOutput = getTensor(env, gradOut);
	THTensor* input = getTensor(env, in);
	THTensor* weight = getTensor(env, ker);

	// get gradOut dimensions
	int n = gradOutput->nDimension == 4 ? gradOutput->size[0] : 1;
	int c = gradOutput->nDimension == 4 ? gradOutput->size[1] : gradOutput->size[0];
	int h = gradOutput->nDimension == 4 ? gradOutput->size[2] : gradOutput->size[1];
	int w =  gradOutput->nDimension == 4 ? gradOutput->size[3] : gradOutput->size[2];

	// declare cudnn descriptors
	cudnnFilterDescriptor_t filterDescriptor;
	cudnnConvolutionDescriptor_t convDescriptor;
	cudnnConvolutionBwdDataAlgo_t algo;

	cudnnTensorDescriptor_t gradInputTensor;
	cudnnTensorDescriptor_t gradOutputTensor;

	// create cudnn descriptors
	checkCUDNN(cudnnCreateFilterDescriptor(&filterDescriptor));
	checkCUDNN(cudnnCreateConvolutionDescriptor(&convDescriptor));

	checkCUDNN(cudnnCreateTensorDescriptor(&gradInputTensor));
	checkCUDNN(cudnnCreateTensorDescriptor(&gradOutputTensor));

	// set filter descriptor
    checkCUDNN(cudnnSetFilter4dDescriptor(filterDescriptor,
                                          CUDNN_DATA_FLOAT,
                                          CUDNN_TENSOR_NCHW,
                                          weight->size[0],
                                          weight->size[1]/(kW*kH),
                                          kH,
										  kW));

    // set convolution descriptor
    checkCUDNN(cudnnSetConvolution2dDescriptor(convDescriptor,
                                          pH, pW,
                                          dH, dW,
                                          1, 1,
										  CUDNN_CROSS_CORRELATION));

    // set tensor descriptor
    checkCUDNN(cudnnSetTensor4dDescriptor(gradOutputTensor,
                                          CUDNN_TENSOR_NCHW,
                                          CUDNN_DATA_FLOAT,
                                          n, c, h, w));

    // gradIn has same size as input
    n = input->nDimension == 4 ? input->size[0] : 1;
    c = input->nDimension == 4 ? input->size[1] : input->size[0];
    h = input->nDimension == 4 ? input->size[2] : input->size[1];
    w = input->nDimension == 4 ? input->size[3] : input->size[2];

    checkCUDNN(cudnnSetTensor4dDescriptor(gradInputTensor,
                                          CUDNN_TENSOR_NCHW,
                                          CUDNN_DATA_FLOAT,
                                          n, c, h, w));

    if(input->nDimension == 3 && n == 1){
		THTensor_(resize3d)(
			state,
			gradInput, c, h, w);
    } else {
		THTensor_(resize4d)(
			state,
			gradInput, n, c, h, w);
    }

    // select backward data algorithm
    if(convBwAlg == -1){
    	checkCUDNN(cudnnGetConvolutionBackwardDataAlgorithm(cudnnHandle,
    							filterDescriptor, gradOutputTensor,
								convDescriptor, gradInputTensor,
								CUDNN_CONVOLUTION_BWD_DATA_PREFER_FASTEST, 0,
								&algo));
    } else {
        algo = static_cast<cudnnConvolutionBwdDataAlgo_t>(convBwAlg);;
    }

    // fix workspace size
	size_t size = 0;
    checkCUDNN(cudnnGetConvolutionBackwardDataWorkspaceSize(cudnnHandle,
    							filterDescriptor, gradOutputTensor,
								convDescriptor, gradInputTensor,
								algo, &size));

    void* ws;
    if(shareWorkspace > 0){
		// resize shared workspace if required
		if(size > workspaceSize){
			THCudaCheck(cudaFree(workspace));
			THCudaCheck(cudaMalloc(&workspace, size));
			workspaceSize = size;
		}

		ws = workspace;
    } else {
    	// use temp tensor to act as local workspace for this op
		THTensor* workspace = getTensor(env, t2);

		THTensor_(resize1d)(
			state,
			workspace, size);

		ws = THTensor_(data)(state, workspace);
    }


    // execute backward
	checkCUDNN(cudnnConvolutionBackwardData(cudnnHandle, &alpha,
											   filterDescriptor, THTensor_(data)(state, weight),
											   gradOutputTensor, THTensor_(data)(state, gradOutput),
											   convDescriptor, algo,
											   ws, size, &beta,
	                                           gradInputTensor, THTensor_(data)(state, gradInput)));


	// cleanup cudnn descriptors
	checkCUDNN(cudnnDestroyTensorDescriptor(gradInputTensor));
	checkCUDNN(cudnnDestroyTensorDescriptor(gradOutputTensor));

	checkCUDNN(cudnnDestroyConvolutionDescriptor(convDescriptor));
	checkCUDNN(cudnnDestroyFilterDescriptor(filterDescriptor));

	return gradIn == NULL ? createTensorObject(env, gradInput) : gradIn;
}



JNIEXPORT void JNICALL Java_be_iminds_iot_dianne_tensor_ModuleOps_spatialconvolveAccGrad
  (JNIEnv * env, jclass cl, jobject gradKer, jobject gradB, jobject gradOut, jobject in, jobject t1, jobject t2, jint kW, jint kH, jint dW, jint dH, jint pW, jint pH){
	THTensor* gradWeight = getTensor(env, gradKer);
	THTensor* gradBias = getTensor(env, gradB);
	THTensor* gradOutput = getTensor(env, gradOut);
	THTensor* input = getTensor(env, in);

	// declare cudnn descriptors
	cudnnFilterDescriptor_t filterDescriptor;
	cudnnConvolutionDescriptor_t convDescriptor;
	cudnnConvolutionBwdFilterAlgo_t algo;

	cudnnTensorDescriptor_t inputTensor;
	cudnnTensorDescriptor_t gradOutputTensor;
	cudnnTensorDescriptor_t gradBiasTensor;

	// create cudnn descriptors
	checkCUDNN(cudnnCreateFilterDescriptor(&filterDescriptor));
	checkCUDNN(cudnnCreateConvolutionDescriptor(&convDescriptor));

	checkCUDNN(cudnnCreateTensorDescriptor(&inputTensor));
	checkCUDNN(cudnnCreateTensorDescriptor(&gradOutputTensor));
	checkCUDNN(cudnnCreateTensorDescriptor(&gradBiasTensor));

	// set filter descriptor
    checkCUDNN(cudnnSetFilter4dDescriptor(filterDescriptor,
                                          CUDNN_DATA_FLOAT,
                                          CUDNN_TENSOR_NCHW,
                                          gradWeight->size[0],
										  gradWeight->size[1]/(kW*kH),
                                          kH,
										  kW));

    // set convolution descriptor
    checkCUDNN(cudnnSetConvolution2dDescriptor(convDescriptor,
                                          pH, pW,
                                          dH, dW,
                                          1, 1,
										  CUDNN_CROSS_CORRELATION));

    // set tensor descriptors
	int n = input->nDimension == 4 ? input->size[0] : 1;
	int c = input->nDimension == 4 ? input->size[1] : input->size[0];
	int h = input->nDimension == 4 ? input->size[2] : input->size[1];
	int w =  input->nDimension == 4 ? input->size[3] : input->size[2];

    checkCUDNN(cudnnSetTensor4dDescriptor(inputTensor,
                                          CUDNN_TENSOR_NCHW,
                                          CUDNN_DATA_FLOAT,
                                          n, c, h, w));

    n = gradOutput->nDimension == 4 ? gradOutput->size[0] : 1;
    c = gradOutput->nDimension == 4 ? gradOutput->size[1] : gradOutput->size[0];
    h = gradOutput->nDimension == 4 ? gradOutput->size[2] : gradOutput->size[1];
    w = gradOutput->nDimension == 4 ? gradOutput->size[3] : gradOutput->size[2];

    checkCUDNN(cudnnSetTensor4dDescriptor(gradOutputTensor,
                                          CUDNN_TENSOR_NCHW,
                                          CUDNN_DATA_FLOAT,
                                          n, c, h, w));

    checkCUDNN(cudnnSetTensor4dDescriptor(gradBiasTensor,
                                          CUDNN_TENSOR_NCHW,
                                          CUDNN_DATA_FLOAT,
                                          1, gradWeight->size[0],
										  1, 1));


    // select backward filter algorithm
    if(convAgAlg == -1) {
    	checkCUDNN(cudnnGetConvolutionBackwardFilterAlgorithm(cudnnHandle,
    							inputTensor, gradOutputTensor,
								convDescriptor, filterDescriptor,
								CUDNN_CONVOLUTION_BWD_FILTER_PREFER_FASTEST, 0,
								&algo));
    } else {
    	algo = static_cast<cudnnConvolutionBwdFilterAlgo_t>(convFwAlg);
    }

    // fix workspace size
	size_t size = 0;
    checkCUDNN(cudnnGetConvolutionBackwardFilterWorkspaceSize(cudnnHandle,
    							inputTensor, gradOutputTensor,
								convDescriptor, filterDescriptor,
								algo, &size));

    void* ws;
    if(shareWorkspace > 0){
		// resize shared workspace if required
		if(size > workspaceSize){
			THCudaCheck(cudaFree(workspace));
			THCudaCheck(cudaMalloc(&workspace, size));
			workspaceSize = size;
		}

		ws = workspace;
    } else {
    	// use temp tensor to act as local workspace for this op
		THTensor* workspace = getTensor(env, t2);

		THTensor_(resize1d)(
			state,
			workspace, size);

		ws = THTensor_(data)(state, workspace);
    }


    // calculate gradient on the bias
    checkCUDNN(cudnnConvolutionBackwardBias(cudnnHandle, &alpha,
    									gradOutputTensor, THTensor_(data)(state, gradOutput),
										&beta, gradBiasTensor, THTensor_(data)(state, gradBias)));


    // calculate gradient on the weights
    checkCUDNN(cudnnConvolutionBackwardFilter(cudnnHandle, &alpha,
    									inputTensor, THTensor_(data)(state, input),
										gradOutputTensor, THTensor_(data)(state, gradOutput),
										convDescriptor, algo,
										ws, size, &beta,
										filterDescriptor, THTensor_(data)(state, gradWeight)));

    // cleanup cudnn descriptors
	checkCUDNN(cudnnDestroyTensorDescriptor(inputTensor));
	checkCUDNN(cudnnDestroyTensorDescriptor(gradOutputTensor));
	checkCUDNN(cudnnDestroyTensorDescriptor(gradBiasTensor));

	checkCUDNN(cudnnDestroyConvolutionDescriptor(convDescriptor));
	checkCUDNN(cudnnDestroyFilterDescriptor(filterDescriptor));
}


