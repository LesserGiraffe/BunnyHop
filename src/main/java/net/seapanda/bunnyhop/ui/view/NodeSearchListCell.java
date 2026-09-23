package net.seapanda.bunnyhop.ui.view;

import javafx.scene.control.ListCell;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.ui.model.NodeSearchListItem;
import net.seapanda.bunnyhop.ui.skin.HighlightableListCellSkin;

/**
 * ノード検索結果に表示される要素のビュー.
 *
 * @author K.Koike
 */
public class NodeSearchListCell extends ListCell<NodeSearchListItem> {

  private NodeSearchListItem model;
  private boolean empty = true;
  private final HighlightableListCellSkin<NodeSearchListItem> skin;
  private boolean isHighlightingEnabled = false;
  private String styleClass = "";

  /** コンストラクタ. */
  public NodeSearchListCell() {
    getStyleClass().add(BhConstants.Css.Class.NODE_SEARCH_RESULT_ITEM);
    skin = new HighlightableListCellSkin<>(this);
    setSkin(skin);
    addEventFilter(MouseEvent.MOUSE_PRESSED, this::changeSelectionState);
  }

  private void changeSelectionState(MouseEvent event) {
    var selModel = getListView().getSelectionModel();
    if (empty || model == null || model == selModel.getSelectedItem()) {
      getListView().getSelectionModel().clearSelection();
    } else {
      selModel.select(getIndex());
    }
    getListView().requestFocus();
    event.consume();
  }

  @Override
  protected void updateItem(NodeSearchListItem item, boolean empty) {
    super.updateItem(item, empty);
    setText(getText(item, empty));
    updateHighlighting(item, empty);
    model = item;
    this.empty = empty;
  }

  private static String getText(NodeSearchListItem item, boolean empty) {
    if (empty || item == null) {
      return null;
    }
    return item.toString();
  }

  /** {@link NodeSearchListCell} オブジェクトが {@code item} と紐づく場合に UI に表示される文字列を返す. */
  public static String getText(NodeSearchListItem item) {
    String text = getText(item, false);
    return text == null ? "" : text;
  }

  /** {@code item} に応じてこのセルのテキストの強調表示を更新する. */
  private void updateHighlighting(NodeSearchListItem item, boolean empty) {
    if (isHighlightingEnabled && item != null && !empty) {
      skin.enableHighlighting(item.getMatchedStringPattern(), styleClass);
    } else {
      skin.disableHighlighting();
    }
  }

  /**
   * このセルのテキストの強調表示を有効化する.
   *
   * @param styleClass 強調表示部分に適用するスタイルクラス
   */
  public void enableHighlighting(String styleClass) {
    isHighlightingEnabled = true;
    this.styleClass = styleClass;
    if (model != null && !empty) {
      skin.enableHighlighting(model.getMatchedStringPattern(), styleClass);
    }
  }

  /** このセルのテキストの強調表示を無効化する. */
  public void disableHighlighting() {
    isHighlightingEnabled = false;
    skin.disableHighlighting();
  }

  /** このセルに割り当てられたアイテムが変わったときのイベント. */
  public record ItemChangeEvent(
      NodeSearchListCell cell,
      NodeSearchListItem oldVal,
      NodeSearchListItem newVal,
      boolean empty) {}
}
