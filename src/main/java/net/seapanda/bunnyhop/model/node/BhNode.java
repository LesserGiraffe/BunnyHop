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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory.MvcType;
import net.seapanda.bunnyhop.model.node.derivative.DerivativeReplacer;
import net.seapanda.bunnyhop.model.node.event.CauseOfDeletion;
import net.seapanda.bunnyhop.model.node.event.MouseEventInfo;
import net.seapanda.bunnyhop.model.node.event.NodeEventInvoker;
import net.seapanda.bunnyhop.model.node.parameter.BhNodeId;
import net.seapanda.bunnyhop.model.node.parameter.BhNodeParameters;
import net.seapanda.bunnyhop.model.node.parameter.BhNodeVersion;
import net.seapanda.bunnyhop.model.node.parameter.BhNodeViewStyleId;
import net.seapanda.bunnyhop.model.node.parameter.DerivationId;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.model.workspace.Workspace;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.TetraConsumer;
import net.seapanda.bunnyhop.view.proxy.BhNodeViewProxy;
import org.apache.commons.lang3.function.TriConsumer;

/**
 * ノードの基底クラス.
 *
 * @author K.Koike
 */
public abstract class BhNode extends SyntaxSymbol {

  /** ワークスペースに存在しているノードとそのノードの {@link InstanceId} を value と key に持つマップ. */
  private static final Map<InstanceId, BhNode> instIdToNodeInWs = new ConcurrentHashMap<>();

  private final BhNodeParameters params;
  /** このノードを繋いでいるコネクタ. */
  protected Connector parentConnector;
  /** このノードがある WorkSpace. */
  protected Workspace workspace;
  /** デフォルトノード (= コネクタからノードを取り外したときに, 代わりに繋がるノード) フラグ. */
  private boolean isDefault = false;
  /** このノードが選択されているかどうかのフラグ. */
  private boolean isSelected = false;
  /** このノードがコンパイルエラーを起こしているかどうかのフラグ. */
  private boolean hasCompileError = false;
  /** このノードに登録されたイベントハンドラを呼び出すオブジェクト. */
  private final transient EventInvoker eventInvoker = this.new EventInvoker();
  /** 最後にこのノードと入れ替わったノード. */
  private transient BhNode lastReplaced;
  /** 派生ノードの入れ替えを行うオブジェクト. */
  private final transient DerivativeReplacer dervReplacer;
  /** ノードの生成に関連する処理を行うオブジェクト. */
  protected final transient BhNodeFactory factory;
  /** イベントハンドラを呼び出すオブジェクト. */
  protected final transient NodeEventInvoker nodeEventInvoker;

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

  /** このオブジェクトに対応するビューの処理を行うプロキシオブジェクトを設定する. */
  public abstract void setViewProxy(BhNodeViewProxy viewProxy);

  /**
   * このノード以下のノードツリーのコピーを作成する.
   *
   * @param fnIsNodeToBeCopied このノード以下のノードがコピーの対象かどうかを判別する関数.
   * @param userOpe undo 用コマンドオブジェクト
   * @return このノード以下のノードツリーのコピー. このノードがコピーの対象にならなかった場合 null.
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
   * このノードに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このノードに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  public abstract EventManager getEventManager();
  

  /** コンストラクタ. */
  protected BhNode(
      BhNodeParameters params,
      BhNodeFactory factory,
      DerivativeReplacer replacer,
      NodeEventInvoker invoker) {
    super(params.name());
    this.params = params;
    this.factory = factory;
    this.dervReplacer = replacer;
    this.nodeEventInvoker = invoker;
  }

  /**
   * コピーコンストラクタ.
   *
   * @param org コピー元オブジェクト
   */
  protected BhNode(BhNode org) {
    super(org);
    params = org.params;
    dervReplacer = org.dervReplacer;
    factory = org.factory;
    nodeEventInvoker = org.nodeEventInvoker;
    isDefault = org.isDefault;
    parentConnector = null;
    workspace = null;
    lastReplaced = null;
  }

  /** このノードの ID を取得する. */
  public BhNodeId getId() {
    return params.nodeId();
  }

  /** このノードのスタイル ID を取得する. */
  public BhNodeViewStyleId getStyleId() {
    return params.styleId();
  }

  /** このノードのバージョンを取得する. */
  public BhNodeVersion getVersion() {
    return params.version();
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
    swappedList.addAll(dervReplacer.replace(newNode, this, userOpe));
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
    BhNode newNode = factory.create(parentConnector.getDefaultNodeId(), userOpe);
    newNode.setDefault(true);
    return replace(newNode, userOpe);
  }

  /** {@code node} が対応するノードビューを持っていなかった場合, 作成する. */
  private void createMvcIfNotHaveView(BhNode node) {
    if (workspace != null && !node.getViewProxy().hasView()) {
      var type = getViewProxy().isTemplateNode() ? MvcType.TEMPLATE : MvcType.DEFAULT;
      factory.setMvc(node, type);
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
   * このノードにコンパニオンノードがあるかどうか調べる.
   *
   * @return このノードにコンパニオンノードがある場合 true
   */
  public boolean hasCompanionNodes() {
    return params.hasCompanionNodes();
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
    getEventManager().invokeOnWorkspaceChanged(oldWs, userOpe);
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
      getEventManager().invokeOnSelectionStateChanged(userOpe);
      userOpe.pushCmdOfSelectNode(this);
    }
  }

  /** このノードを非選択状態にする. */
  public void deselect(UserOperation userOpe) {
    if (isSelected) {
      isSelected = false;
      getEventManager().invokeOnSelectionStateChanged(userOpe);
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

  /** このノードに登録されたイベントハンドラを呼び出すオブジェクトを返す. */
  public EventInvoker getEventInvoker() {
    return eventInvoker;
  }

  /**
   * このノードのコンパニオンノードを作成する.
   *
   * @param type コンパニオンノードに対して適用する MVC 構造
   * @param userOpe undo 用コマンドオブジェクト
   * @return このノードのコンパニオンノードのリスト. コンパニオンノードを持たない場合, 空のリストを返す.
   */
  public List<BhNode> createCompanionNodes(MvcType type, UserOperation userOpe) {
    return nodeEventInvoker.onCompanionNodesCreating(this, type, userOpe);
  }

  /** このノードがコンパイルエラーを起こしているかどうかの状態を変更する. */
  public void setCompileErrState(boolean val, UserOperation userOpe) {
    if (val == hasCompileError) {
      return;
    }
    userOpe.pushCmdOfSetCompileError(this, hasCompileError);
    hasCompileError = val;
    getEventManager().invokeOnCompileErrStateChanged(userOpe);  
  }

  /** このノードがコンパイルエラーを起こしているかどうかの状態を取得する. */
  public boolean getCompileErrState() {
    return hasCompileError;
  }

  /**
   * ノードにコンパイルエラーがあるかどうか調べる.
   *
   * @return コンパイルエラーがある場合 true.  無い場合 false.
   */
  public boolean hasCompileError() {
    if (isDeleted()) {
      return false;
    }
    return nodeEventInvoker.onCompileErrChecking(this);
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

    /** このノードが選択されたときに呼び出すメソッドのリスト. */
    private transient
        SequencedSet<TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation>>
        onSelectionStateChangedList = new LinkedHashSet<>();
    /** このノードのコンパイルエラー状態が変更されたときに呼び出すメソッドのリスト. */
    private transient
        SequencedSet<TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation>>
        onCompileErrStateChangedList = new LinkedHashSet<>();
    /** このノードが他のノードと入れ替わったとき呼び出すメソッドのリスト. */
    private transient
        SequencedSet<TriConsumer<? super BhNode, ? super BhNode, ? super UserOperation>>
        onNodeReplacedList = new LinkedHashSet<>();
    /** このノードが属するワークスペースが変わったときに呼び出すメソッドのリスト. */
    private transient SequencedSet<
        TetraConsumer<? super BhNode, ? super Workspace, ? super Workspace, ? super UserOperation>>
        onWorkspaceChangedList = new LinkedHashSet<>();

    /**
     * ノードの選択状態が変更されたときのイベントハンドラを追加する.
     *  <pre>
     *  イベントハンドラの第 1 引数: 選択状態に変更のあった {@link BhNode}
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
     * ノードのコンパイルエラー状態が変更されたときのイベントハンドラを追加する.
     *  <pre>
     *  イベントハンドラの第 1 引数: コンパイルエラー状態に変更のあった {@link BhNode}
     *  イベントハンドラの第 2 引数: コンパイルエラー状態.
     *  イベントハンドラの第 3 引数: undo 用コマンドオブジェクト
     *  </pre>
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnCompileErrStateChanged(
        TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation> handler) {
      onCompileErrStateChangedList.addLast(handler);
    }

    /**
     * ノードのコンパイルエラー状態が変更されたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnCompileErrStateChanged(
        TriConsumer<? super BhNode, ? super Boolean, ? super UserOperation> handler) {
      onCompileErrStateChangedList.remove(handler);
    }

    /** ノードのコンパイルエラー状態が変更されたときのイベントハンドラを呼び出す. */
    private void invokeOnCompileErrStateChanged(UserOperation userOpe) {
      onCompileErrStateChangedList.forEach(
          handler -> handler.accept(BhNode.this, hasCompileError, userOpe));
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
          handler -> handler.accept(BhNode.this, oldWs, workspace, userOpe));
    }

    /**
     * このノードが属するワークスペースが変わったときのイベントハンドラを追加する.
     *  <pre>
     *  イベントハンドラの第 1 引数: このノード
     *  イベントハンドラの第 2 引数: 変更前のワークスペース
     *  イベントハンドラの第 3 引数: 変更後のワークスペース
     *  イベントハンドラの第 4 引数: undo 用コマンドオブジェクト
     *  </pre>
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnWorkspaceChanged(
        TetraConsumer<? super BhNode, ? super Workspace, ? super Workspace, ? super UserOperation>
        handler) {
      onWorkspaceChangedList.addLast(handler);
    }

    /**
     * このノードが属するワークスペースが変わったときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnWorkspaceChanged(
        TetraConsumer<? super BhNode, ? super Workspace, ? super Workspace, ? super UserOperation>
        handler) {
      onWorkspaceChangedList.remove(handler);
    }

    /** ノード入れ替え時のイベントハンドラを呼び出す. */
    void invokeOnNodeReplaced(BhNode newNode, UserOperation userOpe) {
      onNodeReplacedList.forEach(handler -> handler.accept(BhNode.this, newNode, userOpe));
    }
  }

  /**
   * このノードのイベントハンドラを呼び出す機能を提供するクラス.
   *
   * <p>
   * このオブジェクトと紐づく {@BhNode} オブジェクトを「ターゲットノード」と呼ぶ.
   * </p>
   */
  public class EventInvoker {

    /**
     * ターゲットノードがワークスペースから子ノードに移ったときの処理を実行する.
     *
     * @param oldReplaced ターゲットノードがつながった位置に, 元々子ノードとしてつながっていたノード
     * @param userOpe undo 用コマンドオブジェクト
     */
    public void onMovedFromWsToChild(BhNode oldReplaced, UserOperation userOpe) {
      nodeEventInvoker.onMovedFromWsToChild(BhNode.this, oldReplaced, userOpe);
    }

    /**
     * ターゲットノードが子ノードからワークスペースに移ったときの処理を実行する.
     *
     * @param oldParent 移る前にターゲットノードが接続されていた親ノード
     * @param oldRoot 移る前にターゲットノードが所属していたノードツリーのルートノード
     * @param newReplaced ワークスペースに移る際, ターゲットノードの替わりにつながったノード
     * @param userOpe undo 用コマンドオブジェクト
     */
    public void onMovedFromChildToWs(
        ConnectiveNode oldParent,
        BhNode oldRoot,
        BhNode newReplaced,
        UserOperation userOpe) {
      nodeEventInvoker.onMovedFromChildToWs(
          BhNode.this, oldParent, oldRoot, newReplaced, userOpe);
    }

    /**
     * ターゲットノードの子ノードが入れ替わったときの処理を実行する.
     *
     * @param oldChild 入れ替わった古いノード
     * @param newChild 入れ替わった新しいノード
     * @param parentCnctr 子が入れ替わったコネクタ
     * @param userOpe undo 用コマンドオブジェクト
     */
    public void onChildReplaced(
        BhNode oldChild,
        BhNode newChild,
        Connector parentCnctr,
        UserOperation userOpe) {
      nodeEventInvoker.onChildReplaced(BhNode.this, oldChild, newChild, parentCnctr, userOpe);
    }

    /**
     * ターゲットノードの削除前に呼ばれる処理を実行する.
     *
     * @param nodesToDelete ターゲットノードと共に削除される予定のノード.
     * @param causeOfDeletion ターゲットノードの削除原因
     * @param userOpe undo 用コマンドオブジェクト
     * @return 削除をキャンセルする場合 false. 続行する場合 true.
     */
    public boolean onDeletionRequested(
        Collection<? extends BhNode> nodesToDelete,
        CauseOfDeletion causeOfDeletion,
        UserOperation userOpe) {
      return nodeEventInvoker.onDeletionRequested(
          BhNode.this, nodesToDelete, causeOfDeletion, userOpe);
    }

    /**
     * ユーザー操作により, ターゲットノードがカット & ペーストされる直前に呼ばれる処理を実行する.
     *
     * @param nodesToCut ターゲットノードとともにカットされる予定のノード
     * @param userOpe undo 用コマンドオブジェクト
     * @return カットをキャンセルする場合 false.  続行する場合 true.
     */
    public boolean onCutRequested(
        Collection<? extends BhNode> nodesToCut, UserOperation userOpe) {
      return nodeEventInvoker.onCutRequested(BhNode.this, nodesToCut, userOpe);
    }

    /**
     * ユーザー操作により, ターゲットノードがコピー & ペーストされる直前に呼ばれる処理を実行する.
     *
     * @param nodesToCopy ターゲットノードとともにコピーされる予定のノード
     * @param userOpe undo 用コマンドオブジェクト
     * @return {@link BhNode} を引数にとり, コピーするかどうかの boolean 値を返す関数.
     */
    public Predicate<? super BhNode> onCopyRequested(
        Collection<? extends BhNode> nodesToCopy, UserOperation userOpe) {
      return nodeEventInvoker.onCopyRequested(BhNode.this, nodesToCopy, userOpe);
    }
  
    /**
     * ターゲットノードがテンプレートノードとして作成されたときの処理を実行する.
     *
     * @param userOpe undo 用コマンドオブジェクト
     */
    public void onCreatedAsTemplate(UserOperation userOpe) {
      nodeEventInvoker.onCreatedAsTemplate(BhNode.this, userOpe);
    }

    /**
     * ターゲットノードのドラッグが始まったときの処理を実行する.
     *
     * @param eventInfo ドラッグ操作に関連するマウスイベントを格納したオブジェクト
     * @param userOpe undo 用コマンドオブジェクト
     */
    public void onDragStarted(MouseEventInfo eventInfo, UserOperation userOpe) {
      nodeEventInvoker.onDragStarted(BhNode.this, eventInfo, userOpe);
    }
  }
}
