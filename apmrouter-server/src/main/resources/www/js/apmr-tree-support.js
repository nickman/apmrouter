	var metricTree = null;		
	var treePendingInserts = {};
	var metricModelCache = {};
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
	
	function reapDownedNodes() {
		var callback = reapDownedNodes;
		if($.apmr.config.unloadDownNodes) {
			try {
				var timeNow = new Date().getTime();
				var cnt = 0;
				//console.info("Downed Node Reaper Running....");
				
				$('[apmr_dnode_downTime]').each(function(index, node){
					var dt = parseInt($(node).attr('apmr_dnode_downTime'));
					if(timeNow-dt>=$.apmr.config.downedNodeLingerTime) {
						console.info("Reaping Node [%o]", node);
						$('#metricTree').jstree("remove", $(node).attr('id'));
						$(node).remove();
						cnt++;
					}
				});
				if(cnt>0) {
					console.info("Reaper Removed [%s] Downed Nodes", cnt);
				}
			} catch (e) {
				console.error("ReapDownedNodes Failed:[%o],%o", e, e.stack);
				
			}
			$.apmr.config.downedNodeReaper = setTimeout(reapDownedNodes, 2000);
		}
	}
	
	function initResizes() {
        $("div.column-divider").draggable({
            axis: 'x', // only allow horizontal movement
            drag: function(event, ui) {
              // adjust column1 as they drag
              $('div.column1').width($(this).offset().left);
              $('div.column2').css('margin-left',
                  $(this).offset().left + $(this).width());
            },
            stop: function(event, ui) {
            	var cwidth = parseInt($(this).parent().width());
            	// one more time to ensure it's right when they stop
              $('div.column1').width($(this).offset().left);
              //$('div.column2').css('margin-left', $(this).offset().left + cwidth);
              //$('#chartContainer').css('width',cwidth);
              console.info("Resizing %s Charts", $('#metricDisplayChart>.ChartModel').length);              
              $('#metricDisplayChart>.ChartModel').css('width',(cwidth-$(this).offset().left));
              $('#metricDisplayChart>.ChartModel').resize();
              //treeClickChart.setSize($('#chartContainer').parent().width(), 400, false);
              
            }
          });
	}
	
	function initMetricTree() {
		$.apmr.config.downedNodeReaper = setTimeout(reapDownedNodes, 2000);
		$('#metricTree').bind("loaded.jstree", function (event, data) {
			
		});

		$('#metricTree')
		.jstree({
			core : { 
				load_open : true,
				open_parents : true,
				animation : 0
			},
			json_data : {
				progressive_render : true,
				progressive_unload: true,
				data : function(node, callback) {
					populateNode(node, callback);
				}
			},					
			plugins : [ "themeroller", "themes", "ui", "types", "json_data", "crrm", "unique", "cookies"],
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
		                valid_children : [ 'domain', 'server', 'down-server' ],
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
		            'down-server' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/down_server.png'
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
		                	var _path = metricTree.get_path($(me));
		                	_path.shift();
		                	var gridKey = _path.join('/');
		                	onSelectedMetricFolder(gridKey);
		                	
//		    				var agentId = $(node).attr('agent');
//		    				var level = parseInt($(node).attr('level')) +1;
//		    				var parent = $(node).attr('folder');
//		    				var parentPrefix = '/' + metricTree.get_path('#' + parentId).slice(4).join('/');
//		    				//console.info("Populating Level Metrics and Folder [%s]", parent);
//		    				$.apmr.findLevelMetricsForAgentWithParent(level, agentId, parentPrefix + '%', function(data) {							
		                	
//		                	console.info("Selected Metric Folder:%o", metricTree.get_path($(me)));
//		                	console.info("Selected Metric Folder:%o", metricTree.get_path($(me), true));
		                }
		            },
		            'folder' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/folder_16_16.png'
		                }, valid_children : [ 'metric-folder', 'folder', 'metric' ],
		                select_node : function(me) {
//		                	console.info("Selected Folder:%o", metricTree.get_path($(me)));
//		                	console.info("Selected Folder:%o", metricTree.get_path($(me), true));
		                }				                
		            },
		            'metric' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/metric_16_16.png'
		                }, valid_children : [],
		                select_node : function(me) {		                	
		                	console.info("Selected Metric:[%s]", $(me).attr('metric'));
		                	var metricId = parseInt($(me).attr('metric'));
		                	ChartModel.find(metricId, function(model){model.renderChart({'auto' : true});});
		                }				                
		            }
		        }						
			}
		});
		metricTree = $.jstree._reference('#metricTree');
		
		
		//$.apmr.connect(); 
	}
	
	function parentContainsChild(parentId, childId) {
		if(parentId.split('')[0]!='#') parentId = '#' + parentId;
		metricTree._get_children(parentId).each(function(index, node) {
			if($(node).attr('id')==childId) return true;
		});
		return false;
	}
	
	function rootContainsDomain(domainId) {		
		var _domains = {};
		metricTree._get_children('#root').each(function(index, node) {
			_domains[$(node).attr('id')]=$(node).attr('id');
		});
		if(_domains[domainId]!=null) {
			console.info("Domain [%s] is in the root", domainId);
			return true;
		} else {
			console.info("Domain [%s] is NOT in the root", domainId);
			return false;			
		}		
	}
	
	
	function populateNode(node, callback) {
		//console.info("Populating Node [%o]", node);
		var parentId = $(node).attr('id');
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
		var timeNow = new Date().getTime();
		switch(rel) {
			case 'root':
				$.apmr.allDomains(function(data) {
					var pushedDomains = [];
					$.each(data.msg, function(index, domain) {
						var uid = "domain-" + domain.replace(/\./g, '_');
						if($('#' + uid).length==0 && !rootContainsDomain(uid)) {
							nodeArray.push({
								attr: {id: uid, rel: "domain", 'domain' : domain},  
								data : {title: domain}									
							});							
						}
					});
					callback(nodeArray); fixOpen(nodeArray);
					$.jstree._reference('#metricTree').open_node ( $('#root'), function(){
					}, true);

				});
				break;
			case 'domain':
				var domain = $(node).attr('domain');
				//console.info("Populating Hosts in Domain [%s]", domain);
				$.apmr.hostsByDomain(domain, function(data) {
					$.each(data.msg, function(index, host) {
						var hostName = host.name.split('.').pop();
						var uid = "host-" + host.hostId;
						if($('#' + uid).length==0 && !parentContainsChild(parentId, uid)) {  
							var newNode = {
									attr: {id: uid, rel: host.conn==null ? "down-server" : "server", 'host' : host.hostId, 'hostn' : host.name},  
									data : {title: hostName}									
								};
							nodeArray.push(newNode);
							if(host.conn==null) {
								newNode.attr.apmr_dnode_downTime = timeNow;
							}
							callback([newNode]);
						}
					});
					fixOpen(nodeArray);
				});
				break;
			case 'server':
			case 'down-server':
				var host = $(node).attr('host');
				//console.info("Populating Agents in Server [%s]", host);
				$.apmr.agentsByHost(host, function(data) {
					$.each(data.msg, function(index, agent) {
						var uid = "agent-" + agent.agentId;
						if($('#' + uid).length==0 && !parentContainsChild(parentId, uid)) {
							var newNode = {
									attr: {id: uid, rel: agent.conn!=null ? "online-agent" : "agent", 'agent' : agent.agentId, 'agentn' : agent.name, minl : agent.minl},  
									data : {title: agent.name}									
								};
							nodeArray.push(newNode);
							if(agent.conn==null) {
								newNode.attr.apmr_dnode_downTime = timeNow;
							}
							callback([newNode]);
						}
					});
					fixOpen(nodeArray);
				});
				break;
			case 'agent':
			case 'online-agent':
				var agentId = $(node).attr('agent');
				var level = 0;
				var parent = "";
				$.apmr.findLevelMetricsForAgent(0, agentId, function(data) {							
					$.each(data.msg, function(index, metric) {
						var uid = "metric-" + metric.id;
						if($('#' + uid).length==0) {
							var newNode = {
									attr: {id: uid, rel: "metric", 'metric' : metric.id},  
									data : {title: metric.name}									
								};
							callback([newNode]);
							metricModelCache[metric.id] = metric;
							$('#' + uid).livequery(function(){}, function(){
								var cachedModel = metricModelCache[metric.id];
								if(cachedModel instanceof ChartModel) {
									cachedModel.destroy();
								}
								delete metricModelCache[metric.id];
								console.info("Deleted metricModelCache Entry [%s]", metric.id);
							});							
						}
					});							
					
					
					$.apmr.findLevelFoldersForAgent(level, agentId, parent || '%', function(data) {
						$.each(data.msg, function(index, arr) {
							var folder = arr[0];
							var mlevel = arr[1];
							var isMetricFolder = (mlevel-level==1);														
							var uid = "folder-" + agentId + "-" + folder.replace('=', '_').replace(/\./g, '_');
							if($('#' + uid).length==0) {
								var newNode = {
										attr: {id: uid, rel: isMetricFolder ? "metric-folder" : "folder", 'folder' : folder, 'agent' : agentId, 'level' : level},  
										data : {title: folder}									
									};
								nodeArray.push(newNode);
								callback([newNode]);
							}
						});							
						fixOpen(nodeArray);
					});						
				});
				break;
			case 'folder':
			case 'metric-folder':
				var agentId = $(node).attr('agent');
				var level = parseInt($(node).attr('level')) +1;
				var parent = $(node).attr('folder');
				var parentPrefix = '/' + metricTree.get_path('#' + parentId).slice(4).join('/');
				//console.info("Populating Level Metrics and Folder [%s]", parent);
				$.apmr.findLevelMetricsForAgentWithParent(level, agentId, parentPrefix + '%', function(data) {							
					$.each(data.msg, function(index, metric) {
						var uid = "metric-" + metric.id;
						if($('#' + uid).length==0) {
							var newNode = {
									attr: {id: uid, rel: "metric", 'metric' : metric.id},  
									data : {title: metric.name}									
								}; 
							callback([newNode]);							
							$('#' + $(node).attr('id')).attr('rel', 'metric-folder');
							metricModelCache[metric.id] = metric;
							$('#' + uid).livequery(function(){}, function(){
								var cachedModel = metricModelCache[metric.id];
								if(cachedModel instanceof ChartModel) {
									cachedModel.destroy();
								}
								delete metricModelCache[metric.id];
								console.info("Deleted metricModelCache Entry [%s]", metric.id);
							});
						}
					});							
					//fixOpen(nodeArray);					
					console.info("Parent Folder Prefix [%s]", parentPrefix);
					//console.info("SQL:  select distinct narr[%s], m.level-a.min_level  from metric m, agent a  where a.agent_id = m.agent_id and a.agent_id = %s and namespace like '%s' and narr[%s] is not null", level, agentId, parentPrefix + '%', level);
					$.apmr.findLevelFoldersForAgent(level, agentId, parentPrefix + '%', function(data) {
						$.each(data.msg, function(index, arr) {
							var folder = arr[0];
							var mlevel = arr[1];
							var isMetricFolder = (mlevel-level==1);														
							if(folder!=null) {
								//console.info("This node is [%s] parent path:[%o]", this, metricTree.get_path('#' + parentId));
								var prefix = metricTree.get_path('#' + parentId).slice(4).join('_');
								var uid = "folder-" + agentId + "-" + (prefix + '_' + folder).replace(/=/g, '_').replace(/\./g, '_');
								if($('#' + uid).length==0) {
									var newNode = {
											attr: {id: uid, rel: isMetricFolder ? "metric-folder" : "folder", 'folder' : folder, 'agent' : agentId, 'level' : level},  
											data : {title: folder}									
										};
									callback([newNode]);
									nodeArray.push(newNode);
								}
							}
						});							
						fixOpen(nodeArray);
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
	
	
	

			
