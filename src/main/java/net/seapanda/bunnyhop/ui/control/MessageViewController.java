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

package net.seapanda.bunnyhop.ui.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.ui.control.SearchBox.Query;

/**
 * ユーザへのメッセージを表示する UI 部分のコントローラ.
 *
 * @author K.Koike
 */
public class MessageViewController {

  @FXML TextArea mainMsgArea;
  @FXML Button mvSearchButton;

  private final SearchBox searchBox;
  /** 検索結果を格納するリスト. */
  private final List<Substring> searchResults = new ArrayList<>();
  /**
   * 現在強調されている検索結果に対応する {@link #searchResults} のインデックス.
   * 負の数のとき, 現在の {@link #mainMsgArea} のテキストに対して有効な検索が行われていないことを示す.
   */
  private int currentSearchResultIdx = -1;
  private final Consumer<Query> onSearchRequested = this::highlightText;

  /** コンストラクタ. */
  public MessageViewController(SearchBox searchBox) {
    this.searchBox = searchBox;
  }

  @FXML
  public void initialize() {
    setEvenHandlers();
  }

  private void setEvenHandlers() {
    mainMsgArea.textProperty().addListener((observable, oldVal, newVal) -> onMessageChanged(newVal));

    mainMsgArea.scrollTopProperty().addListener((observable, oldVal, newVal) -> {
      if (oldVal.doubleValue() == Double.MAX_VALUE && newVal.doubleValue() == 0.0) {
        mainMsgArea.setScrollTop(Double.MAX_VALUE);
      }
    });

    mvSearchButton.setOnAction(action -> prepareSearchUi());
  }

  /** {@link #mainMsgArea} のテキストが変わったときの処理. */
  private void onMessageChanged(String newVal) {
    deleteOldText(newVal);
    mainMsgArea.setScrollTop(Double.MAX_VALUE);
    searchResults.clear();
    currentSearchResultIdx = -1;
  }

  /**
   * {@code text} の長さが表示可能なメッセージの最大長を超えていた場合,
   * {@link #mainMsgArea} から古い文字列を超過分だけ消す.
   */
  private void deleteOldText(String text) {
    if (text.length() > BhConstants.Message.MAX_MAIN_MSG_AREA_CHARS) {
      int numDeleteChars = text.length() - BhConstants.Message.MAX_MAIN_MSG_AREA_CHARS;
      mainMsgArea.deleteText(0, numDeleteChars);
    }
  }

  /** アプリケーションのメッセージを表示する {@link TextArea} を取得する. */
  public TextArea getMsgArea() {
    return mainMsgArea;
  }

  /** 検索 UI の準備をする. */
  private void prepareSearchUi() {
    searchBox.setOnSearchRequested(onSearchRequested);
    searchBox.enable();
  }

  /** {@link #mainMsgArea} から {@code query} に一致する文字列を探して強調する. */
  private void highlightText(Query query) {
    Substring result = null;
    if (searchBox.getNumSameRequests() >= 2 && currentSearchResultIdx >= 0) {
      result = findNextOrPrevSearchResult(query.findNext()).orElse(null);
    } else {
      result = findSubstrings(query).orElse(null);
    }
    if (result != null) {
      mainMsgArea.selectRange(result.pos, result.pos + result.text.length());
    }
  }

  /** 既存の検索結果のリスト中から次または前の検索結果を探して返す. */
  private Optional<Substring> findNextOrPrevSearchResult(boolean findNext) {
    if (searchResults.isEmpty()) {
      return Optional.empty();
    }
    if (findNext) {
      currentSearchResultIdx = (currentSearchResultIdx == searchResults.size() - 1)
          ? 0 : (currentSearchResultIdx + 1);
    } else {
      currentSearchResultIdx = (currentSearchResultIdx == 0)
          ? (searchResults.size() - 1) : (currentSearchResultIdx - 1);
    }
    return Optional.of(searchResults.get(currentSearchResultIdx));
  }

  /**
   * {@link #mainMsgArea} のテキスト中から
   * 条件 {@code query} に一致する全ての部分文字列を探して {@link #searchResults} に格納する. <br>
   * その後, 一致する部分文字列があれば最初のものを返す.
   */
  private Optional<Substring> findSubstrings(Query query) {
    try {
      String searchWord = query.isRegex() ? query.word() : Pattern.quote(query.word());
      int regexFlag = query.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE;
      Pattern pattern = Pattern.compile(searchWord, regexFlag);
      Matcher matcher = pattern.matcher(mainMsgArea.getText());
      List<Substring> results = matcher.results()
          .map(result -> new Substring(result.start(), result.group()))
          .toList();
      searchResults.clear();
      searchResults.addAll(results);
      currentSearchResultIdx = 0;
    } catch (PatternSyntaxException e) {
      return Optional.empty();
    }
    return searchResults.isEmpty()
        ? Optional.empty() : Optional.of(searchResults.get(currentSearchResultIdx));
  }

  /**
   * 部分文字列を格納するレコード.
   *
   * @param pos 部分文字列の元の文字列の中の位置
   * @param text 部分文字列
   */
  private record Substring(int pos, String text) {}
}
