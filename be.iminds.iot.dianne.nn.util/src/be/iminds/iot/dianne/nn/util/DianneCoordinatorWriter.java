package be.iminds.iot.dianne.nn.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.stream.JsonWriter;

import be.iminds.iot.dianne.api.coordinator.AgentResult;
import be.iminds.iot.dianne.api.coordinator.EvaluationResult;
import be.iminds.iot.dianne.api.coordinator.Job;
import be.iminds.iot.dianne.api.coordinator.LearnResult;
import be.iminds.iot.dianne.api.nn.eval.Evaluation;
import be.iminds.iot.dianne.api.nn.eval.EvaluationProgress;
import be.iminds.iot.dianne.api.nn.learn.LearnProgress;
import be.iminds.iot.dianne.api.rl.agent.AgentProgress;
import be.iminds.iot.dianne.tensor.Tensor;

/**
 * Helper class for writing Jobs and their results to JSONWriter... used in coordinator and jsonrpc
 * 
 * @author tverbele
 *
 */
public class DianneCoordinatorWriter {

	public static void writeObject(JsonWriter writer, Object o) throws Exception {
		if(o==null){
			writer.value("null");
		} else if(o instanceof LearnResult){
			writeLearnResult(writer, (LearnResult) o);
		} else if(o instanceof EvaluationResult){
			writeEvaluationResult(writer, (EvaluationResult) o);
		} else if(o instanceof AgentResult){
			writeAgentResult(writer, (AgentResult) o);
		} else if(o instanceof List){
			List l = (List)o;
			writer.beginArray();
			for(Object ll : l){
				writeObject(writer, ll);
			}
			writer.endArray();
		} else if(o instanceof Map){
			Map m = (Map) o;
			writer.beginObject();
			for(Object k : m.keySet()){
				writer.name(k.toString());
				writeObject(writer, m.get(k));
			}
			writer.endObject();
		} else if(o.getClass().equals(String.class)
				|| o.getClass().isEnum()
				|| o.getClass().equals(UUID.class)){
			writer.value(o.toString());
		} else {
			writer.beginObject();
			for(Field f : o.getClass().getFields()){
				if(Modifier.isPublic(f.getModifiers())){
					writer.name(f.getName());
					if(f.getType().isPrimitive()){ 
						switch(f.getType().getName()){
						case "long":
							writer.value(f.getLong(o));
							break;
						case "int":
							writer.value(f.getInt(o));
							break;
						case "float":
							writer.value(f.getFloat(o));
							break;
						case "double":
							writer.value(f.getDouble(o));
							break;
						case "boolean":
							writer.value(f.getBoolean(o));
							break;
						case "short": 
							writer.value(f.getShort(o));
							break;
						case "byte":
							writer.value(f.getByte(o));
							break;
						}
					} else {
						writeObject(writer, f.get(o));
					}
				}
			}
			writer.endObject();
		}
	}
	
	public static void writeJob(JsonWriter writer, Job job) throws Exception {
		// job is just an ordinary object
		writeObject(writer, job);
	}
	
	public static void writeLearnResult(JsonWriter writer, LearnResult result) throws Exception {
		writer.beginArray();
		for(LearnProgress p : result.progress){
			writer.beginObject();
			writer.name("error");
			writer.value(new Float(p.error));
			writer.name("iteration");
			writer.value(p.iteration);
			writer.endObject();		
		}
		writer.endArray();
	}
	
	public static void writeEvaluationResult(JsonWriter writer, EvaluationResult result) throws Exception {
		writer.beginArray();
		for(Evaluation eval : result.evaluations.values()){
			writer.beginObject();
			if(eval==null){
				// write nothing?
			} else if(eval instanceof EvaluationProgress){
				writer.name("processed");
				writer.value(((EvaluationProgress) eval).getProcessed());
				writer.name("total");
				writer.value(((EvaluationProgress) eval).getTotal());
			} else {
				writer.name("accuracy");
				writer.value(new Float(eval.accuracy()));
				writer.name("forwardTime");
				writer.value(eval.forwardTime());
				// write confusion matrix
				writer.name("confusionMatrix");
				writer.beginArray();
				Tensor confusionMatrix = eval.getConfusionMatix();
				for(int i=0;i<confusionMatrix.size(0);i++){
					writer.beginArray();
					for(int j=0;j<confusionMatrix.size(1);j++){
						writer.value(new Float(confusionMatrix.get(i, j)));
					}
					writer.endArray();
				}
				writer.endArray();
				// write all outputs
				if(eval.getOutputs()!=null){
					writer.name("outputs");
					writer.beginArray();
					for(Tensor t : eval.getOutputs()){
						writer.beginArray();
						for(float f : t.get()){
							writer.value(new Float(f));
						}
						writer.endArray();
					}
					writer.endArray();
				}
			} 
			writer.endObject();	
		}
		writer.endArray();
	}
	
	public static void writeAgentResult(JsonWriter writer, AgentResult result) throws Exception {
		writer.beginArray();
		for(AgentProgress p : result.results.values()){
			writer.beginObject();
			writer.name("samples");
			writer.value(p.samples);
			writer.endObject();		
		}
		writer.endArray();
	}
	
}
