package be.iminds.iot.dianne.api.nn.module;

import java.util.UUID;

import be.iminds.iot.dianne.tensor.Tensor;

/**
 * Called when a Module has done a forward pass. 
 * 
 * This interface can be registered as an OSGi service with the service property
 *  targets = String[]{nnId:moduleId, nnId:moduleId2}
 * in order to listen to only specific modules from certain neural network instances.
 * 
 * In case only a nnId is given as targets property, this forward listener will only 
 * listen to the Output modules of this neural network instance.
 *   
 * @author tverbele
 *
 */
public interface ForwardListener {

	/**
	 * Called when a Module has performed a forward pass
	 * @param moduleId the moduleId of the module whos forward was called
	 * @param output a copy of the output data
	 * @param tags a copy of the tags provided
	 */
	void onForward(final UUID moduleId, final Tensor output, final String... tags);
	
	void onError(final UUID moduleId, final ModuleException e, final String...tags);
}
