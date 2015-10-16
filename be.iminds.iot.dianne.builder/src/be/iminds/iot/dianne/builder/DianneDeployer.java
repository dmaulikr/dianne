package be.iminds.iot.dianne.builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import be.iminds.iot.dianne.api.nn.module.dto.ModuleDTO;
import be.iminds.iot.dianne.api.nn.module.dto.ModuleInstanceDTO;
import be.iminds.iot.dianne.api.nn.module.dto.NeuralNetworkDTO;
import be.iminds.iot.dianne.api.nn.module.dto.NeuralNetworkInstanceDTO;
import be.iminds.iot.dianne.api.nn.platform.DiannePlatform;
import be.iminds.iot.dianne.api.repository.DianneRepository;
import be.iminds.iot.dianne.nn.util.DianneJSONConverter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;


@Component(service = { javax.servlet.Servlet.class }, 
	property = { "alias:String=/dianne/deployer",
				 "osgi.http.whiteboard.servlet.pattern=/dianne/deployer",
				 "aiolos.proxy=false" }, 
	immediate = true)
public class DianneDeployer extends HttpServlet {

	private DianneRepository repository;
	private DiannePlatform platform;
	
	@Activate
	public void activate(BundleContext context){
	}
	
	@Reference
	void setDianne(DiannePlatform d){
		platform = d;
	}

	@Reference
	void setDianneRepository(DianneRepository repo){
		this.repository = repo;
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String action = request.getParameter("action");
		if(action.equals("deploy")){
			String id = request.getParameter("id");
			if(id==null){
				id = UUID.randomUUID().toString();
			}
			String name = request.getParameter("name");
			if(name==null){
				name = "unknown";
			}

			List<ModuleDTO> toDeploy = new ArrayList<ModuleDTO>();
			if(request.getParameter("modules")!=null){
				String modulesJsonString = request.getParameter("modules");
				NeuralNetworkDTO nn = DianneJSONConverter.parseJSON(modulesJsonString); 
				toDeploy.addAll(nn.modules);
			} else if(request.getParameter("module")!=null){
				String moduleJsonString = request.getParameter("module");
				ModuleDTO module = DianneJSONConverter.parseModuleJSON(moduleJsonString);
				toDeploy.add(module);
			}
				
			String target = request.getParameter("target");
			UUID runtimeId = UUID.fromString(target);
			
			try {
				List<ModuleInstanceDTO> moduleInstances = platform.deployModules(UUID.fromString(id), name, toDeploy, runtimeId);

				// return json object with deployment
				JsonObject result = new JsonObject();
				JsonObject deployment = deploymentToJSON(moduleInstances);
				result.add("id", new JsonPrimitive(id));
				result.add("deployment", deployment);
				try {
					response.getWriter().write(result.toString());
					response.getWriter().flush();
				} catch(IOException e){}
			
			} catch(InstantiationException e){
				System.out.println("Failed to deploy modules: "+e.getMessage());
			}
				
		} else if(action.equals("undeploy")){
			String id = request.getParameter("id");
			String moduleId = request.getParameter("moduleId");
			if(id!=null){
				NeuralNetworkInstanceDTO nn = platform.getNeuralNetworkInstance(UUID.fromString(id));
				if(nn!=null){
					ModuleInstanceDTO moduleInstance = nn.modules.get(UUID.fromString(moduleId));
					platform.undeployModules(Collections.singletonList(moduleInstance));
					
					response.getWriter().write(new JsonPrimitive(id).toString());
					response.getWriter().flush();
				}
			}
		} else if(action.equals("targets")){
			JsonArray targets = new JsonArray();
			
			Map<UUID, String> runtimes = platform.getRuntimes();
			runtimes.entrySet().stream()
				.forEach(e -> {
					JsonObject o = new JsonObject();
					o.add("id", new JsonPrimitive(e.getKey().toString()));
					o.add("name", new JsonPrimitive(e.getValue()));
					targets.add(o);
				});
			
			response.getWriter().write(targets.toString());
			response.getWriter().flush();
		} else if("recover".equals(action)){
			String id = request.getParameter("id");
			if(id==null){
				// list all options
				JsonArray nns = new JsonArray();
				platform.getNeuralNetworkInstances().stream().forEach(
						nni -> { JsonObject o = new JsonObject();
								o.add("id", new JsonPrimitive(nni.id.toString()));
								o.add("name", new JsonPrimitive(nni.name));
								if(nni.description!=null){
									o.add("description", new JsonPrimitive(nni.description));
								}
								nns.add(o);
						});
				response.getWriter().write(nns.toString());
				response.getWriter().flush();
				
			} else {
				NeuralNetworkInstanceDTO nni = platform.getNeuralNetworkInstance(UUID.fromString(id));
				if(nni==null){
					System.out.println("Failed to recover "+nni.id+" , instance not found");
					return;
				}
				
				try {
					response.getWriter().write("{\"nn\":");
					NeuralNetworkDTO nn = repository.loadNeuralNetwork(nni.name);
					String s = DianneJSONConverter.toJsonString(nn); 
					response.getWriter().write(s);
					response.getWriter().write(", \"layout\":");
					String layout = repository.loadLayout(nni.name);
					response.getWriter().write(layout);
					response.getWriter().write(", \"deployment\":");
					JsonObject deployment = deploymentToJSON(nni.modules.values());
					response.getWriter().write(deployment.toString());
					response.getWriter().write(", \"id\":");
					response.getWriter().write("\""+id+"\"");
					response.getWriter().write("}");
					response.getWriter().flush();
				} catch(Exception e){
					System.out.println("Failed to recover "+nni.name+" "+nni.id);
				}
			}
		}
		
	}
	
	private JsonObject deploymentToJSON(Collection<ModuleInstanceDTO> moduleInstances){
		JsonObject deployment = new JsonObject();
		for(ModuleInstanceDTO moduleInstance : moduleInstances){
			String runtime = moduleInstance.runtimeId.toString();
			if(runtime==null){
				runtime = moduleInstance.runtimeId.toString();
			}
			deployment.add(moduleInstance.moduleId.toString(), new JsonPrimitive(runtime));
		}
		return deployment;
	}
}
