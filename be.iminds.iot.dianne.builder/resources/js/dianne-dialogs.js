/*
 * Module configuration/deletion dialog stuff 
 */
 
 var dialogZIndex = 1040;

/**
 * Show a dialog for a given module, will forward to right function depending on currentMode
 */
function showConfigureModuleDialog(moduleItem) {
	var id = moduleItem.attr("id");
	// there can be only one dialog at a time for one module
	// try to reuse dialog
	var dialogId = "dialog-" + id;
	var dialog;
	dialog = $("#" + dialogId);
	if (dialog.length == 0) {
		
		if (currentMode === "build") {
			dialog = createBuildModuleDialog(id, moduleItem);
		} else if (currentMode === "deploy") {
			dialog = createDeployModuleDialog(id, moduleItem);
		} else if (currentMode === "learn") {
			dialog = createLearnModuleDialog(id, moduleItem);
		} else if (currentMode === "run") {
			dialog = createRunModuleDialog(id, moduleItem);
		}
		
		if (dialog !== undefined) {
			var offset = moduleItem.offset();
			offset.top = offset.top - 100;
			offset.left = offset.left - 200;
			// show the modal (disable backdrop)
			dialog.draggable({
				handle : ".modal-header"
			}).mousedown(function(){
	   			// set clicked element to a higher level
	   			$(this).css('z-index', ++dialogZIndex);
			}).offset(offset);
		}
	}
	
	if (dialog !== undefined) {
		dialog.modal({
			'show' : true,
			'backdrop' : false
		}).css('z-index', ++dialogZIndex);
	}
}

/*
 * Helper function to create base dialog and show the Module div
 * Base for each NN configure module dialog in each mode
 */
function createNNModuleDialog(module, title, submit, cancel){
	var dialog = renderTemplate("dialog", {
		'id' : module.id,
		'title' : title,
		'submit': submit,
		'cancel': cancel
	}, $(document.body));
	
	// add module div to dialog to show which module to configure
	renderTemplate("module",
			{	
				name: module.type,
				type: module.type, 
				category: module.category
			}, 
			dialog.find('.content'));
	
	return dialog;
}

/**
 * Create dialog for configuring module in build mode
 */
function createBuildModuleDialog(id, moduleItem){
	var module = nn.modules[id];
	
	var dialog = createNNModuleDialog(module, "Configure module ", "Configure", "Delete");
	
	// then fill in properties
	$.post("/dianne/builder", {"action" : "module-properties","type" : module.type}, 
			function( data ) {
				$.each(data, function(index, property){
					// Render toolbox item
					renderTemplate('form-item',
						{
							name: property.name,
							id: property.id,
							value: module[property.id]
						}, dialog.find('.form-items'));
				});
				if (data.length === 0) {
					dialog.find('.form-items').append("<p>No properties to configure...</p>");
				}
			}
			, "json");
	
	// set button callbacks, disable buttons when module is deployed
	if(deployment[id]!==undefined){
		dialog.find(".submit").prop('disabled', true);
		dialog.find(".cancel").prop('disabled', true);
	} else {
		dialog.find(".submit").click(function(e){
			// apply configuration
			var data = $(this).closest('.modal').find('form').serializeArray();
			
			var module;
			$.each( data, function( i, item ) {
				if(i === 0){
					module = nn.modules[item.value];
				} else {
					module[item.name] = item.value;
				}
			});
			
			$(this).closest(".modal").modal('hide');
		});
		
		dialog.find(".cancel").click(function(e){
			// remove object
			var id = $(this).closest(".modal").find(".module-id").val();
			
			var moduleItem = $('#'+id);
			if(checkRemoveModule(moduleItem)) {
				removeModule(moduleItem);
			}
			
			// remove dialog when module is removed, else keep it for reuse
			$(this).closest(".modal").remove();
		});
	}
	
	return dialog;
}


/**
 * Create dialog for configuring module in deploy mode
 */
function createDeployModuleDialog(id, moduleItem){
	var module = nn.modules[id];
	
	var dialog = createNNModuleDialog(module, "Deploy module ", "Deploy", "Undeploy");
	
	// fill in deployment options
	if(deployment[id]===undefined){
		renderTemplate("form-dropdown", 
				{	
					name: "Deploy to: "
				},
				dialog.find('.form-items'));
		$.post("/dianne/deployer", {"action" : "targets"}, 
				function( data ) {
					$.each(data, function(index, target){
						dialog.find('.options').append("<option value="+target+">"+target+"</option>")
					});
				}
				, "json");
	} else {
		dialog.find('.form-items').append("<p>This module is deployed to "+deployment[id]+"</p>");
	}
	
	// add button callbacks
	if(deployment[id]===undefined){
		dialog.find(".submit").click(function(e){
			// deploy this module
			var id = $(this).closest(".modal").find(".module-id").val();
			var target = $(this).closest('.modal').find('.options').val();
			
			deploy(id, target);
			
			$(this).closest(".modal").remove();
		});
		dialog.find(".cancel").prop('disabled', true);
	} else {
		dialog.find(".cancel").click(function(e){
			// undeploy this module
			var id = $(this).closest(".modal").find(".module-id").val();
			undeploy(id);
			
			$(this).closest(".modal").remove();
		});
		dialog.find(".submit").prop('disabled', true);
	}
	
	return dialog;
}


/**
 * Create dialogs for learning modules
 */
function createLearnModuleDialog(id, moduleItem){
	var module = learning[id];
	if(module===undefined){
		module = nn.modules[id];
		
		if(module.trainable!==undefined){
			var dialog = createNNModuleDialog(module, "Configure module", "Save", "");
			dialog.find(".cancel").remove();
			
			var train = "";
			if(module.trainable==="true"){
				train = "checked";
			}
			renderTemplate("form-checkbox", 
					{	
						name: "Train",
						id: "trainable",
						checked: train
					},
					dialog.find('.form-items'));
			
			dialog.find(".submit").click(function(e){
				// apply training configuration
				var id = $(this).closest(".modal").find(".module-id").val();
				var train = $(this).closest(".modal").find(".trainable").is(':checked');
				if(train){
					nn.modules[id].trainable = "true";
				} else {
					nn.modules[id].trainable = "false";
				}
				
				$(this).closest(".modal").modal('hide');
			});
			
			return dialog; 
			
		} else if(module.category==="Fork"
					|| module.category==="Join"){
			
			var dialog = createNNModuleDialog(module, "Configure module", "Save", "");
			dialog.find(".cancel").remove();

			renderTemplate("form-dropdown", 
				{	
					name: "Mode"
				},
				dialog.find('.form-items'));
			
			dialog.find('.options').append("<option value=\"FORWARD_ON_CHANGE\">Forward on change</option>");
			dialog.find('.options').append("<option value=\"WAIT_FOR_ALL\">Wait for all input/gradOutput</option>");
			dialog.find('.options').change(function(event){
				var selected = dialog.find( "option:selected" ).val();
				var id = dialog.find(".module-id").val();

				// weird to do this with run, but actually makes sense to set runtime mode in run servlet?
				$.post("/dianne/run", {"mode":selected, "target":id, "id": nn.id}, 
						function( data ) {
						}
						, "json");
				
				$(this).closest(".modal").modal('hide');
			});
			
			return dialog;
		} else {
			// no dialogs for untrainable modules
			return undefined;
		}
	}
	
	var dialog;
	if(module.category==="Dataset"){
		dialog = renderTemplate("dialog", {
			id : id,
			title : "Configure "+module.type+" dataset",
			submit: "",
			cancel: "Delete"
		}, $(document.body));
		
		renderTemplate("dataset-learn", {
				id : module.id,
				dataset : module.dataset,
				train: module.train,
				test: module.test,
				validation: module.validation
			},
			dialog.find('.content')
		);
		
	
		$.each(datasets[module.dataset].labels, function(index, label){
			if($.inArray(label, module.labels) > -1){
				dialog.find('.labels').append(
					'<label class="checkbox-inline"><input type="checkbox" checked value="'+label+'">'+label+'</label>'
				);
			} else {
				dialog.find('.labels').append(
					'<label class="checkbox-inline"><input type="checkbox" value="'+label+'">'+label+'</label>'
				);		
			}
		});
		dialog.find('.labels').append(
			'<label class="checkbox-inline"><input type="checkbox" value="other">other</label>'
		);
		dialog.find(':checkbox').change(function() {
			var labels = [];
			var dialog = $(this.closest('.modal'));
			var id = dialog.find('.module-id').val();
			dialog.find(':checkbox').each(function(index, checkbox){
				if($(checkbox).is(':checked')){
					labels.push($(checkbox).val());
				}
			});
			learning[id].labels = labels;
		});
	
		dialog.find(".slider").slider({
			orientation: "vertical",
			range: true,
			max: module.total,
			min: 0,
			step: 1000,
			values: [ module.validation, module.test+module.validation ],
			slide: function( event, ui ) {
				var h1 = parseInt(ui.values[0]);
				var h2 = parseInt(ui.values[1]);
				module.validation = h1;
				module.test = h2-h1;
				module.train = module.total-h2;
				
				// TODO dont use ids here?
				$('#validation').text(module.validation);
				$('#train').text(module.train);
				$('#test').text(module.test);
			}
		}).find(".ui-slider-handle").remove();
		
		// TODO make this a shuffle button?
		dialog.find(".submit").remove();
	
	} else if(module.category==="Trainer"){
		dialog = renderTemplate("dialog", {
			id : id,
			title : "Train your network",
			submit: "Train",
			cancel: "Delete"
		}, $(document.body));
		
		
		// form options
		// TODO fetch parameters from server?
		renderTemplate("form-train", {
				id : module.id,
				loss : module.loss,
				batch: module.batch,
				epochs: module.epochs,
				learningRate: module.learningRate,
				learningRateDecay: module.learningRateDecay
			},
			dialog.find('.form-items'));
		
		
		dialog.find(".submit").click(function(e){
			var id = $(this).closest(".modal").find(".module-id").val();
			
			var trainer = learning[id];
			trainer.loss = $(this).closest(".modal").find("#loss").val();
			trainer.batch = $(this).closest(".modal").find("#batch").val();
			trainer.epochs = $(this).closest(".modal").find("#epochs").val();
			trainer.learningRate = $(this).closest(".modal").find("#learningRate").val();
			trainer.learningRateDecay = $(this).closest(".modal").find("#learningRateDecay").val();

			learn(id);
		});
	} else if(module.category==="Evaluator"){
		dialog = renderTemplate("dialog", {
			id : id,
			title : "Evaluate your network",
			submit: "Evaluate",
			cancel: "Delete"
		}, $(document.body));
				
		// confusion chart and accuracy div
		createConfusionChart(dialog.find(".content"));
		dialog.find(".content").append("<div class=\"accuracy\"></div>")
		
		dialog.find(".submit").click(function(e){
			var id = $(this).closest(".modal").find(".module-id").val();

			evaluate(id);
		});
	}

	// delete module on cancel
	dialog.find(".cancel").click(function(e){
		// remove object
		var id = $(this).closest(".modal").find(".module-id").val();
		
		var moduleItem = $('#'+id);
		if(checkRemoveModule(moduleItem)) {
			removeModule(moduleItem);
		}
		
		// remove dialog when module is removed, else keep it for reuse
		$(this).closest(".modal").remove();
	});
	
	return dialog;
}


/**
 * create dialogs for run modules
 */
function createRunModuleDialog(id, moduleItem){
	var module = running[id];
	if(module===undefined){
		return undefined; // no dialogs for build modules
	}
	
	var dialog;
	if(module.type==="CanvasInput"){
		dialog = renderTemplate("dialog", {
			id : id,
			title : "Draw your input",
			submit: "",
			cancel: "Delete"
		}, $(document.body));
		
		dialog.find(".content").append("<canvas class='inputCanvas' width='224' height='224' style=\"border:1px solid #000000; margin-left:150px\"></canvas>");
		dialog.find(".content").append("<button class='btn' onclick='clearCanvas()' style=\"margin-left:10px\">Clear</button>");
		
		inputCanvas = dialog.find('.inputCanvas')[0];
		inputCanvasCtx = inputCanvas.getContext('2d');

		inputCanvasCtx.lineWidth = 15;
		inputCanvasCtx.lineCap = 'round';
		inputCanvasCtx.lineJoin = 'round';
		
		inputCanvas.addEventListener('mousemove', moveListener, false);
		inputCanvas.addEventListener('touchmove', touchMoveListener, false);
		inputCanvas.addEventListener('mousedown', downListener, false);
		inputCanvas.addEventListener('touchstart', downListener, false);
		inputCanvas.addEventListener('mouseup', upListener, false);
		inputCanvas.addEventListener('touchend', upListener, false);
		
		
	} else if(module.type==="ProbabilityOutput"){
		dialog = renderTemplate("dialog", {
			id : id,
			title : "Output probabilities",
			submit: "",
			cancel: "Delete"
		}, $(document.body));
		
		createOutputChart(dialog.find(".content"));
		if(eventsource===undefined){
			eventsource = new EventSource("run");
			eventsource.onmessage = function(event){
				var data = JSON.parse(event.data);
				$.each(running, function(id, module){
					// choose right RunOutput to set the chart of
					if(module.output===data.id){
						var attr = $("#dialog-"+module.id).find(".content").attr("data-highcharts-chart");
						if(attr!==undefined){
							var index = Number(attr);
							// data.output is tensor representation as string, should be parsed first

							console.log(data.tags+" "+data.output);
							if(data.tags.length != 0){
								Highcharts.charts[index].setTitle({ text: JSON.stringify(data.tags)});
							}
							Highcharts.charts[index].series[0].setData(data.output, true, true, true);
							Highcharts.charts[index].xAxis[0].setCategories(data.labels);
						}
					}
				});
			};
		}
		
		dialog.on('hidden.bs.modal', function () {
			// TODO what if multiple dialogs?
		    eventsource.close();
		    eventsource = undefined;
		    $(this).closest(".modal").remove();
		});
		
	} else if(module.category==="Dataset"){
		dialog = renderTemplate("dialog", {
			id : id,
			title : "Input a sample of the "+module.type+" dataset",
			submit: "",
			cancel: "Delete"
		}, $(document.body));
		
		dialog.find(".content").append("<canvas class='sampleCanvas' width='256' height='256' style=\"border:1px solid #000000; margin-left:150px\"></canvas>");
		dialog.find(".content").append("<button class='btn' onclick='sample(\""+module.type+"\",\""+module.input+"\")' style=\"margin-left:10px\">Sample</button>");
		
		sampleCanvas = dialog.find('.sampleCanvas')[0];
		sampleCanvasCtx = sampleCanvas.getContext('2d');
		
	} else if(module.type==="Camera"){
		dialog = renderTemplate("dialog", {
			id : id,
			title : "Camera input from "+module.name,
			submit: "",
			cancel: "Delete"
		}, $(document.body));
		
		dialog.find(".content").append("<canvas class='cameraCanvas' width='256' height='256' style=\"border:1px solid #000000; margin-left:150px\"></canvas>");

		cameraCanvas = dialog.find('.cameraCanvas')[0];
		cameraCanvasCtx = cameraCanvas.getContext('2d');
		
		if(cameraEventsource===undefined){
			cameraEventsource = new EventSource("input");
			cameraEventsource.onmessage = function(event){
				var data = JSON.parse(event.data);
				render(data, cameraCanvasCtx);
			};
		}
		
		dialog.on('hidden.bs.modal', function () {
		    cameraEventsource.close();
		    cameraEventsource = undefined;
		    $(this).closest(".modal").remove();
		});
	} else if(module.type==="URLInput"){
		dialog = renderTemplate("dialog", {
			id : id,
			title : "Give an URL to forward",
			submit: "",
			cancel: "Delete"
		}, $(document.body));
		
		dialog.find(".content").append("<img class='inputImage' width='224' height='224' style=\"margin-left:150px\"></img><br/><br/>");
		dialog.find(".content").append("<input class='urlInput' size='50' value='http://'></input>");
		dialog.find(".content").append("<button class='btn' onclick='forwardURL(this, \""+module.input+"\")' style=\"margin-left:10px\">Forward</button>");
	} else {
		dialog = createNNModuleDialog(module, "Configure run module", "", "Delete");
	}
	
	dialog.find(".cancel").click(function(e){
		// remove object
		var id = $(this).closest(".modal").find(".module-id").val();
		
		var moduleItem = $('#'+id);
		if(checkRemoveModule(moduleItem)) {
			removeModule(moduleItem);
		}
		
		// remove dialog when module is removed, else keep it for reuse
		$(this).closest(".modal").remove();
	});
	
	// submit button not used atm
	dialog.find(".submit").remove();
	
	return dialog;
}

var inputCanvas;
var inputCanvasCtx;
var mousePos = {x: 0, y:0};

var sampleCanvas;
var sampleCanvasCtx;

// TODO can have multiple camera inputs...
var cameraCanvas;
var cameraCanvasCtx;

function downListener(e) {
	e.preventDefault();
	inputCanvasCtx.moveTo(mousePos.x, mousePos.y);
	inputCanvasCtx.beginPath();
	inputCanvas.addEventListener('mousemove', onPaint, false);
	inputCanvas.addEventListener('touchmove', onPaint, false);
}

function upListener(e) {
	inputCanvas.removeEventListener('mousemove', onPaint, false);
	inputCanvas.removeEventListener('touchmove', onPaint, false);
	// get input 
	var canvasInputId = $(e.target.closest('.modal-body')).find('.module-id').val();
	forwardCanvasInput(running[canvasInputId].input);
}

function moveListener(e) {
	var dialog = inputCanvas.closest(".modal");
	mousePos.x = e.pageX - inputCanvas.offsetLeft - dialog.offsetLeft;
	mousePos.y = e.pageY - inputCanvas.offsetTop - dialog.offsetTop - 75;
}

function touchMoveListener(e) {
	var touches = e.targetTouches;
	var dialog = inputCanvas.closest(".modal");
	mousePos.x = touches[0].pageX - inputCanvas.offsetLeft - dialog.offsetLeft;
	mousePos.y = touches[0].pageY - inputCanvas.offsetTop - dialog.offsetTop - 75;
}

function onPaint() {
	// paint to big canvas
	inputCanvasCtx.lineTo(mousePos.x, mousePos.y);
	inputCanvasCtx.stroke();
}

function clearCanvas() {
	inputCanvasCtx.clearRect(0, 0, 224, 224);
}

function forwardCanvasInput(input){
	var array = [];
	var imageData = inputCanvasCtx.getImageData(0, 0, 224, 224);
    var data = imageData.data;
    
    // TODO hard coded for MNIST right now
    var sample = {};
    sample.width = 28;
    sample.height = 28;
    sample.channels = 1;

	for (var y = 0; y < 224; y+=8) {
        for (var x = 0; x < 224; x+=8) {
        	// collect alpha values
        	array.push(imageData.data[y*224*4+x*4+3]/255);
        }
    }
	sample.data = array;
	
	$.post("/dianne/run", {"forward":JSON.stringify(sample), "input":input, "id":nn.id}, 
			function( data ) {
			}
			, "json");
}

function forwardURL(btn, input){
	var url = $(btn).closest(".modal").find(".urlInput").val();
	
	$(btn).closest(".modal").find(".inputImage").attr("src", url);
	
	$.post("/dianne/run", {"url":url, "input":input, "id":nn.id}, 
			function( data ) {
			}
			, "json");
}

function sample(dataset, input){
	$.post("/dianne/run", {"dataset":dataset,"input":input, "id": nn.id}, 
			function( sample ) {
				render(sample, sampleCanvasCtx);
			}
			, "json");
}

function render(tensor, canvasCtx){
	canvasCtx.clearRect(0,0,256,256);
	
	var scaleX = 256/tensor.width;
	var scaleY = 256/tensor.height;
	var scale = scaleX < scaleY ? scaleX : scaleY;
	
	var width = Math.round(tensor.width*scale);
	var height = Math.round(tensor.height*scale);
	var imageData = canvasCtx.createImageData(width, height);
	
	if(tensor.channels===1){
		for (var y = 0; y < height; y++) {
	        for (var x = 0; x < width; x++) {
	        	// collect alpha values
	        	var x_s = Math.floor(x/scale);
	        	var y_s = Math.floor(y/scale);
	        	var index = y_s*tensor.width+x_s;
	        	imageData.data[y*width*4+x*4+3] = Math.floor(tensor.data[index]*255);
	        }
	    }
	} else if(tensor.channels===3){
		// RGB
		for(var c = 0; c < 3; c++){
			for (var y = 0; y < height; y++) {
		        for (var x = 0; x < width; x++) {
		        	var x_s = Math.floor(x/scale);
		        	var y_s = Math.floor(y/scale);
		        	var index = c*tensor.width*tensor.height + y_s*tensor.width+x_s;
		        	imageData.data[y*width*4+x*4+c] = Math.floor(tensor.data[index]*255);
		        }
		    }		
		}
		for (var y = 0; y < height; y++) {
	        for (var x = 0; x < width; x++) {
	        	imageData.data[y*width*4+x*4+3] = 255;
	        }
		}
	}
	
	var offsetX = Math.floor((256-width)/2);
	var offsetY = Math.floor((256-height)/2);
	canvasCtx.putImageData(imageData, offsetX, offsetY); 
}


/*
 * Deploy the modules
 */

function deployAll(){
	$.post("/dianne/deployer", {"action":"deploy",
			"name":nn.name,
			"modules":JSON.stringify(nn.modules),
			"target":selectedTarget}, 
			function( data ) {
				nn.id = data.id;
				$.each( data.deployment, color);
			}
			, "json");
}

function undeployAll(){
	$.each(deployment, function(id,value){
		undeploy(id);
	});
}

function deploy(id, target){
	$.post("/dianne/deployer", {"action":"deploy", 
		"id": nn.id,
		"name":nn.name,
		"module":JSON.stringify(nn.modules[id]),
		"target": target}, 
			function( data ) {
				nn.id = data.id;
				$.each( data.deployment, color );
			}
			, "json");
}

function undeploy(id){
	$.post("/dianne/deployer", {"action":"undeploy","id":nn.id,"moduleId":id}, 
			function( data ) {
				deployment[id] = undefined;
				$("#"+id).css('background-color', '');
			}
			, "json");
}

function color(id, target){
	deployment[id] = target;
	var c = deploymentColors[target]; 
	if(c === undefined){
		c = nextColor();
		deploymentColors[target] = c;
	}
	$("#"+id).css('background-color', c);
}

/*
 * Learning functions
 */

function learn(id){
	// first create the chart
	createErrorChart($("#dialog-"+id).find(".content"));

	eventsource = new EventSource("learner");
	eventsource.onmessage = function(event){
		var data = JSON.parse(event.data);
		var index = Number($("#dialog-"+id).find(".content").attr("data-highcharts-chart"));
    	var x = Number(data.sample);
        var y = Number(data.error); 
		Highcharts.charts[index].series[0].addPoint([x, y], true, true, false);
	};
	
	var modules = [];
	$.each(nn.modules, function(id, module){
		if(module.category==="Input-Output" 
			|| module.category==="Preprocessing"){
			modules.push(id);
		} else {
			if(module.trainable==="true"){
				modules.push(id);
			}
		}
	});
	
	$.post("/dianne/learner", {"action":"learn",
		"id": nn.id,
		"config":JSON.stringify(learning),
		"modules": JSON.stringify(modules),
		"target": id}, 
			function( data ) {
				// only returns labels of output module
				$.each(data, function(id, labels){
					nn.modules[id].labels = labels;
				});
				eventsource.close();
				eventsource = undefined;
			}
			, "json");
}

function evaluate(id){
	// reset chart
	var index = Number($("#dialog-"+id).find(".content").attr("data-highcharts-chart"));
	Highcharts.charts[index].series[0].setData(null, true, true, false);
	$("#dialog-"+id).find(".accuracy").text("");

	eventsource = new EventSource("learner");
	eventsource.onmessage = function(event){
		var data = JSON.parse(event.data);
		var index = Number($("#dialog-"+id).find(".content").attr("data-highcharts-chart"));
		Highcharts.charts[index].series[0].setData(data, true, true, false);
	}
	$.post("/dianne/learner", {"action":"evaluate",
		"id": nn.id,
		"config":JSON.stringify(learning),
		"target": id}, 
			function( data ) {
				eventsource.close();
				eventsource = undefined;
				$("#dialog-"+id).find(".accuracy").text("Accuracy: "+data.accuracy+" %");
				
				var index = Number($("#dialog-"+id).find(".content").attr("data-highcharts-chart"));
				Highcharts.charts[index].series[0].setData(data.confusionMatrix, true, true, false);
			}
			, "json");
}

/*
 * SSE for feedback when training/running
 */
var eventsource;
var cameraEventsource

if(typeof(EventSource) === "undefined") {
	// load polyfill eventsource library
	$.getScript( "js/eventsource.min.js").done(function( script, textStatus ) {
		console("Fallback to eventsource.js for SSE...");
	}).fail(function( jqxhr, settings, exception ) {
		console.log("Sorry, your browser does not support server-sent events...");
	});
} 



/*
 * Charts
 */

function createOutputChart(container) {
    container.highcharts({
        chart: {
            type: 'column',
    		height: 300,
    		width: 500
        },
        title: {
            text: null
        },
        xAxis: {
            type: 'category',
            labels: {
                rotation: -45
            }
        },
        yAxis: {
            min: 0,
            max: 1,
            title: {
                text: null
            }
        },
        legend: {
            enabled: false
        },
        series: [{
            name: 'Output'
        }]
    });
}


function createErrorChart(container) {
    container.highcharts({
        chart: {
            type: 'line',
            animation: false, // don't animate in old IE
            marginRight: 10,
    		height: 200,
    		width: 500
        },
        title : {
        	text: null
        },
        xAxis: {
            tickPixelInterval: 150
        },
        yAxis: {
            title: {
                text: 'Error'
            },
            max: 1,
            floor: 0,
            plotLines: [{
                value: 0,
                width: 1,
                color: '#808080'
            }]
        },
        legend: {
            enabled: false
        },
        exporting: {
            enabled: false
        },
        series: [{
            name: 'Data',
            data: (function () {
                // generate an array of empty data
                var data = [],i;
                for (i = -19; i <= 0; i += 1) {
                    data.push({
                        x: 0,
                        y: null
                    });
                }
                
                return data;
            }())
        }]
    });
}

function createConfusionChart(container) {
    container.highcharts({
    	chart: {
            type: 'heatmap',
    		height: 500,
    		width: 500
        },
        title: {
            text: "Confusion Matrix"
        },
        colorAxis: {
            stops: [
                [0, '#3060cf'],
                [0.5, '#fffbbc'],
                [0.9, '#c4463a']
            ],
            min: 0
//            min: 0,
//            minColor: Highcharts.getOptions().colors[0],
//            maxColor: '#FFFFFF'
        },
        yAxis: {
            title: {
                text: null
            }
        },
        series: [{
            name: 'Confusion matrix',
            borderWidth: 0,
            dataLabels: {
                enabled: false,
                color: 'black',
                style: {
                    textShadow: 'none',
                    HcTextStroke: null
                }
            }
        }]
    });
}