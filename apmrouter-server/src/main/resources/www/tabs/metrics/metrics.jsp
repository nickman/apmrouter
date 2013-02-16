<script type="text/javascript">
	jQuery(function($) {
		init_metricsBrowser_ui();
	});
</script>
 <style type="text/css">
     .myAltRowClass { background-color: #DDDDDC; background-image: none; }     
 </style>

<div id="metricBrowserBody">
	<div id="metricBrowserTreeExtruder" style="display: none;" class="{title:'Metric Tree'} ui-widget-header" >
	<!-- 
		<input id="searchTreeInput" ></input>
		<div id="metricBrowserTree"></div>
	-->
	</div>
	<table>
		<tr>
			<td width="5%"></td>
			<td width="80%"><input id="gridMaskInput" size="128"></td>
		</tr>
		<tr>
			<td valign="top" >
				<input id="searchTreeInput" ></input>
				<div id="metricBrowserTree"></div>			
			</td>
			<td valign="top" >
				<div id="metricBrowserGridContainer">
					<table id="metricBrowserGrid"><tr><td/></tr></table> 
					<div id="gridpager"></div>
				</div>				
			</td>
		</tr>
		
	</table>		
</div>		

<!-- 

		<div id="metricBrowser" class="ui-tabs-panel ui-widget-content ui-corner-bottom">
				<input id="sessionInput" type="text" width="90" maxlength="50">
				<label for="sessionInput">Session</label>				
				<button id="signInSession">Start Session</button>
				<button id="signOutSession">Signout</button>
				
				<form id="subscribeForm">
					<button id="startSubscriber">Start Subscriber</button>
					<button id="stopSubscriber">Stop Subscriber</button>
					<br>
					<div id="subscription">
						<input id="subMask" type="text" width="90" maxlength="50" >					
						<label for="subMask">Subscription Mask</label>
						<button id="startSubscription">Start Subscription</button>
						<button id="stopSubscription">Stop Subscription</button>
					</div>
				</form>		
	
				
		</div>

		-->