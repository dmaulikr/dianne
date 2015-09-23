package be.iminds.iot.dianne.api.nn.module.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Represents an instance of a neural network.
 * 
 * Contains the UUID of the nn instance, the name of the neural network,
 * and the list of ModuleInstances that this nn is composed of.
 * 
 * @author tverbele
 *
 */
public class NeuralNetworkInstanceDTO {

	// UUID of the neural network instance
	public final UUID id;
	
	// human readable neural network description
	public final String description;
	
	// name of the neural network
	// can be used to fetch the NeuralNetworkDTO from DianneRepository
	public final String name;
	
	// The list of ModuleInstances that this neural network instance is composed of
	public final Map<UUID, ModuleInstanceDTO> modules;
	
	public NeuralNetworkInstanceDTO(UUID id, String name, Map<UUID, ModuleInstanceDTO> modules){
		this(id, name, null, modules);
	}
	
	public NeuralNetworkInstanceDTO(UUID id, String name, String description, Map<UUID, ModuleInstanceDTO> modules){
		this.id = id;
		this.name = name;
		this.description = description;
		this.modules = modules;
	}
	
	@Override
	public boolean equals(Object o){
		if(!(o instanceof NeuralNetworkInstanceDTO)){
			return false;
		}
		
		NeuralNetworkInstanceDTO other = (NeuralNetworkInstanceDTO) o;
		return other.id.equals(id);
	}
	
	@Override
	public int hashCode(){
		return id.hashCode();
	}
}
