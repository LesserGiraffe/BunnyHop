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


import static net.seapanda.bunnyhop.common.configuration.BhConstants.Css.Class.COMPILE_ERROR_MARK;

import java.util.Optional;
import java.util.SequencedSet;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polygon;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.control.BhNodeController;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.derivative.Derivative;
import net.seapanda.bunnyhop.node.view.component.BreakpointIcon;
import net.seapanda.bunnyhop.node.view.component.CompileErrorMark;
import net.seapanda.bunnyhop.node.view.component.CorruptionIcon;
import net.seapanda.bunnyhop.node.view.component.NextStepIcon;
import net.seapanda.bunnyhop.node.view.component.PlayIcon;
import net.seapanda.bunnyhop.node.view.component.RuntimeErrorIcon;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.node.view.style.ChildArrangement;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.workspace.view.WorkspaceView;
import net.seapanda.bunnyhop.workspace.view.quadtree.QuadTreeItem;

/**
 * {@link BhNode} に対応するビュークラスの抽象基底クラス.
 *
 * @author K.Koike
 */
abstract class BhNodeViewBase implements BhNodeView {

  /** GUI コンポーネントを乗せるペイン一式. */
  private final Panes panes;
  /** ノードビューの描画に必要な図形オブジェクト一式. */
  private final Shapes shapes;
  /** ノードの見た目のパラメータオブジェクト. */
  private final BhNodeViewStyle style;
  /** このノードビューに対応するノード. (nullable) */
  private final BhNode model;
  /** このノードビューに対応するコントローラ. */
  private BhNodeController controller;
  /** このノードがテンプレートノードビューである場合 true. */
  private final boolean isTemplate;

  private final QuadTreeSpaceRegistrationImpl qtsRegistration;
  private final VisualImpl visual;
  private final TreeControlImpl treeCtrl;
  private final ArrangementImpl arrangement;
  private final CallbackRegistryImpl cbRegistry;
  private final SizeChangeNotifier sizeChangeNotif;

  /**
   * コンストラクタ.
   *
   * @param style ノードの見た目を決めるパラメータオブジェクト
   * @param model ビューが表すモデル (nullable)
   * @param components このノードビューの子要素に追加するコンポーネントのリスト
   * @param isTemplate このノードビューがテンプレートノードビューである場合 true
   */
  BhNodeViewBase(
      BhNodeViewStyle style, Derivative<?> model, SequencedSet<Node> components, boolean isTemplate)
      throws ViewConstructionException {
    this.style = style;
    this.model = model;
    this.isTemplate = isTemplate;
    shapes = createShapes(style);
    panes = createPanes(style, components);
    sizeChangeNotif = new SizeChangeNotifier(this);

    var body = new QuadTreeItem(0, 0, 0, 0, this);
    var cnctr = new QuadTreeItem(0, 0, 0, 0, this);
    cbRegistry = new CallbackRegistryImpl(this);
    qtsRegistration = new QuadTreeSpaceRegistrationImpl(body, cnctr);
    arrangement = new ArrangementImpl(this, sizeChangeNotif);
    visual = new VisualImpl(this);
    treeCtrl = new TreeControlImpl(this);
    visual.addCssClass(style.cssClasses);
    visual.addCssClass(BhConstants.Css.Class.BH_NODE);
  }


  @Override
  public abstract GeometryBase getGeometry();

  @Override
  public ArrangementImpl getArrangement() {
    return arrangement;
  }

  @Override
  public QuadTreeSpaceRegistrationImpl getQuadTreeSpaceRegistration() {
    return qtsRegistration;
  }

  @Override
  public VisualImpl getVisual() {
    return visual;
  }

  @Override
  public TreeControlImpl getTreeControl() {
    return treeCtrl;
  }

  @Override
  public CallbackRegistryImpl getCallbackRegistry() {
    return cbRegistry;
  }

  @Override
  public void setMouseTransparent(boolean value) {
    panes.root.setMouseTransparent(value);
    shapes.nodeShape.setMouseTransparent(value);
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
    return isTemplate;
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
    BhNodeView view = getTreeControl().getParentView();
    if (view == null) {
      event.consume();
      return;
    }
    view.getCallbackRegistry().dispatch(event);
    event.consume();
  }

  /**
   * このビューに GUI コンポーネントを追加する.
   *
   * @param node 追加するコンポーネント. (nullable)
   */
  void setComponent(Node node) {
    if (node == null) {
      panes.specific.getChildren().clear();
      return;
    }
    panes.specific.getChildren().setAll(node);
  }

  SizeChangeNotifier getSizeChangeNotifier() {
    return sizeChangeNotif;
  }

  /** GUI コンポーネントを乗せるペイン一式を取得する. */
  Panes getPanes() {
    return panes;
  }

  /** ノードビューの描画に必要な図形オブジェクト一式を取得する. */
  Shapes getShapes() {
    return shapes;
  }


  /** ノードの見た目のパラメータを取得する. */
  BhNodeViewStyle getStyle() {
    return style;
  }

  /** ノードビューの描画に必要な図形オブジェクトを作成する. */
  private Shapes createShapes(BhNodeViewStyle style) {
    var compileError = new CompileErrorMark(0, 0, COMPILE_ERROR_MARK, false);
    compileError.setViewOrder(ViewOrderOffset.COMPILE_ERR_MARK.value());

    var nodeShape = new Polygon();
    nodeShape.addEventFilter(Event.ANY, this::forwardEventIfNotHaveController);

    double radius = style.commonPart.breakpointIcon.radius;
    final var circle = new BreakpointIcon(radius, style.commonPart.breakpointIcon.cssClass, false);

    radius = style.commonPart.nextStepIcon.radius;
    final var execute = new NextStepIcon(radius, style.commonPart.nextStepIcon.cssClass, false);

    radius = style.commonPart.runtimeErrorIcon.radius;
    var runtimeErr =
        new RuntimeErrorIcon(radius, style.commonPart.runtimeErrorIcon.cssClass, false);

    double size = style.commonPart.corruptionIcon.size;
    var corruption =
        new CorruptionIcon(size, size, style.commonPart.corruptionIcon.cssClass, false);

    radius = style.commonPart.entryPointIcon.radius;
    var entryPoint = new PlayIcon(radius, style.commonPart.entryPointIcon.cssClass, false);

    return new Shapes(
        nodeShape,
        compileError,
        circle,
        execute,
        runtimeErr,
        corruption,
        entryPoint);
  }

  /** GUI コンポーネントを乗せるペインを作成する. */
  private Panes createPanes(BhNodeViewStyle style, SequencedSet<Node> components) {
    boolean isBaseArrangementRow = (style.baseArrangement == ChildArrangement.ROW);
    Pane root = new Pane();
    root.setPickOnBounds(false);
    // 適切な位置に配置される前の状態が見えるのを防ぐ. (小さくしすぎると WS のスクロールが正常に行われなくなる)
    root.setTranslateY(-5000.0);

    Pane compBase = isBaseArrangementRow ? new HBox() : new VBox();
    Pane common = (style.commonPart.arrangement == ChildArrangement.ROW) ? new HBox() : new VBox();
    Pane specific = new HBox();
    root.getChildren().addAll(shapes.nodeShape, compBase);

    compBase.getChildren().addAll(common, specific);
    compBase.setPickOnBounds(false);
    compBase.widthProperty().addListener(
        (obs, oldVal, newVal) -> sizeChangeNotif.notifySubtreeSizeChanged());
    compBase.heightProperty().addListener(
        (obs, oldVal, newVal) -> sizeChangeNotif.notifySubtreeSizeChanged());
    compBase.setTranslateX(style.paddingLeft);
    compBase.setTranslateY(style.paddingTop);

    common.setPickOnBounds(false);
    common.getStyleClass().add(style.commonPart.cssClass);
    common.getChildren().add(shapes.nextStep);
    common.getChildren().add(shapes.runtimeError);
    common.getChildren().add(shapes.breakpoint);
    common.getChildren().add(shapes.corruption);
    common.getChildren().add(shapes.entryPoint);
    common.getChildren().addAll(components);
    common.getChildren().forEach(child -> {
      child.managedProperty().bind(child.visibleProperty());
      child.visibleProperty().addListener((obs, oldVal, newVal) -> changeCommonPartValidity());
    });
    var pseudoClass = PseudoClass.getPseudoClass(
        isBaseArrangementRow ? BhConstants.Css.Pseudo.ROW : BhConstants.Css.Pseudo.COLUMN);
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

  /**
   * 各種 GUI コンポーネントを配置するペインをまとめたレコード.
   *
   * @param root ノードビューの基底部分となるペイン
   * @param componentBase {@code common} と {@code specific} を乗せるペイン
   * @param common ノードビューが共通で持つコンポーネントを乗せるペイン
   * @param specific ノードビューの種類ごとに異なるコンポーネント (テキストフィールドやコンボボックスなど) を乗せるペイン
   */
  record Panes(
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
   * @param runtimeError ランタイムエラーが発生したノードであることを示す印
   * @param corruption ノードが破損していることを示す印
   */
  record Shapes(
      Polygon nodeShape,
      CompileErrorMark compileError,
      Group breakpoint,
      Group nextStep,
      Group runtimeError,
      Group corruption,
      Group entryPoint) {}


  /** 描画順序. 小さい値ほど手前に描画される. */
  enum ViewOrderOffset {

    COMPILE_ERR_MARK(-4000),
    NODE_BASE(-2000),
    CHILD(-1);

    private final double order;

    ViewOrderOffset(double order) {
      this.order = order;
    }

    public double value() {
      return order;
    }
  }
}
