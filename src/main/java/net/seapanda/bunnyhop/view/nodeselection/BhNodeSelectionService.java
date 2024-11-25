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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.seapanda.bunnyhop.control.nodeselection.BhNodeSelectionController;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.service.BhNodeHandler;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * ノード選択ビューの操作を公開するクラス.
 *
 * @author K.Koike
 */
public class BhNodeSelectionService {

  public static final BhNodeSelectionService INSTANCE = new BhNodeSelectionService();
  private final Map<String, BhNodeSelectionView> categoryNameToSelectionView = new HashMap<>();
  private final Map<String, Workspace> categoryNameToWorkspace = new HashMap<>();

  /** コンストラクタ. */
  private BhNodeSelectionService() {}

  /**
   * ノード選択ビューを登録する.
   *
   * @param view 登録するビュー.
   */
  public void registerView(BhNodeSelectionView view) {
    String categoryName = view.getCategoryName();
    categoryNameToSelectionView.put(categoryName, view);
    var model = new Workspace(categoryName);
    var controller = new BhNodeSelectionController(model, view);
    model.setMsgProcessor(controller);
    categoryNameToWorkspace.put(categoryName, model);
  }

  /**
   * 引数で指定したカテゴリにテンプレートノードを追加する.
   *
   * <p> {@code node} の MVC が構築されていること. </p>
   *
   * @param categoryName {@code node} を追加するカテゴリの名前
   * @param node 追加するノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addTemplateNode(String categoryName, BhNode node, UserOperation userOpe) {
    Workspace ws = categoryNameToWorkspace.get(categoryName);
    if (ws == null) {
      return;
    }
    BhNodeHandler.INSTANCE.moveToWs(ws, node, 0, 0, userOpe);
  }

  /**
   * 引数で指定したカテゴリのノード選択ビューにある全てのノードを取得する.
   *
   * <p> 返されるリストのノードは, {@code addTemplateNode} で追加したノードである. </p>
   *
   * @param categoryName このカテゴリのテンプレートノードを全て取得する
   * @return {@code categoryName} にある全てのテンプレートノード. 登録されていないカテゴリを指定した場合は空のリスト.
   */
  public Collection<BhNode> getTemplateNodes(String categoryName) {
    Workspace ws = categoryNameToWorkspace.get(categoryName);
    if (ws == null) {
      return new ArrayList<BhNode>();
    }
    return ws.getRootNodeList();
  }

  /**
   * 引数で指定したカテゴリのノード選択ビューにある全てのノードを削除する.
   *
   * @param categoryName このカテゴリのテンプレートノードを全て削除する
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void deleteAllNodes(String categoryName, UserOperation userOpe) {
    Collection<BhNode> nodesToDelete = getTemplateNodes(categoryName);
    List<Swapped> swappedNodes =
        BhNodeHandler.INSTANCE.deleteNodes(nodesToDelete, userOpe);
    for (var swapped : swappedNodes) {
      swapped.newNode().findParentNode().getEventAgent().execOnChildReplaced(
          swapped.oldNode(),
          swapped.newNode(),
          swapped.newNode().getParentConnector(),
          userOpe);
    }
  }

  /**
   * ノード選択ビューを全て拡大もしくは縮小する.
   *
   * @param zoomIn 拡大する場合 true
   */
  public void zoomAll(boolean zoomIn) {
    categoryNameToSelectionView.values().forEach(view -> view.zoom(zoomIn));
  }

  /** 全てのノード選択ビューを隠す. */
  public void hideAll() {
    categoryNameToSelectionView.values().forEach(view -> view.setVisible(false));
  }

  /** {@code categoryName} で指定したカテゴリのノード選択ビューを表示する. */
  public void show(String categoryName) {
    BhNodeSelectionView view = categoryNameToSelectionView.get(categoryName);
    if (view == null) {
      return;
    }
    hideAll();
    view.setVisible(true);
  }

  /**
   * 現在表示されているカテゴリの名前を返す.
   *
   * @return 現在表示されているカテゴリの名前. 表示されているカテゴリがない場合は empty.
   */
  public Optional<String> getNameOfShowedCategory() {
    return categoryNameToSelectionView
        .values().stream()
        .filter(view -> view.visibleProperty().get())
        .findFirst()
        .map(view -> view.getCategoryName());
  }

  /**
   * ノード選択ビューのうち表示されているものがあるかどうか調べる.
   *
   * @return BhNode選択パネルのうち一つでも表示されている場合true
   */
  public boolean isAnyShowed() {
    return categoryNameToSelectionView.values().stream()
        .anyMatch(view -> view.visibleProperty().get());
  }

  /**
   * 引数で指定したカテゴリのノード選択ビューが表示されているかどうかを調べる.
   *
   * @param categoryName 表示状態を調べるノード選択ビューのカテゴリ名
   * @return 表示されている場合 true. 批評の場合と {@code categoryName} に対応するビューが見つからなかった場合は false.
   */
  public boolean isShowed(String categoryName) {

    BhNodeSelectionView view = categoryNameToSelectionView.get(categoryName);
    if (view == null) {
      return false;
    }
    return view.isVisible();
  }
}
