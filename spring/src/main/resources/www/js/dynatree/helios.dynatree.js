/**
 * Dynatree Extensions
 * Whitehead, 12/23/2011
 */


/**
 * Takes a path like '/a/b/c' and ensures that the node structure exists.
 * All path members are assumed to be folders except the last, unless the leaf value is passed.
 * If a leaf value is passed, that is assumed to be the non-folder ending node.
 */
jQuery.ui.dynatree.prototype.fillNodes = function(path, leaf) {
		var splitPath = function(p) {
			var spaces = p.trim().replace(/ /g, '').replace(/\/$/g, '').replace(/^\//g, '').split('/');
			if(spaces[0]=='') spaces.shift();
			return spaces;			
		};
		var segments = splitPath(path);
		var context = "";
		var currentNode = this.getRoot();
		var tree = this.getTree();
		var lastElement = segments.length-1;
		$.each(segments, function(index, segment) {			
			context += ("/" + segment);
			var node = tree.getNodeByKey(context);
			if(node==null) {
				node = currentNode.addChild({title: segment, key: context, isFolder: (lastElement!=index || leaf!=null )});
			}
			currentNode = node;			
		});
		if(leaf!=null) {
			currentNode.addChild({title: leaf, key: context + ("/" + leaf), isFolder: false});
		}
}