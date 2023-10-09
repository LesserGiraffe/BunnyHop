/**
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

import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.view.ViewHelper;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle.CNCTR_POS;
import net.seapanda.bunnyhop.viewprocessor.NodeViewProcessor;

/**
 * テキストフィールドを入力フォームに持つビュー
 * @author K.Koike
 */
public final class TextFieldNodeView extends TextInputNodeView {

  private TextField textField = new TextField();
  private final TextNode model;

  /**
   * コンストラクタ
   * @param model このノードビューに対応するノード
   * @param viewStyle このノードビューのスタイル
   * @throws ViewInitializationException ノードビューの初期化に失敗
   */
  public TextFieldNodeView(TextNode model, BhNodeViewStyle viewStyle)
    throws ViewInitializationException {

    super(model, viewStyle);
    this.model = model;
    getTreeManager().addChild(textField);
    textField.addEventFilter(MouseEvent.ANY, this::propagateEvent);
    initStyle();
  }


  private void propagateEvent(Event event) {

    getEventManager().propagateEvent(event);
    if (MsgService.INSTANCE.isTemplateNode(model))
      event.consume();
  }

  private void initStyle() {

    textField.setTranslateX(viewStyle.paddingLeft);
    textField.setTranslateY(viewStyle.paddingTop);
    textField.getStyleClass().add(viewStyle.textField.cssClass);
    textField.heightProperty().addListener(observable -> notifySizeChange());
    textField.widthProperty().addListener(observable -> notifySizeChange());
    textField.setMaxWidth(USE_PREF_SIZE);
    textField.setMinWidth(USE_PREF_SIZE);
    getAppearanceManager().addCssClass(BhParams.CSS.CLASS_TEXT_FIELD_NODE);
  }

  @Override
  public TextNode getModel() {
    return model;
  }

  @Override
  public void setTextChangeListener(Function<String, Boolean> checkFormatFunc) {

    textField.boundsInLocalProperty().addListener(
      (observable, oldVal, newVal) -> updateTextFieldLook(checkFormatFunc));

    // テキストの長さに応じてTextField の長さが変わるように
    textField.textProperty().addListener(
      (observable, oldVal, newVal) ->  updateTextFieldLook(checkFormatFunc));
  }

  /**
   * テキストフィールドの見た目を変える
   * @param checkFormatFunc テキストのフォーマットをチェックする関数
   * */
  private void updateTextFieldLook(Function<String, Boolean> checkFormatFunc) {

    Text textPart = (Text)textField.lookup(".text");
    if (textPart != null) {

      // 正確な文字部分の境界を取得するため, GUI部品内部のTextの境界は使わない.
      double newWidth = ViewHelper.INSTANCE.calcStrWidth(textPart.getText(), textPart.getFont());
      newWidth = Math.max(newWidth, viewStyle.textField.minWidth);
      //幅を (文字幅 + パディング) にするとキャレットの移動時に文字が左右に移動するので定数 3 を足す.
      //この定数はフォントやパディングが違っても機能する.
      newWidth += textField.getPadding().getLeft() + textField.getPadding().getRight() + 3;
      textField.setPrefWidth(newWidth);
      boolean acceptable = checkFormatFunc.apply(textPart.getText());
      if (acceptable)
        textField.pseudoClassStateChanged(PseudoClass.getPseudoClass(BhParams.CSS.PSEUDO_BHNODE), false);
      else
        textField.pseudoClassStateChanged(PseudoClass.getPseudoClass(BhParams.CSS.PSEUDO_BHNODE), true);
    }
  }

  @Override
  protected void arrangeAndResize() {
    getAppearanceManager().updatePolygonShape();
  }

  @Override
  protected Vec2D getBodySize(boolean includeCnctr) {

    Vec2D cnctrSize = viewStyle.getConnectorSize();
    // textField.getWidth() だと設定した値以外が返る場合がある
    double bodyWidth = viewStyle.paddingLeft + textField.getPrefWidth() + viewStyle.paddingRight;
    if (includeCnctr && (viewStyle.connectorPos == CNCTR_POS.LEFT))
      bodyWidth += cnctrSize.x;

    double bodyHeight = viewStyle.paddingTop + textField.getHeight() + viewStyle.paddingBottom;
    if (includeCnctr && (viewStyle.connectorPos == CNCTR_POS.TOP))
      bodyHeight += cnctrSize.y;

    return new Vec2D(bodyWidth, bodyHeight);
  }

  @Override
  protected Vec2D getNodeSizeIncludingOuter(boolean includeCnctr) {
    return getBodySize(includeCnctr);
  }

  @Override
  protected TextInputControl getTextInputControl() {
    return textField;
  }

  @Override
  public void accept(NodeViewProcessor visitor) {
    visitor.visit(this);
  }

  @Override
  public void show(int depth) {
    MsgPrinter.INSTANCE.msgForDebug(
      indent(depth) + "<" + this.getClass().getSimpleName() + ">   " + this.hashCode());
    MsgPrinter.INSTANCE.msgForDebug(
      indent(depth + 1) + "<content" + ">   " + textField.getText());
  }
}












