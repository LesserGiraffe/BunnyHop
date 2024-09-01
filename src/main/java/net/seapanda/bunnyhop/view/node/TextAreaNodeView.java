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

import java.util.function.Function;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.view.ViewHelper;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle.ConnectorPos;
import net.seapanda.bunnyhop.viewprocessor.NodeViewProcessor;

/**
 * テキストエリアを入力フォームに持つビュー.
 *
 * @author K.Koike
 */
public final class TextAreaNodeView  extends TextInputNodeView {

  private TextArea textArea = new TextArea();
  private final TextNode model;

  /**
   * コンストラクタ.
   *
   * @param model このノードビューに対応するノード
   * @param viewStyle このノードビューのスタイル
   * @throws ViewInitializationException ノードビューの初期化に失敗
   */
  public TextAreaNodeView(TextNode model, BhNodeViewStyle viewStyle)
      throws ViewInitializationException {
    super(model, viewStyle);
    this.model = model;
    getTreeManager().addChild(textArea);
    textArea.addEventFilter(MouseEvent.ANY, this::propagateEvent);
    initStyle();
  }

  private void propagateEvent(Event event) {
    getEventManager().propagateEvent(event);
    if (MsgService.INSTANCE.isTemplateNode(model)) {
      event.consume();
    }
  }

  private void initStyle() {
    textArea.setTranslateX(viewStyle.paddingLeft);
    textArea.setTranslateY(viewStyle.paddingTop);
    textArea.getStyleClass().add(viewStyle.textArea.cssClass);
    textArea.heightProperty().addListener((observable, oldVal, newVal) -> notifySizeChange());
    textArea.widthProperty().addListener((observable, oldVal, newVal) -> notifySizeChange());
    textArea.setWrapText(false);
    textArea.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    textArea.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
    getLookManager().addCssClass(BhParams.Css.CLASS_TEXT_AREA_NODE);
  }

  @Override
  public TextNode getModel() {
    return model;
  }

  /**
   * テキスト変更時のイベントハンドラを登録する.
   *
   * @param checkFormatFunc 入力された文字列の形式が正しいかどうか判断する関数 (テキスト変更時のイベントハンドラから呼び出す)
   */
  public void setTextChangeListener(Function<String, Boolean> checkFormatFunc) {
    textArea.boundsInLocalProperty().addListener(
        (observable, oldVal, newVal) -> updateTextAreaLooks(checkFormatFunc));

    // テキストの長さに応じてTextArea のサイズが変わるようにする.
    textArea.textProperty().addListener(
        (observable, oldVal, newVal) ->  updateTextAreaLooks(checkFormatFunc));
  }

  /**
   * テキストエリアの見た目を変える.
   *
   * @param checkFormatFunc テキストのフォーマットをチェックする関数
   * @param text このテキストに基づいてテキストエリアの見た目を変える
   */
  private void updateTextAreaLooks(Function<String, Boolean> checkFormatFunc) {
    Text textPart = (Text) textArea.lookup(".text");
    Region content = (Region) textArea.lookup(".content");
    if (textPart == null || content == null) {
      return;
    }

    // 正確な文字部分の境界を取得するため, GUI部品内部のTextの境界は使わない.
    Vec2D textBounds = ViewHelper.INSTANCE.calcStrBounds(
        textPart.getText(),
        textPart.getFont(),
        textPart.getBoundsType(),
        textPart.getLineSpacing());
    double newWidth = Math.max(textBounds.x, viewStyle.textArea.minWidth);
    //幅を (文字幅 + パディング) にするとwrapの設定によらず文字列が折り返してしまういことがあるので定数 6 を足す
    //この定数はフォントやパディングが違っても機能する.
    newWidth += content.getPadding().getLeft() + content.getPadding().getRight() + 6;
    double newHeight = Math.max(textBounds.y, viewStyle.textArea.minHeight);
    newHeight += content.getPadding().getTop() + content.getPadding().getBottom() + 2;
    textArea.setPrefSize(newWidth, newHeight);
    boolean acceptable = checkFormatFunc.apply(textPart.getText());
    textArea.pseudoClassStateChanged(
        PseudoClass.getPseudoClass(BhParams.Css.PSEUDO_ERROR), !acceptable);
    // textArea.requestLayout() を呼ばないと, newWidth の値によってはノード選択ビューでサイズが更新されない.
    Platform.runLater(() -> textArea.requestLayout());
  }

  @Override
  public void show(int depth) {
    MsgPrinter.INSTANCE.msgForDebug(
        indent(depth) + "<" + getClass().getSimpleName() + ">   " + this.hashCode());
    MsgPrinter.INSTANCE.msgForDebug(
        indent(depth + 1) + "<content" + ">   " + textArea.getText());
  }

  @Override
  protected void arrangeAndResize() {
    getLookManager().updatePolygonShape();
  }

  @Override
  protected Vec2D getBodySize(boolean includeCnctr) {
    Vec2D cnctrSize = viewStyle.getConnectorSize();
    // textField.getWidth() だと設定した値以外が返る場合がある
    double bodyWidth = viewStyle.paddingLeft + textArea.getPrefWidth() + viewStyle.paddingRight;
    if (includeCnctr && (viewStyle.connectorPos == ConnectorPos.LEFT)) {
      bodyWidth += cnctrSize.x;
    }
    double bodyHeight = viewStyle.paddingTop + textArea.getPrefHeight() + viewStyle.paddingBottom;
    if (includeCnctr && (viewStyle.connectorPos == ConnectorPos.TOP)) {
      bodyHeight += cnctrSize.y;
    }
    return new Vec2D(bodyWidth, bodyHeight);
  }

  @Override
  protected Vec2D getNodeSizeIncludingOuter(boolean includeCnctr) {
    return getBodySize(includeCnctr);
  }

  @Override
  protected TextInputControl getTextInputControl() {
    return textArea;
  }

  @Override
  public void accept(NodeViewProcessor visitor) {
    visitor.visit(this);
  }
}
