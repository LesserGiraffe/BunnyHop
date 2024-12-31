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

import java.util.Optional;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.ViewUtil;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle.ConnectorPos;
import net.seapanda.bunnyhop.view.traverse.NodeViewProcessor;

/**
 * テキストフィールドを入力フォームに持つビュー.
 *
 * @author K.Koike
 */
public final class TextFieldNodeView extends TextInputNodeView {

  private TextField textField = new TextField();
  private final TextNode model;

  /**
   * コンストラクタ.
   *
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
    textField.focusedProperty().addListener(
        (ov, oldVal, newVal) -> Platform.runLater(() -> selectText()));
    initStyle();
  }

  /**
   * コンストラクタ.
   *
   * @param viewStyle このノードビューのスタイル
   * @throws ViewInitializationException ノードビューの初期化に失敗
   */
  public TextFieldNodeView(BhNodeViewStyle viewStyle)
      throws ViewInitializationException {
    this(null, viewStyle);
  }

  private void propagateEvent(Event event) {
    BhNodeView view = (model == null) ? getTreeManager().getParentView() : this;
    if (view == null) {
      event.consume();
      return;
    }
    view.getEventManager().propagateEvent(event);    
    view.getModel().ifPresent(node -> {
      if (node.getViewProxy().isTemplateNode()) {
        event.consume();
      }
    });
  }

  private void initStyle() {
    textField.setTranslateX(viewStyle.paddingLeft);
    textField.setTranslateY(viewStyle.paddingTop);
    textField.getStyleClass().add(viewStyle.textField.cssClass);
    textField.heightProperty().addListener(observable -> notifySizeChange());
    textField.widthProperty().addListener(observable -> notifySizeChange());
    textField.setMaxWidth(USE_PREF_SIZE);
    textField.setMinWidth(USE_PREF_SIZE);
    setEditable(viewStyle.textField.editable);
    getLookManager().addCssClass(BhConstants.Css.CLASS_TEXT_FIELD_NODE);
  }

  private void selectText() {
    if (textField.isFocused() && !textField.getText().isEmpty()) {
      textField.selectAll();
    }
  }

  @Override
  public Optional<TextNode> getModel() {
    return Optional.ofNullable(model);
  }

  @Override
  public void setTextChangeListener(Function<String, Boolean> fnCheckFormat) {
    textField.boundsInLocalProperty().addListener(
        (observable, oldVal, newVal) -> updateTextFieldLook(fnCheckFormat));

    // テキストの長さに応じてTextField の長さが変わるように
    textField.textProperty().addListener(
        (observable, oldVal, newVal) ->  updateTextFieldLook(fnCheckFormat));
  }

  /**
   * テキストフィールドの見た目を変える.
   *
   * @param fnCheckFormat テキストのフォーマットをチェックする関数
   */
  private void updateTextFieldLook(Function<String, Boolean> fnCheckFormat) {
    Text textPart = (Text) textField.lookup(".text");
    if (textPart == null) {
      return;
    }
    // 正確な文字部分の境界を取得するため, GUI 部品内部の Text の境界は使わない.
    double newWidth = ViewUtil.calcStrWidth(textPart.getText(), textPart.getFont());
    newWidth = Math.max(newWidth, viewStyle.textField.minWidth);
    // 幅を (文字幅 + パディング) にするとキャレットの移動時に文字が左右に移動するので定数 3 を足す.
    // この定数はフォントやパディングが違っても機能する.
    newWidth += textField.getPadding().getLeft() + textField.getPadding().getRight() + 3;
    textField.setPrefWidth(newWidth);
    boolean acceptable = fnCheckFormat.apply(textPart.getText());
    textField.pseudoClassStateChanged(
        PseudoClass.getPseudoClass(BhConstants.Css.PSEUDO_ERROR), !acceptable);
  }

  @Override
  protected void arrangeAndResize() {
    getLookManager().updatePolygonShape();
  }

  @Override
  protected Vec2D getBodySize(boolean includeCnctr) {
    Vec2D cnctrSize = viewStyle.getConnectorSize(isFixed());
    // textField.getWidth() だと設定した値以外が返る場合がある
    double bodyWidth = viewStyle.paddingLeft + textField.getPrefWidth() + viewStyle.paddingRight;
    if (includeCnctr && (viewStyle.connectorPos == ConnectorPos.LEFT)) {
      bodyWidth += cnctrSize.x;
    }
    double bodyHeight = viewStyle.paddingTop + textField.getHeight() + viewStyle.paddingBottom;
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
    return textField;
  }

  @Override
  public void accept(NodeViewProcessor visitor) {
    visitor.visit(this);
  }

  @Override
  public void show(int depth) {
    BhService.msgPrinter().println(
        "%s<TextFieldNodeView>  %s".formatted(indent(depth), hashCode()));
    BhService.msgPrinter().println(
        "%s<content>  %s".formatted(indent(depth + 1), textField.getText()));
  }
}
