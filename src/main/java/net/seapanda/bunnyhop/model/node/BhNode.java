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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeAttributes;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeVersion;
import net.seapanda.bunnyhop.model.node.attribute.DerivationId;
import net.seapanda.bunnyhop.model.node.hook.HookAgent;
import net.seapanda.bunnyhop.model.node.hook.HookEvent;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.model.traverse.DerivativeReplacer;
import net.seapanda.bunnyhop.model.traverse.NodeMvcBuilder;
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
  /** このノードが選択されているかどうかのフラグ. */
  private boolean isSelected = false;
  /** フックイベント名とフック処理を記述したスクリプト名のマップ. */
  private Map<HookEvent, String> eventToScriptName = new HashMap<>();
  /** このノードに登録されたフック処理を実行するオブジェクト. */
  private transient HookAgent hookAgent = new HookAgent(this);
  /** このノードに登録されたイベントハンドラを管理するオブジェクト. */
  private transient EventManager eventManager = new EventManager();
  /** 最後にこのノードと入れ替わったノード. */
  private transient BhNode lastReplaced;

  /** BhNode がとり得る状態. */
  public enum State {
    /** ワークスペース直下のルートノード. */
    ROOT,
    /** 子ノード (ルートがダングリング状態かどうかは問わない). */
    CHILD,
    /** 削除済み. */
    DELETED,
  }

  /**
   * 子ノードでかつ取り外しができる場合 true を返す.
   * ルートノードの場合 false が返る.
   *
   * @return 子ノードでかつ取り外しができる場合 true
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
    registerScriptName(HookEvent.ON_MOVED_FROM_CHILD_TO_WS, attributes.onMovedFromChildToWs());
    registerScriptName(HookEvent.ON_MOVED_TO_CHILD, attributes.onMovedToChild());
    registerScriptName(HookEvent.ON_DELETION_REQUESTED, attributes.onDeletionRequested());
    registerScriptName(HookEvent.ON_CUT_REQUESTED, attributes.onCutRequested());
    registerScriptName(HookEvent.ON_COPY_REQUESTED, attributes.onCopyRequested());
    registerScriptName(
        HookEvent.ON_PRIVATE_TEMPLATE_CREATING, attributes.onPrivateTemplateCreating());
    registerScriptName(HookEvent.ON_SYNTAX_CHECKING, attributes.onCompileErrorChecking());
    registerScriptName(HookEvent.ON_TEMPLATE_CREATED, attributes.onTemplateCreated());
    registerScriptName(HookEvent.ON_DRAG_STARTED, attributes.onDragStarted());
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
   * このノードがルートノードであった場合は何もしない.
   *
   * @param newNode このノードと入れ替えるノード.
   * @param userOpe undo 用コマンドオブジェクト.
   * @return この操作で入れ替わった子ノードのペアのリスト.
   *         <pre>
   *         このノードが子ノードであった場合
   *             最初の要素 : このノードと {@code newNode} のペア
   *             残りの要素 : この操作によって入れ替わった'子'派生ノードとそれと入れ替わったノードのペア.
   * 
   *         このノードがルートノードであった場合
   *             空のセット
   *         </pre>
   */
  public SequencedSet<Swapped> replace(BhNode newNode, UserOperation userOpe) {
    if (parentConnector == null) {
      return new LinkedHashSet<>();
    }
    createMvcIfNotHaveView(newNode);
    var swappedList = new LinkedHashSet<Swapped>();
    swappedList.addFirst(new Swapped(this, newNode));
    if (newNode.isChild()) {
      SequencedSet<Swapped> tmp = newNode.remove(userOpe);
      // newNode を子ノードから切り離した際の入れ替えは, 戻り値のセットに含めない.
      tmp.removeFirst();
      swappedList.addAll(tmp);
    }
    setLastReplaced(newNode, userOpe);
    parentConnector.connectNode(newNode, userOpe);
    swappedList.addAll(DerivativeReplacer.replace(newNode, this, userOpe));
    return swappedList;
  }

  /**
   * このノードをモデルツリーから取り除く.
   * このノードがルートノードであった場合は何もしない.
   *
   * @param userOpe undo 用コマンドオブジェクト
   * @return この操作で入れ替わった子ノードのペアのリスト.
   *         <pre>
   *         このノードが子ノードであった場合
   *             最初の要素 : このノードとこれと入れ替わったデフォルトノードのペア
   *             残りの要素 : この操作によって入れ替わった'子'派生ノードとそれと入れ替わったノードのペア.
   * 
   *         このノードがルートノードであった場合
   *             空のセット
   *         </pre>
   */
  public SequencedSet<Swapped> remove(UserOperation userOpe) {
    if (parentConnector == null) {
      return new LinkedHashSet<>();
    }
    BhNode newNode =
        BhService.bhNodeFactory().create(parentConnector.getDefaultNodeId(), userOpe);
    newNode.setDefault(true);
    return replace(newNode, userOpe);
  }

  /** {@code node} が対応するノードビューを持っていなかった場合, 作成する. */
  private void createMvcIfNotHaveView(BhNode node) {
    if (workspace != null && !node.getViewProxy().hasView()) {
      if (getViewProxy().isTemplateNode()) {
        NodeMvcBuilder.buildTemplate(node);
      } else {
        NodeMvcBuilder.build(node);
      }
    }
  }

  /**
   * ノードの状態を取得する.
   *
   * @retval State.DELETED 削除済みノード.
   * @retval State.ROOT ルートノード.
   * @retval State.CHILD 子ノード.
   */
  public State getState() {
    if (workspace == null) {
      return State.DELETED;
    } else if (parentConnector == null) {
      return State.ROOT;
    } else {
      return State.CHILD;
    }
  }

  /**
   * 移動可能なノードであるかどうかを調べる.
   *
   * @return 移動可能なノードである場合 true
   */
  public boolean isMovable() {
    return isRemovable() || (getState() == BhNode.State.ROOT);
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
  public boolean isRoot() {
    return getState() == BhNode.State.ROOT;
  }

  /**
   * このノード固有のテンプレートノードがあるかどうか調べる.
   *
   * @return このノード固有のテンプレートノードがある場合 true
   */
  public boolean hasPrivateTemplateNodes() {
    return getScriptName(HookEvent.ON_PRIVATE_TEMPLATE_CREATING).isPresent();
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
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void setWorkspace(Workspace workspace, UserOperation userOpe) {
    if (this.workspace == workspace) {
      return;
    }
    Workspace oldWs = this.workspace;
    this.workspace = workspace;
    if (workspace == null) {
      instIdToNodeInWs.remove(getInstanceId());
    } else {
      instIdToNodeInWs.put(getInstanceId(), this);
    }
    eventManager.invokeOnWorkspaceChanged(oldWs, userOpe);
    // undo 時に Workspace.addNode の逆操作とこのコマンドの逆操作が重複するが問題ない.
    // このメソッドの呼ばれ方によらず, このメソッドの逆操作をするために, ここで操作コマンドを追加する.
    userOpe.pushCmdOfSetWorkspace(oldWs, this);
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
      eventManager.invokeOnSelectionStateChanged(userOpe);
      userOpe.pushCmdOfSelectNode(this);
    }
  }

  /** このノードを非選択状態にする. */
  public void deselect(UserOperation userOpe) {
    if (isSelected) {
      isSelected = false;
      eventManager.invokeOnSelectionStateChanged(userOpe);
      userOpe.pushCmdOfDeselectNode(this);
    }
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
  public Optional<String> getScriptName(HookEvent event) {
    return Optional.ofNullable(eventToScriptName.get(event));
  }

  /**
   * このノードに関連するスクリプト名をイベントとともに登録する.
   *
   * @param event スクリプトに対応するイベント
   * @param scriptName 登録するスクリプト名.  null もしくは空文字の場合は登録しない.
   */
  protected void registerScriptName(HookEvent event, String scriptName) {
    if (scriptName == null || scriptName.isEmpty()) {
      return;
    }
    eventToScriptName.put(event, scriptName);
  }

  /**
   * このノードに登録されたフック処理を実行するオブジェクトを返す.
   *
   * @return このノードに登録されたフック処理を実行するオブジェクト
   */
  public HookAgent getHookAgent() {
    return hookAgent;
  }

  /**
   * このノードに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このノードに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  public EventManager getEventManager() {
    return eventManager;
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
    Optional<String> scriptName = getScriptName(HookEvent.ON_PRIVATE_TEMPLATE_CREATING);
    Script privateTemplateCreator =
        scriptName.map(BhService.bhScriptManager()::getCompiledScript).orElse(null);
    if (privateTemplateCreator == null) {
      return new ArrayList<>();
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_USER_OPE, userOpe);
      }};
    Context cx = Context.enter();
    ScriptableObject scriptScope = getHookAgent().createScriptScope(cx, nameToObj);
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
    Optional<String> scriptName = getScriptName(HookEvent.ON_SYNTAX_CHECKING);
    Script errorChecker =
        scriptName.map(BhService.bhScriptManager()::getCompiledScript).orElse(null);
    if (errorChecker == null) {
      return false;
    }
    Context cx = Context.enter();
    ScriptableObject scope = getHookAgent().createScriptScope(cx);
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

  /** イベントハンドラの管理を行うクラス. */
  public class EventManager {

    /** このノードが選択されたときに呼び出すメソッドと呼び出しスレッドのフラグ. */
    private transient
        SequencedSet<TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation>>
        onSelectionStateChangedList = new LinkedHashSet<>();
    /** このノードが他のノードと入れ替わったとき呼び出すメソッドと呼び出しスレッドのフラグ. */
    private transient
        SequencedSet<TriConsumer<? super BhNode, ? super BhNode, ? super UserOperation>>
        onNodeReplacedList = new LinkedHashSet<>();
    /** このノードが属するワークスペースが変わったときに呼び出すメソッドと呼び出しスレッドのフラグ. */
    private transient
        SequencedSet<TriConsumer<? super Workspace, ? super Workspace, ? super UserOperation>>
        onWorkspaceChangedList = new LinkedHashSet<>();

    /**
     * ノードの選択状態が変更されたときのイベントハンドラを追加する.
     *  <pre>
     *  イベントハンドラの第 1 引数: 選択状態に変換のあった {@link BhNode}
     *  イベントハンドラの第 2 引数: 選択状態. 選択されたなら true.
     *  イベントハンドラの第 3 引数: undo 用コマンドオブジェクト
     *  </pre>
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnSelectionStateChanged(
        TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation> handler) {
      onSelectionStateChangedList.addLast(handler);
    }

    /**
     * ノードの選択状態が変更されたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnSelectionStateChanged(
        TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation> handler) {
      onSelectionStateChangedList.remove(handler);
    }

    /** 選択変更時のイベントハンドラを呼び出す. */
    private void invokeOnSelectionStateChanged(UserOperation userOpe) {
      onSelectionStateChangedList.forEach(
          handler -> handler.accept(BhNode.this, isSelected, userOpe));
    }

    /**
     * ノードが入れ替わったときのイベントハンドラを追加する.
     *  <pre>
     *  イベントハンドラの第 1 引数: このノード
     *  イベントハンドラの第 2 引数: このノードの代わりに, このノードがあった位置に接続されたノード
     *  イベントハンドラの第 3 引数: undo 用コマンドオブジェクト
     *  </pre>
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnNodeReplaced(
        TriConsumer<? super BhNode, ? super BhNode, ? super UserOperation> handler) {
      onNodeReplacedList.addLast(handler);
    }

    /**
     * ノードが入れ替わったときのイベントハンドラを削除する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void removeOnNodeReplaced(
        TriConsumer<? super BhNode, ? super BhNode, ? super UserOperation> handler) {
      onNodeReplacedList.remove(handler);
    }

    /** ワークスペース変更時のイベントハンドラを呼び出す. */
    private void invokeOnWorkspaceChanged(Workspace oldWs, UserOperation userOpe) {
      onWorkspaceChangedList.forEach(
          handler -> handler.accept(oldWs, workspace, userOpe));
    }

    /**
     * このノードが属するワークスペースが変わったときのイベントハンドラを追加する.
     *  <pre>
     *  イベントハンドラの第 1 引数: 変更前のワークスペース
     *  イベントハンドラの第 2 引数: 変更後のワークスペース
     *  イベントハンドラの第 3 引数: undo 用コマンドオブジェクト
     *  </pre>
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnWorkspaceChanged(
        TriConsumer<? super Workspace, ? super Workspace, ? super UserOperation> handler) {
      onWorkspaceChangedList.addLast(handler);
    }

    /**
     * このノードが属するワークスペースが変わったときのイベントハンドラを削除する.
     *
     * @param handler 追加するイベントハンドラ
     */
    public void removeOnWorkspaceChanged(
        TriConsumer<? super Workspace, ? super Workspace, ? super UserOperation> handler) {
      onWorkspaceChangedList.remove(handler);
    }

    /** ノード入れ替え時のイベントハンドラを呼び出す. */
    void invokeOnNodeReplaced(BhNode newNode, UserOperation userOpe) {
      onNodeReplacedList.forEach(handler -> handler.accept(BhNode.this, newNode, userOpe));
    }
  }
}
