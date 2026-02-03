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
import javafx.scene.control.TextField;
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
 * テキストフィールドを入力フォームに持つビュー.
 *
 * @author K.Koike
 */
public final class TextFieldNodeView extends TextInputNodeView {

  private final TextField textField = new TextField();
  private final TextNode model;
  /** クリック時にテキストを選択するかどうかのフラグ. */
  private boolean shouldSelectText = true;
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
  public TextFieldNodeView(
      TextNode model, BhNodeViewStyle style, SequencedSet<Node> components, boolean isTemplate)
      throws ViewConstructionException {
    super(model, style, components, isTemplate);
    this.model = model;
    sizeCalculator = new NodeSizeCalculator(this, this::getTextFieldSize);
    setComponent(textField);
    textField.addEventFilter(MouseEvent.ANY, this::forwardEvent);
    textField.setOnMouseClicked(event -> Platform.runLater(this::selectText));
    textField.focusedProperty().addListener((obs, oldVal, newVal) -> onFocusChanged(newVal));
    initStyle();
  }

  /**
   * コンストラクタ.
   *
   * @param style このノードビューのスタイル
   * @throws ViewConstructionException ノードビューの初期化に失敗
   */
  public TextFieldNodeView(BhNodeViewStyle style, boolean isTemplate)
      throws ViewConstructionException {
    this(null, style, new LinkedHashSet<>(), isTemplate);
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
    textField.getStyleClass().add(style.textField.cssClass);
    textField.setMaxWidth(Region.USE_PREF_SIZE);
    textField.setMinWidth(Region.USE_PREF_SIZE);
    setEditable(style.textField.editable);
    getLookManager().addCssClass(BhConstants.Css.CLASS_TEXT_FIELD_NODE);
  }

  private Vec2D getTextFieldSize() {
    // textField.getWidth() だと設定した値以外が返る場合がある
    return new Vec2D(textField.getPrefWidth(), textField.getHeight());
  }

  private void selectText() {
    if (textField.isFocused() && textField.isEditable() && shouldSelectText) {
      textField.selectAll();
      shouldSelectText = false;
    }
  }

  private void onFocusChanged(boolean focused) {
    if (!focused) {
      textField.deselect();
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
    newWidth = Math.max(newWidth, style.textField.minWidth);
    // 幅を (文字幅 + パディング) にするとキャレットの移動時に文字が左右に移動するので定数 3 を足す.
    // この定数はフォントやパディングが違っても機能する.
    newWidth += textField.getPadding().getLeft() + textField.getPadding().getRight() + 3;
    textField.setPrefWidth(newWidth);
    boolean acceptable = fnCheckFormat.apply(textPart.getText());
    textField.pseudoClassStateChanged(
        PseudoClass.getPseudoClass(BhConstants.Css.PSEUDO_ERROR), !acceptable);
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
    return textField;
  }

  @Override
  public void accept(NodeViewWalker visitor) {
    visitor.visit(this);
  }
}
