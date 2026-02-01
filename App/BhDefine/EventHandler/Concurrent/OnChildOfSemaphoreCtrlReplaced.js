(function() {
  if (bhThis.isDeleted()) {
    return;
  }
  // オリジナルノードであるセマフォ定義が消えた場合, Arg0 に接続された派生ノードであるセマフォ変数ノードが消えて新しいノードに入れ替わる.
  // セマフォ変数ノードが消えたとき, このノードを消す.
  let cnctrName = String(bhParentConnector.getSymbolName());
  if (cnctrName === 'Arg0') {
    bhUtil.moveNodeToAncestor(bhThis.findOuterNode(1), bhThis, [], ['StatVoid'], bhUserOpe);
    bhNodePlacer.deleteNode(bhThis, bhUserOpe);
  }
})();
