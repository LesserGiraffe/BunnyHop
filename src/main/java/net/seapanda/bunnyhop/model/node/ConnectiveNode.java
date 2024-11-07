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
import net.seapanda.bunnyhop.common.constant.VersionInfo;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeAttributes;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.attribute.ImitationId;
import net.seapanda.bunnyhop.model.node.event.BhNodeEvent;
import net.seapanda.bunnyhop.model.node.imitation.ImitationBase;
import net.seapanda.bunnyhop.model.node.section.Section;
import net.seapanda.bunnyhop.model.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.model.templates.BhNodeTemplates;
import net.seapanda.bunnyhop.modelprocessor.BhModelProcessor;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * 子ノードと接続されるノード.
 *
 * @author K.Koike
 */
public class ConnectiveNode extends ImitationBase<ConnectiveNode> {

  private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
  private Section childSection;

  /**
   * コンストラクタ.
   *
   * @param childSection 子セクション
   * @param imitIdToImitNodeId イミテーション接続位置とそれに対応するイミテーションノードIDのマップ
   * @param attributes ノードの設定情報
   */
  public ConnectiveNode(
      Section childSection,
      Map<ImitationId, BhNodeId> imitIdToImitNodeId,
      BhNodeAttributes attributes) {
    super(attributes, imitIdToImitNodeId);
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
      UserOperationCommand userOpeCmd) {
    super(org, userOpeCmd);
  }

  @Override
  public ConnectiveNode copy(
      Predicate<? super BhNode> isNodeToBeCopied, UserOperationCommand userOpeCmd) {
    ConnectiveNode newNode = new ConnectiveNode(this, userOpeCmd);
    newNode.childSection = childSection.copy(isNodeToBeCopied, userOpeCmd);
    newNode.childSection.setParent(newNode);
    return newNode;
  }

  @Override
  public void accept(BhModelProcessor processor) {
    processor.visit(this);
  }

  /**
   * BhModelProcessor を子Section に渡す.
   *
   * @param processor 子Section に渡す BhModelProcessor
   */
  public void sendToSections(BhModelProcessor processor) {
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

  @Override
  public void findSymbolInDescendants(
      int generation,
      boolean toBottom,
      List<SyntaxSymbol> foundSymbolList,
      String... symbolNames) {
    if (generation == 0) {
      for (String symbolName : symbolNames) {
        if (Util.INSTANCE.equals(getSymbolName(), symbolName)) {
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
  public ConnectiveNode createImitNode(ImitationId imitId, UserOperationCommand userOpeCmd) {
    //イミテーションノード作成
    BhNode imitationNode =
        BhNodeTemplates.INSTANCE.genBhNode(getImitationNodeId(imitId), userOpeCmd);
    if (imitationNode instanceof ConnectiveNode imit) {
      //オリジナルとイミテーションの関連付け
      addImitation(imit, userOpeCmd);
      return imit;
    }
    throw new AssertionError("imitation node type inconsistency");    
  }

  @Override
  protected ConnectiveNode self() {
    return this;
  }

  @Override
  public void show(int depth) {
    String parentHashCode = "null";
    if (parentConnector != null) {
      parentHashCode = parentConnector.hashCode() + "";
    }
    String lastReplacedHash = "";
    if (getLastReplaced() != null) {
      lastReplacedHash =  getLastReplaced().hashCode() + "";
    }
    MsgPrinter.INSTANCE.msgForDebug(
        indent(depth) + "<ConnectiveNode" + "  bhID=" + getId() 
        + "  parent=" + parentHashCode + "  > " + this.hashCode());
    MsgPrinter.INSTANCE.msgForDebug(
        indent(depth + 1) + "<" + "last replaced " + lastReplacedHash + "> ");
    MsgPrinter.INSTANCE.msgForDebug(indent(depth + 1) + "<" + "imitation" + "> ");
    getImitationList().forEach(imit -> 
        MsgPrinter.INSTANCE.msgForDebug(indent(depth + 2) + "imit " + imit.hashCode())
    );
    childSection.show(depth + 1);
  }
}
