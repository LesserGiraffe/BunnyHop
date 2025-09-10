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

package net.seapanda.bunnyhop.view.node;

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
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.utility.SimpleCache;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.view.ViewConstructionException;
import net.seapanda.bunnyhop.view.ViewUtil;
import net.seapanda.bunnyhop.view.node.component.SelectableItem;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle.ConnectorAlignment;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle.ConnectorPos;
import net.seapanda.bunnyhop.view.traverse.NodeViewWalker;
import org.apache.commons.lang3.mutable.MutableBoolean;

/**
 * コンボボックスを入力フォームに持つビュー.
 *
 * @author K.Koike
 */
public final class ComboBoxNodeView extends BhNodeViewBase {

  private ComboBox<SelectableItem<String, Object>> comboBox = new ComboBox<>();
  private final TextNode model;
  private final MutableBoolean dragging = new MutableBoolean();
  /** コネクタ部分を含まないノードサイズのキャッシュデータ. */
  private SimpleCache<Vec2D> nodeSizeCache = new SimpleCache<Vec2D>(new Vec2D());
  /** コネクタ部分を含むノードサイズのキャッシュデータ. */
  private SimpleCache<Vec2D> nodeWithCnctrSizeCache = new SimpleCache<Vec2D>(new Vec2D());

  /**
   * コンストラクタ.
   *
   * @param model このノードビューに対応するノード
   * @param viewStyle このノードビューのスタイル
   * @throws ViewConstructionException ノードビューの初期化に失敗
   */
  public ComboBoxNodeView(TextNode model, BhNodeViewStyle viewStyle, SequencedSet<Node> components)
      throws ViewConstructionException {
    super(viewStyle, model, components);
    this.model = model;
    setComponent(comboBox);
    initStyle();
  }

  /**
   * コンストラクタ.
   *
   * @param viewStyle このノードビューのスタイル
   * @throws ViewConstructionException ノードビューの初期化に失敗
   */
  public ComboBoxNodeView(BhNodeViewStyle viewStyle)
      throws ViewConstructionException {
    this(null, viewStyle, new LinkedHashSet<>());
  }

  private void initStyle() {
    comboBox.getStyleClass().add(viewStyle.comboBox.cssClass);
    if (!comboBox.getItems().isEmpty()) {
      comboBox.setValue(comboBox.getItems().get(0));
    }
    comboBox.addEventFilter(Event.ANY, this::forwardEvent);
    comboBox.setButtonCell(new ComboBoxNodeListCell());
    comboBox.setCellFactory(items -> new ComboBoxNodeListCell());
    ViewUtil.enableAutoResize(comboBox, item -> item.getView().toString());
    getLookManager().addCssClass(BhConstants.Css.CLASS_COMBO_BOX_NODE);
  }

  /** コンボボックスの選択肢を登録する. */
  public void setItems(List<SelectableItem<String, Object>> items) {
    comboBox.setItems(FXCollections.observableArrayList(items));
  }

  /** コンボボックスに登録された選択肢を返す. */
  public List<SelectableItem<String, Object>> getItems() {
    return new ArrayList<>(comboBox.getItems());
  }
  
  /** ノードサイズのキャッシュ値を更新する. */
  private void updateNodeSizeCache(boolean includeCnctr, Vec2D nodeSize) {
    if (includeCnctr) {
      nodeWithCnctrSizeCache.update(new Vec2D(nodeSize));
    } else {
      nodeSizeCache.update(new Vec2D(nodeSize));
    }
  }

  @Override
  protected void onNodeSizeChanged() {
    nodeSizeCache.setDirty(true);
    nodeWithCnctrSizeCache.setDirty(true);
  }

  /**
   * このビューのモデルであるBhNodeを取得する.
   *
   * @return このビューのモデルであるBhNode
   */
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
  public void show(int depth) {
    try {
      System.out.println(
          "%s<TextNodeView>   %s".formatted(indent(depth), this.hashCode()));
      System.out.println(
          "%s<content>   %s".formatted(indent(depth + 1), comboBox.getValue()));
    } catch (Exception e) {
      System.out.println("TextNodeView show exception " + e);
    }
  }

  @Override
  protected void updatePosOnWorkspace(double posX, double posY) {
    getPositionManager().setPosOnWorkspace(posX, posY);
  }

  @Override
  protected Vec2D getNodeSize(boolean includeCnctr) {
    if (includeCnctr && !nodeWithCnctrSizeCache.isDirty()) {
      return new Vec2D(nodeWithCnctrSizeCache.getVal());
    }
    if (!includeCnctr && !nodeSizeCache.isDirty()) {
      return new Vec2D(nodeSizeCache.getVal());
    }

    Vec2D nodeSize = calcBodySize();
    if (includeCnctr) {
      Vec2D cnctrSize = getRegionManager().getConnectorSize();
      if (viewStyle.connectorPos == ConnectorPos.LEFT) {
        nodeSize = calcSizeIncludingLeftConnector(nodeSize, cnctrSize);
      } else if (viewStyle.connectorPos == ConnectorPos.TOP) {
        nodeSize = calcSizeIncludingTopConnector(nodeSize, cnctrSize);
      }
    }
    updateNodeSizeCache(includeCnctr, nodeSize);
    return nodeSize;
  }

  /** ノードのボディ部分のサイズを求める. */
  private Vec2D calcBodySize() {
    Vec2D commonPartSize = getRegionManager().getCommonPartSize();
    Vec2D innerSize = switch (viewStyle.baseArrangement) {
      case ROW ->
        new Vec2D(
            commonPartSize.x + comboBox.getWidth(),
            Math.max(commonPartSize.y, comboBox.getHeight()));
      case COLUMN ->
        new Vec2D(
            Math.max(commonPartSize.x, comboBox.getWidth()),
            commonPartSize.y + comboBox.getHeight());
    };
    return new Vec2D(
        viewStyle.paddingLeft + innerSize.x + viewStyle.paddingRight,
        viewStyle.paddingTop + innerSize.y + viewStyle.paddingBottom);
  }

  /**
   * 左にコネクタがついている場合のコネクタを含んだサイズを求める.
   *
   * @param bodySize ボディ部分のサイズ
   * @param cnctrSize コネクタサイズ
   * @return 左にコネクタがついている場合のコネクタを含んだサイズ
   */
  private Vec2D calcSizeIncludingLeftConnector(Vec2D bodySize, Vec2D cnctrSize) {
    double wholeWidth = bodySize.x + cnctrSize.x;
    // ボディの左上を原点としたときのコネクタの上端の座標
    double cnctrTopPos = viewStyle.connectorShift;
    if (viewStyle.connectorAlignment == ConnectorAlignment.CENTER) {
      cnctrTopPos += (bodySize.y - cnctrSize.y) / 2;
    }
    // ボディの左上を原点としたときのコネクタの下端の座標
    double cnctrBottomPos = cnctrTopPos + cnctrSize.y;
    double wholeHeight = Math.max(cnctrBottomPos, bodySize.y) - Math.min(cnctrTopPos, 0);
    return new Vec2D(wholeWidth, wholeHeight);
  }

  /**
   * 上にコネクタがついている場合のコネクタを含んだサイズを求める.
   *
   * @param bodySize ボディ部分のサイズ
   * @param cnctrSize コネクタサイズ
   * @return 左にコネクタがついている場合のコネクタを含んだサイズ
   */
  private Vec2D calcSizeIncludingTopConnector(Vec2D bodySize, Vec2D cnctrSize) {
    double wholeHeight = bodySize.y + cnctrSize.y;
    // ボディの左上を原点としたときのコネクタの左端の座標
    double cnctrLeftPos = viewStyle.connectorShift;
    if (viewStyle.connectorAlignment == ConnectorAlignment.CENTER) {
      cnctrLeftPos += (bodySize.x - cnctrSize.x) / 2;
    }
    // ボディの左上を原点としたときのコネクタの右端の座標
    double cnctrRightPos = cnctrLeftPos + cnctrSize.x;
    double wholeWidth = Math.max(cnctrRightPos, bodySize.x) - Math.min(cnctrLeftPos, 0);
    return new Vec2D(wholeWidth, wholeHeight);
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
