package be.iminds.iot.dianne.api.repository;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import be.iminds.iot.dianne.api.nn.module.dto.NeuralNetworkDTO;
import be.iminds.iot.dianne.tensor.Tensor;

/**
 * The DianneRepository offers access to known neural networks and their stored parameters.
 * 
 * @author tverbele
 *
 */
public interface DianneRepository {

	/**
	 * Get a list of available neural networks
	 * @return list of available neural networks
	 */
	List<String> availableNeuralNetworks();
	
	/**
	 * Get a detailed description of a neural network
	 * @param nnName the name of the neural network
	 * @return the NeuralNetworkDTO representing this neural network
	 * @throws IOException 
	 */
	NeuralNetworkDTO loadNeuralNetwork(String nnName) ;
	
	/**
	 * Store a new neural network
	 * @param nn the NeuralNetworkDTO representing the neural network
	 */
	void storeNeuralNetwork(NeuralNetworkDTO nn);
	
	/**
	 * Load the parameters for a given moduleId, optionally with some tags
	 * 
	 * @param moduleId moduleId for which the parameters to load
	 * @param tag optional tags for the parameters
	 * @return the parameter Tensor
	 * @throws IOException
	 */
	Tensor loadParameters(UUID moduleId, String... tag);
	
	/**
	 * Load the parameters for a number of moduleIds, optionally with some tags
	 * 
	 * @param moduleIds moduleIdds for which the parameters to load
	 * @param tag optional tags for the parameters
	 * @return the parameters Tensors mapped by moduleId
	 * @throws IOException
	 */
	Map<UUID, Tensor> loadParameters(Collection<UUID> moduleIds, String... tag);
	
	/**
	 * Load all parameters for a given neural network name for some tags
	 * @param nnName name of the neural network
	 * @param tag optional tags for the parameters
	 * @return the parameters Tensor mapped by moduleId
	 */
	Map<UUID, Tensor> loadParameters(String nnName, String... tag);
	
	/**
	 * Store parameters for a given moduleId
	 *
	 * @param moduleId the moduleId for which these parameters are applicable
	 * @param parameters the parameters Tensor
	 * @param tag optional tags for the parameters
	 */
	void storeParameters(UUID moduleId, Tensor parameters, String... tag);
	
	/**
	 * Update the parameters for a given moduleId with this diff
	 * 
	 * @param moduleId the moduleId for which these parameters are applicable
	 * @param accParameters a diff with the old parameters
	 * @param tag optional tags for the parameters
	 */
	void accParameters(UUID moduleId, Tensor accParameters, String... tag);
	
	/**
	 * Store parameters for a number of modules
	 *
	 * @param parameters the parameters Tensors mapped by moduleIds
	 * @param tag optional tags for the parameters
	 */
	void storeParameters(Map<UUID, Tensor> parameters, String... tag);
	 
	/**
	 * Update the parameters for a number of modules with this diff
	 * 
	 * @param accParameters a diff with the old parameters mapped by moduleId
	 * @param tag optional tags for the parameters
	 */
	void accParameters(Map<UUID, Tensor> accParameters, String... tag);
	

	// these are some helper methods for saving the jsplumb layout of the UI builder
	// of utterly no importance for the rest and can be ignored...
	String loadLayout(String nnName) throws IOException;
	
	void storeLayout(String nnName, String layout);
}
