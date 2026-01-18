(function() {
  if (bhThis.isDeleted()) {
    return;
  }
  // オリジナルノードのリスト定義が消えた場合, Arg0 に接続された派生ノードであるリストノードが消えて Arg0 が入れ替わる.
  // リストノードが消えたとき, このノードを消す.
  if (String(bhParentConnector.getSymbolName()) === 'Arg0') {
    bhCommon.moveNodeToAncestor(bhThis.findOuterNode(1), bhThis, [], ['StatVoid'], bhUserOpe);
    bhNodePlacer.deleteNode(bhThis, bhUserOpe);
    return;
  }
})();
