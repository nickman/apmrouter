
function jmxTree() {
	console.info("All JMX Scripts Loaded");
	$('#jmxBrowserTree')
}

		
function initJmxTree() {
	$.getScript("js/jolokia/jolokia-min.js", function(data, textStatus, jqxhr) {
		console.info("Jolokia Loaded");
		$.getScript("js/handlebars-1.0.rc.1.js", function(data, textStatus, jqxhr) {
			console.info("Handlebars Loaded");
			$.getScript("js/jquery.tablesorter.min.js", function(data, textStatus, jqxhr) {
				console.info("Table sorter Loaded");
				jmxTree();
			});
		});
	});
	
}


