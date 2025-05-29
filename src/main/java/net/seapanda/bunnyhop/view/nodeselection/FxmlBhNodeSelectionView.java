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

package net.seapanda.bunnyhop.view.nodeselection;

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
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.service.LogManager;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.view.ViewConstructionException;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle.ConnectorPos;

/**
 * テンプレートノードを表示するビュー.
 *
 * @author K.Koike
 */
public final class FxmlBhNodeSelectionView extends ScrollPane implements BhNodeSelectionView {

  @FXML private Pane nodeSelectionPanel;  // FXML で Pane 以外使わないこと
  @FXML private Pane nodeSelectionPanelWrapper;
  @FXML private ScrollPane nodeSelectionPanelBase;

  private final SequencedSet<BhNodeView> rootNodeViews = new LinkedHashSet<>();
  private final SequencedSet<BhNodeView> nodeViews = new LinkedHashSet<>();
  private int zoomLevel = 0;
  private final String categoryName;
  private final Consumer<? super BhNodeView> onNodeSizeUpdated =
      nodeView -> {
        if (isVisible()) {
          arrange();
        }
      };

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

    nodeSelectionPanel.getTransforms().add(new Scale());
    getStyleClass().add(cssClass);
    nodeSelectionPanel.getStyleClass().add(cssClass);
    nodeSelectionPanelWrapper.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    nodeSelectionPanelWrapper.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
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
    if (rootNodeViews.contains(view)) {
      rootNodeViews.remove(view);
    }
    rootNodeViews.addLast(view);
    view.getLookManager().arrange();
    view.getPositionManager().setTreeZpos(0);
  }

  @Override
  public void specifyNodeViewAsNotRoot(BhNodeView view) {
    rootNodeViews.remove(view);
  }

  @Override
  public void addNodeViewTree(BhNodeView view) {
    if (nodeViews.contains(view)) {
      return;
    }
    nodeViews.add(view);
    view.getTreeManager().addToGuiTree(nodeSelectionPanel);
    view.getEventManager().addOnNodeSizeChanged(onNodeSizeUpdated);
  }

  @Override
  public void removeNodeViewTree(BhNodeView view) {
    if (!nodeViews.contains(view)) {
      return;
    }
    specifyNodeViewAsNotRoot(view);
    nodeViews.remove(view);
    view.getTreeManager().removeFromGuiTree();
    view.getEventManager().removeOnNodeSizeChanged(onNodeSizeUpdated);
  }

  @Override
  public long getNumNodeViewTrees() {
    return rootNodeViews.stream()
        .filter(root -> !isSpaceNodeView(root))
        .count();
  }

  /** {@code view} が空白を開けるための {@link BhNodeView} であるかどうか調べる. */
  private boolean isSpaceNodeView(BhNodeView view) {
    return view.getModel().map(
        model -> model.getSymbolName().equals(BhConstants.NodeTemplate.SELECTION_VIEW_SPACE))
        .orElse(false);
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
    nodeSelectionPanel.getTransforms().clear();
    nodeSelectionPanel.getTransforms().add(scale);
    adjustWrapperSize(nodeSelectionPanel.getWidth(), nodeSelectionPanel.getHeight());
  }

  @Override
  public void arrange() {
    double panelWidth = 0.0;
    double panelHeight = 0.0;
    double offset = nodeSelectionPanel.getPadding().getTop();
    final double leftPadding = nodeSelectionPanel.getPadding().getLeft();
    final double rightPadding = nodeSelectionPanel.getPadding().getRight();
    final double topPadding = nodeSelectionPanel.getPadding().getTop();
    final double bottomPadding = nodeSelectionPanel.getPadding().getBottom();

    for (BhNodeView nodeView : rootNodeViews) {
      Vec2D cnctrSize = nodeView.getRegionManager().getConnectorSize();
      if (nodeView.getLookManager().getConnectorPos() == ConnectorPos.TOP) {
        nodeView.getPositionManager().setTreePosOnWorkspace(leftPadding, offset + cnctrSize.y);
      } else if (nodeView.getLookManager().getConnectorPos() == ConnectorPos.LEFT) {
        nodeView.getPositionManager().setTreePosOnWorkspaceByConnector(
            leftPadding - cnctrSize.x, offset);
      }
      Vec2D treeSizeWithCnctr = nodeView.getRegionManager().getNodeTreeSize(true);
      offset += treeSizeWithCnctr.y + BhConstants.LnF.BHNODE_SPACE_ON_SELECTION_PANEL;
      panelWidth = Math.max(panelWidth, treeSizeWithCnctr.x);
    }
    panelHeight =
        (offset - BhConstants.LnF.BHNODE_SPACE_ON_SELECTION_PANEL) + topPadding + bottomPadding;
    panelWidth += rightPadding + leftPadding;
    nodeSelectionPanel.setMinSize(panelWidth, panelHeight);
    //バインディングではなく, ここでこのメソッドを呼ばないとスクロールバーの稼働域が変わらない
    adjustWrapperSize(panelWidth, panelHeight);
  }

  /**
   * スクロールバーの可動域が変わるようにノード選択パネルのラッパーのサイズを変更する.
   *
   * @param panelWidth ノード選択パネルの幅
   * @param panelHeight ノード選択パネルの高さ
   */
  private void adjustWrapperSize(double panelWidth, double panelHeight) {
    double wrapperSizeX = panelWidth * nodeSelectionPanel.getTransforms().get(0).getMxx();
    double wrapperSizeY = panelHeight * nodeSelectionPanel.getTransforms().get(0).getMyy();
    // スクロール時にスクロールバーの可動域が変わるようにする
    nodeSelectionPanelWrapper.setPrefSize(wrapperSizeX, wrapperSizeY);
    Node wsSetTab = Optional.ofNullable(nodeSelectionPanelBase.getScene())
        .map(scene -> scene.lookup("#" + BhConstants.UiId.WORKSPACE_SET_TAB))
        .orElse(null);
    double maxWidth = wrapperSizeX + nodeSelectionPanelBase.getPadding().getLeft()
        + nodeSelectionPanelBase.getPadding().getRight();
    if (wsSetTab != null) {
      maxWidth = Math.min(maxWidth, ((TabPane) wsSetTab).getWidth() * 0.5);
    }
    nodeSelectionPanelBase.setMaxWidth(maxWidth);
    // 選択ビューの幅を設定後にレイアウトしないと適切な幅で表示されない
    Platform.runLater(nodeSelectionPanelBase::requestLayout);
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
}
