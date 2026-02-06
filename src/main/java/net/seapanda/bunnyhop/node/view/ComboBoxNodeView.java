/*
 * Copyright 2017 K.Koike
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.seapanda.bunnyhop.node.view;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseEvent;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.view.component.SelectableItem;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.node.view.traverse.NodeViewWalker;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import org.apache.commons.lang3.mutable.MutableBoolean;

/**
 * コンボボックスを入力フォームに持つビュー.
 *
 * @author K.Koike
 */
public final class ComboBoxNodeView extends BhNodeViewBase {

  private final ComboBox<SelectableItem<String, Object>> comboBox = new ComboBox<>();
  private final TextNode model;
  private final MutableBoolean dragging = new MutableBoolean();
  private final NodeSizeCalculator sizeCalculator;

  /**
   * コンストラクタ.
   *
   * @param model このノードビューに対応するノード
   * @param style このノードビューのスタイル
   * @param components このノードビューに追加する GUI コンポーネント
   * @param isTemplate このノードビューがテンプレートノードビューの場合 true
   * @throws ViewConstructionException ノードビューの初期化に失敗
   */
  public ComboBoxNodeView(
      TextNode model, BhNodeViewStyle style, SequencedSet<Node> components, boolean isTemplate)
      throws ViewConstructionException {
    super(style, model, components, isTemplate);
    this.model = model;
    sizeCalculator = new NodeSizeCalculator(this, this::getComboBoxSize);
    setComponent(comboBox);
    setEventHandlers();
    initStyle();
  }

  /**
   * コンストラクタ.
   *
   * @param style このノードビューのスタイル
   * @param isTemplate このノードビューがテンプレートノードビューの場合 true
   * @throws ViewConstructionException ノードビューの初期化に失敗
   */
  public ComboBoxNodeView(BhNodeViewStyle style, boolean isTemplate)
      throws ViewConstructionException {
    this(null, style, new LinkedHashSet<>(), isTemplate);
  }

  private void initStyle() {
    comboBox.getStyleClass().add(style.comboBox.cssClass);
    getLookManager().addCssClass(BhConstants.Css.Class.COMBO_BOX_NODE);
    if (!comboBox.getItems().isEmpty()) {
      comboBox.setValue(comboBox.getItems().getFirst());
    }
  }

  private Vec2D getComboBoxSize() {
    return new Vec2D(comboBox.getWidth(), comboBox.getHeight());
  }

  private void setEventHandlers() {
    comboBox.addEventFilter(Event.ANY, this::forwardEvent);
    comboBox.setButtonCell(new ComboBoxNodeListCell());
    comboBox.setCellFactory(items -> new ComboBoxNodeListCell());
    ViewUtil.enableAutoResize(comboBox, item -> item.getView().toString());
  }

  /** コンボボックスの選択肢を登録する. */
  public void setItems(List<SelectableItem<String, Object>> items) {
    comboBox.setItems(FXCollections.observableArrayList(items));
  }

  /** コンボボックスに登録された選択肢を返す. */
  public List<SelectableItem<String, Object>> getItems() {
    return new ArrayList<>(comboBox.getItems());
  }

  @Override
  protected void notifyChildSizeChanged() {
    sizeCalculator.notifyNodeSizeChanged();
    super.notifyChildSizeChanged();
  }

  @Override
  public Optional<TextNode> getModel() {
    return Optional.ofNullable(model);
  }

  /**
   * コンボボックスでアイテムが選択された時のイベントハンドラを登録する.
   *
   * @param handler 登録するイベントハンドラ
   */
  public void setOnItemSelected(ChangeListener<SelectableItem<String, Object>> handler) {
    comboBox.valueProperty().addListener(handler);
  }

  @Override
  protected void updatePosOnWorkspace(double posX, double posY) {
    getPositionManager().setPosOnWorkspace(posX, posY);
  }

  @Override
  protected Vec2D getNodeSize(boolean includeCnctr) {
    return sizeCalculator.calcNodeSize(includeCnctr);
  }

  @Override
  protected Vec2D getNodeTreeSize(boolean includeCnctr) {
    return getNodeSize(includeCnctr);
  }

  @Override
  protected void updateChildRelativePos() {}

  /**
   * 現在選択中のコンボボックスのアイテムを取得する.
   *
   * @return 現在のコンボボックスのテキスト
   */
  public SelectableItem<String, Object> getValue() {
    return comboBox.getValue();
  }

  /**
   * 引数で指定した文字列を modelText として持つ SelectableItem を取得する.
   *
   * @param text このテキストを modelText として持つ {@link SelectableItem} を見つける
   * @return 引数で指定した文字列を modelText として持つ {@link SelectableItem}
   */
  public Optional<SelectableItem<String, Object>> getItemByModelText(String text) {
    for (SelectableItem<String, Object> item : comboBox.getItems()) {
      if (item.getModel().equals(text)) {
        return Optional.of(item);
      }
    }
    return Optional.empty();
  }

  /**
   * コンボボックスのアイテムを設定する.
   *
   * @param item 選択する要素
   */
  public void setValue(SelectableItem<String, Object> item) {    
    comboBox.setValue(item);
  }

  private void forwardEvent(Event event) {
    BhNodeView view = (model == null) ? getTreeManager().getParentView() : this;
    if (view == null) {
      event.consume();
      return;
    }
    view.getCallbackRegistry().dispatch(event);
    if (view.isTemplate() || dragging.getValue()) {
      event.consume();
    }
    if (event.getEventType().equals(MouseEvent.DRAG_DETECTED)) {
      dragging.setTrue();
    } else if (event.getEventType().equals(MouseEvent.MOUSE_RELEASED)) {
      dragging.setFalse();
    }
  }

  @Override
  public void accept(NodeViewWalker visitor) {
    visitor.visit(this);
  }

  /** このノードビューが持つコンボボックスのアイテムの View. */
  private static class ComboBoxNodeListCell extends ListCell<SelectableItem<String, Object>> {

    @Override
    protected void updateItem(SelectableItem<String, Object> item, boolean empty) {
      super.updateItem(item, empty);
      if (!empty && item != null) {
        setText(item.getView().toString());
      } else {
        setText(null);
      }
    }
  }
}
