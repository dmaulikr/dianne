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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.osgi.framework.BundleContext;
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
import be.iminds.iot.dianne.api.coordinator.Device;
import be.iminds.iot.dianne.api.coordinator.DianneCoordinator;
import be.iminds.iot.dianne.api.coordinator.EvaluationResult;
import be.iminds.iot.dianne.api.coordinator.Job;
import be.iminds.iot.dianne.api.coordinator.LearnResult;
import be.iminds.iot.dianne.api.coordinator.Notification;
import be.iminds.iot.dianne.api.coordinator.Notification.Level;
import be.iminds.iot.dianne.api.coordinator.Status;
import be.iminds.iot.dianne.api.nn.eval.Evaluator;
import be.iminds.iot.dianne.api.nn.learn.Learner;
import be.iminds.iot.dianne.api.nn.module.dto.NeuralNetworkDTO;
import be.iminds.iot.dianne.api.nn.platform.DiannePlatform;
import be.iminds.iot.dianne.api.repository.DianneRepository;

@Component
public class DianneCoordinatorImpl implements DianneCoordinator {

	long boot = System.currentTimeMillis();
	
	BundleContext context;
	EventAdmin ea;

	DiannePlatform platform;
	DianneRepository repository;
	
	PlatformManager aiolos;

	Queue<AbstractJob> queue = new LinkedBlockingQueue<>();
	Set<AbstractJob> running = new HashSet<>();
	Queue<AbstractJob> finished = new LinkedBlockingQueue<>(10);
	
	Map<UUID, Learner> learners = new ConcurrentHashMap<>();
	Map<UUID, Evaluator> evaluators = new ConcurrentHashMap<>();
	
	ExecutorService pool = Executors.newCachedThreadPool();

	Map<UUID, Device> devices = new ConcurrentHashMap<>();
	// keeps which device is doing what
	// 0 = idle
	// 1 = learning
	// 2 = evaluating
	Map<UUID, Integer> deviceUsage = new ConcurrentHashMap<>(); 
	
	Queue<Notification> notifications = new LinkedBlockingQueue<>(20);
	
	@Override
	public Status getStatus(){
		int idle = deviceUsage.values().stream().mapToInt(i -> (i==0) ? 1 : 0).sum();
		int learn = deviceUsage.values().stream().mapToInt(i -> (i==1) ? 1 : 0).sum();
		int eval = deviceUsage.values().stream().mapToInt(i -> (i==2) ? 1 : 0).sum();

		long spaceLeft = repository.spaceLeft();
		Status currentStatus = new Status(queue.size(), running.size(), learn, eval, idle, spaceLeft, boot);
		return currentStatus;
	}
	
	@Override
	public Promise<LearnResult> learn(NeuralNetworkDTO nn, String dataset, Map<String, String> config) {
		repository.storeNeuralNetwork(nn);
		
		LearnJob job = new LearnJob(this, nn, dataset, config);
		queue.add(job);
		
		sendNotification(job.jobId, Level.INFO, "Job \""+job.name+"\" submitted.");
		
		schedule();
		
		return job.getPromise();
	}

	@Override
	public Promise<LearnResult> learn(String nnName, String dataset, Map<String, String> config) {
		return learn(repository.loadNeuralNetwork(nnName), dataset, config);
	}
	
	@Override
	public Promise<EvaluationResult> eval(NeuralNetworkDTO nn, String dataset, Map<String, String> config) {
		// TODO evaluate time on other/multiple platforms?
		repository.storeNeuralNetwork(nn);
		
		EvaluationJob job = new EvaluationJob(this, nn, dataset, config);
		queue.add(job);
		
		sendNotification(job.jobId, Level.INFO, "Job \""+job.name+"\" submitted.");
		
		schedule();
		
		return job.getPromise();
	}

	@Override
	public Promise<EvaluationResult> eval(String nnName, String dataset, Map<String, String> config) {
		return eval(repository.loadNeuralNetwork(nnName), dataset, config);
	}
	
	@Override
	public List<Job> queuedJobs() {
		return queue.stream().map(j -> j.get()).collect(Collectors.toList());
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
			} else {
				System.out.println("WTF is null?!");
			}
		});
		return new ArrayList<>(devices.values());
	}
	
	// called when a job is done
	void done(AbstractJob job){
		// remove from running list
		running.remove(job);
		job.targets.stream().forEach(uuid -> deviceUsage.put((UUID) uuid, 0));
		
		
		// TODO safe results to disc/archive?
		
		finished.add(job);
		
		
		sendNotification(job.jobId, Level.SUCCESS, "Job \""+job.name+"\" finished successfully.");

		// schedule new one
		schedule();
	}
	
	// try to schedule the job on top of the queue
	synchronized void schedule(){
		// TODO what if not enough learners/evaluators or no matching learners/evaluators?
		
		// try to schedule the next job on the queue
		AbstractJob job = queue.peek();
		// TODO check the config whether this one can execute
		if(job instanceof LearnJob){
			// search free learner
			// TODO collect multiple in case multiple learners required
			int required = 1;
			// TODO filter learners on job properties
			List<UUID> targets = learners.keySet().stream().filter(uuid -> deviceUsage.get(uuid)==0).limit(required).collect(Collectors.toList());
			if(targets.size()!=required)
				return;
			
			for(UUID target : targets){
				deviceUsage.put(target, 1);
			}
			job = queue.poll();
			job.start(targets, pool);
			running.add(job);
			
			sendNotification(job.jobId, Level.INFO, "Job \""+job.name+"\" started.");

		} if(job instanceof EvaluationJob){
			// search free evaluator
			// TODO collect multiple in case multiple evaluators required
			int required = 1;
			// TODO filter evaluators on job properties
			List<UUID> targets = evaluators.keySet().stream().filter(uuid -> deviceUsage.get(uuid)==0).limit(required).collect(Collectors.toList());
			if(targets.size()!=required)
				return;

			for(UUID target : targets){
				deviceUsage.put(target, 2);
			}
			job = queue.poll();
			job.start(targets, pool);
			running.add(job);
			
			sendNotification(job.jobId, Level.INFO, "Job \""+job.name+"\" started.");

		}
	}

	void sendNotification(UUID jobId, Level level, String message){
		Notification n = new Notification(jobId, level, message);
		
		Map<String, Object> properties = new HashMap<>();
		properties.put("id", n.jobId);
		properties.put("level", n.level);
		properties.put("message", n.message);
		properties.put("timestamp", n.timestamp);
		String topic = "dianne/jobs/"+jobId.toString();
		Event e = new Event(topic, properties);
		ea.postEvent(e);
		
		notifications.add(n);
	}

	@Activate
	void activate(BundleContext context){
		this.context = context;
	}
	
	// TODO here we use a name (AIOLOS) that is alphabtically before the others
	// so that the reference is set in addLearer/addEvaluator
	@Reference(cardinality=ReferenceCardinality.OPTIONAL)
	void setAIOLOS(PlatformManager p){  
		this.aiolos = p;
	}
	
	@Reference
	void setEventAdmin(EventAdmin ea){
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
	
	@Reference(policy=ReferencePolicy.DYNAMIC,
			cardinality=ReferenceCardinality.MULTIPLE)
	void addLearner(Learner learner, Map<String, Object> properties){
		UUID id = learner.getLearnerId();
		this.learners.put(id, learner);
		
		Device device = devices.get(id);
		if(device == null){
			deviceUsage.put(id, 0);

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
		device.learn = true;
		
		schedule();
	}
	
	void removeLearner(Learner learner, Map<String, Object> properties){
		UUID id = null;
		Iterator<Entry<UUID, Learner>> it =this.learners.entrySet().iterator();
		while(it.hasNext()){
			Entry<UUID, Learner> e = it.next();
			if(e.getValue()==learner){
				id = e.getKey();
				it.remove();
				break;
			}
		}
		
		if(id!=null){
			if(!learners.containsKey(id) 
				&& !evaluators.containsKey(id)){
				deviceUsage.remove(id);
				devices.remove(id);
			}
		}
	}

	@Reference(policy=ReferencePolicy.DYNAMIC,
			cardinality=ReferenceCardinality.MULTIPLE)
	void addEvaluator(Evaluator evaluator, Map<String, Object> properties){
		UUID id = evaluator.getEvaluatorId();
		this.evaluators.put(id, evaluator);
		
		Device device = devices.get(id);
		if(device == null){
			deviceUsage.put(id, 0);

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
		device.eval = true;
		
		schedule();
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
		
		if(id!=null){
			if(!learners.containsKey(id) 
				&& !evaluators.containsKey(id)){
				deviceUsage.remove(id);
				devices.remove(id);
			}
		}
	}
	
	boolean isRecurrent(NeuralNetworkDTO nn){
		if(nn.modules.values().stream().filter(module -> module.type.equals("Memory")).findAny().isPresent())
			return true;
		
		return nn.modules.values().stream().filter(module -> module.properties.get("category").equals("Composite"))
			.mapToInt(module ->  
				isRecurrent(repository.loadNeuralNetwork(module.properties.get("name"))) ? 1 : 0).sum() > 0;
	}
}
