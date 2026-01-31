(function() {
  if (bhThis.isDeleted()) {
    return;
  }
  // オリジナルノードである変数定義が消えた場合, Arg0 に接続された派生ノードである変数ノードが消えて Arg0 が入れ替わる.
  // 変数ノードが消えたとき, このノードを消す.
  if (String(bhParentConnector.getSymbolName()) === 'LeftVar') {
    bhCommon.moveNodeToAncestor(bhThis.findOuterNode(1), bhThis, [], ['StatVoid'], bhUserOpe);
    bhNodePlacer.deleteNode(bhThis, bhUserOpe);
  }
})();
