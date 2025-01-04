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

package net.seapanda.bunnyhop.view.workspace;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.BhSettings;
import net.seapanda.bunnyhop.common.Rem;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.model.workspace.WorkspaceSet;
import net.seapanda.bunnyhop.quadtree.QuadTreeManager;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle.OverlapOption;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.ViewUtil;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.BhNodeView.RegionManager.Rectangles;

/**
 * {@link WorkspaceView}を表すビュー (タブの中の描画物に対応).
 *
 * @author K.Koike
 */
public class WorkspaceView extends Tab {

  private static final double Z_POS_INTERVAL_BETWEEN_NODE_VIEW_TREES = -20000;
  private static final double MAX_Z_POS_OF_NODE_VIEW_TREES = -2e15;
  private static final double Z_POS_OF_RECT_SEL_TOOL =
      MAX_Z_POS_OF_NODE_VIEW_TREES + Z_POS_INTERVAL_BETWEEN_NODE_VIEW_TREES;

  /** 操作対象のビュー. */
  private @FXML ScrollPane wsScrollPane;
  /** {@link BhNode} を保持するペイン. */
  private @FXML WorkspaceViewPane wsPane;
  /** エラー情報表示用. */
  private @FXML WorkspaceViewPane errInfoPane;
  /** {@code wsPane} の親ペイン. */
  private @FXML Pane wsWrapper;
  /** 矩形選択用ビュー. */
  private @FXML Polygon rectSelTool;
  /** タブ名ラベル. */
  private @FXML Label tabNameLabel;
  /** タブ名入力テキストフィールド. */
  private @FXML TextField tabNameTextField;

  private final Workspace workspace;
  private final Vec2D minPaneSize;
  /**
   * このワークスペースビューにあるルート {@link BhNodeView} 一式.
   * 後ろの要素ほど Z 位置が手前になることを保証しなければならない.
   */
  private final SequencedSet<BhNodeView> rootNodeViews = new LinkedHashSet<>();
  /** このワークスペースビューが保持する {@link BhNodeView} のセット. */
  private final Set<BhNodeView> nodeViews = new HashSet<>();
  /** このワークスペースビュー内の {@link BhNodeView} が移動したときに呼ぶイベントハンドラのリスト. */
  private final List<BiConsumer<? super BhNodeView, ? super Vec2D>> onBhNodeViewMovedList =
      new ArrayList<>();
  /** このワークスペースビュー内の {@link BhNodeView} が移動したときのイベントハンドラを呼ぶ関数オブジェクト. */
  private final BiConsumer<? super BhNodeView, ? super Vec2D> onBhNodeViewMoved =
      this::invokeOnNodeMoved;
  /** ノードの本体部分の重なり判定に使う4 分木管理クラス. */
  private QuadTreeManager quadTreeMngForBody;
  /** ノードのコネクタ部分の重なり判定に使う4 分木管理クラス. */
  private QuadTreeManager quadTreeMngForConnector;
  /**ワークスペースビューの拡大/縮小の段階. */
  private int zoomLevel = 0;
  /**ワークスペースビューの大きさの段階. */
  private int workspaceSizeLevel = 0;
  /** 最前面の Z 位置. */
  private double frontZpos = 0;

  /**
   * コンストラクタ.
   *
   * @param workspace 管理するモデル.
   * @param width ワークスペースビューの初期幅
   * @param height ワークスペースビューの初期高さ
   */
  public WorkspaceView(Workspace workspace, double width, double height)
      throws ViewInitializationException {
    this.workspace = workspace;
    minPaneSize = new Vec2D(width, height);
    configurGuiComponents();
    quadTreeMngForBody =
        new QuadTreeManager(BhConstants.LnF.NUM_DIV_OF_QTREE_SPACE, minPaneSize.x, minPaneSize.y);
    quadTreeMngForConnector =
        new QuadTreeManager(BhConstants.LnF.NUM_DIV_OF_QTREE_SPACE, minPaneSize.x, minPaneSize.y);
    drawGridLines(minPaneSize.x, minPaneSize.y, quadTreeMngForBody.getNumPartitions());
    rectSelTool.setViewOrder(Z_POS_OF_RECT_SEL_TOOL);
  }

  /**
   * GUI 部品の設定を行う.
   *
   * @param width ワークスペースビューの初期幅
   * @param height ワークスペースビューの初期高さ
   * @return 初期化に成功した場合 true
   */
  private void configurGuiComponents()
      throws ViewInitializationException {
    try {
      Path filePath = BhService.fxmlCollector().getFilePath(BhConstants.Path.WORKSPACE_FXML);
      FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
      loader.setController(this);
      loader.setRoot(this);
      loader.load();
    } catch (IOException e) {
      throw new ViewInitializationException(
        "Failed to initizlize %s.\n%s".formatted(getClass().getSimpleName(), e));
    }
    configureWsPane();
    configureTabNameComponents();
    rectSelTool.getPoints().addAll(Stream.generate(() -> 0.0).limit(8).toArray(Double[]::new));
    wsScrollPane.addEventFilter(ScrollEvent.ANY, this::onScroll);
    setOnCloseRequest(this::onCloseRequest);
    setOnClosed(this::onClosed);
  }

  /** スクロール時の処理. */
  private void onScroll(ScrollEvent event) {
    if (event.isControlDown() && event.getDeltaY() != 0) {
      event.consume();
      boolean zoomIn = event.getDeltaY() >= 0;
      zoom(zoomIn);
    }
  }

  /** ワークスペースビューの削除命令を受けた時の処理. */
  private void onCloseRequest(Event event) {
    // 空のワークスペースビュー削除時は警告なし
    if (workspace.getRootNodes().isEmpty()) {
      return;
    }
    Optional<ButtonType> buttonType = BhService.msgPrinter().alert(
        Alert.AlertType.CONFIRMATION,
        TextDefs.Workspace.AskIfDeleteWs.title.get(),
        null,
        TextDefs.Workspace.AskIfDeleteWs.body.get(workspace.getName()));

    // このイベントハンドラを抜けるとき, TabDragPolicy.FIXED にしないと
    // タブ消しをキャンセルした後, そのタブが使えなくなる.
    getTabPane().setTabDragPolicy(TabDragPolicy.FIXED);
    buttonType.ifPresent(btnType -> {
      if (!btnType.equals(ButtonType.OK)) {
        event.consume();
      }
    });
  }

  /**ワークスペースビュー削除時の処理. */
  private void onClosed(Event event) {
    UserOperation userOpe = new UserOperation();
    WorkspaceSet wss = workspace.getWorkspaceSet();
    if (wss != null) {
      wss.removeWorkspace(workspace, userOpe);
    }
    BhService.undoRedoAgent().pushUndoCommand(userOpe);
  }

  /** ノードを配置する部分の設定を行う. */
  private void configureWsPane() { 
    setErrInfoPaneListener();
    errInfoPane.setContainer(this);
    wsPane.setContainer(this);
    //  スクロールバーが表示されなくなるので setPrefSize() は使わない.   
    wsPane.setMinSize(minPaneSize.x, minPaneSize.y);
    wsPane.setMaxSize(minPaneSize.x, minPaneSize.y);
    wsPane.getTransforms().add(new Scale());
  }

  private void setErrInfoPaneListener() {
    wsPane.prefWidthProperty().addListener(
        (observable, oldVal, newVal) -> errInfoPane.setPrefWidth(newVal.doubleValue()));
    wsPane.prefHeightProperty().addListener(
        (observable, oldVal, newVal) -> errInfoPane.setPrefHeight(newVal.doubleValue()));
    wsPane.maxWidthProperty().addListener(
        (observable, oldVal, newVal) -> errInfoPane.setMaxWidth(newVal.doubleValue()));
    wsPane.maxHeightProperty().addListener(
        (observable, oldVal, newVal) -> errInfoPane.setMaxHeight(newVal.doubleValue()));
    wsPane.minWidthProperty().addListener(
        (observable, oldVal, newVal) -> errInfoPane.setMinWidth(newVal.doubleValue()));
    wsPane.minHeightProperty().addListener(
        (observable, oldVal, newVal) -> errInfoPane.setMinHeight(newVal.doubleValue()));
    wsPane.getTransforms().addListener((ListChangeListener.Change<? extends Transform> change) -> {
      errInfoPane.getTransforms().clear();
      change.getList().forEach(errInfoPane.getTransforms()::add);
    });
  }

  /** タブ名を表示する部分の設定を行う. */
  private void configureTabNameComponents() {
    tabNameTextField.setMaxWidth(Region.USE_PREF_SIZE);
    tabNameTextField.setMinWidth(Region.USE_PREF_SIZE);
    tabNameTextField.setPrefWidth(1);
    tabNameLabel.setMaxWidth(BhSettings.LnF.maxWsTabSize);
    tabNameLabel.setMinWidth(BhSettings.LnF.minWsTabSize);

    tabNameTextField.textProperty().addListener((observable, oldVal, newVal) -> {
      updateTabNameWidth();
    });
    tabNameTextField.setOnAction(event -> {
      workspace.setName(tabNameTextField.getText());
      setTabNameNotEditable(workspace.getName());      
    });
    tabNameTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        workspace.setName(tabNameTextField.getText());
        setTabNameNotEditable(workspace.getName());      
      }
    });
    tabNameLabel.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        setTabNameEditable(workspace.getName());
      }
    });

    setTabNameNotEditable(workspace.getName());
  }

  /** タブ名の幅をその文字列に応じて変える. */
  private void updateTabNameWidth() {
    Text textPart = (Text) tabNameTextField.lookup(".text");
    if (textPart == null) {
      return;
    }
    // 正確な文字部分の境界を取得するため, GUI 部品内部の Text の境界は使わない.
    double newWidth = ViewUtil.calcStrWidth(textPart.getText(), textPart.getFont());
    newWidth = Math.max(newWidth, Rem.VAL);
    // 幅を (文字幅 + パディング) にするとキャレットの移動時に文字が左右に移動するので定数 3 を足す.
    // この定数はフォントやパディングが違っても機能する.
    newWidth += tabNameTextField.getPadding().getLeft()
        + tabNameTextField.getPadding().getRight() + 3;
    tabNameTextField.setPrefWidth(newWidth);
  }

  /** タブ名を編集可能な状態にする. */
  private void setTabNameEditable(String tabName) {
    tabNameTextField.setVisible(true);
    tabNameTextField.setDisable(false);
    tabNameTextField.setText(tabName);
    tabNameTextField.selectAll();
    tabNameTextField.requestFocus();
    tabNameLabel.setText("");
    tabNameLabel.setVisible(false);
    tabNameLabel.setDisable(true);
  }

  /** タブ名を編集不可能な状態にする. */
  private void setTabNameNotEditable(String tabName) {
    tabNameLabel.setVisible(true);
    tabNameLabel.setDisable(false);
    tabNameLabel.setText(tabName);
    tabNameTextField.setVisible(false);
    tabNameTextField.setDisable(true);
    tabNameTextField.setText("");
  }

  /**
   * このワークスペースビューに対し {@code view} をルートとして指定する.
   *
   * @param view ルートとして指定するビュー
   */
  public void specifyNodeViewAsRoot(BhNodeView view) {
    if (!nodeViews.contains(view)) {
      return;
    }
    if (rootNodeViews.contains(view)) {
      rootNodeViews.remove(view);
    }
    rootNodeViews.addLast(view);
    view.getLookManager().arrange();
    if (MAX_Z_POS_OF_NODE_VIEW_TREES < frontZpos) {
      updateZposOfTrees();
    } else {
      view.getPositionManager().setTreeZpos(frontZpos);
      frontZpos += Z_POS_INTERVAL_BETWEEN_NODE_VIEW_TREES;
    }
  }

  /**
   * このワークスペースビューに対し {@code view} がルートノードとして指定されているとき, その指定を解除する.
   *
   * @param view ルートの指定を解除するするビュー
   */
  public void specifyNodeViewAsNotRoot(BhNodeView view) {
    rootNodeViews.remove(view);
  }

  /**
   * {@code view} をこのワークスペースビューに追加する.
   *
   * @param view 追加する {@link BhNodeView}
   */
  public void addNodeView(BhNodeView view) {
    if (nodeViews.contains(view)) {
      return;
    }
    nodeViews.add(view);
    view.getTreeManager().addToGuiTree(wsPane);
    view.getEventManager().addOnMoved(onBhNodeViewMoved);
    addRectToQuadTreeSpace(view);
  }

  /**
   * {@code view} をこのワークスペースビューから削除する.
   *
   * @param view 削除する {@link BhNodeView}
   */
  public void removeNodeView(BhNodeView view) {
    if (!nodeViews.contains(view)) {
      return;
    }
    specifyNodeViewAsNotRoot(view);
    nodeViews.remove(view);
    view.getTreeManager().removeFromGuiTree();
    view.getEventManager().removeOnMoved(onBhNodeViewMoved);
    view.getRegionManager().removeQuadTreeRect();
  }

  /**
   * 4 分木空間に矩形を登録する.
   *
   * @param nodeView 登録する矩形を持つ {@link BhNodeView} オブジェクト
   */
  private void addRectToQuadTreeSpace(BhNodeView nodeView) {
    Rectangles rects = nodeView.getRegionManager().getRegions();
    quadTreeMngForBody.addQuadTreeObj(rects.body());
    quadTreeMngForConnector.addQuadTreeObj(rects.cnctr());
  }

  /** このワークスペースの全てのノドビューの Z 位置を更新する. */
  private void updateZposOfTrees() {
    frontZpos = 0;
    for (BhNodeView root : rootNodeViews) {
      root.getPositionManager().setTreeZpos(frontZpos);
      frontZpos += Z_POS_INTERVAL_BETWEEN_NODE_VIEW_TREES;
    }
  }

  /**
   * 引数で指定した矩形と重なるこのワークスペースビュー上にあるノードを探す.
   *
   * @param rect この矩形と重なるノードを探す.
   * @param overlapWithBodyPart ノードのボディ部分と重なるノードを探す場合 true.
   *                            ノードのコネクタ部分と重なるノードを探す場合 false.
   * @param option 検索オプション
   * @return 引数の矩形と重なるノードのビュー
   * */
  public List<BhNodeView> searchForOverlappedNodeViews(
      QuadTreeRectangle rect, boolean overlapWithBodyPart, OverlapOption option) {
    if (rect == null) {
      return new ArrayList<BhNodeView>();
    }
    if (overlapWithBodyPart) {
      quadTreeMngForBody.addQuadTreeObj(rect);
    } else {
      quadTreeMngForConnector.addQuadTreeObj(rect);
    }
    rect.updatePos();
    List<QuadTreeRectangle> overlappedRectList = rect.searchOverlappedRects(option);
    QuadTreeManager.removeQuadTreeObj(rect);

    return overlappedRectList.stream()
        .map(rectangle -> rectangle.<BhNodeView>getUserData())
        .collect(Collectors.toCollection(ArrayList::new));
  }

  /**
   *ワークスペースビュー内でマウスが押された時の処理を追加する.
   *
   * @param handler 追加するイベントハンドラ
   */
  public void addOnMousePressed(EventHandler<? super MouseEvent> handler) {
    wsPane.addEventHandler(MouseEvent.MOUSE_PRESSED, handler);
  }

  /**
   *ワークスペースビュー内でマウスが押された時の処理を削除する.
   *
   * @param handler 削除するイベントハンドラ
   */
  public void removeOnMousePressed(EventHandler<? super MouseEvent> handler) {
    wsPane.removeEventHandler(MouseEvent.MOUSE_PRESSED, handler);
  }

  /**
   *ワークスペースビュー内でマウスがドラッグされた時の処理を追加する.
   *
   * @param handler 追加するイベントハンドラ
   */
  public void addOnMouseDragged(EventHandler<? super MouseEvent> handler) {
    wsPane.addEventHandler(MouseEvent.MOUSE_DRAGGED, handler);
  }

  /**
   *ワークスペースビュー内でマウスがドラッグされた時の処理を削除する.
   *
   * @param handler 削除するイベントハンドラ
   */
  public void removeOnMouseDragged(EventHandler<? super MouseEvent> handler) {
    wsPane.removeEventHandler(MouseEvent.MOUSE_DRAGGED, handler);
  }

  /**
   *ワークスペースビュー内でマウスが離された時の処理を追加する.
   *
   * @param handler 追加するイベントハンドラ
   */
  public void addOnMouseReleased(EventHandler<? super MouseEvent> handler) {
    wsPane.addEventHandler(MouseEvent.MOUSE_RELEASED, handler);
  }

  /**
   *ワークスペースビュー内でマウスが離された時の処理を削除する.
   *
   * @param handler 削除するイベントハンドラ
   */
  public void removeOnMouseReleased(EventHandler<? super MouseEvent> handler) {
    wsPane.removeEventHandler(MouseEvent.MOUSE_RELEASED, handler);
  }

  /**
   *ワークスペースビュー内で {@link BhNodeView} が移動したときの処理を追加する.
   *
   * @param handler 追加するイベントハンドラ
   */
  public void addOnNodeMoved(BiConsumer<? super BhNodeView, ? super Vec2D> handler) {
    onBhNodeViewMovedList.add(handler);
  }

  /**
   *ワークスペースビュー内で {@link BhNodeView} が移動したときの処理を削除する.
   *
   * @param handler 削除するイベントハンドラ
   */
  public void removeOnNodeMoved(BiConsumer<? super BhNodeView, ? super Vec2D> handler) {
    onBhNodeViewMovedList.remove(handler);
  }

  /**ワークスペースビュー内で {@link BhNodeView} が移動したときのイベントハンドラを呼び出す. */
  private void invokeOnNodeMoved(BhNodeView view, Vec2D posOnWs) {
    onBhNodeViewMovedList.forEach(handler -> handler.accept(view, posOnWs));
  }

  /**
   * ワークスペースビューの大きさを返す.
   *
   * @return ワークスペースビューの大きさ
   */
  public Vec2D getWorkspaceSize() {
    return new Vec2D(wsPane.getWidth(), wsPane.getHeight());
  }

  /**
   * この {@link Wo} Viewに対応しているWorkspace を返す.
   *
   * @return この View に対応している {@link Workspace}
   */
  public Workspace getWorkspace() {
    return workspace;
  }

  /**
   *ワークスペースビューの大きさを変える.
   *
   * @param widen ワークスペースビューの大きさを大きくする場合 true
   */
  public void changeViewSize(boolean widen) {
    if ((workspaceSizeLevel == BhConstants.LnF.MIN_WORKSPACE_SIZE_LEVEL) && !widen) {
      return;
    }
    if ((workspaceSizeLevel == BhConstants.LnF.MAX_WORKSPACE_SIZE_LEVEL) && widen) {
      return;
    }
    workspaceSizeLevel = widen ? workspaceSizeLevel + 1 : workspaceSizeLevel - 1;
    Vec2D currentSize = quadTreeMngForBody.getQtSpaceSize();
    double newWsWidth = widen ? currentSize.x * 2.0 : currentSize.x / 2.0;
    double newWsHeight = widen ? currentSize.y * 2.0 : currentSize.y / 2.0;

    wsPane.setMinSize(newWsWidth, newWsHeight);
    wsPane.setMaxSize(newWsWidth, newWsHeight);
    quadTreeMngForBody = new QuadTreeManager(
        quadTreeMngForBody, BhConstants.LnF.NUM_DIV_OF_QTREE_SPACE, newWsWidth, newWsHeight);
    quadTreeMngForConnector = new QuadTreeManager(
        quadTreeMngForConnector, BhConstants.LnF.NUM_DIV_OF_QTREE_SPACE, newWsWidth, newWsHeight);

    //全ノードの位置更新
    for (BhNodeView rootView : rootNodeViews) {
      Vec2D pos = rootView.getPositionManager().getPosOnWorkspace();  //workspace からの相対位置を計算
      rootView.getPositionManager().setTreePosOnWorkspace(pos.x, pos.y);
    }
    drawGridLines(newWsWidth, newWsHeight, quadTreeMngForBody.getNumPartitions());
    recalculateScrollableRange();
  }

  private void drawGridLines(double width, double height, int numDiv) {
    ArrayList<Line> removedList = new ArrayList<>();
    for (Node content : wsPane.getChildren()) {
      if (content instanceof Line) {
        removedList.add((Line) content);
      }
    }
    removedList.forEach(line -> wsPane.getChildren().remove(line));
    for (int i = 0; i < numDiv; ++i) {
      int x = (int) ((width / numDiv) * i);
      wsPane.getChildren().add(0, new Line(x, 0, x, height));
    }
    for (int i = 0; i < numDiv; ++i) {
      int y = (int) ((height / numDiv) * i);
      wsPane.getChildren().add(0, new Line(0, y, width, y));
    }
  }

  /**
   * Scene上の座標をWorkspace上の位置に変換して返す.
   *
   * @param x Scene座標の変換したいX位置
   * @param y Scene座標の変換したいY位置
   * @return 引数の座標のWorkspace上の位置
   */
  public Vec2D sceneToWorkspace(double x, double y) {
    var pos = wsPane.sceneToLocal(x, y);
    return new Vec2D(pos.getX(), pos.getY());
  }

  /**
   *ワークスペースビューのズーム処理を行う.
   *
   * @param zoomIn 拡大処理を行う場合true
   */
  public void zoom(boolean zoomIn) {
    if ((BhConstants.LnF.MIN_ZOOM_LEVEL == zoomLevel) && !zoomIn) {
      return;
    }
    if ((BhConstants.LnF.MAX_ZOOM_LEVEL == zoomLevel) && zoomIn) {
      return;
    }
    Scale scale = new Scale();
    if (zoomIn) {
      ++zoomLevel;
    } else {
      --zoomLevel;
    }
    double mag = Math.pow(BhConstants.LnF.ZOOM_MAGNIFICATION, zoomLevel);
    scale.setX(mag);
    scale.setY(mag);
    wsPane.getTransforms().clear();
    wsPane.getTransforms().add(scale);
    recalculateScrollableRange();
  }

  /**ワークスペースビューの表示の拡大率を設定する. */
  public void setZoomLevel(int level) {
    int numZooms = Math.abs(zoomLevel - level);
    boolean zoomIn = zoomLevel < level;
    for (int i = 0; i < numZooms; ++i) {
      zoom(zoomIn);
    }
  }

  /** スクロール可能な範囲を再計算する. */
  private void recalculateScrollableRange() {
    double magX = wsPane.getTransforms().get(0).getMxx();
    double magY = wsPane.getTransforms().get(0).getMyy();
    // 全ノードの内の右端の最大の位置と下端の最大の位置
    Vec2D maxLowerRightPosOfNodes = rootNodeViews.stream()
        .map(nodeView -> {
          Vec2D nodeSize = nodeView.getRegionManager().getNodeTreeSize(false);
          Vec2D nodePos = nodeView.getPositionManager().getPosOnWorkspace();
          return new Vec2D(
            magX * (nodePos.x + nodeSize.x) + BhConstants.LnF.NODE_SCALE * 20,
            magY * (nodePos.y + nodeSize.y) + BhConstants.LnF.NODE_SCALE * 20);
        })
        .reduce(
          new Vec2D(0.0, 0.0),
          (accum, pos) -> new Vec2D(Math.max(accum.x, pos.x), Math.max(accum.y, pos.y)));

    Vec2D zoomedWsPaneSize = new Vec2D(wsPane.getMinWidth() * magX, wsPane.getMinHeight() * magY);
    double scrollableHorizontalRange = Math.max(maxLowerRightPosOfNodes.x, zoomedWsPaneSize.x);
    double scrollableVerticalRange = Math.max(maxLowerRightPosOfNodes.y, zoomedWsPaneSize.y);
    wsWrapper.setPrefSize(scrollableHorizontalRange, scrollableVerticalRange);
  }

  /** 
   * マルチノードシフタをワークスペースビューに追加する.
   * マルチノードシフタ: 複数ノードを同時に移動させる GUI 部品
   */
  public void addtMultiNodeShifterView(MultiNodeShifterView multiNodeShifter) {
    wsPane.getChildren().add(multiNodeShifter);
  }

  /**
   * 矩形選択ツールを表示する.
   *
   * @param upperLeft 表示する矩形のワークスペースビュー上の左上の座標
   * @param lowerRight 表示する矩形のワークスペースビュー上の右下の座標
   * */
  public void showSelectionRectangle(Vec2D upperLeft, Vec2D lowerRight) {
    rectSelTool.setVisible(true);
    rectSelTool.getPoints().set(0, upperLeft.x);
    rectSelTool.getPoints().set(1, upperLeft.y);
    rectSelTool.getPoints().set(2, lowerRight.x);
    rectSelTool.getPoints().set(3, upperLeft.y);
    rectSelTool.getPoints().set(4, lowerRight.x);
    rectSelTool.getPoints().set(5, lowerRight.y);
    rectSelTool.getPoints().set(6, upperLeft.x);
    rectSelTool.getPoints().set(7, lowerRight.y);
  }

  /** 矩形選択ツールを非表示にする. */
  public void hideSelectionRectangle() {
    rectSelTool.setVisible(false);
  }

  /**
   * {@code nodeView} が中央に表示されるようにスクロールする.
   * {@code nodeView} がこのワークスペースビューに存在しない場合なにもしない.
   *
   * @param view 中央に表示するノードビュー
   */
  public void lookAt(BhNodeView view) {
    if (!this.equals(view.getWorkspaceView())) {
      return;
    }
    getTabPane().getSelectionModel().select(this);
    var zoomedWsSize = new Vec2D(wsWrapper.getPrefWidth(), wsWrapper.getPrefHeight());
    var scrollPaneUpperLeftCenterPos =
        new Vec2D(wsScrollPane.getWidth() * 0.5, wsScrollPane.getHeight() * 0.5);
    var scrollPaneLowerRightCenterPos = new Vec2D(
        zoomedWsSize.x - scrollPaneUpperLeftCenterPos.x,
        zoomedWsSize.y - scrollPaneUpperLeftCenterPos.y);
    var scrollableRange = new Vec2D(
        Math.max(1.0, scrollPaneLowerRightCenterPos.x - scrollPaneUpperLeftCenterPos.x),
        Math.max(1.0, scrollPaneLowerRightCenterPos.y - scrollPaneUpperLeftCenterPos.y));
    var nodeViewCenterPos = getCenterPosOnZoomedWorkspace(view);
    var scrollBarPos = new Vec2D(
        (nodeViewCenterPos.x - scrollPaneUpperLeftCenterPos.x) / scrollableRange.x,
        (nodeViewCenterPos.y - scrollPaneUpperLeftCenterPos.y) / scrollableRange.y);

    scrollBarPos.x = scrollBarPos.x * (wsScrollPane.getHmax() - wsScrollPane.getHmin())
        + wsScrollPane.getHmin();
    scrollBarPos.y = scrollBarPos.y * (wsScrollPane.getVmax() - wsScrollPane.getVmin())
        + wsScrollPane.getVmin();
    wsScrollPane.setHvalue(
        Math.clamp(scrollBarPos.x, wsScrollPane.getHmin(), wsScrollPane.getHmax()));
    wsScrollPane.setVvalue(
        Math.clamp(scrollBarPos.y, wsScrollPane.getVmin(), wsScrollPane.getVmax()));
  }

  /** ズームしたワークスペースビュー上でのノードビューの中心位置を返す. */
  private Vec2D getCenterPosOnZoomedWorkspace(BhNodeView view) {
    double magX = wsPane.getTransforms().get(0).getMxx();
    double magY = wsPane.getTransforms().get(0).getMyy();
    Vec2D nodeSize = view.getRegionManager().getNodeSize(false);
    Vec2D nodePos = view.getPositionManager().getPosOnWorkspace();
    return new Vec2D(magX * (nodePos.x + nodeSize.x * 0.5), magY * (nodePos.y + nodeSize.y * 0.5));
  }

  /**
   * このワークスペースビューの全てのノードビューの影を消す.
   */
  public void hideAllNodeShadows() {
    for (BhNodeView root : rootNodeViews) {
      root.getLookManager().hideShadow();
    }
  }

  /** このワークスペースビューが持つ全てのルートノードビューを取得する. */
  public SequencedSet<BhNodeView> getRootNodeViews() {
    return new LinkedHashSet<>(rootNodeViews);
  }

  /**
   * {@code view} を含むノードビューツリー全体をこのワークスペースビュー上で最前に移動させる.
   * {@code view} を含むノードビューツリーのルートノードビューがこのワークスペースビューにない場合, 何もしない.
   *
   * @param view このノードビューを含むノードビューツリー全体をこのワークスペースビュー上で最前に移動させる.
   */
  public void moveToFront(BhNodeView view) {
    BhNodeView root = view.getTreeManager().getRootView();
    if (!rootNodeViews.contains(root)) {
      return;
    }
    rootNodeViews.remove(root);
    rootNodeViews.addLast(root);
    if (MAX_Z_POS_OF_NODE_VIEW_TREES < frontZpos) {
      updateZposOfTrees();
    } else {
      root.getPositionManager().setTreeZpos(frontZpos);
      frontZpos += Z_POS_INTERVAL_BETWEEN_NODE_VIEW_TREES;
    }
  }
}
