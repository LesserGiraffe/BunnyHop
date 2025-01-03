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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javafx.application.Platform;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeAttributes;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeVersion;
import net.seapanda.bunnyhop.model.node.attribute.DerivationId;
import net.seapanda.bunnyhop.model.node.event.BhNodeEvent;
import net.seapanda.bunnyhop.model.node.event.BhNodeEventAgent;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.model.traverse.DerivativeReplacer;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.view.proxy.BhNodeViewProxy;
import org.apache.commons.lang3.function.TriConsumer;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

/**
 * ノードの基底クラス.
 *
 * @author K.Koike
 */
public abstract class BhNode extends SyntaxSymbol {

  /** ワークスペースに存在しているノードとそのノードの {@link InstanceId} を value と key に持つマップ. */
  private static final Map<InstanceId, BhNode> instIdToNodeInWs = new ConcurrentHashMap<>();

  /** ノード ID. */
  private final BhNodeId bhId;
  /** ノードのバージョン. */
  private final BhNodeVersion version;
  /** このノードを繋いでいるコネクタ. */
  protected Connector parentConnector;
  /** このノードがある WorkSpace. */
  protected Workspace workspace;
  /** デフォルトノード (= コネクタからノードを取り外したときに, 代わりに繋がるノード) フラグ. */
  private boolean isDefault = false;
  private Map<BhNodeEvent, String> eventToScriptName = new HashMap<>();
  private BhNodeEventAgent eventAgent = new BhNodeEventAgent(this);
  /** 最後にこのノードと入れ替わったノード. */
  private transient BhNode lastReplaced;
  /** このノードが選択されたときに呼び出すメソッドと呼び出しスレッドのフラグ. */
  private transient
      Map<TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation>, Boolean>
      onSelectionStateChangedToThreadFlag = new HashMap<>();
  /** このノードが選択されているかどうかのフラグ. */
  private boolean isSelected = false;


  /** BhNode がとり得る状態. */
  public enum State {
    /** ワークスペース直下のルートノード. */
    ROOT_ON_WS,
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
   * このノードが派生ノードだった場合, そのオリジナルノードを返す.
   * 派生ノードで無かった場合 null を返す.
   *
   * @return このノードのオリジナルノード. このノードが派生ノードで無い場合は null を返す.
   */
  public abstract BhNode getOriginal();

  /**
   * 最後にこのノードのオリジナルノードとなったノードを返す.
   *
   * @return 最後にこのノードのオリジナルノードとなったノード.
   *         このノードが一度も派生ノードになったことがない場合 null.
   */
  public abstract BhNode getLastOriginal();

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

  /** このオブジェクトに対応するビューの処理を行うプロキシオブジェクトを取得する. */
  public abstract BhNodeViewProxy getViewProxy();

  /**
   * このノード以下のノードツリーのコピーを作成する.
   *
   * @param fnIsNodeToBeCopied このノードの子ノードがコピーの対象かどうかを判別する関数.
   *                         copy を呼んだノードは判定対象にならず, 必ずコピーされる.
   * @param userOpe undo 用コマンドオブジェクト
   * @return このノード以下のノードツリーのコピー
   */
  public abstract BhNode copy(Predicate<? super BhNode> fnIsNodeToBeCopied, UserOperation userOpe);

  /**
   * このノード以下のノードツリーのコピーを作成する.
   *
   * @param userOpe undo 用コマンドオブジェクト
   * @return このノード以下のノードツリーのコピー
   */
  public BhNode copy(UserOperation userOpe) {
    return copy(node -> true, userOpe);
  }

  /**
   * コンストラクタ.
   *
   * @param type xml のtype属性
   * @param attrbute ノードの設定情報
   */
  protected BhNode(BhNodeAttributes attributes) {
    super(attributes.name());
    this.bhId = attributes.bhNodeId();
    this.version = attributes.version();
    registerScriptName(BhNodeEvent.ON_MOVED_FROM_CHILD_TO_WS, attributes.onMovedFromChildToWs());
    registerScriptName(BhNodeEvent.ON_MOVED_TO_CHILD, attributes.onMovedToChild());
    registerScriptName(BhNodeEvent.ON_DELETION_REQUESTED, attributes.onDeletionRequested());
    registerScriptName(BhNodeEvent.ON_CUT_REQUESTED, attributes.onCutRequested());
    registerScriptName(BhNodeEvent.ON_COPY_REQUESTED, attributes.onCopyRequested());
    registerScriptName(
        BhNodeEvent.ON_PRIVATE_TEMPLATE_CREATING, attributes.onPrivateTemplateCreating());
    registerScriptName(BhNodeEvent.ON_SYNTAX_CHECKING, attributes.onCompileErrorChecking());
    registerScriptName(BhNodeEvent.ON_TEMPLATE_CREATED, attributes.onTemplateCreated());
    registerScriptName(BhNodeEvent.ON_DRAG_STARTED, attributes.onDragStarted());
  }

  /**
   * コピーコンストラクタ.
   *
   * @param org コピー元オブジェクト
   */
  protected BhNode(BhNode org) {
    super(org);
    bhId = org.bhId;
    version = org.version;
    isDefault = org.isDefault;
    parentConnector = null;
    workspace = null;
    eventToScriptName = new HashMap<>(org.eventToScriptName);
    lastReplaced = null;
  }

  /** このノードの ID を取得する. */
  public BhNodeId getId() {
    return bhId;
  }

  /** このノードのバージョンを取得する. */
  public BhNodeVersion getVersion() {
    return version;
  }

  /**
   * {@code newBhNode} とこのノードを入れ替える.
   *
   * @param newNode このノードと入れ替えるノード.
   * @param userOpe undo 用コマンドオブジェクト.
   * @return この入れ替え操作で入れ替わったノード一式.
   *         0 番目が {@code newNode} とこのノードのペアであることが保証される.
   */
  public List<Swapped> replace(BhNode newNode, UserOperation userOpe) {
    if (parentConnector == null) {
      return new ArrayList<>();
    }
    var swappedList = new ArrayList<Swapped>();
    swappedList.add(new Swapped(this, newNode));
    setLastReplaced(newNode, userOpe);
    parentConnector.connectNode(newNode, userOpe);
    swappedList.addAll(DerivativeReplacer.replace(newNode, this, userOpe));
    return swappedList;
  }

  /**
   * このノードをモデルツリーから取り除く.
   *
   * @param userOpe undo 用コマンドオブジェクト
   * @return この入れ替え操作で入れ替わったノード一式.
   *         0 番目がこのノードとこのノードに代わり新しく作成されたノードのペアであることが保証される.
   */
  public List<Swapped> remove(UserOperation userOpe) {
    if (parentConnector == null) {
      return new ArrayList<>();
    }
    BhNode newNode =
        BhService.bhNodeFactory().create(parentConnector.getDefaultNodeId(), userOpe);
    newNode.setDefault(true);
    return replace(newNode, userOpe);
  }

  /**
   * ノードの状態を取得する.
   *
   * @retval State.DELETED 削除済みノード. 子ノードでも削除されていればこの値が返る.
   * @retval State.ROOT_DANGLING ルートノードであるが, ワークスペースにルートノードとして登録されていない
   * @retval State.ROOT_ON_WS ルートノードでかつ, ワークスペースにルートノードとして登録されている
   * @retval State.CHILD 子ノード. ワークスペースに属していてもいなくても子ノードならばこの値が返る
   */
  public State getState() {
    if (workspace == null) {
      return State.DELETED;
    } else if (parentConnector == null) {
      if (workspace.containsAsRoot(this)) {
        return State.ROOT_ON_WS;
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
    return isRemovable() || (getState() == BhNode.State.ROOT_ON_WS);
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
  public boolean isRootOnWs() {
    return getState() == BhNode.State.ROOT_ON_WS;
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
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
    if (workspace == null) {
      instIdToNodeInWs.remove(getInstanceId());
    } else {
      instIdToNodeInWs.put(getInstanceId(), this);
    }
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
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void setLastReplaced(BhNode lastReplaced, UserOperation userOpe) {
    userOpe.pushCmdOfSetLastReplaced(this.lastReplaced, this);
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
    return isSelected;
  }

  /** このノードを選択状態にする. */
  public void select(UserOperation userOpe) {
    if (!isSelected) {
      isSelected = true;
      getViewProxy().notifyNodeSelected();
      onSelectionStateChangedToThreadFlag.forEach(
          (handler, threadFlag) -> invokeOnSelectionStateChanged(handler, threadFlag, userOpe));
      userOpe.pushCmdOfSelectNode(this);
    }
  }

  /** このノードを非選択状態にする. */
  public void deselect(UserOperation userOpe) {
    if (isSelected) {
      isSelected = false;
      getViewProxy().notifyNodeDeselected();
      onSelectionStateChangedToThreadFlag.forEach(
          (handler, threadFlag) -> invokeOnSelectionStateChanged(handler, threadFlag, userOpe));
      userOpe.pushCmdOfDeselectNode(this);
    }
  }

  /** 選択変更時のイベントハンドラを呼び出す. */
  private void invokeOnSelectionStateChanged(
        TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation> handler,
        boolean invokeOnUiThread,
        UserOperation userOpe) {
    if (invokeOnUiThread && !Platform.isFxApplicationThread()) {
      Platform.runLater(() -> handler.accept(this, isSelected, userOpe));
    } else {
      handler.accept(this, isSelected, userOpe);
    }
  }

  /**
   * ノードの選択状態が変更されたときのイベントハンドラを追加する.
   *  <pre>
   *  イベントハンドラの第 1 引数: 選択状態に変換のあった {@link BhNode}
   *  イベントハンドラの第 2 引数: 選択状態. 選択されたなら true.
   *  </pre>
   *
   * @param handler 追加するイベントハンドラ
   * @param invokeOnUiThread UIスレッド上で呼び出す場合 true
   */
  public void addOnSelectionStateChanged(
      TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation> handler,
      boolean invokeOnUiThread) {
    onSelectionStateChangedToThreadFlag.put(handler, invokeOnUiThread);
  }

  /**
   * ノードの選択状態が変更されたときのイベントハンドラを削除する.
   *
   * @param handler 削除するイベントハンドラ
   */
  public void removeOnSelectionStateChanged(
      TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation> handler) {
    onSelectionStateChangedToThreadFlag.remove(handler);
  }

  /**
   * このノードがデフォルトノードかどうか調べる.
   *
   * @return このノードがデフォルトノードの場合 true
   */
  public boolean isDefault() {
    return isDefault;
  }

  /** このノードのデフォルトフラグを変更する. */
  public void setDefault(boolean isDefault) {
    this.isDefault = isDefault;
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

  /**
   * このノードの先祖コネクタの中に, DerivationId.NONE 以外の派生先 ID (派生ノードを特定するための ID) があればそれを返す.
   * なければ, DerivationId.NONE を返す.
   */
  public DerivationId findDerivationIdUp() {
    if (getParentConnector() == null) {
      return null;
    }
    return getParentConnector().findDerivationIdUp();
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
      if (symbolNameMatches(symbolName)) {
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
  public BhNodeEventAgent getEventAgent() {
    return eventAgent;
  }

  /**
   * このノード固有のテンプレートノードを作成する.
   *
   * <p> 返されるノードの MVC は構築されていない. </p>
   *
   * @param userOpe undo 用コマンドオブジェクト
   * @return このノード固有のテンプレートノードのリスト. 固有のテンプレートノードを持たない場合, 空のリストを返す.
   */
  public Collection<BhNode> genPrivateTemplateNodes(UserOperation userOpe) {
    Optional<String> scriptName = getScriptName(BhNodeEvent.ON_PRIVATE_TEMPLATE_CREATING);
    Script privateTemplateCreator =
        scriptName.map(BhService.bhScriptManager()::getCompiledScript).orElse(null);
    if (privateTemplateCreator == null) {
      return new ArrayList<>();
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_USER_OPE, userOpe);
      }};
    Context cx = Context.enter();
    ScriptableObject scriptScope = getEventAgent().createScriptScope(cx, nameToObj);
    try {
      return ((Collection<?>) privateTemplateCreator.exec(cx, scriptScope)).stream()
          .filter(elem -> elem instanceof BhNode)
          .map(elem -> (BhNode) elem)
          .collect(Collectors.toCollection(ArrayList::new));
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(
          "'%s' must return a collection of BhNode(s).\n%s".formatted(scriptName.get(), e));
    } finally {
      Context.exit();
    }
    return new ArrayList<>();
  }

  /**
   * このノードに文法エラーがあるかどうか調べる.
   *
   * @return 文法エラーがある場合 true.  無い場合 false.
   */
  public boolean hasCompileError() {
    if (isDeleted()) {
      return false;
    }
    Optional<String> scriptName = getScriptName(BhNodeEvent.ON_SYNTAX_CHECKING);
    Script errorChecker =
        scriptName.map(BhService.bhScriptManager()::getCompiledScript).orElse(null);
    if (errorChecker == null) {
      return false;
    }
    Context cx = Context.enter();
    ScriptableObject scope = getEventAgent().createScriptScope(cx);
    try {
      return (Boolean) errorChecker.exec(cx, scope);
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(
          "'%s' must return a boolean value.\n%s".formatted(scriptName.get(), e));
    } finally {
      Context.exit();
    }
    return false;
  }

  /**
   * {@link InstanceId} が {@code id} である {@link BhNode} がワークスペース上にあれば, それを返す.
   *
   * @param id この {@link InstanceId} を持つ {@link BhNode} をワークスペースから探す.
   * @return {@link InstanceId} が {@code id} でかつワークスペース上にある {@link BhNode}.
   *         存在しない場合は empty.
   */
  public static final Optional<BhNode> getBhNodeOf(InstanceId id) {
    return Optional.ofNullable(instIdToNodeInWs.get(id));
  }

  /**
   * ノードの入れ替えの結果変化のあったノード一式.
   *
   * @param oldNode 入れ替え前に子ノードであったノード
   * @param newNode 入れ替え後に子ノードとなったノード
   */
  public record Swapped(BhNode oldNode, BhNode newNode) {}
}
