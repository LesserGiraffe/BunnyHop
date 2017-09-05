(function() {

	function connect(parentNode, childNode, cnctrPath) {
		const cnctr = parentNode.findSymbolInDescendants(cnctrPath);
		cnctr.connectNode(childNode, bhUserOpeCmd);
	}

	//カウンタ付き回数指定ループノード作成
	function addRepeatAndCountNode(nodeID) {
	
		let compoundNode = bhNodeTemplates.genBhNode('idCompoundStat', bhUserOpeCmd);
		let countVarNode = bhNodeTemplates.genBhNode('idNumVarDecl', bhUserOpeCmd);
		let initAssignStatNode = bhNodeTemplates.genBhNode('idNumAssignStat', bhUserOpeCmd);
		let repeatStatNode = bhNodeTemplates.genBhNode('idRepeatStat', bhUserOpeCmd);
		let updateAssignStatNode = bhNodeTemplates.genBhNode('idNumAddAssignStat', bhUserOpeCmd);
		
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
				
		bhNodeTemplates.registerNodeTemplate(nodeID, compoundNode);
	}
	
	//移動ノードの初期速度を変更して再登録
	function setInitialMoveSpeed() {
		let newMoveStat = bhNodeTemplates.genBhNode('idMoveStat', bhUserOpeCmd);
		let initialMoveSpeed = bhNodeTemplates.genBhNode('idDefaultNumLiteral', bhUserOpeCmd);
		initialMoveSpeed.setText('2');
		connect(newMoveStat, initialMoveSpeed, ['*', 'Arg0']);
		bhNodeTemplates.registerNodeTemplate('idMoveStat', newMoveStat);
	}
	
	addRepeatAndCountNode('idRepeatAndCount');
	setInitialMoveSpeed();
})();