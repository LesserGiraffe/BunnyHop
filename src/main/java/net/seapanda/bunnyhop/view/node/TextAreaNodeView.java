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

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.utility.SimpleCache;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.view.ViewConstructionException;
import net.seapanda.bunnyhop.view.ViewUtil;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle.ConnectorAlignment;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle.ConnectorPos;
import net.seapanda.bunnyhop.view.traverse.NodeViewWalker;

/**
 * テキストエリアを入力フォームに持つビュー.
 *
 * @author K.Koike
 */
public final class TextAreaNodeView  extends TextInputNodeView {

  private TextArea textArea = new TextArea();
  private final TextNode model;
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
  public TextAreaNodeView(
      TextNode model, BhNodeViewStyle viewStyle, SequencedSet<Node> components)
      throws ViewConstructionException {
    super(model, viewStyle, components);
    this.model = model;
    setComponent(textArea);
    textArea.addEventFilter(MouseEvent.ANY, this::forwardEvent);
    initStyle();
  }

  /**
   * コンストラクタ.
   *
   * @param viewStyle このノードビューのスタイル
   * @throws ViewConstructionException ノードビューの初期化に失敗
   */
  public TextAreaNodeView(BhNodeViewStyle viewStyle)
      throws ViewConstructionException {
    this(null, viewStyle, new LinkedHashSet<>());
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
    textArea.getStyleClass().add(viewStyle.textArea.cssClass);
    textArea.setWrapText(false);
    textArea.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    textArea.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    setEditable(viewStyle.textArea.editable);
    getLookManager().addCssClass(BhConstants.Css.CLASS_TEXT_AREA_NODE);
  }

  @Override
  protected void onNodeSizeChanged() {
    nodeSizeCache.setDirty(true);
    nodeWithCnctrSizeCache.setDirty(true);
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
   * @param text このテキストに基づいてテキストエリアの見た目を変える
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
    double newWidth = Math.max(textBounds.x, viewStyle.textArea.minWidth);
    // 幅を (文字幅 + パディング) にするとwrapの設定によらず文字列が折り返してしまういことがあるので定数 6 を足す
    // この定数はフォントやパディングが違っても機能する.
    newWidth += content.getPadding().getLeft() + content.getPadding().getRight() + 6;
    double newHeight = Math.max(textBounds.y, viewStyle.textArea.minHeight);
    newHeight += content.getPadding().getTop() + content.getPadding().getBottom() + 2;
    textArea.setPrefSize(newWidth, newHeight);
    boolean acceptable = fnCheckFormat.apply(textPart.getText());
    textArea.pseudoClassStateChanged(
        PseudoClass.getPseudoClass(BhConstants.Css.PSEUDO_ERROR), !acceptable);
    // textArea.requestLayout() を呼ばないと, newWidth の値によってはノード選択ビューでサイズが更新されない.
    Platform.runLater(() -> textArea.requestLayout());
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
  public void show(int depth) {
    System.out.println(
        "%s<TextAreaNodeView>  %s".formatted(indent(depth), hashCode()));
    System.out.println(
        "%s<content>  %s".formatted(indent(depth + 1), textArea.getText()));
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
        // textField.getWidth() だと設定した値以外が返る場合がある
        new Vec2D(
            commonPartSize.x + textArea.getPrefWidth(),
            Math.max(commonPartSize.y, textArea.getPrefHeight()));
      case COLUMN ->
        new Vec2D(
            Math.max(commonPartSize.x, textArea.getPrefWidth()),
            commonPartSize.y + textArea.getPrefHeight());
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

  @Override
  protected TextInputControl getTextInputControl() {
    return textArea;
  }

  @Override
  public void accept(NodeViewWalker visitor) {
    visitor.visit(this);
  }
}
