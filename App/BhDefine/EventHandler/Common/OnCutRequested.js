(function() {
  bhCommon.moveNodeToAncestor(
      bhThis.findOuterNode(1),
      bhThis,
      bhTargetNodes,
      ['VarDeclVoid', 'GlobalDataDeclVoid', 'VoidStat'],
      bhUserOpe);

  let parent = bhThis.findParentNode();
  if (parent === null) {
    return true;
  }
  // 親もカット候補なら親だけカットして, このノードはカットしない (false)
  return !bhTargetNodes.contains(parent);
})();
