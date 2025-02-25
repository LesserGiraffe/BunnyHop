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
import java.util.SequencedSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.quadtree.QuadTreeManager;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle.OverlapOption;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.ViewConstructionException;
import net.seapanda.bunnyhop.view.ViewUtil;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.BhNodeView.RegionManager.Rectangles;

/**
 * {@link Workspace} のビュー.
 *
 * @author K.Koike
 */
public class FxmlWorkspaceView extends Tab implements WorkspaceView {

  private static final double Z_POS_INTERVAL_BETWEEN_NODE_VIEW_TREES = -20000;
  private static final double MAX_Z_POS_OF_NODE_VIEW_TREES = -2e15;
  private static final double Z_POS_OF_RECT_SEL_TOOL =
      MAX_Z_POS_OF_NODE_VIEW_TREES + Z_POS_INTERVAL_BETWEEN_NODE_VIEW_TREES;
  private static final double Z_POS_OF_NODE_SHIFTER =
      MAX_Z_POS_OF_NODE_VIEW_TREES + Z_POS_INTERVAL_BETWEEN_NODE_VIEW_TREES - 1;

  /** 操作対象のビュー. */
  private @FXML ScrollPane wsScrollPane;
  /** {@link BhNode} を置くペイン. */
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
  private EventManagerImpl eventManager = new EventManagerImpl();

  /**
   * コンストラクタ.
   *
   * @param workspace 管理するモデル.
   * @param width ワークスペースビューの初期幅
   * @param height ワークスペースビューの初期高さ
   * @param filePath ワークスペースビューが定義された fxml ファイルのパス
   */
  public FxmlWorkspaceView(Workspace workspace, double width, double height, Path filePath)
      throws ViewConstructionException {
    this.workspace = workspace;
    minPaneSize = new Vec2D(width, height);
    configurGuiComponents(filePath);
    quadTreeMngForBody =
        new QuadTreeManager(BhConstants.LnF.NUM_DIV_OF_QTREE_SPACE, minPaneSize.x, minPaneSize.y);
    quadTreeMngForConnector =
        new QuadTreeManager(BhConstants.LnF.NUM_DIV_OF_QTREE_SPACE, minPaneSize.x, minPaneSize.y);
    drawGridLines(minPaneSize.x, minPaneSize.y, quadTreeMngForBody.getNumPartitions());
    rectSelTool.setViewOrder(Z_POS_OF_RECT_SEL_TOOL);
  }

  /** GUI 部品の設定を行う. */
  private void configurGuiComponents(Path filePath)
      throws ViewConstructionException {
    try {
      FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
      loader.setController(this);
      loader.setRoot(this);
      loader.load();
    } catch (IOException e) {
      throw new ViewConstructionException(
        "Failed to initizlize %s.\n%s".formatted(getClass().getSimpleName(), e));
    }
    configureWsPane();
    configureTabNameComponents();
    rectSelTool.getPoints().addAll(Stream.generate(() -> 0.0).limit(8).toArray(Double[]::new));
    wsScrollPane.addEventFilter(ScrollEvent.ANY, this::onScroll);
    setOnCloseRequest(eventManager::invokeOnCloseRequest);
    setOnCloseRequest(eventManager::invokeOnClosed);
  }

  /** スクロール時の処理. */
  private void onScroll(ScrollEvent event) {
    if (event.isControlDown() && event.getDeltaY() != 0) {
      event.consume();
      boolean zoomIn = event.getDeltaY() >= 0;
      zoom(zoomIn);
    }
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

  @Override
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

  @Override
  public void specifyNodeViewAsNotRoot(BhNodeView view) {
    rootNodeViews.remove(view);
  }

  @Override
  public void addNodeView(BhNodeView view) {
    if (nodeViews.contains(view)) {
      return;
    }
    nodeViews.add(view);
    view.getTreeManager().addToGuiTree(wsPane);
    view.getEventManager().addOnMoved(eventManager.onBhNodeViewMoved);
    addRectToQuadTreeSpace(view);
  }

  @Override
  public void removeNodeView(BhNodeView view) {
    if (!nodeViews.contains(view)) {
      return;
    }
    specifyNodeViewAsNotRoot(view);
    nodeViews.remove(view);
    view.getTreeManager().removeFromGuiTree();
    view.getEventManager().removeOnMoved(eventManager.onBhNodeViewMoved);
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

  @Override
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

  @Override
  public Vec2D getSize() {
    return new Vec2D(wsPane.getWidth(), wsPane.getHeight());
  }

  @Override
  public Workspace getWorkspace() {
    return workspace;
  }

  @Override
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

  @Override
  public Vec2D sceneToWorkspace(double x, double y) {
    var pos = wsPane.sceneToLocal(x, y);
    return new Vec2D(pos.getX(), pos.getY());
  }

  @Override
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

  @Override
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

  @Override
  public void addNodeShifterView(NodeShifterView view) {
    view.setViewOrder(Z_POS_OF_NODE_SHIFTER);
    wsPane.getChildren().add(view);
  }

  @Override
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

  @Override
  public void hideSelectionRectangle() {
    rectSelTool.setVisible(false);
  }

  @Override
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

  @Override
  public SequencedSet<BhNodeView> getRootNodeViews() {
    return new LinkedHashSet<>(rootNodeViews);
  }

  @Override
  public void moveNodeViewToFront(BhNodeView view) {
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

  /**
   * このワークスペースビューに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このワークスペースビューに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  public EventManager getEventManager() {
    return eventManager;
  }

  /** イベントハンドラの管理を行うクラス. */
  public class EventManagerImpl implements EventManager {

    /** このワークスペースビュー内の {@link BhNodeView} が移動したときに呼ぶイベントハンドラのリスト. */
    private final SequencedSet<BiConsumer<? super BhNodeView, ? super Vec2D>> onBhNodeViewMovedList
        = new LinkedHashSet<>();
    /** このワークスペースビュー内の {@link BhNodeView} が移動したときのイベントハンドラを呼ぶ関数オブジェクト. */
    private final BiConsumer<? super BhNodeView, ? super Vec2D> onBhNodeViewMoved =
        this::invokeOnNodeMoved;
    /** このワークスペースビューを閉じるリクエストを受け取ったときのイベントハンドラ. */
    private Supplier<? extends Boolean> onCloseRequest = () -> true;
    /** このワークスペースビューが閉じられたときに呼ぶイベントハンドラのリスト. */
    private final SequencedSet<Runnable> onClosedList = new LinkedHashSet<>();
    
    @Override
    public void addOnMousePressed(EventHandler<? super MouseEvent> handler) {
      wsPane.addEventHandler(MouseEvent.MOUSE_PRESSED, handler);
    }

    @Override
    public void removeOnMousePressed(EventHandler<? super MouseEvent> handler) {
      wsPane.removeEventHandler(MouseEvent.MOUSE_PRESSED, handler);
    }

    @Override
    public void addOnMouseDragged(EventHandler<? super MouseEvent> handler) {
      wsPane.addEventHandler(MouseEvent.MOUSE_DRAGGED, handler);
    }

    @Override
    public void removeOnMouseDragged(EventHandler<? super MouseEvent> handler) {
      wsPane.removeEventHandler(MouseEvent.MOUSE_DRAGGED, handler);
    }

    @Override
    public void addOnMouseReleased(EventHandler<? super MouseEvent> handler) {
      wsPane.addEventHandler(MouseEvent.MOUSE_RELEASED, handler);
    }

    @Override
    public void removeOnMouseReleased(EventHandler<? super MouseEvent> handler) {
      wsPane.removeEventHandler(MouseEvent.MOUSE_RELEASED, handler);
    }

    @Override
    public void addOnNodeMoved(BiConsumer<? super BhNodeView, ? super Vec2D> handler) {
      onBhNodeViewMovedList.add(handler);
    }

    @Override
    public void removeOnNodeMoved(BiConsumer<? super BhNodeView, ? super Vec2D> handler) {
      onBhNodeViewMovedList.remove(handler);
    }

    /** ワークスペースビュー内で {@link BhNodeView} が移動したときのイベントハンドラを呼び出す. */
    private void invokeOnNodeMoved(BhNodeView view, Vec2D posOnWs) {
      onBhNodeViewMovedList.forEach(handler -> handler.accept(view, posOnWs));
    }

    @Override
    public void setOnCloseRequest(Supplier<? extends Boolean> handler) {
      if (handler == null) {
        onCloseRequest = () -> true;
      }
      onCloseRequest = handler;
    }

    /** ワークスペースビュー内で {@link BhNodeView} が移動したときのイベントハンドラを呼び出す. */
    private void invokeOnCloseRequest(Event event) {
      if (!onCloseRequest.get()) {
        event.consume();
      }
      TabPane tabPane = getTabPane();
      if (tabPane != null) {
        // このイベントハンドラを抜けるとき, TabDragPolicy.FIXED にしないと
        // タブ消しをキャンセルした後, そのタブが使えなくなる.
        getTabPane().setTabDragPolicy(TabDragPolicy.FIXED);
      }
    }

    @Override
    public void addOnClosed(Runnable handler) {
      onClosedList.add(handler);
    }

    @Override
    public void removeOnClosed(Runnable handler) {
      onClosedList.remove(handler);
    }

    /** ワークスペースビューが閉じられたときのイベントハンドラを呼び出す. */
    private void invokeOnClosed(Event event) {
      onClosedList.forEach(handler -> handler.run());
    }
  }
}
