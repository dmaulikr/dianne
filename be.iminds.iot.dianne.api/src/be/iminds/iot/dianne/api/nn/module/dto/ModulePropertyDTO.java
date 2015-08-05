package be.iminds.iot.dianne.api.nn.module.dto;

/**
 * A configuration property for a Module.
 * 
 * Consists of a human-readable name, and id used as property key,
 * and the name of the expected class type.
 * 
 * @author tverbele
 *
 */
public class ModulePropertyDTO {

	// human-readable name for this property
	public final String name;
	
	// id to be used as property key in an actual configuration
	public final String id;
	
	// clazz that is expected as property value
	public final String clazz;
	
	public ModulePropertyDTO(String name, String id, String clazz){
		this.name = name;
		this.id = id;
		this.clazz = clazz;
	}
}
