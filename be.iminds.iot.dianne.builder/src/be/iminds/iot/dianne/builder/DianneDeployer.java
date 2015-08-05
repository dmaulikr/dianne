package be.iminds.iot.dianne.builder;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import be.iminds.iot.dianne.api.nn.module.dto.ModuleDTO;
import be.iminds.iot.dianne.api.nn.module.dto.NeuralNetworkDTO;
import be.iminds.iot.dianne.api.nn.runtime.ModuleManager;
import be.iminds.iot.dianne.nn.util.DianneJSONConverter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

@Component(service = { javax.servlet.Servlet.class }, 
	property = { "alias:String=/dianne/deployer","aiolos.proxy=false" }, 
	immediate = true)
public class DianneDeployer extends HttpServlet {

	private Map<String, ModuleManager> runtimes = Collections.synchronizedMap(new HashMap<String, ModuleManager>());
	
	private Map<UUID, String> deployment = Collections.synchronizedMap(new HashMap<UUID, String>());
	
	@Reference(cardinality=ReferenceCardinality.AT_LEAST_ONE, 
			policy=ReferencePolicy.DYNAMIC)
	public void addModuleManager(ModuleManager m, Map<String, Object> properties){
		String uuid = (String) properties.get("aiolos.framework.uuid");
		if(uuid == null){
			uuid = "localhost";
		} else if(uuid.equals("00000000-0000-0000-0000-000000000000")){
			uuid = "Laptop";
		} else if(uuid.equals("00000000-0000-0000-0000-000000000001")){
			// some hard coded values for demo
			uuid = "Raspberry Pi";
		} else if(uuid.equals("00000000-0000-0000-0000-000000000002")){
			// some hard coded values for demo
			uuid = "Server";
		} else if(uuid.equals("00000000-0000-0000-0000-000000000003")){
			// some hard coded values for demo
			uuid = "Intel Edison";
		} else if(uuid.equals("00000000-0000-0000-0000-000000000004")){
			// some hard coded values for demo
			uuid = "nVidia Jetson";
		} else if(uuid.equals("00000000-0000-0000-0000-000000000005")){
			// some hard coded values for demo
			uuid = "GPU Server";
		} else {
			// shorten it a bit TODO use human readable name
			uuid = uuid.substring(uuid.lastIndexOf('-')+1);
		}
		runtimes.put(uuid, m);
	}
	
	public void removeModuleManager(ModuleManager m, Map<String, Object> properties){
		String uuid = (String) properties.get("aiolos.framework.uuid"); 
		if(uuid==null){
			uuid = "Laptop"; // TODO for now just fixed item for local runtime
		} else if(uuid.equals("00000000-0000-0000-0000-000000000001")){
			// some hard coded values for demo
			uuid = "Raspberry Pi";
		} else if(uuid.equals("00000000-0000-0000-0000-000000000002")){
			// some hard coded values for demo
			uuid = "Laptop";
		} else if(uuid.equals("00000000-0000-0000-0000-000000000003")){
			// some hard coded values for demo
			uuid = "Intel Edison";
		} else {
			// shorten it a bit TODO use human readable name
			uuid = uuid.substring(0, uuid.indexOf('-'));
		}
		runtimes.remove(uuid);
	}
	

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String action = request.getParameter("action");
		if(action.equals("deploy")){
			if(request.getParameter("modules")!=null){
				String modulesJsonString = request.getParameter("modules");
				String target = request.getParameter("target");
				if(target == null){
					target = "local"; // if no target specified, hard coded local target for now
				}
				NeuralNetworkDTO nn = DianneJSONConverter.parseJSON(modulesJsonString); 
					
				for(ModuleDTO module : nn.modules){
					deployModule(module, target);
				}
				// TODO only return deployment of deployed modules?
				returnDeployment(response.getWriter());
			} else if(request.getParameter("module")!=null){
				String moduleJsonString = request.getParameter("module");
				String target = request.getParameter("target");
				ModuleDTO module = DianneJSONConverter.parseModuleJSON(moduleJsonString); 
				deployModule(module, target);
				// TODO only return deployment of deployed modules?
				returnDeployment(response.getWriter());
			}
		} else if(action.equals("undeploy")){
			String id = request.getParameter("id");
			if(id!=null){
				undeployModule(id);
				response.getWriter().write(new JsonPrimitive(id).toString());
				response.getWriter().flush();
			}
		} else if(action.equals("targets")){
			JsonArray targets = new JsonArray();
			synchronized(runtimes){
				for(String id : runtimes.keySet()){
					targets.add(new JsonPrimitive(id));
				}
			}
			response.getWriter().write(targets.toString());
			response.getWriter().flush();
		}
		
	}
	
	private void deployModule(ModuleDTO module, String target){
		String migrateFrom = null;
		if(deployment.containsKey(module.id)){
			// already deployed... TODO exception or something?
			migrateFrom = deployment.get(module.id); 
			if(target.equals(migrateFrom)){
				return;
			}
		}
		
		try {
			ModuleManager runtime = runtimes.get(target);
			if(runtime!=null){
				runtime.deployModule(module);
				deployment.put(module.id, target);
			}
			
			// when migrating, undeploy module from previous
			if(migrateFrom!=null){
				runtime = runtimes.get(migrateFrom);
				if(runtime!=null){
					runtime.undeployModule(module.id);
				}
			}
		} catch (Exception e) {
			System.err.println("Failed to deploy module "+module.id);
			e.printStackTrace();
		}
	}
	
	private void undeployModule(String id){
		try {
			String target = deployment.remove(id);
			if(target!=null){
				ModuleManager runtime = runtimes.get(target);
				if(runtime!=null){
					runtime.undeployModule(UUID.fromString(id));
				}
			}
		} catch (Exception e) {
			System.err.println("Failed to deploy module "+id);
			e.printStackTrace();
		}
	}
	
	private void returnDeployment(Writer writer){
		JsonObject result = new JsonObject();
		synchronized(deployment){
			for(Iterator<Entry<UUID,String>> it = deployment.entrySet().iterator();it.hasNext();){
				Entry<UUID, String> e = it.next();
				result.add(e.getKey().toString(), new JsonPrimitive(e.getValue()));
			}
		}
		try {
			writer.write(result.toString());
			writer.flush();
		} catch(IOException e){}
	}
}
