(function() {

	let bhCommon = {};

	// 入れ替わってWSに移ったノードを末尾に再接続する
	function appendRemovedNode(newNode, oldNode, manuallyReplaced, bhNodeHandler, bhUserOpeCmd) {
	    
	    let outerEnd = newNode.findOuterEndNode();
		if ((outerEnd.type === "void") && outerEnd.canBeReplacedWith(oldNode) && !manuallyReplaced) {
			bhNodeHandler.removeFromWS(oldNode, bhUserOpeCmd);
			bhNodeHandler.replaceChild(outerEnd, oldNode, bhUserOpeCmd);
			bhNodeHandler.deleteNode(outerEnd, bhUserOpeCmd);
	    }
	}

    bhCommon['appendRemovedNode'] = appendRemovedNode;
    return bhCommon;
})();
