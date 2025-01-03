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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.derivative.DerivativeBase;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.quadtree.QuadTreeManager;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle.OverlapOption;
import net.seapanda.bunnyhop.utility.Showable;
import net.seapanda.bunnyhop.utility.Utility;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.ViewUtil;
import net.seapanda.bunnyhop.view.bodyshape.BodyShapeBase.BodyShape;
import net.seapanda.bunnyhop.view.connectorshape.ConnectorShape;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle.ConnectorPos;
import net.seapanda.bunnyhop.view.node.part.PrivateTemplateCreationButton;
import net.seapanda.bunnyhop.view.traverse.CallbackInvoker;
import net.seapanda.bunnyhop.view.traverse.NodeViewComponent;
import net.seapanda.bunnyhop.view.traverse.TravelUpCallbackInvoker;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;

/**
 * ノードのビュークラス.
 * <pre>
 * 大きさや色などの変更を行うインタフェースを提供する.
 * 位置変更のインタフェースを提供する.
 * {@link BhNodeView} 同士の親子関係を設定するインタフェースを提供する.
 * イベントハンドラ登録用インタフェースを提供.
 * </pre>
 *
 * @author K.Koike
 */
public abstract class BhNodeView extends Pane implements NodeViewComponent, Showable {

  // 描画順序. 小さい値ほど手前に描画される.
  protected static final double SYNTAX_ERR_MARK_VIEW_ORDER = -1e8;
  protected static final double FRONT_VIEW_ORDER_OFFSET = -2e8;
  protected static final double CHILD_VIEW_ORDER_OFFSET_FROM_PARENT = -20.0;
  protected static final double SHADOW_GROUP_VIEW_ORDER_OFFSET = 10.0;
  /** 描画されるポリゴン. */
  protected final Polygon nodeShape = new Polygon();
  /** 影描画用ポリゴン. */
  protected final Polygon shadowShape = new Polygon();
  /** コンパイルエラーノードであることを示す印. */
  protected final CompileErrorMark compileErrorMark = new CompileErrorMark(0.0, 0.0, 0.0, 0.0);
  /** ノードの見た目のパラメータオブジェクト. */
  protected final BhNodeViewStyle viewStyle;
  private final BhNode model;
  private BhNodeController controller;
  /** このノードビューを保持する親グループ.  このノードビューがルートノードビューの場合は null. */
  protected BhNodeViewGroup parent;

  private final ViewRegionManager viewRegionManager = this.new ViewRegionManager();
  private final ViewTreeManager viewTreeManager = this.new ViewTreeManager();
  private final PositionManager positionManager = this.new PositionManager();
  private final EventManager eventHandlerManager = this.new EventManager();
  private final LookManager lookManager;

  /**
   * このビューのモデルを取得する.
   *
   * @return このビューのモデル
   */
  public abstract Optional<? extends BhNode> getModel();

  /**
   * 子コンポーネントを適切に配置し, このノードビューのサイズを変更する.
   *
   * @return このノードビューのサイズが変わった場合 true.
   */
  protected abstract void arrangeAndResize();

  /**
   * 外部ノードを除くボディ部分の大きさを返す.
   *
   * @param includeCnctr コネクタ部分を含む大きさを返す場合true
   * @return 描画ノードの大きさ
   */
  protected abstract Vec2D getBodySize(boolean includeCnctr);

  /**
   * ボディ部分に末尾までの全外部ノードを加えた大きさを返す.
   *
   * @param includeCnctr コネクタ部分を含む大きさを返す場合 true.
   * @return 描画ノードの大きさ
   */
  protected abstract Vec2D getNodeSizeIncludingOuter(boolean includeCnctr);

  /**
   * コンストラクタ.
   *
   * @param viewStyle ノードの見た目を決めるパラメータオブジェクト
   * @param model ビューが表すモデル
   */
  protected BhNodeView(BhNodeViewStyle viewStyle, DerivativeBase<?> model)
      throws ViewInitializationException {
    this.setPickOnBounds(false);  //nodeShape 部分だけが MouseEvent を拾うように
    this.viewStyle = viewStyle;
    this.model = model;
    lookManager = this.new LookManager(viewStyle.bodyShape);
    shadowShape.setVisible(false);
    shadowShape.setMouseTransparent(true);
    viewTreeManager.addChild(nodeShape);
    lookManager.addCssClass(viewStyle.cssClass);
    lookManager.addCssClass(BhConstants.Css.CLASS_BH_NODE);
    compileErrorMark.getStyleClass().add(BhConstants.Css.CLASS_BH_NODE_COMPILE_ERROR);
    // model がない場合は controller も無いので, nodeShape に対するイベントは, 親に投げる.
    if (model == null) {
      nodeShape.addEventFilter(Event.ANY, this::propagateEvent);
      return;
    }
    if (model.hasPrivateTemplateNodes()) {
      createPrivateTemplateButton(model);
    }
  }

  /**
   * ノードの領域関連の処理用インタフェースを返す.
   *
   * @return ノードの領域関連の処理用インタフェース
   */
  public ViewRegionManager getRegionManager() {
    return viewRegionManager;
  }

  /**
   * View の親子関係の処理用インタフェースを返す.
   *
   * @return View の親子関係の処理用インタフェース
   */
  public ViewTreeManager getTreeManager() {
    return viewTreeManager;
  }

  /**
   * 位置変更/取得用インタフェースを返す.
   *
   * @return 位置変更/取得用インタフェース
   */
  public PositionManager getPositionManager() {
    return positionManager;
  }

  /**
   * イベントハンドラ登録用インタフェースを返す.
   *
   * @return イベントハンドラ登録用インタフェース
   */
  public  EventManager getEventManager() {
    return eventHandlerManager;
  }

  /**
   * 見た目変更用インタフェースを返す.
   *
   * @return 見た目変更用インタフェース
   */
  public LookManager getLookManager() {
    return lookManager;
  }

  /**
   * ノードのサイズが変わったことを伝える.
   * ノードのサイズが変わったときにサブクラスから呼ぶこと.
   */
  public void notifySizeChange() {
    TravelUpCallbackInvoker.invoke(
        nodeView -> nodeView.arrangeAndResize(),
        group -> group.arrangeAndResize(),
        this,
        false);

    BhNodeView root = getTreeManager().getRootView();
    Vec2D pos = root.getPositionManager().getPosOnWorkspace();  //workspace からの相対位置を計算
    root.getPositionManager().setPosOnWorkspace(pos.x, pos.y);
    root.getTreeManager().updateEvenFlag();
    CallbackInvoker.invoke(
        nodeView -> nodeView.getEventManager().invokeOnNodeSizesInTreeChanged(),
        root,
        false);
  }

  /** このビューに対応する BhNode が固定ノードであるか調べる.. */
  public boolean isFixed() {
    if (model == null) {
      return false;
    }
    if (model.getParentConnector() == null) {
      return false;
    }
    return model.getParentConnector().isFixed();
  }

  /**
   * このビューのコントローラを取得する.
   *
   * @return このビューのコントローラ
   */
  public Optional<BhNodeController> getController() {
    return Optional.ofNullable(controller);
  }

  /**
   * このビューのコントローラを設定する.
   *
   * @param controller 設定するコントローラ
   */
  public void setController(BhNodeController controller) {
    this.controller = controller;
  }

  private void createPrivateTemplateButton(BhNode model)
      throws ViewInitializationException {
    var err = new ViewInitializationException("Failed to load Private Template Button.");
    var button = PrivateTemplateCreationButton
        .create(model, viewStyle.privatTemplate)
        .orElseThrow(() -> err);
    getTreeManager().addChild(button);
  }

  private void propagateEvent(Event event) {
    BhNodeView view = getTreeManager().getParentView();
    if (view == null) {
      event.consume();
      return;
    }
    view.getEventManager().propagateEvent(event);
    event.consume();
  }

  /** 見た目を変更する処理を行うクラス. */
  public class LookManager {

    /** 影付きノードリスト. */
    public static final Set<BhNodeView> shadowNodes = 
        Collections.newSetFromMap(new WeakHashMap<BhNodeView, Boolean>());

    private BodyShape bodyShape;
    /** 影が描画されるノードビュー群のルートノードである場合 true. */
    private boolean isShadowRoot = false;

    /**
     * コンストラクタ.
     *
     * @param bodyShape 本体部分の形
     */
    public LookManager(BodyShape bodyShape) {
      this.bodyShape = bodyShape;
    }

    /**
     * CSS の擬似クラスの有効無効を切り替える.
     *
     * @param className 有効/無効を切り替える擬似クラス名
     * @param enable 擬似クラスを有効にする場合true
     */
    public void switchPseudoClassState(String className, boolean enable) {
      if (enable) {
        nodeShape.pseudoClassStateChanged(PseudoClass.getPseudoClass(className), true);
        BhNodeView.this.pseudoClassStateChanged(
            PseudoClass.getPseudoClass(className), true);
      } else {
        nodeShape.pseudoClassStateChanged(PseudoClass.getPseudoClass(className), false);
        BhNodeView.this.pseudoClassStateChanged(
            PseudoClass.getPseudoClass(className), false);
      }
    }

    /**
     * cssクラス名を追加する.
     *
     * @param cssClassName css クラス名
     */
    void addCssClass(String cssClassName) {
      nodeShape.getStyleClass().add(cssClassName);
    }

    /**
     * ノードを形作るポリゴンを更新する.
     *
     * @param drawBody ボディを描画する場合 true
     */
    protected void updatePolygonShape() {
      Vec2D bodySize = getRegionManager().getBodySize(false);
      boolean isFixed = BhNodeView.this.isFixed();
      ConnectorShape cnctrShape =
          isFixed ? viewStyle.connectorShapeFixed.shape : viewStyle.connectorShape.shape;
      ConnectorShape notchShape =
          isFixed ? viewStyle.notchShapeFixed.shape : viewStyle.notchShape.shape;
      nodeShape.getPoints().setAll(
          bodyShape.shape.createVertices(
              bodySize.x,
              bodySize.y,
              cnctrShape,
              viewStyle.connectorPos,
              viewStyle.connectorWidth,
              viewStyle.connectorHeight,
              viewStyle.connectorShift,
              notchShape,
              viewStyle.notchPos,
              viewStyle.notchWidth,
              viewStyle.notchHeight));
      shadowShape.getPoints().setAll(nodeShape.getPoints());
      compileErrorMark.setEndX(bodySize.x);
      compileErrorMark.setEndY(bodySize.y);
    }

    /**
     * このノードビュー以下の位置とサイズを再計算する.
     *
     * <p> 4 分木空間上の位置も更新する. </p>
     */
    public void arrangeAndResize() {
      CallbackInvoker.invoke(
          nodeView -> nodeView.arrangeAndResize(),
          group -> group.arrangeAndResize(),
          BhNodeView.this,
          true);
      BhNodeView root = getTreeManager().getRootView();
      Vec2D pos = root.getPositionManager().getPosOnWorkspace();  //workspace からの相対位置を計算
      root.getPositionManager().setPosOnWorkspace(pos.x, pos.y);
      root.getTreeManager().updateEvenFlag();
      CallbackInvoker.invoke(
          nodeView -> nodeView.getEventManager().invokeOnNodeSizesInTreeChanged(),
          root,
          false);
    }

    /** このノード以下の可視性を変更する. */
    public void setVisible(boolean visible) {
      CallbackInvoker.invoke(view -> view.nodeShape.setVisible(visible), BhNodeView.this, false);
    }

    /** ボディの形をセットする. (再描画は行わない) */
    void setBodyShape(BodyShape bodyShape) {
      this.bodyShape = bodyShape;
    }

    /**
     * コンパイルエラー表示の可視性を切り替える.
     *
     * @param visible コンパイルエラー表示を有効にする場合 true. 無効にする場合 false.
     */
    public void setCompileErrorVisibility(boolean visible) {
      BhNodeView.this.compileErrorMark.setVisible(visible);
    }

    /**
     * コンパイルエラー表示の状態を返す.
     *
     * @return コンパイルエラー表示されている場合 true.
     */
    public boolean isCompileErrorVisible() {
      return BhNodeView.this.compileErrorMark.isVisible();
    }

    /**
     * このノードを起点とする影の表示/非表示を切り替える.
     *
     * @param enable 影を表示する場合 true
     */
    public void showShadow(boolean enable) {
      // 同一ツリーにある描画済みの影を消す
      if (enable) {
        CallbackInvoker.invoke(
            nodeView -> {
              nodeView.shadowShape.setVisible(false);
              nodeView.getLookManager().isShadowRoot = false;
            },
            getTreeManager().getRootView(),
            false);
      }
      isShadowRoot = enable;
      CallbackInvoker.invokeForOuters(
          nodeView -> nodeView.shadowShape.setVisible(enable), BhNodeView.this, false);
      getPositionManager().updateShadowZpos();
    }

    /**
     * このノードが影が描画されるノード群のルートかどうかを返す.
     *
     * @return このノードが影が描画されるノード群のルートである場合 true.
     */
    boolean isShadowRoot() {
      return isShadowRoot;
    }

    /**
     * このノードビューのボディの形状の種類を取得する.
     *
     * @return このノードビューのボディの形状の種類
     */
    BodyShape getBodyShape() {
      return bodyShape;
    }

    /**
     * このノードビューに影を付ける.
     * ただし, 影を付けるノードと同じワークスペースに既に影の付いたノードがあった場合, そのノードの影は消える.
     *
     */
    public void drawShadow() {
      if (getModel().isEmpty()) {
        return;
      }
      var nodesToRemoveShadow = shadowNodes.stream()
          .filter(view ->
              view.getPositionManager().isInSameWorkspace(BhNodeView.this)
              || view.getModel().get().isDeleted())
          .toList();
      nodesToRemoveShadow.forEach(view -> view.getLookManager().showShadow(false));
      shadowNodes.removeAll(nodesToRemoveShadow);
      getLookManager().showShadow(true);
      shadowNodes.add(BhNodeView.this);
    }

    /**
     * ワークスペースに描画されているノードビューの影を消す.
     *
     * @param ws このワークスペースに描画されている影を消す
     */
    public static void removeShadow(Workspace ws) {
      var nodesToRemoveShadow = shadowNodes.stream()
          .filter(view ->
              view.getModel().get().getWorkspace() == ws || view.getModel().get().isDeleted())
          .toList();
      nodesToRemoveShadow.forEach(view -> view.getLookManager().showShadow(false));
      shadowNodes.removeAll(nodesToRemoveShadow);
    }
  }

  /** このノードの画面上での領域に関する処理を行うクラス. */
  public class ViewRegionManager {
    /** ノード全体の範囲. */
    private final QuadTreeRectangle wholeBodyRange =
        new QuadTreeRectangle(0.0, 0.0, 0.0, 0.0, BhNodeView.this);
    /** コネクタ部分の範囲. */
    private final QuadTreeRectangle connectorPartRange
        = new QuadTreeRectangle(0.0, 0.0, 0.0, 0.0, BhNodeView.this);

    /**
     * コネクタ部分同士がこのビューに重なっているビューに対応するモデルを探す.
     *
     * @return コネクタ部分同士がこのビューに重なっているビューに対応するモデルのリスト
     */
    public List<BhNode> searchForOverlappedModels() {
      List<QuadTreeRectangle> overlappedRectList =
          connectorPartRange.searchOverlappedRects(OverlapOption.INTERSECT);
      return overlappedRectList.stream()
          .map(rectangle -> rectangle.<BhNodeView>getUserData().model)
          .filter(model -> model != null)
          .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * ボディとコネクタ部分の領域を保持するQuadTreeRectangleを返す.
     *
     * @return ボディとコネクタ部分の領域を保持するQuadTreeRectangleオブジェクトのペア
     */
    public Rectangles getRegions() {
      return new Rectangles(wholeBodyRange, connectorPartRange);
    }

    private void updateBodyPos(
        double upperLeftX, double upperLeftY, double lowerRightX, double lowerRightY) {
      wholeBodyRange.updatePos(upperLeftX, upperLeftY, lowerRightX, lowerRightY);
    }

    private void updateConnectorPos(
        double upperLeftX, double upperLeftY, double lowerRightX, double lowerRightY) {
      connectorPartRange.updatePos(upperLeftX, upperLeftY, lowerRightX, lowerRightY);
    }

    /**
     * 4 分木空間上での位置を更新する.
     *
     * @param posX 本体部分左上のX位置
     * @param posY 本体部分左上のY位置
     */
    private void updatePosOnQtSpace(double posX, double posY) {
      Vec2D bodySize = getBodySize(false);
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

      if (viewStyle.connectorPos == ConnectorPos.LEFT) {
        cnctrUpperLeftX = posX - (boundsWidth + viewStyle.connectorWidth) / 2.0;
        cnctrUpperLeftY =
            posY - (boundsHeight - viewStyle.connectorHeight) / 2.0 + viewStyle.connectorShift;
        cnctrLowerRightX = cnctrUpperLeftX + boundsWidth;
        cnctrLowerRightY = cnctrUpperLeftY + boundsHeight;

      } else if (viewStyle.connectorPos == ConnectorPos.TOP) {
        cnctrUpperLeftX =
            posX - (boundsWidth - viewStyle.connectorWidth) / 2.0 + viewStyle.connectorShift;
        cnctrUpperLeftY = posY - (boundsHeight + viewStyle.connectorHeight) / 2.0;
        cnctrLowerRightX = cnctrUpperLeftX + boundsWidth;
        cnctrLowerRightY = cnctrUpperLeftY + boundsHeight;
      }
      updateBodyPos(bodyUpperLeftX, bodyUpperLeftY, bodyLowerRightX, bodyLowerRightY);
      updateConnectorPos(cnctrUpperLeftX, cnctrUpperLeftY, cnctrLowerRightX, cnctrLowerRightY);
    }

    /** 4 分木空間からこの {@link BhNodeView} の領域判定オブジェクトを消す. */
    public void removeQuadTreeRect() {
      Rectangles regions = getRegions();
      QuadTreeManager.removeQuadTreeObj(regions.body());
      QuadTreeManager.removeQuadTreeObj(regions.cnctr());
    }

    /**
     * ボディ部分に末尾までの全外部ノードを加えた大きさを返す.
     *
     * @param includeCnctr コネクタ部分を含む大きさを返す場合 true.
     * @return 描画ノードの大きさ
     */
    public Vec2D getNodeSizeIncludingOuters(boolean includeCnctr) {
      return BhNodeView.this.getNodeSizeIncludingOuter(includeCnctr);
    }

    /**
     * ボディ部分の大きさを返す.
     *
     * @param includeCnctr コネクタ部分を含む大きさを返す場合 true
     * @return 描画ノードの大きさ
     */
    public Vec2D getBodySize(boolean includeCnctr) {
      return BhNodeView.this.getBodySize(includeCnctr);
    }

    /** ボディ部分のワークスペース上での範囲を取得する. */
    public BodyRange getBodyRange() {
      QuadTreeRectangle bodyRange = getRegions().body();
      return new BodyRange(bodyRange.getUpperLeftPos(), bodyRange.getLowerRightPos());
    }

    /**
     * このノードのボディの領域が引数のノードのボディ領域と重なっているかどうか調べる.
     *
     * @param view このノードとのボディ部分の重なりを調べるノード
     * @param option 重なり具合を判定するオプション
     * @return このノードのボディの領域が引数のノードのボディと重なっている場合 true.
     * */
    public boolean overlapsWith(BhNodeView view, OverlapOption option) {
      return wholeBodyRange.overlapsWith(view.getRegionManager().wholeBodyRange, option);
    }

    /**
     * ボディとコネクタ部分の領域に対応する {@link QuadTreeRectangle} をまとめたレコード.
     *
     * @param bodyId ボディ部分の矩形領域に対応する {@link QuadTreeRectangle} オブジェクト
     * @param cnctr コネクタ部分の矩形領域に対応する {@link QuadTreeRectangle} オブジェクト
     */
    public record Rectangles(QuadTreeRectangle body, QuadTreeRectangle cnctr) { }

    /**
     * ボディ部分の矩形領域.
     *
     * @param upperLeft 矩形領域の左上のワークスペース上での位置
     * @param lowerRight 矩形領域の右下のワークスペース上での位置
     */
    public record BodyRange(Vec2D upperLeft, Vec2D lowerRight) { }

  }

  /** View の木構造を操作するクラス. */
  public class ViewTreeManager {

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

    /**
     * このノードビューの親グループを取得する.
     *
     * @return このノードビューの親グループ. 存在しない場合は null.
     */
    public BhNodeViewGroup getParentGroup() {
      return parent;
    }

    /**
     * このノードビューの親ノードビューを取得する.
     *
     * @return このビューの親となるビュー.  このビューがルートノードの場合は null.
     */
    public ConnectiveNodeView getParentView() {
      if (parent == null) {
        return null;
      }
      return parent.getParentView();
    }

    /**
     * {@link BhNodeView} の木構造上で, このノードを引数のノードと入れ替える.
     * GUI ツリーからは取り除かない.
     *
     * @param newNode このノードと入れ替えるノード.
     */
    public void replace(BhNodeView newNode) {
      parent.replace(BhNodeView.this, newNode);
      newNode.notifySizeChange();
    }

    /**
     * これ以下のノードビューをGUIツリーから取り除く.
     * {@link BhNodeViewGroup} の木構造からは取り除かない.
     */
    public void removeFromGuiTree() {
      WorkspaceView parentWsView = getWorkspaceView();
      CallbackInvoker.invoke(
          nodeView -> {
            // JDK-8205092 対策. viewOrder を使うとノード削除後に NullPointerException が発生するのを防ぐ.
            nodeView.setMouseTransparent(true);
            removeFromParent(nodeView);
            nodeView.getEventManager().invokeOnRemovedFromWorkspaceView(parentWsView);
          },
          BhNodeView.this,
          false);
    }

    /** 引数で指定したノードビューとそれに付随する描画物 (影, エラーマーク) をそれぞれの親要素から取り除く. */
    private void removeFromParent(BhNodeView nodeView) {
      Parent parent = nodeView.getParent();
      if (parent instanceof Group group) {
        group.getChildren().remove(nodeView);
        group.getChildren().remove(nodeView.compileErrorMark);
      } else if (parent instanceof Pane pane) {
        pane.getChildren().remove(nodeView);
        pane.getChildren().remove(nodeView.compileErrorMark);
      }

      nodeView.shadowShape.setVisible(false);
      Node shadowGroup = nodeView.shadowShape.getParent();
      if (shadowGroup instanceof Group group) {
        group.getChildren().remove(nodeView.shadowShape);
      }
    }

    /**
     * これ以下のノードビューを引数で指定したGUIコンポーネントの子として追加する.
     * parent が Group か Pane のサブクラスでない場合, 追加しない.
     *
     * @param parent 親となるGUIコンポーネント. (nullable)
     */
    public void addToGuiTree(Parent parent) {
      if (!(parent instanceof Group) && !(parent instanceof Pane)) {
        return;
      }
      var nodes = new ArrayList<Node>();
      var shadowShapes = new ArrayList<Node>();
      CallbackInvoker.invoke(
          nodeView -> {
            nodes.add(nodeView);
            nodes.add(nodeView.compileErrorMark);
            shadowShapes.add(nodeView.shadowShape);
            nodeView.shadowShape.setVisible(false);
          },
          BhNodeView.this,
          false);

      // 同一ツリー内の子ノードから子ノードへの移動の場合などに
      // 子要素重複追加エラーが発生するので, 重複ノードを取り除く
      ArrayList<Node> nodesToAdd = filterOutDuplicatedNodes(parent, nodes);

      // JDK-8205092 対策
      nodesToAdd.stream()
          .filter(node ->
              !(node instanceof NoContentNodeView) && !(node instanceof CompileErrorMark))
          .forEach(node -> node.setMouseTransparent(false));

      if (parent instanceof Group) {
        ((Group) parent).getChildren().addAll(nodesToAdd);
      } else if (parent instanceof Pane) {
        ((Pane) parent).getChildren().addAll(nodesToAdd);
      }
      addShadowShapes(parent, shadowShapes);
      getPositionManager().updateZpos();
      nodesToAdd.stream()
          .filter(node -> node instanceof BhNodeView)
          .forEach(node ->
              ((BhNodeView) node).getEventManager().invokeOnAddToWorkspaceView(getWorkspaceView()));
    }

    /**
     * parent の子要素と重複するノードを nodes から除外したノードリストを作って返す.
     *
     * @param parent これの子ノードと重複する要素を調べる
     * @param nodes 重複する要素を調べるノードリスト
     * @return {@code nodes} から {@code parent} の子と重複する要素を取り除いたリスト
     */
    private ArrayList<Node> filterOutDuplicatedNodes(Parent parent, Collection<Node> nodes) {
      Set<Node> childNodes = new HashSet<>(parent.getChildrenUnmodifiable());
      ArrayList<Node> nodesToAdd = nodes.stream()
          .filter(node -> !childNodes.contains(node))
          .collect(Collectors.toCollection(ArrayList::new));
      return nodesToAdd;
    }

    /**
     * 影描画用ポリゴンを影描画用領域に追加し, 影描画を更新する.
     *
     * @param parent 影描画用領域の親ノード
     * @param shadowShapes 追加する影ポリゴン
     */
    private void addShadowShapes(Parent parent, Collection<Node> shadowShapes) {
      Node shadowShapeContainer = parent.lookup("#" + BhConstants.Fxml.ID_NODE_VIEW_SHADOW_PANE);
      if (shadowShapeContainer instanceof Group) {
        Group shadowGroup = (Group) shadowShapeContainer;
        Set<Node> childShapes = new HashSet<>(shadowGroup.getChildren());
        ArrayList<Node> shadowShapesToAdd = shadowShapes.stream()
            .filter(shape -> !childShapes.contains(shape))
            .collect(Collectors.toCollection(ArrayList::new));
        shadowGroup.getChildren().addAll(shadowShapesToAdd);
      }
      // 影描画更新
      TravelUpCallbackInvoker.invoke(
          nodeView -> {
            if (nodeView.getLookManager().isShadowRoot()) {
              nodeView.getLookManager().showShadow(true);
            }
          },
          BhNodeView.this,
          false);
    }

    /** このノード以下の奇偶フラグを更新する. */
    private void updateEvenFlag() {
      CallbackInvoker.invoke(
          view -> updateEvenFlag(view),
          BhNodeView.this,
          false);
    }

    /** {@code view} の奇遇フラグを更新する. */
    private static void updateEvenFlag(BhNodeView view) {
      BhNodeView parentView = view.getTreeManager().getParentView();
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

    /**
     * BhNodeView のペインに子要素を追加する.
     *
     * @param child 追加する要素
     */
    void addChild(Node child) {
      BhNodeView.this.getChildren().add(child);
    }

    /** このノードビューのルートノードビューを返す. */
    BhNodeView getRootView() {
      BhNodeView parent = getParentView();
      if (parent == null) {
        return BhNodeView.this;
      }
      return parent.getTreeManager().getRootView();
    }

    /**
     * このノードビューがルートノードかどうか調べる.
     *
     * @return このノードがルートノードの場合 true
     */
    boolean isRootView() {
      return getParentView() == null;
    }

    /**
     * このノードビューが属している {@link WorkspaceView} を取得する.
     * 見つからない場合は null.
     *
     * @return このノードビューが属している {@link WorkspaceView}
     */
    public WorkspaceView getWorkspaceView() {
      return ViewUtil.getWorkspaceView(BhNodeView.this);
    }
  }

  /** 位置の変更, 取得操作を行うクラス. */
  public class PositionManager {

    private BiConsumer<Double, Double> onAbsPosUpdated;
    private final Vec2D relativePos = new Vec2D(0.0, 0.0);

    /**
     * 親 BhNodeView からの相対位置を指定する.
     *
     * @param posX 親 BhNodeView からの相対位置 X
     * @param posY 親 BhNodeView からの相対位置 Y
     */
    final void setRelativePosFromParent(double posX, double posY) {
      relativePos.x = posX;
      relativePos.y = posY;
    }

    /**
     * 親 {@link BhNodeView} からの相対位置を取得する.
     *
     * @return 親 BhNodeView からの相対位置
     */
    final Vec2D getRelativePosFromParent() {
      return new Vec2D(relativePos.x, relativePos.y);
    }

    /**
     * ワークスペース上での位置を返す.
     *
     * @return ワークスペース上での位置
     */
    public Vec2D getPosOnWorkspace() {
      return new Vec2D(BhNodeView.this.getTranslateX(), BhNodeView.this.getTranslateY());
    }

    /**
     * ノードの絶対位置を更新する.
     * ワークスペース上での位置と4 分木空間上での位置を更新する.
     *
     * @param posX 本体部分左上のワークスペース上でのX位置
     * @param posY 本体部分左上のワークスペース上でのY位置
     */
    public void setPosOnWorkspace(double posX, double posY) {
      viewRegionManager.updatePosOnQtSpace(posX, posY);
      updatePosOnWorkspace(posX, posY);
      if (onAbsPosUpdated != null) {
        onAbsPosUpdated.accept(posX, posY);
      }
      getEventManager().invokeOnMoved(new Vec2D(posX, posY));
    }

    /** GUI部品のワークスペース上での位置を更新する. */
    private void updatePosOnWorkspace(double posX, double posY) {
      BhNodeView.this.setTranslateX(posX);
      BhNodeView.this.setTranslateY(posY);
      BhNodeView.this.compileErrorMark.setTranslateX(posX);
      BhNodeView.this.compileErrorMark.setTranslateY(posY);
      BhNodeView.this.shadowShape.setTranslateX(posX);
      BhNodeView.this.shadowShape.setTranslateY(posY);
    }

    /** このノード以下のノードの Z 位置を更新する. */
    public void updateZpos() {
      CallbackInvoker.invoke(
          nodeView -> updateZpos(nodeView),
          BhNodeView.this,
          false);
    }

    /** {@code view} の Z 位置を更新する. */
    private static void updateZpos(BhNodeView view) {
      view.compileErrorMark.setViewOrder(SYNTAX_ERR_MARK_VIEW_ORDER);
      Parent parent = view.getTreeManager().getParentView();
      if (parent == null) {
        view.setViewOrder(0.0);
        view.getPositionManager().updateShadowZpos();
        return;
      }
      double viewOrder = parent.getViewOrder() + CHILD_VIEW_ORDER_OFFSET_FROM_PARENT;
      view.setViewOrder(viewOrder);
      view.getPositionManager().updateShadowZpos();
    }

    /** このノードが影を描画するノード群のルートノードである場合, 影描画用領域の Z 位置を更新する. */
    private void updateShadowZpos() {
      Parent shadowGroup = shadowShape.getParent();
      if (shadowGroup instanceof Group && getLookManager().isShadowRoot()) {
        ((Group) shadowGroup).setViewOrder(getViewOrder() + SHADOW_GROUP_VIEW_ORDER_OFFSET);
      }
    }

    /**
     * ノードをGUI上で動かす. ワークスペース上の絶対位置 (= 4 分木空間上の位置) も更新する.
     *
     * @param diffX X方向移動量
     * @param diffY Y方向移動量
     */
    public void move(double diffX, double diffY) {
      Vec2D posOnWs = getPosOnWorkspace();
      Vec2D wsSize = ViewUtil.getWorkspaceView(BhNodeView.this).getWorkspaceSize();
      Vec2D newPos = ViewUtil.newPosition(new Vec2D(diffX, diffY), wsSize, posOnWs);
      setPosOnWorkspace(newPos.x, newPos.y);
      getEventManager().invokeOnMoved(newPos);
    }

    /** 絶対位置が更新された時のイベントハンドラをセットする. */
    void setOnAbsPosUpdated(BiConsumer<Double, Double> onAbsPosUpdated) {
      this.onAbsPosUpdated = onAbsPosUpdated;
    }

    /** Z 位置を最前面に移動する. */
    public void toFront() {
      Parent parent = getParent();
      if (parent != null) {
        parent.toFront();
      }
      CallbackInvoker.invoke(
          nodeView -> {
            nodeView.setViewOrder(nodeView.getViewOrder() + FRONT_VIEW_ORDER_OFFSET);
            nodeView.getPositionManager().updateShadowZpos();
            nodeView.compileErrorMark.setViewOrder(
                nodeView.compileErrorMark.getViewOrder() + FRONT_VIEW_ORDER_OFFSET);
          },
          BhNodeView.this,
          false);
    }

    /** {@code other} とこの {@link BhNodeView} が同じワークスペース上にいるか調べる. */
    public boolean isInSameWorkspace(BhNodeView other) {
      if (getModel().isEmpty() || other.getModel().isEmpty()) {
        return false;
      }
      return getModel().get().getWorkspace() == other.getModel().get().getWorkspace();
    }
  }

  /** イベントハンドラの登録, 削除, 呼び出しを行うクラス. */
  public class EventManager {

    /** ノードビューの位置が変わったときのイベントハンドラのリスト. */
    private List<BiConsumer<? super BhNodeView, ? super Vec2D>> onMovedList = new ArrayList<>();
    /** ノードビューをワークスペースビューに追加したときのイベントハンドラのリスト. */
    private List<BiConsumer<? super WorkspaceView, ? super BhNodeView>> onAddToWorkspaceView =
        new ArrayList<>();
    /** ノードビューをワークスペースビューから取り除いた時のイベントハンドラのリスト. */
    private List<BiConsumer<? super WorkspaceView, ? super BhNodeView>> onRemovedFromWorkspaceView =
        new ArrayList<>();
    /** ノードビューツリー内の全ノードのサイズ変更が完了した時のイベントハンドラのリスト. */
    private List<Consumer<? super BhNodeView>> onNodeSizesInTreeChanged = new ArrayList<>();

    /**
     * マウス押下時のイベントハンドラを登録する.
     *
     * @param handler 登録するイベントハンドラ
     */
    public void setOnMousePressed(EventHandler<? super MouseEvent> handler) {
      nodeShape.setOnMousePressed(handler);
    }

    /**
     * マウスドラッグ中のイベントハンドラを登録する.
     *
     * @param handler 登録するイベントハンドラ
     */
    public void setOnMouseDragged(EventHandler<? super MouseEvent> handler) {
      nodeShape.setOnMouseDragged(handler);
    }

    /**
     * マウスドラッグを検出したときのイベントハンドラを登録する.
     *
     * @param handler 登録するイベントハンドラ
     */
    public void setOnDragDetected(EventHandler<? super MouseEvent> handler) {
      nodeShape.setOnDragDetected(handler);
    }

    /**
     * マウスボタンを離した時のイベントハンドラを登録する.
     *
     * @param handler 登録するイベントハンドラ
     */
    public void setOnMouseReleased(EventHandler<? super MouseEvent> handler) {
      nodeShape.setOnMouseReleased(handler);
    }

    /**
     * イベントフィルタを登録する.
     *
     * @param type イベントフィルタが受け取るイベントの種類
     * @param handler 登録するイベントフィルタ
     */
    public <T extends Event> void addEventFilter(
        EventType<T> type, EventHandler<? super T> handler) {
      nodeShape.addEventFilter(type, handler);
    }

    /**
     * イベントフィルタを削除する.
     *
     * @param type イベントフィルタを取り除くイベントの種類
     * @param handler 削除するイベントフィルタ
     */
    public <T extends Event> void removeEventFilter(
        EventType<T> type, EventHandler<? super T> handler) {
      nodeShape.removeEventFilter(type, handler);
    }

    /**
     * この view にイベントを伝える.
     *
     * @param event イベント受信対象に伝えるイベント
     */
    public void propagateEvent(Event event) {
      nodeShape.fireEvent(event);
    }

    /**
     * ノードビューの位置が変わったときのイベントハンドラを追加する.
     * 登録したハンドラは, GUIスレッド上で実行される.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnMoved(BiConsumer<? super BhNodeView, ? super Vec2D> handler) {
      onMovedList.add(handler);
    }

    /**
     * ノードビューの位置が変わったときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnMoved(BiConsumer<? super BhNodeView, ? super Vec2D> handler) {
      onMovedList.remove(handler);
    }

    /**
     * ノードビューをワークスペースビューに追加したときのイベントハンドラを追加する.
     * 登録したハンドラは, GUIスレッド上で実行される.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnAddedToWorkspaceView(
        BiConsumer<? super WorkspaceView, ? super BhNodeView> handler) {
      onAddToWorkspaceView.add(handler);
    }

    /**
     * ノードビューをワークスペースビューに追加したときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnAddedToWorkspaceView(
        BiConsumer<? super WorkspaceView, ? super BhNodeView> handler) {
      onAddToWorkspaceView.remove(handler);
    }

    /**
     * ノードビューをワークスペースビューから取り除いた時のイベントハンドラを追加する.
     * 登録したハンドラは, GUIスレッド上で実行される.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnRemovedFromWorkspaceView(
        BiConsumer<? super WorkspaceView, ? super BhNodeView> handler) {
      onRemovedFromWorkspaceView.add(handler);
    }

    /**
     * ノードビューをワークスペースビューから取り除いた時のイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnRemovedFromWorkspaceView(
        BiConsumer<? super WorkspaceView, ? super BhNodeView> handler) {
      onRemovedFromWorkspaceView.remove(handler);
    }

    /**
     * ノードビューツリー内の全ノードのサイズ変更が完了した時のイベントハンドラを登録する.
     *
     * <pre>
     *   イベントハンドラを登録したノードを含むノードビューツリーの全ノードの更新が終わったとき
     *   {@code handler} が呼ばれる.
     *   登録したハンドラは, GUIスレッド上で実行される.
     * </pre>
     *
     * @param handler 追加するイベントハンドラ.
     */
    public void  addOnNodeSizesInTreeChanged(Consumer<? super BhNodeView> handler) {
      onNodeSizesInTreeChanged.add(handler);
    }

    /**
     * ノードビューツリー内の全ノードのサイズ変更が完了した時のイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ.
     */
    public void  removeOnNodeSizesInTreeChanged(Consumer<? super BhNodeView> handler) {
      onNodeSizesInTreeChanged.remove(handler);
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
      onMovedList.forEach(handler -> handler.accept(BhNodeView.this, posOnWs));
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
      onAddToWorkspaceView.forEach(handler -> handler.accept(wsView, BhNodeView.this));
    }

    /** ノードビューをワークスペースビューから取り除いた時のイベントハンドラを実行する. */
    private void invokeOnRemovedFromWorkspaceView(WorkspaceView wsView) {
      if (!Platform.isFxApplicationThread()) {
        throw new IllegalStateException(
            Utility.getCurrentMethodName() 
            + " - a handler invoked in an inappropriate thread");
      }
      if (wsView == null) {
        return;
      }
      onRemovedFromWorkspaceView.forEach(handler -> handler.accept(wsView, BhNodeView.this));
    }

    /** ノードビューツリー内の全ノードのサイズ変更が完了した時のイベントハンドラを実行する. */
    private void invokeOnNodeSizesInTreeChanged() {
      if (!Platform.isFxApplicationThread()) {
        throw new IllegalStateException(
            Utility.getCurrentMethodName() 
            + " - a handler invoked in an inappropriate thread");
      }
      onNodeSizesInTreeChanged.forEach(handler -> handler.accept(BhNodeView.this));
    }
  }

  private class CompileErrorMark extends Line {
    CompileErrorMark(double startX, double startY, double endX, double endY) {
      super(startX, startY, endX, endY);
      this.setVisible(false);
      this.setMouseTransparent(true);
      this.setViewOrder(SYNTAX_ERR_MARK_VIEW_ORDER);
    }
  }
}
