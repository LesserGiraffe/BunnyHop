(function() {

	let BhNodeID = Java.type("net.seapanda.bunnyhop.model.node.BhNodeID");

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
		let moveSpeed = newMoveStat.findSymbolInDescendants('*', 'Arg0', '*');
		let moveTime = newMoveStat.findSymbolInDescendants('*', 'Arg1', '*');
		moveSpeed.setText('2');
		moveTime.setText('1');
		registerNodeTemplate('idMoveStat', newMoveStat);
	}

	//音ノードの初期値を変更して再登録
	function setInitialSoundParams() {
		
		let newFreqSoundLiteral = genBhNode('idFreqSoundLiteral', bhUserOpeCmd);
		let duration = newFreqSoundLiteral.findSymbolInDescendants('*', 'Duration', '*');
		let freq = newFreqSoundLiteral.findSymbolInDescendants('*', 'Frequency', '*');
		duration.setText('1');
		freq.setText('500');
		registerNodeTemplate('idFreqSoundLiteral', newFreqSoundLiteral);
		
		let newScaleSoundLiteral = genBhNode('idScaleSoundLiteral', bhUserOpeCmd);
		duration = newScaleSoundLiteral.findSymbolInDescendants('*', 'Duration', '*');
		duration.setText('1');
		registerNodeTemplate('idScaleSoundLiteral', newScaleSoundLiteral);
	}

	//リストと変数名を変更して再登録
	function setVarAndListName() {

		let varDecls = ['idNumVarDecl', 'idStrVarDecl', 'idBoolVarDecl', 'idSoundVarDecl'];
		let varNames = ['数値変数', '文字列変数', '論理変数', '音変数'];
		for (let i = 0; i < varDecls.length; ++i) {
			let varDecl = genBhNode(varDecls[i], bhUserOpeCmd);
			let varName = varDecl.findSymbolInDescendants('*', 'VarName', '*');
			varName.setText(varNames[i]);
			registerNodeTemplate(varDecls[i], varDecl);
		}
/*
		//数値変数
		var varDecl = genBhNode('idNumVarDecl', bhUserOpeCmd);
		var varName = varDecl.findSymbolInDescendants('*', 'VarName', '*');
		varName.setText('数値変数');
		registerNodeTemplate('idNumVarDecl', varDecl);

		//文字列変数
		varDecl = genBhNode('idStrVarDecl', bhUserOpeCmd);
		varName = varDecl.findSymbolInDescendants('*', 'VarName', '*');
		varName.setText('文字列変数');
		registerNodeTemplate('idStrVarDecl', varDecl);

		//論理変数
		varDecl = genBhNode('idBoolVarDecl', bhUserOpeCmd);
		varName = varDecl.findSymbolInDescendants('*', 'VarName', '*');
		varName.setText('論理変数');
		registerNodeTemplate('idBoolVarDecl', varDecl);
*/

		let listDecls = ['idNumListDecl', 'idStrListDecl', 'idBoolListDecl'];
		let listNames = ['数値リスト', '文字列リスト', '論理リスト'];
		for (let i = 0; i < listDecls.length; ++i) {
			let listDecl = genBhNode(listDecls[i], bhUserOpeCmd);
			let listName = listDecl.findSymbolInDescendants('*', 'ListName', '*');
			listName.setText(listNames[i]);
			registerNodeTemplate(listDecls[i], listDecl);
		}
		/*
		//数値リスト
		varDecl = genBhNode('idNumListDecl', bhUserOpeCmd);
		varName = varDecl.findSymbolInDescendants('*', 'ListName', '*');
		varName.setText('数値リスト');
		registerNodeTemplate('idNumListDecl', varDecl);

		//文字列リスト
		varDecl = genBhNode('idStrListDecl', bhUserOpeCmd);
		varName = varDecl.findSymbolInDescendants('*', 'ListName', '*');
		varName.setText('文字列リスト');
		registerNodeTemplate('idStrListDecl', varDecl);

		//論理リスト
		varDecl = genBhNode('idBoolListDecl', bhUserOpeCmd);
		varName = varDecl.findSymbolInDescendants('*', 'ListName', '*');
		varName.setText('論理リスト');
		registerNodeTemplate('idBoolListDecl', varDecl);*/
	}

	addRepeatAndCountNode('idRepeatAndCount');
	setInitialMoveSpeed();
	setVarAndListName();
	setInitialSoundParams();
})();


