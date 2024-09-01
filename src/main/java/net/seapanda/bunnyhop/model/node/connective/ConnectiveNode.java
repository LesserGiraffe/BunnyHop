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

package net.seapanda.bunnyhop.model.node.connective;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.common.constant.VersionInfo;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.configfilereader.BhScriptManager;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.event.BhNodeEvent;
import net.seapanda.bunnyhop.model.node.imitation.ImitationBase;
import net.seapanda.bunnyhop.model.node.imitation.ImitationId;
import net.seapanda.bunnyhop.model.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.model.templates.BhNodeAttributes;
import net.seapanda.bunnyhop.model.templates.BhNodeTemplates;
import net.seapanda.bunnyhop.modelprocessor.BhModelProcessor;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

/**
 * 子ノードと接続されるノード.
 *
 * @author K.Koike
 */
public class ConnectiveNode extends ImitationBase<ConnectiveNode> {

  private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
  private Section childSection;            //!< セクションの集合 (ノード内部に描画されるもの)

  /**
   * コンストラクタ.
   *
   * @param childSection 子セクション
   * @param imitIdToImitNodeId イミテーションタグとそれに対応するイミテーションノードIDのマップ
   * @param attributes ノードの設定情報
   */
  public ConnectiveNode(
      Section childSection,
      Map<ImitationId, BhNodeId> imitIdToImitNodeId,
      BhNodeAttributes attributes) {

    super(attributes, imitIdToImitNodeId);
    this.childSection = childSection;
    registerScriptName(BhNodeEvent.ON_CHILD_REPLACED, attributes.getOnChildReplaced());
  }

  /**
   * コピーコンストラクタ.
   *
   * @param org コピー元オブジェクト
   */
  private ConnectiveNode(ConnectiveNode org, UserOperationCommand userOpeCmd) {
    super(org, userOpeCmd);
  }

  @Override
  public ConnectiveNode copy(UserOperationCommand userOpeCmd, Predicate<BhNode> isNodeToBeCopied) {

    ConnectiveNode newNode = new ConnectiveNode(this, userOpeCmd);
    newNode.childSection = childSection.copy(userOpeCmd, isNodeToBeCopied);
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

  /**
   * 子ノードが入れ替わったときの処理を実行する.
   *
   * @param oldChild 入れ替わった古いノード
   * @param newChild 入れ替わった新しいノード
   * @param parentCnctr 子が入れ替わったコネクタ
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void execOnChildReplaced(
      BhNode oldChild,
      BhNode newChild,
      Connector parentCnctr,
      UserOperationCommand userOpeCmd) {

    Optional<String> scriptName = getScriptName(BhNodeEvent.ON_CHILD_REPLACED);
    Script onChildReplaced =
        scriptName.map(BhScriptManager.INSTANCE::getCompiledScript).orElse(null);
    if (onChildReplaced == null) {
      return;
    }
    ScriptableObject scriptScope = getEventDispatcher().newDefaultScriptScope();
    ScriptableObject.putProperty(
        scriptScope, BhParams.JsKeyword.KEY_BH_REPLACED_NEW_NODE, newChild);
    ScriptableObject.putProperty(
        scriptScope, BhParams.JsKeyword.KEY_BH_REPLACED_OLD_NODE, oldChild);
    ScriptableObject.putProperty(
        scriptScope, BhParams.JsKeyword.KEY_BH_PARENT_CONNECTOR, parentCnctr);
    ScriptableObject.putProperty(
        scriptScope, BhParams.JsKeyword.KEY_BH_USER_OPE_CMD, userOpeCmd);
    try {
      ContextFactory.getGlobal().call(cx -> onChildReplaced.exec(cx, scriptScope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          Util.INSTANCE.getCurrentMethodName() + " - " + scriptName.get() + "\n" + e + "\n");
    }
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

    if (!(imitationNode instanceof ConnectiveNode)) {
      throw new AssertionError("imitation node type inconsistency");
    }
    //オリジナルとイミテーションの関連付け
    ConnectiveNode connectiveImit = (ConnectiveNode) imitationNode;
    addImitation(connectiveImit, userOpeCmd);
    connectiveImit.setOriginal(this, userOpeCmd);
    return connectiveImit;
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
