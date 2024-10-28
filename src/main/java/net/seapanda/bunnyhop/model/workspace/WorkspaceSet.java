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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.Vec2D;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgProcessor;
import net.seapanda.bunnyhop.message.MsgReceptionWindow;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.modelprocessor.NodeMvcBuilder;
import net.seapanda.bunnyhop.modelprocessor.TextImitationPrompter;
import net.seapanda.bunnyhop.modelservice.BhNodeHandler;
import net.seapanda.bunnyhop.modelservice.DelayedDeleter;
import net.seapanda.bunnyhop.modelservice.DeleteOperation;
import net.seapanda.bunnyhop.modelservice.ModelExclusiveControl;
import net.seapanda.bunnyhop.modelservice.SyntaxErrorNodeManager;
import net.seapanda.bunnyhop.root.BunnyHop;
import net.seapanda.bunnyhop.saveandload.ProjectSaveData;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * ワークスペースの集合を保持、管理するクラス.
 *
 * @author K.Koike
 */
public class WorkspaceSet implements MsgReceptionWindow {
  /** コピー予定のノード. */
  private final ObservableList<BhNode> readyToCopy = FXCollections.observableArrayList();
  /** カット予定のノード. */
  private final ObservableList<BhNode> readyToCut = FXCollections.observableArrayList();
  /** 全てのワークスペース. */
  private final List<Workspace> workspaceList = new ArrayList<>();
  /** このオブジェクト宛てに送られたメッセージを処理するオブジェクト. */
  private MsgProcessor msgProcessor;
  /** ノードの貼り付け位置をずらすためのカウンタ. */
  private int pastePosOffsetCount = -2;
  private Workspace currentWorkspace;
  /**
   * key : コピー予定ノードのリストに変化がった時のイベントハンドラ.
   * value : 呼び出しスレッドを決めるフラグ.
   */
  private final Map<ListChangeListener<? super BhNode>, Boolean> onCopyNodeListChangedToThreadFlag
      = new HashMap<>();
  /**
   * key : カット予定ノードのリストに変化がった時のイベントハンドラ.
   * value : 呼び出しスレッドを決めるフラグ.
   */
  private final Map<ListChangeListener<? super BhNode>, Boolean> onCutNodeListChangedToThreadFlag
      = new HashMap<>();

  /**
   * <pre>
   * 選択ノードリストに変化があった時に呼び出すイベントハンドラのリストと呼び出しスレッドのフラグのマップ.
   * イベントハンドラの第1引数 : 変化のあった選択ノードリストを保持するワークスペース
   * イベントハンドラの第2引数 : 変化のあった選択ノード
   * 呼び出しスレッドのフラグ : true ならUIスレッドで呼び出すことを保証する
   * </pre>
   */
  private final Map<BiConsumer<? super Workspace, ? super Collection<? super BhNode>>, Boolean>
      onSelectedNodeListChangedToThreadFlag = new HashMap<>();

  /**
   * <pre>
   * 操作対象のワークスペースが切り替わった時に呼び出すイベントハンドラのリストと呼び出しスレッドのフラグのマップ.
   * 第1引数 : 前の操作対象のワークスペース
   * 第2引数 : 新しい操作対象のワークスペース
   * </pre>
   */
  private final Map<BiConsumer<? super Workspace, ? super Workspace>, Boolean>
      onCurrentWorkspaceChangedToThreadFlag = new HashMap<>();

  /** コンストラクタ. */
  public WorkspaceSet() {
    readyToCopy.addListener((Change<? extends BhNode> change) ->
        callNodeListChangedEventHandlers(change, onCopyNodeListChangedToThreadFlag));

    readyToCut.addListener((Change<? extends BhNode> change) ->
        callNodeListChangedEventHandlers(change, onCutNodeListChangedToThreadFlag));
  }

  /**
   * ワークスペースを追加する.
   *
   * @param workspace 追加されるワークスペース
   */
  public void addWorkspace(Workspace workspace) {
    workspaceList.add(workspace);
    workspace.setWorkspaceSet(this);
    workspace.addOnSelectedNodeListChanged(this::callSelectedNodeListChangedEventHandlers, false);
  }

  /**
   * ワークスペースを取り除く.
   *
   * @param workspace 取り除かれるワークスペース
   */
  public void removeWorkspace(Workspace workspace) {
    workspaceList.remove(workspace);
    workspace.setWorkspaceSet(null);
    workspace.removeOnSelectedNodeListChanged(this::callSelectedNodeListChangedEventHandlers);
  }

  /**
   * コピー予定のBhNodeリストを追加する.
   *
   * @param nodeList コピー予定のBhNodeリスト
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void addNodesToCopyList(
      Collection<BhNode> nodeList, UserOperationCommand userOpeCmd) {
    clearCutList(userOpeCmd);
    clearCopyList(userOpeCmd);
    readyToCopy.addAll(nodeList);
    userOpeCmd.pushCmdOfAddToList(readyToCopy, nodeList);
  }

  /**
   * コピー予定のBhNodeリストをクリアする.
   *
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void clearCopyList(UserOperationCommand userOpeCmd) {
    userOpeCmd.pushCmdOfRemoveFromList(readyToCopy, readyToCopy);
    readyToCopy.clear();
  }

  /**
   * コピー予定のノードリストからノードを取り除く.
   *
   * @param nodeToRemove 取り除くノード
   * @param userOpeCmd undo 用コマンドオブジェクト
   * */
  public void removeNodeFromCopyList(
      BhNode nodeToRemove, UserOperationCommand userOpeCmd) {
    if (readyToCopy.contains(nodeToRemove)) {
      userOpeCmd.pushCmdOfRemoveFromList(readyToCopy, nodeToRemove);
      readyToCopy.remove(nodeToRemove);
    }
  }

  /**
   * カット予定のBhNodeリストを追加する.
   *
   * @param nodeList カット予定のBhNodeリスト
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void addNodesToCutList(
      Collection<BhNode> nodeList, UserOperationCommand userOpeCmd) {
    clearCutList(userOpeCmd);
    clearCopyList(userOpeCmd);
    readyToCut.addAll(nodeList);
    userOpeCmd.pushCmdOfAddToList(readyToCut, nodeList);
  }

  /**
   * カット予定のBhNodeリストをクリアする.
   *
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void clearCutList(UserOperationCommand userOpeCmd) {
    userOpeCmd.pushCmdOfRemoveFromList(readyToCut, readyToCut);
    readyToCut.clear();
  }

  /**
   * カット予定のノードリストからノードを取り除く.
   *
   * @param nodeToRemove 取り除くノード
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void removeNodeFromCutList(BhNode nodeToRemove, UserOperationCommand userOpeCmd) {
    if (readyToCut.contains(nodeToRemove)) {
      userOpeCmd.pushCmdOfRemoveFromList(readyToCut, nodeToRemove);
      readyToCut.remove(nodeToRemove);
    }
  }

  /**
   * ペースト処理を行う.
   *
   * @param wsToPasteIn 貼り付け先のワークスペース
   * @param pasteBasePos 貼り付け基準位置
   */
  public void paste(Workspace wsToPasteIn, Vec2D pasteBasePos) {
    UserOperationCommand userOpeCmd = new UserOperationCommand();
    copyAndPaste(wsToPasteIn, pasteBasePos, userOpeCmd);
    cutAndPaste(wsToPasteIn, pasteBasePos, userOpeCmd);
    DelayedDeleter.INSTANCE.deleteAll(userOpeCmd);
    SyntaxErrorNodeManager.INSTANCE.updateErrorNodeIndicator(userOpeCmd);
    SyntaxErrorNodeManager.INSTANCE.unmanageNonErrorNodes(userOpeCmd);
    BunnyHop.INSTANCE.pushUserOpeCmd(userOpeCmd);
  }

  /**
   * コピー予定リストのノードをコピーして引数で指定したワークスペースに貼り付ける.
   *
   * @param wsToPasteIn 貼り付け先のワークスペース
   * @param pasteBasePos 貼り付け基準位置
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  private void copyAndPaste(
      Workspace wsToPasteIn, Vec2D pasteBasePos, UserOperationCommand userOpeCmd) {
    Collection<BhNode> candidates = readyToCopy.stream()
        .filter(this::canCopyOrCut).collect(Collectors.toCollection(HashSet::new));

    Collection<Pair<BhNode, BhNode>> orgsAndCopies = candidates.stream()
        .map(node -> new Pair<BhNode, BhNode>(node, node.genCopyNode(candidates, userOpeCmd)))
        .filter(orgAndCopy -> orgAndCopy.v2 != null)
        .collect(Collectors.toCollection(ArrayList::new));

    // 貼り付け処理
    for (var orgAndCopy : orgsAndCopies) {
      BhNode copy = orgAndCopy.v2;
      NodeMvcBuilder.build(copy);
      TextImitationPrompter.prompt(copy);
      BhNodeHandler.INSTANCE.addRootNode(
          wsToPasteIn,
          copy,
          pasteBasePos.x,
          pasteBasePos.y + pastePosOffsetCount * BhConstants.LnF.REPLACED_NODE_SHIFT * 2,
          userOpeCmd);
      //コピー直後のノードは大きさが未確定なので, コピー元ノードの大きさを元に貼り付け位置を算出する.
      Vec2D size = MsgService.INSTANCE.getViewSizeIncludingOuter(orgAndCopy.v1);
      pasteBasePos.x += size.x + BhConstants.LnF.REPLACED_NODE_SHIFT * 2;
    }
    pastePosOffsetCount = (pastePosOffsetCount > 2) ? -2 : ++pastePosOffsetCount;
  }

  /**
   * カット予定リストのノードを引数で指定したワークスペースに移動する.
   *
   * @param wsToPasteIn 貼り付け先のワークスペース
   * @param pasteBasePos 貼り付け基準位置
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  private void cutAndPaste(
      Workspace wsToPasteIn, Vec2D pasteBasePos, UserOperationCommand userOpeCmd) {
    if (readyToCut.isEmpty()) {
      return;
    }
    Collection<BhNode> candidates = readyToCut.stream()
        .filter(this::canCopyOrCut).collect(Collectors.toCollection(HashSet::new));

    Collection<BhNode> nodesToPaste = candidates.stream()
        .filter(node -> node.getEventDispatcher().execOnCutRequested(candidates, userOpeCmd))
        .collect(Collectors.toCollection(ArrayList::new));

    // 貼り付け処理
    for (var node : nodesToPaste) {
      final Optional<BhNode> newChild = BhNodeHandler.INSTANCE.deleteNodeWithDelay(
          node, userOpeCmd, DeleteOperation.REMOVE_FROM_IMIT_LIST);
      BhNodeHandler.INSTANCE.addRootNode(
          wsToPasteIn,
          node,
          pasteBasePos.x,
          pasteBasePos.y + pastePosOffsetCount * BhConstants.LnF.REPLACED_NODE_SHIFT * 2,
          userOpeCmd);
      Vec2D size = MsgService.INSTANCE.getViewSizeIncludingOuter(node);
      pasteBasePos.x += size.x + BhConstants.LnF.REPLACED_NODE_SHIFT * 2;
      DelayedDeleter.INSTANCE.deleteAll(userOpeCmd);
      newChild.ifPresent(child -> {
        node.getEventDispatcher().execOnMovedFromChildToWs(
            child.findParentNode(), child.findRootNode(), child, true, userOpeCmd);
        child.findParentNode().execOnChildReplaced(
            node, child, child.getParentConnector(), userOpeCmd);
      });
    }

    pastePosOffsetCount = (pastePosOffsetCount > 2) ? -2 : ++pastePosOffsetCount;
    userOpeCmd.pushCmdOfRemoveFromList(readyToCut, readyToCut);
    readyToCut.clear();
  }

  /**
   * コピーもしくはカットの対象になるかどうか判定する.
   *
   * @param node 判定対象のノード
   * @return コピーもしくはカットの対象になる場合 true
   */
  private boolean canCopyOrCut(BhNode node) {
    return
      (node.getState() == BhNode.State.CHILD
        && node.findRootNode().getState() == BhNode.State.ROOT_DIRECTLY_UNDER_WS)
        || node.getState() == BhNode.State.ROOT_DIRECTLY_UNDER_WS;
  }

  /**
   * 現在保持しているワークスペース一覧を返す.
   *
   * @return 現在保持しているワークスペース一覧
   */
  public List<Workspace> getWorkspaceList() {
    return Collections.unmodifiableList(workspaceList);
  }

  /**
   * 現在操作対象のワークスペースを設定す.
   *
   * @param ws 現在操作対象のワークスペース
   */
  public void setCurrentWorkspace(Workspace ws) {
    Workspace old = currentWorkspace;
    currentWorkspace = ws;
    callCurrentWorkspaceChangedEventHandlers(old, currentWorkspace);
  }

  /**
   * 現在操作対象のワークスペースを取得する.
   *
   * @return 現在操作対象のワークスペース. 存在しない場合は null.
   */
  public Workspace getCurrentWorkspace() {
    return currentWorkspace;
  }

  /**
   * 全ワークスペースを保存する.
   *
   * @param fileToSave セーブファイル
   * @return セーブに成功した場合true
   */
  public boolean save(File fileToSave) {
    ModelExclusiveControl.INSTANCE.lockForRead();
    ProjectSaveData saveData = new ProjectSaveData(workspaceList);
    try (ObjectOutputStream outputStream =
        new ObjectOutputStream(new FileOutputStream(fileToSave));) {
      outputStream.writeObject(saveData);
      MsgPrinter.INSTANCE.msgForUser("-- 保存完了 (" + fileToSave.getPath() + ") --\n");
      BunnyHop.INSTANCE.shouldSave(false);
      return true;
    } catch (IOException e) {
      MsgPrinter.INSTANCE.alert(
          Alert.AlertType.ERROR,
          "ファイルの保存に失敗しました",
          null,
          fileToSave.getPath() + "\n" + e);
      return false;
    } finally {
      ModelExclusiveControl.INSTANCE.unlockForRead();
    }
  }

  /**
   * ファイルからワークスペースをロードし追加する.
   *
   * @param saveFile ロードするファイル
   * @param isOldWsCleared ロード方法を確認する関数
   * @return ロードに成功した場合true
   */
  public boolean load(File saveFile, Boolean isOldWsCleared) {
    ModelExclusiveControl.INSTANCE.lockForRead();
    try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(saveFile));) {
      ProjectSaveData loadData = (ProjectSaveData) inputStream.readObject();
      UserOperationCommand userOpeCmd = new UserOperationCommand();
      if (isOldWsCleared) {
        BunnyHop.INSTANCE.deleteAllWorkspace(userOpeCmd);
      }
      loadData.load(userOpeCmd).forEach(ws -> BunnyHop.INSTANCE.addWorkspace(ws, userOpeCmd));
      BunnyHop.INSTANCE.pushUserOpeCmd(userOpeCmd);
      return true;
    } catch (ClassNotFoundException | IOException | ClassCastException e) {
      MsgPrinter.INSTANCE.errMsgForDebug(Util.INSTANCE.getCurrentMethodName() + "\n" + e);
      return false;
    } finally {
      ModelExclusiveControl.INSTANCE.unlockForRead();
    }
  }

  /**
   * コピー予定ノードのリストをが空かどうか調べる.
   *
   * @return コピー予定ノードのリストをが空なら true
   */
  public boolean isCopyListEmpty() {
    return readyToCopy.isEmpty();
  }

  /**
   * カット予定ノードのリストをが空かどうか調べる.
   *
   * @return カット予定ノードのリストをが空なら true
   */
  public boolean isCutListEmpty() {
    return readyToCut.isEmpty();
  }

  /**
   * コピー予定のノードが変化したときのイベントハンドラを追加する.
   *
   * @param handler 登録するイベントハンドラ
   * @param invokeOnUiThread UIスレッド上で呼び出す場合 true
   */
  public void addOnCopyListChanged(
      ListChangeListener<? super BhNode> handler, boolean invokeOnUiThread) {
    onCopyNodeListChangedToThreadFlag.put(handler, invokeOnUiThread);
  }

  /**
   * カット予定のノードが変化したときのイベントハンドラを追加する.
   *
   * @param handler 登録するイベントハンドラ
   * @param invokeOnUiThread UIスレッド上で呼び出す場合 true
   */
  public void addOnCutListChanged(
      ListChangeListener<? super BhNode> handler, boolean invokeOnUiThread) {
    onCutNodeListChangedToThreadFlag.put(handler, invokeOnUiThread);
  }

  /**
   * 選択ノードリストに変化があった時のイベントハンドラを登録する.
   *
   * @param handler 登録するイベントハンドラ
   *     <pre>
   *     handler 第1引数 : 変化のあった選択ノードリストを保持するワークスペース
   *     handler 第2引数 : 変化のあった選択ノード
   *     </pre>
   * @param invokeOnUiThread UIスレッド上で呼び出す場合 true
   */
  public void addOnSelectedNodeListChanged(
      BiConsumer<? super Workspace, ? super Collection<? super BhNode>> handler,
      boolean invokeOnUiThread) {

    onSelectedNodeListChangedToThreadFlag.put(handler, invokeOnUiThread);
  }

  /**
   * 操作対象のワークスペースが変わった時のイベントハンドラを登録する.
   *
   * @param handler 登録するイベントハンドラ
   *     <pre>
   *     handler 第1引数 : 前の操作対象のワークスペース
   *     handler 第2引数 : 新しい操作対象のワークスペース
   *     </pre>
   * @param invokeOnUiThread UIスレッド上で呼び出す場合 true
   */
  public void addOnCurrentWorkspaceChanged(
      BiConsumer<? super Workspace, ? super Workspace> handler, boolean invokeOnUiThread) {
    onCurrentWorkspaceChangedToThreadFlag.put(handler, invokeOnUiThread);
  }

  /**
   * ノードリストに変化があった時のイベントハンドラを呼ぶ.
   *
   * @param change リストの変化を表すオブジェクト
   * @param onNodeListChangedToThreadFlag リスト変化時のイベントハンドラと呼び出しスレッドのフラグ
   */
  private void callNodeListChangedEventHandlers(
      Change<? extends BhNode> change,
      Map<ListChangeListener<? super BhNode>, Boolean> onNodeListChangedToThreadFlag) {

    onNodeListChangedToThreadFlag.forEach((handler, invokeOnUiThread) -> {
      if (invokeOnUiThread && !Platform.isFxApplicationThread()) {
        Platform.runLater(() -> handler.onChanged(change));
      } else {
        handler.onChanged(change);
      }
    });
  }

  /**
   * 選択ノードリストに変化があった時のイベントハンドラを呼ぶ.
   *
   * @param ws 変化があった選択ノードリストを保持するワークスペース
   * @param selectedNodeList 変化があった選択ノードリスト
   */
  private void callSelectedNodeListChangedEventHandlers(
      Workspace ws, Collection<? super BhNode> selectedNodeList) {
    onSelectedNodeListChangedToThreadFlag.forEach((handler, invokeOnUiThread) -> {
      if (invokeOnUiThread && !Platform.isFxApplicationThread()) {
        Platform.runLater(() -> handler.accept(ws, selectedNodeList));
      } else {
        handler.accept(ws, selectedNodeList);
      }
    });
  }

  /**
   * 操作対象のワークスペースが変わった時のイベントハンドラを呼ぶ.
   *
   * @param oldWs 前の操作対象のワークスペース
   * @param newWs 新しい操作対象のワークスペース
   */
  private void callCurrentWorkspaceChangedEventHandlers(Workspace oldWs, Workspace newWs) {
    onCurrentWorkspaceChangedToThreadFlag.forEach((handler, invokeOnUiThread) -> {
      if (invokeOnUiThread && !Platform.isFxApplicationThread()) {
        Platform.runLater(() -> handler.accept(oldWs, newWs));
      } else {
        handler.accept(oldWs, newWs);
      }
    });
  }

  @Override
  public void setMsgProcessor(MsgProcessor processor) {
    msgProcessor = processor;
  }

  @Override
  public MsgData passMsg(BhMsg msg, MsgData data) {
    return msgProcessor.processMsg(msg, data);
  }
}
