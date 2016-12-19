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
package be.iminds.iot.dianne.api.coordinator;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.osgi.util.promise.Promise;

import be.iminds.iot.dianne.api.nn.module.dto.NeuralNetworkDTO;

public interface DianneCoordinator {

	Status getStatus();
	
	Promise<LearnResult> learn(String dataset, Map<String, String> config, String... nnName);
	
	Promise<LearnResult> learn(String dataset, Map<String, String> config, NeuralNetworkDTO... nn);

	Promise<EvaluationResult> eval(String dataset, Map<String, String> config, String... nnName);
	
	Promise<EvaluationResult> eval(String dataset, Map<String, String> config, NeuralNetworkDTO... nn);
	
	Promise<AgentResult> act(String dataset, Map<String, String> config, String... nnName);
	
	Promise<AgentResult> act(String dataset, Map<String, String> config, NeuralNetworkDTO... nn);
	
	LearnResult getLearnResult(UUID jobId); // if still running, this contains partial progress results
	
	EvaluationResult getEvaluationResult(UUID jobId);  // if still running, this contains partial progress results
	
	AgentResult getAgentResult(UUID jobId);  // if still running, this contains partial progress results
	
	void stop(UUID jobId) throws Exception; // stop running job or cancel submitted job
	
	Job getJob(UUID jobId);
	
	List<Job> queuedJobs();
	
	List<Job> runningJobs();
	
	List<Job> finishedJobs();
	
	List<Notification> getNotifications();
	
	List<Device> getDevices();
	
}
