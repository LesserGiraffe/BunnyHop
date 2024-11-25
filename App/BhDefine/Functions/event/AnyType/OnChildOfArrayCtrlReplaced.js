(function() {
  if (bhThis.isDeleted()) {
    return;
  }
  // オリジナルノードのリスト定義が消えた場合, Arg0 に接続された派生ノードであるリストノードが消えて Arg0 が入れ替わる.
  // リストノードが消えたとき, このノードを消す.
  if (String(bhParentConnector.getSymbolName()) === 'Arg0') {
    bhCommon.reconnectOuter(bhThis, [], bhMsgService, bhUserOpe);
    bhNodeHandler.deleteNode(bhThis, bhUserOpe);
    return;
  }
})();
