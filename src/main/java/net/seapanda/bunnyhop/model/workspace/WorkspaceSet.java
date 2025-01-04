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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
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
import net.seapanda.bunnyhop.model.node.hook.CauseOfDeletion;
import net.seapanda.bunnyhop.model.traverse.NodeMvcBuilder;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.service.ModelExclusiveControl;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.Vec2D;
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
  /** このワークスペースセットに登録されたイベントハンドラを管理するオブジェクト. */
  private EventManager eventManager = new EventManager();

  /** コンストラクタ. */
  public WorkspaceSet() {}

  /**
   * ワークスペースを追加する.
   *
   * @param workspace 追加するワークスペース
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addWorkspace(Workspace workspace, UserOperation userOpe) {
    workspaceSet.add(workspace);
    workspace.setWorkspaceSet(this);
    workspace.getEventManager().addOnNodeSelectionStateChanged(
        eventManager.onNodeSelectionStateChanged);
    workspace.getEventManager().addOnNodeRemoved(eventManager.onNodeRemovedFromWs);
    eventManager.invokeOnWorkspaceAdded(workspace, userOpe);
    userOpe.pushCmdOfAddWorkspace(workspace);
  }

  /**
   * ワークスペースを取り除く.
   *
   * @param workspace 取り除くワークスペース
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void removeWorkspace(Workspace workspace, UserOperation userOpe) {
    workspaceSet.remove(workspace);
    workspace.setWorkspaceSet(null);
    deleteNodesInWorkspace(workspace, userOpe);
    workspace.getEventManager().removeOnNodeSelectionStateChanged(
        eventManager.onNodeSelectionStateChanged);
    workspace.getEventManager().removeOnNodeRemoved(eventManager.onNodeRemovedFromWs);
    eventManager.invokeOnWorkspaceRemoved(workspace, userOpe);
    userOpe.pushCmdOfRemoveWorkspace(workspace, this);
  }

  /** {@link ws} の中のノードを全て消す. */
  private void deleteNodesInWorkspace(Workspace ws, UserOperation userOpe) {
    SequencedSet<BhNode> rootNodes = ws.getRootNodes();
    var nodesToDelete = rootNodes.stream()
        .filter(node -> node.getHookAgent().execOnDeletionRequested(
            new ArrayList<>(rootNodes), CauseOfDeletion.WORKSPACE_DELETION, userOpe))
        .toList();
    List<Swapped> swappedNodes = BhService.bhNodePlacer().deleteNodes(nodesToDelete, userOpe);
    for (var swapped : swappedNodes) {
      swapped.newNode().findParentNode().getHookAgent().execOnChildReplaced(
          swapped.oldNode(),
          swapped.newNode(),
          swapped.newNode().getParentConnector(),
          userOpe);
    }
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
      eventManager.invokeOnCopyNodeAdded(toAdd, userOpe);
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
      eventManager.invokeOnCopyNodeRemoved(toRemove, userOpe);
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
      eventManager.invokeOnCutNodeAdded(toAdd, userOpe);
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
      eventManager.invokeOnCutNodeRemoved(toRemove, userOpe);
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
    return target.getHookAgent()
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
        .filter(node -> node.getHookAgent().execOnCutRequested(candidates, userOpe))
        .collect(Collectors.toCollection(ArrayList::new));

    // 貼り付け処理
    for (var node : nodesToPaste) {
      SequencedSet<Swapped> swappedNodes = BhService.bhNodePlacer().moveToWs(
          wsToPasteIn,
          node,
          pasteBasePos.x,
          pasteBasePos.y + pastePosOffsetCount * BhConstants.LnF.REPLACED_NODE_SHIFT * 2,
          userOpe);
      Vec2D size = node.getViewProxy().getSizeIncludingOuters(true);
      pasteBasePos.x += size.x + BhConstants.LnF.REPLACED_NODE_SHIFT * 2;
      execHookOnPaste(node, swappedNodes, userOpe);
    }

    pastePosOffsetCount = (pastePosOffsetCount > 2) ? -2 : ++pastePosOffsetCount;
    userOpe.pushCmdOfRemoveFromList(readyToCut, readyToCut);
    readyToCut.clear();
  }

  /** ペースト時のフック処理を実行する. */
  private void execHookOnPaste(
      BhNode node, SequencedSet<Swapped> swappedNodes, UserOperation userOpe) {
    if (!swappedNodes.isEmpty()) {
      node.getHookAgent().execOnMovedFromChildToWs(
          swappedNodes.getFirst().newNode().findParentNode(),
          swappedNodes.getFirst().newNode().findRootNode(),
          swappedNodes.getFirst().newNode(),
          true,
          userOpe);
    }
    for (var swapped : swappedNodes) {
      swapped.newNode().findParentNode().getHookAgent().execOnChildReplaced(
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
    return (node.isChild() && node.findRootNode().isRoot())
        || node.isRoot();
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
    eventManager.invokeOnCurrentWorkspaceChanged(old, currentWorkspace);
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
  
  /**
   * このワークスペースセットに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このワークスペースセットに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  public EventManager getEventManager() {
    return eventManager;
  }

  /** コピー元とコピーされた {@link BhNode} のペア. */
  private record OriginalAndCopy(BhNode org, BhNode copy) {}

  /** イベントハンドラの管理を行うクラス. */
  public class EventManager {

    /** ワークスペースが追加されたときのイベントハンドラのセット. */
    private final
        SequencedSet<TriConsumer<? super WorkspaceSet, ? super Workspace, ? super UserOperation>>
        onWorkspaceAddedList = new LinkedHashSet<>();
    /** ワークスペースが削除されたときのイベントハンドラのセット. */
    private final
        SequencedSet<TriConsumer<? super WorkspaceSet, ? super Workspace, ? super UserOperation>>
        onWorkspaceRemovedList = new LinkedHashSet<>();
    /** コピー予定ノードが追加されたときのイベントハンドラのセット. */
    private final
        SequencedSet<TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation>>
        onCopyNodeAddedList = new LinkedHashSet<>();
    /** コピー予定ノードが削除されたときのイベントハンドラのセット. */
    private final
        SequencedSet<TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation>>
        onCopyNodeRemovedList = new LinkedHashSet<>();
    /** カット予定ノードが追加されたときのイベントハンドラのセット. */
    private final
        SequencedSet<TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation>>
        onCutNodeAddedList = new LinkedHashSet<>();
    /** カット予定ノードが削除されたときのイベントハンドラのセット. */
    private final
        SequencedSet<TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation>>
        onCutNodeRemovedList = new LinkedHashSet<>();
    /**
     * <pre>
     * ノードが選択されたときに呼び出すイベントハンドラのセット.
     * イベントハンドラの第1引数 : 変化のあった選択ノード
     * イベントハンドラの第2引数 : ノードの選択状態
     * 呼び出しスレッドのフラグ : true ならUIスレッドで呼び出すことを保証する
     * </pre>
     */
    private final SequencedSet<TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation>>
        onNodeSelectionStateChangedList = new LinkedHashSet<>();
    /**
     * <pre>
     * 操作対象のワークスペースが切り替わった時に呼び出すイベントハンドラのセット.
     * 第1引数 : 前の操作対象のワークスペース
     * 第2引数 : 新しい操作対象のワークスペース
     * </pre>
     */
    private final
        SequencedSet<BiConsumer<? super Workspace, ? super Workspace>>
        onCurrentWorkspaceChangedList = new LinkedHashSet<>();

    /** ノードの選択状態が変わったときのイベントハンドラを呼ぶ関数オブジェクト. */
    private final TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation>
        onNodeSelectionStateChanged = this::invokeOnNodeSelectionStateChanged;
    /** ワークスペースからノードが削除されたときのイベントハンドラを呼ぶ関数オブジェクト. */
    private final TriConsumer<? super Workspace, ? super BhNode, ? super UserOperation>
        onNodeRemovedFromWs = this::invokeOnNodeRemovedFromWs;
    
    /**
     * ワークスペースが追加されたときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnWorkspaceAdded(
        TriConsumer<? super WorkspaceSet, ? super Workspace, ? super UserOperation> handler) {
      onWorkspaceAddedList.addLast(handler);
    }

    /**
     * ワークスペースが追加されたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnWorkspaceAdded(
        TriConsumer<? super WorkspaceSet, ? super Workspace, ? super UserOperation> handler) {
      onWorkspaceAddedList.remove(handler);
    }

    /**
     * ワークスペースが削除されたときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnWorkspaceRemoved(
        TriConsumer<? super WorkspaceSet, ? super Workspace, ? super UserOperation> handler) {
      onWorkspaceRemovedList.addLast(handler);
    }

    /**
     * ワークスペースが削除されたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnWorkspaceRemoved(
        TriConsumer<? super WorkspaceSet, ? super Workspace, ? super UserOperation> handler) {
      onWorkspaceRemovedList.remove(handler);
    }

    /**
     * コピー予定のノードが追加されたときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnCopyNodeAdded(
        TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation> handler) {
      onCopyNodeAddedList.addLast(handler);
    }

    /**
     * コピー予定のノードが追加されたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnCopyNodeAdded(
        TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation> handler) {
      onCopyNodeAddedList.remove(handler);
    }

    /**
     * コピー予定のノードが削除されたときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnCopyNodeRemoved(
        TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation> handler) {
      onCopyNodeRemovedList.addLast(handler);
    }

    /**
     * コピー予定のノードが削除されたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnCopyNodeRemoved(
        TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation> handler) {
      onCopyNodeRemovedList.remove(handler);
    }


    /**
     * カット予定のノードが追加されたときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnCutNodeAdded(
        TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation> handler) {
      onCutNodeAddedList.addLast(handler);
    }

    /**
     * カット予定のノードが追加されたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnCutNodeAdded(
        TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation> handler) {
      onCutNodeAddedList.remove(handler);
    }

    /**
     * カット予定のノードが削除されたときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnCutNodeRemoved(
        TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation> handler) {
      onCutNodeRemovedList.addLast(handler);
    }

    /**
     * カット予定のノードが削除されたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnCutNodeRemoved(
        TriConsumer<? super WorkspaceSet, ? super BhNode, ? super UserOperation> handler) {
      onCutNodeRemovedList.remove(handler);
    }

    /**
     * ノードの選択状態に変化があった時のイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ
     *     <pre>
     *     handler 第1引数 : 選択状態に変化のあったノード
     *     handler 第2引数 : 選択状態.  選択されたとき true.
     *     </pre>
     */
    public void addOnNodeSelectionStateChanged(
        TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation> handler) {
      onNodeSelectionStateChangedList.addLast(handler);
    }

    /**
     * ノードの選択状態に変化があった時のイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnNodeSelectionStateChanged(
        TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation> handler) {
      onNodeSelectionStateChangedList.remove(handler);
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
     */
    public void addOnCurrentWorkspaceChanged(
        BiConsumer<? super Workspace, ? super Workspace> handler) {
      onCurrentWorkspaceChangedList.addLast(handler);
    }

    /**
     * 操作対象のワークスペースが変わった時のイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnCurrentWorkspaceChanged(
        BiConsumer<? super Workspace, ? super Workspace> handler) {
      onCurrentWorkspaceChangedList.remove(handler);
    }

    /** このワークスペースセットにワークスペースが追加されたときのイベントハンドラを呼び出す. */
    private void invokeOnWorkspaceAdded(Workspace ws, UserOperation userOpe) {
      onWorkspaceAddedList.forEach(handler -> handler.accept(WorkspaceSet.this, ws, userOpe));
    }

    /** このワークスペースセットにワークスペースが追加されたときのイベントハンドラを呼び出す. */
    private void invokeOnWorkspaceRemoved(Workspace ws, UserOperation userOpe) {
      onWorkspaceRemovedList.forEach(handler -> handler.accept(WorkspaceSet.this, ws, userOpe));
    }

    /** このワークスペースセットにコピー予定のノードが追加されたときのイベントハンドラを呼び出す. */
    private void invokeOnCopyNodeAdded(BhNode node, UserOperation userOpe) {
      onCopyNodeAddedList.forEach(handler -> handler.accept(WorkspaceSet.this, node, userOpe));
    }

    /** このワークスペースセットからコピー予定のノードが削除されたときのイベントハンドラを呼び出す. */
    private void invokeOnCopyNodeRemoved(BhNode node, UserOperation userOpe) {
      onCopyNodeRemovedList.forEach(handler -> handler.accept(WorkspaceSet.this, node, userOpe));
    }

    /** このワークスペースセットにカット予定のノードが追加されたときのイベントハンドラを呼び出す. */
    private void invokeOnCutNodeAdded(BhNode node, UserOperation userOpe) {
      onCutNodeAddedList.forEach(handler -> handler.accept(WorkspaceSet.this, node, userOpe));
    }

    /** このワークスペースセットからカット予定のノードが削除されたときのイベントハンドラを呼び出す. */
    private void invokeOnCutNodeRemoved(BhNode node, UserOperation userOpe) {
      onCutNodeRemovedList.forEach(handler -> handler.accept(WorkspaceSet.this, node, userOpe));
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
      onNodeSelectionStateChangedList.forEach(
          handler -> handler.accept(selectedNode, isSelected, userOpe));
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

    /**
     * 操作対象のワークスペースが変わった時のイベントハンドラを呼ぶ.
     *
     * @param oldWs 前の操作対象のワークスペース
     * @param newWs 新しい操作対象のワークスペース
     */
    private void invokeOnCurrentWorkspaceChanged(Workspace oldWs, Workspace newWs) {
      onCurrentWorkspaceChangedList.forEach(handler -> handler.accept(oldWs, newWs));
    }
  }
}
