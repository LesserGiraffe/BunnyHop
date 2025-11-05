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


import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.control.BhNodeController;
import net.seapanda.bunnyhop.node.control.TemplateNodeController;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.derivative.DerivativeBase;
import net.seapanda.bunnyhop.node.view.bodyshape.BodyShapeBase.BodyShape;
import net.seapanda.bunnyhop.node.view.component.Star;
import net.seapanda.bunnyhop.node.view.component.WarningIcon;
import net.seapanda.bunnyhop.node.view.connectorshape.ConnectorShape;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle.ChildArrangement;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle.ConnectorAlignment;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle.ConnectorPos;
import net.seapanda.bunnyhop.node.view.traverse.NvbCallbackInvoker;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.utility.Showable;
import net.seapanda.bunnyhop.utility.Utility;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.SimpleConsumerInvoker;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.workspace.view.WorkspaceView;
import net.seapanda.bunnyhop.workspace.view.quadtree.QuadTreeManager;
import net.seapanda.bunnyhop.workspace.view.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.workspace.view.quadtree.QuadTreeRectangle.OverlapOption;
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
  /** GUI コンポーネントを乗せるペイン一式. */
  private final Panes panes;
  /** ノードビューの描画に必要な図形オブジェクト一式. */
  private final Shapes shapes;
  /** ノードの見た目のパラメータオブジェクト. */
  protected final BhNodeViewStyle viewStyle;
  /** このノードビューを保持する親グループ.  このノードビューがルートノードビューの場合は null. */
  protected BhNodeViewGroup parent;
  /** このノードビューに対応するノード. (nullable) */
  private final BhNode model;
  /** このノードビューに対応するコントローラ. */
  private BhNodeController controller;
  /** ノードのサイズ変更が通知されたことを示すフラグ. */
  private boolean isSizeChangeNotified = false;

  private final RegionManagerBase regionManager = this.new RegionManagerBase();
  private final TreeManagerBase treeManager = this.new TreeManagerBase();
  private final PositionManagerBase positionManager = this.new PositionManagerBase();
  private final CallbackRegistryBase cbRegistry;
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
   * ノードのサイズに変更があったときの処理を行う.
   *
   * <p>ノードのサイズに変更があったときに自動的に呼ばれるので, サブクラスで呼ばないこと.
   */
  protected abstract void onNodeSizeChanged();

  /**
   * コンストラクタ.
   *
   * @param style ノードの見た目を決めるパラメータオブジェクト
   * @param model ビューが表すモデル (nullable)
   * @param components このノードビューの子要素に追加するコンポーネントのリスト
   */
  protected BhNodeViewBase(
      BhNodeViewStyle style, DerivativeBase<?> model, SequencedSet<Node> components)
      throws ViewConstructionException {
    this.viewStyle = style;
    this.model = model;
    shapes = createShapes(style);
    panes = createPanes(style, components);
    cbRegistry = this.new CallbackRegistryBase();
    lookManager = this.new LookManagerBase(style.bodyShape);
    lookManager.addCssClass(style.cssClasses);
    lookManager.addCssClass(BhConstants.Css.CLASS_BH_NODE);
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
  public CallbackRegistryBase getCallbackRegistry() {
    return cbRegistry;
  }

  @Override
  public LookManagerBase getLookManager() {
    return lookManager;
  }

  @Override
  public void setMouseTransparent(boolean value) {
    panes.root.setMouseTransparent(value);
    shapes.nodeShape.setMouseTransparent(value);
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
    return ViewUtil.getWorkspaceView(panes.root);
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
    view.getCallbackRegistry().dispatch(event);
    event.consume();
  }

  /** このノードビューの子要素のサイズが変更されたときにサブクラスから呼ぶこと. */
  protected void notifyChildSizeChanged() {
    onNodeSizeChanged();
    if (isSizeChangeNotified) {
      return;
    }
    BhNodeViewGroup group = getTreeManager().getParentGroup();
    if (group != null) {
      group.notifyChildSizeChanged();
    }
    if (getTreeManager().isRootView()) {
      getLookManager().requestArrangement();
    }
    isSizeChangeNotified = true;
  }

  /**
   * このビューに GUI コンポーネントを追加する.
   *
   * @param node 追加するコンポーネント. (nullable)
   */
  protected void setComponent(Node node) {
    if (node == null) {
      panes.specific.getChildren().clear();  
    }
    panes.specific.getChildren().setAll(node);
  }

  /** ノードの状態を表すインジケータの可視性を更新する. */
  protected void updateNodeStatusVisibility() {
    if (model != null) {
      lookManager.setCompileErrorVisibility(model.hasCompileError());
      lookManager.setBreakpointVisibility(model.isBreakpointSet());
      lookManager.setCorruptionMarkVisibility(model.isCorrupted());
    }
  }

  /** ノードビューの描画に必要な図形オブジェクトを作成する. */
  private Shapes createShapes(BhNodeViewStyle style) {
    var compileErrorMark = new CompileErrorMark(0, 0, 0, 0);
    compileErrorMark.getStyleClass().add(BhConstants.Css.CLASS_COMPILE_ERROR_MARK);

    var nodeShape = new Polygon();
    nodeShape.addEventFilter(Event.ANY, this::forwardEventIfNotHaveController);

    double radius = style.commonPart.breakpoint.radius;
    var circle = new Circle(radius, radius, radius);
    circle.setMouseTransparent(true);
    circle.setVisible(false);
    circle.getStyleClass().add(style.commonPart.breakpoint.cssClass);

    double size = style.commonPart.execStepMark.size;
    Polygon execMark = new Star(size, size, 4, style.commonPart.execStepMark.cssClass);
    execMark.setVisible(false);

    size = style.commonPart.corruptionMark.size;
    var corruption = new WarningIcon(size, size, style.commonPart.corruptionMark.cssClass);
    corruption.setVisible(false);

    return new Shapes(nodeShape, compileErrorMark, circle, execMark, corruption);
  }

  /** GUI コンポーネントを乗せるペインを作成する. */
  private Panes createPanes(BhNodeViewStyle style, SequencedSet<Node> components) {
    boolean isBaseArrangementRow = (style.baseArrangement == ChildArrangement.ROW);
    Pane root = new Pane();
    Pane compBase = isBaseArrangementRow ? new HBox() : new VBox();
    Pane common = (style.commonPart.arrangement == ChildArrangement.ROW) ? new HBox() : new VBox();
    Pane specific = new HBox();
    root.getChildren().addAll(shapes.nodeShape, compBase);
    root.setPickOnBounds(false);

    compBase.getChildren().addAll(common, specific);
    compBase.setPickOnBounds(false);
    compBase.widthProperty().addListener((obs, oldVal, newVal) -> notifyChildSizeChanged());
    compBase.heightProperty().addListener((obs, oldVal, newVal) -> notifyChildSizeChanged());
    compBase.setTranslateX(style.paddingLeft);
    compBase.setTranslateY(style.paddingTop);

    common.setPickOnBounds(false);
    common.getStyleClass().add(style.commonPart.cssClass);
    common.getChildren().add(shapes.nextStep);
    common.getChildren().add(shapes.breakpoint);
    common.getChildren().add(shapes.corruption);
    common.getChildren().addAll(components);
    common.getChildren().forEach(child -> {
      child.managedProperty().bind(child.visibleProperty());
      child.visibleProperty().addListener((obs, oldVal, newVal) -> changeCommonPartValidity());
    });
    var pseudoClass = PseudoClass.getPseudoClass(
        isBaseArrangementRow ? BhConstants.Css.PSEUDO_ROW : BhConstants.Css.PSEUDO_COLUMN);
    common.pseudoClassStateChanged(pseudoClass, true);
    boolean anyVisible = common.getChildren().stream().anyMatch(Node::isVisible);
    common.setManaged(anyVisible);

    specific.setPickOnBounds(false);
    specific.pseudoClassStateChanged(pseudoClass, true);
    specific.getStyleClass().add(style.specificPart.cssClass);
    return new Panes(root, compBase, common, specific);
  }

  /** 共通部分が持つ UI コンポーネントの可視性に応じて, 共通部分の UI ツリー上での有効性を変更する. */
  private void changeCommonPartValidity() {
    boolean anyVisible = panes.common.getChildren().stream().anyMatch(Node::isVisible);
    panes.common.setManaged(anyVisible);
    // undo / redo でノードをワークスペースに戻したときに componentBase 内部のレイアウトが崩れないようにするために必要
    if (panes.componentBase instanceof Region region) {
      region.resize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    }
  }

  /** ノードビューの外観を変更する機能を提供するクラス. */
  public class LookManagerBase implements LookManager {

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
      shapes.nodeShape.pseudoClassStateChanged(PseudoClass.getPseudoClass(className), enable);
    }

    /**
     * cssクラス名を追加する.
     *
     * @param cssClassNames css クラス名
     */
    void addCssClass(String... cssClassNames) {
      for (var cssClassName : cssClassNames) {
        shapes.nodeShape.getStyleClass().add(cssClassName);
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
  
      shapes.nodeShape.getPoints().setAll(
          getBodyShape().shape.createVertices(
              viewStyle,
              bodySize.x,
              bodySize.y,
              cnctrShape,
              notchShape));
      shapes.compileError.setEndX(bodySize.x);
      shapes.compileError.setEndY(bodySize.y);
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
              nodeView.getCallbackRegistry().onSizeChanged();
            }
            nodeView.isSizeChangeNotified = false;
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
          view -> view.shapes.nodeShape.setVisible(visible),
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
      shapes.compileError.setVisible(visible);
    }

    @Override
    public boolean isCompileErrorVisible() {
      return shapes.compileError.isVisible();
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
    public void showShadow(EffectTarget target) {
      Consumer<? super BhNodeViewBase> fnShowShadow = nodeView -> {
        if (nodeView.getLookManager().getBodyShape() != BodyShape.BODY_SHAPE_NONE) {
          nodeView.getLookManager().switchPseudoClassState(BhConstants.Css.PSEUDO_SHADOW, true);
        }
      };
      applyEffectRenderer(target, fnShowShadow);
    }

    @Override
    public void hideShadow(EffectTarget target) {
      Consumer<? super BhNodeViewBase> fnHideShadow = nodeView ->
          nodeView.getLookManager().switchPseudoClassState(BhConstants.Css.PSEUDO_SHADOW, false);
      applyEffectRenderer(target, fnHideShadow);
    }

    /**
     * {@code target} で指定した対象にエフェクトを描画する処理を適用する.
     *
     * @param target エフェクトを適用するターゲット
     * @param renderer エフェクトを描画するメソッド
     */
    private void applyEffectRenderer(
        EffectTarget target, Consumer<? super BhNodeViewBase> renderer) {
      switch (target) {
        case SELF -> renderer.accept(BhNodeViewBase.this);
        case CHILDREN -> NvbCallbackInvoker.invoke(renderer, BhNodeViewBase.this);
        case OUTERS -> NvbCallbackInvoker.invokeForOuters(renderer, BhNodeViewBase.this);
        default ->
            throw new IllegalArgumentException("Invalid Rendering Target {%s}".formatted(target));
      }
    }

    @Override
    public ConnectorPos getConnectorPos() {
      return viewStyle.connectorPos;
    }

    @Override
    public void setBreakpointVisibility(boolean visible) {
      shapes.breakpoint.setVisible(visible);
    }

    @Override
    public boolean isBreakpointVisible() {
      return shapes.breakpoint.isVisible();
    }

    @Override
    public void setExecStepMarkVisibility(boolean visible) {
      BhNodeViewBase.this.getLookManager()
          .switchPseudoClassState(BhConstants.Css.PSEUDO_EXEC_STEP, visible);
      shapes.nextStep.setVisible(visible);
    }

    @Override
    public boolean isExecStepMarkVisible() {
      return shapes.nextStep.isVisible();
    }

    @Override
    public void setCorruptionMarkVisibility(boolean visible) {
      shapes.corruption.setVisible(visible);
    }

    @Override
    public boolean isCorruptionMarkVisible() {
      return shapes.corruption.isVisible();
    }
  }

  /** このノードの画面上での領域に関する処理を行うクラス. */
  public class RegionManagerBase implements RegionManager {
    /** ノード全体の範囲. */
    private final QuadTreeRectangle bodyRange =
        new QuadTreeRectangle(0.0, 0.0, 0.0, 0.0, BhNodeViewBase.this);
    /** コネクタ部分の範囲. */
    private final QuadTreeRectangle cnctrRange =
        new QuadTreeRectangle(0.0, 0.0, 0.0, 0.0, BhNodeViewBase.this);

    @Override
    public List<BhNodeView> searchForOverlapped() {
      List<QuadTreeRectangle> overlappedRectList =
          cnctrRange.searchOverlappedRects(OverlapOption.INTERSECT);
      return overlappedRectList.stream()
          .map(QuadTreeRectangle::<BhNodeView>getUserData)
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
      bodyRange.setPos(posX, posY, bodyLowerRightX, bodyLowerRightY);
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

    @Override
    public Vec2D getCommonPartSize() {
      if (!panes.common.isManaged()) {
        return new Vec2D();
      }
      return new Vec2D(panes.common.getWidth(), panes.common.getHeight());
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
    }

    @Override
    public void removeFromGuiTree() {
      // JDK-8205092 対策. view order を使うとノード削除後に NullPointerException が発生するのを防ぐ.
      panes.root.setMouseTransparent(true);
      removeComponentsFromParent();
      NvbCallbackInvoker.invokeForGroups(
          BhNodeViewGroup::removePseudoViewFromGuiTree,
          BhNodeViewBase.this);
    }

    /** このノードの描画物 (ボディや影など) をそれぞれの親要素から取り除く. */
    private void removeComponentsFromParent() {
      Parent parent = panes.root.getParent();
      if (parent instanceof Group group) {
        group.getChildren().remove(panes.root);
        group.getChildren().remove(shapes.compileError);
        cbRegistry.onParentViewChangedInvoker.invoke(
            new ParentViewChangedEvent(BhNodeViewBase.this, group, null));
      } else if (parent instanceof Pane pane) {
        pane.getChildren().remove(panes.root);
        pane.getChildren().remove(shapes.compileError);
        cbRegistry.onParentViewChangedInvoker.invoke(
            new ParentViewChangedEvent(BhNodeViewBase.this, pane, null));
      }
    }

    @Override
    public void addToGuiTree(Group parent) {
      if (parent == null) {
        return;
      }
      // JDK-8205092 対策
      panes.root.setMouseTransparent(false);
      addComponentsToParent(parent);
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
      panes.root.setMouseTransparent(false);
      addComponentsToParent(parent);
      NvbCallbackInvoker.invokeForGroups(
          group -> group.addPseudoViewToGuiTree(parent),
          BhNodeViewBase.this);
    }

    /** このノードの描画物 (ボディや影など) を {@code parent} に追加する. */
    private void addComponentsToParent(Group parent) {
      Parent oldParent = panes.root.getParent();
      if (oldParent != parent) {
        parent.getChildren().add(panes.root);
        parent.getChildren().add(shapes.compileError);
        cbRegistry.onParentViewChangedInvoker.invoke(
            new ParentViewChangedEvent(BhNodeViewBase.this, oldParent, parent));
      }
    }

    /** このノードの描画物 (ボディや影など) を {@code parent} に追加する. */
    private void addComponentsToParent(Pane parent) {
      Parent oldParent = panes.root.getParent();
      if (oldParent != parent) {
        parent.getChildren().add(panes.root);
        parent.getChildren().add(shapes.compileError);
        cbRegistry.onParentViewChangedInvoker.invoke(
            new ParentViewChangedEvent(BhNodeViewBase.this, oldParent, parent));
      }
    }

    /** このノード以下の奇偶フラグを更新する. */
    private void updateEvenFlag() {
      NvbCallbackInvoker.invoke(
          TreeManagerBase::updateEvenFlag,
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
      return panes.root.getParent();
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
          nodeView -> nodeView.getCallbackRegistry().onMoved(),
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
      panes.root.setTranslateX(posX);
      panes.root.setTranslateY(posY);
      shapes.compileError.setTranslateX(posX);
      shapes.compileError.setTranslateY(posY);
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
    private static void updateZpos(BhNodeViewBase view, double pos) {
      view.shapes.compileError.setViewOrder(pos + COMPILE_ERR_MARK_VIEW_ORDER_OFFSET);
      view.panes.root.setViewOrder(pos + NODE_BASE_VIEW_ORDER_OFFSET);
    }

    @Override
    public double getZpos() {
      return panes.root.getViewOrder();
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
      var localPos = panes.root.sceneToLocal(pos.x, pos.y);
      return new Vec2D(localPos.getX(), localPos.getY());
    }

    @Override
    public Vec2D localToScene(Vec2D pos) {
      var scenePos = panes.root.localToScene(pos.x, pos.y);
      return new Vec2D(scenePos.getX(), scenePos.getY());
    }
  }

  /** {@link BhNodeView} に対してイベントハンドラを追加または削除する機能を提供するクラス. */
  public class CallbackRegistryBase implements CallbackRegistry {

    /** 関連するノードビュー上でマウスボタンが押下されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<MouseEventInfo> onMousePressedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するノードビューがドラッグされたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<MouseEventInfo> onMouseDraggedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するノードビュー上でマウスのドラッグが検出されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<MouseEventInfo> onMouseDragDetectedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するノードビュー上でマウスボタンが離されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<MouseEventInfo> onMouseReleasedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するノードビューの位置が変わったときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<MoveEvent> onMovedInvoker = new SimpleConsumerInvoker<>();

    /** 関連するノードビューのサイズが変わったときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<SizeChangedEvent> onSizeChangedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するノードビューの GUI ツリー上の親要素が変わったときのイベントハンドラ. */
    private final ConsumerInvoker<ParentViewChangedEvent> onParentViewChangedInvoker =
        new SimpleConsumerInvoker<>();

    /** {@link #dispatch} で送られたイベントを区別するためのフラグ. */
    private final Deque<MouseEventInfo> eventStack = new LinkedList<>();

    /** コンストラクタ. */
    private CallbackRegistryBase() {
      shapes.nodeShape.setOnMousePressed(event -> {
        onMousePressedInvoker.invoke(
            new MouseEventInfo(BhNodeViewBase.this, event, eventStack.peekLast()));
        consume(event);
      });
      shapes.nodeShape.setOnMouseDragged(event -> {
        onMouseDraggedInvoker.invoke(
            new MouseEventInfo(BhNodeViewBase.this, event, eventStack.peekLast()));
        consume(event);
      });
      shapes.nodeShape.setOnDragDetected(event -> {
        onMouseDragDetectedInvoker.invoke(
            new MouseEventInfo(BhNodeViewBase.this, event, eventStack.peekLast()));
        consume(event);
      });
      shapes.nodeShape.setOnMouseReleased(event -> {
        onMouseReleasedInvoker.invoke(
            new MouseEventInfo(BhNodeViewBase.this, event, eventStack.peekLast()));
        consume(event);
      });
    }

    @Override
    public ConsumerInvoker<MouseEventInfo>.Registry getOnMousePressed() {
      return onMousePressedInvoker.getRegistry();
    }

    @Override
    public ConsumerInvoker<MouseEventInfo>.Registry getOnMouseDragged() {
      return onMouseDraggedInvoker.getRegistry();
    }

    @Override
    public ConsumerInvoker<MouseEventInfo>.Registry getOnMouseDragDetected() {
      return onMouseDragDetectedInvoker.getRegistry();
    }

    @Override
    public ConsumerInvoker<MouseEventInfo>.Registry getOnMouseReleased() {
      return onMouseReleasedInvoker.getRegistry();
    }

    @Override
    public ConsumerInvoker<MoveEvent>.Registry getOnMoved() {
      return onMovedInvoker.getRegistry();
    }

    @Override
    public ConsumerInvoker<SizeChangedEvent>.Registry getOnSizeChanged() {
      return onSizeChangedInvoker.getRegistry();
    }

    @Override
    public ConsumerInvoker<ParentViewChangedEvent>.Registry getOnParentViewChanged() {
      return onParentViewChangedInvoker.getRegistry();
    }

    @Override
    public void dispatch(Event event) {
      shapes.nodeShape.fireEvent(event);
    }

    @Override
    public void forward(MouseEventInfo info) {
      if (info == null) {
        return;
      }
      eventStack.addLast(info);
      shapes.nodeShape.fireEvent(info.event());
      eventStack.removeLast();
    }

    /** ノードビューの位置が変わったときのイベントハンドラを呼び出す. */
    private void onMoved() {
      if (!Platform.isFxApplicationThread()) {
        throw new IllegalStateException(
            Utility.getCurrentMethodName() + " - a handler invoked in an inappropriate thread");
      }
      onMovedInvoker.invoke(new MoveEvent(BhNodeViewBase.this));
    }

    /** ノードビューのサイズが変更されたときのイベントハンドラを呼び出す. */
    private void onSizeChanged() {
      if (!Platform.isFxApplicationThread()) {
        throw new IllegalStateException(
            Utility.getCurrentMethodName() + " - a handler invoked in an inappropriate thread");
      }
      onSizeChangedInvoker.invoke(new SizeChangedEvent(BhNodeViewBase.this));
    }

    /** {@code event} のターゲットがノード本体を描画するポリゴンであった場合 {@code event} を consume する. */
    private void consume(MouseEvent event) {
      if (event.getTarget() == shapes.nodeShape) {
        event.consume();
      }
    }
  }

  private static class CompileErrorMark extends Line {
    CompileErrorMark(double startX, double startY, double endX, double endY) {
      super(startX, startY, endX, endY);
      this.setVisible(false);
      this.setMouseTransparent(true);
      this.setViewOrder(COMPILE_ERR_MARK_VIEW_ORDER_OFFSET);
    }
  }

  /**
   * 各種 GUI コンポーネントを配置するペインをまとめたレコード.
   *
   * @param root ノードビューの基底部分となるペイン
   * @param componentBase {@code common} と {@code specific} を乗せるペイン
   * @param common ノードビューが共通で持つコンポーネントを乗せるペイン
   * @param specific ノードビューの種類ごとに異なるコンポーネント (テキストフィールドやコンボボックスなど) を乗せるペイン
   */
  private record Panes(
      Pane root,
      Pane componentBase,
      Pane common,
      Pane specific) {}

  /**
   * ノードビューの描画に必要な図形オブジェクトをまとめたレコード.
   *
   * @param nodeShape ノード本体を描画するためのポリゴン
   * @param compileError コンパイルエラーが発生していることを示す印
   * @param breakpoint ブレークポイントが設定されていることを示す印
   * @param nextStep 次に実行されるノードであることを示す印
   * @param corruption ノードが破損していることを示す印
   */
  private record Shapes(
      Polygon nodeShape,
      CompileErrorMark compileError,
      Circle breakpoint,
      Polygon nextStep,
      Group corruption) {}
}
