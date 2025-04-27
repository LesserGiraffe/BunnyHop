(function() {
  
  // このノードが外部ノードでかつ, 親がコピー候補 -> このノード以下はコピーしない
  let parent = bhThis.findParentNode();
  if (bhThis.isOuter() && bhCandidateNodeList.contains(parent))
    return function (node) { return false; };
  
  // コピー判定関数
  // 外部ノードでかつ, コピー対象に含まれていないでかつ, 親はコピー対象 -> コピーしない
  function isNodeToCopy(node) {
    return !(node.isOuter()
            && !bhCandidateNodeList.contains(node)
            && bhCandidateNodeList.contains(node.findParentNode()));
  }
  return isNodeToCopy;
})();
