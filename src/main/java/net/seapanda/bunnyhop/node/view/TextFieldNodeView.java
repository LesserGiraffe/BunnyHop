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
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.node.view.traverse.NodeViewWalker;
import net.seapanda.bunnyhop.search.Substring;
import net.seapanda.bunnyhop.ui.skin.HighlightableTextFieldSkin;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import org.apache.commons.lang3.StringUtils;

/**
 * テキストフィールドを入力フォームに持つビュー.
 *
 * @author K.Koike
 */
public final class TextFieldNodeView extends TextInputNodeView {

  private final TextNode model;
  private final TextField textField = new TextField();
  private final Visual visual = new Visual(this);
  private final Geometry geometry;

  /** クリック時にテキストを選択するかどうかのフラグ. */
  private boolean shouldSelectText = true;
  /** {@link #textField} がフォーカスを得る前に保持していたテキスト. */
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
  public TextFieldNodeView(
      TextNode model, BhNodeViewStyle style, SequencedSet<Node> components, boolean isTemplate)
      throws ViewConstructionException {
    super(model, style, components, isTemplate);
    this.model = model;
    setComponent(textField);
    textField.addEventFilter(MouseEvent.ANY, this::forwardEvent);
    textField.setOnMouseClicked(event -> Platform.runLater(() -> onTextFieldClicked(event)));
    textField.focusedProperty().addListener((obs, oldVal, newVal) -> onFocusChanged(newVal));
    geometry = new Geometry(this, new NodeSizeCalculator(this, this::getContentRegionSize)) {};
    setEditable(getStyle().textField.editable);
    initializeStyle();
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
    textField.getStyleClass().add(getStyle().textField.cssClass);
    textField.setMaxWidth(Region.USE_PREF_SIZE);
    textField.setMinWidth(Region.USE_PREF_SIZE);
    visual.addCssClass(getStyle().cssClasses);
    visual.addCssClass(BhConstants.Css.Class.BH_NODE);
    visual.addCssClass(BhConstants.Css.Class.TEXT_FIELD_NODE);
  }

  private void onTextFieldClicked(MouseEvent event) {
    if (textField.isFocused() && textField.isEditable()) {
      if (shouldSelectText) {
        textField.selectAll();
        shouldSelectText = false;
      } else if (event.getClickCount() == 2) {
        textField.positionCaret(textField.getLength());
      }
    }
  }

  private void onFocusChanged(boolean focused) {
    if (!focused) {
      textField.deselect();
      shouldSelectText = true;
    }

    if (focused) {
      textBeforeFocused = getText();
      return;
    }
    textField.deselect();
    shouldSelectText = true;
    if (!StringUtils.equals(textBeforeFocused, getText())) {
      var event = new TextChangeEvent(this, textBeforeFocused, getText());
      getCallbackRegistry().onTextChangedInvoker.invoke(event);
    }
  }

  /** コンテンツを表示する領域の大きさを取得する. */
  private Vec2D getContentRegionSize() {
    // textField.getWidth() だと設定した値以外が返る場合がある
    return new Vec2D(textField.getPrefWidth(), textField.getHeight());
  }

  @Override
  public void setTextChangeListener(Function<String, Boolean> fnCheckFormat) {
    textField.boundsInLocalProperty().addListener(
        (observable, oldVal, newVal) -> updateTextFieldLooks(fnCheckFormat));

    // テキストの長さに応じてTextField の長さが変わるように
    textField.textProperty().addListener(
        (observable, oldVal, newVal) ->  updateTextFieldLooks(fnCheckFormat));
  }

  /**
   * テキストフィールドの見た目を変える.
   *
   * @param fnCheckFormat テキストのフォーマットをチェックする関数
   */
  private void updateTextFieldLooks(Function<String, Boolean> fnCheckFormat) {
    Text text = (Text) textField.lookup(".text");
    if (text == null) {
      return;
    }
    // 正確な文字部分の境界を取得するため, GUI 部品内部の Text の境界は使わない.
    double newWidth = ViewUtil.calcStrWidth(text.getText(), text.getFont());
    newWidth = Math.max(newWidth, getStyle().textField.minWidth);
    // 幅を (文字幅 + パディング) にするとキャレットの移動時に文字が左右に移動するので定数 3 を足す.
    // この定数はフォントやパディングが違っても機能する.
    newWidth += textField.getPadding().getLeft() + textField.getPadding().getRight() + 3;
    textField.setPrefWidth(newWidth);
    boolean acceptable = fnCheckFormat.apply(text.getText());
    textField.pseudoClassStateChanged(
        PseudoClass.getPseudoClass(BhConstants.Css.Pseudo.ERROR), !acceptable);
  }

  @Override
  protected TextInputControl getTextInputControl() {
    return textField;
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

    /** テキストフィールドに適用するスキン. */
    private final HighlightableTextFieldSkin skin;

    Visual(TextFieldNodeView view) {
      super(view);
      String styleClass = view.getStyle().textField.textHighlight.cssClass;
      skin = new HighlightableTextFieldSkin(view.textField, styleClass, REFRESH);
      view.textField.setSkin(skin);
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
