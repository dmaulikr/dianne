/*******************************************************************************
 * DIANNE  - Framework for distributed artificial neural networks
 * Copyright (C) 2015  Ghent University, iMinds
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

var DIANNE = {
		learn: function (nn, dataset, properties) {
			return this.call("learn", [nn, dataset, properties]);
		},
		eval: function (nnName, dataset, properties){
			return this.call("eval", [nnName, dataset, properties]);
		},
		act: function (nnName, dataset, properties){
			return this.call("act", [nnName, dataset, properties]);
		},
		learnResult : function(jobId){
			return this.call("learnResult", [jobId]);
		},
		evaluationResult : function(jobId){
			return this.call("evaluationResult", [jobId]);
		},
		agentResult : function(jobId){
			return this.call("agentResult", [jobId]);
		},
		job : function(jobId){
			return this.call("job", [jobId]);
		},
		nns : function(){
			return this.call("availableNeuralNetworks");
		},
		datasets : function(){
			return this.call("availableDatasets");
		},
		queuedJobs : function(){
			return this.call("queuedJobs");
		}, 
		runningJobs : function(){
			return this.call("runningJobs");
		},
		finishedJobs : function(){
			return this.call("finishedJobs");
		},
		notifications : function(){
			return this.call("notifications");
		},
		status : function(){
			return this.call("status");
		},
		devices : function(){
			return this.call("devices");
		},
		call: function(method, params){
			var d = $.Deferred();
			jsonrpc(method, params, function(data){
				d.resolve(data);
			}, function(err){
				d.reject(err);
			});
			return d.promise();
		}
};


var id = 0;

function jsonrpc(method, params, callbackSuccess, callbackError){
	var request = {};
	request.id = id++;
	request.jsonrpc = "2.0";
	request.method = method;
	request.params = params;
	
	$.ajax({
	    url: '/dianne/jsonrpc',
	    type: 'POST',
	    dataType: 'json',
	    contentType: 'application/json-rpc',
	    data: JSON.stringify(request),
	    processData: false,
	    timeout: 15000,
	    success: function(data) {
	    	if(data.error !== undefined){
	    		if(callbackError !== undefined)
	    			callbackError(data.error.message);
	    	} else {
	    		if(callbackSuccess !== undefined)
	    			callbackSuccess(data.result);
	    	}
	    },
	    error: function(err) {
	    	if(callbackError !== undefined)
	    		callbackError(err.status);
	    }
	});

}
