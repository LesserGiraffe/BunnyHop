
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

package net.seapanda.bunnyhop.ui.skin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.regex.Pattern;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.shape.Path;
import javafx.scene.text.Text;
import net.seapanda.bunnyhop.search.StringSearcher;
import net.seapanda.bunnyhop.search.Substring;
import org.apache.commons.lang3.IntegerRange;

/**
 * テキストエリアのテキストを強調表示する機能を提供するスキン.
 *
 * @author K.Koike
 */
public class HighlightableTextAreaSkin extends TextAreaSkin {

  /** 強調表示中の文字列のリスト. */
  private List<Substring> highlightedTexts = new ArrayList<>();
  /** 1 層目の強調表示として現在描画されている {@link Path} のリスト. */
  private List<Path> primaryHighlights = new ArrayList<>();
  /** 2 層目の強調表示として現在描画されている {@link Path} のリスト. */
  private List<Path> secondaryHighlights = new ArrayList<>();
  /** 1 層目の強調表示に適用するスタイルのクラス名. */
  private String primaryStyleClass;
  /** 2 層目の強調表示に適用するスタイルのクラス名. */
  private String secondaryStyleClass;
  private final Text text;
  /** 強調表示する文字列のパターン. */
  private Pattern pattern;
  /** 強調表示する箇所の上限. */
  private int maxHighlights;
  /** 2 層目の強調表示をする部分文字列のインデックス. */
  private List<Integer> secondaryHighlightIndexes = new ArrayList<>();

  /**
   * コンストラクタ.
   *
   * @param textArea このスキンを適用するテキストエリア
   * @param policy テキストエリアのテキストが変更されたときの強調表示の変更方法
   */
  public HighlightableTextAreaSkin(TextArea textArea, HighlightingChangePolicy policy) {
    super(textArea);
    var sp = (ScrollPane) textArea.lookup(".scroll-pane");
    text = (Text) sp.getContent().lookup(".text");
    text.textProperty().addListener((obs, oldVal, newVal) -> onTextChanged(policy));
  }

  private void onTextChanged(HighlightingChangePolicy policy) {
    switch (policy) {
      case REFRESH -> updateHighlighting();
      case DISABLE -> disableHighlighting();
      default -> { /* Do nothing. */ }
    }
  }

  private void updateHighlighting() {
    if (isHighlightingEnabled()) {
      enableHighlighting(pattern, primaryStyleClass, maxHighlights);
    }
  }

  /**
   * テキストの強調表示を有効化する.
   *
   * <p>{@code pattern} に一致する全ての文字列を, 1 層目のスタイル ({@code styleClass}) で
   * 強調表示する. 既に 2 層目のスタイル ({@link #setSecondaryStyle}) が設定されている場合は,
   * 同じインデックスに対してそれも再適用される.
   *
   * @param pattern 強調表示する文字列の正規表現
   * @param styleClass 強調表示部分に適用する 1 層目のスタイルのクラス
   * @return {@code pattern} に一致した部分文字列のコレクション (テキスト中に現れる順)
   */
  public SequencedCollection<Substring> enableHighlighting(Pattern pattern, String styleClass) {
    return enableHighlighting(pattern, styleClass, -1);
  }

  /**
   * テキストの強調表示を有効化する.
   *
   * <p>{@code pattern} に一致する文字列を, 1 層目のスタイル ({@code styleClass}) で
   * 強調表示する. 既に 2 層目のスタイル ({@link #setSecondaryStyle}) が設定されている場合は,
   * 同じインデックスに対してそれも再適用される.
   *
   * @param pattern 強調表示する文字列の正規表現
   * @param styleClass 強調表示部分に適用する 1 層目のスタイルのクラス
   * @param maxHighlights 強調表示する箇所の上限.  負の数を指定すると全ての一致箇所を強調表示する.
   * @return {@code pattern} に一致した部分文字列のコレクション (テキスト中に現れる順)
   */
  public SequencedCollection<Substring> enableHighlighting(
      Pattern pattern, String styleClass, int maxHighlights) {
    this.primaryStyleClass = styleClass;
    this.pattern = pattern;
    this.maxHighlights = maxHighlights;
    SequencedCollection<Substring> substrings = search(pattern, maxHighlights);
    SequencedCollection<IntegerRange> ranges = substrings.stream()
        .map(str -> str.getRange().orElse(null))
        .filter(Objects::nonNull)
        .toList();
    // テキストエリアの幅が足りない場合, テキストエリアの折り返しの有効 / 無効に関わらず折り返したテキストを元に範囲が計算される.
    // これを防ぐために, 折り返し幅を 0 (= 折り返し無し) にする.
    // 折り返し幅を元の値に戻す必要はない.
    if (!getSkinnable().isWrapText()) {
      text.setWrappingWidth(0);
    }
    List<Path> newHighlights = TextRangePathFactory.create(text, ranges, primaryStyleClass);
    replaceHighlightPaths(primaryHighlights, newHighlights);
    primaryHighlights = newHighlights;
    setSecondaryStyle(secondaryStyleClass, secondaryHighlightIndexes.toArray(new Integer[0]));
    return substrings;
  }

  private SequencedCollection<Substring> search(Pattern pattern, int maxHighlights) {
    SequencedCollection<Substring> substrings =
        StringSearcher.search(pattern, text.getText(), maxHighlights);
    highlightedTexts = new ArrayList<>(substrings);
    return substrings;
  }

  private void replaceHighlightPaths(List<Path> oldPaths, List<Path> newPaths) {
    removeHighlight(oldPaths);
    addHighlight(newPaths, 0);
  }

  /** テキストの強調表示を無効化する. */
  public void disableHighlighting() {
    removeHighlight(primaryHighlights);
    removeHighlight(secondaryHighlights);
    pattern = null;
    primaryHighlights = new ArrayList<>();
    secondaryHighlights = new ArrayList<>();
    secondaryHighlightIndexes = new ArrayList<>();
    highlightedTexts = new ArrayList<>();
  }

  /** 強調表示が有効かどうかを調べる. */
  public boolean isHighlightingEnabled() {
    return pattern != null;
  }

  /** 現在強調表示されている文字列のリストを返す. */
  public SequencedCollection<Substring> getHighlightedTexts() {
    return new ArrayList<>(highlightedTexts);
  }

  /**
   * 既に強調表示している文字列のうち, {@code indexes} で指定した要素に対して,
   * 1 層目の強調表示 ({@link #enableHighlighting}) とは別のスタイルを重ねて適用する.
   *
   * <p>それまで重ねて適用されていたスタイルは全て取り除かれる.
   *
   * @param styleClass 重ねて適用するスタイルのクラス名
   * @param indexes 強調表示対象の文字列のうち, このスタイルを適用する要素のインデックス
   */
  public void setSecondaryStyle(String styleClass, Integer... indexes) {
    secondaryHighlightIndexes = List.of(indexes);
    secondaryStyleClass = styleClass;
    List<Path> newHighlights = secondaryHighlightIndexes.stream()
        .flatMap(index -> calcSecondaryStylePaths(index, styleClass).stream())
        .toList();
    replaceHighlightPaths(secondaryHighlights, newHighlights);
    secondaryHighlights = newHighlights;
  }

  private List<Path> calcSecondaryStylePaths(int index, String styleClass) {
    if (index < 0 || highlightedTexts.size() <= index) {
      return new ArrayList<>();
    }
    return highlightedTexts.get(index)
        .getRange()
        .map(range -> TextRangePathFactory.create(text, List.of(range), styleClass))
        .orElse(new ArrayList<>());
  }

  /** {@link #setSecondaryStyle} で適用したスタイルを全て取り除く. */
  public void removeSecondaryStyle() {
    var newHighlights = new ArrayList<Path>();
    replaceHighlightPaths(secondaryHighlights, newHighlights);
    secondaryHighlights = newHighlights;
    secondaryHighlightIndexes = new ArrayList<>();
  }
}
