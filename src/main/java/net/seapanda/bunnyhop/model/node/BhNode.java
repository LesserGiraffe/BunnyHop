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

package net.seapanda.bunnyhop.model.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.common.constant.VersionInfo;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.configfilereader.BhScriptManager;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.message.MsgProcessor;
import net.seapanda.bunnyhop.message.MsgReceptionWindow;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.connective.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.connective.Connector;
import net.seapanda.bunnyhop.model.node.event.BhNodeEvent;
import net.seapanda.bunnyhop.model.node.event.BhNodeEventDispatcher;
import net.seapanda.bunnyhop.model.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.model.templates.BhNodeAttributes;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.modelprocessor.ImitationReplacer;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

/**
 * ノードの基底クラス.
 *
 * @author K.Koike
 */
public abstract class BhNode extends SyntaxSymbol implements MsgReceptionWindow {

  private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
  /** ノードID (Node タグの bhID). */
  private final BhNodeId bhId;
  /** このノードを繋いでいるコネクタ. */
  protected Connector parentConnector;
  /** このノードがあるWorkSpace. */
  protected Workspace workspace;
  private Map<BhNodeEvent, String> eventToScriptName = new HashMap<>();
  private BhNodeEventDispatcher dispatcher = new BhNodeEventDispatcher(this);
  /** 最後にこのノードと入れ替わったノード. */
  private BhNode lastReplaced;
  /** デフォルトノードである場合true. */
  private boolean isDefault = false;
  /** このオブジェクト宛てに送られたメッセージを処理するオブジェクト. */
  private transient MsgProcessor msgProcessor;

  /** BhNode がとり得る状態. */
  public enum State {
    /** ワークスペース直下のルートノード. */
    ROOT_DIRECTLY_UNDER_WS,
    /** ワークスペース直下に無いルートノード (ダングリング状態). */
    ROOT_DANGLING,
    /** 子ノード (ルートがダングリング状態かどうかは問わない). */
    CHILD,
    /** 削除済み. */
    DELETED,
  }

  /**
   * 手動で子ノードからの取り外しができる場合 true を返す.
   * ルートノードの場合falseが返る.
   *
   * @return 手動で子ノードからの取り外しができる場合 true
   */
  public abstract boolean isRemovable();

  /**
   * 引数のノードをこのノードの代わりに置き換えることができる場合 true を返す.
   *
   * @param node 置き換え対象の BhNode
   * @return このノードの代わりに置き換えられる場合 true
   */
  public abstract boolean canBeReplacedWith(BhNode node);

  /**
   * このノードがイミテーションノードだった場合, そのオリジナルノードを返す.
   * イミテーションノードで無かった場合 null を返す.
   *
   * @return このノードのオリジナルノード. このノードがイミテーションノードで無い場合は null を返す.
   */
  public abstract BhNode getOriginal();

  /**
   * 外部ノードを取得する. 指定した世代にあたる外部ノードがなかった場合, nullを返す.
   *
   * @param generation
   *      <pre>
   *      取得する外部ノードの世代.
   *      例 (0: 自分, 1: 子世代にあたる外部ノード, 2: 孫世代にあたる外部ノード. 負の数: 末尾の外部ノードを取得する)
   *      </pre>
   * @return 外部ノード
   */
  public abstract BhNode findOuterNode(int generation);

  /**
   * このノード以下のノードツリーのコピーを作成する.
   *
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @param isNodeToBeCopied ノードがコピーの対象かどうかを判別する関数.
   *                         ただし, copyを呼んだBhNodeは判定対象にならず, 必ずコピーされる.
   * @return このノード以下のノードツリーのコピー
   */
  public abstract BhNode copy(UserOperationCommand userOpeCmd, Predicate<BhNode> isNodeToBeCopied);

  /**
   * コンストラクタ.
   *
   * @param type xml のtype属性
   * @param attrbute ノードの設定情報
   */
  protected BhNode(BhNodeAttributes attributes) {
    super(attributes.getName());
    this.bhId = attributes.getBhNodeId();
    registerScriptName(
        BhNodeEvent.ON_MOVED_FROM_CHILD_TO_WS, attributes.getOnMovedFromChildToWs());
    registerScriptName(BhNodeEvent.ON_MOVED_TO_CHILD, attributes.getOnMovedToChild());
    registerScriptName(BhNodeEvent.ON_DELETION_REQUESTED, attributes.getOnDeletionRequested());
    registerScriptName(BhNodeEvent.ON_CUT_REQUESTED, attributes.getOnCutRequested());
    registerScriptName(BhNodeEvent.ON_COPY_REQUESTED, attributes.getOnCopyRequested());
    registerScriptName(
        BhNodeEvent.ON_PRIVATE_TEMPLATE_CREATING, attributes.getOnPrivateTemplateCreating());
    registerScriptName(BhNodeEvent.ON_SYNTAX_CHECKING, attributes.getOnSyntaxChecking());
  }

  /**
   * コピーコンストラクタ.
   *
   * @param org コピー元オブジェクト
   */
  protected BhNode(BhNode org) {
    super(org);
    bhId = org.bhId;
    parentConnector = null;
    workspace = null;
    eventToScriptName = new HashMap<>(org.eventToScriptName);
    lastReplaced = null;
    isDefault = org.isDefault;
  }

  public BhNodeId getId() {
    return bhId;
  }

  /**
   * {@code newBhNode} とこのノードを入れ替える.
   *
   * @param newNode このノードと入れ替えるノード.
   * @param userOpeCmd undo 用コマンドオブジェクト.
   */
  public void replace(BhNode newNode, UserOperationCommand userOpeCmd) {
    setLastReplaced(newNode, userOpeCmd);
    parentConnector.connectNode(newNode, userOpeCmd);  //model 更新
    ImitationReplacer.replace(newNode, this, userOpeCmd);
  }

  /**
   * このノードをモデルツリーから取り除く. ノードの削除は行わない.
   *
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return このノードを取り除いた結果, 新しくできたノード
   */
  public BhNode remove(UserOperationCommand userOpeCmd) {
    return parentConnector.remove(userOpeCmd);
  }

  /**
   * ノードの状態を取得する.
   *
   * @retval State.DELETED 削除済みノード. 子ノードでも削除されていればこの値が返る.
   * @retval State.ROOT_DANGLING ルートノードであるが, ワークスペースにルートノードとして登録されていない
   * @retval State.ROOT_DIRECTLY_UNDER_WS ルートノードでかつ, ワークスペースにルートノードとして登録されている
   * @retval State.CHILD 子ノード. ワークスペースに属していてもいなくても子ノードならばこの値が返る
   */
  public State getState() {
    if (workspace == null) {
      return State.DELETED;
    } else if (parentConnector == null) {
      if (workspace.containsAsRoot(this)) {
        return State.ROOT_DIRECTLY_UNDER_WS;
      } else {
        return State.ROOT_DANGLING;
      }
    } else {
      if (workspace.containsAsRoot(this)) {
        throw new AssertionError("A child node is contained in a workspace as a root node.");
      }
      return State.CHILD;
    }
  }

  /**
   * 移動可能なノードであるかどうかを調べる.
   *
   * @return 移動可能なノードである場合 true
   */
  public boolean isMovable() {
    return isRemovable() || (getState() == BhNode.State.ROOT_DIRECTLY_UNDER_WS);
  }

  /**
   * 削除済みノードであるかどうか調べる.
   *
   * @return 削除済みノードである場合 true
   */
  public boolean isDeleted() {
    return getState() == BhNode.State.DELETED;
  }

  /**
   * 子ノードであるかどうか調べる.
   *
   * @return 子ノードである場合 true
   */
  public boolean isChild() {
    return getState() == BhNode.State.CHILD;
  }

  /**
   * ワークスペース直下のルートノードかどうか調べる.
   *
   * @return ワークスペース直下のルートノードである場合 true
   */
  public boolean isRootDirectolyUnderWs() {
    return getState() == BhNode.State.ROOT_DIRECTLY_UNDER_WS;
  }

  /**
   * ダングリング状態のルートノードかどうか調べる.
   *
   * @return ダングリング状態のルートノードである場合 true
   */
  public boolean isRootDangling() {
    return getState() == BhNode.State.ROOT_DANGLING;
  }

  /**
   * このノード固有のテンプレートノードがあるかどうか調べる.
   *
   * @return このノード固有のテンプレートノードがある場合 true
   */
  public boolean hasPrivateTemplateNodes() {
    return getScriptName(BhNodeEvent.ON_PRIVATE_TEMPLATE_CREATING).isPresent();
  }

  /**
   * 接続されるコネクタを登録する.
   *
   * @param parentConnector このノードと繋がるコネクタ
   */
  public void setParentConnector(Connector parentConnector) {
    this.parentConnector = parentConnector;
  }

  /**
   * このノードの親コネクタを返す.
   *
   * @return 親コネクタ. 親コネクタが無い場合は null を返す.
   */
  public Connector getParentConnector() {
    return parentConnector;
  }

  /**
   * このノードがあるワークスペースを登録する.
   *
   * @param workspace このノードを直接保持するワークスペース
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void setWorkspace(Workspace workspace, UserOperationCommand userOpeCmd) {
    userOpeCmd.pushCmdOfSetWorkspace(this.workspace, this);
    this.workspace = workspace;
  }

  /**
   * このノードがあるワークスペースを登録する.
   *
   * @return このノードがあるワークスペース
   */
  public Workspace getWorkspace() {
    return workspace;
  }

  /**
   * このノードがワークスペースに存在するノードである場合 true を返す.
   *
   * @return このノードがワークスペースに存在するノードである場合 true
   */
  public boolean isInWorkspace() {
    return workspace != null;
  }

  /**
   * このノードと入れ替わったノードをセットする.
   *
   * @param lastReplaced このノードと入れ替わったノード
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void setLastReplaced(BhNode lastReplaced, UserOperationCommand userOpeCmd) {
    userOpeCmd.pushCmdOfSetLastReplaced(this.lastReplaced, this);
    this.lastReplaced = lastReplaced;
  }

  /**
   * このノードと入れ替わったノードを取得する.
   *
   * @return 最後にこのオブジェクトと入れ替わったノード
   */
  public BhNode getLastReplaced() {
    return lastReplaced;
  }

  /**
   * このノードが選択されているかどうかを調べる.
   *
   * @return このノードが選択されている場合 trueを返す
   */
  public boolean isSelected() {
    if (workspace == null) {
      return false;
    }
    return workspace.getSelectedNodeList().contains(this);
  }

  /**
   * このノードがデフォルトノードかどうか調べる.
   *
   * @return このノードがデフォルトノードの場合true
   */
  public boolean isDefault() {
    return isDefault;
  }

  /**
   * このノードが外部ノードかどうか調べる.
   *
   * @retrun このノードが外部ノードの場合 true
   */
  public boolean isOuter() {
    if (parentConnector == null) {
      return false;
    }
    return parentConnector.isOuter();
  }

  /**
   * デフォルトノードかどうかを設定する.
   *
   * @param isDefault デフォルトノードの場合 true
   */
  public void setDefault(boolean isDefault) {
    this.isDefault = isDefault;
  }

  /**
   * このノードの親となるノードを返す. ルートノードの場合は null を返す.
   *
   * @return このノードの親となるノード
   */
  public ConnectiveNode findParentNode() {
    if (parentConnector == null) {
      return null;
    }
    return parentConnector.getParentNode();
  }

  /**
   * このノードのルートであるノードを返す <br> ルートノードの場合は自身を帰す.
   *
   * @return このノードのルートノード
   */
  public BhNode findRootNode() {
    if (parentConnector == null) {
      return this;
    }
    return parentConnector.getParentNode().findRootNode();
  }

  @Override
  public boolean isDescendantOf(SyntaxSymbol ancestor) {
    if (this == ancestor) {
      return true;
    }
    if (parentConnector == null) {
      return false;
    }
    return parentConnector.isDescendantOf(ancestor);
  }

  @Override
  public SyntaxSymbol findSymbolInAncestors(String symbolName, int generation, boolean toTop) {
    if (generation == 0) {
      if (Util.INSTANCE.equals(getSymbolName(), symbolName)) {
        return this;
      }
      if (!toTop) {
        return null;
      }
    }

    if (parentConnector == null) {
      return null;
    }
    return parentConnector.findSymbolInAncestors(symbolName, Math.max(0, generation - 1), toTop);
  }

  /** 引数で指定したイベント名に対応するスクリプト名を返す. */
  public Optional<String> getScriptName(BhNodeEvent event) {
    return Optional.ofNullable(eventToScriptName.get(event));
  }

  /**
   * このノードに関連するスクリプト名をイベントとともに登録する.
   *
   * @param event スクリプトに対応するイベント
   * @param scriptName 登録するスクリプト名.  null もしくは空文字の場合は登録しない.
   */
  protected void registerScriptName(BhNodeEvent event, String scriptName) {
    if (scriptName == null || scriptName.isEmpty()) {
      return;
    }
    eventToScriptName.put(event, scriptName);
  }

  /**
   * このノードに登録されたイベントとその処理を実行するオブジェクトを返す.
   *
   * @return このノードに登録されたイベントとその処理を実行するオブジェクト
   */
  public BhNodeEventDispatcher getEventDispatcher() {
    return dispatcher;
  }

  /**
   * このノードをコピーする.
   *
   * <p> 返されるノードの MVC は構築されていない. </p>
   *
   * @param nodesToCopy このノードとともにコピーされるノード
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return 作成したコピーノード.  コピーノードを作らなかった場合 null.
   */
  public BhNode genCopyNode(
      Collection<? extends BhNode> nodesToCopy, UserOperationCommand userOpeCmd) {
    Optional<String> scriptName = getScriptName(BhNodeEvent.ON_COPY_REQUESTED);
    Script onCopyRequested =
        scriptName.map(BhScriptManager.INSTANCE::getCompiledScript).orElse(null);
    if (onCopyRequested == null) {
      return copy(userOpeCmd, node -> true);
    }
    Object ret = null;
    ScriptableObject scriptScope = getEventDispatcher().newDefaultScriptScope();
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_CANDIDATE_NODE_LIST, nodesToCopy);
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_USER_OPE_CMD, userOpeCmd);
    try {
      ret = ContextFactory.getGlobal().call(cx -> onCopyRequested.exec(cx, scriptScope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          Util.INSTANCE.getCurrentMethodName() + " - " + scriptName.get() + "\n" + e + "\n");
    }

    if (ret == null) {
      return null;
    }
    if (ret instanceof Function) {
      Function copyCheckFunc = (Function) ret;
      return copy(userOpeCmd, genCopyCheckFunc(copyCheckFunc, scriptName.get()));
    }
    throw new AssertionError(
        scriptName.get() + " must return null or a function that returns a boolean value.");
  }

  /**
   * コピー判定関数を作成する.
   *
   * @param copyCheckFunc 作成するコピー判定関数が呼び出す Javascript の関数
   * @param scriptName copyCheckFunc を返したスクリプトの名前
   * @return コピー判定関数
   */
  private Predicate<BhNode> genCopyCheckFunc(Function copyCheckFunc, String scriptName) {
    Predicate<BhNode> isNodeToCopy = node -> {
      ScriptableObject scriptScope = getEventDispatcher().newDefaultScriptScope();
      Object retVal = ContextFactory.getGlobal().call(cx -> 
          ((Function) copyCheckFunc).call(cx, scriptScope, scriptScope, new Object[] {node}));
      if (!(retVal instanceof Boolean)) {
        String msg = scriptName + " must return null or a function that returns a boolean value.";
        MsgPrinter.INSTANCE.errMsgForDebug(msg);
        throw new ClassCastException();
      }
      return (boolean) retVal;
    };
    return isNodeToCopy;
  }

  /**
   * このノード固有のテンプレートノードを作成する.
   *
   * <p> 返されるノードの MVC は構築されていない. </p>
   *
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return このノード固有のテンプレートノードのリスト. 固有のテンプレートノードを持たない場合, 空のリストを返す.
   */
  public Collection<BhNode> genPrivateTemplateNodes(UserOperationCommand userOpeCmd) {
    Optional<String> scriptName = getScriptName(BhNodeEvent.ON_PRIVATE_TEMPLATE_CREATING);
    Script privateTemplateCreator =
        scriptName.map(BhScriptManager.INSTANCE::getCompiledScript).orElse(null);
    if (privateTemplateCreator == null) {
      return new ArrayList<>();
    }
    Object privateTemplateNodes = null;
    ScriptableObject scriptScope = getEventDispatcher().newDefaultScriptScope();
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_USER_OPE_CMD, userOpeCmd);
    try {
      privateTemplateNodes =
        ContextFactory.getGlobal().call(cx -> privateTemplateCreator.exec(cx, scriptScope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          Util.INSTANCE.getCurrentMethodName() + " - " + scriptName.get() + "\n" + e + "\n");
    }

    if (privateTemplateNodes instanceof Collection<?>) {
      return ((Collection<?>) privateTemplateNodes).stream()
          .filter(elem -> elem instanceof BhNode)
          .map(elem -> (BhNode) elem)
          .collect(Collectors.toCollection(ArrayList::new));
    }
    throw new AssertionError(scriptName.get() + " must return a collection of BhNode.");
  }

  /**
   * このノードに文法エラーがあるかどうか調べる.
   *
   * @return 文法エラーがある場合 true.  無い場合 false.
   */
  public boolean hasSyntaxError() {
    if (getState() == BhNode.State.DELETED) {
      return false;
    }
    Optional<String> scriptName = getScriptName(BhNodeEvent.ON_SYNTAX_CHECKING);
    Script syntaxErrorChecker
        = scriptName.map(BhScriptManager.INSTANCE::getCompiledScript).orElse(null);
    if (syntaxErrorChecker == null) {
      return false;
    }
    Object hasError = null;
    ScriptableObject scriptScope = getEventDispatcher().newDefaultScriptScope();
    try {
      hasError = ContextFactory.getGlobal().call(cx -> syntaxErrorChecker.exec(cx, scriptScope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          Util.INSTANCE.getCurrentMethodName() + " - " + scriptName.get() + "\n" + e + "\n");
    }

    if (hasError instanceof Boolean) {
      return (Boolean) hasError;
    }
    throw new AssertionError(scriptName.get() + " must return a boolean value.");
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
