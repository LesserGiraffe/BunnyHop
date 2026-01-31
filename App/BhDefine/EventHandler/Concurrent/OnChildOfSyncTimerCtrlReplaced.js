(function() {
  if (bhThis.isDeleted()) {
    return;
  }
  // オリジナルノードである同期タイマー定義が消えた場合, Arg0 に接続された派生ノードである同期タイマー変数ノードが消えて新しいノードに入れ替わる.
  // 同期タイマー変数ノードが消えたとき, このノードを消す.
  let cnctrName = String(bhParentConnector.getSymbolName());
  if (cnctrName === 'Arg0') {
    bhCommon.moveNodeToAncestor(bhThis.findOuterNode(1), bhThis, [], ['StatVoid'], bhUserOpe);
    bhNodePlacer.deleteNode(bhThis, bhUserOpe);
  }
})();
