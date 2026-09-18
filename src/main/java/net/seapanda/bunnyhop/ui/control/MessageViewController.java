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

import static javafx.css.PseudoClass.getPseudoClass;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Css.Class.DEFAULT_TEXT_HIGHLIGHT;
import static net.seapanda.bunnyhop.common.configuration.BhConstants.Css.Class.FOCUSED_TEXT_HIGHLIGHT;
import static net.seapanda.bunnyhop.common.configuration.BhSettings.Search.maxResultsInMainMessage;
import static net.seapanda.bunnyhop.ui.skin.HighlightingChangePolicy.DISABLE;

import java.util.SequencedCollection;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.search.SearchBoxDelegate;
import net.seapanda.bunnyhop.search.SearchQuery;
import net.seapanda.bunnyhop.search.SearchQueryResult;
import net.seapanda.bunnyhop.search.Substring;
import net.seapanda.bunnyhop.ui.skin.HighlightableTextAreaSkin;
import net.seapanda.bunnyhop.utility.collection.ImmutableCircularList;

/**
 * ユーザへのメッセージを表示する UI 部分のコントローラ.
 *
 * @author K.Koike
 */
public class MessageViewController {

  @FXML TextArea mainMsgArea;
  @FXML Button mvSearchButton;

  private final SearchBox searchBox;
  private ImmutableCircularList<Substring> searchResult;
  private HighlightableTextAreaSkin skin;

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
    mvSearchButton.setOnAction(action -> onSearchButtonClicked());

    skin = new HighlightableTextAreaSkin(mainMsgArea, DISABLE);
    mainMsgArea.setSkin(skin);
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

  /** 検索ボタンが押されたときの処理. */
  private void onSearchButtonClicked() {
    if (searchBox.getUser() == this) {
      searchBox.close();
      return;
    }
    mvSearchButton.pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.ON), true);
    searchBox.open(new SearchBoxDelegateImpl());
  }

  /** {@link #mainMsgArea} から {@code query} に一致する文字列を探して強調する. */
  private SearchQueryResult highlightText(SearchQuery query) {
    if (query.isEmpty()) {
      return new SearchQueryResult(0, 0);
    }
    int idx;
    if (searchBox.getNumConsecutiveSameRequests() >= 2 && searchResult != null) {
      idx = query.isForward() ? searchResult.moveAhead(1) : searchResult.movePrevious(1);
    } else {
      searchResult = searchAndHighlight(query);
      idx = query.isForward() ? searchResult.getPointer() : searchResult.movePrevious(1);
    }
    if (idx >= 0) {
      mainMsgArea.positionCaret(searchResult.get(idx).getStart());
      skin.setSecondaryStyle(FOCUSED_TEXT_HIGHLIGHT, idx);
    }
    boolean truncated = searchResult.size() == maxResultsInMainMessage;
    return new SearchQueryResult(idx, searchResult.size(), truncated);
  }

  /**
   * {@code query} でメインメッセージエリア全体を検索し, 一致した文字列を強調表示した上で,
   * それらを巡回可能なリストとして返す.
   *
   * @param query 検索条件
   * @return {@code query} に一致した部分文字列を格納する巡回リスト
   */
  private ImmutableCircularList<Substring> searchAndHighlight(SearchQuery query) {
    SequencedCollection<Substring> substrings = skin.enableHighlighting(
        query.getPattern(), DEFAULT_TEXT_HIGHLIGHT, maxResultsInMainMessage);
    return new ImmutableCircularList<>(substrings);
  }


  /** {@link SearchBox} によるメッセージ欄の検索を担当するクラス. */
  private class SearchBoxDelegateImpl implements SearchBoxDelegate {
    @Override
    public SearchQueryResult onSearchRequested(SearchQuery query) {
      return highlightText(query);
    }

    @Override
    public void onClosed() {
      mvSearchButton.pseudoClassStateChanged(getPseudoClass(BhConstants.Css.Pseudo.ON), false);
      ((HighlightableTextAreaSkin) mainMsgArea.getSkin()).disableHighlighting();
    }

    @Override
    public Object getUser() {
      return MessageViewController.this;
    }
  }
}
