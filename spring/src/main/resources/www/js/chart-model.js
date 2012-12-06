	var ChartModel = Object.subClass({
		init: function(json){
			this.metricId = json.msg[0];
			this.metricDef = metricModelCache[this.metricId];
			this.step = json.msg[1].step;
			this.lastSequenceNumber = -1;
			this.lastTimestamp = -1;
			this.width = json.msg[1].width;
			this.subSeries = {0:[], 1:[], 2:[], 3:[]};
			this.subscription = -1;
			this.container = null;
			this.treeClickChart = null;
			this.anim = {
    				duration: 1000,
                    easing: 'easeOutBounce'
    		};
			var st = new Date().getTime();
			this.extractSeries(json.msg[2]);   // push directly into series. no point caching.			
			var endt = new Date().getTime();
			console.info("EXS:%s ms.", (endt-st));
			metricModelCache[this.metricId] = this;
		},
		ChartModel : function(json) {
			if (!(this instanceof arguments.callee)) {
				return new ChartModel(json);
			}
		},
		extractSeries : function(rawData) {
			var cnt = 0;
			var firstTime = null;
			var lastTime = null;
			for(var i = 0, m = rawData.length; i < m; i++) {
				var ts = rawData[i][0];
				if(firstTime!=null) firstTime = ts;
				lastTime = ts;
				this.lastTimestamp = ts;
				for(var x = 0; x < 4; x++) {
					this.subSeries[x].push([ts, rawData[i][x+1]])
				}
				cnt++;
			}
			console.info("Extracted %s Values, First TS:[%s], Last TS:[%s]", cnt, new Date(firstTime), new Date(lastTime));
		},
		
		
		acceptLiveUpdate : function(t) {		
			var _this = t;
			return function(json) {
				if(json.msg.sequenceNumber<=_this.lastSequenceNumber || json.msg.timeStamp < _this.lastTimestamp) {
					//console.warn("Out of sequence event. Current: [%s],[%s]  Incoming: [%s],[%s]", new Date(_this.lastTimestamp), _this.lastSequenceNumber, new Date(json.msg.timeStamp), json.msg.sequenceNumber);
					return;
				}
				_this.lastSequenceNumber = json.msg.sequenceNumber; 
				_this.lastTimestamp = json.msg.timeStamp;
				if(json.msg.userData!=null && json.msg.userData.length >= 2 && json.msg.userData[1]==_this.metricId) {	
					console.debug("Live update for metric ID: %s, Time:[%s] Data:[%o]", json.msg.userData[1], new Date(json.msg.userData[0][0]), json.msg.userData[0]);
					var mid = json.msg.userData[1];
					var tsdata = json.msg.userData[0];
					try {
						$.each( _this.treeClickChart.series, function(index, series) {
							var shiftPoints = series.data.length>=_this.width;
//							var viz = series.visible;
//							if(shiftPoints && !viz) series.show();
							series.addPoint([tsdata[0], tsdata[index+1]], true, shiftPoints, false);
							_this.treeClickChart.redraw();
//							if(shiftPoints && !viz) series.hide();
						});
//						$.each( _this.subSeries, function(index, series) {
//							var shiftPoints = series.length>=_this.width;
//							series.push([tsdata[0], tsdata[index+1]]);
//							if(shiftPoints) series.shift();
//							_this.treeClickChart.series[index].
//						});
						
					} catch (e) {
						console.error("Failed to process live update Error was [%o], %o", e, e.stack);
					}
					
					_this.treeClickChart.redraw();
				}
			}
			
		},
		renderChart : function(props) {
    		
    		//metricModelCache
    		if(this.treeClickChart == null ) {
    			this.container = 'chart-container-metric-' + this.metricId;
    			$('#chartContainer>.ChartModel').hide();
    			$('#chartContainer').append(
    					$('<div id="' + this.container + '" class="ChartModel"></div>')
    			);
    			var cont = this.container;
	    		this.treeClickChart = new Highcharts.Chart({
	    	        chart: {
	    	            renderTo: cont,
	    	            animation: false,
	    	            zoomType: 'xy'
	    	        },
	    	        xAxis: {
	    	            type: 'datetime',
	    	            minRange: 15000
	    	        },
	    	        credits: {
	    	            enabled: false
	    	        },	    
	    	        exporting: {
	    	            enabled: true,
	    	            //url: 'http://' + window.document.domain + ':8161/highcharts'
	    	            url : 'http://10.230.13.67:8161/highcharts/'
	    	        },	    	        
	    	        yAxis: [
	    	            {title: ''},
	    	            {title: {text : 'Invocations'}, opposite: true}
	    	            
	    	        ],			
	    	        subtitle : {
	    	        	text : (this.metricDef.ag.host.name + ':' + this.metricDef.ag.name)
	    	        },
	    	        loading: {
	    	        	showDuration: 0
	    	        },
	                tooltip: {
	                    formatter: function() {
	                            return '<b>'+ this.series.name +'</b><br/>'+
	                            Highcharts.dateFormat('%Y-%m-%d %H:%M:%S', this.x) +'<br/>'+
	                            Highcharts.numberFormat(this.y, 2) +'<br/>'+
	                            "Length:" + this.series.data.length;
	                            /*
	                             * Need to format like this:
	                             * ==========================
	                             * Fully Qualfied Name
	                             * Timestamp
	                             * Min
	                             * Max
	                             * Avg
	                             * Cnt 
	                             * ==========================
	                             * this.series.index is the index of the series.
	                             */
	                    }
	    	        
	                },    	        
	    	        title: {
	    	            text: this.metricDef.ns + '/' + this.metricDef.name + '  [' + this.metricId + ']' 
	    	        },
	    	        series: [
	    	            {id: 'series-Min-' + this.metricId, name: this.metricDef.name + " Min", data: this.subSeries[ChartModel.MIN], animation: false, visible: false},
	    	            {id: 'series-Max-' + this.metricId, name: this.metricDef.name + " Max", data: this.subSeries[ChartModel.MAX], animation: false, visible: false},
	    	            {id: 'series-Avg-' + this.metricId, name: this.metricDef.name + " Avg", data: this.subSeries[ChartModel.AVG], animation: false},
	    	            {id: 'series-Cnt-' + this.metricId, name: this.metricDef.name + " Cnt", data: this.subSeries[ChartModel.CNT], animation: false, visible: false, yAxis: 1, dashStyle : 'Dash'}
	    	        ]
	    	    });
	    		var an = this.anim;
	    		for(var i = 0, m = this.treeClickChart.series.length; i < m; i++) {
	    			this.treeClickChart.series[i].options.animation = an;
	    		}
	    		//this.subSeries = [];

	    		this.treeClickChart.series.metricId = this.metricId;
	    		if(props.auto || false) {
	    			this.subscription = $.apmr.subMetricOn(this.metricId, this.acceptLiveUpdate(this));
	    			console.info("Subscribed to Metric [%s]", this.metricId);
	    		}
	    		var chartie = this.treeClickChart;
	    		var parent = $('#' + this.container).parent();
	    		$('#' + this.container).resizable({
	                handles: 's',
	                stop: function(event, ui) {
	                    $(this).css("width", '');
	                    chartie.setSize($(this).width(), $(this).height());
	               }
	            });        

	    		parent.resize(function(e){
	    			console.info("Resizing [%s]", chartie);
	    			chartie.setSize(parent.width(), parent.height());
	    			//chartie.redraw();
	    		});
			} else {
				$('#chartContainer>.ChartModel').hide();
				$('#' + this.container).show();
			}
		}, 
		destroy : function() {
			$.apmr.subMetricOff(this.subscription);
			$('#' + this.container).remove();
			console.info("Removed chart [%s]", '#' + this.container);			
			this.treeClickChart.destroy();
			console.info("Destroyed chart for metric [%s]", this.metricId);
		}
	});
	
	ChartModel.MIN = 0;
	ChartModel.MAX = 1;
	ChartModel.AVG = 2;
	ChartModel.CNT = 3;
	ChartModel.DECODE = {'MIN':0, 'MAX':1, 'AVG':2, 'CNT':3}
	ChartModel.XDECODE = {0:'MIN', 1:'MAX', 2:'AVG', 3:'CNT'}
	
	ChartModel.find = function(metricId, callback) {
		// Check the cache and type of cached object
		var o = metricModelCache[metricId];
		if(o!=null && (o instanceof ChartModel)) {
			if(callback!=null) {
				callback(o);				
			}
			return o;
		}
		if(o==null) {  // we don't have the metricDef
			$.apmr.metricById(metricId, function(json){
				var metricDef = json.msg[0];
				metricModelCache[metricId] = metricDef;
				$.apmr.liveData(metricId, function(tsdata){
					var model = new ChartModel(tsdata);
					if(callback!=null) {
						callback(model);
					}
				});
			});
		} else {
			$.apmr.liveData([metricId], function(tsdata){		                		
				var model = new ChartModel(tsdata);
				if(callback!=null) {
					callback(model);
				}
			});
		}
	}
