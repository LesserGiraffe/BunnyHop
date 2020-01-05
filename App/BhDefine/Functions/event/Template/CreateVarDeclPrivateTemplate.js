/** 代入ノード作成 */
function genAssignStat() {

	let varDeclToAssignStatID = {
		'NumVarDecl'    : 'idNumAssignStat',
		'StrVarDecl' : 'idStrAssignStat',
		'BoolVarDecl': 'idBoolAssignStat',
		'ColorVarDecl'  : 'idColorAssignStat',
		'SoundVarDecl'  : 'idSoundAssignStat'};

	let assignStatID = varDeclToAssignStatID[bhThis.getSymbolName()];
	let variable = bhCommon.buildImitation(bhThis, 'imitIdManual', bhUserOpeCmd);
	let assignStat = bhCommon.genBhNode(assignStatID, bhNodeTemplates, bhUserOpeCmd);
	let leftVar = assignStat.findSymbolInDescendants('*', 'LeftVar', '*');
	leftVar.replace(variable, bhUserOpeCmd);
	return assignStat;
}

function genAddAssignStat() {
	
	let variable = bhCommon.buildImitation(bhThis, 'imitIdManual', bhUserOpeCmd);
	let addAssignStat = bhCommon.genBhNode('idNumAddAssignStat', bhNodeTemplates, bhUserOpeCmd);
	let leftVar = addAssignStat.findSymbolInDescendants('*', 'LeftVar', '*');
	leftVar.replace(variable, bhUserOpeCmd);
	return addAssignStat;
}

(function() {

	let variable = bhCommon.buildImitation(bhThis, 'imitIdManual', bhUserOpeCmd);
	templates = [variable, genAssignStat()];
	if (String(bhThis.getSymbolName()) === 'NumVarDecl')
		templates.push(genAddAssignStat());
	
	return templates;
})();
