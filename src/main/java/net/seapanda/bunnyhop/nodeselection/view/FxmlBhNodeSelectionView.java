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

package net.seapanda.bunnyhop.nodeselection.view;

import static net.seapanda.bunnyhop.node.view.BhNodeView.Geometry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.BhNodeView.SizeChangedEvent;
import net.seapanda.bunnyhop.node.view.style.ConnectorOrientation;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.SimpleConsumerInvoker;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.workspace.model.Workspace;

/**
 * テンプレートノードを表示するビュー.
 *
 * @author K.Koike
 */
public final class FxmlBhNodeSelectionView extends ScrollPane implements BhNodeSelectionView {

  @FXML private BhNodeSelectionViewPane nodeSelectionView;
  @FXML private Pane nodeSelectionViewWrapper;
  @FXML private ScrollPane nodeSelectionViewBase;

  private final SequencedSet<BhNodeView> rootNodeViews = new LinkedHashSet<>();
  private final SequencedSet<BhNodeView> nodeViews = new LinkedHashSet<>();
  private final Workspace ws;
  private final String categoryName;
  private final CallbackRegistryImpl cbRegistry = new CallbackRegistryImpl();
  private final Consumer<? super SizeChangedEvent> onNodeSizeChanged =
      event -> requestArrangementIfVisible();

  /** ノードの整列を要求されている状態かどうかのフラグ. */
  private boolean isArrangementRequested = false;

  /**
   * GUI コンポーネントを初期化し, 各種イベントハンドラをセットする.
   *
   * @param filePath ノード選択ビューが定義された fxml ファイルのパス
   * @param categoryName このビューに関連付けられたカテゴリ名
   * @param ws テンプレートノードを保持するワークスペース.
   * @param cssClass ビューに適用する css クラス名
   */
  public FxmlBhNodeSelectionView(
      Path filePath,
      String categoryName,
      Workspace ws,
      String cssClass)
      throws ViewConstructionException {
    this.ws = ws;
    this.categoryName = categoryName;
    try {
      FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
      loader.setController(this);
      loader.setRoot(this);
      loader.load();
    } catch (IOException e) {
      String msg = "Failed to construct a node selection view.  (%s)".formatted(categoryName);
      LogManager.logger().error(msg, e);
      throw new ViewConstructionException(msg, e);
    }
    nodeSelectionView.setContainer(this);
    nodeSelectionView.getTransforms().add(new Scale());
    getStyleClass().add(cssClass);
    nodeSelectionView.getStyleClass().add(cssClass);
    nodeSelectionViewWrapper.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    nodeSelectionViewWrapper.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
    setEventHandlers();
    hide();
  }

  private void setEventHandlers() {
    Workspace.CallbackRegistry registry = ws.getCallbackRegistry();
    registry.getOnNodeAdded().add(event -> event.node().getView().ifPresent(this::addNodeView));
    registry.getOnNodeRemoved().add(
        event -> event.node().getView().ifPresent(this::removeNodeView));
    registry.getOnRootNodeAdded().add(
        event -> event.node().getView().ifPresent(this::specifyNodeViewAsRoot));
    registry.getOnRootNodeRemoved().add(
        event -> event.node().getView().ifPresent(this::specifyNodeViewAsNotRoot));

    visibleProperty().addListener((observable, oldVal, newVal) -> {
      arrange();
      cbRegistry.onVisibilityChangedInvoker.invoke(new VisibilityChangedEvent(this, newVal));
    });
  }

  /**
   * このノード選択ビューに対し {@code view} をルートとして指定する.
   *
   * @param view ルートとして指定するビュー
   */
  private void specifyNodeViewAsRoot(BhNodeView view) {
    if (!nodeViews.contains(view)) {
      return;
    }
    rootNodeViews.remove(view);
    rootNodeViews.addLast(view);
    view.getArrangement().arrange();
    view.getGeometry().setTreeZposition(0);
  }

  /**
   * このノード選択ビューに対し {@code view} を非ルートノードとして指定する.
   *
   * @param view 非ルートとして指定するビュー
   */
  private void specifyNodeViewAsNotRoot(BhNodeView view) {
    rootNodeViews.remove(view);
  }


  @Override
  public Region getRegion() {
    return this;
  }

  @Override
  public String getCategoryName() {
    return categoryName;
  }

  @Override
  public Workspace getWorkspace() {
    return ws;
  }

  @Override
  public void addNodeView(BhNodeView view) {
    if (nodeViews.contains(view)) {
      return;
    }
    nodeViews.add(view);
    view.getTreeControl().addToTree(nodeSelectionView);
    view.getCallbackRegistry().getOnSizeChanged().add(onNodeSizeChanged);
    cbRegistry.onNodeAddedInvoker.invoke(new NodeAddedEvent(this, view));
  }

  @Override
  public void removeNodeView(BhNodeView view) {
    if (!nodeViews.contains(view)) {
      return;
    }
    specifyNodeViewAsNotRoot(view);
    nodeViews.remove(view);
    view.getTreeControl().removeFromTree();
    view.getCallbackRegistry().getOnSizeChanged().remove(onNodeSizeChanged);
    cbRegistry.onNodeRemovedInvoker.invoke(new NodeRemovedEvent(this, view));
  }

  @Override
  public long getNumNodeViewTrees() {
    return rootNodeViews.size();
  }

  @Override
  public void zoom(int level) {
    level = Math.clamp(level, BhConstants.Ui.MIN_ZOOM_LEVEL, BhConstants.Ui.MAX_ZOOM_LEVEL);
    double mag = Math.pow(BhConstants.Ui.ZOOM_MAGNIFICATION, level);
    Scale scale = new Scale(mag, mag);
    nodeSelectionView.getTransforms().set(0, scale);
    adjustWrapperSize(nodeSelectionView.getWidth(), nodeSelectionView.getHeight());
  }

  @Override
  public void arrange() {
    final Padding padding = new Padding(nodeSelectionView.getPadding());
    double offset = padding.top;
    double maxWidth = 0.0;

    for (BhNodeView nodeView : rootNodeViews) {
      positionNodeView(nodeView, offset, padding.left);
      Vec2D treeSize = nodeView.getGeometry().getNodeTreeSize(true);
      offset += treeSize.y + BhConstants.Ui.BHNODE_SPACE_ON_SELECTION_VIEW;
      maxWidth = Math.max(maxWidth, treeSize.x);
    }
    double width = maxWidth + padding.left + padding.right;
    double height = calculateHeight(offset, padding);
    nodeSelectionView.setMinSize(width, height);
    adjustWrapperSize(width, height);
  }

  /** ノードビューを配置する. */
  private void positionNodeView(BhNodeView nodeView, double offset, double leftPadding) {
    Geometry geometry = nodeView.getGeometry();
    ConnectorOrientation connectorPos = geometry.getConnectorOrientation();
    Vec2D cnctrSize = nodeView.getGeometry().getConnectorSize();
    if (connectorPos == ConnectorOrientation.TOP) {
      geometry.setTreePosition(leftPadding, offset + cnctrSize.y);
    } else if (connectorPos == ConnectorOrientation.LEFT) {
      geometry.setTreePositionByUpperLeft(leftPadding - cnctrSize.x, offset);
    }
  }

  /** ノード選択ビューの高さを計算する. */
  private double calculateHeight(double offset, Padding padding) {
    return (offset - BhConstants.Ui.BHNODE_SPACE_ON_SELECTION_VIEW) + padding.top + padding.bottom;
  }

  /**
   * スクロールバーの可動域が変わるようにノード選択ビューのラッパーのサイズを変更する.
   *
   * @param width ノード選択ビューの幅
   * @param height ノード選択ビューの高さ
   */
  private void adjustWrapperSize(double width, double height) {
    double wrapperSizeX = width * nodeSelectionView.getTransforms().getFirst().getMxx();
    double wrapperSizeY = height * nodeSelectionView.getTransforms().getFirst().getMyy();
    // スクロール時にスクロールバーの可動域が変わるようにする
    nodeSelectionViewWrapper.setPrefSize(wrapperSizeX, wrapperSizeY);
    Node wsSetTab = Optional.ofNullable(nodeSelectionViewBase.getScene())
        .map(scene -> scene.lookup("#" + BhConstants.UiId.WORKSPACE_SET_TAB))
        .orElse(null);
    double maxWidth = wrapperSizeX + nodeSelectionViewBase.getPadding().getLeft()
        + nodeSelectionViewBase.getPadding().getRight();
    if (wsSetTab != null) {
      maxWidth = Math.min(maxWidth, ((TabPane) wsSetTab).getWidth() * 0.5);
    }
    nodeSelectionViewBase.setMaxWidth(maxWidth);
    // 選択ビューの幅を設定後にレイアウトしないと適切な幅で表示されない
    Platform.runLater(nodeSelectionViewBase::requestLayout);
  }


  /** このノード選択ビューが見えている場合, 保持しているノードビューの並べ替えをリクエストする. */
  private void requestArrangementIfVisible() {
    if (isVisible() && !isArrangementRequested) {
      Platform.runLater(() -> {
        arrange();
        isArrangementRequested = false;
      });
      isArrangementRequested = true;
    }
  }

  @Override
  public SequencedSet<BhNodeView> getNodeViewList() {
    return new LinkedHashSet<>(rootNodeViews);
  }

  @Override
  public void show() {
    setVisible(true);
  }

  @Override
  public void hide() {
    setVisible(false);
  }

  @Override
  public boolean isShowed() {
    return visibleProperty().get();
  }

  @Override
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /** パディング情報を保持するレコード. */
  private record Padding(double left, double right, double top, double bottom) {
    Padding(javafx.geometry.Insets insets) {
      this(insets.getLeft(), insets.getRight(), insets.getTop(), insets.getBottom());
    }
  }

  /** {@link FxmlBhNodeSelectionView} に対してイベントハンドラを追加または削除する機能を提供するクラス. */
  static class CallbackRegistryImpl implements CallbackRegistry {

    /** 関連するノード選択ビューにノードビューが追加されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeAddedEvent> onNodeAddedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するノード選択ビューからノードビューが削除されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<NodeRemovedEvent> onNodeRemovedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するノード選択ビューの可視性が変更されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<VisibilityChangedEvent> onVisibilityChangedInvoker =
        new SimpleConsumerInvoker<>();


    @Override
    public ConsumerInvoker<NodeAddedEvent>.Registry getOnNodeAdded() {
      return onNodeAddedInvoker.getRegistry();
    }

    @Override
    public ConsumerInvoker<NodeRemovedEvent>.Registry getOnNodeRemoved() {
      return onNodeRemovedInvoker.getRegistry();
    }

    @Override
    public ConsumerInvoker<VisibilityChangedEvent>.Registry getOnVisibilityChanged() {
      return onVisibilityChangedInvoker.getRegistry();
    }
  }
}
