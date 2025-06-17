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
import java.util.Map;
import java.util.function.Predicate;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.model.node.derivative.DerivativeBase;
import net.seapanda.bunnyhop.model.node.derivative.DerivativeReplacer;
import net.seapanda.bunnyhop.model.node.event.NodeEventInvoker;
import net.seapanda.bunnyhop.model.node.parameter.BhNodeId;
import net.seapanda.bunnyhop.model.node.parameter.BhNodeParameters;
import net.seapanda.bunnyhop.model.node.parameter.ConnectorId;
import net.seapanda.bunnyhop.model.node.parameter.DerivationId;
import net.seapanda.bunnyhop.model.node.section.Section;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.model.traverse.BhNodeWalker;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * 子ノードと接続されるノード.
 *
 * @author K.Koike
 */
public class ConnectiveNode extends DerivativeBase<ConnectiveNode> {

  private Section childSection;
  /** このノードに登録されたイベントハンドラを管理するオブジェクト. */
  private final transient CallbackRegistry cbRegistry = new CallbackRegistry();

  /**
   * コンストラクタ.
   *
   * @param params ノードのパラメータ
   * @param childSection 子セクション
   * @param derivationToDerivative このノードに紐づけられた派生ノードの ID ({@link BhNodeId}) と
   *                               それを選択するための ID ({@link DerivationId}) のマップ
   * @param factory ノードの生成に関連する処理を行うためのオブジェクト
   * @param replacer 派生ノードを入れ替えるためのオブジェクト
   * @param invoker ノードに対して定義されたイベントハンドラを呼び出すためのオブジェクト.
   */
  public ConnectiveNode(
      BhNodeParameters params,
      Section childSection,
      Map<DerivationId, BhNodeId> derivationToDerivative,
      BhNodeFactory factory,
      DerivativeReplacer replacer,
      NodeEventInvoker invoker) {
    super(params, derivationToDerivative, factory, replacer, invoker);
    this.childSection = childSection;
  }

  /**
   * コピーコンストラクタ.
   *
   * @param org コピー元オブジェクト
   */
  private ConnectiveNode(
      ConnectiveNode org,
      UserOperation userOpe) {
    super(org, userOpe);
  }

  @Override
  public ConnectiveNode copy(
      Predicate<? super BhNode> fnIsNodeToBeCopied, UserOperation userOpe) {
    if (!fnIsNodeToBeCopied.test(this)) {
      return null;
    }
    ConnectiveNode newNode = new ConnectiveNode(this, userOpe);
    newNode.childSection = childSection.copy(fnIsNodeToBeCopied, userOpe);
    newNode.childSection.setParent(newNode);
    return newNode;
  }

  @Override
  public void accept(BhNodeWalker processor) {
    processor.visit(this);
  }

  /**
   * {@link BhNodeWalker} を子 {@link Section} に渡す.
   *
   * @param processor 子 Section に渡す BhNodeWalker
   */
  public void sendToSections(BhNodeWalker processor) {
    childSection.accept(processor);
  }

  @Override
  public BhNode findOuterNode(int generation) {
    if (generation == 0) {
      return this;
    }
    BhNode outerNode = childSection.findOuterNode(generation);
    if (outerNode != null) {
      return outerNode;
    }
    if (generation < 0) {
      return this;
    }
    return null;
  }

  /**
   * このノードの下にある {@code id} で指定したコネクタ ID 持つコネクタを取得する.
   * 子ノード以下は探さない.
   *
   * @param id 探すコネクタの ID
   * @return {@code id} に一致するコネクタ ID を持つコネクタ.
   */
  public Connector findConnector(ConnectorId id) {
    return childSection.findConnector(id);
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
    childSection.findDescendantOf(
        Math.max(0, generation - 1), toBottom, foundSymbolList, symbolNames);
  }

  @Override
  public ConnectiveNode createDerivative(DerivationId derivationId, UserOperation userOpe) {
    // 派生ノード作成
    BhNode derivative = factory.create(getDerivativeIdOf(derivationId), userOpe);
    if (derivative instanceof ConnectiveNode node) {
      // オリジナルと派生ノードの関連付け
      addDerivative(node, userOpe);
      return node;
    }
    throw new AssertionError("derivative type inconsistency");
  }

  @Override
  protected ConnectiveNode self() {
    return this;
  }

  @Override
  public CallbackRegistry getCallbackRegistry() {
    // シリアライズしたノードを操作したときに null が返るのを防ぐ.
    if (cbRegistry == null) {
      return new CallbackRegistry();
    }
    return cbRegistry;
  }

  @Override
  public void show(int depth) {
    var parentinstId =
        (parentConnector != null) ? parentConnector.getSerialNo() : InstanceId.NONE;
    var lastReplacedInstId =
        (getLastReplaced() != null) ? getLastReplaced().getSerialNo() : InstanceId.NONE;

    System.out.println("%s<ConnectiveNode  bhID=%s  parent=%s>  %s"
        .formatted(indent(depth), getId(), parentinstId, getSerialNo()));
    System.out.println("%s<last replaced>  %s"
        .formatted(indent(depth + 1), lastReplacedInstId));
    System.out.println(indent(depth + 1) + "<derivation>");
    getDerivatives().forEach(derv -> System.out.println(
        "%s<derivative>  %s".formatted(indent(depth + 2), derv.getSerialNo())));
    childSection.show(depth + 1);
  }
}
