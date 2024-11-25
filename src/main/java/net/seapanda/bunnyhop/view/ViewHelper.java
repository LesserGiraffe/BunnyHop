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

package net.seapanda.bunnyhop.view;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.ComboBoxNodeView;
import net.seapanda.bunnyhop.view.node.LabelNodeView;
import net.seapanda.bunnyhop.view.node.TextAreaNodeView;
import net.seapanda.bunnyhop.view.node.TextFieldNodeView;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.part.SelectableItem;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;
import net.seapanda.bunnyhop.view.workspace.WorkspaceViewPane;

/**
 * View に対する共通の処理を定義したクラス.
 *
 * @author K.Koike
 */
public class ViewHelper {

  public static final ViewHelper INSTANCE = new ViewHelper();
  /** 影付きノードリスト. */
  public final Set<BhNodeView> shadowNodes = 
      Collections.newSetFromMap(new WeakHashMap<BhNodeView, Boolean>());

  /**
   * 移動後のフィールド上の位置を算出する.
   *
   * @param diff 移動量
   * @param fieldSize フィールドサイズ
   * @param curPos 現在のフィールド上の位置
   * @return 移動後の新しい位置
   */
  public Vec2D newPosition(Vec2D diff, Vec2D fieldSize, Vec2D curPos) {
    double newDiffX = calcNewDiff(fieldSize.x, curPos.x, diff.x);
    double newDiffY = calcNewDiff(fieldSize.y, curPos.y, diff.y);
    return new Vec2D(curPos.x + newDiffX, curPos.y + newDiffY);
  }

  /**
   * 移動時のフィールド上の移動距離を算出する.
   *
   * @param diff 移動量
   * @param fieldSize フィールドサイズ
   * @param curPos 現在のフィールド上の位置
   * @return 移動距離
   */
  public Vec2D distance(Vec2D diff, Vec2D fieldSize, Vec2D curPos) {
    double newDiffX = calcNewDiff(fieldSize.x, curPos.x, diff.x);
    double newDiffY = calcNewDiff(fieldSize.y, curPos.y, diff.y);
    return new Vec2D(newDiffX, newDiffY);
  }

  /**
   * ワークスペースから出ない範囲で新しい移動量を算出する.
   *
   * @param targetRange 新しい位置を計算する際に使う範囲
   * @param curPos 現在のWS上での位置
   * @param diff 移動量
   * @return 新しい移動量
   */
  private double calcNewDiff(double targetRange, double curPos, double diff) {
    boolean curPosIsInTargetRange = (0 < curPos) && (curPos < targetRange);
    if (curPosIsInTargetRange) {
      double newPos = curPos + diff;
      boolean newPosIsInTargetRange = (0 < newPos) && (newPos < targetRange);
      // 現在範囲内に居て移動後に範囲外に居る場合, 移動させない
      if (!newPosIsInTargetRange) {
        if (newPos < 0) {
          return -curPos + 1.0;
        } else {
          return targetRange - curPos - 1.0;
        }
      }
    }
    return diff;
  }

  /**
   * {@code node} のワークスペース上での位置を取得する.
   *
   * @param node ワークペース上での位置を計算するノード
   * @return {@code node} のワークスペース上での位置.
   */
  public Vec2D getPosOnWorkspace(Node node) {
    Parent parent = node.getParent();
    while (parent != null && !BhConstants.Fxml.ID_WS_PANE.equals(parent.getId())) {
      parent = parent.getParent();
    }
    if (parent != null) {
      Point2D posOnScene = node.localToScene(0.0, 0.0);
      Point2D posOnWorkspace = parent.sceneToLocal(posOnScene);
      return new Vec2D(posOnWorkspace.getX(), posOnWorkspace.getY());
    }
    return new Vec2D(0.0, 0.0);
  }

  /**
   * {@code node} が属するワークスペースビューを取得する.
   *
   * @return {@code node} が属するワークスペースビュー.
   *         どのワークスペースビューにも属していない場合は null を返す.
   */
  public WorkspaceView getWorkspaceView(Node node) {
    Parent parent = node.getParent();
    while (parent != null && !BhConstants.Fxml.ID_WS_PANE.equals(parent.getId())) {
      parent = parent.getParent();
    }
    if (parent != null) {
      return ((WorkspaceViewPane) parent).getContainer();
    }
    return null;
  }

  /**
   * 文字列を表示したときのサイズを計算する.
   *
   * @param str サイズを計算する文字列
   * @param font フォント
   * @param boundType 境界算出方法
   * @param lineSpacing 行間
   */
  public Vec2D calcStrBounds(String str, Font font, TextBoundsType boundType, double lineSpacing) {
    Text text = new Text(str);
    text.setFont(font);
    text.setBoundsType(boundType);
    text.setLineSpacing(lineSpacing);
    return new Vec2D(text.getBoundsInLocal().getWidth(), text.getBoundsInLocal().getHeight());
  }

  /**
   * 引数で指定した文字列の表示幅を計算する.
   *
   * @param str 表示幅を計算する文字列
   * @param font 表示時のフォント
   * @return 文字列を表示したときの幅
   */
  public double calcStrWidth(String str, Font font) {
    Text text = new Text(str);
    text.setFont(font);
    return text.getBoundsInLocal().getWidth();
  }

  /**
   * 引数で指定したノードビューに影を付ける.
   * ただし, 影を付けるノードと同じワークスペースに既に影の付いたノードがあった場合, そのノードの影は消える.
   *
   * @param nodeToPutShadowOn 新たに影を付けるノード.
   */
  public void drawShadow(BhNodeView nodeToPutShadowOn) {
    if (nodeToPutShadowOn.getModel().isEmpty()) {
      return;
    }
    var nodesToRemoveShadow = shadowNodes.stream()
        .filter(view ->
            areInSameWorkspace(view, nodeToPutShadowOn) || view.getModel().get().isDeleted())
        .toList();
    nodesToRemoveShadow.forEach(view -> view.getLookManager().showShadow(false));
    shadowNodes.removeAll(nodesToRemoveShadow);
    nodeToPutShadowOn.getLookManager().showShadow(true);
    shadowNodes.add(nodeToPutShadowOn);
  }

  /**
   * ワークスペースに描画されているノードビューの影を消す.
   *
   * @param ws このワークスペースに描画されている影を消す
   */
  public void removeShadow(Workspace ws) {
    var nodesToRemoveShadow = shadowNodes.stream()
        .filter(view ->
            view.getModel().get().getWorkspace() == ws || view.getModel().get().isDeleted())
        .toList();
    nodesToRemoveShadow.forEach(view -> view.getLookManager().showShadow(false));
    shadowNodes.removeAll(nodesToRemoveShadow);
  }

  /** {@code viewA} と {@code viewB} が同じワークスペース上にあるか調べる. */
  public boolean areInSameWorkspace(BhNodeView viewA, BhNodeView viewB) {
    if (viewA.getModel().isEmpty() || viewB.getModel().isEmpty()) {
      return false;
    }
    return viewA.getModel().get().getWorkspace() == viewB.getModel().get().getWorkspace();
  }

  /**
   * モデルを持たない {@link BhNodeView} を作成する.
   *
   * @param styleId 作成するビューのスタイル ID
   * @param text 作成するビューに設定する文字列
   * @return 作成した {@link BhNodeView}
   */
  public BhNodeView createModellessNodeView(String styleId, String text)
      throws ViewInitializationException {
    Optional<BhNodeViewStyle> style = BhNodeViewStyle.getStyleFromStyleId(styleId);
    if (style.isEmpty()) {
      throw new ViewInitializationException(
        "BhNode View Style '%s' was not found.".formatted(styleId));
    }
    return switch (style.get().component) {
      case TEXT_FIELD -> {
        var view = new TextFieldNodeView(style.get());
        view.setTextChangeListener(str -> true);
        view.setText(text);
        yield view;
      }
      case COMBO_BOX -> {
        var view = new ComboBoxNodeView(style.get());
        view.setValue(new SelectableItem(text, text));
        yield view;
      }
      case LABEL -> {
        var view = new LabelNodeView(style.get());
        view.setText(text);
        yield view;
      }
      case TEXT_AREA -> {
        var view = new TextAreaNodeView(style.get());
        view.setTextChangeListener(str -> true);
        view.setText(text);
        yield view;
      }
      default -> {
        throw new ViewInitializationException(
            "Cannot create a modelless node view whose component is '%s'.  (%s)"
                .formatted(style.get().component, styleId));
      }
    };
  }
}
