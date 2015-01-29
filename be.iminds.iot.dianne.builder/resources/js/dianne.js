/**
 * This script allows to create a NN structure by drag-and-drop using jsPlumb
 */

// keep a model of constructed modules
var modules = {};


/*
 * jsPlumb rendering and setup
 */

// definition of source Endpoints
var source = {
	isSource:true,
	anchor : "Right",	
	paintStyle:{ 
		strokeStyle:"#555", 
		fillStyle:"#FFF", 
		lineWidth:2 
	},
	hoverPaintStyle:{
		lineWidth:3 
	},			
	connectorStyle:{
		lineWidth:4,
		strokeStyle:"#333",
		joinstyle:"round",
		outlineColor:"white",
		outlineWidth:2
	},
	connectorHoverStyle:{
		lineWidth:4,
		strokeStyle:"#555",
		outlineWidth:2,
		outlineColor:"white"
	},
//		maxConnections:-1,
}		

// the definition of target Endpoints 
var target = {
	isTarget:true,
	anchor: "Left",					
	paintStyle:{ 
		fillStyle:"#333"
	},
	hoverPaintStyle:{ 
		fillStyle: "#555"
	},
//		maxConnections:-1,
}

/**
 * On ready, fill the toolbox with available supported modules
 */
$( document ).ready(function() {
	$.post("/dianne/builder", {action : "supported-modules"}, 
		function( data ) {
			$.each(data, function(index, name){
				console.log(name);	
				// Render toolbox item
				var template = $('#toolbox-module').html();
				Mustache.parse(template);
				var rendered = Mustache.render(template, 
						{
							name: name 
						});
				$('#toolbox').append(rendered);
				
				// make draggable and add code to create new modules drag-and-drop style
				$('#'+name).draggable({helper: "clone"});
				$('#'+name).bind('dragstop', function(event, ui) {
					if(checkAddModule($(this))){
						// clone the toolbox item
					    var moduleItem = $(ui.helper).clone().removeClass("toolbox");
					    
						addModule(moduleItem, $(this));
					}
				});
			});
		}
		, "json");
});

// jsPlumb init code
jsPlumb.ready(function() {       
    jsPlumb.setContainer($("canvas"));
    jsPlumb.importDefaults({
    	ConnectionOverlays : [[ "Arrow", { location : 1 } ]],
    	Connector : [ "Flowchart", { stub:[40, 60], gap:10, cornerRadius:5, alwaysRespectStubs:true } ],
    	DragOptions : { cursor: 'pointer', zIndex:2000 },
    });		

	// suspend drawing and initialise.
	jsPlumb.doWhileSuspended(function() {
		//
		// listen for connection add/removes
		//
		jsPlumb.bind("beforeDrop", function(connection) {
			if(!checkAddConnection(connection)){
				return false;
			}
			addConnection(connection);
			return true;
		});
		
		jsPlumb.bind("beforeDetach", function(connection) {
			if(!checkRemoveConnection(connection)){
				return false;
			}
			removeConnection(connection);
			return true;
		});
	});

});

/*
 * Module configuration/deletion dialog stuff 
 */

function showConfigureModuleDialog(module){
	var dialog = $('#configureModuleDialog');
	
	var id = module.attr("id");
	dialog.find('#configure-id').val(id);
	// set configuration options
	
	dialog.modal('show');
}

$("#configure").click(function(e){
	// apply configuration
	var data = $('#configureModuleDialog').find('form').serializeArray();
	
	$('#configureModuleDialog').modal('hide');
});

$("#delete").click(function(e){
	// remove object
	var id = $('#configureModuleDialog').find('#configure-id').val();
	
	var moduleItem = $('#'+id);
	if(checkRemoveModule(moduleItem)) {
		removeModule(moduleItem);
	} 
	
	$('#configureModuleDialog').modal('hide');
});

/*
 * Module/Connection add/remove checks
 */

/**
 * Check whether one is allowed to instantiate another item from this tooblox
 */
function checkAddModule(toolboxItem){
	return true;
}

/**
 * Check whether one is allowed to remove this module
 */
function checkRemoveModule(moduleItem){
	return true;
}

/**
 * Check whether one is allowed to instantiate this connection
 */
function checkAddConnection(connection){
	return true;
}

/**
 * Check whether one is allowed to remove this connection
 */
function checkRemoveConnection(connection){
	return true;
}


/*
 * Module/Connection add/remove methods
 */

/**
 * Add a module to the canvas and to modules datastructure
 * 
 * @param moduleItem a freshly cloned DOM element from toolbox item 
 * @param toolboxItem the toolbox DOM element the moduleItem was cloned from
 */
function addModule(moduleItem, toolboxItem){
	moduleItem.appendTo("#canvas");
	 
    // fix offset of toolbox 
    var offset = {};
    offset.left = moduleItem.offset().left - ($("#canvas").offset().left - $("#toolbox").offset().left);
    offset.top = moduleItem.offset().top - ($("#canvas").offset().top - $("#toolbox").offset().top);
    moduleItem.offset(offset);
  
    // get type from toolbox item and generate new UUID
	var type = toolboxItem.attr("id");
	var id = guid();
	moduleItem.attr("id",id);
	
	// TODO this should not be hard coded?
	if(type==="Input"){
		jsPlumb.addEndpoint(moduleItem, source);
	} else if(type==="Output"){
		jsPlumb.addEndpoint(moduleItem, target);
	} else {
		jsPlumb.addEndpoint(moduleItem, source);
		jsPlumb.addEndpoint(moduleItem, target);
	}
	
	// show dialog on double click
	moduleItem.dblclick(function() {
		showConfigureModuleDialog($(this));
	});
	
	// make draggable
	moduleItem.draggable(
	{
		drag: function(){
		    jsPlumb.repaintEverything();
		}
	});
	
	// add to modules
	var module = {};
	module.type = type;
	module.id = id;
	modules[id] = module;
	
	console.log("Add module "+id);
}

/**
 * Remove a module from the canvas and the modules datastructure
 * 
 * @param moduleItem the DOM element on the canvas representing the module
 */
function removeModule(moduleItem){
	var id = moduleItem.attr("id");

	// delete this moduleItem
	$.each(jsPlumb.getEndpoints(moduleItem), function(index, endpoint){
		jsPlumb.deleteEndpoint(endpoint)}
	);
	
	jsPlumb.detachAllConnections(moduleItem);
	moduleItem.remove();

	// remove from modules
	if(modules[modules[id].next]!==undefined){
		delete modules[modules[id].next].prev;
	}
	if(modules[modules[id].prev]!==undefined){
		delete modules[modules[id].prev].next;
	}
	delete modules[id];
	
	console.log("Remove module "+id);
	
}

/**
 * Add a connection between two modules
 * @param connection to add
 */
function addConnection(connection){
	console.log("Add connection " + connection.sourceId + " -> " + connection.targetId);

	modules[connection.sourceId].next = connection.targetId;
	modules[connection.targetId].prev = connection.sourceId;
}

/**
 * Remove a connection between two modules
 * @param connection to remove
 */
function removeConnection(connection){
	console.log("Remove connection " + connection.sourceId + " -> " + connection.targetId);

	delete modules[connection.sourceId].next;
	delete modules[connection.targetId].prev;
}

/**
 * Generates a GUID string.
 * @returns {String} The generated GUID.
 * @example af8a8416-6e18-a307-bd9c-f2c947bbb3aa
 * @author Slavik Meltser (slavik@meltser.info).
 * @link http://slavik.meltser.info/?p=142
 */
function guid() {
    function _p8(s) {
        var p = (Math.random().toString(16)+"000000000").substr(2,8);
        return s ? "-" + p.substr(0,4) + "-" + p.substr(4,4) : p ;
    }
    return _p8() + _p8(true) + _p8(true) + _p8();
}
