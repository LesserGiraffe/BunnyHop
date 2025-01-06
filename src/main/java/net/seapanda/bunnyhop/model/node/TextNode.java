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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.function.Predicate;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeAttributes;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.node.attribute.DerivationId;
import net.seapanda.bunnyhop.model.node.derivative.DerivativeBase;
import net.seapanda.bunnyhop.model.node.hook.HookEvent;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.model.traverse.BhNodeWalker;
import net.seapanda.bunnyhop.service.BhService;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.Pair;
import net.seapanda.bunnyhop.view.proxy.BhNodeViewProxy;
import org.apache.commons.lang3.function.TriConsumer;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

/**
 * 文字情報を持つ終端 BhNode.
 *
 * @author K.Koike
 */
public class TextNode extends DerivativeBase<TextNode> {

  private String text = "";
  /** このオブジェクトに対応するビューの処理を行うプロキシオブジェクト. */
  private transient BhNodeViewProxy viewProxy = new BhNodeViewProxy() {};
  /** このノードに登録されたイベントハンドラを管理するオブジェクト. */
  private transient TextEventManager eventManager = new TextEventManager();

  /**
   * コンストラクタ.
   *
   * @param derivationToDerivative 派生先 ID とそれに対応する派生ノード ID のマップ
   * @param attributes ノードの設定情報
   */
  public TextNode(
      Map<DerivationId, BhNodeId> derivationToDerivative, BhNodeAttributes attributes) {
    super(attributes, derivationToDerivative);
    registerScriptName(HookEvent.ON_TEXT_FORMATTING, attributes.onTextFormatting());
    registerScriptName(
        HookEvent.ON_TEXT_CHECKING, attributes.onTextChecking());
    registerScriptName(
        HookEvent.ON_VIEW_OPTIONS_CREATING, attributes.onTextOptionsCreating());
    text = attributes.initialText();
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

  /** このオブジェクトに対応するビューの処理を行うプロキシオブジェクトを設定する. */
  public void setViewProxy(BhNodeViewProxy viewProxy) {
    Objects.requireNonNull(viewProxy);
    this.viewProxy = viewProxy;
  }


  @Override
  public TextNode copy(
      Predicate<? super BhNode> fnIsNodeToBeCopied, UserOperation userOpe) {
    return new TextNode(this, userOpe);
  }

  @Override
  public void accept(BhNodeWalker processor) {
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
    setText(text, new UserOperation());
  }

  /**
   * 引数の文字列をセットする.
   *
   * @param text セットする文字列
   * @param userOpe undo 用コマンドオブジェクト
   */
  public void setText(String text, UserOperation userOpe) {
    if (this.text.equals(text)) {
      return;
    }
    String oldText = text;
    this.text = text;
    getEventManager().invokeOnTextChanged(oldText, userOpe);
    userOpe.pushCmdOfSetText(this, oldText);
  }

  /**
   * 引数の文字列がセット可能かどうか判断する.
   *
   * @param text セット可能かどうか判断する文字列
   * @return 引数の文字列がセット可能だった
   */
  public boolean isTextAcceptable(String text) {
    Optional<String> scriptName = getScriptName(HookEvent.ON_TEXT_CHECKING);
    Script checker = scriptName.map(BhService.bhScriptManager()::getCompiledScript).orElse(null);
    if (checker == null) {
      return true;
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_TEXT, text);
      }};
    Context cx = Context.enter();
    ScriptableObject scope = getHookAgent().createScriptScope(cx, nameToObj);
    try {
      return (Boolean) checker.exec(cx, scope);
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(
          "'%s' must return a boolean value.\n%s".formatted(scriptName.get(), e));
    } finally {
      Context.exit();
    }
    return true;
  }

  /**
   * 入力されたテキストを整形して返す.
   *
   * @param text 整形対象の全文字列
   * @param addedText 前回整形したテキストから新たに追加された文字列
   * @return v1 -> テキスト全体を整形した場合 true. 追加分だけ整形した場合 false.
   *         v2 -> 整形した部分のテキスト.
   */
  public Pair<Boolean, String> formatText(String text, String addedText) {
    Optional<String> scriptName = getScriptName(HookEvent.ON_TEXT_FORMATTING);
    Script formatter = scriptName.map(BhService.bhScriptManager()::getCompiledScript).orElse(null);
    if (formatter == null) {
      return new Pair<Boolean, String>(false, addedText);
    }
    Map<String, Object> nameToObj = new HashMap<>() {{
        put(BhConstants.JsIdName.BH_TEXT, text);
        put(BhConstants.JsIdName.BH_ADDED_TEXT, addedText);
      }};
    Context cx = Context.enter();
    ScriptableObject scope = getHookAgent().createScriptScope(cx, nameToObj);
    try {
      NativeObject jsObj = (NativeObject) formatter.exec(cx, scope);
      Boolean isEntireTextFormatted =
          (Boolean) jsObj.get(BhConstants.JsIdName.BH_IS_ENTIRE_TEXT_FORMATTED);
      String formattedText = (String) jsObj.get(BhConstants.JsIdName.BH_FORMATTED_TEXT);
      return new Pair<Boolean, String>(isEntireTextFormatted, formattedText);
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(
          "Invalid text formatter  (%s).\n%s".formatted(scriptName.get(), e));
    } finally {
      Context.exit();
    }
    return new Pair<Boolean, String>(false, addedText);
  }

  /**
   * このノードが保持する可能性のあるテキストデータのリストを取得する.
   *
   * @return [ (モデルが保持するテキスト 0, ビューが保持するオブジェクト 0), 
   *           (モデルが保持するテキスト 1, ビューが保持するオブジェクト 1),
   *           ... ]
   */
  public List<Pair<String, Object>> getOptions() {
    Optional<String> scriptName = getScriptName(HookEvent.ON_VIEW_OPTIONS_CREATING);
    Script creator =
        scriptName.map(BhService.bhScriptManager()::getCompiledScript).orElse(null);
    var options = new ArrayList<Pair<String, Object>>();
    if (creator == null) {
      return options;
    }
    Context cx = Context.enter();
    ScriptableObject scriptScope = getHookAgent().createScriptScope(cx);
    try {
      List<?> contents = (List<?>) creator.exec(cx, scriptScope);
      for (Object content : contents) {
        List<?> modelAndView = (List<?>) content;
        options.add(new Pair<>(modelAndView.get(0).toString(), modelAndView.get(1)));
      }
    } catch (Exception e) {
      BhService.msgPrinter().errForDebug(scriptName.get() + "\n" + e);
    } finally {
      Context.exit();
    }
    return options;
  }

  /**
   * このノードの派生ノードにこのノードのテキストを設定する.
   * 派生ノードの先の全ての派生ノードにも再帰的にこの処理を適用する.
   */
  public void assignContentsToDerivatives() {
    getDerivatives().forEach(derv -> derv.setText(text));
    getDerivatives().forEach(derv -> derv.assignContentsToDerivatives());
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
        if (symbolNameMatches(symbolName)) {
          foundSymbolList.add(this);
        }
      }
    }
  }

  @Override
  public TextNode createDerivative(DerivationId derivationId, UserOperation userOpe) {
    BhNode node = BhService.bhNodeFactory().create(getDerivativeIdOf(derivationId), userOpe);
    if (!(node instanceof TextNode)) {
      throw new AssertionError("derivative node type inconsistency");
    }
    //オリジナルと派生ノードの関連付け
    TextNode derivative = (TextNode) node;
    addDerivative(derivative, userOpe);
    return derivative;
  }

  @Override
  public BhNodeViewProxy getViewProxy() {
    return viewProxy;
  }

  @Override
  protected TextNode self() {
    return this;
  }

  @Override
  public TextEventManager getEventManager() {
    // シリアライズしたノードを操作したときに null が返るのを防ぐ.
    if (eventManager == null) {
      return new TextEventManager();
    }
    return eventManager;
  }

  @Override
  public void show(int depth) {
    var parentinstId =
        (parentConnector != null) ? parentConnector.getInstanceId() : InstanceId.NONE;
    var lastReplacedInstId =
        (getLastReplaced() != null) ? getLastReplaced().getInstanceId() : InstanceId.NONE;

    BhService.msgPrinter().println("%s<TextNode text=%s  bhID=%s  parent=%s>  %s"
        .formatted(indent(depth), text, getId(), parentinstId, getInstanceId()));
    BhService.msgPrinter().println("%s<ws>  %s".formatted(indent(depth + 1), workspace));
    BhService.msgPrinter().println(
        "%s<last replaced>  %s".formatted(indent(depth + 1), lastReplacedInstId));
    BhService.msgPrinter().println(indent(depth + 1) + "<derivation>");
    getDerivatives().forEach(derv ->  BhService.msgPrinter().println(
        "%s<derivative>  %s".formatted(indent(depth + 2), derv.getInstanceId())));
  }

  /** イベントハンドラの管理を行うクラス. */
  public class TextEventManager extends BhNode.EventManager {
    private transient
        SequencedSet<TriConsumer<? super String, ? super String, ? super UserOperation>>
        onTextChangedList = new LinkedHashSet<>();
    
    /**
     * ノードの選択状態が変更されたときのイベントハンドラを追加する.
     *  <pre>
     *  イベントハンドラの第 1 引数: 選択状態に変換のあった {@link BhNode}
     *  イベントハンドラの第 2 引数: 選択状態. 選択されたなら true.
     *  イベントハンドラの第 3 引数: undo 用コマンドオブジェクト
     *  </pre>
     *
     * @param handler 追加するイベントハンドラ
     */
    public void addOnTextChanged(
        TriConsumer<? super String, ? super String, ? super UserOperation> handler) {
      onTextChangedList.addLast(handler);
    }

    /**
     * ノードの選択状態が変更されたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    public void removeOnTextChanged(
        TriConsumer<? super String, ? super String, ? super UserOperation> handler) {
      onTextChangedList.remove(handler);
    }

    /** 選択変更時のイベントハンドラを呼び出す. */
    private void invokeOnTextChanged(String oldText, UserOperation userOpe) {
      onTextChangedList.forEach(handler -> handler.accept(oldText, text, userOpe));
    }
  }
}
