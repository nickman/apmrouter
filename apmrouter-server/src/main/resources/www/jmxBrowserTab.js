function initjmxBrowserTab() {		
	if($('#jmxBrowserTabInited').length==1) {
		console.debug("########## jmxBrowserTab already inited ##########");
		return;
	}
	console.info("########## initing jmxBrowserTab ##########");
	var initedFlag = $('<div id="jmxBrowserTabInited"></div>').hide();
	$('#jmxBrowserTab').append(initedFlag);
	$.get('js/jmxBrowser.js', function(){
		initJmxTree();		
	});
}

function completejmxBrowserTab() {
	console.info("########## completing jmxBrowserTab ##########");
}

