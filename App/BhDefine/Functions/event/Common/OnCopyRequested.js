(function() {
  
  // 親もコピー候補なら親だけコピーして, このノードはコピーしない
  let parent = bhThis.findParentNode();
  if (bhCandidateNodeList.contains(parent))
    return null;
  
  // コピー判定関数
  // 外部ノードでかつ, コピー対象に含まれていないでかつ, 親はコピー対象 -> コピーしない
  function isNodeToCopy(node) {
    return !(node.isOuter() && 
          !bhCandidateNodeList.contains(node) &&
          bhCandidateNodeList.contains(node.findParentNode()));
  }
  return isNodeToCopy;
})();
