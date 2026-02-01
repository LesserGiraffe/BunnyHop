(function() {
  if (bhThis.isDeleted()) {
    return;
  }
  // オリジナルノードであるリスト定義が消えた場合, Arg0 または LeftVar に接続された派生ノードであるリストノードが消えて新しいノードに入れ替わる.
  // リストノードが消えたとき, このノードを消す.
  let cnctrName = String(bhParentConnector.getSymbolName());
  if (cnctrName === 'Arg0' || cnctrName === 'LeftVar') {
    bhUtil.moveNodeToAncestor(bhThis.findOuterNode(1), bhThis, [], ['StatVoid'], bhUserOpe);
    bhNodePlacer.deleteNode(bhThis, bhUserOpe);
  }
})();
