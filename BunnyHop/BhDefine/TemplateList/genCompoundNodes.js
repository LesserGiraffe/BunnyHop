(function() {

	let BhNodeID = Java.type("pflab.bunnyhop.model.BhNodeID");

	function registerNodeTemplate(bhNodeID, bhNode) {
		bhNodeTemplates.registerNodeTemplate(BhNodeID.createBhNodeID(bhNodeID), bhNode);
	}
	
	function genBhNode(bhNodeID, bhUserOpeCmd) {
		return bhNodeTemplates.genBhNode(BhNodeID.createBhNodeID(bhNodeID), bhUserOpeCmd);
	}

	function connect(parentNode, childNode, cnctrPath) {
		const cnctr = parentNode.findSymbolInDescendants(cnctrPath);
		cnctr.connectNode(childNode, bhUserOpeCmd);
	}

	//カウンタ付き回数指定ループノード作成
	function addRepeatAndCountNode(nodeID) {
	
		let compoundNode = genBhNode('idCompoundStat', bhUserOpeCmd);
		let countVarNode = genBhNode('idNumVarDecl', bhUserOpeCmd);
		let initAssignStatNode = genBhNode('idNumAssignStat', bhUserOpeCmd);
		let repeatStatNode = genBhNode('idRepeatStat', bhUserOpeCmd);
		let updateAssignStatNode = genBhNode('idNumAddAssignStat', bhUserOpeCmd);
		
		connect(compoundNode, initAssignStatNode, ['*', '*', 'StatList']);
		connect(compoundNode, countVarNode, ['*', '*', 'LocalVarDecl']);
		connect(initAssignStatNode, repeatStatNode, ['*', 'NextStat']);
		connect(repeatStatNode, updateAssignStatNode,  ['*', 'LoopStat']);
		
		//変数名変更
		let countVarName = countVarNode.findSymbolInDescendants(['*', 'VarName', '*']);
		countVarName.setText('カウンター');
		
		//カウンタ更新幅変更
		let updateDiff = updateAssignStatNode.findSymbolInDescendants(['*', 'RightExp', '*']);
		updateDiff.setText('1');
				
		registerNodeTemplate(nodeID, compoundNode);
	}
	
	//移動ノードの初期速度を変更して再登録
	function setInitialMoveSpeed() {
		let newMoveStat = genBhNode('idMoveStat', bhUserOpeCmd);
		let initialMoveSpeed = genBhNode('idDefaultNumLiteral', bhUserOpeCmd);
		initialMoveSpeed.setText('2');
		connect(newMoveStat, initialMoveSpeed, ['*', 'Arg0']);
		registerNodeTemplate('idMoveStat', newMoveStat);
	}
	
	addRepeatAndCountNode('idRepeatAndCount');
	setInitialMoveSpeed();
})();