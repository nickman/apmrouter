


var ObjectName = Class.extend({
	spaceRegex : new RegExp(' ', 'g'),
	wildCard : '*',
	name: '',
	domain: '',
	keyProperties: {},
	pattern: false,
  	isPat: function(expr) {
  		if(expr==null || expr=='') throw ('The expression was null or zero length');
  		if(this.pattern) return true;
        var indx = expr.indexOf(this.wildCard);
  		return indx>-1;
  	},	
	init: function(objectName){
		if(objectName==null) throw ('The passed objectName was null');
		this.name = objectName.replace(this.spaceRegex, '');
		var tmp = this.name.split(':');
		this.domain = tmp[0];
		this.pattern = this.isPat(this.domain);
		tmp = tmp[1].split(',');
		for(var i in tmp) {
			var pair = tmp[i].split('=');
			this.keyProperties[pair[0]]=pair[1];
			this.pattern = this.isPat(pair[0]);
			this.pattern = this.isPat(pair[1]);
		}
  	},
  	getName: function() {
  		return this.name;
  	},
  	isPattern: function(){
  		return this.pattern;
  	},
  	getProp: function(key) {
  		return this.keyProperties[key];
  	},
  	getDomain:	function() {
  		return this.domain;
  	},
  	toString:  function() {
  		return this.name;
  	}
});


/*
console.clear();
var a = "org.helios.js:service=JMX, type=ObjectName";
var b = "*:service=JMX, type=ObjectName";
var c = "*:service=JMX, type=*";
var d = "*:*=JMX, type=ObjectName";

var on = new ObjectName(a);
console.info("ObjectName[%s] is pattern: [%s]", on.getName(), on.isPattern());
on = new ObjectName(b);
console.info("ObjectName[%s] is pattern: [%s]", on.getName(), on.isPattern());
on = new ObjectName(c);
console.info("ObjectName[%s] is pattern: [%s]", on.getName(), on.isPattern());
on = new ObjectName(d);
console.info("ObjectName[%s] is pattern: [%s]", on.getName(), on.isPattern());
*/
