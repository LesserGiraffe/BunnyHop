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

package net.seapanda.bunnyhop.model.node.event;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.common.constant.VersionInfo;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.configfilereader.BhScriptManager;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.model.templates.BhNodeTemplates;
import net.seapanda.bunnyhop.modelservice.BhNodeHandler;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

/**
 * BhNode に登録されたイベントごとに対応する処理を呼び出すクラス.
 *
 * @author K.Koike
 */
public class BhNodeEventDispatcher implements Serializable {

  private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
  private final BhNode target;

  /**
   * コンストラクタ.
   *
   * @param target このノードに登録されたイベントを処理する.
   */
  public BhNodeEventDispatcher(BhNode target) {
    this.target = target;
  }

  /**
   * 初期値が設定されたスクリプトスコープを作成する.
   */
  public ScriptableObject newDefaultScriptScope() {
    ScriptableObject scriptScope = BhScriptManager.INSTANCE.createScriptScope();
    ScriptableObject.putProperty(scriptScope, BhConstants.JsKeyword.KEY_BH_THIS, target);
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_NODE_HANDLER, BhNodeHandler.INSTANCE);
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_MSG_SERVICE, MsgService.INSTANCE);
    ScriptableObject.putProperty(
        scriptScope,
        BhConstants.JsKeyword.KEY_BH_COMMON,
        BhScriptManager.INSTANCE.getCommonJsObj());
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_NODE_TEMPLATES, BhNodeTemplates.INSTANCE);

    return scriptScope;
  }


  /**
   * 子ノードに移ったときの処理を実行する.
   *
   * @param oldParent 移る前に接続されていた親. ワークスペースから子ノードに移動したときはnull.
   * @param oldRoot 移る前に所属していたノードツリーのルートノード. ワークスペースから子ノードに移動したときは, このオブジェクト.
   * @param oldReplaced 元々子ノードとしてつながっていたノード
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void execOnMovedToChild(
      ConnectiveNode oldParent,
      BhNode oldRoot,
      BhNode oldReplaced,
      UserOperationCommand userOpeCmd) {

    Optional<String> scriptName = target.getScriptName(BhNodeEvent.ON_MOVED_TO_CHILD);
    Script onMovedToChild =
        scriptName.map(BhScriptManager.INSTANCE::getCompiledScript).orElse(null);
    if (onMovedToChild == null) {
      return;
    }
    ScriptableObject scriptScope = newDefaultScriptScope();
    ScriptableObject.putProperty(scriptScope, BhConstants.JsKeyword.KEY_BH_OLD_PARENT, oldParent);
    ScriptableObject.putProperty(scriptScope, BhConstants.JsKeyword.KEY_BH_OLD_ROOT, oldRoot);
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_REPLACED_OLD_NODE, oldReplaced);
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_USER_OPE_CMD, userOpeCmd);
    try {
      ContextFactory.getGlobal().call(cx -> onMovedToChild.exec(cx, scriptScope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          Util.INSTANCE.getCurrentMethodName() +  " - " + scriptName.get() + "\n" + e + "\n");
    }
  }

  /**
   * 子ノードからワークスペースに移ったときの処理を実行する.
   *
   * @param oldParent 移る前に接続されていた親
   * @param oldRoot 移る前に所属していたルートノード
   * @param newReplaced WSに移る際, このノードの替わりにつながったノード
   * @param manuallyRemoved D&Dで子ノードからワークスペースに移された場合 true
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void execOnMovedFromChildToWs(
      ConnectiveNode oldParent,
      BhNode oldRoot,
      BhNode newReplaced,
      Boolean manuallyRemoved,
      UserOperationCommand userOpeCmd) {
    Optional<String> scriptName = target.getScriptName(BhNodeEvent.ON_MOVED_FROM_CHILD_TO_WS);
    Script onMovedFromChildToWs =
        scriptName.map(BhScriptManager.INSTANCE::getCompiledScript).orElse(null);
    if (onMovedFromChildToWs == null) {
      return;
    }
    ScriptableObject scriptScope = newDefaultScriptScope();
    ScriptableObject.putProperty(scriptScope, BhConstants.JsKeyword.KEY_BH_OLD_PARENT, oldParent);
    ScriptableObject.putProperty(scriptScope, BhConstants.JsKeyword.KEY_BH_OLD_ROOT, oldRoot);
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_REPLACED_NEW_NODE, newReplaced);
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_MANUALLY_REMOVED, manuallyRemoved);
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_USER_OPE_CMD, userOpeCmd);
    try {
      ContextFactory.getGlobal().call(cx -> onMovedFromChildToWs.exec(cx, scriptScope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          Util.INSTANCE.getCurrentMethodName() + " - " + scriptName.get() + "\n" + e + "\n");
    }
  }

  /**
   * このノードの削除前に呼ばれる処理を実行する.
   *
   * <pre>
   * この関数を呼び出す対象となるノードを削除原因ごとに記す.
   *   ORIGINAL_DELETION -> オリジナルノード削除後に残っているすべてのイミテーションノード
   *   TRASH_BOX -> ゴミ箱の上に D&D されたノード
   *   SYNTAX_ERROR -> 構文エラーを起こしているノード
   *   SELECTED_FOR_DELETION ->  削除対象として選択されているノード
   *   WORKSPACE_DELETION -> 削除されるワークスペースにあるルートノード
   * </pre>
   *
   * @param nodesToDelete このノードと共に削除される予定のノード.
   * @param causeOfDeletion ノードの削除原因
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return 削除をキャンセルする場合 false. 続行する場合 true.
   */
  public boolean execOnDeletionRequested(
      Collection<? extends BhNode> nodesToDelete,
      CauseOfDeletion causeOfDeletion,
      UserOperationCommand userOpeCmd) {

    Optional<String> scriptName = target.getScriptName(BhNodeEvent.ON_DELETION_REQUESTED);
    Script onDeletionRequested =
        scriptName.map(BhScriptManager.INSTANCE::getCompiledScript).orElse(null);
    if (onDeletionRequested == null) {
      return true;
    }
    ScriptableObject scriptScope = newDefaultScriptScope();
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_CANDIDATE_NODE_LIST, nodesToDelete);
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_CAUSE_OF_DELETION, causeOfDeletion);
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_USER_OPE_CMD, userOpeCmd);
    Object doDeletion = null;
    try {
      doDeletion = ContextFactory.getGlobal().call(cx -> onDeletionRequested.exec(cx, scriptScope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          Util.INSTANCE.getCurrentMethodName() + " - " + scriptName.get() + "\n" + e + "\n");
    }

    if (doDeletion instanceof Boolean) {
      return (Boolean) doDeletion;
    }
    throw new AssertionError(scriptName.get() + " must return a boolean value.");
  }

  /**
   * ユーザー操作により, このノードがカット & ペーストされる直前に呼ばれる処理を実行する.
   *
   * @param nodesToCut このノードとともにカットされる予定のノード
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return カットをキャンセルする場合 false.  続行する場合 true.
   */
  public boolean execOnCutRequested(
      Collection<? extends BhNode> nodesToCut, UserOperationCommand userOpeCmd) {
    Optional<String> scriptName = target.getScriptName(BhNodeEvent.ON_CUT_REQUESTED);
    Script onCutRequested =
        scriptName.map(BhScriptManager.INSTANCE::getCompiledScript).orElse(null);
    if (onCutRequested == null) {
      return true;
    }
    Object doCut = null;
    ScriptableObject scriptScope = newDefaultScriptScope();
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_CANDIDATE_NODE_LIST, nodesToCut);
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_USER_OPE_CMD, userOpeCmd);
    try {
      doCut = ContextFactory.getGlobal().call(cx -> onCutRequested.exec(cx, scriptScope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          Util.INSTANCE.getCurrentMethodName() + " - " + scriptName.get() + "\n" + e + "\n");
    }

    if (doCut instanceof Boolean) {
      return (Boolean) doCut;
    }
    throw new AssertionError(scriptName.get() + " must return a boolean value.");
  }
}
