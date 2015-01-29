package be.iminds.iot.dianne.builder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpService;

import be.iminds.iot.dianne.nn.module.description.ModuleDescription;
import be.iminds.iot.dianne.nn.module.description.ModuleProperty;
import be.iminds.iot.dianne.nn.module.factory.ModuleFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

@Component(service={javax.servlet.Servlet.class},
	property={"alias:String=/dianne/builder"},
	immediate=true)
public class DianneBuilder extends HttpServlet {

	private List<ModuleFactory> factories = Collections.synchronizedList(new ArrayList<ModuleFactory>());
	
	@Reference(cardinality=ReferenceCardinality.AT_LEAST_ONE, 
			policy=ReferencePolicy.DYNAMIC)
	public void addModuleFactory(ModuleFactory factory){
		factories.add(factory);
	}
	
	public void removeModuleFactory(ModuleFactory factory){
		factories.remove(factory);
	}
	
	@Reference
	public void setHttpService(HttpService http){
		try {
			// Use manual registration - problems with whiteboard
			http.registerResources("/dianne", "res", null);
			http.registerServlet("/dianne/builder", this, null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String action = request.getParameter("action");

		if(action.equals("available-modules")){
			getAvailableModules(response.getWriter());
		} else if(action.equals("module-properties")){
			String type = request.getParameter("type");
			getModuleProperties(type, response.getWriter());
		} 
	}
	
	private void getAvailableModules(PrintWriter writer){
		List<ModuleDescription> modules = new ArrayList<ModuleDescription>();
		synchronized(factories){
			for(ModuleFactory f : factories){
				modules.addAll(f.getAvailableModules());
			}
		}
		
		JsonArray jsonModules = new JsonArray();
		for(ModuleDescription module : modules){
			jsonModules.add(new JsonPrimitive(module.getName()));
		}
		writer.write(jsonModules.toString());
		writer.flush();
	}
	
	private void getModuleProperties(String type, PrintWriter writer){
		ModuleDescription module = null;
		synchronized(factories){
			for(ModuleFactory f : factories){
				module = f.getModuleDescription(type);
				if(module!=null)
					break;
			}
		}
		
		if(module==null){
			// return;
		}
		
		JsonArray jsonProperties = new JsonArray();
		for(ModuleProperty p : module.getProperties()){
			JsonObject jsonProperty = new JsonObject();
			jsonProperty.addProperty("id", p.getId());
			jsonProperty.addProperty("name", p.getName());
			
			jsonProperties.add(jsonProperty);
		}
		
		writer.write(jsonProperties.toString());
		writer.flush();
	}
}
