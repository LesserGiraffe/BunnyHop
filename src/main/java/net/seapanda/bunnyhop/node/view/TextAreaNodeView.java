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

import static net.seapanda.bunnyhop.ui.skin.HighlightingChangePolicy.REFRESH;

import java.util.LinkedHashSet;
import java.util.SequencedCollection;
import java.util.SequencedSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.node.view.traverse.NodeViewWalker;
import net.seapanda.bunnyhop.search.Substring;
import net.seapanda.bunnyhop.ui.skin.HighlightableTextAreaSkin;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import org.apache.commons.lang3.StringUtils;

/**
 * テキストエリアを入力フォームに持つビュー.
 *
 * @author K.Koike
 */
public final class TextAreaNodeView extends TextInputNodeView {

  private final TextNode model;
  private final TextArea textArea = new TextArea();
  private final Visual visual = new Visual(this);
  private final Geometry geometry;

  /** クリック時にテキストを選択するかどうかのフラグ. */
  private boolean shouldSelectText = true;
  /** {@link #textArea} がフォーカスを得る前に保持していたテキスト. */
  private String textBeforeFocused = "";

  /**
   * コンストラクタ.
   *
   * @param model このノードビューに対応するノード
   * @param style このノードビューのスタイル
   * @param components このノードビューに追加する GUI コンポーネント
   * @param isTemplate このノードビューがテンプレートノードビューの場合 true
   * @throws ViewConstructionException ノードビューの初期化に失敗
   */
  public TextAreaNodeView(
      TextNode model, BhNodeViewStyle style, SequencedSet<Node> components, boolean isTemplate)
      throws ViewConstructionException {
    super(model, style, components, isTemplate);
    this.model = model;
    geometry = new Geometry(this, new NodeSizeCalculator(this, this::getContentRegionSize)) {};
    setComponent(textArea);
    textArea.addEventFilter(MouseEvent.ANY, this::forwardEvent);
    textArea.setOnMouseClicked(event -> Platform.runLater(() -> onTextAreaClicked(event)));
    textArea.focusedProperty().addListener((obs, oldVal, newVal) -> onFocusChanged(newVal));
    setEditable(getStyle().textArea.editable);
    initializeStyle();
  }

  /**
   * コンストラクタ.
   *
   * @param style このノードビューのスタイル
   * @param isTemplate このノードビューがテンプレートノードビューの場合 true
   * @throws ViewConstructionException ノードビューの初期化に失敗
   */
  public TextAreaNodeView(BhNodeViewStyle style, boolean isTemplate)
      throws ViewConstructionException {
    this(null, style, new LinkedHashSet<>(), isTemplate);
  }

  private void forwardEvent(Event event) {
    BhNodeView view = (model == null) ? getTreeControl().getParentView() : this;
    if (view == null) {
      event.consume();
      return;
    }
    view.getCallbackRegistry().dispatch(event);    
    if (view.isTemplate()) {
      event.consume();
    }
  }

  private void initializeStyle() {
    textArea.getStyleClass().add(getStyle().textArea.cssClass);
    textArea.setWrapText(false);
    textArea.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    textArea.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    visual.addCssClass(getStyle().cssClasses);
    visual.addCssClass(BhConstants.Css.Class.BH_NODE);
    visual.addCssClass(BhConstants.Css.Class.TEXT_AREA_NODE);
  }

  private void onTextAreaClicked(MouseEvent event) {
    if (textArea.isFocused() && textArea.isEditable()) {
      if (shouldSelectText) {
        textArea.selectAll();
        shouldSelectText = false;
      } else if (event.getClickCount() == 2) {
        textArea.positionCaret(textArea.getLength());
      }
    }
  }

  /** {@link #textArea} のフォーカスステートが変わったときのイベントハンドラ. */
  private void onFocusChanged(boolean focused) {
    if (focused) {
      textBeforeFocused = getText();
      return;
    }
    textArea.deselect();
    shouldSelectText = true;
    if (!StringUtils.equals(textBeforeFocused, getText())) {
      var event = new TextChangeEvent(this, textBeforeFocused, getText());
      getCallbackRegistry().onTextChangedInvoker.invoke(event);
    }
  }

  /**
   * テキスト変更時のイベントハンドラを登録する.
   *
   * @param fnCheckFormat 入力された文字列の形式が正しいかどうか判断する関数 (テキスト変更時のイベントハンドラから呼び出す)
   */
  public void setTextChangeListener(Function<String, Boolean> fnCheckFormat) {
    textArea.boundsInLocalProperty().addListener(
        (observable, oldVal, newVal) -> updateTextAreaLooks(fnCheckFormat));

    // テキストの長さに応じてTextArea のサイズが変わるようにする.
    textArea.textProperty().addListener(
        (observable, oldVal, newVal) -> updateTextAreaLooks(fnCheckFormat));
  }

  /**
   * テキストエリアの見た目を変える.
   *
   * @param fnCheckFormat テキストのフォーマットをチェックする関数
   */
  private void updateTextAreaLooks(Function<String, Boolean> fnCheckFormat) {
    Text text = (Text) textArea.lookup(".text");
    Region content = (Region) textArea.lookup(".content");
    if (text == null || content == null) {
      return;
    }
    // 正確な文字部分の境界を取得するため, GUI部品内部のTextの境界は使わない.
    Vec2D textBounds = ViewUtil.calcStrBounds(
        text.getText(), text.getFont(), text.getBoundsType(), text.getLineSpacing());
    double newWidth = Math.max(textBounds.x, getStyle().textArea.minWidth);
    // 幅を (文字幅 + パディング) にするとwrapの設定によらず文字列が折り返してしまうことがあるので定数 6 を足す
    // この定数はフォントやパディングが違っても機能する.
    newWidth += content.getPadding().getLeft() + content.getPadding().getRight() + 6;
    double newHeight = Math.max(textBounds.y, getStyle().textArea.minHeight);
    newHeight += content.getPadding().getTop() + content.getPadding().getBottom() + 2;
    textArea.setPrefSize(newWidth, newHeight);
    boolean acceptable = fnCheckFormat.apply(text.getText());
    textArea.pseudoClassStateChanged(
        PseudoClass.getPseudoClass(BhConstants.Css.Pseudo.ERROR), !acceptable);
    // textArea.requestLayout() を呼ばないと, newWidth の値によってはノード選択ビューでサイズが更新されない
    Platform.runLater(textArea::requestLayout);
  }

  /** コンテンツを表示する領域の大きさを取得する. */
  private Vec2D getContentRegionSize() {
    // textArea.getWidth() だと設定した値以外が返る場合がある
    return new Vec2D(textArea.getPrefWidth(), textArea.getPrefHeight());
  }

  @Override
  TextInputControl getTextInputControl() {
    return textArea;
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

  /** ノードビューの視覚効果に関する機能を提供するクラス. */
  public static class Visual extends TextNodeView.Visual {

    /** テキストエリアに適用するスキン. */
    private final HighlightableTextAreaSkin skin;

    Visual(TextAreaNodeView view) {
      super(view);
      String styleClass = view.getStyle().textArea.textHighlight.cssClass;
      skin = new HighlightableTextAreaSkin(view.textArea, styleClass, REFRESH);
      view.textArea.setSkin(skin);
    }

    @Override
    public SequencedCollection<Substring> enableTextHighlighting(
        Pattern pattern, int maxHighlights) {
      return skin.enableHighlighting(pattern, maxHighlights);
    }

    @Override
    public void disableTextHighlighting() {
      skin.disableHighlighting();
    }

    @Override
    public SequencedCollection<Substring> getHighlightedTexts() {
      return skin.getHighlightedTexts();
    }

    @Override
    public boolean isTextHighlightingEnabled() {
      return skin.isHighlightingEnabled();
    }
  }
}
