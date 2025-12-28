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

package net.seapanda.bunnyhop.workspace.view;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.BhNodeView.RegionManager.Rectangles;
import net.seapanda.bunnyhop.service.accesscontrol.ModelAccessNotificationService;
import net.seapanda.bunnyhop.service.accesscontrol.ModelAccessNotificationService.Context;
import net.seapanda.bunnyhop.ui.view.Rem;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.SimpleConsumerInvoker;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.workspace.model.Workspace;
import net.seapanda.bunnyhop.workspace.view.quadtree.QuadTreeManager;
import net.seapanda.bunnyhop.workspace.view.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.workspace.view.quadtree.QuadTreeRectangle.OverlapOption;

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
  @FXML private ScrollPane wsScrollPane;
  /** {@link BhNode} を置くペイン. */
  @FXML private WorkspaceViewPane wsPane;
  /** エラー情報表示用. */
  @FXML private WorkspaceViewPane errInfoPane;
  /** {@code wsPane} の親ペイン. */
  @FXML private Pane wsWrapper;
  /** 矩形選択用ビュー. */
  @FXML private Polygon rectSelTool;
  /** タブ名ラベル. */
  @FXML private Label tabNameLabel;
  /** タブ名入力テキストフィールド. */
  @FXML private TextField tabNameTextField;

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
  /** モデルへのアクセスの通知先となるオブジェクト. */
  private final ModelAccessNotificationService notifService;
  private final CallbackRegistryImpl cbRegistry = new CallbackRegistryImpl();

  /**
   * コンストラクタ.
   *
   * @param workspace 管理するモデル.
   * @param size ワークスペースの初期サイズ
   * @param filePath ワークスペースビューが定義された fxml ファイルのパス
   * @param service モデルへのアクセスの通知先となるオブジェクト
   */
  public FxmlWorkspaceView(
      Workspace workspace,
      Vec2D size,
      Path filePath,
      ModelAccessNotificationService service)
      throws ViewConstructionException {
    Objects.requireNonNull(workspace);
    this.workspace = workspace;
    minPaneSize = new Vec2D(size);
    this.notifService = service;
    configureGuiComponents(filePath);
    setEventHandlers();
    quadTreeMngForBody =
        new QuadTreeManager(BhConstants.Ui.NUM_DIV_OF_QTREE_SPACE, minPaneSize.x, minPaneSize.y);
    quadTreeMngForConnector =
        new QuadTreeManager(BhConstants.Ui.NUM_DIV_OF_QTREE_SPACE, minPaneSize.x, minPaneSize.y);
    drawGridLines(minPaneSize.x, minPaneSize.y, quadTreeMngForBody.getNumPartitions());
    rectSelTool.setViewOrder(Z_POS_OF_RECT_SEL_TOOL);
  }

  /** GUI 部品の設定を行う. */
  private void configureGuiComponents(Path filePath)
      throws ViewConstructionException {
    try {
      FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
      loader.setController(this);
      loader.setRoot(this);
      loader.load();
    } catch (IOException e) {
      throw new ViewConstructionException(
        "Failed to initialize %s.\n%s".formatted(getClass().getSimpleName(), e));
    }
    configureWsPane();
    configureTabNameComponents();
    rectSelTool.getPoints().addAll(Stream.generate(() -> 0.0).limit(8).toArray(Double[]::new));
  }

  private void setEventHandlers() {
    wsScrollPane.addEventFilter(ScrollEvent.ANY, this::onScroll);
    setOnCloseRequest(cbRegistry::onCloseRequested);
    setOnClosed(cbRegistry::onClosed);
    workspace.getCallbackRegistry().getOnNameChanged().add(
        event -> setTabNameNotEditable(event.newName()));
    cbRegistry.setEventHandlers();
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
    tabNameTextField.textProperty().addListener(
        (observable, oldVal, newVal) -> updateTabNameWidth());
    tabNameTextField.setOnAction(event -> tabNameTextField.setVisible(false));
    tabNameTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        changeWsName();
      }
    });
    tabNameLabel.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        setTabNameEditable(workspace.getName());
      }
    });

    setTabNameNotEditable(workspace.getName());
  }

  /** ワークスペース名を {@link #tabNameTextField} のテキストに変更する. */
  private void changeWsName() {
    Context context = notifService.beginWrite();
    try {
      workspace.setName(tabNameTextField.getText(), context.userOpe());
    } finally {
      notifService.endWrite();
    }
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

  /** タブ名を {@code tabName} に変更した上で編集不可能な状態にする. */
  private void setTabNameNotEditable(String name) {
    tabNameLabel.setVisible(true);
    tabNameLabel.setDisable(false);
    tabNameLabel.setText(name);
    tabNameTextField.setVisible(false);
    tabNameTextField.setDisable(true);
    tabNameTextField.setText("");
  }

  @Override
  public void specifyNodeViewAsRoot(BhNodeView view) {
    if (!nodeViews.contains(view)) {
      return;
    }
    rootNodeViews.remove(view);
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
    view.getCallbackRegistry().getOnMoved().add(cbRegistry.onNodeMoved);
    view.getCallbackRegistry().getOnSizeChanged().add(cbRegistry.onNodeSizeChanged);
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
    view.getCallbackRegistry().getOnMoved().remove(cbRegistry.onNodeMoved);
    view.getCallbackRegistry().getOnSizeChanged().remove(cbRegistry.onNodeSizeChanged);
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
      return new ArrayList<>();
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
        .map(QuadTreeRectangle::<BhNodeView>getUserData)
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
    if ((workspaceSizeLevel == BhConstants.Ui.MIN_WORKSPACE_SIZE_LEVEL) && !widen) {
      return;
    }
    if ((workspaceSizeLevel == BhConstants.Ui.MAX_WORKSPACE_SIZE_LEVEL) && widen) {
      return;
    }
    workspaceSizeLevel = widen ? workspaceSizeLevel + 1 : workspaceSizeLevel - 1;
    Vec2D currentSize = quadTreeMngForBody.getQtSpaceSize();
    double newWsWidth = widen ? currentSize.x * 2.0 : currentSize.x / 2.0;
    double newWsHeight = widen ? currentSize.y * 2.0 : currentSize.y / 2.0;

    wsPane.setMinSize(newWsWidth, newWsHeight);
    wsPane.setMaxSize(newWsWidth, newWsHeight);
    quadTreeMngForBody = new QuadTreeManager(
        quadTreeMngForBody, BhConstants.Ui.NUM_DIV_OF_QTREE_SPACE, newWsWidth, newWsHeight);
    quadTreeMngForConnector = new QuadTreeManager(
        quadTreeMngForConnector, BhConstants.Ui.NUM_DIV_OF_QTREE_SPACE, newWsWidth, newWsHeight);

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
      wsPane.getChildren().addFirst(new Line(x, 0, x, height));
    }
    for (int i = 0; i < numDiv; ++i) {
      int y = (int) ((height / numDiv) * i);
      wsPane.getChildren().addFirst(new Line(0, y, width, y));
    }
  }

  @Override
  public Vec2D sceneToWorkspace(double x, double y) {
    var pos = wsPane.sceneToLocal(x, y);
    return new Vec2D(pos.getX(), pos.getY());
  }

  @Override
  public Vec2D sceneToWorkspace(Vec2D pos) {
    return sceneToWorkspace(pos.x, pos.y);
  }

  @Override
  public void zoom(boolean zoomIn) {
    if ((BhConstants.Ui.MIN_ZOOM_LEVEL == zoomLevel) && !zoomIn) {
      return;
    }
    if ((BhConstants.Ui.MAX_ZOOM_LEVEL == zoomLevel) && zoomIn) {
      return;
    }
    Scale scale = new Scale();
    if (zoomIn) {
      ++zoomLevel;
    } else {
      --zoomLevel;
    }
    double mag = Math.pow(BhConstants.Ui.ZOOM_MAGNIFICATION, zoomLevel);
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
    double magX = wsPane.getTransforms().getFirst().getMxx();
    double magY = wsPane.getTransforms().getFirst().getMyy();
    // 全ノードの内の右端の最大の位置と下端の最大の位置
    Vec2D maxLowerRightPosOfNodes = rootNodeViews.stream()
        .map(nodeView -> {
          Vec2D nodeSize = nodeView.getRegionManager().getNodeTreeSize(false);
          Vec2D nodePos = nodeView.getPositionManager().getPosOnWorkspace();
          return new Vec2D(
            magX * (nodePos.x + nodeSize.x) + BhConstants.Ui.NODE_SCALE * 20,
            magY * (nodePos.y + nodeSize.y) + BhConstants.Ui.NODE_SCALE * 20);
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
    Bounds nodeBounds = view.getPositionManager().getBounds();
    lookAt(new Vec2D(nodeBounds.getCenterX(), nodeBounds.getCenterY()));
  }

  @Override
  public void lookAt(Vec2D pos) {
    if (getTabPane() == null) {
      return;
    }
    // スクロールバーを左上に移動させたときに, ワークスペースビューの中心が指すワークスペース上の位置
    var viewCenterAtMinScroll = new Vec2D(
        wsScrollPane.getWidth() * 0.5,
        wsScrollPane.getHeight() * 0.5);
    // スクロールバーを右下に移動させたときに, ワークスペースビューの中心が指すワークスペース上の位置
    var viewCenterAtMaxScroll = new Vec2D(
        wsWrapper.getWidth() - viewCenterAtMinScroll.x,
        wsWrapper.getHeight() - viewCenterAtMinScroll.y);
    var scrollDistance = new Vec2D(
        Math.max(1.0, viewCenterAtMaxScroll.x - viewCenterAtMinScroll.x),
        Math.max(1.0, viewCenterAtMaxScroll.y - viewCenterAtMinScroll.y));

    double magX = wsPane.getTransforms().getFirst().getMxx();
    double magY = wsPane.getTransforms().getFirst().getMyy();
    var scrollBarPos = new Vec2D(
        Math.clamp((pos.x * magX - viewCenterAtMinScroll.x) / scrollDistance.x, 0, 1),
        Math.clamp((pos.y * magY - viewCenterAtMinScroll.y) / scrollDistance.y, 0, 1));
    scrollBarPos = new Vec2D(
        scrollBarPos.x * (wsScrollPane.getHmax() - wsScrollPane.getHmin()) + wsScrollPane.getHmin(),
        scrollBarPos.y * (wsScrollPane.getVmax() - wsScrollPane.getVmin()) + wsScrollPane.getVmin()
    );
    wsScrollPane.setHvalue(scrollBarPos.x);
    wsScrollPane.setVvalue(scrollBarPos.y);
    getTabPane().getSelectionModel().select(this);
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

  @Override
  public boolean isPosInViewport(Vec2D pos) {
    var viewOriginAtMaxScroll = new Vec2D(
        Math.max(0, wsWrapper.getWidth() - wsScrollPane.getWidth()),
        Math.max(0, wsWrapper.getHeight() - wsScrollPane.getHeight()));

    double visibleLeft = viewOriginAtMaxScroll.x * wsScrollPane.getHvalue();
    double visibleTop = viewOriginAtMaxScroll.y * wsScrollPane.getVvalue();
    double visibleRight = visibleLeft + wsScrollPane.getWidth();
    double visibleBottom = visibleTop + wsScrollPane.getHeight();
    double magX = wsPane.getTransforms().getFirst().getMxx();
    double magY = wsPane.getTransforms().getFirst().getMyy();
    var magPos = new Vec2D(pos.x * magX, pos.y * magY);
    return visibleLeft <= magPos.x && magPos.x <= visibleRight
        && visibleTop <= magPos.y && magPos.y <= visibleBottom;
  }

  @Override
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /** {@link WorkspaceView} に対してイベントハンドラを追加または削除する機能を提供するクラス. */
  public class CallbackRegistryImpl implements CallbackRegistry {

    /** 関連するワークスペースビュー上でマウスボタンが押下されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<MouseEventInfo> onMousePressedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するワークスペースビュー上でマウスがドラッグされたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<MouseEventInfo> onMouseDraggedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するワークスペースビュー上でマウスボタンが離されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<MouseEventInfo> onMouseReleasedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するワークスペースビューのイベントフィルタを管理するオブジェクト. */
    private final ConsumerInvoker<UiEventInfo> eventFilters = new SimpleConsumerInvoker<>();

    /** 関連するワークスペースビューのノードビューの位置が変更されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeMoveEvent> onNodeMovedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するワークスペースビューのノードビューのサイズが変更されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeSizeChangedEvent> onNodeSizeChangedInvoke =
        new SimpleConsumerInvoker<>();

    /** 関連するワークスペースビューが閉じられたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<CloseEvent> onClosedInvoker = new SimpleConsumerInvoker<>();

    /** 関連するワークスペースビューを閉じるリクエストを受け取ったときのイベントハンドラ. */
    private Supplier<? extends Boolean> onCloseRequested = () -> true;

    /** 関連するワークスペースビュー上でノードビューが移動したときのイベントハンドラ. */
    private final Consumer<? super BhNodeView.MoveEvent> onNodeMoved = this::onNodeMoved;

    /** 関連するワークスペースビュー上でノードビューのサイズが変更されたときのイベントハンドラ. */
    private final Consumer<? super BhNodeView.SizeChangedEvent> onNodeSizeChanged =
        this::onNodeSizeChanged;

    private void setEventHandlers() {
      wsPane.addEventHandler(
          MouseEvent.MOUSE_PRESSED,
          event -> {
            wsPane.requestFocus();
            onMousePressedInvoker.invoke(new MouseEventInfo(FxmlWorkspaceView.this, event));
            consume(event);
          });
      wsPane.addEventHandler(
          MouseEvent.MOUSE_DRAGGED,
          event -> {
            onMouseDraggedInvoker.invoke(new MouseEventInfo(FxmlWorkspaceView.this, event));
            consume(event);
          });
      wsPane.addEventHandler(
          MouseEvent.MOUSE_RELEASED,
          event -> {
            onMouseReleasedInvoker.invoke(new MouseEventInfo(FxmlWorkspaceView.this, event));
            consume(event);
          });
      wsPane.addEventFilter(
          Event.ANY,
          event -> eventFilters.invoke(new UiEventInfo(FxmlWorkspaceView.this, event)));
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
    public ConsumerInvoker<MouseEventInfo>.Registry getOnMouseReleased() {
      return onMouseReleasedInvoker.getRegistry();
    }

    @Override
    public ConsumerInvoker<UiEventInfo>.Registry eventFilters() {
      return eventFilters.getRegistry();
    }

    @Override
    public ConsumerInvoker<NodeMoveEvent>.Registry getOnNodeMoved() {
      return onNodeMovedInvoker.getRegistry();
    }

    @Override
    public ConsumerInvoker<NodeSizeChangedEvent>.Registry getOnNodeSizeChanged() {
      return onNodeSizeChangedInvoke.getRegistry();
    }

    @Override
    public ConsumerInvoker<CloseEvent>.Registry getOnClosed() {
      return onClosedInvoker.getRegistry();
    }

    @Override
    public void setOnCloseRequested(Supplier<? extends Boolean> handler) {
      if (handler == null) {
        onCloseRequested = () -> true;
        return;
      }
      onCloseRequested = handler;
    }

    /** 関連するワークスペースビューを閉じるリクエストを受け取ったときのイベントハンドラ. */
    private void onCloseRequested(Event event) {
      if (!onCloseRequested.get()) {
        event.consume();
      }
      TabPane tabPane = getTabPane();
      if (tabPane != null) {
        // このイベントハンドラを抜けるとき, TabDragPolicy.FIXED にしないと
        // タブ消しをキャンセルした後, そのタブが使えなくなる.
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);
      }
    }

    /** 関連するワークスペースビュー上で {@link BhNodeView} が移動したときのイベントハンドラを呼び出す. */
    private void onNodeMoved(BhNodeView.MoveEvent event) {
      onNodeMovedInvoker.invoke(new NodeMoveEvent(FxmlWorkspaceView.this, event.view()));
    }

    /** 関連するワークスペースビュー上で {@link BhNodeView} が移動したときのイベントハンドラを呼び出す. */
    private void onNodeSizeChanged(BhNodeView.SizeChangedEvent event) {
      onNodeSizeChangedInvoke.invoke(
          new NodeSizeChangedEvent(FxmlWorkspaceView.this, event.view()));
    }

    /** ワークスペースビューが閉じられたときのイベントハンドラを呼び出す. */
    private void onClosed(Event event) {
      onClosedInvoker.invoke(new CloseEvent(FxmlWorkspaceView.this));
    }

    /** {@code event} のターゲットが {@link #wsPane} であった場合 {@code event} を consume する. */
    private void consume(MouseEvent event) {
      if (event.getTarget() == wsPane) {
        event.consume();
      }
    }    
  }
}
