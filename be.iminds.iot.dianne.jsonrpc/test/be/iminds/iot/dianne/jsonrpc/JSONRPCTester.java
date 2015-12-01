package be.iminds.iot.dianne.jsonrpc;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import be.iminds.iot.dianne.api.nn.module.dto.NeuralNetworkDTO;
import be.iminds.iot.dianne.nn.util.DianneJSONRPCRequestFactory;
import be.iminds.iot.dianne.nn.util.DianneNeuralNetworkBuilder;
import be.iminds.iot.dianne.nn.util.DianneNeuralNetworkBuilder.Activation;

public class JSONRPCTester {

	public static void main(String[] args) throws Exception {
		
		Socket s = new Socket("127.0.0.1", 9090);
		
		OutputStream out = s.getOutputStream();
		out.flush();
		JsonReader reader = new JsonReader(new InputStreamReader(s.getInputStream()));
		JsonParser parser = new JsonParser();
		
		// learn
		//byte[] learnRequest = Files.readAllBytes(FileSystems.getDefault().getPath("test", "learnRequest"));
		NeuralNetworkDTO nn = DianneNeuralNetworkBuilder.createMLP("Test", 784, 10, Activation.Sigmoid, 20);
		String dataset = "MNIST";
		Map<String, String> learnProperties = new HashMap<>();
		learnProperties.put("clean", "true");
		learnProperties.put("trace", "true");
		learnProperties.put("tag", "test");
		learnProperties.put("learningRate", "0.01");
		learnProperties.put("batchSize", "10");
		learnProperties.put("momentum", "0.9");
		learnProperties.put("regularization", "0.0001");
		learnProperties.put("trainingSet", "0:60000");
		learnProperties.put("maxIterations", "10000");
		JsonObject learnRequestJson = DianneJSONRPCRequestFactory.createLearnRequest(1, nn, dataset, learnProperties);
		byte[] learnRequest = learnRequestJson.toString().getBytes();
		out.write(learnRequest);
		
		JsonElement learnResult = parser.parse(reader);
		System.out.println(learnResult.toString());
		
		// eval
		//byte[] evalRequest = Files.readAllBytes(FileSystems.getDefault().getPath("test", "evalRequest"));
		Map<String, String> evalProperties = new HashMap<>();
		evalProperties.put("clean", "true");
		evalProperties.put("tag", "test");	
		evalProperties.put("testSet", "60000:70000");
		JsonObject evalRequestJson = DianneJSONRPCRequestFactory.createEvalRequest(2, nn.name, dataset, evalProperties);
		byte[] evalRequest = evalRequestJson.toString().getBytes();
		out.write(evalRequest);
		
		JsonElement evalResult = parser.parse(reader);
		String result = evalResult.toString();
		if(result.length()>1000){
			result = result.substring(0, 1000)+"...";
		}
		System.out.println(result);
	}
}