package be.iminds.iot.dianne.nn.test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.UUID;

import junit.framework.TestCase;

import org.junit.Assert;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import be.iminds.iot.dianne.dataset.Dataset;
import be.iminds.iot.dianne.dataset.DatasetAdapter;
import be.iminds.iot.dianne.nn.module.Input;
import be.iminds.iot.dianne.nn.module.Output;
import be.iminds.iot.dianne.nn.module.Trainable;
import be.iminds.iot.dianne.nn.runtime.ModuleManager;
import be.iminds.iot.dianne.nn.runtime.util.DianneJSONParser;
import be.iminds.iot.dianne.nn.train.Criterion;
import be.iminds.iot.dianne.nn.train.Evaluation;
import be.iminds.iot.dianne.nn.train.criterion.MSECriterion;
import be.iminds.iot.dianne.nn.train.criterion.NLLCriterion;
import be.iminds.iot.dianne.nn.train.eval.ArgMaxEvaluator;
import be.iminds.iot.dianne.nn.train.strategy.StochasticGradient;
import be.iminds.iot.dianne.tensor.TensorFactory;

public class DianneTest extends TestCase {

    private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    
    private TensorFactory factory;
    private Dataset mnist;
    private ModuleManager mm;

    private List<UUID> modules = null;
    
    public void setUp(){
    	// get references to required services
    	// TODO should only be done once?
    	ServiceReference rf = context.getServiceReference(TensorFactory.class.getName());
    	factory = (TensorFactory) context.getService(rf);
    	
    	ServiceReference rd = context.getServiceReference(Dataset.class.getName());
    	mnist = (Dataset) context.getService(rd);
    	
    	ServiceReference rmm =  context.getServiceReference(ModuleManager.class.getName());
    	mm = (ModuleManager) context.getService(rmm);
    }
    
    public void tearDown(){
    	// tear down deployed NN modules after each test
    	if(modules!=null){
    		undeployNN(modules);
    		modules = null;
    	}
    }
    
    public void testLinearSigmoid() throws Exception {
    	sgd("test-mnist-linear-sigmoid", 10, 1, new MSECriterion(factory));
    }
    
    public void testLinearSoftmax() throws Exception {
    	sgd("test-mnist-linear-softmax", 10, 1, new NLLCriterion(factory));
    }
    
    public void testConv() throws Exception {
    	sgd("test-mnist-conv", 10, 1, new NLLCriterion(factory));
    }
    
    private void sgd(String config, int batch, int epochs, Criterion loss) throws Exception {
    	modules = deployNN("nn/"+config+".txt");
    	
    	StochasticGradient trainer = new StochasticGradient(batch, epochs);
    	
    	Input input = getInput();
    	Output output = getOutput();
    	
    	Dataset train = new DatasetAdapter(mnist, 0, 60000);
    	trainer.train(input, output, getTrainable(), loss, train);

    	ArgMaxEvaluator evaluator = new ArgMaxEvaluator(factory);
    	Dataset test = new DatasetAdapter(mnist, 60000, 70000);
    	
    	Evaluation eval = evaluator.evaluate(input, output, test);
    	System.out.println("Accuracy "+eval.accuracy());
    	Assert.assertTrue(eval.accuracy()>0.8);
    }
    
    private Input getInput(){
    	ServiceReference ri =  context.getServiceReference(Input.class.getName());
    	Assert.assertNotNull(ri);
    	Input input = (Input) context.getService(ri);
    	return input;
    }
    
    private Output getOutput(){
    	ServiceReference ro =  context.getServiceReference(Output.class.getName());
    	Assert.assertNotNull(ro);
    	Output output = (Output) context.getService(ro);
    	return output;
    }
    
    private List<Trainable> getTrainable() throws Exception {
    	List<Trainable> modules = new ArrayList<Trainable>();
    	ServiceReference[] refs = context.getAllServiceReferences(Trainable.class.getName(), null);
    	for(ServiceReference r : refs){
    		modules.add((Trainable)context.getService(r));
    	}
    	return modules;
    }
    
    private List<UUID> deployNN(String configLocation) throws Exception {
    	String json = new String(Files.readAllBytes(Paths.get(configLocation)));
    	List<Dictionary<String, Object>> configs = DianneJSONParser.parseJSON(json);
    	
    	List<UUID> ids = new ArrayList<UUID>();
    	for(Dictionary<String, Object> config : configs){
    		try {
	    		mm.deployModule(config);
	    		
	    		String id = (String)config.get("module.id");
	    		ids.add(UUID.fromString(id));
    		} catch(InstantiationException e){}
    	}
    	
    	return ids;
    }
    
    private void undeployNN(List<UUID> modules){
    	for(UUID id : modules){
    		mm.undeployModule(id);
    	}
    }
}
