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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.seapanda.bunnyhop.common.Pair;
import net.seapanda.bunnyhop.common.constant.BhConstants;
import net.seapanda.bunnyhop.common.constant.VersionInfo;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.configfilereader.BhScriptManager;
import net.seapanda.bunnyhop.message.MsgService;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeAttributes;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.attribute.DerivationId;
import net.seapanda.bunnyhop.model.node.derivative.DerivativeBase;
import net.seapanda.bunnyhop.model.node.event.BhNodeEvent;
import net.seapanda.bunnyhop.model.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.model.templates.BhNodeTemplates;
import net.seapanda.bunnyhop.modelprocessor.BhModelProcessor;
import net.seapanda.bunnyhop.undo.UserOperation;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

/**
 * 文字情報を持つ終端 BhNode.
 *
 * @author K.Koike
 */
public class TextNode  extends DerivativeBase<TextNode> {

  private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
  private String text = "";

  /**
   * コンストラクタ.
   *
   * @param derivationToDerivative 派生先 ID とそれに対応する派生ノード ID のマップ
   * @param attributes ノードの設定情報
   */
  public TextNode(
      Map<DerivationId, BhNodeId> derivationToDerivative, BhNodeAttributes attributes) {
    super(attributes, derivationToDerivative);
    registerScriptName(BhNodeEvent.ON_TEXT_FORMATTING, attributes.onTextFormatting());
    registerScriptName(
        BhNodeEvent.ON_TEXT_CHECKING, attributes.onTextChecking());
    registerScriptName(
        BhNodeEvent.ON_VIEW_CONTENTS_CREATING, attributes.onTextOptionsCreating());
    text = attributes.initString();
  }

  /**
   * コピーコンストラクタ.
   *
   * @param org コピー元オブジェクト
   */
  private TextNode(TextNode org, UserOperation userOpe) {
    super(org, userOpe);
    text = org.text;
  }

  @Override
  public TextNode copy(
      Predicate<? super BhNode> isNodeToBeCopied, UserOperation userOpe) {
    return new TextNode(this, userOpe);
  }

  @Override
  public void accept(BhModelProcessor processor) {
    processor.visit(this);
  }

  /**
   * このノードが保持している文字列を返す.
   *
   * @return 表示文字列
   */
  public String getText() {
    return text;
  }

  /**
   * 引数の文字列をセットする.
   *
   * @param text セットする文字列
   */
  public void setText(String text) {
    this.text = text;
  }


  /**
   * 引数の文字列がセット可能かどうか判断する.
   *
   * @param text セット可能かどうか判断する文字列
   * @return 引数の文字列がセット可能だった
   */
  public boolean isTextAcceptable(String text) {
    Optional<String> scriptName = getScriptName(BhNodeEvent.ON_TEXT_CHECKING);
    Script checker =
        scriptName.map(BhScriptManager.INSTANCE::getCompiledScript).orElse(null);

    if (checker == null) {
      return true;
    }
    ScriptableObject scriptScope = getEventAgent().newDefaultScriptScope();
    ScriptableObject.putProperty(scriptScope, BhConstants.JsKeyword.KEY_BH_TEXT, text);
    Object jsReturn = null;
    try {
      jsReturn = ContextFactory.getGlobal().call(cx -> checker.exec(cx, scriptScope));
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          Util.INSTANCE.getCurrentMethodName() + " - " + scriptName.get() + "\n" + e + "\n");
    }

    if (jsReturn instanceof Boolean) {
      return (Boolean) jsReturn;
    }
    return false;
  }

  /**
   * 入力されたテキストを整形して返す.
   *
   * @param text 整形対象の全文字列
   * @param addedText 前回整形したテキストから新たに追加された文字列
   * @return v1 -> テキスト全体を整形した場合 true. 追加分だけ整形した場合 false.
   *         v2 -> 整形したテキスト.
   */
  public Pair<Boolean, String> formatText(String text, String addedText) {
    Optional<String> scriptName = getScriptName(BhNodeEvent.ON_TEXT_FORMATTING);
    Script formatter = scriptName.map(BhScriptManager.INSTANCE::getCompiledScript).orElse(null);

    if (formatter == null) {
      return new Pair<Boolean, String>(false, addedText);
    }
    ScriptableObject scriptScope = getEventAgent().newDefaultScriptScope();
    ScriptableObject.putProperty(scriptScope, BhConstants.JsKeyword.KEY_BH_TEXT, text);
    ScriptableObject.putProperty(scriptScope, BhConstants.JsKeyword.KEY_BH_ADDED_TEXT, addedText);
    try {
      NativeObject jsObj = (NativeObject) ContextFactory.getGlobal().call(
          cx -> formatter.exec(cx, scriptScope));
      Boolean isEntireTextFormatted =
          (Boolean) jsObj.get(BhConstants.JsKeyword.KEY_BH_IS_ENTIRE_TEXT_FORMATTED);
      String formattedText = (String) jsObj.get(BhConstants.JsKeyword.KEY_BH_FORMATTED_TEXT);
      return new Pair<Boolean, String>(isEntireTextFormatted, formattedText);
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          Util.INSTANCE.getCurrentMethodName() + " - " + scriptName.get() + "\n" + e + "\n");
    }
    return new Pair<Boolean, String>(false, addedText);
  }
  
  /**
   * このノードが保持する可能性のあるテキストデータのリストを取得する.
   *
   * @return [ (モデルが保持するテキスト 0, ビューが保持するオブジェクト 0), 
   *           (モデルが保持するテキスト 1, ビューが保持するオブジェクト 1), ... ]
   */
  public List<Pair<String, Object>> getOptions() {
    Optional<String> scriptName = getScriptName(BhNodeEvent.ON_VIEW_CONTENTS_CREATING);
    Script creator =
        scriptName.map(BhScriptManager.INSTANCE::getCompiledScript).orElse(null);
    var options = new ArrayList<Pair<String, Object>>();
    if (creator == null) {
      return options;
    }

    ScriptableObject scriptScope = getEventAgent().newDefaultScriptScope();
    try {
      List<?> contents =
          (List<?>) ContextFactory.getGlobal().call(cx -> creator.exec(cx, scriptScope));
      for (Object content : contents) {
        List<?> modelAndView = (List<?>) content;
        options.add(new Pair<>(modelAndView.get(0).toString(), modelAndView.get(1)));
      }
    } catch (Exception e) {
      MsgPrinter.INSTANCE.errMsgForDebug(
          Util.INSTANCE.getCurrentMethodName() + " - " + scriptName.get() + "\n" + e + "\n");
    }
    return options;
  }

  /** このノードの派生ノードにこのノードのテキストを設定する. */
  public void assignContentsToDerivatives() {
    String viewText = MsgService.INSTANCE.getViewText(this);
    getDerivatives().forEach(derv -> MsgService.INSTANCE.setText(derv, text, viewText));
  }

  @Override
  public BhNode findOuterNode(int generation) {
    if (generation <= 0) {
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
    }
  }

  @Override
  public TextNode createDerivative(DerivationId derivationId, UserOperation userOpe) {
    BhNode node =
        BhNodeTemplates.INSTANCE.genBhNode(getDerivativeIdOf(derivationId), userOpe);

    if (!(node instanceof TextNode)) {
      throw new AssertionError("derivative node type inconsistency");
    }
    //オリジナルと派生ノードの関連付け
    TextNode derivative = (TextNode) node;
    addDerivative(derivative, userOpe);
    return derivative;
  }

  @Override
  protected TextNode self() {
    return this;
  }

  @Override
  public void show(int depth) {
    String parentHash = "";
    if (parentConnector != null) {
      parentHash = parentConnector.hashCode() + "";
    }
    String lastReplacedHash = "";
    if (getLastReplaced() != null) {
      lastReplacedHash =  getLastReplaced().hashCode() + "";
    }

    MsgPrinter.INSTANCE.msgForDebug(
        indent(depth) + "<TextNode" + "string=" + text + "  bhID=" + getId()
        + "  parent=" + parentHash + "> " + this.hashCode());
    MsgPrinter.INSTANCE.msgForDebug(indent(depth + 1) + "<ws " + workspace + "> ");
    MsgPrinter.INSTANCE.msgForDebug(
        indent(depth + 1) + "<" + "last replaced " + lastReplacedHash + "> ");
    MsgPrinter.INSTANCE.msgForDebug(indent(depth + 1) + "<derivation");
    getDerivatives().forEach(derv -> {
      MsgPrinter.INSTANCE.msgForDebug(indent(depth + 2) + "derivative " + derv.hashCode());
    });
  }
}
