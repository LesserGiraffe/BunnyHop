(function() {
  if (bhThis.isDeleted()) {
    return;
  }
  // オリジナルノードのリスト定義が消えた場合, Arg0 に接続された派生ノードである変数ノードが消えて Arg0 が入れ替わる.
  // 変数ノードが消えたとき, このノードを消す.
  if (String(bhParentConnector.getSymbolName()) === 'LeftVar') {
    bhCommon.moveNodeToAncestor(bhThis.findOuterNode(1), bhThis, [], ['VoidStat'], bhUserOpe);
    bhNodePlacer.deleteNode(bhThis, bhUserOpe);
  }
})();
