(function() {
  if (bhThis.isDeleted()) {
    return;
  }
  // オリジナルノードのリスト定義が消えた場合, Arg0 に接続されたイミテーションである変数ノードが消えて Arg0 が入れ替わる.
  // 変数ノードが消えたとき, このノードを消す.
  if (String(bhParentConnector.getSymbolName()) === 'LeftVar') {
    bhCommon.reconnectOuter(bhThis, [], bhMsgService, bhNodeHandler, bhUserOpeCmd);
    bhNodeHandler.deleteNode(bhThis, bhUserOpeCmd);
    return;
  }
})();
