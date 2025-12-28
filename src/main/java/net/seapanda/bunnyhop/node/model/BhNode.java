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

package net.seapanda.bunnyhop.node.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.SequencedSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.seapanda.bunnyhop.node.model.derivative.DerivativeReplacer;
import net.seapanda.bunnyhop.node.model.event.CauseOfDeletion;
import net.seapanda.bunnyhop.node.model.event.NodeEventInvoker;
import net.seapanda.bunnyhop.node.model.event.UiEvent;
import net.seapanda.bunnyhop.node.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.node.model.factory.BhNodeFactory.MvcType;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeId;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeParameters;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeVersion;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeViewStyleId;
import net.seapanda.bunnyhop.node.model.parameter.BreakpointSetting;
import net.seapanda.bunnyhop.node.model.parameter.DerivationId;
import net.seapanda.bunnyhop.node.model.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.SimpleConsumerInvoker;
import net.seapanda.bunnyhop.workspace.model.Workspace;

/**
 * ノードの基底クラス.
 *
 * @author K.Koike
 */
public abstract class BhNode extends SyntaxSymbol {

  private final BhNodeParameters params;
  /** このノードを繋いでいるコネクタ. */
  protected Connector parentConnector;
  /** このノードがある WorkSpace. */
  protected Workspace workspace;
  /** デフォルトノード (= コネクタからノードを取り外したときに, 代わりに繋がるノード) フラグ. */
  private boolean isDefault = false;
  /** このノードが選択されているかどうかのフラグ. */
  private boolean isSelected = false;
  /** このノードがコンパイルエラーを持つ場合のエラーメッセージ一覧. */
  private SequencedCollection<String> compileErrorMessages = new ArrayList<>();
  /** ブレークポイントが設定されているかどうかのフラグ. */
  private boolean isBreakpointSet = false;
  /**
   * このノードが破損しているかどうかのフラグ.
   * ロード時に必要な子ノードが見つからなかったり,
   * 現在のシステムと互換性のないバージョンのノードから復元された場合, 破損したものとして扱う.
   */
  private boolean isCorrupted = false;
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
  /** このノードに対応するビュー. */
  private transient BhNodeView view;

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
  public abstract CallbackRegistry getCallbackRegistry();
  
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

  /**
   * このノードを含むブレークポイントグループのリーダーとなるノードを取得する.
   *
   * <p>ブレークポイントグループとは, ブレークの対象となる処理を構成する複数のノードを含むグループである.
   * 各ブレークポイントグループは, グループのリーダーとなるノード持つ.
   */
  public Optional<BhNode> findBreakpointGroupLeader() {
    return findBreakpointGroupLeader(this);
  }

  /** {@code node} を含むブレークポイントグループのリーダーとなるノードを取得する. */
  private static Optional<BhNode> findBreakpointGroupLeader(BhNode node) {
    if (node == null) {
      return Optional.empty();
    }
    if (node.params.breakpointSetting() == BreakpointSetting.SET) {
      return Optional.of(node);
    }
    if (node.params.breakpointSetting() == BreakpointSetting.IGNORE) {
      return Optional.empty();
    }
    if (node.params.breakpointSetting() == BreakpointSetting.SPECIFY_PARENT) {
      return findBreakpointGroupLeader(node.findParentNode());
    }
    return Optional.empty();
  }

  /**
   * このノードがブレークポイントグループのリーダかどうか調べる.
   *
   * @return このノードがブレークポイントグループのリーダである場合 true.
   */
  public boolean isBreakpointGroupLeader() {
    return params.breakpointSetting() == BreakpointSetting.SET;
  }

  /**
   * このノードのブレークポイントの設定を変更する.
   *
   * @param val このノードにブレークポイントを設定する場合 true.  <br>
   *            このノードにブレークポイントを設定しない場合 false
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void setBreakpoint(boolean val, UserOperation userOpe) {
    if (val != isBreakpointSet) {
      isBreakpointSet = val;
      userOpe.pushCmd(ope -> setBreakpoint(!val, ope));
      getCallbackRegistry().onBreakpointSetInvoker.invoke(
          new BreakpointSetEvent(this, isBreakpointSet, userOpe));
    }
  }

  /**
   * このノードにブレークポイントが設定されているか調べる.
   *
   * @return このノードにブレークポイントが設定されている場合 true.
   */
  public boolean isBreakpointSet() {
    return isBreakpointSet;
  }

  /**
   * このノードが破損しているかどうかを設定する.
   *
   * @param val このノードが破損している場合 true.
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void setCorrupted(boolean val, UserOperation userOpe) {
    if (val != isCorrupted) {
      isCorrupted = val;
      userOpe.pushCmd(ope -> setCorrupted(!val, ope));
      getCallbackRegistry().onCorruptionStateChangedInvoker.invoke(
          new CorruptionStateChangedEvent(this, isCorrupted, userOpe));
    }
  }

  /**
   * このノードが破損しているかどうかを設定する.
   *
   * @param val このノードが破損している場合 true.
   */
  public void setCorrupted(boolean val) {
    setCorrupted(val, new UserOperation());
  }

  /**
   * このノードが破損しているかどうかを調べる.
   *
   * @return このノードが破損している場合 true.
   */
  public boolean isCorrupted() {
    return isCorrupted;
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
    if (workspace != null && node.getView().isEmpty()) {
      getView()
          .map(view -> view.isTemplate() ? MvcType.TEMPLATE : MvcType.DEFAULT)
          .ifPresent(type -> factory.setMvc(node, type));
    }
  }

  /**
   * ノードの状態を取得する.
   *
   * @return State.DELETED 削除済みノード. <br>
   *         State.ROOT ルートノード. <br>
   *         State.CHILD 子ノード.
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
    if (this.parentConnector != null) {
      this.parentConnector.getCallbackRegistry().getOnNodeReplaced()
          .remove(getCallbackRegistry().onNodeReplaced);
    }
    if (parentConnector != null) {
      parentConnector.getCallbackRegistry().getOnNodeReplaced()
          .add(getCallbackRegistry().onNodeReplaced);
    }
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
    // undo 時に Workspace.addNode の逆操作とこのコマンドの逆操作が重複するが問題ない.
    // このメソッドの呼ばれ方によらず, このメソッドの逆操作をするために, ここで操作コマンドを追加する.
    userOpe.pushCmd(ope -> setWorkspace(oldWs, ope));
    getCallbackRegistry().onWsChangedInvoker.invoke(
        new WorkspaceChangeEvent(this, oldWs, workspace, userOpe));
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
    BhNode oldLastReplaced = this.lastReplaced;
    this.lastReplaced = lastReplaced;
    userOpe.pushCmd(ope -> setLastReplaced(oldLastReplaced, ope));
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
      userOpe.pushCmd(this::deselect);
      getCallbackRegistry().onSelStateChangedInvoker.invoke(
          new SelectionEvent(this, true, userOpe));
    }
  }

  /** このノードを非選択状態にする. */
  public void deselect(UserOperation userOpe) {
    if (isSelected) {
      isSelected = false;
      userOpe.pushCmd(this::select);
      getCallbackRegistry().onSelStateChangedInvoker.invoke(
          new SelectionEvent(this, false, userOpe));
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
   * @return このノードが外部ノードの場合 true
   */
  public boolean isOuter() {
    return getView().map(view -> view.getTreeManager().isOuter()).orElse(false);
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
  public SyntaxSymbol findAncestorOf(String symbolName, int generation, boolean toTop) {
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
    return parentConnector.findAncestorOf(symbolName, Math.max(0, generation - 1), toTop);
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

  /** このノードがコンパイルエラーを起こしているかどうかの状態を取得する. */
  public boolean hasCompileErr() {
    return !compileErrorMessages.isEmpty();
  }

  /**
   * このノードにコンパイルエラーがあるかどうの状態を更新する.
   *
   * <p>{@link #hasCompileErr} が返すコンパイルエラーの有無と {@link #getCompileErrorMessages} が返すメッセージが更新される.
   *
   * @return 更新後にコンパイルエラーがある場合 true.  無い場合 false.
   */
  public boolean checkCompileError(UserOperation userOpe) {
    boolean hadCompileError = hasCompileErr();
    if (isDeleted()) {
      compileErrorMessages = new ArrayList<>();
    } else {
      compileErrorMessages = new ArrayList<>(nodeEventInvoker.onCompileErrChecking(this));
    }
    boolean hasCompileError = hasCompileErr();
    // エラーメッセージが変わったかは確認せずに, 更新前か後のどちらか一方でもエラー状態であればイベントハンドラを呼ぶ.
    if (hadCompileError || hasCompileError) {
      getCallbackRegistry().onCompileErrStateUpdatedInvoker.invoke(
          new CompileErrorEvent(this, hasCompileError, userOpe));
    }
    return hasCompileError;
  }

  /**
   * このノードのコンパイルエラーメッセージを返す.
   *
   * @return このノードのコンパイルエラーメッセージ.  このノードにコンパイルエラーがない場合は空のリスト.
   */
  public SequencedCollection<String> getCompileErrorMessages() {
    return new ArrayList<>(compileErrorMessages);
  }

  /**
   * このノードのエイリアスを取得する.
   *
   * @return このノードのエイリアス
   */
  public String getAlias() {
    return nodeEventInvoker.onAliasAsked(this);
  }

  /**
   * このノードにユーザが定義した名前がある場合, それを取得する.
   *
   * @return このノードにつけられたユーザ定義名
   */
  public Optional<String> getUserDefinedName() {
    return nodeEventInvoker.onUserDefinedNameAsked(this);
  }

  /** このノードに対応するビューを設定する. */
  public void setView(BhNodeView view) {
    this.view = view;
  }

  /** このノードに対応するビューを取得する. */
  public Optional<BhNodeView> getView() {
    return Optional.ofNullable(view);
  }

  /**
   * ノードの入れ替えの結果変化のあったノード一式.
   *
   * @param oldNode 入れ替え前に子ノードであったノード
   * @param newNode 入れ替え後に子ノードとなったノード
   */
  public record Swapped(BhNode oldNode, BhNode newNode) {}

  /** {@link BhNode} に対してイベントハンドラを追加または削除する機能を提供するクラス. */
  public class CallbackRegistry {

    /** 関連するノードの選択状態が変更されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<SelectionEvent> onSelStateChangedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するノードのコンパイルエラー状態が変更されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<CompileErrorEvent> onCompileErrStateUpdatedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するノードが {@link Connector} に接続されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<ConnectionEvent> onConnectedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するノードが属するワークスペースが変わったときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<WorkspaceChangeEvent> onWsChangedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するノードのブレークポイントの設定が変わったときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<BreakpointSetEvent> onBreakpointSetInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連するノードの破損の有無が変わったときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<CorruptionStateChangedEvent> onCorruptionStateChangedInvoker =
        new SimpleConsumerInvoker<>();

    /** ノードが入れ替わったときのイベントハンドラ. */
    private final Consumer<? super Connector.ReplacementEvent> onNodeReplaced =
        this::onNodeReplaced;

    /** 関連するノードの選択状態が変更されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<SelectionEvent>.Registry getOnSelectionStateChanged() {
      return onSelStateChangedInvoker.getRegistry();
    }

    /** 関連するノードのコンパイルエラー状態が更新されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<CompileErrorEvent>.Registry getOnCompileErrorStateUpdated() {
      return onCompileErrStateUpdatedInvoker.getRegistry();
    }

    /** 関連するノードが, {@link Connector} に接続されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<ConnectionEvent>.Registry getOnConnected() {
      return onConnectedInvoker.getRegistry();
    }

    /** 関連するノードが属するワークスペースが変更されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<WorkspaceChangeEvent>.Registry getOnWorkspaceChanged() {
      return onWsChangedInvoker.getRegistry();
    }

    /** 関連するノードのブレークポイントの設定が変わったときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<BreakpointSetEvent>.Registry getOnBreakpointSet() {
      return onBreakpointSetInvoker.getRegistry();
    }

    /** 関連するノードの破損の有無が変わったときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<CorruptionStateChangedEvent>.Registry getOnCorruptionStateChanged() {
      return onCorruptionStateChangedInvoker.getRegistry();
    }

    /** コネクタに接続されたノードが入れ替わったときに呼び出されるコールバック関数. */
    private void onNodeReplaced(Connector.ReplacementEvent event) {
      if (event.newNode() == BhNode.this) {
        onConnectedInvoker.invoke(
            new ConnectionEvent(event.oldNode(), BhNode.this, event.userOpe()));
      }
    }    
  }

  /**
   * ノードの選択状態が変更されたときの情報を格納したレコード.
   *
   * @param node 選択状態が変更されたノード
   * @param isSelected {@code node} が選択された場合 true
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record SelectionEvent(BhNode node, boolean isSelected, UserOperation userOpe) {}

  /**
   * ノードのコンパイルエラー状態が更新されたときの情報を格納したレコード.
   *
   * @param node コンパイルエラー状態が更新されたノード
   * @param hasError {@code node} がコンパイルエラーを持つ場合 true
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record CompileErrorEvent(BhNode node, boolean hasError, UserOperation userOpe) {}

  /**
   * 子ノードが入れ替わったときの情報を格納したレコード.
   *
   * @param disconnected {@code connected} が接続される前に接続されていたノード
   * @param connected {@code disconnected} が接続されていた {@link Connector} に新しく接続されたノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record ConnectionEvent(BhNode disconnected, BhNode connected, UserOperation userOpe) {}

  /**
   * ノードが属するワークスペースが変更されたときの情報を格納したレコード.
   *
   * @param node 所属するワークスペースが変わったノード
   * @param oldWs {@code node} が所属していたワークスペース
   * @param newWs {@code node} が現在所属しているワークスペース
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record WorkspaceChangeEvent(
      BhNode node, Workspace oldWs, Workspace newWs, UserOperation userOpe) {}

  /**
   * ノードのブレークポイントの設定が変更されたときの情報を格納したレコード.
   *
   * @param node ブレークポイントの設定が変更されたノード
   * @param isBreakpointSet ブレークポイントが設定された場合 true, 設定が解除された場合 false
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record BreakpointSetEvent(BhNode node, boolean isBreakpointSet, UserOperation userOpe) {}

  /**
   * ノードの破損の有無が変更されたときの情報を格納したレコード.
   *
   * @param node 破損の有無が変更されたノード
   * @param isCorrupted このノードが破損している場合 true.
   * @param userOpe undo 用コマンドオブジェクト
   */
  public record CorruptionStateChangedEvent(
      BhNode node, boolean isCorrupted, UserOperation userOpe) {}

  /** ノードのイベントハンドラを呼び出す機能を提供するクラス. */
  public class EventInvoker {

    /**
     * 関連するノードがワークスペースから子ノードに移ったときの処理を実行する.
     *
     * @param oldReplaced 関連するノードがつながった位置に, 元々子ノードとしてつながっていたノード
     * @param userOpe undo 用コマンドオブジェクト
     */
    public void onMovedFromWsToChild(BhNode oldReplaced, UserOperation userOpe) {
      nodeEventInvoker.onMovedFromWsToChild(BhNode.this, oldReplaced, userOpe);
    }

    /**
     * 関連するノードが子ノードからワークスペースに移ったときの処理を実行する.
     *
     * @param oldParent 移る前に関連するノードが接続されていた親ノード
     * @param oldRoot 移る前に関連するノードが所属していたノードツリーのルートノード
     * @param newReplaced ワークスペースに移る際, 関連するノードの替わりにつながったノード
     * @param userOpe undo 用コマンドオブジェクト
     */
    public void onMovedFromChildToWs(
        ConnectiveNode oldParent,
        BhNode oldRoot,
        BhNode newReplaced,
        UserOperation userOpe) {
      nodeEventInvoker.onMovedFromChildToWs(BhNode.this, oldParent, oldRoot, newReplaced, userOpe);
    }

    /**
     * 関連するノードの子ノードが入れ替わったときの処理を実行する.
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
     * 関連するノードの削除前に呼ばれる処理を実行する.
     *
     * @param nodesToDelete 関連するノードと共に削除される予定のノード.
     * @param causeOfDeletion 関連するノードの削除原因
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
     * ユーザー操作により, 関連するノードがカット & ペーストされる直前に呼ばれる処理を実行する.
     *
     * @param nodesToCut 関連するノードとともにカットされる予定のノード
     * @param userOpe undo 用コマンドオブジェクト
     * @return カットをキャンセルする場合 false.  続行する場合 true.
     */
    public boolean onCutRequested(
        Collection<? extends BhNode> nodesToCut, UserOperation userOpe) {
      return nodeEventInvoker.onCutRequested(BhNode.this, nodesToCut, userOpe);
    }

    /**
     * ユーザー操作により, 関連するノードがコピー & ペーストされる直前に呼ばれる処理を実行する.
     *
     * @param nodesToCopy 関連するノードとともにコピーされる予定のノード
     * @param userOpe undo 用コマンドオブジェクト
     * @return {@link BhNode} を引数にとり, コピーするかどうかの boolean 値を返す関数.
     */
    public Predicate<? super BhNode> onCopyRequested(
        Collection<? extends BhNode> nodesToCopy, UserOperation userOpe) {
      return nodeEventInvoker.onCopyRequested(BhNode.this, nodesToCopy, userOpe);
    }
  
    /**
     * 関連するノードがテンプレートノードとして作成されたときの処理を実行する.
     *
     * @param userOpe undo 用コマンドオブジェクト
     */
    public void onCreatedAsTemplate(UserOperation userOpe) {
      nodeEventInvoker.onCreatedAsTemplate(BhNode.this, userOpe);
    }

    /**
     * 関連するノードが UI イベントを受け取ったときの処理を実行する.
     *
     * @param eventInfo UI イベントに関連する情報を格納したオブジェクト
     * @param userOpe undo 用コマンドオブジェクト
     */
    public void onUiEventReceived(UiEvent eventInfo, UserOperation userOpe) {
      nodeEventInvoker.onUiEventReceived(BhNode.this, eventInfo, userOpe);
    }

    /**
     * 関連するノードと関係のあるノード一覧を取得する.
     *
     * @return 関連するノードと関係のあるノード一覧
     */
    public Collection<BhNode> onRelatedNodesRequired() {
      return nodeEventInvoker.onRelatedNodesRequired(BhNode.this);
    }
  }
}
