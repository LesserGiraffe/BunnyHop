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
import java.util.SequencedCollection;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;
import org.apache.commons.lang3.IntegerRange;

class TextRangePathFactory {

  /**
   * {@code text} の文字列から {@code ranges} で指定した部分を囲う {@link Path} を生成する.
   *
   * @param text 対象のテキストノード
   * @param ranges 囲う範囲のリスト
   * @param styleClass 生成する {@link Path} に適用する CSS スタイルのクラス名
   * @return {@code ranges} に対応する {@link Path}
   */
  static List<Path> create(
      Text text, SequencedCollection<IntegerRange> ranges, String styleClass) {
    return mergeRange(ranges).stream()
        .map(range -> createPath(text, range, styleClass))
        .toList();
  }

  /**
   * 範囲リストを走査し、重なっている範囲または隣接している範囲を 1 つに結合する.
   *
   * @param ranges 結合対象の範囲リスト
   * @return 結合後の範囲リスト
   */
  static SequencedCollection<IntegerRange> mergeRange(SequencedCollection<IntegerRange> ranges) {
    var merged = new ArrayList<IntegerRange>();
    if (ranges.isEmpty()) {
      return merged;
    }
    var current = ranges.getFirst();
    for (IntegerRange range : ranges) {
      if (range.getMinimum() <= current.getMaximum() + 1) {
        current = IntegerRange.of(current.getMinimum(), range.getMaximum());
      } else {
        merged.add(current);
        current = range;
      }
    }
    merged.add(current);
    return merged;
  }

  private static Path createPath(Text text, IntegerRange range, String styleClass) {
    PathElement[] elems = text.rangeShape(range.getMinimum(), range.getMaximum() + 1);
    var path = new Path();
    path.getElements().addAll(elems);
    path.setViewOrder(1);
    path.getStyleClass().add(styleClass);
    path.relocate(path.getBoundsInLocal().getMinX(), 0);
    return path;
  }
}
