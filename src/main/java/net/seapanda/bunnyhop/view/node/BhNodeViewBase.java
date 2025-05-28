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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.control.node.BhNodeController;
import net.seapanda.bunnyhop.control.node.TemplateNodeController;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.derivative.DerivativeBase;
import net.seapanda.bunnyhop.quadtree.QuadTreeManager;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle.OverlapOption;
import net.seapanda.bunnyhop.utility.Showable;
import net.seapanda.bunnyhop.utility.Utility;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.ViewConstructionException;
import net.seapanda.bunnyhop.view.ViewUtil;
import net.seapanda.bunnyhop.view.bodyshape.BodyShapeBase.BodyShape;
import net.seapanda.bunnyhop.view.connectorshape.ConnectorShape;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle.ConnectorAlignment;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle.ConnectorPos;
import net.seapanda.bunnyhop.view.traverse.NvbCallbackInvoker;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;
import org.apache.commons.lang3.mutable.MutableDouble;

/**
 * {@link BhNode} に対応するビュークラスの抽象基底クラス.
 *
 * @author K.Koike
 */
public abstract class BhNodeViewBase implements BhNodeView, Showable {

  // 描画順序. 小さい値ほど手前に描画される.
  protected static final double COMPILE_ERR_MARK_VIEW_ORDER_OFFSET = -4000;
  protected static final double NODE_BASE_VIEW_ORDER_OFFSET = -2000;
  protected static final double CHILD_VIEW_ORDER_OFFSET_FROM_PARENT = -1.0;
  /** ノード描画用ポリゴン, ボタン, コンパイルエラーマークを乗せるペイン. */
  protected final Pane nodeBase = new Pane();
  /** ノード描画用ポリゴン. */
  protected final Polygon nodeShape = new Polygon();
  /** コンパイルエラーノードであることを示す印. */
  protected final CompileErrorMark compileErrorMark = new CompileErrorMark(0.0, 0.0, 0.0, 0.0);
  /** ノードの見た目のパラメータオブジェクト. */
  protected final BhNodeViewStyle viewStyle;
  private final BhNode model;
  private BhNodeController controller;
  /** このノードビューを保持する親グループ.  このノードビューがルートノードビューの場合は null. */
  protected BhNodeViewGroup parent;

  private final RegionManagerBase regionManager = this.new RegionManagerBase();
  private final TreeManagerBase treeManager = this.new TreeManagerBase();
  private final PositionManagerBase positionManager = this.new PositionManagerBase();
  private final EventManagerBase eventManager = this.new EventManagerBase();
  private final LookManagerBase lookManager;

  /**
   * このノードのボディ部分に末尾までの全外部ノードを加えた大きさを返す.
   *
   * @param includeCnctr コネクタ部分を含む大きさを返す場合 true.
   * @return 描画ノードの大きさ
   */
  protected abstract Vec2D getNodeTreeSize(boolean includeCnctr);

  /**
   * 外部ノードを除くボディ部分の大きさを返す.
   *
   * @param includeCnctr コネクタ部分を含む大きさを返す場合true
   * @return 描画ノードの大きさ
   */
  protected abstract Vec2D getNodeSize(boolean includeCnctr);

  /** このノードビューより下のノードビューの親 {@link BhNodeViewGroup} からの相対位置を更新する. */
  protected abstract void updateChildRelativePos();

  /**
   * このノードビュー以下のノードビューのワークスペース上の位置を更新する.
   *
   * @param posX このノードのボディ部分の左上の X 位置
   * @param posY このノードのボディ部分の左上の Y 位置
   */
  protected abstract void updatePosOnWorkspace(double posX, double posY);

  /**
   * コンストラクタ.
   *
   * @param viewStyle ノードの見た目を決めるパラメータオブジェクト
   * @param model ビューが表すモデル
   * @param components このノードビューの子要素に追加するコンポーネントのリスト
   */
  protected BhNodeViewBase(
      BhNodeViewStyle viewStyle, DerivativeBase<?> model, SequencedSet<Node> components)
      throws ViewConstructionException {
    this.viewStyle = viewStyle;
    this.model = model;
    nodeBase.setPickOnBounds(false);
    lookManager = this.new LookManagerBase(viewStyle.bodyShape);
    lookManager.addCssClass(viewStyle.cssClasses);
    lookManager.addCssClass(BhConstants.Css.CLASS_BH_NODE);
    compileErrorMark.getStyleClass().add(BhConstants.Css.CLASS_BH_NODE_COMPILE_ERROR);
    compileErrorMark.setMouseTransparent(true);
    addComponent(nodeShape);
    nodeShape.addEventFilter(Event.ANY, this::forwardEventIfNotHaveController);
    components.forEach(this::addComponent);
  }

  @Override
  public Optional<BhNodeController> getController() {
    return Optional.ofNullable(controller);
  }

  @Override
  public void setController(BhNodeController controller) {
    this.controller = controller;
  }

  @Override
  public RegionManagerBase getRegionManager() {
    return regionManager;
  }

  @Override
  public TreeManagerBase getTreeManager() {
    return treeManager;
  }

  @Override
  public PositionManagerBase getPositionManager() {
    return positionManager;
  }

  @Override
  public  EventManagerBase getEventManager() {
    return eventManager;
  }

  @Override
  public LookManagerBase getLookManager() {
    return lookManager;
  }

  @Override
  public void setMouseTransparent(boolean value) {
    nodeBase.setMouseTransparent(value);
  }

  @Override
  public boolean isFixed() {
    if (model == null) {
      return false;
    }
    if (model.getParentConnector() == null) {
      return false;
    }
    return model.getParentConnector().isFixed();
  }

  @Override
  public boolean isTemplate() {
    BhNodeView view = this;
    while (view != null) {
      if (view.getController().isPresent()) {
        return view.getController().get() instanceof TemplateNodeController;
      }
      view = view.getTreeManager().getParentView();
    }
    return false;
  }

  @Override
  public WorkspaceView getWorkspaceView() {
    return ViewUtil.getWorkspaceView(nodeBase);
  }

  /** このノードビューがコントローラを持たない場合, 親ノードビューにイベントを渡す. */
  private void forwardEventIfNotHaveController(Event event) {
    if (controller != null) {
      return;
    }
    BhNodeView view = getTreeManager().getParentView();
    if (view == null) {
      event.consume();
      return;
    }
    view.getEventManager().dispatch(event);
    event.consume();
  }

  /**
   * このビューに GUI コンポーネントを追加する.
   *
   * @param node 追加するコンポーネント.
   */
  protected void addComponent(Node node) {
    nodeBase.getChildren().add(node);
  }

  /** ノードビューの外観を変更する機能を提供するクラス. */
  public class LookManagerBase implements LookManager {

    /** 影付きノードリスト. */
    public static final Set<BhNodeView> shadowNodes = 
        Collections.newSetFromMap(new WeakHashMap<BhNodeView, Boolean>());

    /** ボディの形を取得する関数オブジェクト. */
    private Supplier<BodyShape> fnGetBodyShape;
    /** 現在描画されているノードのポリゴンの形状を決めているノードの大きさ. */
    private final Vec2D currentPolygonSize = new Vec2D(Double.MIN_VALUE, Double.MIN_VALUE);
    /** 現在描画されているノードのポリゴンの形状を決めているノードの大きさ. */
    private boolean isArrangementRequested = false;

    /**
     * コンストラクタ.
     *
     * @param bodyShape 本体部分の形
     */
    public LookManagerBase(BodyShape bodyShape) {
      this.fnGetBodyShape = () -> bodyShape;
    }

    @Override
    public void switchPseudoClassState(String className, boolean enable) {
      if (enable) {
        nodeShape.pseudoClassStateChanged(PseudoClass.getPseudoClass(className), true);
        nodeBase.pseudoClassStateChanged(PseudoClass.getPseudoClass(className), true);
      } else {
        nodeShape.pseudoClassStateChanged(PseudoClass.getPseudoClass(className), false);
        nodeBase.pseudoClassStateChanged(PseudoClass.getPseudoClass(className), false);
      }
    }

    /**
     * cssクラス名を追加する.
     *
     * @param cssClassNames css クラス名
     */
    void addCssClass(String... cssClassNames) {
      for (var cssClassName : cssClassNames) {
        nodeShape.getStyleClass().add(cssClassName);
      }
    }

    /** 現在のノードの状態に応じて疑似クラスの状態を変更する. */
    private void changePseudoClassState() {
      boolean isUnfixedDefault = model != null
          && model.isDefault()
          && model.getParentConnector() != null
          && !model.getParentConnector().isFixed();
      BhNodeViewBase.this.getLookManager().switchPseudoClassState(
          BhConstants.Css.PSEUDO_UNFIXED_DEFAULT, isUnfixedDefault);
    }

    /**
     * ノードを形作るポリゴンを更新する.
     *
     * @param drawBody ボディを描画する場合 true
     * @return ポリゴンの大きさが変化した場合 true.
     */
    private boolean updatePolygonShape() {
      Vec2D bodySize = getRegionManager().getNodeSize(false);
      if (currentPolygonSize.equals(bodySize)) {
        return false;
      }
      boolean isFixed = BhNodeViewBase.this.isFixed();
      ConnectorShape cnctrShape =
          isFixed ? viewStyle.connectorShapeFixed.shape : viewStyle.connectorShape.shape;
      ConnectorShape notchShape =
          isFixed ? viewStyle.notchShapeFixed.shape : viewStyle.notchShape.shape;
  
      nodeShape.getPoints().setAll(
          getBodyShape().shape.createVertices(
              viewStyle,
              bodySize.x,
              bodySize.y,
              cnctrShape,
              notchShape));
      compileErrorMark.setEndX(bodySize.x);
      compileErrorMark.setEndY(bodySize.y);
      currentPolygonSize.set(bodySize);
      return true;
    }

    @Override
    public void arrange() {
      updateChildRelativePos();
      Vec2D pos = getPositionManager().getPosOnWorkspace();
      getPositionManager().setTreePosOnWorkspace(pos.x, pos.y);
      NvbCallbackInvoker.invoke(
          nodeView -> {
            nodeView.getLookManager().changePseudoClassState();
            boolean isSizeChanged = nodeView.getLookManager().updatePolygonShape();
            if (isSizeChanged) {
              nodeView.getEventManager().invokeOnNodeSizeChanged();
            }
          },
          BhNodeViewBase.this);
      getTreeManager().updateEvenFlag();
    }

    @Override
    public void requestArrangement() {
      if (!isArrangementRequested) {
        Platform.runLater(() -> {
          arrange();
          isArrangementRequested = false;
        });
        isArrangementRequested = true;
      }
    }

    @Override
    public void setVisible(boolean visible) {
      NvbCallbackInvoker.invoke(
          view -> ((BhNodeViewBase) view).nodeShape.setVisible(visible),
          BhNodeViewBase.this);
    }

    /** ボディの形をセットする関数を設定する. */
    void setBodyShapeGetter(Supplier<BodyShape> fnGetBodyShape) {
      if (fnGetBodyShape != null) {
        this.fnGetBodyShape = fnGetBodyShape;
      }
    }

    @Override
    public void setCompileErrorVisibility(boolean visible) {
      BhNodeViewBase.this.compileErrorMark.setVisible(visible);
    }

    @Override
    public boolean isCompileErrorVisible() {
      return BhNodeViewBase.this.compileErrorMark.isVisible();
    }

    /**
     * このノードビューのボディの形状の種類を取得する.
     *
     * @return このノードビューのボディの形状の種類
     */
    BodyShape getBodyShape() {
      return fnGetBodyShape.get();
    }

    @Override
    public void showShadow(boolean onlyOuter) {
      Consumer<? super BhNodeViewBase> fnShowShadow = nodeView -> {
        if (nodeView.getLookManager().getBodyShape() != BodyShape.BODY_SHAPE_NONE) {
          nodeView.getLookManager().switchPseudoClassState(BhConstants.Css.PSEUDO_SHADOW, true);
        }
      };
      if (onlyOuter) {
        NvbCallbackInvoker.invokeForOuters(fnShowShadow, BhNodeViewBase.this);
      } else {
        NvbCallbackInvoker.invoke(fnShowShadow, BhNodeViewBase.this);
      }
    }

    @Override
    public void hideShadow(boolean onlyOuter) {
      Consumer<? super BhNodeViewBase> fnShowShadow = nodeView -> 
          nodeView.getLookManager().switchPseudoClassState(BhConstants.Css.PSEUDO_SHADOW, false);
      if (onlyOuter) {
        NvbCallbackInvoker.invokeForOuters(fnShowShadow, BhNodeViewBase.this);
      } else {
        NvbCallbackInvoker.invoke(fnShowShadow, BhNodeViewBase.this);
      }
    }

    @Override
    public ConnectorPos getConnectorPos() {
      return viewStyle.connectorPos;
    }
  }

  /** このノードの画面上での領域に関する処理を行うクラス. */
  public class RegionManagerBase implements RegionManager {
    /** ノード全体の範囲. */
    private final QuadTreeRectangle bodyRange =
        new QuadTreeRectangle(0.0, 0.0, 0.0, 0.0, BhNodeViewBase.this);
    /** コネクタ部分の範囲. */
    private final QuadTreeRectangle cnctrRange
        = new QuadTreeRectangle(0.0, 0.0, 0.0, 0.0, BhNodeViewBase.this);

    @Override
    public List<BhNodeView> searchForOverlapped() {
      List<QuadTreeRectangle> overlappedRectList =
          cnctrRange.searchOverlappedRects(OverlapOption.INTERSECT);
      return overlappedRectList.stream()
          .map(rectangle -> rectangle.<BhNodeView>getUserData())
          .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Rectangles getRegions() {
      return new Rectangles(bodyRange, cnctrRange);
    }

    /**
     * 4 分木空間上での位置を更新する.
     *
     * @param posX 本体部分左上のX位置
     * @param posY 本体部分左上のY位置
     */
    private void setPosOnQuadTreeSpace(double posX, double posY) {
      Vec2D bodySize = getNodeSize(false);
      double bodyUpperLeftX = posX;
      double bodyUpperLeftY = posY;
      double bodyLowerRightX = posX + bodySize.x;
      double bodyLowerRightY = posY + bodySize.y;
      double cnctrUpperLeftX = 0.0;
      double cnctrUpperLeftY = 0.0;
      double cnctrLowerRightX = 0.0;
      double cnctrLowerRightY = 0.0;
      double boundsWidth = viewStyle.connectorWidth * viewStyle.connectorBoundsRate;
      double boundsHeight = viewStyle.connectorHeight * viewStyle.connectorBoundsRate;
      double alignOffsetX = 0.0;
      double alignOffsetY = 0.0;

      if (viewStyle.connectorAlignment == ConnectorAlignment.CENTER) {
        alignOffsetX = (bodySize.x - viewStyle.connectorWidth) / 2.0;
        alignOffsetY = (bodySize.y - viewStyle.connectorHeight) / 2.0;
      }
      if (viewStyle.connectorPos == ConnectorPos.LEFT) {
        cnctrUpperLeftX = posX - (boundsWidth + viewStyle.connectorWidth) / 2.0;
        cnctrUpperLeftY = (posY + alignOffsetY)
            - (boundsHeight - viewStyle.connectorHeight) / 2.0 + viewStyle.connectorShift;
        cnctrLowerRightX = cnctrUpperLeftX + boundsWidth;
        cnctrLowerRightY = cnctrUpperLeftY + boundsHeight;

      } else if (viewStyle.connectorPos == ConnectorPos.TOP) {
        cnctrUpperLeftX = (posX + alignOffsetX)
            - (boundsWidth - viewStyle.connectorWidth) / 2.0 + viewStyle.connectorShift;
        cnctrUpperLeftY = posY - (boundsHeight + viewStyle.connectorHeight) / 2.0;
        cnctrLowerRightX = cnctrUpperLeftX + boundsWidth;
        cnctrLowerRightY = cnctrUpperLeftY + boundsHeight;
      }
      bodyRange.setPos(bodyUpperLeftX, bodyUpperLeftY, bodyLowerRightX, bodyLowerRightY);
      cnctrRange.setPos(cnctrUpperLeftX, cnctrUpperLeftY, cnctrLowerRightX, cnctrLowerRightY);
    }

    @Override
    public void removeQuadTreeRect() {
      Rectangles regions = getRegions();
      QuadTreeManager.removeQuadTreeObj(regions.body());
      QuadTreeManager.removeQuadTreeObj(regions.cnctr());
    }

    @Override
    public Vec2D getNodeTreeSize(boolean includeCnctr) {
      return BhNodeViewBase.this.getNodeTreeSize(includeCnctr);
    }

    @Override
    public Vec2D getNodeSize(boolean includeCnctr) {
      return BhNodeViewBase.this.getNodeSize(includeCnctr);
    }

    @Override
    public Vec2D getConnectorSize() {
      return viewStyle.getConnectorSize(isFixed());
    }

    @Override
    public BodyRange getBodyRange() {
      return new BodyRange(bodyRange.getUpperLeftPos(), bodyRange.getLowerRightPos());
    }

    @Override
    public BodyRange getConnectorRange() {
      return new BodyRange(cnctrRange.getUpperLeftPos(), cnctrRange.getLowerRightPos());
    }

    @Override
    public boolean overlapsWith(BhNodeView view, OverlapOption option) {
      if (view.getRegionManager() instanceof RegionManagerBase rm) {
        return bodyRange.overlapsWith(rm.bodyRange, option);
      }
      return false;
    }
  }

  /** ノードビューの GUI ツリーに関する操作を提供するクラス. */
  public class TreeManagerBase implements TreeManager {

    /**
     * ルートを0として、階層が偶数であった場合 true.
     * ただし, outerノードは親と同階層とする
     */
    private boolean isEven = true;

    /**
     * NodeView の親をセットする.
     *
     * @param parentGroup このBhNodeViewを保持するBhNodeViewGroup
     */
    void setParentGroup(BhNodeViewGroup parentGroup) {
      parent = parentGroup;
    }

    @Override
    public BhNodeViewGroup getParentGroup() {
      return parent;
    }

    @Override
    public ConnectiveNodeView getParentView() {
      if (parent == null) {
        return null;
      }
      return parent.getParentView();
    }

    @Override
    public void replace(BhNodeView newNode) {
      if (parent == null
          || newNode == null
          || BhNodeViewBase.this == newNode
          || !(newNode instanceof BhNodeViewBase)) {
        return;
      }
      parent.replace(BhNodeViewBase.this, (BhNodeViewBase) newNode);
      BhNodeViewGroup group = newNode.getTreeManager().getParentGroup();
      if (group != null) {
        group.notifyChildSizeChanged();
      }
      WorkspaceView wsView = newNode.getWorkspaceView();
      if (wsView != null) {
        wsView.moveNodeViewToFront(newNode);
      } else {
        // ノード選択ビュー用の処理
        double posZ = BhNodeViewBase.this.getPositionManager().getZpos();
        newNode.getPositionManager().setTreeZpos(posZ);
      }
      getEventManager().invokeOnNodeReplaced(newNode);
    }

    @Override
    public void removeFromGuiTree() {
      WorkspaceView wsView = getWorkspaceView();
      // JDK-8205092 対策. view order を使うとノード削除後に NullPointerException が発生するのを防ぐ.
      nodeBase.setMouseTransparent(true);
      removeComponentsFromParent();
      getEventManager().invokeOnRemovedFromWorkspaceView(wsView);
      NvbCallbackInvoker.invokeForGroups(
          group -> group.removePseudoViewFromGuiTree(),
          BhNodeViewBase.this);
    }

    /** このノードの描画物 (ボディや影など) をそれぞれの親要素から取り除く. */
    private void removeComponentsFromParent() {
      Parent parent = nodeBase.getParent();
      if (parent instanceof Group group) {
        group.getChildren().remove(nodeBase);
        group.getChildren().remove(compileErrorMark);
      } else if (parent instanceof Pane pane) {
        pane.getChildren().remove(nodeBase);
        pane.getChildren().remove(compileErrorMark);
      }
    }

    @Override
    public void addToGuiTree(Group parent) {
      if (parent == null) {
        return;
      }
      // JDK-8205092 対策
      nodeBase.setMouseTransparent(false);
      addComponentsToParent(parent);
      getEventManager().invokeOnAddToWorkspaceView(getWorkspaceView());
      NvbCallbackInvoker.invokeForGroups(
          group -> group.addPseudoViewToGuiTree(parent),
          BhNodeViewBase.this);
    }

    @Override
    public void addToGuiTree(Pane parent) {
      if (parent == null) {
        return;
      }
      // JDK-8205092 対策
      nodeBase.setMouseTransparent(false);
      addComponentsToParent(parent);
      getEventManager().invokeOnAddToWorkspaceView(getWorkspaceView());
      NvbCallbackInvoker.invokeForGroups(
          group -> group.addPseudoViewToGuiTree(parent),
          BhNodeViewBase.this);
    }

    /** このノードの描画物 (ボディや影など) を {@link parent} に追加する. */
    private void addComponentsToParent(Group parent) {
      parent.getChildren().add(nodeBase);
      parent.getChildren().add(compileErrorMark);
    }

    /** このノードの描画物 (ボディや影など) を {@link parent} に追加する. */
    private void addComponentsToParent(Pane parent) {
      parent.getChildren().add(nodeBase);
      parent.getChildren().add(compileErrorMark);
    }

    /** このノード以下の奇偶フラグを更新する. */
    private void updateEvenFlag() {
      NvbCallbackInvoker.invoke(
          view -> updateEvenFlag(view),
          BhNodeViewBase.this);
    }

    /** {@code view} の奇遇フラグを更新する. */
    private static void updateEvenFlag(BhNodeViewBase view) {
      BhNodeViewBase parentView = view.getTreeManager().getParentView();
      if (parentView != null) {
        BodyShape bodyShape = parentView.getLookManager().getBodyShape();
        if (view.parent.inner && !bodyShape.equals(BodyShape.BODY_SHAPE_NONE)) {
          view.getTreeManager().isEven = !parentView.getTreeManager().isEven;
        } else {
          view.getTreeManager().isEven = parentView.getTreeManager().isEven;
        }
      } else {
        view.getTreeManager().isEven = true;  //ルートは even
      }
      view.getLookManager().switchPseudoClassState(
          BhConstants.Css.PSEUDO_IS_EVEN, view.getTreeManager().isEven);
    }

    @Override
    public BhNodeView getRootView() {
      BhNodeView parent = getParentView();
      if (parent == null) {
        return BhNodeViewBase.this;
      }
      return parent.getTreeManager().getRootView();
    }

    @Override
    public boolean isRootView() {
      return getParentView() == null;
    }

    @Override
    public Parent getParentGuiComponent() {
      return nodeBase.getParent();
    }
  }

  /** ノードビューの位置の変更, 取得に関する操作を提供するクラス. */
  public class PositionManagerBase implements PositionManager {

    private final Vec2D relativePos = new Vec2D(0.0, 0.0);
    private final Vec2D posOnWorkspace = new Vec2D(0.0, 0.0);

    /**
     * 親 {@link BhNodeViewGroup} からの相対位置を設定する.
     *
     * @param posX 親 {@link BhNodeViewGroup} からの相対 X 位置
     * @param posY 親 {@link BhNodeViewGroup} からの相対 Y 位置
     */
    void setRelativePosFromParent(double posX, double posY) {
      relativePos.x = posX;
      relativePos.y = posY;
    }

    /**
     * 親 {@link BhNodeViewGroup} からの相対位置を取得する.
     *
     * @return 親 {@link BhNodeViewGroup} からの相対位置
     */
    Vec2D getRelativePosFromParent() {
      return new Vec2D(relativePos.x, relativePos.y);
    }

    /**
     * ワークスペース上での位置を設定する.
     *
     * @param posX ワークスペース上の X 位置
     * @param posY ワークスペース上の Y 位置
     */
    void setPosOnWorkspace(double posX, double posY) {
      posOnWorkspace.x = posX;
      posOnWorkspace.y = posY;
    }

    @Override
    public Vec2D getPosOnWorkspace() {
      return new Vec2D(posOnWorkspace);
    }

    @Override
    public void setTreePosOnWorkspace(double posX, double posY) {
      BhNodeViewBase.this.updatePosOnWorkspace(posX, posY);
      NvbCallbackInvoker.invoke(
          nodeView -> {
            Vec2D pos = nodeView.getPositionManager().getPosOnWorkspace();
            nodeView.getPositionManager().setComponentPosOnWorkspace(pos.x, pos.y);
            nodeView.getRegionManager().setPosOnQuadTreeSpace(pos.x, pos.y);
          },
          BhNodeViewBase.this);
      NvbCallbackInvoker.invoke(
          nodeView -> {
            Vec2D pos = nodeView.getPositionManager().getPosOnWorkspace();
            nodeView.getEventManager().invokeOnMoved(new Vec2D(pos.x, pos.y));
          },
          BhNodeViewBase.this);
    }

    @Override
    public void setTreePosOnWorkspaceByConnector(double posX, double posY) {
      // ボディの範囲とコネクタの範囲が確定していない状態でも適切な値を返せるように,
      // コネクタの位置と大きさからオフセットを計算する.
      Vec2D cnctrSize = regionManager.getConnectorSize();
      setTreePosOnWorkspace(posX, posY);
      if (viewStyle.connectorPos == ConnectorPos.LEFT) {
        posX += cnctrSize.x;
        if (viewStyle.connectorAlignment == ConnectorAlignment.CENTER) {
          posY += (viewStyle.connectorHeight - regionManager.getNodeSize(false).y) / 2;
        }
      } else if (viewStyle.connectorPos == ConnectorPos.TOP) {
        posY += cnctrSize.y;
        if (viewStyle.connectorAlignment == ConnectorAlignment.CENTER) {
          posX += (viewStyle.connectorWidth - regionManager.getNodeSize(false).x) / 2;
        }
      }
      setTreePosOnWorkspace(posX, posY);
    }

    /** GUI 部品のワークスペース上での位置を更新する. */
    private void setComponentPosOnWorkspace(double posX, double posY) {
      nodeBase.setTranslateX(posX);
      nodeBase.setTranslateY(posY);
      compileErrorMark.setTranslateX(posX);
      compileErrorMark.setTranslateY(posY);
    }

    @Override
    public void setTreeZpos(double pos) {
      MutableDouble posZ = new MutableDouble(pos);
      NvbCallbackInvoker.invoke(
          nodeView -> {
            updateZpos(nodeView, posZ.getValue());
            posZ.add(CHILD_VIEW_ORDER_OFFSET_FROM_PARENT);
          },
          BhNodeViewBase.this);
    }

    /** {@code view} の Z 位置を更新する. */
    private static void updateZpos(BhNodeViewBase view, double posZ) {
      view.compileErrorMark.setViewOrder(posZ + COMPILE_ERR_MARK_VIEW_ORDER_OFFSET);
      view.nodeBase.setViewOrder(posZ + NODE_BASE_VIEW_ORDER_OFFSET);
    }

    @Override
    public double getZpos() {
      return nodeBase.getViewOrder();
    }

    @Override
    public void move(double diffX, double diffY) {
      WorkspaceView wsView = getWorkspaceView();
      if (wsView == null) {
        return;
      }
      Vec2D posOnWs = getPosOnWorkspace();
      Vec2D wsSize = wsView.getSize();
      Vec2D newPos = ViewUtil.newPosition(new Vec2D(diffX, diffY), wsSize, posOnWs);
      setTreePosOnWorkspace(newPos.x, newPos.y);
    }

    @Override
    public void move(Vec2D diff) {
      move(diff.x, diff.y);
    }

    @Override
    public Vec2D sceneToLocal(Vec2D pos) {
      var localPos = nodeBase.sceneToLocal(pos.x, pos.y);
      return new Vec2D(localPos.getX(), localPos.getY());
    }

    @Override
    public Vec2D localToScene(Vec2D pos) {
      var scenePos = nodeBase.localToScene(pos.x, pos.y);
      return new Vec2D(scenePos.getX(), scenePos.getY());
    }
  }

  /** ノードビューのイベントハンドラの登録および削除操作を提供するクラス. */
  public class EventManagerBase implements EventManager {

    /** ノードビューの位置が変わったときのイベントハンドラのリスト. */
    private SequencedSet<BiConsumer<? super BhNodeView, ? super Vec2D>> onMovedList
        = new LinkedHashSet<>();
    /** ノードビューをワークスペースビューに追加したときのイベントハンドラのリスト. */
    private SequencedSet<BiConsumer<? super WorkspaceView, ? super BhNodeView>> onAddToWorkspaceView
        = new LinkedHashSet<>();
    /** ノードビューをワークスペースビューから取り除いたときのイベントハンドラのリスト. */
    private SequencedSet<BiConsumer<? super WorkspaceView, ? super BhNodeView>>
        onRemovedFromWorkspaceView = new LinkedHashSet<>();
    /** ノードのサイズが変更されたときのイベントハンドラのリスト. */
    private SequencedSet<Consumer<? super BhNodeView>> onNodeSizeChanged = new LinkedHashSet<>();
    /** ノードが入れ替わったときのイベントハンドラのリスト. */
    private SequencedSet<BiConsumer<? super BhNodeView, ? super BhNodeView>> onNodeReplaced =
        new LinkedHashSet<>();

    @Override
    public void setOnMousePressed(EventHandler<? super MouseEvent> handler) {
      nodeShape.setOnMousePressed(handler);
    }

    @Override
    public void setOnMouseDragged(EventHandler<? super MouseEvent> handler) {
      nodeShape.setOnMouseDragged(handler);
    }

    @Override
    public void setOnDragDetected(EventHandler<? super MouseEvent> handler) {
      nodeShape.setOnDragDetected(handler);
    }

    @Override
    public void setOnMouseReleased(EventHandler<? super MouseEvent> handler) {
      nodeShape.setOnMouseReleased(handler);
    }

    @Override
    public <T extends Event> void addEventFilter(
        EventType<T> type, EventHandler<? super T> handler) {
      nodeShape.addEventFilter(type, handler);
    }

    @Override
    public <T extends Event> void removeEventFilter(
        EventType<T> type, EventHandler<? super T> handler) {
      nodeShape.removeEventFilter(type, handler);
    }

    @Override
    public void dispatch(Event event) {
      nodeShape.fireEvent(event);
    }

    @Override
    public void addOnMoved(BiConsumer<? super BhNodeView, ? super Vec2D> handler) {
      onMovedList.addLast(handler);
    }

    @Override
    public void removeOnMoved(BiConsumer<? super BhNodeView, ? super Vec2D> handler) {
      onMovedList.remove(handler);
    }

    @Override
    public void addOnAddedToWorkspaceView(
        BiConsumer<? super WorkspaceView, ? super BhNodeView> handler) {
      onAddToWorkspaceView.addLast(handler);
    }

    @Override
    public void removeOnAddedToWorkspaceView(
        BiConsumer<? super WorkspaceView, ? super BhNodeView> handler) {
      onAddToWorkspaceView.remove(handler);
    }

    @Override
    public void addOnRemovedFromWorkspaceView(
        BiConsumer<? super WorkspaceView, ? super BhNodeView> handler) {
      onRemovedFromWorkspaceView.addLast(handler);
    }

    @Override
    public void removeOnRemovedFromWorkspaceView(
        BiConsumer<? super WorkspaceView, ? super BhNodeView> handler) {
      onRemovedFromWorkspaceView.remove(handler);
    }

    @Override
    public void addOnNodeSizeChanged(Consumer<? super BhNodeView> handler) {
      onNodeSizeChanged.addLast(handler);
    }

    @Override
    public void removeOnNodeSizeChanged(Consumer<? super BhNodeView> handler) {
      onNodeSizeChanged.remove(handler);
    }

    @Override
    public void addOnNodeReplaced(BiConsumer<? super BhNodeView, ? super BhNodeView> handler) {
      onNodeReplaced.addLast(handler);
    }

    @Override
    public void removeOnNodeReplaced(BiConsumer<? super BhNodeView, ? super BhNodeView> handler) {
      onNodeReplaced.remove(handler);
    }

    /**
     * ノードビューの位置が変わったときのイベントハンドラを実行する.
     *
     * @param posOnWs 変更後のワークスペースビュー上での位置
     */
    private void invokeOnMoved(Vec2D posOnWs) {
      if (!Platform.isFxApplicationThread()) {
        throw new IllegalStateException(
            Utility.getCurrentMethodName() 
            + " - a handler invoked in an inappropriate thread");
      }
      onMovedList.forEach(handler -> handler.accept(BhNodeViewBase.this, posOnWs));
    }

    /** ノードビューをワークスペースビューに追加したときのイベントハンドラを実行する. */
    private void invokeOnAddToWorkspaceView(WorkspaceView wsView) {
      if (!Platform.isFxApplicationThread()) {
        throw new IllegalStateException(
          Utility.getCurrentMethodName()
          + " - a handler invoked in an inappropriate thread");
      }
      if (wsView == null) {
        return;
      }
      onAddToWorkspaceView.forEach(handler -> handler.accept(wsView, BhNodeViewBase.this));
    }

    /** ノードビューをワークスペースビューから取り除いたときのイベントハンドラを実行する. */
    private void invokeOnRemovedFromWorkspaceView(WorkspaceView wsView) {
      if (!Platform.isFxApplicationThread()) {
        throw new IllegalStateException(
            Utility.getCurrentMethodName() 
            + " - a handler invoked in an inappropriate thread");
      }
      if (wsView == null) {
        return;
      }
      onRemovedFromWorkspaceView.forEach(handler -> handler.accept(wsView, BhNodeViewBase.this));
    }

    /** ノードビューのサイズが変更されたときのイベントハンドラを実行する. */
    private void invokeOnNodeSizeChanged() {
      if (!Platform.isFxApplicationThread()) {
        throw new IllegalStateException(
            Utility.getCurrentMethodName() 
            + " - a handler invoked in an inappropriate thread");
      }
      onNodeSizeChanged.forEach(handler -> handler.accept(BhNodeViewBase.this));
    }

    /** ノードビューが入れ替わったときのイベントハンドラを実行する. */
    private void invokeOnNodeReplaced(BhNodeView newView) {
      if (!Platform.isFxApplicationThread()) {
        throw new IllegalStateException(
            Utility.getCurrentMethodName() 
            + " - a handler invoked in an inappropriate thread");
      }
      onNodeReplaced.forEach(handler -> handler.accept(BhNodeViewBase.this, newView));
    }
  }

  private class CompileErrorMark extends Line {
    CompileErrorMark(double startX, double startY, double endX, double endY) {
      super(startX, startY, endX, endY);
      this.setVisible(false);
      this.setMouseTransparent(true);
      this.setViewOrder(COMPILE_ERR_MARK_VIEW_ORDER_OFFSET);
    }
  }
}
