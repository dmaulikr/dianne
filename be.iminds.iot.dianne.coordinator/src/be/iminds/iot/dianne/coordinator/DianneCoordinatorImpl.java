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
package be.iminds.iot.dianne.coordinator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.promise.Promise;

import be.iminds.aiolos.info.NodeInfo;
import be.iminds.aiolos.monitor.node.api.NodeMonitorInfo;
import be.iminds.aiolos.platform.api.PlatformManager;
import be.iminds.iot.dianne.api.coordinator.AgentResult;
import be.iminds.iot.dianne.api.coordinator.Device;
import be.iminds.iot.dianne.api.coordinator.DianneCoordinator;
import be.iminds.iot.dianne.api.coordinator.EvaluationResult;
import be.iminds.iot.dianne.api.coordinator.Job;
import be.iminds.iot.dianne.api.coordinator.Job.Type;
import be.iminds.iot.dianne.api.coordinator.LearnResult;
import be.iminds.iot.dianne.api.coordinator.Notification;
import be.iminds.iot.dianne.api.coordinator.Notification.Level;
import be.iminds.iot.dianne.api.coordinator.Status;
import be.iminds.iot.dianne.api.nn.eval.Evaluator;
import be.iminds.iot.dianne.api.nn.learn.LearnProgress;
import be.iminds.iot.dianne.api.nn.learn.Learner;
import be.iminds.iot.dianne.api.nn.module.dto.NeuralNetworkDTO;
import be.iminds.iot.dianne.api.nn.platform.DiannePlatform;
import be.iminds.iot.dianne.api.repository.DianneRepository;
import be.iminds.iot.dianne.api.rl.agent.Agent;

@Component
public class DianneCoordinatorImpl implements DianneCoordinator {

	long boot = System.currentTimeMillis();
	
	BundleContext context;
	EventAdmin ea;

	DiannePlatform platform;
	DianneRepository repository;
	
	PlatformManager aiolos;

	// separate queues for learn and eval jobs, 
	// makes sure eval jobs are not blocked by big learn jobs
	Queue<AbstractJob> queueLearn = new LinkedBlockingQueue<>();
	Queue<AbstractJob> queueEval = new LinkedBlockingQueue<>();
	Queue<AbstractJob> queueAct = new LinkedBlockingQueue<>();


	Set<AbstractJob> running = new HashSet<>();
	Queue<AbstractJob> finished = new CircularBlockingQueue<>(10);
	
	Map<String, Map<UUID, Learner>> learners = new ConcurrentHashMap<>();
	Map<UUID, Evaluator> evaluators = new ConcurrentHashMap<>();
	Map<UUID, Agent> agents = new ConcurrentHashMap<>();

	
	ExecutorService pool = Executors.newCachedThreadPool();

	Map<UUID, Device> devices = new ConcurrentHashMap<>();
	// keeps which device is doing what (-1 is idle)
	Map<UUID, Integer> deviceUsage = new ConcurrentHashMap<>(); 
	
	Queue<Notification> notifications = new CircularBlockingQueue<>(20);
	
	@Override
	public Status getStatus(){
		int idle = deviceUsage.values().stream().mapToInt(t -> (t==-1) ? 1 : 0).sum();
		int learn = deviceUsage.values().stream().mapToInt(t -> (t==Type.LEARN.ordinal()) ? 1 : 0).sum();
		int eval = deviceUsage.values().stream().mapToInt(t -> (t==Type.EVALUATE.ordinal()) ? 1 : 0).sum();
		int act = deviceUsage.values().stream().mapToInt(t -> (t==Type.ACT.ordinal()) ? 1 : 0).sum();

		
		long spaceLeft = repository.spaceLeft();
		Status currentStatus = new Status(queueLearn.size()+queueEval.size(), running.size(), learn, eval, act, idle, spaceLeft, boot);
		return currentStatus;
	}
	
	@Override
	public Promise<LearnResult> learn(NeuralNetworkDTO nn, String dataset, Map<String, String> config) {
		repository.storeNeuralNetwork(nn);
		
		LearnJob job = new LearnJob(this, nn, dataset, config);
		queueLearn.add(job);
		
		sendNotification(job.jobId, Level.INFO, "Learn job \""+job.name+"\" submitted.");
		
		schedule(Type.LEARN);
		
		return job.getPromise();
	}

	@Override
	public Promise<LearnResult> learn(String nnName, String dataset, Map<String, String> config) {
		return learn(repository.loadNeuralNetwork(nnName), dataset, config);
	}
	
	@Override
	public Promise<EvaluationResult> eval(NeuralNetworkDTO nn, String dataset, Map<String, String> config) {
		repository.storeNeuralNetwork(nn);
		
		EvaluationJob job = new EvaluationJob(this, nn, dataset, config);
		queueEval.add(job);
		
		sendNotification(job.jobId, Level.INFO, "Evaluation job \""+job.name+"\" submitted.");
		
		schedule(Type.EVALUATE);
		
		return job.getPromise();
	}

	@Override
	public Promise<AgentResult> act(String nnName, String dataset, Map<String, String> config) {
		return act(repository.loadNeuralNetwork(nnName), dataset, config);
	}
	
	@Override
	public Promise<AgentResult> act(NeuralNetworkDTO nn, String dataset, Map<String, String> config) {
		repository.storeNeuralNetwork(nn);
		
		ActJob job = new ActJob(this, nn, dataset, config);
		queueAct.add(job);
		
		sendNotification(job.jobId, Level.INFO, "Act job \""+job.name+"\" submitted.");
		
		schedule(Type.ACT);
		
		return job.getPromise();
	}

	@Override
	public Promise<EvaluationResult> eval(String nnName, String dataset, Map<String, String> config) {
		return eval(repository.loadNeuralNetwork(nnName), dataset, config);
	}
	
	@Override
	public LearnResult getLearnResult(UUID jobId) {
		// check if this job is running, if so return progress
		AbstractJob running = getRunningJob(jobId);
		if(running!=null && running instanceof LearnJob){
			return ((LearnJob)running).getProgress();
		}
		
		// dig into the done results
		Object result = getResult(jobId);
		if(result instanceof LearnResult){
			return (LearnResult) result;
		}
		
		// noting found
		return null;
	}

	@Override
	public EvaluationResult getEvaluationResult(UUID jobId) {
		// check if this job is running, if so return progress
		AbstractJob running = getRunningJob(jobId);
		if(running!=null && running instanceof EvaluationJob){
			return ((EvaluationJob)running).getProgress();
		}
		
		// dig into the done results
		Object result = getResult(jobId);
		if(result instanceof EvaluationResult){
			return (EvaluationResult) result;
		}
		
		// nothing found
		return null;
	}

	@Override
	public AgentResult getAgentResult(UUID jobId) {
		// check if this job is running, if so return progress
		AbstractJob running = getRunningJob(jobId);
		if(running!=null && running instanceof ActJob){
			return ((ActJob)running).getProgress();
		}
		
		// dig into the done results
		Object result = getResult(jobId);
		if(result instanceof AgentResult){
			return (AgentResult) result;
		}
		
		// nothing found
		return null;
	}
	
	@Override
	public void stop(UUID jobId) throws Exception {
		AbstractJob job = getAbstractJob(jobId);
		if(job!=null){
			job.stop();
		}
	}
	
	private AbstractJob getAbstractJob(UUID jobId){
		AbstractJob job = null;
		try {
			job = queueLearn.stream().filter(j -> j.jobId.equals(jobId)).findFirst().get();
		} catch(NoSuchElementException e){}
		try {
			job = queueEval.stream().filter(j -> j.jobId.equals(jobId)).findFirst().get();
		} catch(NoSuchElementException e){}
		try {
			job = running.stream().filter(j -> j.jobId.equals(jobId)).findFirst().get();
		} catch(NoSuchElementException e){}
		try {
			job = finished.stream().filter(j -> j.jobId.equals(jobId)).findFirst().get();
		} catch(NoSuchElementException e){}
		
		return job;
	}
	
	@Override
	public Job getJob(UUID jobId){
		AbstractJob job = getAbstractJob(jobId);
		if(job!=null)
			return job.get();
		
		// TODO read from persistent storage?
		
		return null;
	}
	
	@Override
	public List<Job> queuedJobs() {
		List<Job> learnJobs = queueLearn.stream().map(j -> j.get()).collect(Collectors.toList());
		List<Job> evalJobs = queueEval.stream().map(j -> j.get()).collect(Collectors.toList());

		List<Job> allJobs = learnJobs;
		allJobs.addAll(evalJobs);
		allJobs.sort(new Comparator<Job>() {

			@Override
			public int compare(Job o1, Job o2) {
				return (int)(o1.submitted - o2.submitted);
			}
		});
		return allJobs;
	}

	@Override
	public List<Job> runningJobs() {
		return running.stream().map(j -> j.get()).collect(Collectors.toList());
	}

	@Override
	public List<Job> finishedJobs() {
		return finished.stream().map(j -> j.get()).collect(Collectors.toList());
	}
	
	
	@Override
	public List<Notification> getNotifications(){
		return new ArrayList<>(notifications);
	}

	@Override
	public List<Device> getDevices(){
		devices.values().stream().forEach(device -> {
			NodeMonitorInfo nmi = aiolos.getNodeMonitorInfo(device.id.toString());
			if(nmi!=null){
				device.cpuUsage = nmi.getCpuUsage();
				device.memUsage = nmi.getMemoryUsage();
			}
		});
		return new ArrayList<>(devices.values());
	}
	
	// called when a job is done
	void done(AbstractJob job) {
		// remove from running list
		if(running.remove(job)){
			job.targets.stream().forEach(uuid -> deviceUsage.put((UUID) uuid, -1));
			
			try {
				Throwable error = job.getPromise().getFailure();
				if(error !=null){
					sendNotification(job.jobId, Level.DANGER, "Job \""+job.name+"\" failed: "+error.getMessage());
				} else {
					// TODO safe results to disc/archive?
					
					
					
					
					sendNotification(job.jobId, Level.SUCCESS, "Job \""+job.name+"\" finished successfully.");
				}
			} catch (InterruptedException e) {
			}
		} else {
			// if not running, remove from any queue
			queueLearn.remove(job);
			queueEval.remove(job);
			queueAct.remove(job);
			sendNotification(job.jobId, Level.WARNING, "Job \""+job.name+"\" canceled.");
		}
		
		finished.add(job);

		// schedule new one, check which queue has longest waiting queue item and schedule that one first
		SortedSet<Queue<AbstractJob>> queues = new TreeSet<>(new Comparator<Queue<AbstractJob>>() {
			@Override
			public int compare(Queue<AbstractJob> o1, Queue<AbstractJob> o2) {
				AbstractJob j1 = o1.peek();
				AbstractJob j2 = o2.peek();
				if(j1 == null){
					if(j2 ==null){
						return 0;
					} else {
						return 1;
					}
				} else if(j2 ==null){
					return -1;
				} else {
					return (int)(j1.submitted - j2.submitted);
				}
			}
		});
		queues.add(queueLearn);
		queues.add(queueEval);
		queues.add(queueAct);
		for(Queue q : queues){
			schedule( (q==queueLearn) ? Type.LEARN : (q==queueEval) ? Type.EVALUATE : Type.ACT  );
		}
	}
	
	// try to schedule the job on top of the queue
	synchronized void schedule(Type type){

		Queue<AbstractJob> queue = null;
		switch(type){
		case LEARN:
			queue = queueLearn;
			break;
		case EVALUATE:
			queue = queueEval;
			break;
		case ACT:
			queue = queueAct;
			break;
		}
		
		// try to schedule the next job on the queue
		AbstractJob job = queue.peek();
		if(job==null){
			// no more jobs...
			return;
		}
		
		// check in case a target list is given as comma separated uuids
		List<UUID> targets = null;
		String t = (String)job.config.get("targets");
		if(t!=null){
			try {
				targets = new ArrayList<>();
				for(String tt : t.split(",")){
					targets.add(UUID.fromString(tt));
				}
			} catch(Exception e){
				e.printStackTrace();
				targets = null;
			}
		}
		
		// check if count/filter is specified
		int count = 1;
		if(job.config.containsKey("targetCount")){
			count = Integer.parseInt((String)job.config.get("targetCount"));
		} else if(targets!=null){
			// if no count given but targets is, use all of them?
			count = targets.size();
		}
		String filter = (String)job.config.get("targetFilter");
		
		try {
			if(targets!=null){
				targets = findTargets(targets, filter, count);
			} else if(type==Type.EVALUATE){
				targets = findTargets(evaluators.keySet(), filter, count);
			} else if(type==Type.LEARN){
				if(!learners.containsKey(job.category.toString())){
					throw new Exception("No learner available for category "+job.category.toString());
				}
				targets = findTargets(learners.get(job.category.toString()).keySet(), filter, count);
			} else if(type==Type.ACT){
				targets = findTargets(agents.keySet(), filter, count);
			}
		} catch(Exception e){
			
			job = queue.poll();
			job.deferred.fail(e);
			
			sendNotification(job.jobId, Level.DANGER, "Job \""+job.name+"\" failed to start: "+e.getMessage());
		}
		
		if(targets==null){
			// what if no targets found? try next one or just keep on waiting?
			return;
		}
			
		for(UUID target : targets){
			deviceUsage.put(target, job.type.ordinal());
		}
		job = queue.poll();
		job.start(targets, pool);
		running.add(job);
			
		sendNotification(job.jobId, Level.INFO, "Job \""+job.name+"\" started.");

	}
	
	List<UUID> findTargets(Collection<UUID> ids, String filter, int count) throws Exception {
		List<UUID> targets = ids.stream()
					.map(uuid -> devices.get(uuid))
					.filter( device -> {	// match device filter
						if(device==null)
							return false;  // could be an invalid uuid if given from targets property
						
						if(filter==null)
							return true;
						
						try {
							Filter f = context.createFilter(filter);
							return f.matches(toMap(device));
						} catch(Exception e){
							e.printStackTrace();
							return false;
						}
					})
					.map(device -> device.id)
					.collect(Collectors.toList());
		// check if this is possible
		if(targets.size() < count)
			throw new Exception("Insufficient infrastructure to meet the requirements of this Job");
		
		// check if the possible devices are currently available
		targets = targets.stream()
			.filter(uuid -> deviceUsage.get(uuid)==-1) // only free nodes can be selected
			.limit(count)
			.collect(Collectors.toList());

		if(targets.size() != count){
			return null;
		}
		
		return targets;
	}

	void sendNotification(UUID jobId, Level level, String message){
		Notification n = new Notification(jobId, level, message);
		
		Map<String, Object> properties = new HashMap<>();
		if(jobId!=null)
			properties.put("jobId", jobId);
		properties.put("level", n.level);
		properties.put("message", n.message);
		properties.put("timestamp", n.timestamp);
		
		String topic = (jobId != null) ? "dianne/jobs/"+jobId.toString() : "dianne/jobs";
		Event e = new Event(topic, properties);
		ea.postEvent(e);
		
		notifications.add(n);
	}

	void sendLearnProgress(UUID jobId, LearnProgress progress){
		Map<String, Object> properties = new HashMap<>();
		properties.put("jobId", jobId.toString());
		properties.put("iteration", progress.iteration);
		properties.put("error", progress.error);
		
		String topic = "dianne/jobs/"+jobId.toString()+"/progress";
		Event e = new Event(topic, properties);
		ea.postEvent(e);
	}
	
	@Activate
	void activate(BundleContext context){
		this.context = context;
	}
	
	// TODO here we use a name (AIOLOS) that is alphabetically before the others
	// so that the reference is set in addLearer/addEvaluator
	@Reference(cardinality=ReferenceCardinality.OPTIONAL)
	void setAIOLOS(PlatformManager p){  
		this.aiolos = p;
	}
	
	@Reference
	void setEA(EventAdmin ea){
		this.ea = ea;
	}
	
	@Reference
	void setDiannePlatform(DiannePlatform platform){
		this.platform = platform;
	}
	
	@Reference
	void setDianneRepository(DianneRepository repository){
		this.repository = repository;
	}
	
	Device addDevice(UUID id){
		Device device = devices.get(id);
		if(device == null){
			deviceUsage.put(id, -1);

			String name = id.toString();
			String arch = "unknown";
			String os = "unknown";
			String ip = "unknown";
			
			if(aiolos!=null){
				NodeInfo n = aiolos.getNode(id.toString());
				name = n.getName();
				arch = n.getArch();
				os = n.getOS();
				ip = n.getIP();
			}
			device = new Device(id, name, arch, os, ip);
			devices.put(id, device);
		}
		return device;
	}
	
	void removeDevice(UUID id){
		if(id!=null){
			if(!learners.containsKey(id) 
			&& !evaluators.containsKey(id)
			&& !agents.containsKey(id)){
				deviceUsage.remove(id);
				devices.remove(id);
			}
		}
	}
	
	@Reference(policy=ReferencePolicy.DYNAMIC,
			cardinality=ReferenceCardinality.MULTIPLE)
	void addLearner(Learner learner, Map<String, Object> properties){
		String type = (String)properties.get("dianne.learner.category");
		
		Map<UUID, Learner> ll = learners.get(type);
		if(ll==null){
			ll = new ConcurrentHashMap<>();
			learners.put(type, ll);
		}
		
		UUID id = learner.getLearnerId();
		ll.put(id, learner);
		
		Device device = addDevice(id);
		device.learn = true;
		
		sendNotification(null, Level.INFO, "New "+type+" Learner "+id+" is added to the system.");
		
		schedule(Type.LEARN);
	}
	
	void removeLearner(Learner learner, Map<String, Object> properties){
		String type = (String)properties.get("dianne.learner.category");

		Map<UUID, Learner> ll = learners.get(type);
		
		UUID id = null;
		Iterator<Entry<UUID, Learner>> it = ll.entrySet().iterator();
		while(it.hasNext()){
			Entry<UUID, Learner> e = it.next();
			if(e.getValue()==learner){
				id = e.getKey();
				it.remove();
				break;
			}
		}
		
		removeDevice(id);
		
		sendNotification(null, Level.WARNING, "Learner "+id+" is removed from the system.");
		
		final UUID target = id;
		running.stream()
			.filter(job -> job.type == Type.LEARN)
			.filter(job -> job.targets.contains(target)).forEach(job -> job.done(new Exception("Job failed because executing node was killed")));
	}

	@Reference(policy=ReferencePolicy.DYNAMIC,
			cardinality=ReferenceCardinality.MULTIPLE)
	void addEvaluator(Evaluator evaluator, Map<String, Object> properties){
		UUID id = evaluator.getEvaluatorId();
		this.evaluators.put(id, evaluator);
		
		Device device = addDevice(id);
		device.eval = true;
		
		sendNotification(null, Level.INFO, "New Evaluator "+id+" is added to the system.");
		
		schedule(Type.EVALUATE);
	}
	
	void removeEvaluator(Evaluator evaluator, Map<String, Object> properties){
		UUID id = null;
		Iterator<Entry<UUID, Evaluator>> it =this.evaluators.entrySet().iterator();
		while(it.hasNext()){
			Entry<UUID, Evaluator> e = it.next();
			if(e.getValue()==evaluator){
				id = e.getKey();
				it.remove();
				break;
			}
		}
		
		removeDevice(id);
		
		sendNotification(null, Level.WARNING, "Evaluator "+id+" is removed from the system.");
		
		final UUID target = id;
		running.stream()
			.filter(job -> job.type == Type.EVALUATE)
			.filter(job -> job.targets.contains(target)).forEach(job -> job.done(new Exception("Job failed because executing node was killed")));
	}
	
	@Reference(policy=ReferencePolicy.DYNAMIC,
			cardinality=ReferenceCardinality.MULTIPLE)
	void addAgent(Agent agent, Map<String, Object> properties){
		UUID id = agent.getAgentId();
		this.agents.put(id, agent);
		
		Device device = addDevice(id);
		device.act = true;
		
		sendNotification(null, Level.INFO, "New Agent "+id+" is added to the system.");
		
		schedule(Type.ACT);
	}
	
	void removeAgent(Agent agent, Map<String, Object> properties){
		UUID id = null;
		Iterator<Entry<UUID, Agent>> it =this.agents.entrySet().iterator();
		while(it.hasNext()){
			Entry<UUID, Agent> e = it.next();
			if(e.getValue()==agent){
				id = e.getKey();
				it.remove();
				break;
			}
		}
		
		removeDevice(id);
		
		sendNotification(null, Level.WARNING, "Agent "+id+" is removed from the system.");
		
		final UUID target = id;
		running.stream()
			.filter(job -> job.type == Type.ACT)
			.filter(job -> job.targets.contains(target)).forEach(job -> job.done(new Exception("Job failed because executing node was killed")));
	}
	
	private AbstractJob getRunningJob(UUID jobId){
		try {
			return running.stream().filter(job -> job.jobId.equals(jobId)).findFirst().get();
		} catch(NoSuchElementException e){
			return null;
		}
	}
	
	private Object getResult(UUID jobId){
		// TODO read from persistent storage?
		try {
			return finished.stream().filter(job -> job.jobId.equals(jobId)).findFirst().get().getPromise().getValue();
		} catch(Exception e){
			return null;
		}
	}
	
	boolean isRecurrent(NeuralNetworkDTO nn){
		if(nn.modules.values().stream().filter(module -> module.type.equals("Memory")).findAny().isPresent())
			return true;
		
		return nn.modules.values().stream().filter(module -> module.properties.get("category")!= null && module.properties.get("category").equals("Composite"))
			.mapToInt(module ->  
				isRecurrent(repository.loadNeuralNetwork(module.properties.get("name"))) ? 1 : 0).sum() > 0;
	}
	
	// TODO use Object Conversion spec for this...
	private Map<String, Object> toMap(Object o){
		Map<String, Object> properties = new HashMap<>();
		for(Field f : o.getClass().getFields()){
			try {
				properties.put(f.getName(), f.get(o));
			} catch (Exception e) {
			}
		}
		return properties;
	}
}
