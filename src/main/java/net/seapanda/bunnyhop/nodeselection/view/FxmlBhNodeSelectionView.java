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
import net.seapanda.bunnyhop.common.configuration.BhSettings;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.BhNodeView.PositionManager;
import net.seapanda.bunnyhop.node.view.BhNodeView.SizeChangedEvent;
import net.seapanda.bunnyhop.node.view.style.ConnectorPos;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.utility.math.Vec2D;

/**
 * テンプレートノードを表示するビュー.
 *
 * @author K.Koike
 */
public final class FxmlBhNodeSelectionView extends ScrollPane implements BhNodeSelectionView {

  @FXML private Pane nodeSelectionView;  // FXML で Pane 以外使わないこと
  @FXML private Pane nodeSelectionViewWrapper;
  @FXML private ScrollPane nodeSelectionViewBase;

  private final SequencedSet<BhNodeView> rootNodeViews = new LinkedHashSet<>();
  private final SequencedSet<BhNodeView> nodeViews = new LinkedHashSet<>();
  private int zoomLevel = 0;
  private final String categoryName;
  private final Consumer<? super SizeChangedEvent> onNodeSizeChanged =
      event -> requestArrangementIfVisible();
  /** ノードの整列を要求されている状態かどうかのフラグ. */
  private boolean isArrangementRequested = false;

  /**
   * GUI コンポーネントを初期化する.
   *
   * @param filePath ノード選択ビューが定義された fxml ファイルのパス
   * @param categoryName このビューに関連付けられたカテゴリ名
   * @param cssClass ビューに適用する css クラス名
   */
  public FxmlBhNodeSelectionView(
      Path filePath,
      String categoryName,
      String cssClass)
      throws ViewConstructionException {
    this.categoryName = categoryName;
    try {
      FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
      loader.setController(this);
      loader.setRoot(this);
      loader.load();
    } catch (IOException e) {
      LogManager.logger().error("category : %s\n%s".formatted(categoryName, e));
      throw new ViewConstructionException(
          "Failed to initialize " + BhNodeSelectionView.class.getSimpleName());
    }

    nodeSelectionView.getTransforms().add(new Scale());
    getStyleClass().add(cssClass);
    nodeSelectionView.getStyleClass().add(cssClass);
    nodeSelectionViewWrapper.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    nodeSelectionViewWrapper.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
    visibleProperty().addListener((observable, oldVal, newVal) -> arrange());
    hide();
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
  public void specifyNodeViewAsRoot(BhNodeView view) {
    if (!nodeViews.contains(view)) {
      return;
    }
    rootNodeViews.remove(view);
    rootNodeViews.addLast(view);
    view.getLookManager().arrange();
    view.getPositionManager().setTreeZpos(0);
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
    view.getTreeManager().addToGuiTree(nodeSelectionView);
    view.getCallbackRegistry().getOnSizeChanged().add(onNodeSizeChanged);
  }

  @Override
  public void removeNodeView(BhNodeView view) {
    if (!nodeViews.contains(view)) {
      return;
    }
    specifyNodeViewAsNotRoot(view);
    nodeViews.remove(view);
    view.getTreeManager().removeFromGuiTree();
    view.getCallbackRegistry().getOnSizeChanged().remove(onNodeSizeChanged);
  }

  @Override
  public long getNumNodeViewTrees() {
    return rootNodeViews.size();
  }

  @Override
  public void zoom(boolean zoomIn) {
    int level = zoomIn ? zoomLevel + 1 : zoomLevel - 1;
    zoom(level);
  }

  @Override
  public void zoom(int level) {
    zoomLevel = Math.clamp(level, BhConstants.Ui.MIN_ZOOM_LEVEL, BhConstants.Ui.MAX_ZOOM_LEVEL);
    double mag = Math.pow(BhConstants.Ui.ZOOM_MAGNIFICATION, zoomLevel);
    Scale scale = new Scale(mag, mag);
    nodeSelectionView.getTransforms().set(0, scale);
    adjustWrapperSize(nodeSelectionView.getWidth(), nodeSelectionView.getHeight());
    BhSettings.Ui.currentNodeSelectionViewZoomLevel = zoomLevel;
  }

  @Override
  public void arrange() {
    final Padding padding = new Padding(nodeSelectionView.getPadding());
    double offset = padding.top;
    double maxWidth = 0.0;

    for (BhNodeView nodeView : rootNodeViews) {
      positionNodeView(nodeView, offset, padding.left);
      Vec2D treeSize = nodeView.getRegionManager().getNodeTreeSize(true);
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
    Vec2D cnctrSize = nodeView.getRegionManager().getConnectorSize();
    ConnectorPos connectorPos = nodeView.getLookManager().getConnectorPos();
    PositionManager posManager = nodeView.getPositionManager();
    if (connectorPos == ConnectorPos.TOP) {
      posManager.setTreePosOnWorkspace(leftPadding, offset + cnctrSize.y);
    } else if (connectorPos == ConnectorPos.LEFT) {
      posManager.setTreePosOnWorkspaceByUpperLeft(leftPadding - cnctrSize.x, offset);
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
    BhSettings.Ui.currentNodeSelectionViewZoomLevel = zoomLevel;
  }

  @Override
  public void hide() {
    setVisible(false);
  }

  @Override
  public boolean isShowed() {
    return visibleProperty().get();
  }

  /** パディング情報を保持するレコード. */
  private record Padding(double left, double right, double top, double bottom) {
    Padding(javafx.geometry.Insets insets) {
      this(insets.getLeft(), insets.getRight(), insets.getTop(), insets.getBottom());
    }
  }
}
