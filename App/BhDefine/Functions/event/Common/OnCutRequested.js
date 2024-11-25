(function() {
  bhCommon.reconnectOuter(bhThis, bhCandidateNodeList, bhMsgService, bhUserOpe);

  let parent = bhThis.findParentNode();
  if (parent === null)
    return true;
  
  // 親もカット候補なら親だけカットして, このノードはカットしない (false)
  return !bhCandidateNodeList.contains(parent);
})();
