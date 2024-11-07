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

package net.seapanda.bunnyhop.model.node.section;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.seapanda.bunnyhop.common.constant.VersionInfo;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.model.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.modelprocessor.BhModelProcessor;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * コネクタ集合を持つグループ.
 *
 * @author K.Koike
 */
public class ConnectorSection extends Section {

  private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
  /** コネクタリスト. */
  private final List<Connector> cnctrList;

  /**
   * コンストラクタ.
   *
   * @param symbolName  終端, 非終端記号名
   * @param cnctrList 保持するコネクタのリスト
   */
  public ConnectorSection(String symbolName, List<Connector> cnctrList) {
    super(symbolName);
    this.cnctrList = cnctrList;
  }

  /**
   * コピーコンストラクタ.
   *
   * @param org コピー元オブジェクト
   * @param parentNode このセクションを保持する ConnectiveNode オブジェクト
   * @param parentSection このセクションを保持している Subsection オブジェクト
   */
  private ConnectorSection(ConnectorSection org) {
    super(org);
    cnctrList = new ArrayList<>();
  }

  @Override
  public ConnectorSection copy(
      Predicate<? super BhNode> isNodeToBeCopied, UserOperationCommand userOpeCmd) {
    var newSection = new ConnectorSection(this);
    for (int i = 0; i < cnctrList.size(); ++i) {
      Connector newConnector = cnctrList.get(i).copy(newSection, isNodeToBeCopied, userOpeCmd);
      newSection.cnctrList.add(newConnector);
    }
    return newSection;
  }

  @Override
  public void accept(BhModelProcessor visitor) {
    visitor.visit(this);
  }

  /**
   * visitor をコネクタに渡す.
   *
   * @param visitor コネクタに渡す visitor
   */
  public void sendToConnectors(BhModelProcessor visitor) {
    cnctrList.forEach(connector -> connector.accept(visitor));
  }

  /**
   * コネクタのリストを返す.
   *
   * @return コネクタのリスト
   */
  public List<Connector> getConnectorList() {
    return cnctrList;
  }

  @Override
  public void findSymbolInDescendants(
      int generation, boolean toBottom, List<SyntaxSymbol> foundSymbolList, String... symbolNames) {
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

    int childLevel = generation - 1;
    for (Connector cnctr : cnctrList) {
      cnctr.findSymbolInDescendants(
          Math.max(0, childLevel), toBottom, foundSymbolList, symbolNames);
    }
  }

  @Override
  public BhNode findOuterNode(int generation) {
    for (int i = cnctrList.size() - 1; i >= 0; --i) {
      if (cnctrList.get(i).isOuter()) {
        return cnctrList.get(i).getConnectedNode().findOuterNode(Math.max(generation - 1, -1));
      }
    }
    return null;
  }

  /**
   * モデルの構造を表示する.
   *
   * @param depth 表示インデント数
   */
  @Override
  public void show(int depth) {
    int parentHash;
    if (parentNode != null) {
      parentHash = parentNode.hashCode();
    } else {
      parentHash = parentSection.hashCode();
    }
    MsgPrinter.INSTANCE.msgForDebug(
        indent(depth) + "<ConnectorGroup  " + "name=" + getSymbolName() 
        + "  parenNode=" + parentHash  + "  > " + this.hashCode());
    cnctrList.forEach(connector -> connector.show(depth + 1));
  }
}
