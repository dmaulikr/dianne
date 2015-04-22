package be.iminds.iot.dianne.api.nn.module.description;

import java.util.Collections;
import java.util.List;

public class ModuleType {

	private final String type;
	private final String category;
	private final List<ModuleProperty> properties;
	private final boolean trainable;
	
	public ModuleType(String type, String category, List<ModuleProperty> properties, boolean trainable){
		this.type = type;
		this.category = category;
		this.properties = Collections.unmodifiableList(properties);
		this.trainable = trainable;
	}

	public String getType() {
		return type;
	}
	
	public String getCategory(){
		return category;
	}

	public List<ModuleProperty> getProperties() {
		return properties;
	}
	
	public boolean isTrainable(){
		return trainable;
	}
	
}
