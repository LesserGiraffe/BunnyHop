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
import javafx.scene.transform.Scale;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.ViewInitializationException;
import net.seapanda.bunnyhop.view.node.BhNodeView;

/**
 * ノードカテゴリ選択時にワークスペース上に現れる {@link BhNode} 選択パネル.
 *
 * @author K.Koike
 */
public final class BhNodeSelectionView extends ScrollPane {

  @FXML Pane nodeSelectionPanel;  //FXML で Pane 以外使わないこと
  @FXML Pane nodeSelectionPanelWrapper;
  @FXML ScrollPane nodeSelectionPanelBase;
  private final SequencedSet<BhNodeView> rootNodeList = new LinkedHashSet<>();
  private int zoomLevel = 0;
  private final String categoryName;
  private final Consumer<BhNodeView> onNodeSizeChanged =
      nodeView -> {
        if (isVisible()) {
          arrange();
        }
      };

  /**
   * GUI コンポーネントを初期化する.
   *
   * @param categoryName このビューに関連付けられたカテゴリ名
   * @param cssClass ビューに適用する css クラス名
   * @param categoryListView このビューを保持しているカテゴリリストのビュー
   */
  public BhNodeSelectionView(
      String categoryName,
      String cssClass,
      BhNodeCategoryListView categoryListView)
      throws ViewInitializationException {
    this.categoryName = categoryName;
    try {
      Path filePath =
          BhService.fxmlCollector().getFilePath(BhConstants.Path.NODE_SELECTION_PANEL_FXML);
      FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
      loader.setController(this);
      loader.setRoot(this);
      loader.load();
    } catch (IOException e) {
      BhService.msgPrinter().errForDebug("category : %s\n%s".formatted(categoryName, e));
      throw new ViewInitializationException(
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

  /**
   * このビューのカテゴリ名を取得する.
   *
   * @return このビューのカテゴリ名
   */
  public String getCategoryName() {
    return categoryName;
  }

  /**
   * {@code root} 以下のノードをノード選択リストに追加する.
   *
   * @param root ノード選択リストに追加する {@link BhNodeView}
   */
  public void addNodeViewTree(BhNodeView root) {
    root.getTreeManager().addToGuiTree(nodeSelectionPanel);
    root.getEventManager().addOnNodeSizesInTreeChanged(onNodeSizeChanged);
    rootNodeList.add(root);
  }

  /**
   * {@code root} 以下のノードをノード選択リストから削除する.
   *
   * @param root ノード選択リストから削除する {@link BhNodeView}
   */
  public void removeNodeViewTree(BhNodeView root) {
    if (rootNodeList.contains(root)) {
      root.getTreeManager().removeFromGuiTree();
      root.getEventManager().removeOnNodeSizesInTreeChanged(onNodeSizeChanged);
      rootNodeList.remove(root);
      if (getNumNodeViewTrees() == 0 && isShowed()) {
        hide();
      }
    }
  }

  /** ノード選択リストに追加されている {@link BhNodeView} を全て削除する. */
  public void removeAll() {
    while (rootNodeList.size() != 0) {
      removeNodeViewTree(rootNodeList.getLast());
    }
  }

  /** このオブジェクトが現在保持する {@link BhNodeView} のツリーの数を取得する. */
  public long getNumNodeViewTrees() {
    return rootNodeList.stream()
        .filter(root -> !isSpaceNodeView(root))
        .count();
  }

  /** {@code view} が空白を開けるための {@link BhNodeView} であるかどうか調べる. */
  private boolean isSpaceNodeView(BhNodeView view) {
    return view.getModel().map(
        model -> model.getSymbolName().equals(BhConstants.NodeTemplate.SELECTION_VIEW_SPACE))
        .orElse(false);
  }

  /**
   * ノード選択ビューのズーム処理を行う.
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
    nodeSelectionPanel.getTransforms().clear();
    nodeSelectionPanel.getTransforms().add(scale);
    adjustWrapperSize(nodeSelectionPanel.getWidth(), nodeSelectionPanel.getHeight());
  }

  /** 表示するノードを並べる. */
  public void arrange() {
    double panelWidth = 0.0;
    double panelHeight = 0.0;
    double offset = nodeSelectionPanel.getPadding().getTop();
    final double leftPadding = nodeSelectionPanel.getPadding().getLeft();
    final double rightPadding = nodeSelectionPanel.getPadding().getRight();
    final double topPadding = nodeSelectionPanel.getPadding().getTop();
    final double bottomPadding = nodeSelectionPanel.getPadding().getBottom();

    for (int i = 0; i < nodeSelectionPanel.getChildren().size(); ++i) {
      Node node = nodeSelectionPanel.getChildren().get(i);
      if (!(node instanceof BhNodeView)) {
        continue;
      }
      BhNodeView nodeToShift = (BhNodeView) node;
      if (nodeToShift.getTreeManager().getParentView() != null) {
        continue;
      }
      Vec2D wholeBodySize = nodeToShift.getRegionManager().getNodeSizeIncludingOuters(true);
      Vec2D bodySize = nodeToShift.getRegionManager().getNodeSizeIncludingOuters(false);
      double upperCnctrHeight = wholeBodySize.y - bodySize.y;
      nodeToShift.getPositionManager().setPosOnWorkspace(leftPadding, offset + upperCnctrHeight);
      offset += wholeBodySize.y + BhConstants.LnF.BHNODE_SPACE_ON_SELECTION_PANEL;
      panelWidth = Math.max(panelWidth, wholeBodySize.x);
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
        .map(scene -> scene.lookup("#" + BhConstants.Fxml.ID_WORKSPACE_SET_TAB))
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

  /**
   * このオブジェクトに登録したノードをリストに格納して返す.
   *
   * @return このオブジェクトに登録したノードのリスト.
   */
  public SequencedSet<BhNodeView> getNodeViewList() {
    return new LinkedHashSet<>(rootNodeList);
  }

  /**
   * このビューを表示する.
   */
  public void show() {
    setVisible(true);
  }

  /**
   * このビューを非表示にする.
   */
  public void hide() {
    setVisible(false);
  }

  /**
   * このビューの現在の可視性を取得する.
   *
   * @return このビューの可視性.
   */
  public boolean isShowed() {
    return visibleProperty().get();
  }
}
