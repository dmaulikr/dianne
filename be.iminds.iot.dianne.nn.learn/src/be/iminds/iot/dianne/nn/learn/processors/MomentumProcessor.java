package be.iminds.iot.dianne.nn.learn.processors;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import be.iminds.iot.dianne.api.nn.learn.Processor;
import be.iminds.iot.dianne.tensor.Tensor;

/**
 * Additional learning techniques like Momentum can be implemented as a Processor decorator
 */
public class MomentumProcessor extends AbstractProcessor {

	private final Processor decorated;
	
	private float momentum = 0.9f;
	
	private Map<UUID, Tensor> previousDelta = new HashMap<UUID, Tensor>();
	
	public MomentumProcessor( AbstractProcessor p ) {
		super(p.factory, p.input, p.output, p.toTrain, p.dataset, p.config, p.logger);
		decorated = p;
		
		String m = config.get("momentum");
		if(m!=null){
			momentum = Float.parseFloat(m);
		}
		
		System.out.println("Momentum");
		System.out.println("* rate = "+momentum);
		System.out.println("---");
		
	}
	
	@Override
	public float processNext() {
		float error = decorated.processNext();
		
		// add momentum
		toTrain.entrySet().stream().forEach(e -> {
			Tensor prev = previousDelta.get(e.getKey());
			if(prev!=null){
				Tensor deltaParams = e.getValue().getDeltaParameters();
				factory.getTensorMath().add(deltaParams, deltaParams , momentum, prev);
			}
		});
		
		// copy to previousGrad
		toTrain.entrySet().stream().forEach(e -> {
			Tensor prev = previousDelta.get(e.getKey());
			Tensor deltaParams = e.getValue().getDeltaParameters();
			deltaParams.copyInto(prev);
			previousDelta.put(e.getKey(), deltaParams);
		});
		
		return error;
	}

}
