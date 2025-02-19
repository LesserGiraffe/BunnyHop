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

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.model.node.event.ConnectorEventInvoker;
import net.seapanda.bunnyhop.model.node.parameter.BhNodeId;
import net.seapanda.bunnyhop.model.node.parameter.ConnectorId;
import net.seapanda.bunnyhop.model.node.parameter.ConnectorParameters;
import net.seapanda.bunnyhop.model.node.parameter.DerivationId;
import net.seapanda.bunnyhop.model.node.parameter.DerivativeJointId;
import net.seapanda.bunnyhop.model.node.section.ConnectorSection;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.model.traverse.BhNodeWalker;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * ノードとノードをつなぐコネクタのクラス.
 *
 * @author K.Koike
 */
public class Connector extends SyntaxSymbol {

  // このコネクタの生成時のパラメータ
  private final ConnectorParameters params;
  /** ノードが取り外されたときに変わりに繋がるノードのID (Connector タグの bhID). */
  private BhNodeId defaultNodeId;
  /** 接続中のノード. */
  private BhNode connectedNode;
  /** このオブジェクトを保持する ConnectorSection オブジェクト. */
  private final ConnectorSection parent;
  /** 外部描画ノードを接続するコネクタの場合true. */
  private boolean outer = false;
  /** {@link BhNode} 生成用オブジェクト. */
  private final transient BhNodeFactory factory;
  /** このコネクタ対して定義されたイベントハンドラを呼び出すためのオブジェクト. */
  private final transient ConnectorEventInvoker eventInvoker;

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
    this.eventInvoker = invoker;
    defaultNodeId = params.defaultNodeId();
    parent = null;
  }

  /** コピーコンストラクタ. */
  private Connector(Connector org, ConnectorSection parent) {
    super(org.getSymbolName());
    this.params = org.params;
    this.factory = org.factory;
    this.eventInvoker = org.eventInvoker;
    this.parent = parent;
    defaultNodeId = org.defaultNodeId;
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
      newNode = factory.create(defaultNodeId, userOpe);
      newNode.setDefault(true);
    }
    var newConnector = new Connector(this, parent);
    newConnector.connectNode(newNode, null);
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
   * @param userOpe undo 用コマンドオブジェクト
   */
  public final void connectNode(BhNode node, UserOperation userOpe) {
    Objects.requireNonNull(node);
    if (connectedNode != null) {
      connectedNode.setParentConnector(null);
    }
    BhNode oldNode = connectedNode;
    connectedNode = node;
    node.setParentConnector(this);
    // コネクタの接続の更新 -> ワークスペースへの登録 -> ノード入れ替え時のイベントハンドラ呼び出し
    // の順に行う必要があるので, ここでワークスペースへの登録を行う.
    if (oldNode != null && oldNode.getWorkspace() != null) {
      oldNode.getWorkspace().addNodeTree(node, userOpe);
    }
    // redo 操作の定義されたこのメソッドからイベントハンドラを呼ぶ
    if (oldNode != null) {
      oldNode.getEventManager().invokeOnNodeReplaced(node, userOpe);
    }
    if (userOpe != null) {
      userOpe.pushCmdOfConnectNode(oldNode, this);
    }
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
   * <p>
   * 固定コネクタ: 接続されたノードの入れ替えと取り外しができないコネクタ
   * </p>
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
  public boolean isConnectableWith(BhNode node) {
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
   * 外部描画ノードかどうかを示すフラグをセットする.
   *
   * @param outer このコネクタが外部描画ノードを接続する場合true
   */
  public void setOuterFlag(boolean outer) {
    this.outer = outer;
  }

  /**
   * 外部描画ノードをつなぐコネクタかどうかを調べる.
   *
   * @return 外部描画ノードをコネクタの場合true
   */
  public boolean isOuter() {
    return outer;
  }

  /**
   * ノードが取り外されたときに変わりに繋がるノードの ID (= デフォルトノード) を設定する.
   *
   * @param nodeId このコネクタに設定するデフォルトノードの ID
   */
  public void setDefaultNodeId(BhNodeId nodeId) {
    Objects.requireNonNull(nodeId);
    defaultNodeId = nodeId;
  }

  /** ノードが取り外されたときに変わりに繋がるノードの ID (= デフォルトノード) を取得する. */
  public BhNodeId getDefaultNodeId() {
    return defaultNodeId;
  }

  @Override
  public void findSymbolInDescendants(
      int generationi,
      boolean toBottom,
      List<SyntaxSymbol> foundSymbolList,
      String... symbolNames) {
    if (generationi == 0) {
      for (String symbolName : symbolNames) {
        if (symbolNameMatches(symbolName)) {
          foundSymbolList.add(this);
        }
      }
      if (!toBottom) {
        return;
      }
    }
    connectedNode.findSymbolInDescendants(
        Math.max(0, generationi - 1), toBottom, foundSymbolList, symbolNames);
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
    return parent.findSymbolInAncestors(symbolName, Math.max(0, generation - 1), toTop);
  }

  @Override
  public boolean isDescendantOf(SyntaxSymbol ancestor) {
    if (this == ancestor) {
      return true;
    }
    return parent.isDescendantOf(ancestor);
  }

  @Override
  public void show(int depth) {
    System.out.println("%s<Connector bhID=%s nodeID=%s parent=%s>  %s".formatted(
        indent(depth),
        params.connectorId(),
        connectedNode.getId(),
        parent.getInstanceId(),
        getInstanceId()));
    connectedNode.show(depth + 1);
  }
}
