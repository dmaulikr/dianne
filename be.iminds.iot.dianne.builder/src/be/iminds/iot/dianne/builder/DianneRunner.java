package be.iminds.iot.dianne.builder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import be.iminds.iot.dianne.nn.module.ForwardListener;
import be.iminds.iot.dianne.nn.module.Input;
import be.iminds.iot.dianne.nn.module.Module;
import be.iminds.iot.dianne.nn.module.Module.Mode;
import be.iminds.iot.dianne.nn.module.Output;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

@Component(service = { javax.servlet.Servlet.class }, 
	property = { "alias:String=/dianne/run","aiolos.proxy=false" }, 
	immediate = true)
public class DianneRunner extends HttpServlet {
	
	private TensorFactory factory;
	
	private JsonParser parser = new JsonParser();
	
	// for now fixed 1 input, 1 output, and trainable modules
	private Map<String, Module> modules = new HashMap<String, Module>();

	private AsyncContext sse = null;
	
	@Reference
	public void setTensorFactory(TensorFactory factory){
		this.factory = factory;
	}
	
	@Reference(cardinality=ReferenceCardinality.MULTIPLE, 
			policy=ReferencePolicy.DYNAMIC)
	public void addModule(Module m){
		this.modules.put(m.getId().toString(), m);
		if(m instanceof Output){
			final Output output = (Output) m;
			final String id  = m.getId().toString();
			output.addForwardListener(new ForwardListener() {
				@Override
				public void onForward(Tensor t) {
					if(sse!=null){
						try {
							JsonObject data = new JsonObject();
							
							
							// format output as [['label', val],['label2',val2],...] for in highcharts
							JsonArray result = new JsonArray();
							String[] labels;
							float[] values;
							if(t.size()>10){
								// if more than 10 outputs, only send top-10 results
								Integer[] indices = new Integer[t.size()];
								for(int i=0;i<t.size();i++){
									indices[i] = i;
								}
								Arrays.sort(indices, new Comparator<Integer>() {
									@Override
									public int compare(Integer o1, Integer o2) {
										float v1 = t.get(o1);
										float v2 = t.get(o2);
										// inverse order to have large->small order
										return v1 > v2 ? -1 : (v1 < v2 ? 1 : 0);
									}
								});
								labels = new String[10];
								values = new float[10];
								for(int i=0;i<10;i++){
									labels[i] = output.getOutputLabels()[indices[i]];
									values[i] = t.get(indices[i]);
								}
							} else {
								labels = output.getOutputLabels();
								values = t.get();
							}
							for(int i=0;i<t.size();i++){
								JsonArray item = new JsonArray();
								item.add(new JsonPrimitive(labels[i]));
								item.add(new JsonPrimitive(values[i]));
								result.add(item);
							}
														
							data.add("output", new JsonPrimitive(result.toString()));
							data.add("id", new JsonPrimitive(id));
							
							StringBuilder builder = new StringBuilder();
							builder.append("data: ").append(data.toString()).append("\n\n");
			
							PrintWriter writer = sse.getResponse().getWriter();
							writer.write(builder.toString());
							writer.flush();
						} catch(Exception e){}
					}
				}
			});
		}
	}
	
	public void removeModule(Module m){
		Iterator<Entry<String, Module>> it = modules.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, Module> e = it.next();
			if(e.getValue()==m){
				it.remove();
			}
		}
	}
		
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// write text/eventstream response
		response.setContentType("text/event-stream");
		response.setHeader("Cache-Control", "no-cache");
		response.setCharacterEncoding("UTF-8");
		response.addHeader("Connection", "keep-alive");
		
		sse = request.startAsync();
		sse.setTimeout(0); // no timeout => remove listener when error occurs.
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if(request.getParameter("forward")!=null){
			String inputId = request.getParameter("input");
			Input input = (Input) modules.get(inputId);
			
			JsonObject sample = parser.parse(request.getParameter("forward")).getAsJsonObject();
			int channels = sample.get("channels").getAsInt();
			int width = sample.get("width").getAsInt();
			int height = sample.get("height").getAsInt();

			float[] data = parseInput(sample.get("data").getAsJsonArray().toString());
			Tensor t = factory.createTensor(data, channels, height, width);
			input.input(t);
		} else if(request.getParameter("mode")!=null){
			String mode = request.getParameter("mode");
			String targetId = request.getParameter("target");

			Module m = modules.get(targetId);
			if(m!=null){
				m.setMode(Mode.valueOf(mode));
			}
		}
	}
	
	private float[] parseInput(String string){
		String[] strings = string.replace("[", "").replace("]", "").split(",");
		float result[] = new float[strings.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = Float.parseFloat(strings[i]);
		}
		return result;
	}
}
