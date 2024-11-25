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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.service.BhNodeHandler;
import net.seapanda.bunnyhop.service.BhScriptManager;
import net.seapanda.bunnyhop.service.MsgPrinter;
import net.seapanda.bunnyhop.undo.UserOperation;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

/**
 * {@link BhNode} に登録されたイベントごとに対応する処理を実行するクラス.
 *
 * @author K.Koike
 */
public class BhNodeEventAgent implements Serializable {

  private final BhNode target;

  /**
   * コンストラクタ.
   *
   * @param target このノードに登録されたイベントを処理する.
   */
  public BhNodeEventAgent(BhNode target) {
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
        scriptScope, BhConstants.JsKeyword.KEY_BH_NODE_FACTORY, BhNodeFactory.INSTANCE);

    return scriptScope;
  }


  /**
   * 子ノードに移ったときの処理を実行する.
   *
   * @param oldParent 移る前に接続されていた親. ワークスペースから子ノードに移動したときはnull.
   * @param oldRoot 移る前に所属していたノードツリーのルートノード. ワークスペースから子ノードに移動したときは, このオブジェクト.
   * @param oldReplaced 元々子ノードとしてつながっていたノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void execOnMovedToChild(
      ConnectiveNode oldParent,
      BhNode oldRoot,
      BhNode oldReplaced,
      UserOperation userOpe) {
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
        scriptScope, BhConstants.JsKeyword.KEY_BH_USER_OPE, userOpe);
    try {
      ContextFactory.getGlobal().call(cx -> onMovedToChild.exec(cx, scriptScope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(scriptName.get() + "\n" + e);
    }
  }

  /**
   * 子ノードからワークスペースに移ったときの処理を実行する.
   *
   * @param oldParent 移る前に接続されていた親
   * @param oldRoot 移る前に所属していたルートノード
   * @param newReplaced ワークスペースに移る際, この処理を呼び出すノードの替わりにつながったノード
   * @param isSpecifiedDirectly この処理を呼び出すノードが, D&D やカット&ペーストで直接指定されてワークスペースに移動した場合 true
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void execOnMovedFromChildToWs(
      ConnectiveNode oldParent,
      BhNode oldRoot,
      BhNode newReplaced,
      Boolean isSpecifiedDirectly,
      UserOperation userOpe) {
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
        scriptScope, BhConstants.JsKeyword.KEY_BH_IS_SPECIFIED_DIRECTLY, isSpecifiedDirectly);
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_USER_OPE, userOpe);
    try {
      ContextFactory.getGlobal().call(cx -> onMovedFromChildToWs.exec(cx, scriptScope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(scriptName.get() + "\n" + e);
    }
  }

  /**
   * 子ノードが入れ替わったときの処理を実行する.
   *
   * @param oldChild 入れ替わった古いノード
   * @param newChild 入れ替わった新しいノード
   * @param parentCnctr 子が入れ替わったコネクタ
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void execOnChildReplaced(
      BhNode oldChild,
      BhNode newChild,
      Connector parentCnctr,
      UserOperation userOpe) {
    Optional<String> scriptName = target.getScriptName(BhNodeEvent.ON_CHILD_REPLACED);
    Script onChildReplaced =
        scriptName.map(BhScriptManager.INSTANCE::getCompiledScript).orElse(null);
    if (onChildReplaced == null) {
      return;
    }
    ScriptableObject scriptScope = newDefaultScriptScope();
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_REPLACED_NEW_NODE, newChild);
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_REPLACED_OLD_NODE, oldChild);
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_PARENT_CONNECTOR, parentCnctr);
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_USER_OPE, userOpe);
    try {
      ContextFactory.getGlobal().call(cx -> onChildReplaced.exec(cx, scriptScope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(scriptName.get() + "\n" + e);
    }
  }

  /**
   * このノードの削除前に呼ばれる処理を実行する.
   *
   * <pre>
   * この関数を呼び出す対象となるノードを削除原因ごとに記す.
   *   TRASH_BOX -> ゴミ箱の上に D&D されたノード
   *   SYNTAX_ERROR -> 構文エラーを起こしているノード
   *   SELECTED_FOR_DELETION ->  削除対象として選択されているノード
   *   WORKSPACE_DELETION -> 削除されるワークスペースにあるルートノード
   * </pre>
   *
   * @param nodesToDelete このノードと共に削除される予定のノード.
   * @param causeOfDeletion ノードの削除原因
   * @param userOpe undo 用コマンドオブジェクト
   * @return 削除をキャンセルする場合 false. 続行する場合 true.
   */
  public boolean execOnDeletionRequested(
      Collection<? extends BhNode> nodesToDelete,
      CauseOfDeletion causeOfDeletion,
      UserOperation userOpe) {
    Optional<String> scriptName = target.getScriptName(BhNodeEvent.ON_DELETION_REQUESTED);
    Script onDeletionRequested =
        scriptName.map(BhScriptManager.INSTANCE::getCompiledScript).orElse(null);
    if (onDeletionRequested == null) {
      return true;
    }
    ScriptableObject scriptScope = newDefaultScriptScope();
    ScriptableObject.putProperty(
        scriptScope,
        BhConstants.JsKeyword.KEY_BH_CANDIDATE_NODE_LIST,
        new ArrayList<>(nodesToDelete));
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_CAUSE_OF_DELETION, causeOfDeletion);
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_USER_OPE, userOpe);
    Object doDeletion = null;
    try {
      doDeletion = ContextFactory.getGlobal().call(cx -> onDeletionRequested.exec(cx, scriptScope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(scriptName.get() + "\n" + e);
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
   * @param userOpe undo 用コマンドオブジェクト
   * @return カットをキャンセルする場合 false.  続行する場合 true.
   */
  public boolean execOnCutRequested(
      Collection<? extends BhNode> nodesToCut, UserOperation userOpe) {
    Optional<String> scriptName = target.getScriptName(BhNodeEvent.ON_CUT_REQUESTED);
    Script onCutRequested =
        scriptName.map(BhScriptManager.INSTANCE::getCompiledScript).orElse(null);
    if (onCutRequested == null) {
      return true;
    }
    Object doCut = null;
    ScriptableObject scriptScope = newDefaultScriptScope();
    ScriptableObject.putProperty(
        scriptScope,
        BhConstants.JsKeyword.KEY_BH_CANDIDATE_NODE_LIST,
        new ArrayList<>(nodesToCut));
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_USER_OPE, userOpe);
    try {
      doCut = ContextFactory.getGlobal().call(cx -> onCutRequested.exec(cx, scriptScope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(scriptName.get() + "\n" + e);
    }

    if (doCut instanceof Boolean) {
      return (Boolean) doCut;
    }
    throw new AssertionError(scriptName.get() + " must return a boolean value.");
  }

  /**
   * ユーザー操作により, このノードがコピー & ペーストされる直前に呼ばれる処理を実行する.
   *
   * @param nodesToCopy このノードとともにコピーされる予定のノード
   * @param defaultFunc 対応する処理が定義されていなかった場合の処理.
   * @param userOpe undo 用コマンドオブジェクト
   * @return {@link BhNode} を引数にとり, コピーするかどうかの boolean 値を返す関数.
   */
  public Optional<Predicate<? super BhNode>> execOnCopyRequested(
      Collection<? extends BhNode> nodesToCopy,
      Predicate<? super BhNode> defaultFunc,
      UserOperation userOpe) {
    Optional<String> scriptName = target.getScriptName(BhNodeEvent.ON_COPY_REQUESTED);
    Script onCopyRequested =
        scriptName.map(BhScriptManager.INSTANCE::getCompiledScript).orElse(null);
    if (onCopyRequested == null) {
      return Optional.ofNullable(defaultFunc);
    }

    ScriptableObject scriptScope = target.getEventAgent().newDefaultScriptScope();
    ScriptableObject.putProperty(
        scriptScope,
        BhConstants.JsKeyword.KEY_BH_CANDIDATE_NODE_LIST,
        new ArrayList<>(nodesToCopy));
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_USER_OPE, userOpe);
    Object ret = null;
    try {
      ret = ContextFactory.getGlobal().call(cx -> onCopyRequested.exec(cx, scriptScope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(scriptName.get() + "\n" + e);
    }
    if (ret == null) {
      return Optional.empty();
    }
    if (ret instanceof Function copyCheckFunc) {
      return Optional.of(genCopyCheckFunc(copyCheckFunc, scriptName.get(), target));
    }
    throw new AssertionError(
        scriptName.get() + " must return null or a function that returns a boolean value.");
  }

  /**
   * コピー判定関数を作成する.
   *
   * @param copyCheckFunc 作成するコピー判定関数が呼び出す JavaScript の関数
   * @param scriptName copyCheckFunc を返したスクリプトの名前
   * @param caller コピー判定関数を呼ぶノード
   * @return コピー判定関数
   */
  private Predicate<BhNode> genCopyCheckFunc(
      Function copyCheckFunc, String scriptName, BhNode caller) {
    Predicate<BhNode> isNodeToCopy = node -> {
      ScriptableObject scriptScope = caller.getEventAgent().newDefaultScriptScope();
      Object retVal = ContextFactory.getGlobal().call(cx -> 
          ((Function) copyCheckFunc).call(cx, scriptScope, scriptScope, new Object[] {node}));
      if (!(retVal instanceof Boolean)) {
        MsgPrinter.INSTANCE.errMsgForDebug(String.format(
            "'%s' must return null or a function that returns a boolean value.", scriptName));
        throw new ClassCastException();
      }
      return (boolean) retVal;
    };
    return isNodeToCopy;
  }

  /**
   * テンプレートノードが作成されたときの処理を実行する.
   *
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void execOnTemplateCreated(UserOperation userOpe) {
    Optional<String> scriptName = target.getScriptName(BhNodeEvent.ON_TEMPLATE_CREATED);
    Script onTemplateCreated =
        scriptName.map(BhScriptManager.INSTANCE::getCompiledScript).orElse(null);
    if (onTemplateCreated == null) {
      return;
    }
    ScriptableObject scriptScope = newDefaultScriptScope();
    ScriptableObject.putProperty(scriptScope, BhConstants.JsKeyword.KEY_BH_USER_OPE, userOpe);
    try {
      ContextFactory.getGlobal().call(cx -> onTemplateCreated.exec(cx, scriptScope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(scriptName.get() + "\n" + e);
    }
  }
}
