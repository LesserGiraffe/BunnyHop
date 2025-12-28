(function () {
  // 右クリックでドラッグしたとき, 以下のノードを抜き出す.
  // - ドラッグされたノード (= A)
  // - A から選択済みの外部ノードを辿って到達できるノード群
  if (bhUiEvent.type.isDragDetected
      && bhUiEvent.isDragAndDropTarget
      && bhUiEvent.isSecondaryButtonPressed) {
    bhCommon.moveNodeToAncestor(
        bhCommon.findOuterNotSelected(bhThis), bhThis, [], ['GlobalDataDeclVoid'], bhUserOpe);
    return;
  }

  if (bhUiEvent.type.isMouseReleased
      && bhUiEvent.isDragAndDropTarget
      && bhUiEvent.isPrimaryButtonPressed
      && bhUiEvent.clickCount === 2) {
    // ダブルクリックしたとき, 外部末尾ノードまで選択する.
    bhCommon.selectToOutermost(bhThis, ['GlobalDataDeclVoid'], bhUserOpe);
  }
})();
