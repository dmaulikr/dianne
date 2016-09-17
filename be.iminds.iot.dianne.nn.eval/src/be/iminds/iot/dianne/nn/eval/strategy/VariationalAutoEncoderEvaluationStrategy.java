package be.iminds.iot.dianne.nn.eval.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import be.iminds.iot.dianne.api.dataset.Batch;
import be.iminds.iot.dianne.api.dataset.Dataset;
import be.iminds.iot.dianne.api.nn.NeuralNetwork;
import be.iminds.iot.dianne.api.nn.eval.Evaluation;
import be.iminds.iot.dianne.api.nn.eval.EvaluationProgress;
import be.iminds.iot.dianne.api.nn.eval.EvaluationStrategy;
import be.iminds.iot.dianne.api.nn.learn.Criterion;
import be.iminds.iot.dianne.nn.eval.strategy.config.EvaluationStrategyConfig;
import be.iminds.iot.dianne.nn.learn.criterion.CriterionFactory;
import be.iminds.iot.dianne.nn.util.DianneConfigHandler;
import be.iminds.iot.dianne.tensor.Tensor;

public class VariationalAutoEncoderEvaluationStrategy implements EvaluationStrategy {

	protected EvaluationStrategyConfig config;
	
	protected int latentDims = 1;
	
	protected double error = 0;
	protected long tForward = 0;
	
	protected Batch batch;
	protected int[] indices;
	protected Dataset dataset;
	
	protected NeuralNetwork encoder;
	protected NeuralNetwork decoder;
	
	protected Tensor latent;
	protected Tensor latentParams;
	
	protected Criterion criterion;
	
	protected List<Tensor> params;
	protected EvaluationProgress progress;
	
	@Override
	public void setup(Map<String, String> config, Dataset dataset, NeuralNetwork... nns) throws Exception {
		this.dataset = dataset;
		
		this.encoder = nns[0];
		this.decoder = nns[1];
		
		if(config.containsKey("latentDims"))
			this.latentDims = Integer.parseInt(config.get("latentDims"));
		
		this.config = DianneConfigHandler.getConfig(config, EvaluationStrategyConfig.class);
		
		this.indices = new int[this.config.batchSize];
		this.criterion = CriterionFactory.createCriterion(this.config.criterion);
		
		if(this.config.includeOutputs)
			this.params = new ArrayList<Tensor>(dataset.size());
	}

	@Override
	public EvaluationProgress processIteration(long i) throws Exception {
		if(i % indices.length != 0)
			return progress;
		
		if(i+indices.length > dataset.size()) {
			indices = new int[(int)(dataset.size()-i)];
			batch = null;
		}
		
		for(int b=0; b<indices.length; b++)
			indices[b] = (int)(i+b);
		
		batch = dataset.getBatch(batch, indices);
		
		long t = System.nanoTime();
		latentParams = encoder.forward(batch.input);
		tForward += System.nanoTime() - t;
		
		meanLatentVariables();
		
		t = System.nanoTime();
		Tensor output = decoder.forward(latent);
		tForward += System.nanoTime() - t;
		
		if(config.includeOutputs)
			for(int b=0; b<indices.length; b++)
				params.add(latentParams.select(0, b).copyInto(null));
		
		error += criterion.error(output, batch.target).get(0);
		
		return progress = new EvaluationProgress(i+indices.length, dataset.size(), (float) (error/(i+indices.length)));
	}

	@Override
	public Evaluation getResult() {
		Evaluation eval = new Evaluation();
		eval.total = dataset.size();
		eval.error = (float) (error/dataset.size());
		eval.outputs = params;
		eval.forwardTime = (tForward/1000000f)/dataset.size();
		return eval;
	}
	
	private void meanLatentVariables() {
		latent = latentParams.narrow(1, 0, latentDims).copyInto(latent);
	}
}
