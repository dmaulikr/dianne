package be.iminds.iot.dianne.rl.agent.strategy;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import be.iminds.iot.dianne.api.log.DataLogger;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorFactory;

@Component(property={"strategy=boltzmann"})
public class BoltzmannStrategy implements ActionStrategy {
	
	private TensorFactory factory;

	private double temperatureMax = 1e0;
	private double temperatureMin = 1e0;
	private double temperatureDecay = 1e-6;
	
	private DataLogger logger = null;
	private String[] loglabels = new String[]{"Q0", "Q1", "Q2", "temperature"};
	
	@Reference
	public void setTensorFactory(TensorFactory f){
		this.factory =f;
	}
	
	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	public void setDataLogger(DataLogger l){
		this.logger = l;
		this.logger.setAlpha("temperature", 1f);
		this.logger.setAlpha("Q0", 1f);
		this.logger.setAlpha("Q1", 1f);
		this.logger.setAlpha("Q2", 1f);
	}
	
	@Override
	public Tensor selectActionFromOutput(Tensor output, long i) {
		
		Tensor action = factory.createTensor(output.size());
		action.fill(-1);
		
		double temperature = temperatureMin + (temperatureMax - temperatureMin) * Math.exp(-i * temperatureDecay);
		
		if(logger!=null){
			logger.log("AGENT", loglabels, output.get(0), output.get(1), output.get(2), (float) temperature);
		}
		
		factory.getTensorMath().div(output, output, (float) temperature);
		factory.getTensorMath().softmax(output, output);
		
		double s = 0, r = Math.random();
		int a = 0;
		
		while((s += output.get(a)) < r)
			a++;
		
		action.set(1, a);
		
		return action;
	}

	@Override
	public void configure(Map<String, String> config) {
		if (config.containsKey("temperatureMax"))
			temperatureMax = Double.parseDouble(config.get("temperatureMax"));
		
		if (config.containsKey("temperatureMin"))
			temperatureMin = Double.parseDouble(config.get("temperatureMin"));
		
		if (config.containsKey("temperatureDecay"))
			temperatureDecay = Double.parseDouble(config.get("temperatureDecay"));
		
		System.out.println("Boltzmann Action Selection");
		System.out.println("* temperature max = "+temperatureMax);
		System.out.println("* temperature min = "+temperatureMin);
		System.out.println("* temperature decay = "+temperatureDecay);
		System.out.println("---");
	}

}
