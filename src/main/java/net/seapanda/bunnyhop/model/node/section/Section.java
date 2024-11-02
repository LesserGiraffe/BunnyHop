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

import java.util.function.Predicate;
import net.seapanda.bunnyhop.common.constant.VersionInfo;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * ノード定義の ConnectorSection と Section に該当するクラスの基底クラス.
 *
 * @author K.Koike
 */
public abstract class Section extends SyntaxSymbol {

  private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
  
  //どちらか一方のみのフィールドが親オブジェクトを持つ
  /** このセクションを保持している ConnectiveNode オブジェクト. */
  protected ConnectiveNode parentNode;
  /** このセクションを保持している Subsection オブジェクト. */
  protected Subsection parentSection;

  /**
   * コンストラクタ.
   *
   * @param symbolName  終端, 非終端記号名
   */
  protected Section(String symbolName) {
    super(symbolName);
  }

  /**
   * コピーコンストラクタ.
   *
   * @param org コピー元オブジェクト
   */
  protected Section(Section org) {
    super(org);
  }

  /**
   * このノードのコピーを作成し返す.
   *
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @param isNodeToBeCopied ノードがコピーの対象かどうか判定する関数
   * @return このノードのコピー
   */
  public abstract Section copy(UserOperationCommand userOpeCmd, Predicate<BhNode> isNodeToBeCopied);

  /**
   * 最後尾に繋がる外部ノードを探す.
   *
   * @param generation 取得する外部ノードの世代.
   *               例 (0: 自分, 1: 子世代の外部ノード, 2: 孫世代の外部ノード. 負の数: 末尾の外部ノードを取得する)
   * @return 最後尾に繋がる外部ノード
   */
  public abstract BhNode findOuterNode(int generation);

  /**
   * このセクションを保持している ConnectiveNode オブジェクトをセットする.
   *
   * @param parentNode このセクションを保持している ConnectiveNode オブジェクト
   */
  public void setParent(ConnectiveNode parentNode) {
    this.parentNode = parentNode;
  }

  /**
   * このセクションを保持しているSubsection オブジェクトを登録する.
   *
   * @param parentSection このセクションを保持しているSubsection オブジェクト
   */
  public void setParent(Subsection parentSection) {
    this.parentSection = parentSection;
  }

  /**
   * このセクションのルートとなるConnectiveNode オブジェクトを返す.
   *
   * @return このセクションのルートとなるConnectiveNode オブジェクト
   */
  public ConnectiveNode findParentNode() {
    if (parentNode != null) {
      return parentNode;
    }
    return parentSection.findParentNode();
  }

  @Override
  public boolean isDescendantOf(SyntaxSymbol ancestor) {
    if (this == ancestor) {
      return true;
    }
    if (parentNode != null) {
      return parentNode.isDescendantOf(ancestor);
    }
    return parentSection.isDescendantOf(ancestor);
  }

  @Override
  public SyntaxSymbol findSymbolInAncestors(String symbolName, int generation, boolean toTop) {
    if (generation == 0) {
      if (Util.INSTANCE.equals(getSymbolName(), symbolName)) {
        return this;
      }
      if (!toTop) {
        return null;
      }
    }

    if (parentNode != null) {
      return parentNode.findSymbolInAncestors(symbolName, Math.max(0, generation - 1), toTop);
    }
    return parentSection.findSymbolInAncestors(symbolName, Math.max(0, generation - 1), toTop);
  }
}
