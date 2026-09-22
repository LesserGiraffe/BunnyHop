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

package net.seapanda.bunnyhop.ui.model;

import static net.seapanda.bunnyhop.common.configuration.BhSettings.Search.maxCharsInNodeSearchResultItem;

import java.util.regex.Pattern;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.TextNodeView;
import net.seapanda.bunnyhop.search.Substring;

/**
 * ノードの検索結果を格納するクラス.
 *
 * @author K.Koike
 */
public class NodeSearchListItem {

  /** 検索結果のビューに表示する文字列. */
  private final String viewString;
  /** {@link #viewString} の中の検索に一致した部分を表す正規表現. */
  private final Pattern pattern;
  /** 検索に一致した文字列を含む {@link BhNodeView}. */
  private final TextNodeView nodeView;

  /**
   * コンストラクタ.
   *
   * @param matched 検索に一致した文字列
   * @param nodeView {@code matched} を含む {@link BhNodeView}
   */
  public NodeSearchListItem(Substring matched, TextNodeView nodeView) {
    Substring viewString = extractViewString(matched);
    this.viewString = viewString.getSource();
    pattern = getSubstringPattern(viewString.getStart(), viewString.getLength());
    this.nodeView = nodeView;
  }

  /** {@code matched} から検索結果のビューに表示する文字列を抽出する. */
  private Substring extractViewString(Substring matched) {
    if (matched.getLength() >= maxCharsInNodeSearchResultItem) {
      return new Substring(matched.toString(), 0, matched.toString());
    }
    int affixSize = (maxCharsInNodeSearchResultItem - matched.getLength()) / 2;
    String prefix = extractPrefix(matched, affixSize);
    String suffix = extractSuffix(matched, affixSize);
    String viewString = prefix + matched + suffix;
    return new Substring(viewString, prefix.length(), matched.toString());
  }

  /** {@code substring} が表す部分文字列の直前にある文字列を {@code size} 分抽出する. */
  private static String extractPrefix(Substring substring, int size) {
    StringBuilder prefix = new StringBuilder();
    for (int i = 0; i < size; ++i) {
      int charPos = substring.getStart() - i - 1;
      if (charPos < 0) {
        break;
      }
      int character = substring.getSource().charAt(charPos);
      if (character == '\n') {
        break;
      }
      prefix.append(Character.toString(character));
    }
    return prefix.reverse().toString();
  }

  /** {@code substring} が表す部分文字列の直後にある文字列を {@code size} 分抽出する. */
  private static String extractSuffix(Substring substring, int size) {
    StringBuilder suffix = new StringBuilder();
    int endPos = substring.getStart() + substring.getLength() - 1;
    for (int i = 0; i < size; ++i) {
      int charPos = endPos + i + 1;
      if (substring.getSource().length() <= charPos) {
        break;
      }
      int character = substring.getSource().charAt(charPos);
      if (character == '\n') {
        break;
      }
      suffix.append(Character.toString(character));
    }
    return suffix.toString();
  }

  /** 文字列の N 文字目から M 文字分の部分文字列に一致する正規表現を返す. */
  private static Pattern getSubstringPattern(int n, int m) {
    return Pattern.compile("(?<=^.{%s}).{%s}".formatted(n, m), Pattern.DOTALL);
  }

  /** {@link #toString} が返す文字列の中で検索に一致した部分を表す正規表現を返す. */
  public Pattern getMatchedStringPattern() {
    return pattern;
  }

  /** 検索に一致した文字列を含む {@link BhNodeView} を返す. */
  public TextNodeView getNodeView() {
    return nodeView;
  }

  @Override
  public String toString() {
    return viewString;
  }
}
