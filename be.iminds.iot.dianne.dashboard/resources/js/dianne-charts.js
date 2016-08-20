

// pie chart for the system status
function createStatusChart(container, status){
	container.highcharts({
        chart: {
            plotBackgroundColor: null,
            plotBorderWidth: null,
            plotShadow: false,
            height: 220,
            type: 'pie'
        },
        legend: {
            layout: 'horizontal',
            floating: true,
            x: -10,
            y: 20
        },
        credits: {
            enabled: false
        },
        title:{
            text:''
        },
        tooltip: {
            pointFormat: '{series.name}: <b>{point.y}</b>'
        },
        plotOptions: {
            pie: {
                allowPointSelect: true,
                cursor: 'pointer',
                dataLabels: {
                    enabled: false
                },
                showInLegend: true
            }
        },
        series: [{
            name: 'Utilization',
            colorByPoint: true,
            data: [{
                name: 'Learning',
                y: status.learn
            }, {
                name: 'Idle',
                y: status.idle
            }, {
                name: 'Evaluating',
                y: status.eval
            }, {
                name: 'Acting',
                y: status.act
            }]
        }]
    });	
}

// gauge charts for cpu/memory usage
function createGaugeChart(container, name, value) {
	container.highcharts(Highcharts.merge(gaugeOptions, {
        yAxis: {
            min: 0,
            max: 100,
            title: {
                text: name
            }
        },
        series: [{
            name: name,
            data: [value],
            dataLabels: {
                format: '<div style="text-align:center"><span style="font-size:25px;color:' +
                    ((Highcharts.theme && Highcharts.theme.contrastTextColor) || 'black') + '">{y:.1f}%</span></div>'
            },
            tooltip: {
                valueSuffix: ' %'
            }
        }]
    }));
}

// create job results chart
function createResultChart(container, job, scale){
	// in case of learn chart, plot learn progress curve
	if(job.type==="LEARN"){
     	if(job.category==="RL"){
     		// in case of RL jobs, plot Q value (TODO plot 2 series error + Q?)
 	  		DIANNE.learnResult(job.id).then(function(learnprogress){
				 var q = [];
				 $.each(learnprogress, function(i) {
					 // TODO determine how to sample
					 if(i % 1000 == 0){
						 var progress = learnprogress[i];
						 q.push({
							 x: progress.iteration,
		                     y: progress.q
		                 });
					 }
				 });
				 createQChart(container, scale, q);
			});
     	} else { 
     		// else plot error value
			DIANNE.learnResult(job.id).then(function(learnprogress){
				 var miniBatchError = [];
				 var validationError = [];
				 $.each(learnprogress, function(i) {
					 // TODO determine how to sample
					 if(i % 1000 == 0){
						 var progress = learnprogress[i];
						 miniBatchError.push({
							 x: progress.iteration,
		                     y: progress.miniBatchError
		                 });
						 if(progress.validationError !== undefined){
							 validationError.push({
								 x: progress.iteration,
								 y: progress.validationError
					 	 	});
						 }
					 }
				 });
				 createErrorChart(container, scale, miniBatchError, validationError);
			});
     	}
	} else if(job.type==="EVALUATE"){
		// in case of evaluate jobs, plot a progress bar if still busy, confusion matrix heatmap otherwise
		container.empty();
		DIANNE.evaluationResult(job.id).then(function(evaluations){
			$.each(evaluations, function(i) {
				var eval = evaluations[i];
				if(eval.confusionMatrix!==undefined){
					// TODO what with multiple evaluations?
					if(eval.confusionMatrix.length <= 20)
						createConfusionChart(container, eval.confusionMatrix, scale);
					else {
						container.prepend( "<div><b>Top-5 accuracy:</b> "+eval.top5*100+" %</div><br/>" );
						container.prepend( "<div><b>Top-3 accuracy:</b> "+eval.top3*100+" %</div><br/>" );
					}
					container.prepend( "<div><b>Accuracy:</b> "+eval.accuracy*100+" %</div><br/>" );

				} else {				
					if(eval.processed!==undefined){
						createProgressBar(container, 100*eval.processed/eval.total, eval.processed+"/"+eval.total+" samples processed");
					} else if(eval.error !== undefined){
						container.prepend( "<div><b>Error:</b> "+eval.error+"</div><br/>" );
					}
				}
			});
		});
	} else if(job.type==="ACT"){
		// visualize act jobs as a running progress bar
		container.empty();
 	  	DIANNE.agentResult(job.id).then(function(results){
			$.each(results, function(i) {
				var result = results[i];
				createProgressBar($('#'+job.id+"-result"), 100, result.samples+" samples generated", job.stopped===0);
			});
		});
	}
}

function createErrorChart(container, scale, miniBatchError, validationError){
	createLineChart(container, 'Iterations', 'Error', scale, 'minibatch error', miniBatchError, 'validation error', validationError);
}

function createQChart(container, scale, q){
	createLineChart(container, 'Iterations', 'Q', scale, 'Q', q);
}

// generic line chart
function createLineChart(container, xAxis, yAxis, scale, title, data, title2, data2) {
	// if no data specified, initialize empty
	data = initializeLineData(data);
	data2 = initializeLineData(data2);
	
	if(scale === undefined){
		scale = 1;
	}
    container.highcharts({
        chart: {
            type: 'line',
            animation: false, // don't animate in old IE
            marginRight: 10,
    		height: 250*scale,
    		width: 500*scale
        },
        title : {
        	text: null
        },
        xAxis: {
            tickPixelInterval: 150,
            title: {
                text: xAxis
            },
        },
        plotOptions:{
            series:{
                turboThreshold: 1000000
            }
        },
        yAxis: {
            title: {
                text: yAxis
            },
            min: 0,
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
        credits: {
            enabled: false
        },
        series: [{
            name: title,
            data: data
        },
        {
            name: title2,
            data: data2
        }]
    });
}

function initializeLineData(data){
	var i;
	if(data===undefined){
		data = [];
	    for (i = -29; i <= 0; i += 1) {
	    	data.push({
	         	x: 0,
	        	y: null
	         });
	    }
	} else if(data.length < 30){
		// if not enough data, add some empty points
		for(i = 0; i < 30-data.length; i+=1){
	    	data.unshift({
	         	x: 0,
	        	y: null
	         });
		}
	}
	return data;
}

// confusion matrix heatmap chart
function createConfusionChart(container, matrix, scale) {
	if(scale === undefined){
		scale = 1;
	}
	// convert confusion matrix from array[x][y] to array[[x, y, val],...] 
	var data = [];
	var i,j;
	for (i = 0; i < matrix.length; i++) { 
		for (j = 0; j < matrix[i].length; j++) { 
			data.push([i, j, matrix[i][j]]);
		}
	}
	
    container.highcharts({
    	chart: {
            type: 'heatmap',
    		height: 250*scale,
    		width: 300*scale
        },
        title: {
            text: ""
        },
        credits: {
            enabled: false
        },
        colorAxis: {
            stops: [
                [0, '#3060cf'],
                [0.5, '#fffbbc'],
                [0.9, '#c4463a']
            ],
            min: 0
        },
        yAxis: {
            title: {
                text: null
            }
        }, 
        legend: {
            align: 'right',
            layout: 'vertical',
            margin: 0,
            verticalAlign: 'top'
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
            }, 
            data: data
        }]
    });
}


// progress bar
function createProgressBar(container, value, message, active){
	var progress = {};
	progress.value = value;
	if(message===undefined){
		progress.message = "";
	} else {
		progress.message = message;
	}
	if(active){
		progress.active = "progress-bar-striped active";
	} else {
		progress.active = "";
	}
	var template = $('#progress').html();
	Mustache.parse(template);
	var rendered = Mustache.render(template, progress);
	$(rendered).appendTo(container);
}


// init colors
$(function () {
	$(document).ready(function () {
		// gradient fill for pie chart
		Highcharts.getOptions().colors = Highcharts.map(Highcharts.getOptions().colors, function (color) {
	        return {
	            radialGradient: {
	                cx: 0.5,
	                cy: 0.3,
	                r: 0.7
	            },
	            stops: [
	                [0, color],
	                [1, Highcharts.Color(color).brighten(-0.3).get('rgb')] // darken
	            ]
	        };
	    });
	});
});


// gauge charts options
var gaugeOptions = {
        chart: {
            type: 'solidgauge',
            height: 200
        },
        title: null,
        pane: {
            center: ['50%', '85%'],
            size: '140%',
            startAngle: -90,
            endAngle: 90,
            background: {
                backgroundColor: (Highcharts.theme && Highcharts.theme.background2) || '#EEE',
                innerRadius: '60%',
                outerRadius: '100%',
                shape: 'arc'
            }
        },
        tooltip: {
            enabled: false
        },
        credits: {
            enabled: false
        },
        // the value axis
        yAxis: {
        	color: {
        	    radialGradient: { cx: 0.5, cy: 0.5, r: 0.5 },
        	    stops: [
        	       [0, '#003399'],
        	       [1, '#3366AA']
        	    ]
        	},
            lineWidth: 0,
            minorTickInterval: null,
            tickPixelInterval: 400,
            tickWidth: 0,
            title: {
                y: -70
            },
            labels: {
                y: 16
            }
        },
        plotOptions: {
            solidgauge: {
                dataLabels: {
                    y: 5,
                    borderWidth: 0,
                    useHTML: true
                }
            }
        }
    };