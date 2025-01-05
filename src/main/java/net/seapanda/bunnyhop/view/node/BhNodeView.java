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
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.control.node.BhNodeController;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.derivative.DerivativeBase;
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
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;
import org.apache.commons.lang3.mutable.MutableDouble;

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
public abstract class BhNodeView implements NodeViewComponent, Showable {

  // 描画順序. 小さい値ほど手前に描画される.
  protected static final double COMPILE_ERR_MARK_VIEW_ORDER_OFFSET = -4000;
  protected static final double NODE_BASE_VIEW_ORDER_OFFSET = -2000;
  protected static final double SHADOW_VIEW_ORDER_OFFSET = 0;
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

  private final RegionManager regionManager = this.new RegionManager();
  private final TreeManager treeManager = this.new TreeManager();
  private final PositionManager positionManager = this.new PositionManager();
  private final EventManager eventManager = this.new EventManager();
  private final LookManager lookManager;

  /**
   * このビューのモデルを取得する.
   *
   * @return このビューのモデル
   */
  public abstract Optional<? extends BhNode> getModel();
  
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
   */
  protected BhNodeView(BhNodeViewStyle viewStyle, DerivativeBase<?> model)
      throws ViewInitializationException {
    this.viewStyle = viewStyle;
    this.model = model;
    nodeBase.setPickOnBounds(false);
    lookManager = this.new LookManager(viewStyle.bodyShape);
    lookManager.addCssClass(viewStyle.cssClass);
    lookManager.addCssClass(BhConstants.Css.CLASS_BH_NODE);
    compileErrorMark.getStyleClass().add(BhConstants.Css.CLASS_BH_NODE_COMPILE_ERROR);
    compileErrorMark.setMouseTransparent(true);
    addComponent(nodeShape);
    // model がない場合は controller も無いので, nodeShape に対するイベントは, 親に投げる.
    if (model == null) {
      nodeShape.addEventFilter(Event.ANY, this::propagateEvent);
      return;
    }
    if (model.hasPrivateTemplateNodes()) {
      Button button = createPrivateTemplateButton(model);
      addComponent(button);
    }
  }

  /**
   * ノードの領域関連の処理用インタフェースを返す.
   *
   * @return ノードの領域関連の処理用インタフェース
   */
  public RegionManager getRegionManager() {
    return regionManager;
  }

  /**
   * View の親子関係の処理用インタフェースを返す.
   *
   * @return View の親子関係の処理用インタフェース
   */
  public TreeManager getTreeManager() {
    return treeManager;
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
    return eventManager;
  }

  /**
   * 見た目変更用インタフェースを返す.
   *
   * @return 見た目変更用インタフェース
   */
  public LookManager getLookManager() {
    return lookManager;
  }

  /** {@code other} とこの {@link BhNodeView} が同じワークスペース上にいるか調べる. */
  public boolean isInSameWorkspaceWith(BhNodeView other) {
    if (getModel().isEmpty() || other.getModel().isEmpty()) {
      return false;
    }
    return getModel().get().getWorkspace() == other.getModel().get().getWorkspace();
  }

  /**
   * このノードビューがマウスイベントを受け付けるかどうかを設定する.
   *
   * @param value true の場合, このノードビューがマウスイベントを受けなくなる.
   *              false の場合, このノードビューがマウスイベントを受けるようになる.
   */
  public void setMouseTransparent(boolean value) {
    nodeBase.setMouseTransparent(value);
  }

  /**
   * このビューに GUI コンポーネントを追加する.
   *
   * @param node 追加するコンポーネント.
   */
  void addComponent(Node node) {
    nodeBase.getChildren().add(node);
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

  /**
   * このノードビューが属している {@link WorkspaceView} を取得する.
   * 見つからない場合は null.
   *
   * @return このノードビューが属している {@link WorkspaceView}
   */
  public WorkspaceView getWorkspaceView() {
    return ViewUtil.getWorkspaceView(nodeBase);
  }

  private Button createPrivateTemplateButton(BhNode model) throws ViewInitializationException {
    var err = new ViewInitializationException("Failed to load Private Template Button.");
    return PrivateTemplateCreationButton
        .create(model, viewStyle.privatTemplate)
        .orElseThrow(() -> err);
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
    public LookManager(BodyShape bodyShape) {
      this.fnGetBodyShape = () -> bodyShape;
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
        nodeBase.pseudoClassStateChanged(PseudoClass.getPseudoClass(className), true);
      } else {
        nodeShape.pseudoClassStateChanged(PseudoClass.getPseudoClass(className), false);
        nodeBase.pseudoClassStateChanged(PseudoClass.getPseudoClass(className), false);
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
     * @return ポリゴンの大きさが変化した場合 true.
     */
    private boolean updatePolygonShape() {
      Vec2D bodySize = getRegionManager().getNodeSize(false);
      if (currentPolygonSize.equals(bodySize)) {
        return false;
      }
      boolean isFixed = BhNodeView.this.isFixed();
      ConnectorShape cnctrShape =
          isFixed ? viewStyle.connectorShapeFixed.shape : viewStyle.connectorShape.shape;
      ConnectorShape notchShape =
          isFixed ? viewStyle.notchShapeFixed.shape : viewStyle.notchShape.shape;
          
      nodeShape.getPoints().setAll(
          getBodyShape().shape.createVertices(
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
      compileErrorMark.setEndX(bodySize.x);
      compileErrorMark.setEndY(bodySize.y);
      currentPolygonSize.set(bodySize);
      return true;
    }

    /** このノードビュー以下のノードのワークスペース上の位置, 四分木空間上の位置, および配置を更新する. */
    public void arrange() {
      updateChildRelativePos();
      Vec2D pos = getPositionManager().getPosOnWorkspace();
      getPositionManager().setTreePosOnWorkspace(pos.x, pos.y);
      CallbackInvoker.invoke(
          nodeView -> {
            boolean isSizeChanged = nodeView.getLookManager().updatePolygonShape();
            if (isSizeChanged) {
              nodeView.getEventManager().invokeOnNodeSizeChanged();
            }
          },
          BhNodeView.this);
      getTreeManager().updateEvenFlag();
    }

    /**
     * このノードビュー以下のノードのワークスペース上の位置, 四分木空間上の位置, および配置の更新を
     * UI スレッドの処理としてキューイングする.
     *
     * <p>
     * 大きなノードツリーをワークスペースに初めて配置したときに, 複数の子要素がサイズの更新を要求する.
     * それらの更新要求を全てまとめて処理するために本メソッドを用意した.
     * </p>
     */
    public void requestArrangement() {
      if (!isArrangementRequested) {
        Platform.runLater(() -> {
          arrange();
          isArrangementRequested = false;
        });
        isArrangementRequested = true;
      }
    }

    /** このノード以下の可視性を変更する. */
    public void setVisible(boolean visible) {
      CallbackInvoker.invoke(
          view -> view.nodeShape.setVisible(visible),
          BhNodeView.this);
    }

    /** ボディの形をセットする関数を設定する. */
    void setBodyShapeGetter(Supplier<BodyShape> fnGetBodyShape) {
      if (fnGetBodyShape != null) {
        this.fnGetBodyShape = fnGetBodyShape;
      }
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
     * このノードビューのボディの形状の種類を取得する.
     *
     * @return このノードビューのボディの形状の種類
     */
    BodyShape getBodyShape() {
      return fnGetBodyShape.get();
    }

    /**
     * このノードビューとそれから辿れる外部ノードに影を付ける.
     *
     * @param onlyOuter 外部ノードのみを辿って影を付ける場合 true.
     *                  内部ノードと外部ノードを辿って影を付ける場合 false.
     */
    public void showShadow(boolean onlyOuter) {
      if (onlyOuter) {
        CallbackInvoker.invokeForOuters(
            nodeView -> nodeView.getLookManager().switchPseudoClassState(
                BhConstants.Css.PSEUDO_SHADOW, true),
            BhNodeView.this);
      } else {
        CallbackInvoker.invoke(
            nodeView -> nodeView.getLookManager().switchPseudoClassState(
              BhConstants.Css.PSEUDO_SHADOW, true),
            BhNodeView.this);
      }
    }

    /**
     * このノードビューとそれから辿れるノードに影を消す.
     *
     * @param onlyOuter 外部ノードのみを辿って影を消す場合 true.
     *                  内部ノードと外部ノードを辿って影を消す場合 false.
     */
    public void hideShadow(boolean onlyOuter) {
      if (onlyOuter) {
        CallbackInvoker.invokeForOuters(
            nodeView -> nodeView.getLookManager().switchPseudoClassState(
                BhConstants.Css.PSEUDO_SHADOW, false),
            BhNodeView.this);
      } else {
        CallbackInvoker.invoke(
            nodeView -> nodeView.getLookManager().switchPseudoClassState(
              BhConstants.Css.PSEUDO_SHADOW, false),
            BhNodeView.this);
      }
    }
  }

  /** このノードの画面上での領域に関する処理を行うクラス. */
  public class RegionManager {
    /** ノード全体の範囲. */
    private final QuadTreeRectangle wholeBodyRange =
        new QuadTreeRectangle(0.0, 0.0, 0.0, 0.0, BhNodeView.this);
    /** コネクタ部分の範囲. */
    private final QuadTreeRectangle cnctrRange
        = new QuadTreeRectangle(0.0, 0.0, 0.0, 0.0, BhNodeView.this);

    /**
     * コネクタ部分同士がこのビューに重なっているビューに対応するモデルを探す.
     *
     * @return コネクタ部分同士がこのビューに重なっているビューに対応するモデルのリスト
     */
    public List<BhNode> searchForOverlappedModels() {
      List<QuadTreeRectangle> overlappedRectList =
          cnctrRange.searchOverlappedRects(OverlapOption.INTERSECT);
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
      return new Rectangles(wholeBodyRange, cnctrRange);
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
      wholeBodyRange.setPos(bodyUpperLeftX, bodyUpperLeftY, bodyLowerRightX, bodyLowerRightY);
      cnctrRange.setPos(cnctrUpperLeftX, cnctrUpperLeftY, cnctrLowerRightX, cnctrLowerRightY);
    }

    /** 4 分木空間からこの {@link BhNodeView} の領域判定オブジェクトを消す. */
    public void removeQuadTreeRect() {
      Rectangles regions = getRegions();
      QuadTreeManager.removeQuadTreeObj(regions.body());
      QuadTreeManager.removeQuadTreeObj(regions.cnctr());
    }

    /**
     * このノードに末尾までの全外部ノードを加えた大きさを返す.
     *
     * @param includeCnctr コネクタ部分を含む大きさを返す場合 true.
     * @return 描画ノードの大きさ
     */
    public Vec2D getNodeTreeSize(boolean includeCnctr) {
      return BhNodeView.this.getNodeTreeSize(includeCnctr);
    }

    /**
     * ノードの大きさを返す.
     *
     * @param includeCnctr コネクタ部分を含む大きさを返す場合 true
     * @return 描画ノードの大きさ
     */
    public Vec2D getNodeSize(boolean includeCnctr) {
      return BhNodeView.this.getNodeSize(includeCnctr);
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
  public class TreeManager {

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
      if (parent == null || newNode == null || BhNodeView.this == newNode) {
        return;
      }
      parent.replace(BhNodeView.this, newNode);
      BhNodeViewGroup group = newNode.getTreeManager().getParentGroup();
      if (group != null) {
        group.notifyChildSizeChanged();
      }
      WorkspaceView wsView = newNode.getWorkspaceView();
      if (wsView != null) {
        wsView.moveToFront(newNode);
      } else {
        // ノード選択ビュー用の処理
        double posZ = BhNodeView.this.getPositionManager().getZpos();
        newNode.getPositionManager().setTreeZpos(posZ);
      }
      getEventManager().invokeOnNodeReplaced(newNode);
    }

    /**
     * これ以下のノードビューをGUIツリーから取り除く.
     * {@link BhNodeViewGroup} の木構造からは取り除かない.
     */
    public void removeFromGuiTree() {
      WorkspaceView wsView = getWorkspaceView();
      // JDK-8205092 対策. view order を使うとノード削除後に NullPointerException が発生するのを防ぐ.
      nodeBase.setMouseTransparent(true);
      removeComponentsFromParent();
      getEventManager().invokeOnRemovedFromWorkspaceView(wsView);
      CallbackInvoker.invokeForGroups(
          group -> group.removePseudoViewFromGuiTree(),
          BhNodeView.this);
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

    /**
     * このノードビューを {@link parent} に子として追加する.
     *
     * @param parent 親となる GUI コンポーネント.
     */
    public void addToGuiTree(Group parent) {
      if (parent == null) {
        return;
      }
      // JDK-8205092 対策
      nodeBase.setMouseTransparent(false);
      addComponentsToParent(parent);
      getEventManager().invokeOnAddToWorkspaceView(getWorkspaceView());
      CallbackInvoker.invokeForGroups(
          group -> group.addPseudoViewToGuiTree(parent),
          BhNodeView.this);
    }

    /**
     * このノードビューを {@link parent} に子として追加する.
     *
     * @param parent 親となる GUI コンポーネント.
     */
    public void addToGuiTree(Pane parent) {
      if (parent == null) {
        return;
      }
      // JDK-8205092 対策
      nodeBase.setMouseTransparent(false);
      addComponentsToParent(parent);
      getEventManager().invokeOnAddToWorkspaceView(getWorkspaceView());
      CallbackInvoker.invokeForGroups(
          group -> group.addPseudoViewToGuiTree(parent),
          BhNodeView.this);
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
      CallbackInvoker.invoke(
          view -> updateEvenFlag(view),
          BhNodeView.this);
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

    /** このノードビューのルートノードビューを返す. */
    public BhNodeView getRootView() {
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
    public boolean isRootView() {
      return getParentView() == null;
    }

    /**
     * このノードを保持する GUI コンポーネントを取得する.
     *
     * @return このノード保持する GUI コンポーネント.
     */
    public Parent getParentGuiComponent() {
      return nodeBase.getParent();
    }
  }

  /** 位置の変更, 取得操作を行うクラス. */
  public class PositionManager {

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

    /**
     * ワークスペース上での位置を返す.
     *
     * @return ワークスペース上での位置
     */
    public Vec2D getPosOnWorkspace() {
      return new Vec2D(posOnWorkspace);
    }

    /**
     * このノードビュー以下のノードビューのワークスペースの上での位置と 4 分木空間上での位置を更新する.
     *
     * @param posX 本体部分左上のワークスペース上での X 位置
     * @param posY 本体部分左上のワークスペース上での Y 位置
     */
    public void setTreePosOnWorkspace(double posX, double posY) {
      BhNodeView.this.updatePosOnWorkspace(posX, posY);
      CallbackInvoker.invoke(
          nodeView -> {
            Vec2D pos = nodeView.getPositionManager().getPosOnWorkspace();
            nodeView.getPositionManager().setComponentPosOnWorkspace(pos.x, pos.y);
            nodeView.getRegionManager().setPosOnQuadTreeSpace(pos.x, pos.y);
          },
          BhNodeView.this);
      CallbackInvoker.invoke(
          nodeView -> {
            Vec2D pos = nodeView.getPositionManager().getPosOnWorkspace();
            nodeView.getEventManager().invokeOnMoved(new Vec2D(pos.x, pos.y));
          },
          BhNodeView.this);          
    }

    /** GUI 部品のワークスペース上での位置を更新する. */
    private void setComponentPosOnWorkspace(double posX, double posY) {
      nodeBase.setTranslateX(posX);
      nodeBase.setTranslateY(posY);
      compileErrorMark.setTranslateX(posX);
      compileErrorMark.setTranslateY(posY);
    }

    /**
     * このノードビュー以下のノードビューの Z 位置を設定する.
     *
     * @param pos このノードビューの Z 位置
     */
    public void setTreeZpos(double pos) {
      MutableDouble posZ = new MutableDouble(pos);
      CallbackInvoker.invoke(
          nodeView -> {
            updateZpos(nodeView, posZ.getValue());
            posZ.subtract(1);
          },
          BhNodeView.this);
    }

    /** {@code view} の Z 位置を更新する. */
    private static void updateZpos(BhNodeView view, double posZ) {
      view.compileErrorMark.setViewOrder(posZ + COMPILE_ERR_MARK_VIEW_ORDER_OFFSET);
      view.nodeBase.setViewOrder(posZ + NODE_BASE_VIEW_ORDER_OFFSET);
    }

    /**
     * このノードビューの Z 位置を取得する.
     *
     * @return このノードビューの Z 位置
     */
    public double getZpos() {
      return nodeBase.getViewOrder();
    }

    /**
     * このノードビュー以下のノードビューをワークスペースビューからはみ出さないように動かす.
     *
     * @param diffX X 方向移動量
     * @param diffY Y 方向移動量
     */
    public void move(double diffX, double diffY) {
      WorkspaceView wsView = getWorkspaceView();
      if (wsView == null) {
        return;
      }
      Vec2D posOnWs = getPosOnWorkspace();
      Vec2D wsSize = wsView.getWorkspaceSize();
      Vec2D newPos = ViewUtil.newPosition(new Vec2D(diffX, diffY), wsSize, posOnWs);
      setTreePosOnWorkspace(newPos.x, newPos.y);
    }

    /**
     * シーンの座標空間の位置 {@code pos} をこのノードビューのローカル座標空間の位置に変換する.
     *
     * @param pos シーンの座標空間の位置
     * @return {@code pos} のローカル座標空間における位置.
     */
    public Vec2D sceneToLocal(Vec2D pos) {
      var localPos = nodeBase.sceneToLocal(pos.x, pos.y);
      return new Vec2D(localPos.getX(), localPos.getY());
    }

    /**
     * このノードビューのローカル座標空間の位置 {@code pos} をシーンの座標空間の位置に変換する.
     *
     * @param pos このノードビューのローカル座標空間の位置
     * @return {@code pos} のシーンの座標空間の位置
     */
    public Vec2D localToScene(Vec2D pos) {
      var scenePos = nodeBase.localToScene(pos.x, pos.y);
      return new Vec2D(scenePos.getX(), scenePos.getY());
    }
  }

  /** イベントハンドラの登録, 削除, 呼び出しを行うクラス. */
  public class EventManager {

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

    /**
     * マウス押下ときのイベントハンドラを登録する.
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
     * マウスボタンを離したときのイベントハンドラを登録する.
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
      onMovedList.addLast(handler);
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
      onAddToWorkspaceView.addLast(handler);
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
     * ノードビューをワークスペースビューから取り除いたときのイベントハンドラを追加する.
     * 登録したハンドラは, GUIスレッド上で実行される.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnRemovedFromWorkspaceView(
        BiConsumer<? super WorkspaceView, ? super BhNodeView> handler) {
      onRemovedFromWorkspaceView.addLast(handler);
    }

    /**
     * ノードビューをワークスペースビューから取り除いたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnRemovedFromWorkspaceView(
        BiConsumer<? super WorkspaceView, ? super BhNodeView> handler) {
      onRemovedFromWorkspaceView.remove(handler);
    }

    /**
     * ノードのサイズが変更されたときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ.
     */
    public void addOnNodeSizeChanged(Consumer<? super BhNodeView> handler) {
      onNodeSizeChanged.addLast(handler);
    }

    /**
     * ノードのサイズが変更されたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ.
     */
    public void removeOnNodeSizeChanged(Consumer<? super BhNodeView> handler) {
      onNodeSizeChanged.remove(handler);
    }

    /**
     * ノードビューが入れ替わったときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ.
     */
    public void addOnNodeReplaced(BiConsumer<? super BhNodeView, ? super BhNodeView> handler) {
      onNodeReplaced.addLast(handler);
    }

    /**
     * ノードビューが入れ替わったときのイベントハンドラを削除する.
     *
     * <pre>
     * イベントハンドラの第 1 引数 : このノードビュー.
     * イベントハンドラの第 2 引数 : このノードビューの代わりに接続されたノードビュー.
     * </pre>
     *
     * @param handler 削除するイベントハンドラ.
     */
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
      onRemovedFromWorkspaceView.forEach(handler -> handler.accept(wsView, BhNodeView.this));
    }

    /** ノードビューのサイズが変更されたときのイベントハンドラを実行する. */
    private void invokeOnNodeSizeChanged() {
      if (!Platform.isFxApplicationThread()) {
        throw new IllegalStateException(
            Utility.getCurrentMethodName() 
            + " - a handler invoked in an inappropriate thread");
      }
      onNodeSizeChanged.forEach(handler -> handler.accept(BhNodeView.this));
    }

    /** ノードビューが入れ替わったときのイベントハンドラを実行する. */
    private void invokeOnNodeReplaced(BhNodeView newView) {
      if (!Platform.isFxApplicationThread()) {
        throw new IllegalStateException(
            Utility.getCurrentMethodName() 
            + " - a handler invoked in an inappropriate thread");
      }
      onNodeReplaced.forEach(handler -> handler.accept(BhNodeView.this, newView));
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
