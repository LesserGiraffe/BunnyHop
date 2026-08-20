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
import java.util.Collection;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.regex.Pattern;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.skin.LabelSkin;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Path;
import javafx.scene.text.Text;
import net.seapanda.bunnyhop.search.StringSearcher;
import net.seapanda.bunnyhop.search.Substring;
import org.apache.commons.lang3.IntegerRange;

/**
 * ラベルのテキストを強調表示する機能を提供するスキン.
 *
 * @author K.Koike
 */
public class HighlightableLabelSkin extends LabelSkin {

  private final Collection<Substring> highlightedTexts = new ArrayList<>();
  private final Pane highlightLayer = new Pane();
  private final String styleClass;
  private final Text text;
  /** 強調表示する文字列のパターン. */
  private Pattern pattern;
  /** 強調表示する箇所の上限. */
  private int maxHighlights;

  /**
   * コンストラクタ.
   *
   * @param label このスキンを適用するラベル
   * @param styleClass 強調表示部分に適用するスタイルクラス
   * @param policy ラベルのテキストが変更されたときの強調表示の変更方法
   */
  public HighlightableLabelSkin(Label label, String styleClass, HighlightingChangePolicy policy) {
    super(label);
    this.styleClass = styleClass;
    text = (Text) label.lookup(".text");
    highlightLayer.setViewOrder(1);
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
      enableHighlighting(pattern, maxHighlights);
    }
  }

  /**
   * テキストの強調表示を有効化する.
   *
   * @param pattern 強調表示する文字列の正規表現
   */
  public SequencedCollection<Substring> enableHighlighting(Pattern pattern) {
    return enableHighlighting(pattern, -1);
  }

  /**
   * テキストの強調表示を有効化する.
   *
   * @param pattern 強調表示する文字列の正規表現
   * @param maxHighlights 強調表示する箇所の上限.  負の数を指定すると全ての一致箇所を強調表示する.
   */
  public SequencedCollection<Substring> enableHighlighting(Pattern pattern, int maxHighlights) {
    this.pattern = pattern;
    this.maxHighlights = maxHighlights;
    SequencedCollection<Substring> substrings = search(pattern, maxHighlights);
    SequencedCollection<IntegerRange> ranges = substrings.stream()
        .map(str -> str.getRange().orElse(null))
        .filter(Objects::nonNull)
        .toList();
    SequencedCollection<Path> paths = TextRangePathFactory.create(text, ranges, styleClass);
    highlightLayer.getChildren().setAll(paths);
    return substrings;
  }

  private SequencedCollection<Substring> search(Pattern pattern, int maxHighlights) {
    SequencedCollection<Substring> substrings =
        StringSearcher.search(pattern, text.getText(), maxHighlights);
    highlightedTexts.clear();
    highlightedTexts.addAll(substrings);
    return substrings;
  }

  /** テキストの強調表示を無効化する. */
  public void disableHighlighting() {
    pattern = null;
    highlightLayer.getChildren().clear();
    highlightedTexts.clear();
  }

  /** 強調表示が有効かどうかを調べる. */
  public boolean isHighlightingEnabled() {
    return pattern != null;
  }

  /** 現在強調表示されている文字列のリストを返す. */
  public SequencedCollection<Substring> getHighlightedTexts() {
    return new ArrayList<>(highlightedTexts);
  }

  @Override
  protected void layoutLabelInArea(double x, double y, double w, double h, Pos alignment) {
    if (!getChildren().contains(highlightLayer)) {
      getChildren().add(highlightLayer);
    }
    Insets insets = getSkinnable().getInsets();
    highlightLayer.relocate(text.getLayoutX(), insets.getTop());
    super.layoutLabelInArea(x, y, w, h, alignment);
  }
}
