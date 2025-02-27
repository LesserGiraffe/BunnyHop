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
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.utility.SimpleCache;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.ViewConstructionException;
import net.seapanda.bunnyhop.view.ViewUtil;
import net.seapanda.bunnyhop.view.node.component.SelectableItem;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle.ConnectorPos;
import net.seapanda.bunnyhop.view.traverse.NodeViewWalker;
import org.apache.commons.lang3.mutable.MutableBoolean;

/**
 * コンボボックスを入力フォームに持つビュー.
 *
 * @author K.Koike
 */
public final class ComboBoxNodeView extends BhNodeViewBase {

  private ComboBox<SelectableItem> comboBox = new ComboBox<>();
  private final TextNode model;
  private final ListCell<SelectableItem> buttonCell = new ComboBoxNodeListCell();
  private final MutableBoolean dragging = new MutableBoolean(false);
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
    addComponent(comboBox);
    initStyle();
    setComboBoxEventHandlers();
  }

  /**
   * コンストラクタ.
   *
   * @param viewStyle このノードビューのスタイル
   * @throws ViewConstructionException ノードビューの初期化に失敗
   */
  public ComboBoxNodeView(BhNodeViewStyle viewStyle)
      throws ViewConstructionException {
    this(null, viewStyle, null);
  }

  private void initStyle() {
    comboBox.setButtonCell(buttonCell);
    comboBox.setTranslateX(viewStyle.paddingLeft);
    comboBox.setTranslateY(viewStyle.paddingTop);
    comboBox.getStyleClass().add(viewStyle.comboBox.cssClass);
    comboBox.heightProperty().addListener(observable -> onNodeSizeChanged());
    comboBox.widthProperty().addListener(observable -> onNodeSizeChanged());
    if (!comboBox.getItems().isEmpty()) {
      comboBox.setValue(comboBox.getItems().get(0));
    }
    getLookManager().addCssClass(BhConstants.Css.CLASS_COMBO_BOX_NODE);
  }

  /** コンボボックスの選択肢を登録する. */
  public void setItems(List<SelectableItem> items) {
    comboBox.setItems(FXCollections.observableArrayList(items));
  }

  /** コンボボックスに登録された選択肢を返す. */
  public List<SelectableItem> getItems() {
    return new ArrayList<>(comboBox.getItems());
  }
  
  /** ノードサイズのキャッシュを dirty にして, ノードの大きさが変わったことを親グループに伝える. */
  private void onNodeSizeChanged() {
    nodeSizeCache.setDirty(true);
    nodeWithCnctrSizeCache.setDirty(true);
    BhNodeViewGroup group = getTreeManager().getParentGroup();
    if (group != null) {
      group.notifyChildSizeChanged();
    }
    if (getTreeManager().isRootView()) {
      getLookManager().requestArrangement();
    }
  }

  /** ノードサイズのキャッシュ値を更新する. */
  private void updateNodeSizeCache(boolean includeCnctr, Vec2D nodeSize) {
    if (includeCnctr) {
      nodeWithCnctrSizeCache.update(new Vec2D(nodeSize));
    } else {
      nodeSizeCache.update(new Vec2D(nodeSize));
    }
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
   * コンボボックスのアイテム変化時のイベントハンドラを登録する.
   *
   * @param handler コンボボックスのアイテム変化時のイベントハンドラ
   */
  public void setTextChangeListener(ChangeListener<SelectableItem> handler) {
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

    Vec2D cnctrSize = viewStyle.getConnectorSize(isFixed());
    double bodyWidth = viewStyle.paddingLeft + comboBox.getWidth() + viewStyle.paddingRight;
    if (includeCnctr && (viewStyle.connectorPos == ConnectorPos.LEFT)) {
      bodyWidth += cnctrSize.x;
    }
    double bodyHeight = viewStyle.paddingTop + comboBox.getHeight() + viewStyle.paddingBottom;
    if (includeCnctr && (viewStyle.connectorPos == ConnectorPos.TOP)) {
      bodyHeight += cnctrSize.y;
    }
    var nodeSize = new Vec2D(bodyWidth, bodyHeight);
    updateNodeSizeCache(includeCnctr, nodeSize);
    return nodeSize;
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
  public SelectableItem getValue() {
    return comboBox.getValue();
  }

  /**
   * 引数で指定した文字列を modelText として持つ SelectableItem を取得する.
   *
   * @param text このテキストを modelText として持つ {@link SelectableItem} を見つける
   * @return 引数で指定した文字列を modelText として持つ {@link SelectableItem}
   */
  public Optional<SelectableItem> getItemByModelText(String text) {
    for (SelectableItem item : comboBox.getItems()) {
      if (item.getModelText().equals(text)) {
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
  public void setValue(SelectableItem item) {    
    comboBox.setValue(item);
  }

  /** コンボボックスの垂直スクロールバーを取得する. */
  private ScrollBar getVerticalScrollbar() {
    ScrollBar result = null;
    for (Node node : buttonCell.getListView().lookupAll(".scroll-bar")) {
      if (node instanceof ScrollBar) {
        ScrollBar bar = (ScrollBar) node;
        if (bar.getOrientation().equals(Orientation.VERTICAL)) {
          result = bar;
        }
      }
    }
    return result;
  }

  /**
   * 引数で指定した文字列のリストの最大幅を求める.
   *
   * @param items 文字列のリスト
   * @param font 文字列の幅を求める際のフォント
   * @return 引数で指定した文字列のリストの最大幅
   */
  private double calcMaxStrWidth(List<String> strList, Font font) {
    double width = 0.0;
    for (String str : strList) {
      double strWidth = ViewUtil.calcStrWidth(str, font);
      width = Math.max(width, strWidth);
    }
    return width;
  }

  /** コンボボックスに関連するイベントハンドラを設定する. */
  private void setComboBoxEventHandlers() {
    comboBox.addEventFilter(Event.ANY, this::propagateEvent);
    comboBox.setOnShowing(event -> fitComboBoxWidthToListWidth());
    comboBox.setOnHidden(event -> fitComboBoxWidthToContentWidth(
        comboBox.getValue().getViewString(), buttonCell.getFont()));
  }

  private void propagateEvent(Event event) {
    BhNodeView view = (model == null) ? getTreeManager().getParentView() : this;
    if (view == null) {
      event.consume();
      return;
    }
    view.getEventManager().propagateEvent(event);
    view.getModel().ifPresent(node -> {
      if (node.getViewProxy().isTemplateNode() || dragging.getValue()) {
        event.consume();  
      }
    });
    if (event.getEventType().equals(MouseEvent.DRAG_DETECTED)) {
      dragging.setTrue();
    } else if (event.getEventType().equals(MouseEvent.MOUSE_RELEASED)) {
      dragging.setFalse();
    }
  }

  /** コンボボックスの幅を表示されているリストの幅に合わせる. */
  private void fitComboBoxWidthToListWidth() {
    List<String> itemTextList = new ArrayList<>();
    comboBox.getItems().forEach(item -> itemTextList.add(item.getViewString()));
    double maxWidth = calcMaxStrWidth(itemTextList, buttonCell.fontProperty().get());
    ScrollBar scrollBar = getVerticalScrollbar();
    if (scrollBar != null) {
      maxWidth += scrollBar.getWidth();
    }
    maxWidth += buttonCell.getInsets().getLeft() + buttonCell.getInsets().getRight();
    maxWidth += buttonCell.getPadding().getLeft() + buttonCell.getPadding().getRight();
    buttonCell.getListView().setPrefWidth(maxWidth);
  }

  /** コンボボックスの幅を表示されている文字の幅に合わせる. */
  private void fitComboBoxWidthToContentWidth(String currentStr, Font font) {
    double width = ViewUtil.calcStrWidth(currentStr, font);
    width += buttonCell.getInsets().getLeft() + buttonCell.getInsets().getRight();
    width += buttonCell.getPadding().getLeft() + buttonCell.getPadding().getRight();
    buttonCell.getListView().setPrefWidth(width);
  }

  @Override
  public void accept(NodeViewWalker visitor) {
    visitor.visit(this);
  }

  /** BhNode カテゴリの View.  BhNodeCategoryとの結びつきは動的に変わる. */
  public class ComboBoxNodeListCell extends ListCell<SelectableItem> {

    SelectableItem item;

    public ComboBoxNodeListCell() {}

    @Override
    protected void updateItem(SelectableItem item, boolean empty) {
      super.updateItem(item, empty);
      this.item = item;
      if (!empty) {
        setText(item.getViewString());
        fitComboBoxWidthToContentWidth(item.getViewString(), getFont());
      }
    }
  }
}
