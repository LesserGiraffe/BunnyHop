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

import static net.seapanda.bunnyhop.common.configuration.BhConstants.Css.Pseudo.IS_EVEN;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Css.Pseudo.UNFIXED_DEFAULT;

import java.util.HashSet;
import java.util.Set;
import javafx.application.Platform;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.view.bodyshape.BodyShape;
import net.seapanda.bunnyhop.node.view.bodyshape.BodyShapeType;
import net.seapanda.bunnyhop.node.view.connectorshape.ConnectorShape;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.utility.math.Vec2D;

/**
 * ノードビューを整列する機能を提供するクラス.
 *
 * @author K.Koike
 */
class ArrangementImpl implements BhNodeView.Arrange {

  private final BhNodeViewBase view;
  /** 現在描画されているノードのポリゴンの大きさ. */
  private final Vec2D currentPolygonSize = new Vec2D(Double.MIN_VALUE, Double.MIN_VALUE);
  /** ルートを 0 として, ノードビューの階層が偶数であった場合 true. ただし, 外部ノードは親と同階層とする. */
  private boolean isEven = false;
  private final SizeChangeNotifier notif;
  /** ノードの整列を待っている {@link BhNodeView} のセット. */
  private static final Set<BhNodeView> viewsAwaitingArrangement = new HashSet<>();

  /** コンストラクタ. */
  ArrangementImpl(BhNodeViewBase view, SizeChangeNotifier notif) {
    this.view = view;
    this.notif = notif;
  }

  @Override
  public void arrange() {
    view.getGeometry().updateDescendantRelativePositions();
    Vec2D pos = view.getGeometry().getPosition();
    view.getGeometry().setTreePosition(pos.x, pos.y);
    NvbCallbackInvoker.invoke(view -> view.getArrangement().updateViewLayout(), view);
    NvbCallbackInvoker.invoke(view -> view.getArrangement().updateEvenFlag(), view);
    notif.markSubtreeSizeUpToDate();
  }

  private void updateViewLayout() {
    updatePseudoClassStates();
    boolean isSizeChanged = updatePolygonShape();
    if (isSizeChanged) {
      view.getCallbackRegistry().onSizeChanged();
    }
  }

  /** 現在のノードの状態に応じて疑似クラスの状態を変更する. */
  private void updatePseudoClassStates() {
    BhNode model = view.getModel().orElse(null);
    boolean isUnfixedDefault = model != null
        && model.isDefault()
        && model.getParentConnector() != null
        && !model.getParentConnector().isFixed();
    view.getVisual().setPseudoClassState(isUnfixedDefault, UNFIXED_DEFAULT);
  }

  @Override
  public void requestArrangement() {
    if (viewsAwaitingArrangement.isEmpty()) {
      Platform.runLater(() -> {
        viewsAwaitingArrangement.forEach(view -> view.getArrangement().arrange());
        viewsAwaitingArrangement.clear();
      });
    }
    viewsAwaitingArrangement.add(view);
  }

  /**
   * ノードを形作るポリゴンを更新する.
   *
   * @return ポリゴンの大きさが変化した場合 true.
   */
  private boolean updatePolygonShape() {
    Vec2D bodySize = view.getGeometry().getNodeSize(false);
    if (currentPolygonSize.equals(bodySize)) {
      return false;
    }
    boolean isFixed = view.isFixed();
    BhNodeViewStyle style = view.getStyle();
    ConnectorShape cnctrShape =
        isFixed ? style.connectorShapeFixed.shape : style.connectorShape.shape;
    ConnectorShape notchShape =
        isFixed ? style.notchShapeFixed.shape : style.notchShape.shape;

    BodyShape bodyShape = view.getGeometry().getBodyShape().shape;
    view.getShapes().nodeShape().getPoints().setAll(
        bodyShape.createVertices(style, bodySize.x, bodySize.y, cnctrShape, notchShape));
    view.getShapes().compileError().setSize(bodySize.x, bodySize.y);
    currentPolygonSize.set(bodySize);
    return true;
  }

  /** {@link #view} の奇遇フラグを更新する. */
  private void updateEvenFlag() {
    BhNodeViewBase parentView = view.getTreeControl().getParentView();
    if (parentView != null) {
      BodyShapeType bodyShape = parentView.getGeometry().getBodyShape();
      if (view.getTreeControl().getParentGroup().inner && bodyShape != BodyShapeType.NONE) {
        isEven = !parentView.getArrangement().isEven;
      } else {
        isEven = parentView.getArrangement().isEven;
      }
    } else {
      isEven = true;  //ルートは even
    }
    view.getVisual().setPseudoClassState(isEven, IS_EVEN);
  }
}
