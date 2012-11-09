function init_ui() {
	var container = $('body'),
		west = $('body > .west'),
		east = $('body > .east'),
		center = $('body > .center');
	
	  function relayout() {
	        container.layout({resize: false});
	    }
	function layout() {
		container.layout();
		$('#accordion').accordion();
		$('#accordion').accordion('resize');
		$('#topBar').next().attr('id', 'topBarNext');
		$('#topBarNext>ul>li').css('font-size', '60%');
		// This ensures that the center is never smaller than 400 pixels.
		east.resizable('option', 'maxWidth', (center.width() + east.width()) - 400);
		west.resizable('option', 'maxWidth', (center.width() + west.width()) - 400);
	}
	
	// Make the west and east panels resizable
	west.resizable({
		handles: 'e',
		stop: layout,
		helper: 'ui-resizable-helper-west',
		minWidth: 200
	});
	
	east.resizable({
		handles: 'w',
		stop: layout,
		helper: 'ui-resizable-helper-east',
		minWidth: 200
	});
	
	// Lay out the west panel first
	west.layout();
	$('.west').toggle();
	$('.east').toggle();
	
	// Then do the main layout.
	layout();
	
	// Hook up the re-layout to the window resize event.
	$(window).resize(layout);
	
	//$('#topBar').layout();
	
	
	/**
	 * Below here is all demo code, which has no relation to the layout.
	 */
	$('#accordion').accordion({header: 'h3', fillSpace: true});
	
	// Set up the tabs in the center panel and remove the unwanted corners class
	center.tabs();
	center.children('ul').removeClass('ui-corner-all');
	
	$('#controlsToggle').click(function () {
		//$('.west').toggle();
		west.animate({width: 'toggle'}, {duration: 500, complete: relayout, step: relayout});
	});
	$('#diagnosticsToggle').click(function () {
		east.animate({width: 'toggle'}, {duration: 500, complete: relayout, step: relayout});
	});
	$('#termSessionBtn').click(function () {
		$.helios.termSession();
	});
	
	$.extend($.ui.slider.defaults, {
		range: "min",
		animate: true,
		orientation: "vertical"
	});
	
	$("#eq > span").each(function() {
		var value = parseInt($(this).text());
		$(this).empty();
		$(this).slider({
			value: value
		});
	});				
	
	$("#progressbar").progressbar({
		value: 59
	});
	
	$('#datepicker').datepicker();
	
	$('#topBar').children().css('vertical-align', 'middle');
	$('#topBar img').css('vertical-align', 'middle');
	
	// =================================================================
	//  Hook up default singularity and data subscriptions
	// =================================================================
	
	$('#ajaxActivity').ajaxStop(function(){
		$(this).attr('src', 'img/red-light-16X16.png');
	});
	$('#ajaxActivity').ajaxError(function(){
		$(this).attr('src', 'img/red-light-16X16.png');
	});
	
	$('#ajaxActivity').ajaxStart(function(){
		$(this).attr('src', 'img/green-light-16X16.png');
	});
	$('#ajaxDataLink').prepend((window.WebSocket==null ? "Poller Active:&nbsp;" : "WebSocket Active:&nbsp;"));


	
};
