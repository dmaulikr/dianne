/*******************************************************************************
 * DIANNE  - Framework for distributed artificial neural networks
 * Copyright (C) 2015  iMinds - IBCN - UGent
 *
 * This file is part of DIANNE.
 *
 * DIANNE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Tim Verbelen, Steven Bohez
 *******************************************************************************/
package be.iminds.iot.dianne.rl.experience.adapters;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import be.iminds.iot.dianne.api.dataset.Dataset;
import be.iminds.iot.dianne.api.dataset.DatasetDTO;
import be.iminds.iot.dianne.api.dataset.Sample;
import be.iminds.iot.dianne.api.dataset.Sequence;
import be.iminds.iot.dianne.api.nn.Dianne;
import be.iminds.iot.dianne.api.nn.NeuralNetwork;
import be.iminds.iot.dianne.api.nn.module.dto.NeuralNetworkInstanceDTO;
import be.iminds.iot.dianne.api.nn.platform.DiannePlatform;
import be.iminds.iot.dianne.api.repository.RepositoryListener;
import be.iminds.iot.dianne.api.rl.dataset.BatchedExperiencePoolSequence;
import be.iminds.iot.dianne.api.rl.dataset.ExperiencePool;
import be.iminds.iot.dianne.api.rl.dataset.ExperiencePoolBatch;
import be.iminds.iot.dianne.api.rl.dataset.ExperiencePoolSample;
import be.iminds.iot.dianne.api.rl.dataset.ExperiencePoolSequence;
import be.iminds.iot.dianne.tensor.Tensor;
import be.iminds.iot.dianne.tensor.TensorOps;

@Component(
		service={Dataset.class, ExperiencePool.class},	
		configurationPolicy=ConfigurationPolicy.REQUIRE,
		configurationPid="be.iminds.iot.dianne.dataset.adapters.StateBeliefAdapter")
public class StateBeliefAdapter implements ExperiencePool {

	private Map<String, Object> properties;
	
	private ExperiencePool pool;
	private String name;

	private DiannePlatform platform;
	private Dianne dianne;
	
	private NeuralNetwork posterior;
	private UUID[] posteriorIn;
	private UUID[] posteriorOut;
	
	private NeuralNetwork prior;
	protected UUID[] priorIn;
	protected UUID[] priorOut;
	
	private ServiceRegistration<RepositoryListener> repoListenerReg;
	
	private int stateSize;
	
	private int maxSize = 100000;
	
	private List<ExperiencePoolSample> samples;
	private List<Integer> endOfSequences = new ArrayList<>();
	private int index = 0;
	private int sampleSize = 10;
	
	private Executor sampleThread = Executors.newSingleThreadExecutor();
	
	@Reference
	void setDataset(ExperiencePool p){
		this.pool = p;
	}
	
	@Reference
	void setDiannePlatform(DiannePlatform p){
		this.platform = p;
	}
	
	@Reference
	void setDianne(Dianne d){
		this.dianne = d;
	}
	
	@Activate
	void activate(Map<String, Object> properties, BundleContext context) throws Exception {
		// deploy the posterior/prior on local runtime
		UUID frameworkId = UUID.fromString(context.getProperty(Constants.FRAMEWORK_UUID));
		
		this.properties = properties;
		this.name = (String)properties.get("name");
		
		String tag = (String)properties.get("tag");
		String post = (String)properties.get("posterior");
		NeuralNetworkInstanceDTO nnPosterior = platform.deployNeuralNetwork(post, name, frameworkId, new String[]{tag});
		this.posterior = dianne.getNeuralNetwork(nnPosterior).getValue();
		this.posteriorIn = posterior.getModuleIds("State","Action","Observation");
		this.posteriorOut = new UUID[]{posterior.getOutput().getId()};
		
		String pri = (String)properties.get("prior");
		if(pri != null){
			NeuralNetworkInstanceDTO nnPrior = platform.deployNeuralNetwork(pri, name, frameworkId, new String[]{tag});
			prior = dianne.getNeuralNetwork(nnPrior).getValue();
			this.priorIn = prior.getModuleIds("State","Action");
			this.priorOut = new UUID[]{prior.getOutput().getId()};
		}
		
		this.stateSize = Integer.parseInt(properties.get("stateSize").toString().trim());
		
		if(properties.containsKey("maxSize")){
			this.maxSize = Integer.parseInt(properties.get("maxSize").toString().trim());
		}
		
		samples = new ArrayList<ExperiencePoolSample>(maxSize);
		
		if(properties.containsKey("sampleSize")){
			this.sampleSize = Integer.parseInt(properties.get("sampleSize").toString().trim());
		}
		
		
		Dictionary<String, Object> props = new Hashtable<>();
		String[] t = new String[]{":"+tag};
		props.put("targets", t);
		props.put("aiolos.unique", true);
		repoListenerReg = context.registerService(RepositoryListener.class, new RepositoryListener() {
			@Override
			public synchronized void onParametersUpdate(UUID nnId, Collection<UUID> moduleIds, String... tag) {
				if(posterior.getId().equals(nnId)){
					try {
						posterior.loadParameters(tag);
					} catch (Exception e) {
						System.out.println("Failed to update posterior parameters");
					}
				}
				
				if(prior != null && prior.getId().equals(nnId)){
					try {
						prior.loadParameters(tag);
					} catch (Exception e) {
						System.out.println("Failed to update prior parameters");
					}
				}
			}
		}, props);
		
	}

	@Deactivate
	void deactivate(){
		platform.undeployNeuralNetwork(posterior.getId());
		if(prior != null)
			platform.undeployNeuralNetwork(prior.getId());
		
		if(repoListenerReg!=null)
			repoListenerReg.unregister();
	}
	
	@Override
	public DatasetDTO getDTO(){
		DatasetDTO dto = pool.getDTO();
		
		dto.name = getName();
		dto.inputDims = inputDims();
		dto.inputType = inputType();
		dto.targetDims = targetDims();
		dto.targetType = targetType();
		dto.size = size();
		dto.labels = getLabels();
		
		properties.entrySet().forEach(e -> {
			if(e.getKey().contains("."))
				return;
			
			for(Field f : DatasetDTO.class.getFields()){
				if(f.getName().equals(e.getKey()))
					return;
			}
			dto.properties.put(e.getKey(), e.getValue().toString());
		});
		
		return dto;
	}
	
	@Override
	public String getName(){
		return name;
	}
	
	@Override
	public int[] inputDims() {
		return new int[]{stateSize};
	}
	
	@Override
	public String inputType(){
		return "state";
	}

	@Override
	public int[] targetDims() {
		return pool.targetDims();
	}

	@Override
	public String targetType(){
		return pool.targetType();
	}
	
	@Override
	public String[] getLabels() {
		return pool.getLabels();
	}

	@Override
	public int size() {
		return samples.size() < maxSize ? index : maxSize;
	}
	
	@Override
	public int sequences(){
		return endOfSequences.size();
	}
	
	@Override
	public int sequenceLength(int sequence){
		return samples.size();
	}

	@Override
	public Sample getSample(Sample s, int index) {
		if(s == null){
			s = new Sample();
		}
		ExperiencePoolSample xp = getSample((ExperiencePoolSample)null, index);
		s.input = xp.input.copyInto(s.input);
		s.target = xp.target.copyInto(s.target);
		return s;
	}
	
	@Override
	public ExperiencePoolSample getSample(ExperiencePoolSample s, int index){
		s = samples.get(index).copyInto(s);
		return s;
	}
		
	@Override
	public ExperiencePoolBatch getBatch(ExperiencePoolBatch b, int... indices) {
		if(b == null){
			b = new ExperiencePoolBatch(indices.length, new int[]{stateSize}, pool.actionDims());
		}
		
		for(int i=0;i<indices.length;i++){
			getSample(b.getSample(i), indices[i]);
		}
		
		return b;
	}
	
	@Override
	public ExperiencePoolSequence getSequence(ExperiencePoolSequence s, int sequence, int index, int length){
		if(sequence >= endOfSequences.size()){
			throw new RuntimeException("Invalid sequence "+sequence);
		}
		
		int end = endOfSequences.get(sequence);
		int start = 0;
		int l = end+1;
		if(sequence == 0){ 
			if(samples.size() == maxSize){
				//we are cycling ... start is the last end + 1
				start = endOfSequences.get(endOfSequences.size()-1)+1;
				l += maxSize-start; 
			}
		} else {
			start = endOfSequences.get(sequence-1)+1;
			l = end - start + 1;
		}
		
		if(length != -1){
			l = length;
		}
		
		if(start+index > end){
			throw new RuntimeException("Invalid sequence index "+index);
		}
		
		start = start+index;
		if(start > maxSize){
			start -= maxSize;
		}
		
		if(start+l-1 > end){
			throw new RuntimeException("Invalid sequence length "+length);
		}
		
		end = start+l-1;
		if(end > maxSize){
			end -= maxSize;
		}
		
		if(s == null){
			s = new ExperiencePoolSequence();
		}
		
		List<ExperiencePoolSample> list = s.data;
		int i = start;
		int k = 0;
		do {
			if(list.size() <= k){
				list.add(samples.get(i));
			} else {
				samples.get(i).copyInto(list.get(k));
			}
			
			k++;
			i++;
			if(i == maxSize){
				i = 0;
			}
		} while(i != end+1);
		
		s.size = l;
		return s;
	}
	
	@Override
	public BatchedExperiencePoolSequence getBatchedSequence(BatchedExperiencePoolSequence b, int[] sequences, int[] indices,
			int length) {
		throw new UnsupportedOperationException("Not implemented...");
	}
	
	@Override
	public void addSequence(Sequence<ExperiencePoolSample> sequence){
		pool.addSequence(sequence);
		sampleThread.execute(() -> getStateSamplesFromObservations(sequence));
	}
	
	@Override
	public void reset() {
		pool.reset();
		
		samples.clear();
		endOfSequences.clear();
		index = 0;
	}
	
	@Override
	public void dump() {
		pool.dump();
	}

	private void getStateSamplesFromObservations(Sequence<ExperiencePoolSample> sequence) {
		
		Tensor state = new Tensor(stateSize);
		Tensor action = new Tensor(pool.actionDims());
		
		try {
			for(int k = 0;k<sampleSize;k++){
				state.fill(0.0f);
				action.fill(0.0f);
				
				for(int l = 0;l<sequence.size();l++){
					
					ExperiencePoolSample xp = sequence.get(l);
					
					if(prior!=null || l == 0){ // if not using prior, just reuse previous nextState
						Tensor posteriorParams = posterior.forward(posteriorIn, posteriorOut, new Tensor[]{state, action, xp.getState()}).getValue().tensor;
			
						// generate state sample
						sampleState(state, posteriorParams);
					}
					
					xp.getAction().copyInto(action);
					
					// store this sample
					ExperiencePoolSample s;
					if(index < samples.size()){
						s = samples.get(index);
						if(s.isTerminal()){
							endOfSequences.remove((Integer)index);
						}
					} else {
						s = new ExperiencePoolSample();
						samples.add(s);
					}
					
					s.input = state.copyInto(s.input);
					s.target = action.copyInto(s.target);
					s.reward = xp.reward.copyInto(s.reward);
					s.terminal = xp.terminal.copyInto(s.terminal);
					
					if(!xp.isTerminal()){
						// sample next state from prior or posterior?
						if(prior != null){
							Tensor priorParams = prior.forward(priorIn, priorOut, new Tensor[]{state, action}).getValue().tensor;
							sampleState(state, priorParams);
						} else {
							Tensor posteriorParams2 = posterior.forward(posteriorIn, posteriorOut, new Tensor[]{state, action, xp.getNextState()}).getValue().tensor;
							sampleState(state, posteriorParams2);
						}
						s.nextState = state.copyInto(s.nextState);
					} else {
						endOfSequences.add(index);
					}
					
					index++;
					if(index == maxSize){
						index = 0;
					}
				}
			}
		
		} catch(Exception e){
			throw new RuntimeException("Failed to get state from observations...", e);
		}
	}
	
	private void sampleState(Tensor state, Tensor params){
		Tensor mean = params.narrow(0, 0, stateSize);
		Tensor stdev = params.narrow(0, stateSize, stateSize);	
		state.randn();
		TensorOps.cmul(state, state, stdev);
		TensorOps.add(state, state, mean);
	}
	
}
