	var metricTree = null;		
	function rebuildNamespace(value) {
		value.shift(); value.shift(); value.shift(); value.shift();
		return value.join('');				
	}
	function splitRootPlus(value) {
		var frags = value.split('/');
		while(frags[0]=="") {
			frags.shift();
		}
		$.each(frags, function(index){
			frags[index] = "/" + frags[index]; 
		});
		return frags;				
	}
	function mapSize(value) {
		var cnt = 0;
		$.each(value, function(key, value){
			cnt++;
		});
		return cnt;				
	}
	
	function initMetricTree() {
		
		$('#metricTree')
		.jstree({
			core : { 
				animation : 0
			},
			json_data : {
				progressive_render : false,
				data : function(node, callback) {
					populateNode(node, callback);
				}
			},					
			plugins : [ "themes", "ui", "types", "json_data"],
			types : {
				'types' : {
		            'root' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/Helios_Symbol_12_18.png'
		                },
		                valid_children : [ 'domain' ],
		                select_node : function(me) {
		                	console.info("Selected Node:[%o]", me);
		                }				                
		            },
		            'domain' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/domain_16_16.png'
		                },
		                valid_children : [ 'domain', 'server' ],
		                select_node : function(me) {
		                	console.info("Selected Node:[%o]", me);
		                }
		            },				            
		            'server' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/server_16_16.png'
		                }, valid_children : [ 'agent', 'online-agent' ],
		                select_node : function(me) {
		                	console.info("Selected Node:[%o]", me);
		                }
		            },
		            'online-agent' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/online_agentX_16_16.png'
		                }, valid_children : [ 'metric-folder', 'folder', 'metric' ],
		                select_node : function(me) {
		                	console.info("Selected Node:[%o]", me);
		                }
		            },				            
		            'agent' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/offline_agentX_16_16.png'
		                }, valid_children : [ 'metric-folder', 'folder', 'metric' ],
		                select_node : function(me) {
		                	console.info("Selected Node:[%o]", me);
		                }
		            },
		            'metric-folder' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/chart-folder_16_16.png'
		                }, valid_children : [ 'metric-folder', 'folder', 'metric' ],
		                select_node : function(me) {
		                	console.info("Selected Node:[%o]", me);
		                }
		            },
		            'folder' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/folder_16_16.png'
		                }, valid_children : [ 'metric-folder', 'folder', 'metric' ],
		                select_node : function(me) {
		                	console.info("Selected Node:[%o]", me);
		                }				                
		            },
		            'metric' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/metric_16_16.png'
		                }, valid_children : [],
		                select_node : function(me) {
		                	console.info("Selected Node:[%o]", me);
		                }				                
		            }
		        }						
			}
		});
		metricTree = $.jstree._reference('#metricTree');
		//$.apmr.connect(); 
	}
	
	function populateNode(node, callback) {
		//console.info("Populating Node [%o]", node);
		if(node==-1) {
			callback([{
				attr: {id: "root", rel: "root"},  
				data : {title: "Helios"}
			}]);
			$("#root").removeClass('jstree-leaf').addClass('jstree-closed');
			return;
		} 				
		var rel = $(node).attr('rel');
		var nodeArray = [];
		switch(rel) {
			case 'root':
				$.apmr.allDomains(function(data) {
					$.each(data.msg, function(index, domain) {
						nodeArray.push({
							attr: {id: "domain-" + domain.replace('.', '_'), rel: "domain", 'domain' : domain},  
							data : {title: domain}									
						});	
					});
					callback(nodeArray); fixOpen(nodeArray);
				});
				break;
			case 'domain':
				var domain = $(node).attr('domain');
				//console.info("Populating Hosts in Domain [%s]", domain);
				$.apmr.hostsByDomain(domain, function(data) {
					$.each(data.msg, function(index, host) {
						nodeArray.push({
							attr: {id: "host-" + host.hostId, rel: "server", 'host' : host.hostId},  
							data : {title: host.name}									
						});	
					});
					callback(nodeArray); fixOpen(nodeArray);
				});
				break;
			case 'server':
				var host = $(node).attr('host');
				//console.info("Populating Agents in Server [%s]", host);
				$.apmr.agentsByHost(host, function(data) {
					$.each(data.msg, function(index, agent) {
						nodeArray.push({
							attr: {id: "agent-" + agent.agentId, rel: agent.conn!=null ? "online-agent" : "agent", 'agent' : agent.agentId, minl : agent.minl},  
							data : {title: agent.name}									
						});	
					});
					callback(nodeArray); fixOpen(nodeArray);
				});
				break;
			case 'agent':
			case 'online-agent':
				var agentId = $(node).attr('agent');
				var level = 0;
				var parent = "";
				$.apmr.findLevelMetricsForAgent(0, agentId, function(data) {							
					$.each(data.msg, function(index, metric) {
						nodeArray.push({
							attr: {id: "metric-" + metric.metricId, rel: "metric", 'metric' : metric.metricId, metricBody : metric},  
							data : {title: metric.name}									
						});	
					});							
					callback(nodeArray); //fixOpen(nodeArray);
					nodeArray = [];
					$.apmr.findLevelFoldersForAgent(level, agentId, parent, function(data) {
						$.each(data.msg, function(index, folder) {
							nodeArray.push({
								attr: {id: "folder-" + agentId + "-" + folder.replace('=', '_'), rel: "folder", 'folder' : folder, 'agent' : agentId, 'level' : level},  
								data : {title: folder}									
							});	
						});							
						callback(nodeArray); fixOpen(nodeArray);
					});						
				});
				break;
			case 'folder':
			case 'metric-folder':
				var agentId = $(node).attr('agent');
				var level = parseInt($(node).attr('level')) +1;
				var parent = $(node).attr('folder');
				//console.info("Populating Level Metrics and Folder [%s]", parent);
				$.apmr.findLevelMetricsForAgentWithParent(level, agentId, parent, function(data) {							
					$.each(data.msg, function(index, metric) {
						nodeArray.push({
							attr: {id: "metric-" + metric.metricId, rel: "metric", 'metric' : metric.metricId, metricBody : metric},  
							data : {title: metric.name}									
						});	
					});							
					callback(nodeArray); //fixOpen(nodeArray);
					nodeArray = [];
					$.apmr.findLevelFoldersForAgent(level, agentId, parent, function(data) {
						$.each(data.msg, function(index, folder) {
							if(folder!=null) {
								nodeArray.push({
									attr: {id: "folder-" + agentId + "-" + folder.replace('=', '_'), rel: "folder", 'folder' : folder, 'agent' : agentId, 'level' : level},  
									data : {title: folder}									
								});
							}
						});							
						if(nodeArray.length>0) { callback(nodeArray); fixOpen(nodeArray); }
					});						
				});
				break;
				
				
		}
		
	}
	function fixOpen(nodeArray) {
		$.each(nodeArray, function(index, node){
			$("#" + node.attr.id).removeClass('jstree-leaf').addClass('jstree-closed');
			
		});
	}
			
/*
		$('li.jstree-closed>a').livequery(function(){
			var node = this;
			var id = $(node).parent().attr('id');
			if(!$('#' + id).hasClass('tooltip-set')) {
				$('#' + id).addClass('tooltip-set');
				$('#' + id).qtip({
					   style: { name: 'cream', tip: true },
					   content: id,
					   show: 'mouseover',
					   hide: 'mouseout'
					})
				metricTree.get_path($('#' + id))
				console.info("CREATED NODE:[%o]", $('#metricTree').jstree("_get_node", $(node).parent()));	
			}			
		});
		$('li.jstree-open>a').livequery(function(){
			var node = this;
			var id = $(node).parent().attr('id');
			if(!$('#' + id).hasClass('tooltip-set')) {
				$('#' + id).addClass('tooltip-set');
				$('#' + id).qtip({
					   style: { name: 'cream', tip: true },
					   content: id,
					   show: 'mouseover',
					   hide: 'mouseout'
					})
				metricTree.get_path($('#' + id))				
				console.info("CREATED NODE:[%o]", $('#metricTree').jstree("_get_node", $(node).parent()));	
			}			
		});

*/	