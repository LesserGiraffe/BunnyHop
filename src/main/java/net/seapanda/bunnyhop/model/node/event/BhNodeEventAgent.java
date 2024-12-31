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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.undo.UserOperation;
import org.mozilla.javascript.Context;
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

  /** スクリプト実行時のスコープ変数を作成する. */
  public ScriptableObject createScriptScope(Context cx, Map<String, Object> nameToObj) {
    nameToObj = new HashMap<>(nameToObj);
    nameToObj.put(BhConstants.JsIdName.BH_THIS, target);
    nameToObj.put(BhConstants.JsIdName.BH_NODE_PLACER, BhService.bhNodePlacer());
    nameToObj.put(BhConstants.JsIdName.BH_COMMON, BhService.bhScriptManager().getCommonJsObj());
    nameToObj.put(BhConstants.JsIdName.BH_NODE_FACTORY, BhService.bhNodeFactory());
    nameToObj.put(BhConstants.JsIdName.BH_TEXT_DB, BhService.textDb());

    ScriptableObject scope = cx.initStandardObjects();
    for (String name : nameToObj.keySet()) {
      Object val = Context.javaToJS(nameToObj.get(name), scope);
      scope.put(name, scope, val);
    }
    return scope;
  }

  /** スクリプト実行時のスコープ変数を作成する. */
  public ScriptableObject createScriptScope(Context cx) {
    return createScriptScope(cx, new HashMap<>());
  }

  /**
   * 子ノードに移ったときの処理を実行する.
   *
   * @param oldParent 移る前に接続されていた親. ワークスペースから子ノードに移動したときは null.
   * @param oldRoot 移る前に所属していたノードツリーのルートノード.
   *                ワークスペースから子ノードに移動したときは, この処理を呼び出すノードを指定すること.
   * @param oldReplaced このノードがつながった位置に, 元々子ノードとしてつながっていたノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void execOnMovedToChild(
      ConnectiveNode oldParent,
      BhNode oldRoot,
      BhNode oldReplaced,
      UserOperation userOpe) {
    Optional<String> scriptName = target.getScriptName(BhNodeEvent.ON_MOVED_TO_CHILD);
    Script onMovedToChild =
        scriptName.map(BhService.bhScriptManager()::getCompiledScript).orElse(null);
    if (onMovedToChild == null) {
      return;
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_OLD_PARENT, oldParent);
        put(BhConstants.JsIdName.BH_OLD_ROOT, oldRoot);
        put(BhConstants.JsIdName.BH_REPLACED_OLD_NODE, oldReplaced);
        put(BhConstants.JsIdName.BH_USER_OPE, userOpe);
      }};
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, nameToObj);
    try {
      onMovedToChild.exec(cx, scope);
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(scriptName.get() + "\n" + e);
    } finally {
      Context.exit();
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
        scriptName.map(BhService.bhScriptManager()::getCompiledScript).orElse(null);
    if (onMovedFromChildToWs == null) {
      return;
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_OLD_PARENT, oldParent);
        put(BhConstants.JsIdName.BH_OLD_ROOT, oldRoot);
        put(BhConstants.JsIdName.BH_REPLACED_NEW_NODE, newReplaced);
        put(BhConstants.JsIdName.BH_IS_SPECIFIED_DIRECTLY, isSpecifiedDirectly);
        put(BhConstants.JsIdName.BH_USER_OPE, userOpe);
      }};
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, nameToObj);
    try {
      onMovedFromChildToWs.exec(cx, scope);
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(scriptName.get() + "\n" + e);
    } finally {
      Context.exit();
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
        scriptName.map(BhService.bhScriptManager()::getCompiledScript).orElse(null);
    if (onChildReplaced == null) {
      return;
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_REPLACED_NEW_NODE, newChild);
        put(BhConstants.JsIdName.BH_REPLACED_OLD_NODE, oldChild);
        put(BhConstants.JsIdName.BH_PARENT_CONNECTOR, parentCnctr);
        put(BhConstants.JsIdName.BH_USER_OPE, userOpe);
      }};
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, nameToObj);
    try {
      onChildReplaced.exec(cx, scope);
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(scriptName.get() + "\n" + e);
    } finally {
      Context.exit();
    }
  }

  /**
   * このノードの削除前に呼ばれる処理を実行する.
   *
   * <pre>
   * この関数を呼び出す対象となるノードを削除原因ごとに記す.
   *   TRASH_BOX -> ゴミ箱の上に D&D されたノード
   *   COMPILE_ERROR -> コンパイルエラーを起こしているノード
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
        scriptName.map(BhService.bhScriptManager()::getCompiledScript).orElse(null);
    if (onDeletionRequested == null) {
      return true;
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_CANDIDATE_NODE_LIST, new ArrayList<>(nodesToDelete));
        put(BhConstants.JsIdName.BH_CAUSE_OF_DELETION, causeOfDeletion);
        put(BhConstants.JsIdName.BH_USER_OPE, userOpe);
      }};
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, nameToObj);
    try {
      return (Boolean) onDeletionRequested.exec(cx, scope);
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(
          "'%s' must return a boolean value.\n%s".formatted(scriptName.get(), e));
    } finally {
      Context.exit();
    }
    return true;
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
        scriptName.map(BhService.bhScriptManager()::getCompiledScript).orElse(null);
    if (onCutRequested == null) {
      return true;
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_CANDIDATE_NODE_LIST, new ArrayList<>(nodesToCut));
        put(BhConstants.JsIdName.BH_USER_OPE, userOpe);
      }};
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, nameToObj);
    try {
      return (Boolean) onCutRequested.exec(cx, scope);
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(
          "'%s' must return a boolean value.\n%s".formatted(scriptName.get(), e));
    } finally {
      Context.exit();
    }
    return true;
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
        scriptName.map(BhService.bhScriptManager()::getCompiledScript).orElse(null);
    if (onCopyRequested == null) {
      return Optional.ofNullable(defaultFunc);
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_CANDIDATE_NODE_LIST, new ArrayList<>(nodesToCopy));
        put(BhConstants.JsIdName.BH_USER_OPE, userOpe);
      }};
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, nameToObj);
    try {
      Function copyCheckFunc = (Function) onCopyRequested.exec(cx, scope);
      return (copyCheckFunc == null)
          ?  Optional.empty()
          : Optional.of(node -> isNodeToCopy(node, copyCheckFunc, scriptName.get()));
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(String.format(
          "'%s' must return null or a function that returns a boolean value.\n%s", scriptName, e));
    } finally {
      Context.exit();
    }
    return Optional.ofNullable(defaultFunc);
  }

  /**
   * コピー判定関数.
   *
   * @param node このノードをコピーするか判定する.
   * @param copyCheckFunc このメソッドが呼び出すコピー判定関数
   * @param scriptName {@code copyCheckFunc} を返したスクリプトの名前
   * @return {@code node} をコピーする場合 true
   */
  private boolean isNodeToCopy(BhNode node, Function copyCheckFunc, String scriptName) {
    Context cx = Context.enter();
    ScriptableObject scope = node.getEventAgent().createScriptScope(cx);
    try {
      return (Boolean) copyCheckFunc.call(cx, scope, scope, new Object[] {node});
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(String.format(
          "'%s' must return null or a function that returns a boolean value.\n%s", scriptName, e));
      throw e;
    } finally {
      Context.exit();
    }
  }

  /**
   * テンプレートノードが作成されたときの処理を実行する.
   *
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void execOnTemplateCreated(UserOperation userOpe) {
    Optional<String> scriptName = target.getScriptName(BhNodeEvent.ON_TEMPLATE_CREATED);
    Script onTemplateCreated =
        scriptName.map(BhService.bhScriptManager()::getCompiledScript).orElse(null);
    if (onTemplateCreated == null) {
      return;
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_USER_OPE, userOpe);
      }};
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, nameToObj);
    try {
      onTemplateCreated.exec(cx, scope);
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(scriptName.get() + "\n" + e);
    } finally {
      Context.exit();
    }
  }

  /**
   * ノードのドラッグが始まったときの処理を実行する.
   *
   * @param eventInfo ドラッグ操作に関連するマウスイベントを格納したオブジェクト
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void execOnDragStarted(MouseEventInfo eventInfo, UserOperation userOpe) {
    Optional<String> scriptName = target.getScriptName(BhNodeEvent.ON_DRAG_STARTED);
    Script onDragStarted =
        scriptName.map(BhService.bhScriptManager()::getCompiledScript).orElse(null);
    if (onDragStarted == null) {
      return;
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_MOUSE_EVENT, eventInfo);
        put(BhConstants.JsIdName.BH_USER_OPE, userOpe);
      }};
    Context cx = Context.enter();
    ScriptableObject scope = createScriptScope(cx, nameToObj);
    try {
      onDragStarted.exec(cx, scope);
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(scriptName.get() + "\n" + e);
    } finally {
      Context.exit();
    }
  }

  /** マウスイベントに関連する情報を格納するクラス. */
  public static class MouseEventInfo {

    public final boolean isFromPrimaryButton;
    public final boolean isFromSecondaryButton;
    public final boolean isFromMiddleButton;
    public final boolean isFromBackButton;
    public final boolean isFromforwardButton;
    public final boolean isShiftDown;
    public final boolean isCtrlDown;
    public final boolean isAltDown;
    
    /** コンストラクタ. */
    public MouseEventInfo(
        boolean isFromPrimaryButton,
        boolean isFromSecondaryButton,
        boolean isFromMiddleButton,
        boolean isFromBackButton,
        boolean isFromforwardButton,
        boolean isShiftDown,
        boolean isCtrlDown,
        boolean isAltDown) {
      this.isFromPrimaryButton = isFromPrimaryButton;
      this.isFromSecondaryButton = isFromSecondaryButton;
      this.isFromMiddleButton = isFromMiddleButton;
      this.isFromBackButton = isFromBackButton;
      this.isFromforwardButton = isFromforwardButton;
      this.isShiftDown = isShiftDown;
      this.isCtrlDown = isCtrlDown;
      this.isAltDown = isAltDown;
    }
  }
}
