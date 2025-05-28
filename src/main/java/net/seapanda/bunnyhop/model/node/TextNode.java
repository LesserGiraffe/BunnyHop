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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.function.Predicate;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory;
import net.seapanda.bunnyhop.model.node.derivative.DerivativeBase;
import net.seapanda.bunnyhop.model.node.derivative.DerivativeReplacer;
import net.seapanda.bunnyhop.model.node.event.NodeEventInvoker;
import net.seapanda.bunnyhop.model.node.parameter.BhNodeId;
import net.seapanda.bunnyhop.model.node.parameter.BhNodeParameters;
import net.seapanda.bunnyhop.model.node.parameter.DerivationId;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.InstanceId;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.model.traverse.BhNodeWalker;
import net.seapanda.bunnyhop.undo.UserOperation;
import org.apache.commons.lang3.function.TriConsumer;

/**
 * 文字情報を持つ終端 BhNode.
 *
 * @author K.Koike
 */
public class TextNode extends DerivativeBase<TextNode> {

  private String text = "";
  /** このノードに登録されたイベントハンドラを管理するオブジェクト. */
  private transient TextEventManager eventManager = new TextEventManager();

  /**
   * コンストラクタ.
   *
   * @param params ノードのパラメータ
   * @param text このノードに最初に設定するテキスト
   * @param derivationToDerivative 派生先 ID とそれに対応する派生ノード ID のマップ
   * @param factory ノードの生成に関連する処理を行うオブジェクト
   * @param replacer 派生ノードを入れ替える機能を持つオブジェクト
   * @param invoker ノードに対して定義されたイベントハンドラを呼び出すためのオブジェクト.
   */
  public TextNode(
      BhNodeParameters params,
      String text,
      Map<DerivationId, BhNodeId> derivationToDerivative,
      BhNodeFactory factory,
      DerivativeReplacer replacer,
      NodeEventInvoker invoker) {
    super(params, derivationToDerivative, factory, replacer, invoker);
    this.text = (text == null) ? "" : text;
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
      Predicate<? super BhNode> fnIsNodeToBeCopied, UserOperation userOpe) {
    if (!fnIsNodeToBeCopied.test(this)) {
      return null;
    }
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
   * @return 引数の文字列がセット可能だった場合 true
   */
  public boolean isTextAcceptable(String text) {
    return nodeEventInvoker.onTextChecking(this, text);
  }

  /**
   * 入力されたテキストを整形して返す.
   *
   * @param text 整形対象の全文字列
   * @param addedText 前回整形したテキストから新たに追加された文字列
   * @return フォーマット結果
   */
  public FormatResult formatText(String text, String addedText) {
    return nodeEventInvoker.onTextFormatting(this, text, addedText);
  }

  /**
   * このノードが保持する可能性のあるテキストデータのリストを取得する.
   *
   * @return [ (モデルが保持するテキストとビューが保持するオブジェクト 0), 
   *           (モデルが保持するテキストとビューが保持するオブジェクト 1), 
   *           ... ]
   */
  public List<TextOption> getOptions() {
    return nodeEventInvoker.onTextOptionCreating(this);
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
    }
  }

  @Override
  public TextNode createDerivative(DerivationId derivationId, UserOperation userOpe) {
    BhNode node = factory.create(getDerivativeIdOf(derivationId), userOpe);
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

    System.out.println("%s<TextNode text=%s  bhID=%s  parent=%s>  %s"
        .formatted(indent(depth), text, getId(), parentinstId, getInstanceId()));
    System.out.println("%s<ws>  %s".formatted(indent(depth + 1), workspace));
    System.out.println(
        "%s<last replaced>  %s".formatted(indent(depth + 1), lastReplacedInstId));
    System.out.println(indent(depth + 1) + "<derivation>");
    getDerivatives().forEach(derv ->  System.out.println(
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
  
  /**
   * テキストをフォーマットした結果を格納するレコード.
   *
   * @param isWholeFormatted テキスト全体を整形した場合 true. 追加分だけ整形した場合 false.
   * @param text 整形した部分のテキスト
   */
  public record FormatResult(boolean isWholeFormatted, String text) {}

  /**
   * このノードが保持する可能性のあるテキストデータとそれに対応するビューが保持するオブジェクト.
   *
   * @param modelText このノードが保持する可能性のあるテキストデータ
   * @param viewObj このノードが {@code modelText} を保持したときにビューが保持すべきオブジェクト
   */
  public record TextOption(String modelText, Object viewObj)  {}
}
