package be.iminds.iot.dianne.jsonrpc;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Promise;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import be.iminds.iot.dianne.api.coordinator.Device;
import be.iminds.iot.dianne.api.coordinator.DianneCoordinator;
import be.iminds.iot.dianne.api.coordinator.EvaluationResult;
import be.iminds.iot.dianne.api.coordinator.Job;
import be.iminds.iot.dianne.api.coordinator.LearnResult;
import be.iminds.iot.dianne.api.coordinator.Notification;
import be.iminds.iot.dianne.api.coordinator.Status;
import be.iminds.iot.dianne.api.nn.eval.Evaluation;
import be.iminds.iot.dianne.api.nn.module.dto.NeuralNetworkDTO;
import be.iminds.iot.dianne.api.nn.platform.DiannePlatform;
import be.iminds.iot.dianne.nn.util.DianneJSONConverter;
import be.iminds.iot.dianne.tensor.Tensor;

@Component
public class DianneRequestHandler implements JSONRPCRequestHandler {

	private DianneCoordinator coordinator;
	private DiannePlatform platform;
	
	@Override
	public void handleRequest(JsonReader reader, JsonWriter writer) throws Exception {
		JsonParser parser = new JsonParser();
		JsonObject request = null; 
		try {
			request = parser.parse(reader).getAsJsonObject();
			handleRequest(request, writer);
		} catch(Exception e){
			writeError(writer, null, -32700, "Parse error");
			return;
		}
	}
	
	@Override
	public void handleRequest(JsonObject request, JsonWriter writer) throws Exception {
		String i = "null";
		if(request.has("id")){
			i = request.get("id").getAsString();
		}
		final String id = i;
		
		if(!request.has("jsonrpc")){
			writeError(writer, id, -32600, "Invalid JSONRPC request");
			return;
		}
		
		if(!request.get("jsonrpc").getAsString().equals("2.0")){
			writeError(writer, id, -32600, "Wrong JSONRPC version: "+request.get("jsonrpc").getAsString());
			return;
		}
		
		if(!request.has("method")){
			writeError(writer, id, -32600, "No method specified");
			return;
		}

		String method = request.get("method").getAsString();
		
		// TODO use a more generic approach here?
		switch(method){
		case "learn":
		case "eval":
			String nnName = null;
			NeuralNetworkDTO nn = null;
			String dataset;
			Map<String, String> config;
			
			try {
				JsonArray params = request.get("params").getAsJsonArray();
				if(params.get(0).isJsonPrimitive()){
					nnName = params.get(0).getAsString();
				} else {
					nn = DianneJSONConverter.parseJSON(params.get(0).getAsJsonObject());
				}
				dataset = params.get(1).getAsString();
				config = params.get(2).getAsJsonObject()
						.entrySet().stream().collect(Collectors.toMap( e -> e.getKey(), e -> e.getValue().getAsString()));

			} catch(Exception e){
				writeError(writer, id, -32602, "Incorrect parameters provided: "+e.getMessage());
				return;
			}
			
			// call coordinator
			if(method.equals("learn")){
				// learn
				Promise<LearnResult> result = null;
				if(nnName!=null){
					result= coordinator.learn(nnName, dataset, config);
				} else {
					result = coordinator.learn(nn, dataset, config);
				}
				result.then(p -> {
					writeResult(writer, id, p.getValue());
					return null;
				}, p -> {
					writeError(writer, id, -32603, "Error during learning: "+p.getFailure().getMessage());
				});
			} else {
				// eval
				Promise<EvaluationResult> result = null;
				if(nnName!=null){
					result= coordinator.eval(nnName, dataset, config);
				} else {
					result = coordinator.eval(nn, dataset, config);
				}
				result.then(p -> {
					writeResult(writer, id, p.getValue());
					return null;
				}, p -> {
					writeError(writer, id, -32603, "Error during learning: "+p.getFailure().getMessage());
				});
			}
			break;
		case "availableNeuralNetworks": 
			writeResult(writer, id, platform.getAvailableNeuralNetworks());
			break;
		case "availableDatasets":
			writeResult(writer, id, platform.getAvailableDatasets());
			break;
		case "queuedJobs":
			writeResult(writer, id, coordinator.queuedJobs());
			break;
		case "runningJobs":
			writeResult(writer, id, coordinator.runningJobs());
			break;
		case "finishedJobs":
			writeResult(writer, id, coordinator.finishedJobs());
			break;
		case "notifications":
			writeResult(writer, id, coordinator.getNotifications());
			break;	
		case "status":
			writeResult(writer, id, coordinator.getStatus());
			break;
		case "devices":
			writeResult(writer, id, coordinator.getDevices());
			break;	
		default:
			writeError(writer, id, -32601, "Method "+method+" not found");
		}
	}
	
	private void writeError(JsonWriter writer, String id, int code, String message) throws Exception {
		writer.beginObject();
		writer.name("jsonrpc");
		writer.value("2.0");
		writer.name("id");
		writer.value(id);
		writer.name("error");
		writer.beginObject();
		// error object
		writer.name("code");
		writer.value(code);
		writer.name("message");
		writer.value(message);
		writer.endObject();
		// end error object
		writer.endObject();
		writer.flush();					
	}
	
	private void writeResult(JsonWriter writer, String id, Object result) throws Exception{
		writer.beginObject();
		writer.name("jsonrpc");
		writer.value("2.0");
		writer.name("id");
		writer.value(id);
		writer.name("result");
		writer.beginArray();
		// write result object
		if(result instanceof List){
			for(Object o : (List) result){
				writeObject(writer, o);
			}
		} else {
			writeObject(writer, result);
		}
		// end result object
		writer.endArray();
		writer.endObject();
		writer.flush();			
	}
	
	public void writeObject(JsonWriter writer, Object o) throws Exception{
		// TODO object conversion would be handy here...
		if(o instanceof LearnResult){
			LearnResult learnResult = (LearnResult) o;
			writer.beginObject();
			writer.name("error");
			writer.value(learnResult.error);
			writer.name("iterations");
			writer.value(learnResult.iterations);
			writer.endObject();
		} else if(o instanceof EvaluationResult){
			EvaluationResult evalResult = (EvaluationResult) o;
			for(Evaluation eval : evalResult.evaluations.values()){
				writer.beginObject();
				writer.name("accuracy");
				writer.value(eval.accuracy());
				writer.name("forwardTime");
				writer.value(eval.forwardTime());
				writer.name("outputs");
				writer.beginArray();
				for(Tensor t : eval.getOutputs()){
					writer.beginArray();
					for(float f : t.get()){
						writer.value(f);
					}
					writer.endArray();
				}
				writer.endArray();
				writer.endObject();
			}
		} else if(o instanceof Job){
			Job job = (Job) o;
			writer.beginObject();
			writer.name("id");
			writer.value(job.id.toString());
			writer.name("name");
			writer.value(job.name);
			writer.name("type");
			writer.value(job.type);
			writer.name("nn");
			writer.value(job.nn);
			writer.name("dataset");
			writer.value(job.dataset);
			writer.endObject();
		} else if(o instanceof Notification) {
			Notification n = (Notification) o;
			writer.beginObject();
			writer.name("message");
			writer.value(n.message);
			writer.name("level");
			writer.value(n.level.toString().toLowerCase());
			writer.name("time");
			Date date = new Date(n.timestamp);
			SimpleDateFormat sdfDate = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
			writer.value(sdfDate.format(date));
			writer.endObject();
		} else if(o instanceof Status){
			writer.beginObject();
			Status s = (Status) o;
			writer.name("queued");
			writer.value(s.queued);
			writer.name("running");
			writer.value(s.running);
			writer.name("learn");
			writer.value(s.learn);
			writer.name("eval");
			writer.value(s.eval);
			writer.name("idle");
			writer.value(s.idle);
			writer.name("devices");
			writer.value(s.learn+s.eval+s.idle);
			writer.name("spaceLeft");
			float gb = s.spaceLeft/1000000000f;
			writer.value(String.format("%.1f", gb));
			writer.name("uptime");
			writer.value(getElapsedTime(s.bootTime));
			writer.endObject();
		} else if(o instanceof Device){ 
			Device d = (Device) o;
			writer.beginObject();
			writer.name("id");
			writer.value(d.id.toString());
			writer.name("name");
			writer.value(d.name);
			writer.name("arch");
			writer.value(d.arch);
			writer.name("os");
			writer.value(d.os);
			writer.name("ip");
			writer.value(d.ip);
			writer.name("learn");
			writer.value(d.learn);
			writer.name("eval");
			writer.value(d.eval);
			writer.name("cpuUsage");
			writer.value(d.cpuUsage);
			writer.name("memUsage");
			writer.value(d.memUsage);
			writer.endObject();
		}else {
			writer.value(o.toString());
		}
	}
	
	@Reference
	void setDianneCoordinator(DianneCoordinator c){
		this.coordinator = c;
	}

	@Reference
	void setDiannePlatform(DiannePlatform p){
		this.platform = p;
	}
	
	private String getElapsedTime(long since){
		long diff = System.currentTimeMillis()-since;
		
		long secondsInMilli = 1000;
		long minutesInMilli = secondsInMilli * 60;
		long hoursInMilli = minutesInMilli * 60;
		long daysInMilli = hoursInMilli * 24;

		long elapsedDays = diff / daysInMilli;
		diff = diff % daysInMilli;
		
		long elapsedHours = diff / hoursInMilli;
		diff = diff % hoursInMilli;
		
		long elapsedMinutes = diff / minutesInMilli;
		diff = diff % minutesInMilli;
		
		StringBuilder builder = new StringBuilder();
		builder.append(elapsedDays).append(" days, ")
			.append(elapsedHours).append(" hours and ")
			.append(elapsedMinutes).append(" minutes");
		return builder.toString();
	}
}