#ifndef THTENSOR_CUDA_OPS_H
#define THTENSOR_CUDA_OPS_H

// define some additional operations for CUDATensors 

void THCudaTensor_dtanh(THCState *state, THCudaTensor *dst, THCudaTensor* src);
void THCudaTensor_sigoid(THCState *state, THCudaTensor *dst, THCudaTensor* src);
void THCudaTensor_dsigmoid(THCState *state, THCudaTensor *dst, THCudaTensor* src);



#endif
