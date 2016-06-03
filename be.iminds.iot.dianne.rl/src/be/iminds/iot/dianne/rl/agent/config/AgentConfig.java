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
package be.iminds.iot.dianne.rl.agent.config;


public class AgentConfig {

	public enum ActionStrategy {
		GREEDY,
		BOLTZMANN,
		MANUAL
		// TODO add "raw" in case of continuous outputs?
	}
	
	/**
	 * The strategy with which actions are chosen
	 */
	public ActionStrategy strategy = ActionStrategy.GREEDY;
	
	/**
	 * After how many actions to fetch new weights from the Repository
	 */
	public int syncInterval = 10000;
	
	/**
	 * Tag of fetched weights
	 */
	public String tag;
	
	/**
	 * After how many actions to push experience to Experience Pool
	 */
	public int experienceInterval = 1000;
	
	/**
	 * Maximum size of the experience pool
	 */
	public int experienceSize = 1000000;

	/**
	 * After how many actions to trigger gc
	 */
	public int gcInterval = 1000;
	
	/**
	 * Clean experience pool at start
	 */
	public boolean clean = false;


}
