(function() {
  if (bhThis.isDeleted()) {
    return;
  }
  // オリジナルノードのリスト定義が消えた場合, Arg0 に接続されたイミテーションであるリストノードが消えて Arg0 が入れ替わる.
  // リストノードが消えたとき, このノードを消す.
  if (String(bhParentConnector.getSymbolName()) === 'Arg0') {
    bhCommon.reconnectOuter(bhThis, [], bhMsgService, bhNodeHandler, bhUserOpeCmd);
    bhNodeHandler.deleteNode(bhThis, bhUserOpeCmd);
    return;
  }
})();
