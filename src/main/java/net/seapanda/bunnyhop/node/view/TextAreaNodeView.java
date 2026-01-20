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

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.node.view.traverse.NodeViewWalker;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.utility.math.Vec2D;

/**
 * テキストエリアを入力フォームに持つビュー.
 *
 * @author K.Koike
 */
public final class TextAreaNodeView extends TextInputNodeView {

  private final TextArea textArea = new TextArea();
  private final TextNode model;
  /** クリック時にテキストを選択するかどうかのフラグ. */
  private boolean shouldSelectText = true;
  private final NodeSizeCalculator sizeCalculator;

  /**
   * コンストラクタ.
   *
   * @param model このノードビューに対応するノード
   * @param style このノードビューのスタイル
   * @throws ViewConstructionException ノードビューの初期化に失敗
   */
  public TextAreaNodeView(
      TextNode model, BhNodeViewStyle style, SequencedSet<Node> components)
      throws ViewConstructionException {
    super(model, style, components);
    this.model = model;
    sizeCalculator = new NodeSizeCalculator(this, this::getTextAreaSize);
    setComponent(textArea);
    textArea.addEventFilter(MouseEvent.ANY, this::forwardEvent);
    textArea.setOnMouseClicked(event -> Platform.runLater(this::selectText));
    textArea.focusedProperty().addListener((obs, oldVal, newVal) -> onFocusChanged(newVal));
    initStyle();
  }

  /**
   * コンストラクタ.
   *
   * @param style このノードビューのスタイル
   * @throws ViewConstructionException ノードビューの初期化に失敗
   */
  public TextAreaNodeView(BhNodeViewStyle style) throws ViewConstructionException {
    this(null, style, new LinkedHashSet<>());
  }

  private void forwardEvent(Event event) {
    BhNodeView view = (model == null) ? getTreeManager().getParentView() : this;
    if (view == null) {
      event.consume();
      return;
    }
    view.getCallbackRegistry().dispatch(event);    
    if (view.isTemplate()) {
      event.consume();
    }
  }

  private void initStyle() {
    textArea.getStyleClass().add(style.textArea.cssClass);
    textArea.setWrapText(false);
    textArea.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    textArea.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    setEditable(style.textArea.editable);
    getLookManager().addCssClass(BhConstants.Css.CLASS_TEXT_AREA_NODE);
  }

  private Vec2D getTextAreaSize() {
    // textArea.getWidth() だと設定した値以外が返る場合がある
    return new Vec2D(textArea.getPrefWidth(), textArea.getPrefHeight());
  }

  private void selectText() {
    if (textArea.isFocused() && textArea.isEditable() && shouldSelectText) {
      textArea.selectAll();
      shouldSelectText = false;
    }
  }

  private void onFocusChanged(boolean focused) {
    if (!focused) {
      textArea.deselect();
      shouldSelectText = true;
    }
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
   * テキスト変更時のイベントハンドラを登録する.
   *
   * @param fnCheckFormat 入力された文字列の形式が正しいかどうか判断する関数 (テキスト変更時のイベントハンドラから呼び出す)
   */
  public void setTextChangeListener(Function<String, Boolean> fnCheckFormat) {
    textArea.boundsInLocalProperty().addListener(
        (observable, oldVal, newVal) -> updateTextAreaLooks(fnCheckFormat));

    // テキストの長さに応じてTextArea のサイズが変わるようにする.
    textArea.textProperty().addListener(
        (observable, oldVal, newVal) ->  updateTextAreaLooks(fnCheckFormat));
  }

  /**
   * テキストエリアの見た目を変える.
   *
   * @param fnCheckFormat テキストのフォーマットをチェックする関数
   */
  private void updateTextAreaLooks(Function<String, Boolean> fnCheckFormat) {
    Text textPart = (Text) textArea.lookup(".text");
    Region content = (Region) textArea.lookup(".content");
    if (textPart == null || content == null) {
      return;
    }
    // 正確な文字部分の境界を取得するため, GUI部品内部のTextの境界は使わない.
    Vec2D textBounds = ViewUtil.calcStrBounds(
        textPart.getText(),
        textPart.getFont(),
        textPart.getBoundsType(),
        textPart.getLineSpacing());
    double newWidth = Math.max(textBounds.x, style.textArea.minWidth);
    // 幅を (文字幅 + パディング) にするとwrapの設定によらず文字列が折り返してしまういことがあるので定数 6 を足す
    // この定数はフォントやパディングが違っても機能する.
    newWidth += content.getPadding().getLeft() + content.getPadding().getRight() + 6;
    double newHeight = Math.max(textBounds.y, style.textArea.minHeight);
    newHeight += content.getPadding().getTop() + content.getPadding().getBottom() + 2;
    textArea.setPrefSize(newWidth, newHeight);
    boolean acceptable = fnCheckFormat.apply(textPart.getText());
    textArea.pseudoClassStateChanged(
        PseudoClass.getPseudoClass(BhConstants.Css.PSEUDO_ERROR), !acceptable);
    disableTextAreaCache();
    // textArea.requestLayout() を呼ばないと, newWidth の値によってはノード選択ビューでサイズが更新されない
    Platform.runLater(textArea::requestLayout);
  }

  /**
   * テキストエリアの描画データのキャッシュを無効化する.
   *
   * <p>この無効化を行わない場合, テキストエリアの文字がにじむ.
   */
  private void disableTextAreaCache() {
    textArea.setCache(false);
    if (textArea.getChildrenUnmodifiable().getFirst() instanceof ScrollPane sp) {
      sp.setCache(false);
      for (Node child : sp.getChildrenUnmodifiable()) {
        child.setCache(false);
      }
    }
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

  @Override
  protected TextInputControl getTextInputControl() {
    return textArea;
  }

  @Override
  public void accept(NodeViewWalker visitor) {
    visitor.visit(this);
  }
}
