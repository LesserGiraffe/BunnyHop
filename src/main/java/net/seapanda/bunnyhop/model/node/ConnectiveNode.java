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
import net.seapanda.bunnyhop.model.node.attribute.BhNodeAttributes;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.attribute.ConnectorId;
import net.seapanda.bunnyhop.model.node.attribute.DerivationId;
import net.seapanda.bunnyhop.model.node.derivative.DerivativeBase;
import net.seapanda.bunnyhop.model.node.event.BhNodeEvent;
import net.seapanda.bunnyhop.model.node.section.Section;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.model.traverse.BhNodeWalker;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * 子ノードと接続されるノード.
 *
 * @author K.Koike
 */
public class ConnectiveNode extends DerivativeBase<ConnectiveNode> {

  private Section childSection;

  /**
   * コンストラクタ.
   *
   * @param childSection 子セクション
   * @param derivationToDerivative このノードに紐づけられた派生ノードの ID ({@link BhNodeId})
   *                               とそれを選択するための ID ({@link DerivationId})
   * @param attributes ノードの設定情報
   */
  public ConnectiveNode(
      Section childSection,
      Map<DerivationId, BhNodeId> derivationToDerivative,
      BhNodeAttributes attributes) {
    super(attributes, derivationToDerivative);
    this.childSection = childSection;
    registerScriptName(BhNodeEvent.ON_CHILD_REPLACED, attributes.onChildReplaced());
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
      Predicate<? super BhNode> isNodeToBeCopied, UserOperation userOpe) {
    ConnectiveNode newNode = new ConnectiveNode(this, userOpe);
    newNode.childSection = childSection.copy(isNodeToBeCopied, userOpe);
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
  public void findSymbolInDescendants(
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
    childSection.findSymbolInDescendants(
        Math.max(0, generation - 1), toBottom, foundSymbolList, symbolNames);
  }

  @Override
  public ConnectiveNode createDerivative(DerivationId derivationId, UserOperation userOpe) {
    // 派生ノード作成
    BhNode derivative = BhService.bhNodeFactory().create(getDerivativeIdOf(derivationId), userOpe);
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
  public void show(int depth) {
    var parentinstId =
        (parentConnector != null) ? parentConnector.getInstanceId() : InstanceId.NONE;
    var lastReplacedInstId =
        (getLastReplaced() != null) ? getLastReplaced().getInstanceId() : InstanceId.NONE;

    BhService.msgPrinter().println("%s<ConnectiveNode  bhID=%s  parent=%s>  %s"
        .formatted(indent(depth), getId(), parentinstId, getInstanceId()));
    BhService.msgPrinter().println("%s<last replaced>  %s"
        .formatted(indent(depth + 1), lastReplacedInstId));
    BhService.msgPrinter().println(indent(depth + 1) + "<derivation>");
    getDerivatives().forEach(derv -> BhService.msgPrinter().println(
        "%s<derivative>  %s".formatted(indent(depth + 2), derv.getInstanceId())));
    childSection.show(depth + 1);
  }
}
