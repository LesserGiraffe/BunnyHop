let imitIdManual = 'imitIdManual';

let listDeclToPushStatID = {
	'NumListDecl'   : 'idNumArrayPushStat',
	'StrListDecl'   : 'idStrArrayPushStat',
	'BoolListDecl'  : 'idBoolArrayPushStat',
	'ColorListDecl' : 'idColorArrayPushStat',
	'SoundListDecl' : 'idSoundArrayPushStat'};

let listDeclToGetStatID = {
	'NumListDecl'   : 'idNumArrayGetExp',
	'StrListDecl'   : 'idStrArrayGetExp',
	'BoolListDecl'  : 'idBoolArrayGetExp',
	'ColorListDecl' : 'idColorArrayGetExp',
	'SoundListDecl' : 'idSoundArrayGetExp'};

let listDeclToGetLastStatID = {
	'NumListDecl'   : 'idNumArrayGetLastExp',
	'StrListDecl'   : 'idStrArrayGetLastExp',
	'BoolListDecl'  : 'idBoolArrayGetLastExp',
	'ColorListDecl' : 'idColorArrayGetLastExp',
	'SoundListDecl' : 'idSoundArrayGetLastExp'};

let listDeclToPopStatID = {
	'NumListDecl'   : 'idNumArrayPopStat',
	'StrListDecl'   : 'idStrArrayPopStat',
	'BoolListDecl'  : 'idBoolArrayPopStat',
	'ColorListDecl' : 'idColorArrayPopStat',
	'SoundListDecl' : 'idSoundArrayPopStat'};

let listDeclToInsertStatID = {
	'NumListDecl'   : 'idNumArrayInsertStat',
	'StrListDecl'   : 'idStrArrayInsertStat',
	'BoolListDecl'  : 'idBoolArrayInsertStat',
	'ColorListDecl' : 'idColorArrayInsertStat',
	'SoundListDecl' : 'idSoundArrayInsertStat'};

let listDeclToRemoveStatID = {
	'NumListDecl'   : 'idNumArrayRemoveStat',
	'StrListDecl'   : 'idStrArrayRemoveStat',
	'BoolListDecl'  : 'idBoolArrayRemoveStat',
	'ColorListDecl' : 'idColorArrayRemoveStat',
	'SoundListDecl' : 'idSoundArrayRemoveStat'};

let listDeclToSetStatID = {
	'NumListDecl'   : 'idNumArraySetStat',
	'StrListDecl'   : 'idStrArraySetStat',
	'BoolListDecl'  : 'idBoolArraySetStat',
	'ColorListDecl' : 'idColorArraySetStat',
	'SoundListDecl' : 'idSoundArraySetStat'};

let listDeclToAppendStatID = {
	'NumListDecl'   : 'idNumArrayAppendStat',
	'StrListDecl'   : 'idStrArrayAppendStat',
	'BoolListDecl'  : 'idBoolArrayAppendStat',
	'ColorListDecl' : 'idColorArrayAppendStat',
	'SoundListDecl' : 'idSoundArrayAppendStat'
};

let listDeclToClearStatID = {
	'NumListDecl'   : 'idNumArrayClearStat',
	'StrListDecl'   : 'idStrArrayClearStat',
	'BoolListDecl'  : 'idBoolArrayClearStat',
	'ColorListDecl' : 'idColorArrayClearStat',
	'SoundListDecl' : 'idSoundArrayClearStat'};

function genArrayControlStat(listDeclToNodeID) {

	let nodeID = listDeclToNodeID[bhThis.getSymbolName()];
	let variable = bhCommon.buildImitation(bhThis, imitIdManual, bhUserOpeCmd);
	let node = bhCommon.genBhNode(nodeID, bhNodeTemplates, bhUserOpeCmd);
	let listArg = node.findSymbolInDescendants('*', 'Arg0', '*');
	listArg.replace(variable, bhUserOpeCmd);
	return node;
}

function genListInputNode(nodeID, listArgName) {

	let variable = bhCommon.buildImitation(bhThis, imitIdManual, bhUserOpeCmd);
	let arrayToStrExp = bhCommon.genBhNode(nodeID, bhNodeTemplates, bhUserOpeCmd);
	let listArg = arrayToStrExp.findSymbolInDescendants('*', listArgName, '*');
	listArg.replace(variable, bhUserOpeCmd);
	return arrayToStrExp;
}

(function() {

	let list = bhCommon.buildImitation(bhThis, imitIdManual, bhUserOpeCmd);
	let templates = [
		list,
		genArrayControlStat(listDeclToPushStatID),
		genArrayControlStat(listDeclToGetStatID),
		genArrayControlStat(listDeclToGetLastStatID),
		genListInputNode('idAnyArrayLengthExp', 'Array'),
		genArrayControlStat(listDeclToPopStatID),
		genArrayControlStat(listDeclToInsertStatID),
		genArrayControlStat(listDeclToRemoveStatID),
		genArrayControlStat(listDeclToSetStatID),
		genArrayControlStat(listDeclToAppendStatID),
		genArrayControlStat(listDeclToClearStatID),
		genListInputNode('idAnyListToStrExp', 'Arg0')
	];

	if (String(bhThis.getSymbolName()) === 'SoundListDecl')
		templates.push(genListInputNode('idPlaySoundListStat', 'Arg0'));

	return templates;
})();
