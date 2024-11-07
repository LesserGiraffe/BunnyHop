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

package net.seapanda.bunnyhop.model.workspace;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import net.seapanda.bunnyhop.common.constant.VersionInfo;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgDispatcher;
import net.seapanda.bunnyhop.message.MsgProcessor;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * ワークスペースを表すクラス.
 *
 * @author K.Koike
 */
public class Workspace implements MsgDispatcher, Serializable {

  private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
  /** ワークスペースのルートノードのリスト. */
  private final Set<BhNode> rootNodeList = new HashSet<>();
  /** 選択中のノード. 挿入順を保持したいので LinkedHashSet を使う. */
  private final Set<BhNode> selectedList = new LinkedHashSet<BhNode>();
  /** ワークスペース名. */
  private final String workspaceName;
  /**
   * <pre>
   * 選択ノードリストに変化があった時に呼び出すイベントハンドラのリストと呼び出しスレッドのフラグのマップ.
   * イベントハンドラの第1引数 : 変化のあった選択ノードリストを保持するワークスペース
   * イベントハンドラの第2引数 : 変化のあった選択ノード
   * </pre>
   * 呼び出しスレッドのフラグ : true ならUIスレッドで呼び出すことを保証する
   */
  private transient Map<BiConsumer<? super Workspace, ? super Collection<? super BhNode>>, Boolean>
      onSelectedNodeListChangedToThreadFlag = new HashMap<>();
  /** このワークスペースを持つワークスペースセット. */
  private transient WorkspaceSet workspaceSet;
  /** このオブジェクト宛てに送られたメッセージを処理するオブジェクト. */
  private transient MsgProcessor msgProcessor = (msg, data) -> null;

  /**
   * コンストラクタ.
   *
   * @param workspaceName ワークスペース名
   */
  public Workspace(String workspaceName) {
    this.workspaceName = workspaceName;
  }

  /**
   * ルートノードを追加する.
   *
   * @param node 追加するBhノード
   */
  public void addRootNode(BhNode node) {
    rootNodeList.add(node);
  }

  /**
   * ルートノードを削除する.
   *
   * @param node 削除するノード
   */
  public void removeRootNode(BhNode node) {
    rootNodeList.remove(node);
  }

  /**
   * このワークスペースを持つワークスペースセットをセットする.
   *
   * @param wss このワークスペースを持つワークスペースセット
   */
  public void setWorkspaceSet(WorkspaceSet wss) {
    workspaceSet = wss;
  }

  /**
   * このワークスペースを持つワークスペースセットを返す.
   *
   * @return このワークスペースを持つワークスペースセット
   */
  public WorkspaceSet getWorkspaceSet() {
    return workspaceSet;
  }

  /** ロードのための初期化処理をする. */
  public void initForLoad() {
    rootNodeList.clear();
    selectedList.clear();
    onSelectedNodeListChangedToThreadFlag = new HashMap<>();
  }

  /**
   * {@code node} をルートノードとして持っているかどうかチェックする.
   *
   * @param node ワークスペース直下のルートノードかどうかを調べるノード
   * @return {@code node} をルートノードとして持っている場合 true
   */
  public boolean containsAsRoot(BhNode node) {
    return rootNodeList.contains(node);
  }

  /**
   * ワークスペース内のルート {@link BhNode} の集合を返す.
   *
   * @return ワークスペース内のルート {@link BhNode} の集合
   */
  public Collection<BhNode> getRootNodeList() {
    return new ArrayList<>(rootNodeList);
  }

  /**
   * 選択されたノードをセットする. このワークスペースの選択済みのノードは全て非選択になる.
   *
   * @param selected 新たに選択されたノード
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void setSelectedNode(BhNode selected, UserOperationCommand userOpeCmd) {
    // 同じノードをクリックしたときにundoスタックにコマンドが積まれるのを避ける
    if ((selectedList.size() == 1) && selectedList.contains(selected)) {
      return;
    }
    clearSelectedNodeList(userOpeCmd);
    addSelectedNode(selected, userOpeCmd);
  }

  /**
   * 選択されたノードを選択済みリストに追加する.
   *
   * @param nodeToAdd 追加されるノード
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void addSelectedNode(BhNode nodeToAdd, UserOperationCommand userOpeCmd) {
    if (selectedList.contains(nodeToAdd)) {
      return;
    }
    selectedList.add(nodeToAdd);
    MsgService.INSTANCE.selectNodeView(nodeToAdd, true);
    MsgService.INSTANCE.updateMultiNodeShifter(nodeToAdd, this);
    MsgService.INSTANCE.hilightImit(nodeToAdd, true);
    onSelectedNodeListChangedToThreadFlag.forEach(this::invokeOnSelectedNodeChanged);
    userOpeCmd.pushCmdOfAddSelectedNode(this, nodeToAdd);
  }

  /**
   * 選択中のBhNodeのリストを返す.
   *
   * @return 選択中のBhNodeのリスト
   */
  public List<BhNode> getSelectedNodeList() {
    return new ArrayList<>(selectedList);
  }

  /**
   * 引数で指定したノードを選択済みリストから削除する.
   *
   * @param nodeToRemove 選択済みリストから削除する {@link BhNode}
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void removeSelectedNode(BhNode nodeToRemove, UserOperationCommand userOpeCmd) {
    if (!selectedList.contains(nodeToRemove)) {
      return;
    }
    selectedList.remove(nodeToRemove);
    MsgService.INSTANCE.selectNodeView(nodeToRemove, false);
    MsgService.INSTANCE.updateMultiNodeShifter(nodeToRemove, this);
    MsgService.INSTANCE.hilightImit(nodeToRemove, false);
    onSelectedNodeListChangedToThreadFlag.forEach(this::invokeOnSelectedNodeChanged);
    userOpeCmd.pushCmdOfRemoveSelectedNode(this, nodeToRemove);
  }

  /** 選択変更時のイベントハンドラを呼び出す. */
  private void invokeOnSelectedNodeChanged(
      BiConsumer<? super Workspace, ? super Collection<? super BhNode>> handler,
      boolean invokeOnUiThread) {

    if (invokeOnUiThread && !Platform.isFxApplicationThread()) {
      Platform.runLater(() -> handler.accept(this, getSelectedNodeList()));
    } else {
      handler.accept(this, getSelectedNodeList());
    }
  }

  /**
   * 選択中のノードをすべて非選択にする.
   *
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void clearSelectedNodeList(UserOperationCommand userOpeCmd) {
    BhNode[] nodesToDeselect = selectedList.toArray(new BhNode[selectedList.size()]);
    for (BhNode node : nodesToDeselect) {
      removeSelectedNode(node, userOpeCmd);
    }
  }

  /**
   * ワークスペース名を取得する.
   *
   * @return ワークスペース名
   */
  public String getName() {
    return workspaceName;
  }

  /**
   * <pre>
   * 選択ノードリストに変化があった時のイベントハンドラを登録する.
   * イベントハンドラの第1引数 : 変化のあった選択ノードリストを保持するワークスペース
   * イベントハンドラの第2引数 : 変化のあった選択ノード
   * </pre>
   *
   * @param handler 登録するイベントハンドラ
   * @param invokeOnUiThread UIスレッド上で呼び出す場合 true
   */
  public void addOnSelectedNodeListChanged(
      BiConsumer<? super Workspace, ? super Collection<? super BhNode>> handler,
      boolean invokeOnUiThread) {

    onSelectedNodeListChangedToThreadFlag.put(handler, invokeOnUiThread);
  }

  /**
   * 選択ノードリストに変化があった時のイベントハンドラを削除する.
   *
   * @param handler 削除するイベントハンドラ
   */
  public void removeOnSelectedNodeListChanged(
      BiConsumer<? super Workspace, ? super Collection<? super BhNode>> handler) {
    onSelectedNodeListChangedToThreadFlag.remove(handler);
  }

  @Override
  public void setMsgProcessor(MsgProcessor processor) {
    msgProcessor = processor;
  }

  @Override
  public MsgData dispatch(BhMsg msg, MsgData data) {
    return msgProcessor.processMsg(msg, data);
  }
}
