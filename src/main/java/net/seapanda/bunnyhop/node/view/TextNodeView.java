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

package net.seapanda.bunnyhop.node.view;

import java.util.Optional;
import java.util.SequencedCollection;
import java.util.SequencedSet;
import java.util.regex.Pattern;
import javafx.scene.Node;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle;
import net.seapanda.bunnyhop.search.Substring;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.event.SimpleConsumerInvoker;

/**
 * テキスト表示可能な NodeView の基底クラス.
 *
 * @author K.Koike
 */
public abstract class TextNodeView extends LeafNodeView {

  private final TextNode model;
  private final CallbackRegistry cbRegistry;

  /** 表示しているテキストを取得する. */
  public abstract String getText();

  @Override
  public abstract Visual getVisual();

  TextNodeView(
      TextNode model, BhNodeViewStyle style, SequencedSet<Node> components, boolean isTemplate)
      throws ViewConstructionException {
    super(model, style, components, isTemplate);
    this.model = model;
    cbRegistry = new CallbackRegistry(this);
  }

  @Override
  public Optional<TextNode> getModel() {
    return Optional.ofNullable(model);
  }

  @Override
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /** ノードビューの視覚効果に関する機能を提供するクラス. */
  public abstract static class Visual extends VisualBase {

    /**
     * ノードビューが表示する文字列の強調表示を有効にする.
     *
     * @param pattern 強調表示する部分の正規表現
     * @param maxHighlilghts 強調表示する箇所の上限.  負の数を指定すると全ての一致箇所を強調表示する.
     * @return 一致した部分文字列のリスト
     */
    public abstract SequencedCollection<Substring> enableTextHighlighting(
        Pattern pattern, int maxHighlilghts);

    /**
     * ノードビューが表示する文字列の強調表示を有効にする.
     *
     * @param pattern 強調表示する部分の正規表現
     * @return 一致した部分文字列のリスト
     */
    public SequencedCollection<Substring> enableTextHighlighting(Pattern pattern) {
      return enableTextHighlighting(pattern, -1);
    }

    /** ノードビューが表示する文字列の強調表示を無効にする. */
    public abstract void disableTextHighlighting();

    /** 強調表示されている文字列のリストを取得する. */
    public abstract SequencedCollection<Substring> getHighlightedTexts();

    /** 文字列の強調表示が有効かどうかを調べる. */
    public abstract boolean isTextHighlightingEnabled();

    Visual(TextNodeView view) {
      super(view);
    }
  }

  /** ノードビューに対してイベントハンドラを追加または削除する機能を提供するクラス. */
  public static class CallbackRegistry extends CallbackRegistryBase {

    /**  関連するノードビューのテキストが変わったときのイベントハンドラを管理するオブジェクト. */
    final ConsumerInvoker<TextChangeEvent> onTextChangedInvoker = new SimpleConsumerInvoker<>();

    CallbackRegistry(TextNodeView view) {
      super(view);
    }

    /** 関連するノードビューのテキストが変わったときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<TextChangeEvent>.Registry getTextChanged() {
      return onTextChangedInvoker.getRegistry();
    }

  }

  /**
   * テキストが変更されたときの情報を格納したレコード.
   *
   * @param view テキストが変更されたノードビュー
   * @param oldText 変更前のテキスト (nullable)
   * @param newText 変更後のテキスト (nullable)
   */
  public record TextChangeEvent(TextNodeView view, String oldText, String newText) {}
}
