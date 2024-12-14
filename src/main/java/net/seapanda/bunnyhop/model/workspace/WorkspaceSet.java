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
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import net.seapanda.bunnyhop.command.BhCmd;
import net.seapanda.bunnyhop.command.CmdData;
import net.seapanda.bunnyhop.command.CmdDispatcher;
import net.seapanda.bunnyhop.command.CmdProcessor;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.export.CorruptedSaveDataException;
import net.seapanda.bunnyhop.export.IncompatibleSaveFormatException;
import net.seapanda.bunnyhop.export.ProjectExporter;
import net.seapanda.bunnyhop.export.ProjectImporter;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.BhNode.Swapped;
import net.seapanda.bunnyhop.model.traverse.NodeMvcBuilder;
import net.seapanda.bunnyhop.model.traverse.TextPrompter;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.service.ModelExclusiveControl;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.Pair;
import net.seapanda.bunnyhop.utility.Vec2D;

/**
 * ワークスペースの集合を保持、管理するクラス.
 *
 * @author K.Koike
 */
public class WorkspaceSet implements CmdDispatcher {
  /** コピー予定のノード. */
  private final ObservableList<BhNode> readyToCopy = FXCollections.observableArrayList();
  /** カット予定のノード. */
  private final ObservableList<BhNode> readyToCut = FXCollections.observableArrayList();
  /** 全てのワークスペース. */
  private final List<Workspace> workspaceList = new ArrayList<>();
  /** このオブジェクト宛てに送られたメッセージを処理するオブジェクト. */
  private CmdProcessor msgProcessor = (msg, data) -> null;
  /** ノードの貼り付け位置をずらすためのカウンタ. */
  private int pastePosOffsetCount = -2;
  /** 保存後に変更されたかどうかのフラグ. */
  private boolean isDirty = false;
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
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addNodesToCopyList(
      Collection<BhNode> nodeList, UserOperation userOpe) {
    clearCutList(userOpe);
    clearCopyList(userOpe);
    readyToCopy.addAll(nodeList);
    userOpe.pushCmdOfAddToList(readyToCopy, nodeList);
  }

  /**
   * コピー予定のBhNodeリストをクリアする.
   *
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void clearCopyList(UserOperation userOpe) {
    userOpe.pushCmdOfRemoveFromList(readyToCopy, readyToCopy);
    readyToCopy.clear();
  }

  /**
   * コピー予定のノードリストからノードを取り除く.
   *
   * @param nodeToRemove 取り除くノード
   * @param userOpe undo 用コマンドオブジェクト
   * */
  public void removeNodeFromCopyList(
      BhNode nodeToRemove, UserOperation userOpe) {
    if (readyToCopy.contains(nodeToRemove)) {
      userOpe.pushCmdOfRemoveFromList(readyToCopy, nodeToRemove);
      readyToCopy.remove(nodeToRemove);
    }
  }

  /**
   * カット予定のBhNodeリストを追加する.
   *
   * @param nodeList カット予定のBhNodeリスト
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void addNodesToCutList(
      Collection<BhNode> nodeList, UserOperation userOpe) {
    clearCutList(userOpe);
    clearCopyList(userOpe);
    readyToCut.addAll(nodeList);
    userOpe.pushCmdOfAddToList(readyToCut, nodeList);
  }

  /**
   * カット予定のBhNodeリストをクリアする.
   *
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void clearCutList(UserOperation userOpe) {
    userOpe.pushCmdOfRemoveFromList(readyToCut, readyToCut);
    readyToCut.clear();
  }

  /**
   * カット予定のノードリストからノードを取り除く.
   *
   * @param nodeToRemove 取り除くノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void removeNodeFromCutList(BhNode nodeToRemove, UserOperation userOpe) {
    if (readyToCut.contains(nodeToRemove)) {
      userOpe.pushCmdOfRemoveFromList(readyToCut, nodeToRemove);
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
    UserOperation userOpe = new UserOperation();
    copyAndPaste(wsToPasteIn, pasteBasePos, userOpe);
    cutAndPaste(wsToPasteIn, pasteBasePos, userOpe);
    BhService.compileErrNodeManager().updateErrorNodeIndicator(userOpe);
    BhService.compileErrNodeManager().unmanageNonErrorNodes(userOpe);
    BhService.derivativeCache().clearAll();
    BhService.undoRedoAgent().pushUndoCommand(userOpe);
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

    Collection<Pair<BhNode, BhNode>> orgsAndCopies = candidates.stream()
        .map(node ->
            new Pair<BhNode, BhNode>(node, genCopyNode(node, candidates, userOpe)))
        .filter(orgAndCopy -> orgAndCopy.v2 != null)
        .collect(Collectors.toCollection(ArrayList::new));

    // 貼り付け処理
    for (var orgAndCopy : orgsAndCopies) {
      BhNode copy = orgAndCopy.v2;
      NodeMvcBuilder.build(copy);
      TextPrompter.prompt(copy);
      BhService.bhNodePlacer().moveToWs(
          wsToPasteIn,
          copy,
          pasteBasePos.x,
          pasteBasePos.y + pastePosOffsetCount * BhConstants.LnF.REPLACED_NODE_SHIFT * 2,
          userOpe);
      //コピー直後のノードは大きさが未確定なので, コピー元ノードの大きさを元に貼り付け位置を算出する.
      Vec2D size = BhService.cmdProxy().getViewSizeIncludingOuter(orgAndCopy.v1);
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
      Vec2D size = BhService.cmdProxy().getViewSizeIncludingOuter(node);
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
  public List<Workspace> getWorkspaceList() {
    return new ArrayList<>(workspaceList);
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
   * @param fileToSave 保存先のファイル
   * @return セーブに成功した場合 true
   */
  public boolean save(File fileToSave) {
    ModelExclusiveControl.lockForRead();
    try {
      ProjectExporter.export(workspaceList, fileToSave.toPath());
      BhService.msgPrinter().infoForUser(TextDefs.Export.hasSaved.get(fileToSave.getPath()));
      isDirty = false;
      return true;
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(
          "Failed to save the project.\n%s\n%s".formatted(fileToSave.getPath(), e));
      BhService.msgPrinter().alert(
          Alert.AlertType.ERROR,
          TextDefs.Export.InformFailedToSave.title.get(),
          null,
          fileToSave.getPath() + "\n" + e);
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
      result.workspaces().forEach(ws -> BhService.cmdProxy().addWorkspace(ws, userOpe));
      result.workspaces().stream()
          .flatMap(ws -> ws.getRootNodeList().stream())
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
    for (Workspace ws : new ArrayList<>(workspaceList)) {
      BhService.cmdProxy().deleteWorkspace(ws, userOpe);
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
   * @param invokeOnUiThread UI スレッド上で呼び出す場合 true
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

  /** ワークスペースセットが保存後に変更されていることを示すフラグをセットする. */
  public void setDirty() {
    isDirty = true;
  }

  /** ワークスペースセットが保存後に変更されている場合 true を返す. */
  public boolean isDirty() {
    return isDirty;
  }

  @Override
  public void setMsgProcessor(CmdProcessor processor) {
    msgProcessor = processor;
  }

  @Override
  public CmdData dispatch(BhCmd msg, CmdData data) {
    return msgProcessor.process(msg, data);
  }
}
