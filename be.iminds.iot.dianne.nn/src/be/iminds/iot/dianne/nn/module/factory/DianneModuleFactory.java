package be.iminds.iot.dianne.nn.module.factory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import be.iminds.iot.dianne.nn.module.AbstractModule;
import be.iminds.iot.dianne.nn.module.Module;
import be.iminds.iot.dianne.nn.module.activation.Sigmoid;
import be.iminds.iot.dianne.nn.module.activation.Tanh;
import be.iminds.iot.dianne.nn.module.description.ModuleDescription;
import be.iminds.iot.dianne.nn.module.description.ModuleProperty;
import be.iminds.iot.dianne.nn.module.io.InputImpl;
import be.iminds.iot.dianne.nn.module.io.OutputImpl;
import be.iminds.iot.dianne.nn.module.layer.Linear;
import be.iminds.iot.dianne.tensor.TensorFactory;

@Component(property={"aiolos.export=false"})
public class DianneModuleFactory implements ModuleFactory {

	private ExecutorService executor = Executors.newCachedThreadPool();
	
	private final Map<String, ModuleDescription> supportedModules = new HashMap<String, ModuleDescription>();
	
	@Activate
	public void activate(){
		// build list of supported modules
		// TODO use reflection for this?
		
		{
			List<ModuleProperty> properties = new ArrayList<ModuleProperty>();
			properties.add(new ModuleProperty("Input size", "input"));
			properties.add(new ModuleProperty("Output size", "output"));
			ModuleDescription description = new ModuleDescription("Linear", properties);
			supportedModules.put(description.getName(), description);
		}
		{
			List<ModuleProperty> properties = new ArrayList<ModuleProperty>();
			ModuleDescription description = new ModuleDescription("Sigmoid", properties);
			supportedModules.put(description.getName(), description);
		}
		{
			List<ModuleProperty> properties = new ArrayList<ModuleProperty>();
			ModuleDescription description = new ModuleDescription("Tanh", properties);
			supportedModules.put(description.getName(), description);
		}
		{
			List<ModuleProperty> properties = new ArrayList<ModuleProperty>();
			ModuleDescription description = new ModuleDescription("Input", properties);
			supportedModules.put(description.getName(), description);
		}
		{
			List<ModuleProperty> properties = new ArrayList<ModuleProperty>();
			ModuleDescription description = new ModuleDescription("Output", properties);
			supportedModules.put(description.getName(), description);
		}
	}
	
	@Override
	public Module createModule(TensorFactory factory, Dictionary<String, ?> config)
			throws InstantiationException {
		AbstractModule module = null;
		
		// TODO use reflection for this?
		// for now just hard code an if/else for each known module
		String type = (String)config.get("module.type");
		UUID id = UUID.fromString((String)config.get("module.id"));
		
		if(type.equals("Linear")){
			int input = Integer.parseInt((String)config.get("module.linear.input"));
			int output = Integer.parseInt((String)config.get("module.linear.output"));
			
			module = new Linear(factory, id, input, output);
		} else if(type.equals("Tanh")){
			module = new Tanh(factory, id); 
		} else if(type.equals("Sigmoid")){
			module = new Sigmoid(factory, id);
		} else if(type.equals("Input")){
			module = new InputImpl(factory, id); 
		} else if(type.equals("Output")){
			module = new OutputImpl(factory, id);
		} else if(type.equals("anotherTypeHere")){
			// instantiate other types ... 
		}
		
		if(module==null){
			throw new InstantiationException("Could not instantiate module of type "+type);
		}
		
		// re-use a cached threadpool
		module.setExecutorService(executor);
		
		return module;
	}

	@Override
	public List<ModuleDescription> getAvailableModules() {
		return new ArrayList<ModuleDescription>(supportedModules.values());
	}

	@Override
	public ModuleDescription getModuleDescription(String name) {
		return supportedModules.get(name);
	}
	
}
