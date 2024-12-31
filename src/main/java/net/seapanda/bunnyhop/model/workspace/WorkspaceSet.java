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

import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.export.CorruptedSaveDataException;
import net.seapanda.bunnyhop.export.IncompatibleSaveFormatException;
import net.seapanda.bunnyhop.export.ProjectExporter;
import net.seapanda.bunnyhop.export.ProjectImporter;
import net.seapanda.bunnyhop.model.AppRoot;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.model.traverse.NodeMvcBuilder;
import net.seapanda.bunnyhop.model.traverse.TextPrompter;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.service.ModelExclusiveControl;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.proxy.WorkspaceSetViewProxy;
import org.apache.commons.lang3.function.TriConsumer;

/**
 * ワークスペースの集合を保持、管理するクラス.
 *
 * @author K.Koike
 */
public class WorkspaceSet {
  /** コピー予定のノード. */
  private final SequencedSet<BhNode> readyToCopy = new LinkedHashSet<>();
  /** カット予定のノード. */
  private final SequencedSet<BhNode> readyToCut = new LinkedHashSet<>();
  /** 全てのワークスペース. */
  private final SequencedSet<Workspace> workspaceSet = new LinkedHashSet<>();
  /** ノードの貼り付け位置をずらすためのカウンタ. */
  private int pastePosOffsetCount = -2;
  /** 保存後に変更されたかどうかのフラグ. */
  private boolean isDirty = false;
  private Workspace currentWorkspace;
  /** このオブジェクトを保持する {@link AppRoot} オブジェクト. */
  private AppRoot appRoot;
  /** このオブジェクトに対応するビューの処理を行うプロキシオブジェクト. */
  WorkspaceSetViewProxy viewProxy = new WorkspaceSetViewProxy() {};
  /**
   * key : コピー予定ノードが追加されたときのイベントハンドラ.
   * value : 呼び出しスレッドを決めるフラグ.
   */
  private final
      Map<TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation>, Boolean>
      onCopyNodeAddedToThreadFlag = new HashMap<>();
  /**
   * key : コピー予定ノードが削除されたときのイベントハンドラ.
   * value : 呼び出しスレッドを決めるフラグ.
   */
  private final
      Map<TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation>, Boolean>
      onCopyNodeRemovedToThreadFlag = new HashMap<>();
  /**
   * key : カット予定ノードが追加されたときのイベントハンドラ.
   * value : 呼び出しスレッドを決めるフラグ.
   */
  private final
      Map<TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation>, Boolean>
      onCutNodeAddedToThreadFlag = new HashMap<>();
  /**
   * key : カット予定ノードが削除されたときのイベントハンドラ.
   * value : 呼び出しスレッドを決めるフラグ.
   */
  private final
      Map<TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation>, Boolean>
      onCutNodeRemovedToThreadFlag = new HashMap<>();
  /**
   * <pre>
   * ノードが選択されたときに呼び出すイベントハンドラのリストと呼び出しスレッドのフラグのマップ.
   * イベントハンドラの第1引数 : 変化のあった選択ノード
   * イベントハンドラの第2引数 : ノードの選択状態
   * 呼び出しスレッドのフラグ : true ならUIスレッドで呼び出すことを保証する
   * </pre>
   */
  private final Map<TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation>, Boolean>
      onNodeSelectionStateChangedToThreadFlag = new HashMap<>();
  /**
   * <pre>
   * 操作対象のワークスペースが切り替わった時に呼び出すイベントハンドラのリストと呼び出しスレッドのフラグのマップ.
   * 第1引数 : 前の操作対象のワークスペース
   * 第2引数 : 新しい操作対象のワークスペース
   * </pre>
   */
  private final
      Map<BiConsumer<? super Workspace, ? super Workspace>, Boolean>
      onCurrentWorkspaceChangedToThreadFlag = new HashMap<>();
  /** ノードの選択状態が変わったときのイベントハンドラを呼ぶ関数オブジェクト. */
  private final TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation>
      onNodeSelectionStateChanged = this::invokeOnNodeSelectionStateChanged;
  /** ワークスペースからノードが削除されたときのイベントハンドラを呼ぶ関数オブジェクト. */
  private final TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation>
      onNodeRemovedFromWs = this::invokeOnNodeRemovedFromWs;


  /** コンストラクタ. */
  public WorkspaceSet() {}

  /**
   * ワークスペースを追加する.
   *
   * @param workspace 追加されるワークスペース
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addWorkspace(Workspace workspace, UserOperation userOpe) {
    workspaceSet.add(workspace);
    workspace.setWorkspaceSet(this);
    workspace.addOnNodeSelectionStateChanged(onNodeSelectionStateChanged, false);
    workspace.addOnNodeRemoved(onNodeRemovedFromWs, true);
    viewProxy.notifyWorkspaceAdded(workspace);
    userOpe.pushCmdOfAddWorkspace(workspace);
  }

  /**
   * ワークスペースを取り除く.
   *
   * @param workspace 取り除かれるワークスペース
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void removeWorkspace(Workspace workspace, UserOperation userOpe) {
    workspaceSet.remove(workspace);
    workspace.setWorkspaceSet(null);
    workspace.removeOnNodeSelectionStateChanged(onNodeSelectionStateChanged);
    workspace.removeOnNodeRemoved(onNodeRemovedFromWs);
    viewProxy.notifyWorkspaceRemoved(workspace);
    userOpe.pushCmdOfRemoveWorkspace(workspace, this);
  }

  /**
   * コピー予定の {@link BhNode} をリストに追加する.
   *
   * @param toAdd コピー予定の {@link BhNode} を格納したコレクション
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addNodeToCopyList(BhNode toAdd, UserOperation userOpe) {
    if (!readyToCopy.contains(toAdd)) {
      readyToCopy.add(toAdd);
      invokeOnCutOrCopyListChanged(onCopyNodeAddedToThreadFlag, toAdd, userOpe);
      userOpe.pushCmdOfAddNodeToCopyList(this, toAdd);
    }
  }

  /**
   * コピー予定の {@link BhNode} のリストをクリアする.
   *
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void clearCopyList(UserOperation userOpe) {
    while (readyToCopy.size() != 0) {
      removeNodeFromCopyList(readyToCopy.getFirst(), userOpe);
    }
  }

  /**
   * コピー予定のノードリストからノードを取り除く.
   *
   * @param toRemove 取り除くノード
   * @param userOpe undo 用コマンドオブジェクト
   * */
  public void removeNodeFromCopyList(BhNode toRemove, UserOperation userOpe) {
    if (readyToCopy.contains(toRemove)) {
      readyToCopy.remove(toRemove);
      invokeOnCutOrCopyListChanged(onCopyNodeRemovedToThreadFlag, toRemove, userOpe);
      userOpe.pushCmdOfRemoveNodeFromCopyList(this, toRemove);
    }
  }

  /** コピー予定のノードのセットを返す. */
  public SequencedSet<BhNode> getCopyList() {
    return new LinkedHashSet<>(readyToCopy);
  }

  /**
   * カット予定の {@link BhNode} をリストに追加する.
   * 既存のカット予定, コピー予定のノードはそれぞれのリストから取り除かれる.
   *
   * @param toAdd カット予定の {@link BhNode} を格納したコレクション
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addNodeToCutList(BhNode toAdd, UserOperation userOpe) {
    if (!readyToCut.contains(toAdd)) {
      readyToCut.add(toAdd);
      invokeOnCutOrCopyListChanged(onCutNodeAddedToThreadFlag, toAdd, userOpe);
      userOpe.pushCmdOfAddNodeToCutList(this, toAdd);
    }
  }

  /**
   * カット予定のBhNodeリストをクリアする.
   *
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void clearCutList(UserOperation userOpe) {
    while (readyToCut.size() != 0) {
      removeNodeFromCutList(readyToCut.getFirst(), userOpe);
    }    
  }

  /**
   * カット予定のノードリストからノードを取り除く.
   *
   * @param toRemove 取り除くノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void removeNodeFromCutList(BhNode toRemove, UserOperation userOpe) {
    if (readyToCut.contains(toRemove)) {
      readyToCut.remove(toRemove);
      invokeOnCutOrCopyListChanged(onCutNodeRemovedToThreadFlag, toRemove, userOpe);
      userOpe.pushCmdOfRemoveNodeFromCutList(this, toRemove);
    }
  }

  /** カット予定のノードのセットを返す. */
  public SequencedSet<BhNode> getCutList() {
    return new LinkedHashSet<>(readyToCut);
  }

  /**
   * ペースト処理を行う.
   *
   * @param wsToPasteIn 貼り付け先のワークスペース
   * @param pasteBasePos 貼り付け基準位置
   */
  public void paste(Workspace wsToPasteIn, Vec2D pasteBasePos, UserOperation userOpe) {
    copyAndPaste(wsToPasteIn, pasteBasePos, userOpe);
    cutAndPaste(wsToPasteIn, pasteBasePos, userOpe);
    BhService.compileErrNodeManager().updateErrorNodeIndicator(userOpe);
    BhService.compileErrNodeManager().unmanageNonErrorNodes(userOpe);
    BhService.derivativeCache().clearAll();
  }

  /**
   * コピー予定リストのノードをコピーして引数で指定したワークスペースに貼り付ける.
   *
   * @param wsToPasteIn 貼り付け先のワークスペース
   * @param pasteBasePos 貼り付け基準位置
   * @param userOpe undo 用コマンドオブジェクト
   */
  private void copyAndPaste(
      Workspace wsToPasteIn, Vec2D pasteBasePos, UserOperation userOpe) {
    Collection<BhNode> candidates = readyToCopy.stream()
        .filter(this::canCopyOrCut).collect(Collectors.toCollection(HashSet::new));

    Collection<OriginalAndCopy> listOfOrgAndCopy = candidates.stream()
        .map(node -> new OriginalAndCopy(node, genCopyNode(node, candidates, userOpe)))
        .filter(orgAndCopy -> orgAndCopy.copy() != null)
        .collect(Collectors.toCollection(ArrayList::new));

    // 貼り付け処理
    for (var orgAndCopy : listOfOrgAndCopy) {
      NodeMvcBuilder.build(orgAndCopy.copy());
      TextPrompter.prompt(orgAndCopy.copy());
      BhService.bhNodePlacer().moveToWs(
          wsToPasteIn,
          orgAndCopy.copy(),
          pasteBasePos.x,
          pasteBasePos.y + pastePosOffsetCount * BhConstants.LnF.REPLACED_NODE_SHIFT * 2,
          userOpe);
      //コピー直後のノードは大きさが未確定なので, コピー元ノードの大きさを元に貼り付け位置を算出する.
      Vec2D size = orgAndCopy.org().getViewProxy().getSizeIncludingOuters(true);
      pasteBasePos.x += size.x + BhConstants.LnF.REPLACED_NODE_SHIFT * 2;
    }
    pastePosOffsetCount = (pastePosOffsetCount > 2) ? -2 : ++pastePosOffsetCount;
  }

  /**
   * このノードをコピーする.
   *
   * <p> 返されるノードの MVC は構築されない. </p>
   *
   * @param target コピー対象のノード
   * @param nodesToCopy {@code target} ノードとともにコピーされるノード
   * @param userOpe undo 用コマンドオブジェクト
   * @return 作成したコピーノード.  コピーノードを作らなかった場合 null.
   */
  private BhNode genCopyNode(
      BhNode target,
      Collection<? extends BhNode> nodesToCopy,
      UserOperation userOpe) {
    return target.getEventAgent()
        .execOnCopyRequested(nodesToCopy, node -> true, userOpe)
        .map(fnIsNodeToBeCopied -> target.copy(fnIsNodeToBeCopied, userOpe)).orElse(null);
  }

  /**
   * カット予定リストのノードを引数で指定したワークスペースに移動する.
   *
   * @param wsToPasteIn 貼り付け先のワークスペース
   * @param pasteBasePos 貼り付け基準位置
   * @param userOpe undo 用コマンドオブジェクト
   */
  private void cutAndPaste(
      Workspace wsToPasteIn, Vec2D pasteBasePos, UserOperation userOpe) {
    if (readyToCut.isEmpty()) {
      return;
    }
    Collection<BhNode> candidates = readyToCut.stream()
        .filter(this::canCopyOrCut).collect(Collectors.toCollection(HashSet::new));

    Collection<BhNode> nodesToPaste = candidates.stream()
        .filter(node -> node.getEventAgent().execOnCutRequested(candidates, userOpe))
        .collect(Collectors.toCollection(ArrayList::new));

    // 貼り付け処理
    for (var node : nodesToPaste) {
      List<Swapped> swappedNodes = BhService.bhNodePlacer().moveToWs(
          wsToPasteIn,
          node,
          pasteBasePos.x,
          pasteBasePos.y + pastePosOffsetCount * BhConstants.LnF.REPLACED_NODE_SHIFT * 2,
          userOpe);
      Vec2D size = node.getViewProxy().getSizeIncludingOuters(true);
      pasteBasePos.x += size.x + BhConstants.LnF.REPLACED_NODE_SHIFT * 2;
      dispatchEventsOnPaste(node, swappedNodes, userOpe);
    }

    pastePosOffsetCount = (pastePosOffsetCount > 2) ? -2 : ++pastePosOffsetCount;
    userOpe.pushCmdOfRemoveFromList(readyToCut, readyToCut);
    readyToCut.clear();
  }

  /** ペースト時のイベント処理を実行する. */
  private void dispatchEventsOnPaste(
      BhNode node, List<Swapped> swappedNodes, UserOperation userOpe) {
    if (!swappedNodes.isEmpty()) {
      node.getEventAgent().execOnMovedFromChildToWs(
          swappedNodes.get(0).newNode().findParentNode(),
          swappedNodes.get(0).newNode().findRootNode(),
          swappedNodes.get(0).newNode(),
          true,
          userOpe);
    }
    for (var swapped : swappedNodes) {
      swapped.newNode().findParentNode().getEventAgent().execOnChildReplaced(
          node,
          swapped.newNode(),
          swapped.newNode().getParentConnector(),
          userOpe);
    }
  }

  /**
   * コピーもしくはカットの対象になるかどうか判定する.
   *
   * @param node 判定対象のノード
   * @return コピーもしくはカットの対象になる場合 true
   */
  private boolean canCopyOrCut(BhNode node) {
    return (node.isChild() && node.findRootNode().isRootOnWs())
        || node.isRootOnWs();
  }

  /**
   * 現在保持しているワークスペース一覧を返す.
   *
   * @return 現在保持しているワークスペース一覧
   */
  public SequencedSet<Workspace> getWorkspaces() {
    return new LinkedHashSet<>(workspaceSet);
  }

  /**
   * 現在操作対象のワークスペースを設定する.
   * 
   * <pre>
   * この操作は undo の対象にしない.
   * 理由は, この操作はワークスペースのタブ切り替え時は単一の undo 操作となるが,
   * ワークスペースの追加 (redo によるものも含む) 時には, これに付随する undo 操作となるので
   * 本メソッドに渡すべき {@link UserOperation} オブジェクトの選択が複雑になる.
   * </pre>
   *
   * @param ws 現在操作対象のワークスペース
   */
  public void setCurrentWorkspace(Workspace ws) {
    Workspace old = currentWorkspace;
    currentWorkspace = ws;
    invokeOnCurrentWorkspaceChanged(old, currentWorkspace);
  }

  /**
   * 現在, 操作対象となっているワークスペースを取得する.
   *
   * @return 現在操作対象となっているワークスペース. 存在しない場合は null.
   */
  public Workspace getCurrentWorkspace() {
    return currentWorkspace;
  }

  /**
   * 全ワークスペースを保存する.
   *
   * @param file 保存先のファイル
   * @return セーブに成功した場合 true
   */
  public boolean save(File file) {
    ModelExclusiveControl.lockForRead();
    try {
      ProjectExporter.export(workspaceSet, file.toPath());
      BhService.msgPrinter().infoForUser(TextDefs.Export.hasSaved.get(file.getPath()));
      isDirty = false;
      return true;
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(
          "Failed to save the project.\n%s\n%s".formatted(file.getPath(), e));
      BhService.msgPrinter().alert(
          Alert.AlertType.ERROR,
          TextDefs.Export.InformFailedToSave.title.get(),
          null,
          file.getPath() + "\n" + e);
      return false;
    } finally {
      ModelExclusiveControl.unlockForRead();
    }
  }

  /**
   * ファイルからワークスペースをロードし追加する.
   *
   * @param saveFile ロードするファイル
   * @param clearOldWorkspaces 既存のワークスペースを全て消す場合 true
   * @return ロードに成功した場合true
   */
  public boolean load(File saveFile, Boolean clearOldWorkspaces) {
    ModelExclusiveControl.lockForRead();
    try {
      ProjectImporter.Result result =  ProjectImporter.imports(saveFile.toPath());
      if (!result.warnings().isEmpty()) {
        BhService.msgPrinter().errForDebug(result.warningMsg());
      }
      boolean continueLoading = result.warnings().isEmpty() || askIfContinueLoading();
      if (!continueLoading) {
        return false;
      }
      UserOperation userOpe = new UserOperation();
      if (clearOldWorkspaces) {
        deleteAllWorkspaces(userOpe);
      }
      result.workspaces().forEach(ws -> this.addWorkspace(ws, userOpe));
      result.workspaces().stream()
          .flatMap(ws -> ws.getRootNodes().stream())
          .forEach(root -> BhService.compileErrNodeManager().collect(root, userOpe));
      BhService.undoRedoAgent().pushUndoCommand(userOpe);
      return true;

    } catch (IncompatibleSaveFormatException e) {
      String msg = TextDefs.Import.Error.unsupportedSaveDataVersion.get(
          e.version, BhConstants.SAVE_DATA_VERSION);
      outputLoadErrMsg(saveFile, e, msg);
      return false;

    } catch (CorruptedSaveDataException | JsonSyntaxException e) {
      String msg = TextDefs.Import.Error.corruptedSaveData.get();
      outputLoadErrMsg(saveFile, e, msg);
      return false;

    } catch (Exception e) {
      String msg = TextDefs.Import.Error.failedToReadSaveFile.get();
      outputLoadErrMsg(saveFile, e, msg);
      return false;

    } finally {
      ModelExclusiveControl.unlockForRead();
    }
  }

  /**
   * ロードを中断するか確認する.
   *
   * @retval true ロードを中断する
   * @retval false 既存のワークスペースにロードしたワークスペースを追加
   */
  private Boolean askIfContinueLoading() {
    String title = TextDefs.Import.AskIfContinue.title.get();
    String body = TextDefs.Import.AskIfContinue.body.get(
        ButtonType.YES.getText(), ButtonType.NO.getText());
    Optional<ButtonType> buttonType = BhService.msgPrinter().alert(
        AlertType.CONFIRMATION, title, null, body,
        ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);

    return buttonType.map(type -> type.equals(ButtonType.YES)).orElse(false);
  }

  /** ロード失敗時のエラーメッセージを出力する. */
  private void outputLoadErrMsg(File saveFile, Exception e, String msg) {
    msg += "\n" + saveFile.getAbsolutePath();
    String title = TextDefs.Import.Error.title.get();
    BhService.msgPrinter().errForDebug(e.toString());
    BhService.msgPrinter().alert(Alert.AlertType.INFORMATION, title, null, msg);
  }

  /**
   * 全てのワークスペースを削除する.
   *
   * @param userOpe undo 用コマンドオブジェクト
   */
  private void deleteAllWorkspaces(UserOperation userOpe) {
    for (Workspace ws : new ArrayList<>(workspaceSet)) {
      removeWorkspace(ws, userOpe);
    }
  }

  /**
   * コピー予定のノードが追加されたときのイベントハンドラを追加する.
   *
   * @param handler 追加するイベントハンドラ
   * @param invokeOnUiThread UI スレッド上で呼び出す場合 true
   */
  public void addOnCopyNodeAdded(
      TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation> handler,
      boolean invokeOnUiThread) {
    onCopyNodeAddedToThreadFlag.put(handler, invokeOnUiThread);
  }

  /**
   * コピー予定のノードが追加されたときのイベントハンドラを削除する.
   *
   * @param handler 削除するイベントハンドラ
   * @param invokeOnUiThread UI スレッド上で呼び出す場合 true
   */
  public void removeOnCopyNodeAdded(
      TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation> handler,
      boolean invokeOnUiThread) {
    onCopyNodeAddedToThreadFlag.remove(handler);
  }

  /**
   * コピー予定のノードが削除されたときのイベントハンドラを追加する.
   *
   * @param handler 追加するイベントハンドラ
   * @param invokeOnUiThread UI スレッド上で呼び出す場合 true
   */
  public void addOnCopyNodeRemoved(
      TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation> handler,
      boolean invokeOnUiThread) {
    onCopyNodeRemovedToThreadFlag.put(handler, invokeOnUiThread);
  }

  /**
   * コピー予定のノードが削除されたときのイベントハンドラを削除する.
   *
   * @param handler 削除するイベントハンドラ
   * @param invokeOnUiThread UI スレッド上で呼び出す場合 true
   */
  public void removeOnCopyNodeRemoved(
      TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation> handler,
      boolean invokeOnUiThread) {
    onCopyNodeRemovedToThreadFlag.remove(handler);
  }


  /**
   * カット予定のノードが追加されたときのイベントハンドラを追加する.
   *
   * @param handler 追加するイベントハンドラ
   * @param invokeOnUiThread UI スレッド上で呼び出す場合 true
   */
  public void addOnCutNodeAdded(
      TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation> handler,
      boolean invokeOnUiThread) {
    onCutNodeAddedToThreadFlag.put(handler, invokeOnUiThread);
  }

  /**
   * カット予定のノードが追加されたときのイベントハンドラを削除する.
   *
   * @param handler 削除するイベントハンドラ
   * @param invokeOnUiThread UI スレッド上で呼び出す場合 true
   */
  public void removeOnCutNodeAdded(
      TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation> handler,
      boolean invokeOnUiThread) {
    onCutNodeAddedToThreadFlag.remove(handler);
  }

  /**
   * カット予定のノードが削除されたときのイベントハンドラを追加する.
   *
   * @param handler 追加するイベントハンドラ
   * @param invokeOnUiThread UI スレッド上で呼び出す場合 true
   */
  public void addOnCutNodeRemoved(
      TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation> handler,
      boolean invokeOnUiThread) {
    onCutNodeRemovedToThreadFlag.put(handler, invokeOnUiThread);
  }

  /**
   * カット予定のノードが削除されたときのイベントハンドラを削除する.
   *
   * @param handler 削除するイベントハンドラ
   * @param invokeOnUiThread UI スレッド上で呼び出す場合 true
   */
  public void removeOnCutNodeRemoved(
      TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation> handler,
      boolean invokeOnUiThread) {
    onCutNodeRemovedToThreadFlag.remove(handler);
  }

  /**
   * ノードの選択状態に変化があった時のイベントハンドラを追加する.
   *
   * @param handler 追加するイベントハンドラ
   *     <pre>
   *     handler 第1引数 : 選択状態に変化のあったノード
   *     handler 第2引数 : 選択状態.  選択されたとき true.
   *     </pre>
   * @param invokeOnUiThread UIスレッド上で呼び出す場合 true
   */
  public void addOnNodeSelectionStateChanged(
      TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation> handler,
      boolean invokeOnUiThread) {
    onNodeSelectionStateChangedToThreadFlag.put(handler, invokeOnUiThread);
  }

  /**
   * ノードの選択状態に変化があった時のイベントハンドラを削除する.
   *
   * @param handler 削除するイベントハンドラ
   */
  public void removeOnNodeSelectionStateChanged(
      TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation> handler) {
    onNodeSelectionStateChangedToThreadFlag.remove(handler);
  }

  /**
   * 操作対象のワークスペースが変わった時のイベントハンドラを追加する.
   *
   * @param handler 追加するイベントハンドラ
   *     <pre>
   *     handler 第1引数 : 前の操作対象のワークスペース
   *     handler 第2引数 : 新しい操作対象のワークスペース
   *     handler 第3引数 : undo 用コマンドオブジェクト
   *     </pre>
   * @param invokeOnUiThread UI スレッド上で呼び出す場合 true
   */
  public void addOnCurrentWorkspaceChanged(
      BiConsumer<? super Workspace, ? super Workspace> handler,
      boolean invokeOnUiThread) {
    onCurrentWorkspaceChangedToThreadFlag.put(handler, invokeOnUiThread);
  }

  /**
   * 操作対象のワークスペースが変わった時のイベントハンドラを削除する.
   *
   * @param handler 削除するイベントハンドラ
   */
  public void removeOnCurrentWorkspaceChanged(
      BiConsumer<? super Workspace, ? super Workspace> handler) {
    onCurrentWorkspaceChangedToThreadFlag.remove(handler);
  }

  /** このワークスペースセットのコピー or カット予定のノードが変更されたときのイベントハンドラを呼び出す. */
  private void invokeOnCutOrCopyListChanged(
      Map<TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation>, Boolean>
        handlerToThreadFlag,
      BhNode node,
      UserOperation userOpe) {
    handlerToThreadFlag.forEach((handler, invokeOnUiThread) -> {
      if (invokeOnUiThread && !Platform.isFxApplicationThread()) {
        Platform.runLater(() -> handler.accept(this, node, userOpe));
      } else {
        handler.accept(this, node, userOpe);
      }
    });
  }

  /**
   * ノードの選択状態に変化があった時のイベントハンドラを呼ぶ.
   *
   * @param selectedNode 選択状態に変化があったノード
   * @param isSelected {@link selectedNode} の選択状態.  選択されているとき true.
   * @param userOpe undo 用コマンドオブジェクト
   */
  private void invokeOnNodeSelectionStateChanged(
      BhNode selectedNode, Boolean isSelected, UserOperation userOpe) {
    onNodeSelectionStateChangedToThreadFlag.forEach((handler, invokeOnUiThread) -> {
      if (invokeOnUiThread && !Platform.isFxApplicationThread()) {
        Platform.runLater(() -> handler.accept(selectedNode, isSelected, userOpe));
      } else {
        handler.accept(selectedNode, isSelected, userOpe);
      }
    });
  }

  /**
   * 操作対象のワークスペースが変わった時のイベントハンドラを呼ぶ.
   *
   * @param oldWs 前の操作対象のワークスペース
   * @param newWs 新しい操作対象のワークスペース
   */
  private void invokeOnCurrentWorkspaceChanged(Workspace oldWs, Workspace newWs) {
    onCurrentWorkspaceChangedToThreadFlag.forEach((handler, invokeOnUiThread) -> {
      if (invokeOnUiThread && !Platform.isFxApplicationThread()) {
        Platform.runLater(() -> handler.accept(oldWs, newWs));
      } else {
        handler.accept(oldWs, newWs);
      }
    });
  }

  /** ワークスペースからノードが削除されたときのイベントハンドラ. */
  private void invokeOnNodeRemovedFromWs(Workspace ws, BhNode node, UserOperation userOpe) {
    if (readyToCopy.contains(node)) {
      removeNodeFromCopyList(node, userOpe);
    }
    if (readyToCut.contains(node)) {
      removeNodeFromCutList(node, userOpe);
    }
  }

  /** ワークスペースセットが保存後に変更されていることを示すフラグをセットする. */
  public void setDirty() {
    isDirty = true;
  }

  /** ワークスペースセットが保存後に変更されている場合 true を返す. */
  public boolean isDirty() {
    return isDirty;
  }

  /** このオブジェクトを保持する {@link AppRoot} オブジェクトを取得する.*/
  public AppRoot getAppRoot() {
    return appRoot;
  }

  /** このオブジェクトを保持する {@link AppRoot} オブジェクトを設定する.*/
  public void setAppRoot(AppRoot appRoot) {
    this.appRoot = appRoot;
  }

  /** このオブジェクトに対応するビューの処理を行うプロキシオブジェクトを取得する. */
  public WorkspaceSetViewProxy getViewProxy() {
    return viewProxy;
  }

  /** このオブジェクトに対応するビューの処理を行うプロキシオブジェクトを設定する. */
  public void setViewProxy(WorkspaceSetViewProxy viewProxy) {
    this.viewProxy = viewProxy;
  }

  /** コピー元とコピーされた {@link BhNode} のペア. */
  private record OriginalAndCopy(BhNode org, BhNode copy) {}
}
