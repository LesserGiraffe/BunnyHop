/**
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.Showable;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.imitation.Imitatable;
import net.seapanda.bunnyhop.model.node.imitation.ImitationBase;
import net.seapanda.bunnyhop.quadtree.QuadTreeManager;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle.OVERLAP_OPTION;
import net.seapanda.bunnyhop.view.ViewHelper;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.bodyshape.BodyShape.BODY_SHAPE;
import net.seapanda.bunnyhop.view.connectorshape.ConnectorShape;
import net.seapanda.bunnyhop.view.connectorshape.ConnectorShape.CNCTR_SHAPE;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle;
import net.seapanda.bunnyhop.view.node.part.BhNodeViewStyle.CNCTR_POS;
import net.seapanda.bunnyhop.view.node.part.ImitationCreationButton;
import net.seapanda.bunnyhop.view.node.part.PrivateTemplateCreationButton;
import net.seapanda.bunnyhop.viewprocessor.CallbackInvoker;
import net.seapanda.bunnyhop.viewprocessor.NodeViewComponent;
import net.seapanda.bunnyhop.viewprocessor.TravelUpCallbackInvoker;

/**
 * ノードのビュークラス <br>
 * 大きさや色などの変更を行うインタフェースを提供 <br>
 * 位置変更のインタフェースを提供 <br>
 * View ノード同士の親子関係を処理するインタフェースを提供 <br>
 * イベントハンドラ登録用インタフェースを提供
 * @author K.Koike
 * */
public abstract class BhNodeView extends Pane implements NodeViewComponent, Showable {

  // 描画順序. 小さい値ほど手前に描画される.
  private final double SYNTAX_ERR_MARK_VIEW_ORDER = -1e8;
  private final double FRONT_VIEW_ORDER_OFFSET = -2e8;
  private final double CHILD_VIEW_ORDER_OFFSET_FROM_PARENT = -20.0;
  private final double SHADOW_GROUP_VIEW_ORDER_OFFSET = 10.0;

  protected final Polygon nodeShape = new Polygon();  //!< 描画されるポリゴン
  protected final Polygon shadowShape = new Polygon();  //!< 影描画用ポリゴン
  protected final SyntaxErrorMark syntaxErrorMark = this.new SyntaxErrorMark(0.0, 0.0, 0.0, 0.0);  //!< 構文エラーノードであることを示す印
  protected final BhNodeViewStyle viewStyle;  //!< ノードの見た目のパラメータオブジェクト
  private final BhNode model;
  /** このノードビューを保持する親グループ.  このノードビューがルートノードビューの場合は null. */
  protected BhNodeViewGroup parent;

  private final ViewRegionManager viewRegionManager = this.new ViewRegionManager();
  private final ViewTreeManager viewTreeManager = this.new ViewTreeManager();
  private final PositionManager positionManager = this.new PositionManager();
  private final EventManager eventHandlerManager = this.new EventManager();
  private final AppearanceManager appearanceManager;

  /**
   * このビューのモデルであるBhNodeを取得する
   * @return このビューのモデルであるBhNode
   */
  abstract public BhNode getModel();

  /**
   * 子コンポーネントを適切に配置し, このノードビューのサイズを変更する.
   * @return このノードビューのサイズが変わった場合 true.
   */
  abstract protected void arrangeAndResize();

  /**
   * 外部ノードを除くボディ部分の大きさを返す
   * @param includeCnctr コネクタ部分を含む大きさを返す場合true
   * @return 描画ノードの大きさ
   */
  abstract protected Vec2D getBodySize(boolean includeCnctr);

  /**
   * ボディ部分に末尾までの全外部ノードを加えた大きさを返す
   * @param includeCnctr コネクタ部分を含む大きさを返す場合 true.
   * @return 描画ノードの大きさ
   */
  abstract protected Vec2D getNodeSizeIncludingOuter(boolean includeCnctr);

  /**
   * コンストラクタ
   * @param viewStyle ノードの見た目を決めるパラメータオブジェクト
   * @param model ビューが表すモデル
   * */
  protected BhNodeView(BhNodeViewStyle viewStyle, ImitationBase<?> model)
    throws ViewInitializationException {

    this.setPickOnBounds(false);  //nodeShape 部分だけがMouseEvent を拾うように
    this.viewStyle = viewStyle;
    this.model = model;
    appearanceManager = this.new AppearanceManager(viewStyle.bodyShape, viewStyle.notchShape);
    shadowShape.setVisible(false);
    shadowShape.setMouseTransparent(true);
    viewTreeManager.addChild(nodeShape);
    appearanceManager.addCssClass(viewStyle.cssClass);
    appearanceManager.addCssClass(BhParams.CSS.CLASS_BHNODE);
    if (model.canCreateImitManually)
      createImitButton(model);
    if (model.hasPrivateTemplateNodes())
      createPrivateTemplateButton(model);
    
  }

  /**
   * ノードの領域関連の処理用インタフェースを返す
   * @return ノードの領域関連の処理用インタフェース
   * */
  public ViewRegionManager getRegionManager() {
    return viewRegionManager;
  }

  /**
   * View の親子関係の処理用インタフェースを返す
   * @return View の親子関係の処理用インタフェース
   * */
  public ViewTreeManager getTreeManager() {
    return viewTreeManager;
  }

  /**
   * 位置変更/取得用インタフェースを返す
   * @return 位置変更/取得用インタフェース
   * */
  public PositionManager getPositionManager() {
    return positionManager;
  }

  /**
   * イベントハンドラ登録用インタフェースを返す
   * @return イベントハンドラ登録用インタフェース
   * */
  public  EventManager getEventManager() {
    return eventHandlerManager;
  }

  /**
   * 見た目変更用インタフェースを返す
   * @return 見た目変更用インタフェース
   * */
  public AppearanceManager getAppearanceManager() {
    return appearanceManager;
  }

  /**
   * ノードのサイズが変わったことを伝える.
   *
   * <p>ノードのサイズが変わったときにサブクラスから呼ぶこと.
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
    root.getTreeManager().updateEvenFlg();
    CallbackInvoker.invoke(
      nodeView -> nodeView.getEventManager().invokeOnNodeSizesInTreeChanged(),
      root,
      false);
  }

  private void createImitButton(Imitatable model)
    throws ViewInitializationException {

    var err = new ViewInitializationException("Failed To load the Imitation Creation Button.");
    var button = ImitationCreationButton
      .create(model, viewStyle.imitation)
      .orElseThrow(() -> err);
    getTreeManager().addChild(button);
  }

  private void createPrivateTemplateButton(BhNode model)
    throws ViewInitializationException {
    
    var err = new ViewInitializationException("Failed To load the Private Template Button.");
    var button = PrivateTemplateCreationButton
      .create(model, viewStyle.privatTemplate)
      .orElseThrow(() -> err);
    getTreeManager().addChild(button);
  }
  
  /**
   * 見た目を変更する処理を行うクラス
   * */
  public class AppearanceManager {

    private final ConnectorShape notch;  //!< 切り欠き部分の形を表すオブジェクト
    private BODY_SHAPE bodyShape;
    private boolean isShadowRoot = false;  //!< 影が描画されるノードビュー群のルートノードである場合 true

    /**
     * コンストラクタ
     * @param bodyShape 本体の形
     * @param notchShape 切り欠きの形
     * */
    public AppearanceManager(BODY_SHAPE bodyShape, CNCTR_SHAPE notchShape) {

      notch = notchShape.SHAPE;
      this.bodyShape = bodyShape;
    }

    /**
     * CSSの擬似クラスの有効無効を切り替える
     * @param activate 擬似クラスを有効にする場合true
     * @param pseudoClassName 有効/無効を切り替える擬似クラス名
     * */
    public void switchPseudoClassActivation(boolean activate, String pseudoClassName) {

      if (activate) {
        nodeShape.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), true);
        BhNodeView.this.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), true);
      }
      else {
        nodeShape.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), false);
        BhNodeView.this.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), false);
      }
    }

    /**
     * cssクラス名を追加する
     * @param cssClassName cssクラス名
     */
    void addCssClass(String cssClassName) {
      nodeShape.getStyleClass().add(cssClassName);
      BhNodeView.this.getStyleClass().add(cssClassName + BhParams.CSS.CLASS_SUFFIX_PANE);
      BhNodeView.this.syntaxErrorMark.getStyleClass().add(cssClassName + BhParams.CSS.CLASS_SUFFIX_SYNTAX_ERROR);
    }

    /**
     * ノードを形作るポリゴンを更新する
     * @param drawBody ボディを描画する場合 true
     * */
    protected void updatePolygonShape() {

      Vec2D bodySize = getRegionManager().getBodySize(false);
      nodeShape.getPoints().setAll(
        bodyShape.SHAPE.createVertices(
          bodySize.x,
          bodySize.y,
          viewStyle.connectorShape.SHAPE,
          viewStyle.connectorPos,
          viewStyle.connectorWidth,
          viewStyle.connectorHeight,
          viewStyle.connectorShift,
          notch,
          viewStyle.notchPos,
          viewStyle.notchWidth,
          viewStyle.notchHeight));
      shadowShape.getPoints().setAll(nodeShape.getPoints());
      syntaxErrorMark.setEndX(bodySize.x);
      syntaxErrorMark.setEndY(bodySize.y);
    }

    /**
     * このノードビュー以下のノードビューを適切な位置に配置してサイズを変更する.
     *
     * <p> 4分木空間上の位置も更新する.
     * @param child 形状が変わった子ノードを含むグループ. このノード自体の形状が変わった場合 null を指定する.
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
      root.getTreeManager().updateEvenFlg();
      CallbackInvoker.invoke(
        nodeView -> nodeView.getEventManager().invokeOnNodeSizesInTreeChanged(),
        root,
        false);
    }

    /**
     * このノード以下の可視性を変更する.
     */
    public void setVisible(boolean visible) {
      CallbackInvoker.invoke(view -> view.nodeShape.setVisible(visible), BhNodeView.this, false);
    }

    /**
     * ボディの形をセットする. (再描画は行わない)
     */
    void setBodyShape(BODY_SHAPE bodyShape) {
      this.bodyShape = bodyShape;
    }

    /**
     * 構文エラー表示の可視性を切り替える
     * @param visible 構文エラー表示を有効にする場合 true. 無効にする場合 false.
     */
    public void setSytaxErrorVisibility(boolean visible) {
      BhNodeView.this.syntaxErrorMark.setVisible(visible);
    }

    /**
     * 構文エラー表示の状態を返す
     * @return 構文エラー表示されている場合 true.
     */
    public boolean isSyntaxErrorVisible() {
      return BhNodeView.this.syntaxErrorMark.isVisible();
    }

    /**
     * ノードビューの選択表示の有効/無効を切り替える
     * @param enable 選択表示を有効化する場合 true
     */
    public void select(boolean enable) {
      switchPseudoClassActivation(enable, BhParams.CSS.PSEUDO_SELECTED);
    }

    /**
     * このノードを起点とする影の表示/非表示を切り替える.
     * @param enable 影を表示する場合 true
     */
    public void showShadow(boolean enable) {

      // 同一ツリーにある描画済みの影を消す
      if (enable) {
        CallbackInvoker.invoke(
          nodeView -> {
            nodeView.shadowShape.setVisible(false);
            nodeView.getAppearanceManager().isShadowRoot = false;
          },
          getTreeManager().getRootView(),
          false);
      }

      isShadowRoot = enable;
      CallbackInvoker.invokeForOuters(
        nodeView -> nodeView.shadowShape.setVisible(enable), BhNodeView.this, false);
      getPositionManager().updateShadowZPos();
    }

    /**
     * このノードが影が描画されるノード群のルートかどうかを返す.
     * @return このノードが影が描画されるノード群のルートである場合 true.
     */
    boolean isShadowRoot() {
      return isShadowRoot;
    }

    /**
     * このノードビューのボディの形状の種類を取得する.
     * @return このノードビューのボディの形状の種類
     */
    BODY_SHAPE getBodyShape() {
      return bodyShape;
    }
  }

  /**
   * このノードの画面上での領域に関する処理を行うクラス
   * */
  public class ViewRegionManager {

    private final QuadTreeRectangle wholeBodyRange = new QuadTreeRectangle(0.0, 0.0, 0.0, 0.0, BhNodeView.this);    //!< ノード全体の範囲
    private final QuadTreeRectangle connectorPartRange = new QuadTreeRectangle(0.0, 0.0, 0.0, 0.0, BhNodeView.this);  //!< コネクタ部分の範囲

    /**
     * コネクタ部分同士がこのビューに重なっているビューに対応するモデルを探す
     * @return コネクタ部分同士がこのビューに重なっているビューに対応するモデルのリスト
     */
    public List<BhNode> searchForOverlappedModels() {

      List<QuadTreeRectangle> overlappedRectList = connectorPartRange.searchOverlappedRects(OVERLAP_OPTION.INTERSECT);
      return overlappedRectList.stream()
          .map(rectangle -> rectangle.<BhNodeView>getRelatedObj().model)
          .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * ボディとコネクタ部分の領域を保持するQuadTreeRectangleを返す
     * @return ボディとコネクタ部分の領域を保持するQuadTreeRectangleオブジェクトのペア
     */
    public Pair<QuadTreeRectangle, QuadTreeRectangle> getRegions() {
      return new Pair<>(wholeBodyRange, connectorPartRange);
    }

    private void updateBodyPos(double upperLeftX, double upperLeftY, double lowerRightX, double lowerRightY) {
      wholeBodyRange.updatePos(upperLeftX, upperLeftY, lowerRightX, lowerRightY);
    }

    private void updateConnectorPos(double upperLeftX, double upperLeftY, double lowerRightX, double lowerRightY) {
      connectorPartRange.updatePos(upperLeftX, upperLeftY, lowerRightX, lowerRightY);
    }

    /**
     * 4分木空間上での位置を更新する.
     * @param posX 本体部分左上のX位置
     * @param posY 本体部分左上のY位置
     */
    private void updatePosOnQTSpace(double posX, double posY) {

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

      if (viewStyle.connectorPos == CNCTR_POS.LEFT) {
        cnctrUpperLeftX = posX - (boundsWidth + viewStyle.connectorWidth) / 2.0;
        cnctrUpperLeftY = posY - (boundsHeight - viewStyle.connectorHeight) / 2.0 + viewStyle.connectorShift;
        cnctrLowerRightX = cnctrUpperLeftX + boundsWidth;
        cnctrLowerRightY = cnctrUpperLeftY + boundsHeight;
      }
      else if (viewStyle.connectorPos == CNCTR_POS.TOP) {
        cnctrUpperLeftX = posX - (boundsWidth - viewStyle.connectorWidth) / 2.0 + viewStyle.connectorShift;
        cnctrUpperLeftY = posY - (boundsHeight + viewStyle.connectorHeight) / 2.0;
        cnctrLowerRightX = cnctrUpperLeftX + boundsWidth;
        cnctrLowerRightY = cnctrUpperLeftY + boundsHeight;
      }
      updateBodyPos(bodyUpperLeftX, bodyUpperLeftY, bodyLowerRightX, bodyLowerRightY);
      updateConnectorPos(cnctrUpperLeftX, cnctrUpperLeftY, cnctrLowerRightX, cnctrLowerRightY);
    }

    /**
     * 4分木空間からこのView以下の領域判定オブジェクトを消す
     */
    public void removeQtRectable() {

      CallbackInvoker.invoke(
        view -> {
          QuadTreeManager.removeQuadTreeObj(view.getRegionManager().connectorPartRange);
          QuadTreeManager.removeQuadTreeObj(view.getRegionManager().wholeBodyRange);
        },
        BhNodeView.this,
        false);
    }

    /**
     * ボディ部分に末尾までの全外部ノードを加えた大きさを返す
     * @param includeCnctr コネクタ部分を含む大きさを返す場合 true.
     * @return 描画ノードの大きさ
     * */
    public Vec2D getNodeSizeIncludingOuter(boolean includeCnctr) {
      return BhNodeView.this.getNodeSizeIncludingOuter(includeCnctr);
    }

    /**
     * 外部ノードを覗くボディ部分の大きさを返す
     * @param includeCnctr コネクタ部分を含む大きさを返す場合true
     * @return 描画ノードの大きさ
     * */
    public Vec2D getBodySize(boolean includeCnctr) {
      return BhNodeView.this.getBodySize(includeCnctr);
    }

    /**
     * このノードのボディの領域が引数のノードのボディ領域と重なっているかどうか調べる. <br>
     * @param view このノードとのボディ部分の重なりを調べるノード
     * @param option 重なり具合を判定するオプション
     * @return このノードのボディの領域が引数のノードのボディと重なっている場合 true.
     * */
    public boolean overlapsWith(BhNodeView view, OVERLAP_OPTION option) {
      return wholeBodyRange.overlapsWith(view.getRegionManager().wholeBodyRange, option);
    }
  }

  /**
   * View の木構造を操作するクラス
   * */
  public class ViewTreeManager {

    private boolean isEven = true;  //!< ルートを0として、階層が偶数であった場合true.
                      //!< ただし, outerノードは親と同階層とする
    /**
     * NodeView の親をセットする
     * @param parentGroup このBhNodeViewを保持するBhNodeViewGroup
     */
    void setParentGroup(BhNodeViewGroup parentGroup) {
      parent = parentGroup;
    }

    /**
     * このノードビューの親グループを取得する.
     * @return このノードビューの親グループ. 存在しない場合は null.
     */
    public BhNodeViewGroup getParentGroup() {
      return parent;
    }

    /**
     * このノードビューの親ノードビューを取得する
     * @return このビューの親となるビュー.  このビューがルートノードの場合は null.
     */
    public BhNodeView getParentView() {

      if (parent == null)
        return null;

      return parent.getParentView();
    }

    /**
     * BhNodeViewの木構造上で, このノードを引数のノードと入れ替える. <br>
     * GUIツリーからは取り除かない.
     * @param newNode
     */
    public void replace(BhNodeView newNode) {

      parent.replace(BhNodeView.this, newNode);
      newNode.notifySizeChange();
    }

    /**
     * これ以下のノードビューをGUIツリーから取り除く
     * BhNodeViewGroup の木構造からは取り除かない
     */
    public void removeFromGUITree() {

      CallbackInvoker.invoke(
        nodeView -> {
          // JDK-8205092 対策. viewOrder を使うとノード削除後に NullPointerException が発生するのを防ぐ.
          nodeView.setMouseTransparent(true);
          removeFromParent(nodeView);
          nodeView.getEventManager().invokeOnRemovedFromWorkspaceView();
        },
        BhNodeView.this,
        false);
    }

    /**
     * 引数で指定したノードビューとそれに付随する描画物 (影, エラーマーク) をそれぞれの親要素から取り除く
     */
    private void removeFromParent(BhNodeView nodeView) {

      Parent parent = nodeView.getParent();
      if (parent instanceof Group) {
        var group = (Group)parent;
        group.getChildren().remove(nodeView);
        group.getChildren().remove(nodeView.syntaxErrorMark);
      }
      else if (parent instanceof Pane) {
        var pane = (Pane)parent;
        pane.getChildren().remove(nodeView);
        pane.getChildren().remove(nodeView.syntaxErrorMark);
      }

      nodeView.shadowShape.setVisible(false);
      Node shadowGroup = nodeView.shadowShape.getParent();
      if (shadowGroup instanceof Group)
        ((Group)shadowGroup).getChildren().remove(nodeView.shadowShape);
    }

    /**
     * これ以下のノードビューを引数で指定したGUIコンポーネントの子として追加する. <br>
     * parent が Group か Pane のサブクラスでない場合, 追加しない.
     * @param parent 親となるGUIコンポーネント. (null可)
     */
    public void addToGUITree(Parent parent) {

      if (!(parent instanceof Group) && !(parent instanceof Pane))
        return;

      var nodes = new ArrayList<Node>();
      var shadowShapes = new ArrayList<Node>();
      CallbackInvoker.invoke(
        nodeView -> {
          nodes.add(nodeView);
          nodes.add(nodeView.syntaxErrorMark);
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
        !(node instanceof NoContentNodeView) &&
        !(node instanceof SyntaxErrorMark))
      .forEach(node -> node.setMouseTransparent(false));

      if (parent instanceof Group)
        ((Group)parent).getChildren().addAll(nodesToAdd);
      else if (parent instanceof Pane)
        ((Pane)parent).getChildren().addAll(nodesToAdd);

      addShadowShapes(parent, shadowShapes);
      getPositionManager().updateZPos();
      nodesToAdd.stream()
      .filter(node -> node instanceof BhNodeView)
      .forEach(node -> ((BhNodeView)node).getEventManager().invokeOnAddToWorkspaceView());
    }

    /**
     * parent の子要素と重複するノードを nodes から除外したノードリストを作って返す.
     * @param parent これの子ノードと重複する要素を調べる
     * @param nodes 重複する要素を調べるノードリスト
     * @return nodes から parent の子と重複する要素を取り除いたリスト
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
     * @param parent 影描画用領域の親ノード
     * @param shadowShapes 追加する影ポリゴン
     */
    private void addShadowShapes(Parent parent, Collection<Node> shadowShapes) {

      Node shadowShapeContainer = parent.lookup("#"+BhParams.Fxml.ID_NODE_VIEW_SHADOW_PANE);
      if (shadowShapeContainer instanceof Group) {
        Group shadowGroup = (Group)shadowShapeContainer;
        Set<Node> childShapes = new HashSet<>(shadowGroup.getChildren());
        ArrayList<Node> shadowShapesToAdd = shadowShapes.stream()
        .filter(shape -> !childShapes.contains(shape))
        .collect(Collectors.toCollection(ArrayList::new));
        shadowGroup.getChildren().addAll(shadowShapesToAdd);
      }

      // 影描画更新
      TravelUpCallbackInvoker.invoke(
        nodeView -> {
          if(nodeView.getAppearanceManager().isShadowRoot())
            nodeView.getAppearanceManager().showShadow(true);
        },
        BhNodeView.this,
        false);
    }

    /**
     * このノード以下の奇偶フラグを更新する
     */
    private void updateEvenFlg() {

      CallbackInvoker.invoke(
        view -> {
          BhNodeView parentView = view.getTreeManager().getParentView();

          if (parentView != null) {
            if (view.parent.inner &&
              !parentView.getAppearanceManager().getBodyShape().equals(BODY_SHAPE.BODY_SHAPE_NONE))
              view.getTreeManager().isEven = !parentView.getTreeManager().isEven;
            else
              view.getTreeManager().isEven = parentView.getTreeManager().isEven;
          }
          else {
            view.getTreeManager().isEven = true;  //ルートはeven
          }
          view.getAppearanceManager().switchPseudoClassActivation(
            view.getTreeManager().isEven, BhParams.CSS.PSEUDO_IS_EVEN);
        },
        BhNodeView.this,
        false);
    }

    /**
     * BhNodeView のペインに子要素を追加する
     * @param child 追加する要素
     */
    void addChild(Node child) {
      BhNodeView.this.getChildren().add(child);
    }

    /**
     * このノードビューのルートノードビューを返す
     */
    BhNodeView getRootView() {

      BhNodeView parent = getParentView();
      if (parent == null)
        return BhNodeView.this;

      return parent.getTreeManager().getRootView();
    }

    /**
     * このノードがルートノードかどうか調べる.
     * @return このノードがルートノードの場合 true
     */
    boolean isRootView() {
      return getParentView() == null;
    }
  }

  /**
   * 位置の変更, 取得操作を行うクラス
   */
  public class PositionManager {

    private BiConsumer<Double, Double> onAbsPosUpdated;
    private final Vec2D relativePos = new Vec2D(0.0, 0.0);

    /**
     * 親 BhNodeView からの相対位置を指定する
     * @param posX 親 BhNodeView からの相対位置 X
     * @param posY 親 BhNodeView からの相対位置 Y
     * */
    final void setRelativePosFromParent(double posX, double posY) {
      relativePos.x = posX;
      relativePos.y = posY;
    }

    /**
     * 親 BhNodeView からの相対位置を取得する
     * @return 親 BhNodeView からの相対位置
     * */
    final Vec2D getRelativePosFromParent() {
      return new Vec2D(relativePos.x, relativePos.y);
    }

    /**
     * ワークスペース上での位置を返す
     * @return ワークスペース上での位置
     * */
    public Vec2D getPosOnWorkspace() {
      return new Vec2D(BhNodeView.this.getTranslateX(), BhNodeView.this.getTranslateY());
    }

    /**
     * ノードの絶対位置を更新する. <br>
     * ワークスペース上での位置と4分木空間上での位置を更新する.
     * @param posX 本体部分左上のワークスペース上でのX位置
     * @param posY 本体部分左上のワークスペース上でのY位置
     */
    public void setPosOnWorkspace(double posX, double posY) {

      viewRegionManager.updatePosOnQTSpace(posX, posY);
      updatePosOnWorkspace(posX, posY);
      if (onAbsPosUpdated != null)
        onAbsPosUpdated.accept(posX, posY);
    }

    /**
     * GUI部品のワークスペース上での位置を更新する.
     */
    private void updatePosOnWorkspace(double posX, double posY) {

      BhNodeView.this.setTranslateX(posX);
      BhNodeView.this.setTranslateY(posY);
      BhNodeView.this.syntaxErrorMark.setTranslateX(posX);
      BhNodeView.this.syntaxErrorMark.setTranslateY(posY);
      BhNodeView.this.shadowShape.setTranslateX(posX);
      BhNodeView.this.shadowShape.setTranslateY(posY);
    }

    /**
     * このノード以下のノードのZ位置を更新する
     */
    public void updateZPos() {

      CallbackInvoker.invoke(
        nodeView -> {
          nodeView.syntaxErrorMark.setViewOrder(SYNTAX_ERR_MARK_VIEW_ORDER);
          Parent parent = nodeView.getTreeManager().getParentView();
          if (parent == null) {
            nodeView.setViewOrder(0.0);
            nodeView.getPositionManager().updateShadowZPos();
            return;
          }
          double viewOrder = parent.getViewOrder() + CHILD_VIEW_ORDER_OFFSET_FROM_PARENT;
          nodeView.setViewOrder(viewOrder);
          nodeView.getPositionManager().updateShadowZPos();
        },
        BhNodeView.this,
        false);
    }

    /**
     * このノードが影を描画するノード群のルートノードである場合, 影描画用領域のZ位置を更新する
     */
    private void updateShadowZPos() {

      Parent shadowGroup = shadowShape.getParent();
      if (shadowGroup instanceof Group && getAppearanceManager().isShadowRoot()) {
        ((Group)shadowGroup).setViewOrder(getViewOrder() + SHADOW_GROUP_VIEW_ORDER_OFFSET);
      }
    }

    /**
     * ノードをGUI上で動かす. WS上の絶対位置(=4分木空間上の位置)も更新する
     * @param diffX X方向移動量
     * @param diffY Y方向移動量
     */
    public void move(double diffX, double diffY) {

      Vec2D posOnWS = getPosOnWorkspace();
      Vec2D wsSize = ViewHelper.INSTANCE.getWorkspaceView(BhNodeView.this).getWorkspaceSize();
      Vec2D newPos = ViewHelper.INSTANCE.newPosition(new Vec2D(diffX, diffY), wsSize, posOnWS);
      setPosOnWorkspace(newPos.x, newPos.y);
      getEventManager().invokeOnMoved();
    }

    /**
     * 絶対位置が更新された時のイベントハンドラをセットする
     */
    void setOnAbsPosUpdated(BiConsumer<Double, Double> onAbsPosUpdated) {
      this.onAbsPosUpdated = onAbsPosUpdated;
    }

    /**
     * Z位置を最前面か本来の位置にする.
     * @param enable 最前面に移動する場合 true. 本来の位置に移動する場合 false.
     * */
    public void toFront(boolean enable) {

      if (enable) {
        Parent parent = getParent();
        if (parent != null)
          parent.toFront();

        CallbackInvoker.invoke(
          nodeView -> {
            nodeView.setViewOrder(nodeView.getViewOrder() + FRONT_VIEW_ORDER_OFFSET);
            nodeView.getPositionManager().updateShadowZPos();
            nodeView.syntaxErrorMark.setViewOrder(
              nodeView.syntaxErrorMark.getViewOrder() + FRONT_VIEW_ORDER_OFFSET);
          },
          BhNodeView.this,
          false);
      }
      else {
        updateZPos();
      }
    }
  }

  public class EventManager {

    /** ノードビューの位置が変わったときのイベントハンドラのリスト */
    private List<Consumer<? super BhNodeView>> onMoved = new ArrayList<>();
    /** ノードビューをワークスペースビューに追加したときのイベントハンドラのリスト */
    private List<Consumer<? super BhNodeView>> onAddToWorkspaceView = new ArrayList<>();
    /** ノードビューをワークスペースビューから取り除いた時のイベントハンドラのリスト */
    private List<Consumer<? super BhNodeView>> onRemovedFromWorkspaceView = new ArrayList<>();
    /** ノードビューツリー内の全ノードのサイズ変更が完了した時のイベントハンドラのリスト */
    private List<Consumer<? super BhNodeView>> onNodeSizesInTreeChanged = new ArrayList<>();

    /**
     * マウス押下時のイベントハンドラを登録する
     * @param handler 登録するイベントハンドラ
     * */
    public void setOnMousePressed(EventHandler<? super MouseEvent> handler) {
      nodeShape.setOnMousePressed(handler);
    }

    /**
     * マウスドラッグ中のイベントハンドラを登録する
     * @param handler 登録するイベントハンドラ
     * */
    public void setOnMouseDragged(EventHandler<? super MouseEvent> handler) {
      nodeShape.setOnMouseDragged(handler);
    }

    /**
     * マウスドラッグを検出したときのイベントハンドラを登録する
     * @param handler 登録するイベントハンドラ
     * */
    public void setOnDragDetected(EventHandler<? super MouseEvent> handler) {
      nodeShape.setOnDragDetected(handler);
    }

    /**
     * マウスボタンを離した時のイベントハンドラを登録する
     * @param handler 登録するイベントハンドラ
     * */
    public void setOnMouseReleased(EventHandler<? super MouseEvent> handler) {
      nodeShape.setOnMouseReleased(handler);
    }

    /**
     * この view にイベントを伝える
     * @param event イベント受信対象に伝えるイベント
     * */
    public void propagateEvent(Event event) {
      nodeShape.fireEvent(event);
    }

    /**
     * ノードビューの位置が変わったときのイベントハンドラを追加する. <br>
     * 登録したハンドラは, GUIスレッド上で実行される
     * @param handler 追加するイベントハンドラ
     */
    public void addOnMoved(Consumer<? super BhNodeView> handler) {
      onMoved.add(handler);
    }

    /**
     * ノードビューの位置が変わったときのイベントハンドラを削除する. <br>
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnMoved(Consumer<? super BhNodeView> handler) {
      onMoved.remove(handler);
    }

    /**
     * ノードビューをワークスペースビューに追加したときのイベントハンドラを追加する. <br>
     * 登録したハンドラは, GUIスレッド上で実行される.
     * @param handler 追加するイベントハンドラ
     */
    public void addOnAddedToWorkspaceView(Consumer<? super BhNodeView> handler) {
      onAddToWorkspaceView.add(handler);
    }

    /**
     * ノードビューをワークスペースビューに追加したときのイベントハンドラを削除する. <br>
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnAddedToWorkspaceView(Consumer<? super BhNodeView> handler) {
      onAddToWorkspaceView.remove(handler);
    }

    /**
     * ノードビューをワークスペースビューから取り除いた時のイベントハンドラを追加する. <br>
     * 登録したハンドラは, GUIスレッド上で実行される.
     * @param handler 追加するイベントハンドラ
     */
    public void addOnRemovedFromWorkspaceView(Consumer<? super BhNodeView> handler) {
      onRemovedFromWorkspaceView.add(handler);
    }

    /**
     * ノードビューをワークスペースビューから取り除いた時のイベントハンドラを削除する.
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnRemovedFromWorkspaceView(Consumer<? super BhNodeView> handler) {
      onRemovedFromWorkspaceView.remove(handler);
    }

    /**
     * ノードビューツリー内の全ノードのサイズ変更が完了した時のイベントハンドラを登録する.
     *
     * <p> イベントハンドラを登録したノードを含むノードビューツリーの全ノードの更新が終わったとき
     * {@code handler} が呼ばれる.
     * <p> 登録したハンドラは, GUIスレッド上で実行される.
     *
     * @param handler 追加するイベントハンドラ.
     */
    public void  addOnNodeSizesInTreeChanged(Consumer<? super BhNodeView> handler) {
      onNodeSizesInTreeChanged.add(handler);
    }

    /**
     * ノードビューツリー内の全ノードのサイズ変更が完了した時のイベントハンドラを削除する.
     * @param handler 削除するイベントハンドラ.
     */
    public void  removeOnNodeSizesInTreeChanged(Consumer<? super BhNodeView> handler) {
      onNodeSizesInTreeChanged.add(handler);
    }

    /**
     * ノードビューの位置が変わったときのイベントハンドラを実行する,
     */
    private void invokeOnMoved() {

      if (!Platform.isFxApplicationThread())
        throw new IllegalStateException(getClass().getSimpleName() + ".invokeOnMoved"+
        "  A handler invoked in an inappropriate thread");
      onMoved.forEach(handler -> handler.accept(BhNodeView.this));
    }

    /**
     * ノードビューをワークスペースビューに追加したときのイベントハンドラを実行する.
     */
    private void invokeOnAddToWorkspaceView() {

      if (!Platform.isFxApplicationThread())
        throw new IllegalStateException(getClass().getSimpleName() + ".invokeOnAddToWorkspaceView"+
        "  A handler invoked in an inappropriate thread");
      onAddToWorkspaceView.forEach(handler -> handler.accept(BhNodeView.this));
    }

    /**
     * ノードビューをワークスペースビューから取り除いた時のイベントハンドラを実行する.
     */
    private void invokeOnRemovedFromWorkspaceView() {

      if (!Platform.isFxApplicationThread())
        throw new IllegalStateException(getClass().getSimpleName() + "invokeOnRemovedFromWorkspaceView" +
        "    A handler invoked in an inappropriate thread");
      onRemovedFromWorkspaceView.forEach(handler -> handler.accept(BhNodeView.this));
    }

    /**
     * ノードビューツリー内の全ノードのサイズ変更が完了した時のイベントハンドラを実行する.
     */
    private void invokeOnNodeSizesInTreeChanged() {

      if (!Platform.isFxApplicationThread())
        throw new IllegalStateException(getClass().getSimpleName() + ".invokeOnNodeSizesInTreeChanged" +
        "    A handler invoked in an inappropriate thread");
      onNodeSizesInTreeChanged.forEach(handler -> handler.accept(BhNodeView.this));
    }
  }

  private class SyntaxErrorMark extends Line {

    SyntaxErrorMark(double startX, double startY, double endX, double endY) {
      super(startX, startY, endX, endY);
      this.setVisible(false);
      this.setMouseTransparent(true);
      this.setViewOrder(SYNTAX_ERR_MARK_VIEW_ORDER);
    }
  }
}













