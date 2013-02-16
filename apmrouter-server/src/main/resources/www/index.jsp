<html>
	<head>
		<title>Helios MetricID Scratch Pad</title>
		<script type="text/javascript" src="js/jquery.min.js"></script>
		<script type="text/javascript" src="js/helios.js"></script>
		<script type="text/javascript" src='js/jquery.cookie.js'></script>
		<script type="text/javascript" src="js/ui/jquery-ui-1.8.9.custom.min.js"></script>   
    	<script type="text/javascript" src='js/dynatree/jquery.dynatree.min.js'></script>
		
		<link rel="stylesheet" type="text/css" href="js/ui/css/smoothness/jquery-ui-1.8.9.custom.css"  />	
		<link rel='stylesheet' type='text/css' href='js/dynatree/skin/ui.dynatree.css'>	
		
		
		
		<script type="text/javascript">
			$(function(){
				// jQuery Init Stuff Here.
				//helios_init();
					
				$('#goButton').bind('click', function() {
					helios_metricPath($('#metricIDPath')[0].value);
				});
				$("#goButton").keypress(function(event) {
					alert("Key Event:" + event.which);
					console.info("Key Event:" + event.which); 
					  if ( event.which == 13 ) {
					     event.preventDefault();
					     helios_metricPath($('#metricIDPath')[0].value);
					  }
				});
				
				$("#tree").dynatree({
			        onActivate: function(node) {
			            console.info("MetricTree activated: " + node);
			        },
		            onLazyRead: function(node){
		            	console.info("Lazy Read [%s]", node.data.key);
		            	helios_metricPath(node.data.key, function(nodes) {
	                    	node.setLazyNodeStatus(DTNodeStatus_Ok);
	                    	$.each(nodes, function(i, v) {
	                    		node.addChild(v);
	                    	});
	                    								
		            	});			            		            	
	            	}		        		            			        
			    });
			    var rootNode = $("#tree").dynatree("getRoot");
			    var actualRootNode = rootNode.addChild({
			        title: "Root",
			        tooltip: "The root node",
			        isFolder: true,
			        isLazy: true,
			        key: "/"
				});
			    helios_signin();
			    var prefix = $('#pageTitle').html();
			    $('#pageTitle').html(prefix + " [" + sessionId + "]");
			    helios_startSubscriber();
				
				/*
				$("#tree").dynatree({
		            onActivate: function(node) {
		                // A DynaTreeNode object is passed to the activation handler
		                // Note: we also get this event, if persistence is on, and the page is reloaded.
		                //alert("You activated " + node.data.title);
		            },
		            persist: true,
		            initId: "metricTreeId",
		            minExpandLevel: 0, 	
		 		            
		            children: [ // Pass an array of nodes.
		                {title: "Root", isLazy: "true", isFolder: true, expand: false, key: "/"  //children: []
		                }
		            ],
		            onLazyRead: function(node){
			            console.info("Lazy Read [%s]", node.data.key);
			            helios_metricPath(node.data.key, function(nodes) {
		                    node.setLazyNodeStatus(DTNodeStatus_Ok);
		                    node.addChild(nodes);							
			            });			            		            	
		            }		        		            
		        });
		        */				
			});				
			//var jolokia = new Jolokia({url: "/jolokia/"});
		</script>
		
		<style type="text/css">
			/*demo page css*/
			body{ font: 62.5% "Trebuchet MS", sans-serif; margin: 50px;}
			.demoHeaders { margin-top: 2em; }
			#dialog_link {padding: .4em 1em .4em 20px;text-decoration: none;position: relative;}
			#dialog_link span.ui-icon {margin: 0 5px 0 0;position: absolute;left: .2em;top: 50%;margin-top: -8px;}
			ul#icons {margin: 0; padding: 0;}
			ul#icons li {margin: 2px; position: relative; padding: 4px 0; cursor: pointer; float: left;  list-style: none;}
			ul#icons span.ui-icon {float: left; margin: 0 4px;}
			.console-base {
		    	float: none;
		    	height: 95%;
		    	width: 95%;
		    }		
			.tabs-base {
		    	float: none;
		    	height: 90%;
		    	width: 90%;
		    }		
			
		</style>			
	</head>
<body class="console-base">
	<!-- <img id="consoleTitle" src="img/Helios_Symbol_30_45.png"></img> -->	
	<form class="niceform">
		<fieldset>
		<legend id="pageTitle" >Helios MetricID ScratchPad</legend>
        <dl>
        	<dt><label for="sessionId">Start At:</label></dt>
            <dd><input type="text" name="metricIDPath" id="metricIDPath" size="32" value="njw810"/></dd>
        </dl>
		<dl>
            <dd><button class="NFButton" type="button" name="goButton" id="goButton">Go</button></dd>
        </dl>
                
        </fieldset>
        <div id="metricScratchOutput">
        	<div id="tree">
        	
        	</div>
        	<textarea rows="10" cols="100" id="output" readonly="readonly" title="Output"></textarea>
        </div>
        
    </form>
	

</body>
</html>


<!-- 

		<ul>
			<li><a href="jmx/mbeanserver-tree.jsp">MBeanServers</a></li>
			<li><a href="jmx/mbeanserver-tree2.jsp">ServerStatus</a></li>
			<li><a href="#dashboards">Dashboards</a></li>
		</ul>


<li class="ui-state-default ui-corner-all" title=".ui-icon-carat-1-n"><span class="ui-icon ui-icon-carat-1-n"></span></li>

<li class="ui-state-default ui-corner-all" title=".ui-icon-power"><span class="ui-icon ui-icon-power"></span></li>
<li class="ui-state-default ui-corner-all" title=".ui-icon-cancel"><span class="ui-icon ui-icon-cancel"></span></li>


			<ul id="sessionbuttons" class="ui-widget ui-helper-clearfix">
			<li  class="ui-state-default ui-corner-all" title=".ui-icon-power"><span class="ui-icon ui-icon-power"></span></li>
			<li class="ui-state-default ui-corner-all" title=".ui-icon-cancel"><span class="ui-icon ui-icon-cancel"></span></li>
			</ul>


 -->