package be.iminds.iot.dianne.api.repository;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import be.iminds.iot.dianne.api.nn.module.dto.NeuralNetworkDTO;

public interface DianneRepository {

	List<String> avialableNeuralNetworks();
	
	NeuralNetworkDTO loadNeuralNetwork(String network) throws IOException;
	
	void storeNeuralNetwork(String network, String modules);
	
	String loadLayout(String network) throws IOException;
	
	void storeLayout(String network, String layout);

	float[] loadWeights(UUID id) throws IOException;
	
	void storeWeights(UUID id, float[] weights);
}
