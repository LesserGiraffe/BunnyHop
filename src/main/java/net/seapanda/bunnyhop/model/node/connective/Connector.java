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
import java.util.Objects;
import java.util.function.Predicate;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.common.constant.VersionInfo;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.configfilereader.BhScriptManager;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.imitation.ImitCnctPosId;
import net.seapanda.bunnyhop.model.node.imitation.ImitationId;
import net.seapanda.bunnyhop.model.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.model.templates.BhNodeTemplates;
import net.seapanda.bunnyhop.modelprocessor.BhModelProcessor;
import net.seapanda.bunnyhop.modelprocessor.NodeMvcBuilder;
import net.seapanda.bunnyhop.modelprocessor.TextImitationPrompter;
import net.seapanda.bunnyhop.modelservice.BhNodeHandler;
import net.seapanda.bunnyhop.undo.UserOperationCommand;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

/**
 * ノードとノードをつなぐコネクタのクラス.
 *
 * @author K.Koike
 */
public class Connector extends SyntaxSymbol {

  private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
  /** コネクタID (Connector タグの bhID). */
  private final ConnectorId id;
  /** ノードが取り外されたときに変わりに繋がるノードのID (Connector タグの bhID). */
  public final BhNodeId defaultNodeId;
  /** 最初に接続されているノードのID. */
  public final BhNodeId initNodeId;
  /** 接続中のノード. null となるのは、テンプレート構築中とClone メソッドの一瞬のみ. */
  private BhNode connectedNode;
  /** このオブジェクトを保持する ConnectorSection オブジェクト. */
  private ConnectorSection parent;
  /** このコネクタにつながるBhNodeが手動で取り外しや入れ替えができない場合true. */
  private final boolean fixed;
  /** 外部描画ノードを接続するコネクタの場合true. */
  private boolean outer = false;
  /** イミテーション生成時のID. */
  private ImitationId imitId;
  /** イミテーション生成時のタグ. */
  private ImitCnctPosId imitCnctPoint;
  /** ノードを接続可能かどうかチェックするスクリプトの名前. */
  private final String cnctCheckScriptName;
  /** コネクタに付けられたクラス. */
  private final String claz;
  /** スクリプト実行時のスコープ. */
  protected transient ScriptableObject scriptScope;

  @Override
   public void accept(BhModelProcessor visitor) {
    visitor.visit(this);
  }

  /**
   * コンストラクタ.
   *
   * @param id コネクタID (Connector タグの bhID)
   * @param defaultNodeId ノードが取り外されたときに変わりに繋がるノードのID
   * @param initialNodeId 最初に接続されているノードのID
   * @param claz コネクタに付けられたクラス
   * @param fixed このコネクタにつながるノードの入れ替えや取り外しができない場合true
   * @param cnctCheckScriptName ノードを入れ替え可能かどうかチェックするスクリプトの名前
   */
  public Connector(
      ConnectorId id,
      BhNodeId defaultNodeId,
      BhNodeId initialNodeId,
      String claz,
      boolean fixed,
      String cnctCheckScriptName) {

    super("");
    this.id = id;
    this.cnctCheckScriptName = cnctCheckScriptName;
    this.defaultNodeId = defaultNodeId;
    this.initNodeId = initialNodeId;  // BhNodeID.NONE でも initNodeID = defaultNodeID としないこと
    this.fixed = fixed;
    this.claz = claz;
  }

  /**
   * コピーコンストラクタ.
   *
   * @param org コピー元オブジェクト
   * @param name コネクタ名
   * @param imitId 作成するイミテーションノードの識別子
   * @param imitCncrPoint イミテーション接続位置の識別子
   * @param isOuter 外部描画フラグ
   * @param parent 親コネクタセクション
   */
  private Connector(
      Connector org,
      String name,
      ImitationId imitId,
      ImitCnctPosId imitCnctPoint,
      ConnectorSection parent) {

    super(name);
    id = org.id;
    defaultNodeId = org.defaultNodeId;
    initNodeId = org.initNodeId;
    cnctCheckScriptName = org.cnctCheckScriptName;
    fixed = org.fixed;
    this.imitId = imitId;
    this.imitCnctPoint = imitCnctPoint;
    this.parent = parent;
    this.claz = org.claz;
  }

  /**
   * このコネクタのコピーを作成して返す.
   *
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @param name コネクタ名
   * @param imitId 作成するイミテーションの識別子
   * @param imitCnctPoint イミテーション接続位置の識別子
   * @param parent 親コネクタセクション
   * @param isNodeToBeCopied ノードがコピーの対象かどうかを判別する関数
   * @return このノードのコピー
   */
  public Connector copy(
      UserOperationCommand userOpeCmd,
      String name,
      ImitationId imitId,
      ImitCnctPosId imitCnctPoint,
      ConnectorSection parent,
      Predicate<BhNode> isNodeToBeCopied) {
    Connector newConnector = new Connector(this, name, imitId, imitCnctPoint, parent);
    BhNode newNode = null;
    if (isNodeToBeCopied.test(connectedNode)) {
      newNode = connectedNode.copy(userOpeCmd, isNodeToBeCopied);
    } else {
      // コピー対象のノードでない場合, 初期ノードもしくはデフォルトノードを新規作成して接続する
      BhNodeId nodeId = initNodeId.equals(BhNodeId.NONE) ? defaultNodeId : initNodeId;
      newNode = BhNodeTemplates.INSTANCE.genBhNode(nodeId, userOpeCmd);
    }
    if (newNode.getId().equals(defaultNodeId) && !defaultNodeId.equals(initNodeId)) {
      newNode.setDefaultNode(true);
    }
    newConnector.connectNode(newNode, null);
    return newConnector;
  }

  /**
   * BhModelProcessor を接続されているノードに渡す.
   *
   * @param processor 接続されているノードに渡す BhModelProcessor
   */
  public void sendToConnectedNode(BhModelProcessor processor) {
    connectedNode.accept(processor);
  }

  /**
   * ノードを接続する.
   *
   * @param node 接続されるノード.  null 不可.
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public final void connectNode(BhNode node, UserOperationCommand userOpeCmd) {
    Objects.requireNonNull(node);
    if (userOpeCmd != null) {
      userOpeCmd.pushCmdOfConnectNode(connectedNode, this);
    }
    if (connectedNode != null) {
      connectedNode.setParentConnector(null);  //古いノードから親を消す
    }
    connectedNode = node;
    node.setParentConnector(this);  //新しいノードの親をセット
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
   * このコネクタにつながるノードの入れ替えと取り外しができない場合trueを返す.
   *
   * @return このコネクタにつながるノードの入れ替えと取り外しができない場合true
   */
  public boolean isFixed() {
    return fixed;
  }

  /**
   * 引数で指定したノードがこのコネクタに接続可能か調べる.
   *
   * @param node 接続可能か調べるノード
   * @return 引数で指定したノードがこのコネクタに接続可能な場合, true を返す
   */
  public boolean isConnectableWith(BhNode node) {
    if (fixed) {
      return false;
    }
    Script script = BhScriptManager.INSTANCE.getCompiledScript(cnctCheckScriptName);
    if (script == null) {
      return false;
    }
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_CURRENT_NODE, connectedNode);
    ScriptableObject.putProperty(scriptScope, BhConstants.JsKeyword.KEY_BH_NODE_TO_CONNECT, node);
    Object isConnectable;
    try {
      isConnectable = ContextFactory.getGlobal().call(cx -> script.exec(cx, scriptScope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          Util.INSTANCE.getCurrentMethodName() + " - " + cnctCheckScriptName + "\n" + e + "\n");
      return false;
    }

    if (isConnectable instanceof Boolean res) {
      return res;
    }
    return false;
  }

  /**
   * 現在繋がっているノードを取り除く.
   *
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return 現在繋がっているノードを取り除いた結果, 新しくできたノード
   */
  public BhNode remove(UserOperationCommand userOpeCmd) {

    if (connectedNode == null) {
      throw new AssertionError("try to remove null");
    }
    BhNode newNode = BhNodeTemplates.INSTANCE.genBhNode(defaultNodeId, userOpeCmd);  //デフォルトノードを作成
    NodeMvcBuilder.build(newNode); //MVC構築
    TextImitationPrompter.prompt(newNode);
    newNode.setDefaultNode(true);
    connectedNode.replace(newNode, userOpeCmd);
    return newNode;
  }

  public ConnectorId getId() {
    return id;
  }

  /** コネクタクラスを取得する. */
  public String getClaz() {
    return claz;
  }

  /**
   * このコネクタに接続されているBhNode を返す.
   *
   * @return このコネクタに接続されているBhNode
   */
  public BhNode getConnectedNode() {
    return connectedNode;
  }

  /** スクリプト実行時のスコープ変数を登録する. */
  public final void setScriptScope() {
    scriptScope = BhScriptManager.INSTANCE.createScriptScope();
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_THIS, this);
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_NODE_HANDLER, BhNodeHandler.INSTANCE);
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_MSG_SERVICE, MsgService.INSTANCE);
    ScriptableObject.putProperty(
        scriptScope,
        BhConstants.JsKeyword.KEY_BH_COMMON,
        BhScriptManager.INSTANCE.getCommonJsObj());
    ScriptableObject.putProperty(
        scriptScope, BhConstants.JsKeyword.KEY_BH_NODE_UTIL, Util.INSTANCE);
  }

  /**
   * イミテーション作成時のIDを取得する.
   *
   * @return イミテーション作成時のID
   */
  public ImitationId findImitationId() {
    if (imitId.equals(ImitationId.NONE)) {
      Connector parentCnctr = getParentNode().getParentConnector();
      if (parentCnctr != null) {
        return parentCnctr.findImitationId();
      }
    }
    return imitId;
  }

  /**
   * イミテーション接続位置の識別子を取得する.
   *
   * @return イミテーション接続位置の識別子
   */
  public ImitCnctPosId getImitCnctPoint() {
    return imitCnctPoint;
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

  @Override
  public void findSymbolInDescendants(
      int generationi,
      boolean toBottom,
      List<SyntaxSymbol> foundSymbolList,
      String... symbolNames) {
    if (generationi == 0) {
      for (String symbolName : symbolNames) {
        if (Util.INSTANCE.equals(getSymbolName(), symbolName)) {
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
      if (Util.INSTANCE.equals(getSymbolName(), symbolName)) {
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

  /**
   * モデルの構造を表示する.
   *
   * @param depth 表示インデント数
   */
  @Override
  public void show(int depth) {
    MsgPrinter.INSTANCE.msgForDebug(
        indent(depth) + "<Connector" 
        + " bhID=" + id 
        + " nodeID=" + connectedNode.getId() 
        + " parent=" + parent.hashCode() + "> " + this.hashCode());
    connectedNode.show(depth + 1);
  }
}
