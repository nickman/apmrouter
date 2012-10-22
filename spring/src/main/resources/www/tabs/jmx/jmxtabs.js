	function init_jmx_tab() {
		var data = searchAll();
			$("#nav")
			   .jstree ({
			      "json_data" : data,
			      "themes" : {
			         "theme" : "classic"
			      },
			      "core" : {
			         "animation" : 50
			      },
			        // "plugins" : ["json_data","ui", "themeroller"]
				"plugins" : ["themes", "default", "json_data","ui"]
			   })
			   .bind("select_node.jstree", function (event, data) {
			      var mbean = data.rslt.obj.data("mbean");
			      if (mbean != null) {
			         listMbean(mbean);
			      }
			   });
			}
   				



		function extractDomainMap(mbeans) {
			   var domains = {};
			   for (var i = 0; i < mbeans.length; i++) {
			      var mbean = mbeans[i];
			      var domainEnd = mbean.indexOf(":");
			      var domainName = mbean.substring(0, domainEnd);
			      var domain = domains[domainName];
			      if (domain == null) {
			         domain = {"name": domainName, "entries": {}};
			         domains[domainName] = domain;
			      }
			      var container = domain.entries;
			      var tokens = mbean.substring(domainEnd + 1).split(",");
			      for (var ii = 0; ii < tokens.length; ii++) {
			         var token = tokens[ii];
			         var typeEnd = token.indexOf("=");
			         var name = token.substring(typeEnd + 1);
			         var item = container[name];
			         if (item == null) {
			            item = {"name": name, "entries": []};
			            container[name] = item;
			         }
			         if (ii == (tokens.length - 1)) {
			            item.mbean = mbean;
			         }
			         container = item.entries;
			      }
			   }
			   return domains;
			}
			
			function createJsTreeDataRecursive(entries, children) {
			   var i = 0;
			   for (var itemName in entries) {
			      if (entries.hasOwnProperty(itemName)) {
			         var item = entries[itemName];
			         var node = {"data": item.name, "children": []};
			         if (item.mbean != null) {
			            node.metadata = {"mbean": item.mbean};
			         }
			         children[i] = node;
			         createJsTreeDataRecursive(item.entries, node.children);
			         i++;
			      }
			   }
			}
			
			function createJsTreeData(domains) {
			   var data = {"data" : []};
			   var i = 0;
			   for (var domainName in domains) {
			      if (domains.hasOwnProperty(domainName)) {
			         var domain = domains[domainName];
			         var node = {"data": domain.name, "children": []};
			         data.data[i] = node;
			         createJsTreeDataRecursive(domain.entries, node.children);
			         i++;
			      }
			   }
			   return data;
			}
			
			function searchAll() {
			   var response = new Jolokia({"url": "jolokia"}).request(
			     {type: "search", mbean:"*:*", keyorder:"constructionTime"}, 
			     {method: "post"});
			   response.value.sort();
			   return createJsTreeData(extractDomainMap(response.value));
			}
			
			var data = searchAll();
			$("#nav")
			   .jstree ({
			      "json_data" : data,
			      "themes" : {
			         "theme" : "classic"
			      },
			      "core" : {
			         "animation" : 50
			      },
			         "plugins" : ["themes", "default", "json_data","ui"]
				// "plugins" : ["json_data","ui", "themeroller"]
			   })
			   .bind("select_node.jstree", function (event, data) {
			      var mbean = data.rslt.obj.data("mbean");
			      if (mbean != null) {
			         listMbean(mbean);
			      }
			   });

			var currentMbean;
			function listMbean(mbean) {
			   var path = mbean.replace(new RegExp("/", 'g'), "!/");
			   path = path.replace(":", "/");
			   var meta = new Jolokia({"url": "/jolokia/"}).request(
			     {"type": "list", "path": path}, 
			     {method: "post"});
			   currentMbean = mbean;
			   currentMeta = meta;
			   $("#mbeanInfo").html("<h2 id='mbeanName'>" + mbean + "</h2><p>" + meta.value.desc + "</p>");
			   var attributes = { 'attributes' : [] };
			   for (var attr in meta.value.attr) {
			      if (meta.value.attr.hasOwnProperty(attr)) {
			         attributes['attributes'].push({
			            'name' : attr,
			            'value' : meta.value.attr[attr]
			         });
			      }
			   }
			   var source = $("#attributes-template").html();
			   var template = Handlebars.compile(source);
			   $("#attributes").html(template(attributes));
			   $("#attributes-table").tablesorter({widgets: ['zebra']});
			   // inject the values
			   var values = new Jolokia({"url": "jolokia"}).request(
			     {"type": "read", "mbean": mbean}, 
			     {method: "post"});
			   for (var attr in values.value) {
			      if (values.value.hasOwnProperty(attr)) {
			         var value = values.value[attr];
			         if (meta.value.attr[attr].rw) {
			            $("#attr-input-" + attr).val(value);
			         } else {
			            $("#" + attr).html(value);
			         }
			      }
			   }
			}
			
			
			function updateAttribute(name) {
			   var mbean = currentMbean;
			   var value = $("#attr-input-" + name).val();
			    new Jolokia({"url": "/jolokia/"}).request(
			     {"type": "write", "mbean": mbean, "attribute": name, "value": value}, 
			     {method: "post"});
			   listMbean(mbean);
			}
			