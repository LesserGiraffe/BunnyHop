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
import java.util.SequencedCollection;
import java.util.SequencedSet;
import java.util.regex.Pattern;
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
import net.seapanda.bunnyhop.search.Substring;
import net.seapanda.bunnyhop.ui.skin.HighlightableListCellSkin;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import org.apache.commons.lang3.mutable.MutableBoolean;

/**
 * コンボボックスを入力フォームに持つビュー.
 *
 * @author K.Koike
 */
public final class ComboBoxNodeView extends TextNodeView {

  private final TextNode model;
  private final ComboBox<SelectableItem<String, Object>> comboBox = new ComboBox<>();
  private final MutableBoolean dragging = new MutableBoolean();
  private final Visual visual = new Visual(this);
  private final Geometry geometry;

  /**
   * コンストラクタ.
   *
   * @param model      このノードビューに対応するノード
   * @param style      このノードビューのスタイル
   * @param components このノードビューに追加する GUI コンポーネント
   * @param isTemplate このノードビューがテンプレートノードビューの場合 true
   * @throws ViewConstructionException ノードビューの初期化に失敗
   */
  public ComboBoxNodeView(
      TextNode model, BhNodeViewStyle style, SequencedSet<Node> components, boolean isTemplate)
      throws ViewConstructionException {
    super(model, style, components, isTemplate);
    this.model = model;
    geometry = new Geometry(this, new NodeSizeCalculator(this, this::getContentRegionSize)) {};
    setComponent(comboBox);
    setEventHandlers();
    initializeStyle();
    initializeItem();
  }

  /**
   * コンストラクタ.
   *
   * @param style      このノードビューのスタイル
   * @param isTemplate このノードビューがテンプレートノードビューの場合 true
   * @throws ViewConstructionException ノードビューの初期化に失敗
   */
  public ComboBoxNodeView(BhNodeViewStyle style, boolean isTemplate)
      throws ViewConstructionException {
    this(null, style, new LinkedHashSet<>(), isTemplate);
  }

  private void initializeStyle() {
    visual.addCssClass(getStyle().cssClasses);
    visual.addCssClass(BhConstants.Css.Class.BH_NODE);
    visual.addCssClass(BhConstants.Css.Class.COMBO_BOX_NODE);
    comboBox.getStyleClass().add(getStyle().comboBox.cssClass);
  }

  private void initializeItem() {
    if (!comboBox.getItems().isEmpty()) {
      comboBox.setValue(comboBox.getItems().getFirst());
    }
  }

  private void setEventHandlers() {
    comboBox.addEventFilter(Event.ANY, this::forwardEvent);
    addOnItemSelected((obs, oldVal, newVal) -> onItemChanged(oldVal, newVal));
    ViewUtil.enableAutoResize(comboBox, item -> item.getView().toString());
  }

  /**
   * コンボボックスのアイテムが変更されたときの処理.
   */
  private void onItemChanged(
      SelectableItem<String, Object> oldVal, SelectableItem<String, Object> newVal) {
    String oldText = oldVal == null ? null : oldVal.getView().toString();
    String newText = newVal == null ? null : newVal.getView().toString();
    var event = new TextChangeEvent(this, oldText, newText);
    getCallbackRegistry().onTextChangedInvoker.invoke(event);
  }

  /**
   * コンボボックスの選択肢を登録する.
   */
  public void setItems(List<SelectableItem<String, Object>> items) {
    comboBox.setItems(FXCollections.observableArrayList(items));
  }

  /**
   * コンボボックスに登録された選択肢を返す.
   */
  public List<SelectableItem<String, Object>> getItems() {
    return new ArrayList<>(comboBox.getItems());
  }

  /**
   * コンボボックスでアイテムが選択された時のイベントハンドラを追加する.
   *
   * @param handler 登録するイベントハンドラ
   */
  public void addOnItemSelected(ChangeListener<? super SelectableItem<String, Object>> handler) {
    comboBox.valueProperty().addListener(handler);
  }

  /**
   * 現在選択中のコンボボックスのアイテムを取得する.
   *
   * @return 現在のコンボボックスのテキスト
   */
  public SelectableItem<String, Object> getValue() {
    return comboBox.getValue();
  }

  /**
   * コンボボックスに登録された選択肢のうち, {@link SelectableItem#getModel()} が
   * {@code text} と一致する {@link SelectableItem}を取得する.
   *
   * @param text 検索対象の文字列
   * @return 一致するアイテム. 一致するものがない場合は {@link Optional#empty()}
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
    BhNodeView view = (model == null) ? getTreeControl().getParentView() : this;
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

  /** コンテンツを表示する領域の大きさを取得する. */
  private Vec2D getContentRegionSize() {
    return new Vec2D(comboBox.getWidth(), comboBox.getHeight());
  }

  @Override
  public String getText() {
    return comboBox.getValue().getView().toString();
  }

  @Override
  public Visual getVisual() {
    return visual;
  }

  @Override
  public Geometry getGeometry() {
    return geometry;
  }

  @Override
  public void accept(NodeViewWalker visitor) {
    visitor.visit(this);
  }

  /**
   * このノードビューが持つコンボボックスのアイテムの View.
   */
  private static class ComboBoxNodeListCell extends ListCell<SelectableItem<String, Object>> {

    private final HighlightableListCellSkin<SelectableItem<String, Object>> skin;

    ComboBoxNodeListCell(String styleClass) {
      skin = new HighlightableListCellSkin<>(this, styleClass);
      setSkin(skin);
    }

    @Override
    protected void updateItem(SelectableItem<String, Object> item, boolean empty) {
      super.updateItem(item, empty);
      if (!empty && item != null) {
        setText(item.getView().toString());
      } else {
        setText(null);
      }
    }

    /**
     * セルのテキストの強調表示を有効化する.
     *
     * @param pattern 強調表示する文字列の正規表現
     * @param maxHighlights  強調表示する箇所の上限.  負の数を指定すると全ての一致箇所を強調表示する.
     */
    private SequencedCollection<Substring> enableHighlighting(Pattern pattern, int maxHighlights) {
      return skin.enableHighlighting(pattern, maxHighlights);
    }

    /** セルのテキストの強調表示を無効化する. */
    private void disableHighlighting() {
      skin.disableHighlighting();
    }

    /** セル内の現在強調表示されている文字列のリストを返す. */
    private SequencedCollection<Substring> getHighlightedTexts() {
      return skin.getHighlightedTexts();
    }
  }

  /**
   * ノードビューの視覚効果に関する機能を提供するクラス.
   */
  public static class Visual extends TextNodeView.Visual {
    /** コンボボックスが持つセル一覧. */
    private final List<ComboBoxNodeListCell> cells = new ArrayList<>();
    /** 現在有効になっている強調表示のパターン. */
    private Pattern highlightPattern;
    /** 強調表示する箇所の上限. */
    private int maxHighlights;

    private Visual(ComboBoxNodeView view) {
      super(view);
      String styleClass = view.getStyle().comboBox.textHighlight.cssClass;
      view.comboBox.setButtonCell(createCell(styleClass));
      view.comboBox.setCellFactory(listView -> createCell(styleClass));
    }

    private ComboBoxNodeListCell createCell(String styleClass) {
      var cell = new ComboBoxNodeListCell(styleClass);
      if (isTextHighlightingEnabled()) {
        cell.enableHighlighting(highlightPattern, maxHighlights);
      }
      cells.add(cell);
      return cell;
    }

    @Override
    public SequencedCollection<Substring> enableTextHighlighting(
        Pattern pattern, int maxHighlights) {
      highlightPattern = pattern;
      this.maxHighlights = maxHighlights;
      cells.subList(1, cells.size())
          .forEach(cell -> cell.enableHighlighting(pattern, maxHighlights));
      return cells.getFirst().enableHighlighting(pattern, maxHighlights);
    }

    @Override
    public void disableTextHighlighting() {
      this.highlightPattern = null;
      cells.forEach(ComboBoxNodeListCell::disableHighlighting);
    }

    @Override
    public SequencedCollection<Substring> getHighlightedTexts() {
      return cells.getFirst().getHighlightedTexts();
    }

    @Override
    public boolean isTextHighlightingEnabled() {
      return highlightPattern != null;
    }
  }
}
