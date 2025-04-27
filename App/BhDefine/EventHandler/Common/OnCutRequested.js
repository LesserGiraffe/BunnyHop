(function() {
  bhCommon.reconnect(bhThis.findOuterNode(1), bhThis, bhCandidateNodeList, bhUserOpe);

  let parent = bhThis.findParentNode();
  if (parent === null)
    return true;
  
  // 親もカット候補なら親だけカットして, このノードはカットしない (false)
  return !bhCandidateNodeList.contains(parent);
})();
