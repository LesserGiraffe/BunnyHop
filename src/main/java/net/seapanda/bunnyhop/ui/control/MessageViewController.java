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

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.ui.model.SearchQuery;
import net.seapanda.bunnyhop.ui.model.SearchQueryResult;
import net.seapanda.bunnyhop.ui.service.search.StringSearcher;
import net.seapanda.bunnyhop.utility.collection.ImmutableCircularList;
import org.apache.commons.lang3.StringUtils;

/**
 * ユーザへのメッセージを表示する UI 部分のコントローラ.
 *
 * @author K.Koike
 */
public class MessageViewController {

  @FXML TextArea mainMsgArea;
  @FXML Button mvSearchButton;

  private final SearchBox searchBox;
  private ImmutableCircularList<StringSearcher.Substring> searchResult;

  /** コンストラクタ. */
  public MessageViewController(SearchBox searchBox) {
    this.searchBox = searchBox;
  }

  /** このコントローラの UI 要素を初期化する. */
  @FXML
  public void initialize() {
    mainMsgArea.textProperty().addListener(
        (observable, oldVal, newVal) -> onMessageChanged(newVal));

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
    searchResult = null;
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
    searchBox.setOnSearchRequested(this::highlightText);
    searchBox.enable();
  }

  /** {@link #mainMsgArea} から {@code query} に一致する文字列を探して強調する. */
  private SearchQueryResult highlightText(SearchQuery query) {
    if (StringUtils.isEmpty(query.word())) {
      return new SearchQueryResult(0, 0);
    }
    StringSearcher.Substring found = null;
    if (searchBox.getNumConsecutiveSameRequests() >= 2 && searchResult != null) {
      found = query.findNext() ? searchResult.getNext() : searchResult.getPrevious();
    } else {
      searchResult = StringSearcher.search(query, mainMsgArea.getText());
      found = searchResult.getCurrent();
    }
    if (found != null) {
      mainMsgArea.setScrollTop(-1); // 選択位置に自動でジャンプするために必要
      mainMsgArea.selectRange(found.pos(), found.pos() + found.text().length());
    }
    return new SearchQueryResult(searchResult.getPointer(), searchResult.size());
  }
}
