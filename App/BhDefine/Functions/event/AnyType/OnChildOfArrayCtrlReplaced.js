(function() {
  if (bhThis.isDeleted()) {
    return;
  }
  // オリジナルノードのリスト定義が消えた場合, Arg0 に接続された派生ノードであるリストノードが消えて Arg0 が入れ替わる.
  // リストノードが消えたとき, このノードを消す.
  if (String(bhParentConnector.getSymbolName()) === 'Arg0') {
    bhCommon.reconnect(bhThis.findOuterNode(1), bhThis, [], bhUserOpe);
    bhNodePlacer.deleteNode(bhThis, bhUserOpe);
    return;
  }
})();
