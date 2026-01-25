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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import net.seapanda.bunnyhop.node.model.derivative.DerivativeRemover;
import net.seapanda.bunnyhop.node.model.event.ConnectorEventInvoker;
import net.seapanda.bunnyhop.node.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeId;
import net.seapanda.bunnyhop.node.model.parameter.ConnectorId;
import net.seapanda.bunnyhop.node.model.parameter.ConnectorParameters;
import net.seapanda.bunnyhop.node.model.parameter.DerivationId;
import net.seapanda.bunnyhop.node.model.parameter.DerivativeJointId;
import net.seapanda.bunnyhop.node.model.section.ConnectorSection;
import net.seapanda.bunnyhop.node.model.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.node.model.traverse.BhNodeWalker;
import net.seapanda.bunnyhop.service.undo.UserOperation;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.SimpleConsumerInvoker;

/**
 * ノードとノードをつなぐコネクタのクラス.
 *
 * @author K.Koike
 */
public class Connector extends SyntaxSymbol {

  // このコネクタの生成時のパラメータ
  private final ConnectorParameters params;
  /** 接続中のノード. */
  private BhNode connectedNode;
  /** 最後にこのコネクタに接続されていたデフォルトノードのスナップショット. */
  private transient BhNode lastDefaultNodeSnapshot;
  /** このオブジェクトを保持する ConnectorSection オブジェクト. */
  private final ConnectorSection parent;
  /** {@link BhNode} 生成用オブジェクト. */
  private final transient BhNodeFactory factory;
  /** このコネクタ対して定義されたイベントハンドラを呼び出すためのオブジェクト. */
  private final transient ConnectorEventInvoker eventInvoker;
  /** このコネクタ対して定義されたイベントハンドラの登録 / 削除を行う機能を提供するオブジェクト. */
  private final transient CallbackRegistry cbRegistry = new CallbackRegistry();

  @Override
  public void accept(BhNodeWalker visitor) {
    visitor.visit(this);
  }

  /** コンストラクタ. */
  public Connector(
      ConnectorParameters params, BhNodeFactory factory, ConnectorEventInvoker invoker) {
    super(params.name());
    this.params = params;
    this.factory = factory;
    eventInvoker = invoker;
    parent = null;
  }

  /** コピーコンストラクタ. */
  private Connector(Connector org, ConnectorSection parent, UserOperation userOpe) {
    super(org.getSymbolName());
    params = org.params;
    factory = org.factory;
    eventInvoker = org.eventInvoker;
    this.parent = parent;
    lastDefaultNodeSnapshot = copyLastDefaultNodeSnapshot(org, userOpe);
  }

  /** 元のコネクタの最後のデフォルトノードのスナップショットをコピーする. */
  private BhNode copyLastDefaultNodeSnapshot(Connector org, UserOperation userOpe) {
    if (org.lastDefaultNodeSnapshot == null) {
      return null;
    }
    BhNode snapShot = org.lastDefaultNodeSnapshot.copy(userOpe);
    DerivativeRemover.remove(snapShot, userOpe);
    return snapShot;
  }

  /**
   * このコネクタのコピーを作成して返す.
   *
   * @param parent 親コネクタセクション
   * @param fnIsNodeToBeCopied 子ノードがコピーの対象かどうかを判別する関数
   * @param userOpe undo 用コマンドオブジェクト
   * @return このノードのコピー
   */
  public Connector copy(
      ConnectorSection parent,
      Predicate<? super BhNode> fnIsNodeToBeCopied,
      UserOperation userOpe) {
    BhNode newNode = null;
    if (connectedNode != null) {
      newNode = connectedNode.copy(fnIsNodeToBeCopied, userOpe);
    }
    // コピー対象のノードでない場合, デフォルトノードを新規作成して接続する
    if (newNode == null) {
      newNode = createDefaultNode(userOpe);
    }
    var newConnector = new Connector(this, parent, userOpe);
    newConnector.connect(newNode);
    return newConnector;
  }

  /**
   * {@link BhNodeWalker} を接続されているノードに渡す.
   *
   * @param processor 接続されているノードに渡す {@link BhNodeWalker}
   */
  public void sendToConnectedNode(BhNodeWalker processor) {
    connectedNode.accept(processor);
  }

  /**
   * ノードを接続する.
   *
   * @param node 接続されるノード.  (null 不可)
   */
  public final void connect(BhNode node) {
    connect(node, new UserOperation());
  }

  /**
   * ノードを接続する.
   *
   * <p>{@code node} がデフォルトノード指定されている場合, このコネクタのデフォルトノードの ID は
   * そのノードの ID で更新される.
   * この処理は, 最後に接続されていたデフォルトノードの状態を復元するかどうかのオプションとは関係なく行われる.
   *
   * @param node 接続されるノード.  (null 不可)
   * @param userOpe undo 用コマンドオブジェクト
   */
  public final void connect(BhNode node, UserOperation userOpe) {
    Objects.requireNonNull(node);
    BhNode snapshot = computeNextDefaultNodeSnapshot(node, connectedNode);
    connectImpl(node, snapshot, userOpe);
  }

  /**
   * 接続されたノードの入れ替えと {@link #lastDefaultNodeSnapshot} の更新を行う.
   *
   * <p>このメソッドは, デフォルトノードのスナップショットを作成する処理
   * ({@link #computeNextDefaultNodeSnapshot}) を undo / redo の対象にしないために導入した.
   * 同処理を undo / redo の対象にすると <b> 旧ノード = 非デフォルトノード, 新ノード = デフォルトノード </b>
   * であった場合, undo 処理で {@link #lastDefaultNodeSnapshot} に新ノードのスナップショットが保存されるので,
   * 旧ノードがつながれていたときのスナップショットが復元されない問題が生じる.
   */
  private void connectImpl(BhNode node, BhNode snapshot, UserOperation userOpe) {
    if (connectedNode != null) {
      connectedNode.setParentConnector(null);
    }
    BhNode oldNode = connectedNode;
    connectedNode = node;
    node.setParentConnector(this);
    // コネクタの接続の更新 -> ワークスペースへの登録 -> ノード入れ替え時のイベントハンドラ呼び出し
    // の順に行う必要があるので, ここでワークスペースへの登録を行う.
    Optional.ofNullable(oldNode)
        .map(BhNode::getWorkspace)
        .ifPresent(ws -> ws.addNodeTree(node, userOpe));

    BhNode oldSnapshot = lastDefaultNodeSnapshot;
    lastDefaultNodeSnapshot = snapshot;
    userOpe.pushCmd(ope -> connectImpl(oldNode, oldSnapshot, ope));
    getCallbackRegistry().onNodeReplaced.invoke(new ReplacementEvent(oldNode, node, userOpe));
  }

  /**
   * コネクタに接続されるノードが入れ替わったときに {@link #lastDefaultNodeSnapshot} に代入すべき値を取得する.
   *
   * @param newNode コネクタに新しく接続されるノード
   * @param oldNode {@code newNode} と入れ替わるノード
   * @return コネクタに接続されるノードが入れ替わったときに {@link #lastDefaultNodeSnapshot} に代入すべき値
   */
  private BhNode computeNextDefaultNodeSnapshot(BhNode newNode, BhNode oldNode) {
    // 新ノードがデフォルトノードのとき, それまでのスナップショットは無効になるので null を返す
    if (newNode.isDefault()) {
      return null;

    // 旧ノードがデフォルトノードでかつ, 新ノードが非デフォルトノードであった場合, 旧ノードのスナップショットを返す
    } else if (oldNode != null && oldNode.isDefault()) {
      var userOpe = new UserOperation();
      BhNode snapShot = oldNode.copy(userOpe);
      DerivativeRemover.remove(snapShot, userOpe);
      return snapShot;
    }
    // それ以外の場合スナップショットに変更はない
    return lastDefaultNodeSnapshot;
  }

  /** ノードが取り外されたときに変わりに繋がるノード (= デフォルトノード) の ID を取得する. */
  public BhNodeId getDefaultNodeId() {
    if (connectedNode != null && connectedNode.isDefault()) {
      return connectedNode.getId();
    }
    if (lastDefaultNodeSnapshot != null) {
      return lastDefaultNodeSnapshot.getId();
    }
    return params.defaultNodeId();
  }

  /**
   * このコネクタに指定されたデフォルトノードを新しく作成する.
   * MVC 構造は構築しない.
   */
  public BhNode createDefaultNode(UserOperation userOpe) {
    BhNode defaultNode;
    if (!params.restoreLastDefaultNode()) {
      defaultNode = factory.create(getDefaultNodeId(), userOpe);
    } else if (connectedNode != null && connectedNode.isDefault()) {
      defaultNode = connectedNode.copy(userOpe);
      DerivativeRemover.remove(defaultNode, userOpe);
    } else if (lastDefaultNodeSnapshot != null) {
      defaultNode = lastDefaultNodeSnapshot.copy(userOpe);
      DerivativeRemover.remove(defaultNode, userOpe);
    } else {
      defaultNode = factory.create(params.defaultNodeId(), userOpe);
    }
    defaultNode.setDefault(true);
    return defaultNode;
  }

  /**
   * 最後に接続されていたデフォルトノードのスナップショットを設定する.
   *
   * <p>最後に接続されたデフォルトノードのスナップショットは基本的にこのクラスで管理する.
   * プロジェクトのロード時など, 必要な場合にのみ呼ぶこと.
   *
   * @param node このオブジェクトを最後に接続されていたデフォルトノードのスナップショットとする. (nullable)
   */
  public void setLastDefaultNodeSnapshot(BhNode node) {
    lastDefaultNodeSnapshot = node;
  }

  /** 最後に接続されていたデフォルトノードのスナップショットを取得する. */
  public Optional<BhNode> getLastDefaultNodeSnapshot() {
    return Optional.ofNullable(lastDefaultNodeSnapshot);
  }

  /**
   * このコネクタの親となるノードを返す.
   *
   * @return このコネクタの親となるノード
   */
  public ConnectiveNode getParentNode() {
    return parent.findParentNode();
  }

  /**
   * 固定コネクタかどうかを調べる.
   *
   * <p>固定コネクタ: 接続されたノードの入れ替えと取り外しができないコネクタ
   *
   * @return このコネクタが固定コネクタの場合 true を返す.
   */
  public boolean isFixed() {
    return params.fixed();
  }
  
  /**
   * 引数で指定したノードがこのコネクタに接続可能か調べる.
   *
   * @param node 接続可能か調べるノード
   * @return 引数で指定したノードがこのコネクタに接続可能な場合, true を返す
   */
  public boolean canConnect(BhNode node) {
    if (isFixed()) {
      return false;
    } // ここは残す
    return eventInvoker.onConnectabilityChecking(this, node);
  }

  public ConnectorId getId() {
    return params.connectorId();
  }

  /**
   * このコネクタに接続されているBhNode を返す.
   *
   * @return このコネクタに接続されているBhNode
   */
  public BhNode getConnectedNode() {
    return connectedNode;
  }

  /**
   * このコネクタと先祖コネクタの中に, DerivationId.NONE 以外の派生先 ID (派生ノードを特定するための ID) があればそれを返す.
   * なければ, DerivationId.NONE を返す.
   */
  DerivationId findDerivationIdUp() {    
    if (params.derivationId().equals(DerivationId.NONE)) {
      Connector parentCnctr = getParentNode().getParentConnector();
      if (parentCnctr != null) {
        return parentCnctr.findDerivationIdUp();
      }
    }
    return params.derivationId();
  }

  /**
   * 派生ノード接続位置の識別子を取得する.
   *
   * @return 派生ノード接続位置の識別子
   */
  public DerivativeJointId getDerivativeJoint() {
    return params.derivativeJointId();
  }

  /**
   * このコネクタに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このコネクタに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  CallbackRegistry getCallbackRegistry() {
    // シリアライズしたノードを操作したときに null が返るのを防ぐ.
    if (cbRegistry == null) {
      return new CallbackRegistry();
    }
    return cbRegistry;
  }

  @Override
  public void findDescendantOf(
      int generation,
      boolean toBottom,
      List<SyntaxSymbol> foundSymbolList,
      String... symbolNames) {
    if (generation == 0) {
      for (String symbolName : symbolNames) {
        if (symbolNameMatches(symbolName)) {
          foundSymbolList.add(this);
        }
      }
      if (!toBottom) {
        return;
      }
    }
    connectedNode.findDescendantOf(
        Math.max(0, generation - 1), toBottom, foundSymbolList, symbolNames);
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
    return parent.findAncestorOf(symbolName, Math.max(0, generation - 1), toTop);
  }

  @Override
  public boolean isDescendantOf(SyntaxSymbol ancestor) {
    if (this == ancestor) {
      return true;
    }
    return parent.isDescendantOf(ancestor);
  }

  /** イベントハンドラの登録 / 削除を行う機能を提供するクラス. */
  class CallbackRegistry {

    /** このノードが選択されたときのイベントハンドラを管理するオブジェクト. */
    private final transient ConsumerInvoker<ReplacementEvent> onNodeReplaced =
        new SimpleConsumerInvoker<ReplacementEvent>();
    
    /** このコネクタに接続されるノードが入れ替わったときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<ReplacementEvent>.Registry getOnNodeReplaced() {
      return onNodeReplaced.getRegistry();
    }
  }

  /**
   * ノードが入れ替わったときの情報を格納したレコード.
   *
   * @param oldNode {@code newNode} と入れ替わったノード
   * @param newNode {@code oldNode} の替わりに接続新しく接続されたノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  record ReplacementEvent(BhNode oldNode, BhNode newNode, UserOperation userOpe) {}
}
