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

import java.util.List;
import java.util.regex.PatternSyntaxException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.search.SearchBoxDelegate;
import net.seapanda.bunnyhop.search.SearchQuery;
import net.seapanda.bunnyhop.search.SearchQueryResult;
import net.seapanda.bunnyhop.search.Substring;
import net.seapanda.bunnyhop.ui.skin.HighlightableTextAreaSkin;

/**
 * ユーザへのメッセージを表示する UI 部分のコントローラ.
 *
 * @author K.Koike
 */
public class MessageViewController {

  @FXML TextArea mainMsgArea;
  @FXML Button mvSearchButton;

  private final SearchBox searchBox;
  private List<Substring> searchResults = null;
  private HighlightableTextAreaSkin skin;

  /** コンストラクタ. */
  public MessageViewController(SearchBox searchBox) {
    this.searchBox = searchBox;
  }

  /** このコントローラの UI 要素を初期化する. */
  @FXML
  public void initialize() {
    setEventHandlers();
    skin = new HighlightableTextAreaSkin(mainMsgArea, DISABLE);
    mainMsgArea.setSkin(skin);
  }

  /** イベントハンドラを設定する. */
  private void setEventHandlers() {
    mainMsgArea.textProperty().addListener(
        (observable, oldVal, newVal) -> onMessageChanged(newVal));
    mainMsgArea.scrollTopProperty().addListener((observable, oldVal, newVal) -> {
      if (oldVal.doubleValue() == Double.MAX_VALUE && newVal.doubleValue() == 0.0) {
        mainMsgArea.setScrollTop(Double.MAX_VALUE);
      }
    });
    mvSearchButton.setOnAction(action -> onSearchButtonClicked());
  }

  /** {@link #mainMsgArea} のテキストが変わったときの処理. */
  private void onMessageChanged(String newVal) {
    deleteOldText(newVal);
    mainMsgArea.setScrollTop(Double.MAX_VALUE);
    searchResults = null;
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
    try {
      if (searchBox.getNumConsecutiveSameRequests() <= 1 || searchResults == null) {
        searchResults = skin.enableHighlighting(
            query.getPattern(), DEFAULT_TEXT_HIGHLIGHT, maxResultsInMainMessage);
      }
      int idx = findResultIdxByDirection(query);
      if (idx >= 0) {
        mainMsgArea.positionCaret(searchResults.get(idx).getEnd() + 1);
        skin.setSecondaryStyle(FOCUSED_TEXT_HIGHLIGHT, idx);
      }
      boolean truncated = searchResults.size() == maxResultsInMainMessage;
      return new SearchQueryResult(idx, searchResults.size(), truncated);
    } catch (PatternSyntaxException e) {
      skin.disableHighlighting();
      searchResults = null;
      return new SearchQueryResult(true);
    }
  }

  /**
   * {@code query} の検索方向に応じて, キャレット位置を基準に
   * 次に強調表示すべき {@link #searchResults} 内の要素のインデックスを求める.
   *
   * @param query 検索クエリ (検索方向の判定に使用する)
   * @return 対象となる検索結果のインデックス. {@link #searchResults} が空の場合は -1
   */
  private int findResultIdxByDirection(SearchQuery query) {
    int caretPos = mainMsgArea.getCaretPosition();
    return query.isForward()
        ? findResultIdxAtOrAfterCaretPos(searchResults, caretPos)
        : findResultIdxBeforeCaretPos(searchResults, caretPos);
  }

  /**
   * {@code searchResults} の中から, 開始位置が {@code caretPos} 以上となる要素のうち,
   * 最も開始位置が小さいものを二分探索で探し, そのインデックスを返す.
   * 該当する要素がない場合は, 先頭 (添字 0) にラップアラウンドして返す
   * (末尾まで検索したら先頭に戻って検索を続けるため).
   *
   * @param searchResults 検索結果のリスト. {@link Substring#getStart()} の昇順にソートされていること
   * @param caretPos 探索の基準となるキャレット位置
   * @return 条件を満たす検索結果のインデックス. {@code searchResults} が空の場合は -1
   */
  private static int findResultIdxAtOrAfterCaretPos(List<Substring> searchResults, int caretPos) {
    if (searchResults.isEmpty()) {
      return -1;
    }
    int low = 0;
    int high = searchResults.size() - 1;
    int nearestIdx = -1;
    while (low <= high) {
      int mid = (low + high) / 2;
      if (searchResults.get(mid).getStart() >= caretPos) {
        nearestIdx = mid;
        high = mid - 1;
      } else {
        low = mid + 1;
      }
    }
    return Math.max(nearestIdx, 0);
  }

  /**
   * {@code searchResults} の中から, 終了位置の次の位置 ({@link Substring#getEnd()} + 1) が
   * {@code caretPos} より小さくなる要素のうち, 最もインデックスが大きいものを二分探索で探し,
   * そのインデックスを返す.
   * ({@code caretPos} にちょうど接している (直前に選択された) マッチは対象から除外される.)
   * 該当する要素がない場合は, 末尾 (添字 {@code searchResults.size() - 1}) に
   * ラップアラウンドして返す (先頭まで検索したら末尾に戻って検索を続けるため).
   *
   * @param searchResults 検索結果のリスト. {@link Substring#getStart()} の昇順にソートされていること
   * @param caretPos 探索の基準となるキャレット位置
   * @return 条件を満たす検索結果のインデックス. {@code searchResults} が空の場合は -1
   */
  private static int findResultIdxBeforeCaretPos(List<Substring> searchResults, int caretPos) {
    if (searchResults.isEmpty()) {
      return -1;
    }
    int low = 0;
    int high = searchResults.size() - 1;
    int nearestIdx = Integer.MAX_VALUE;
    while (low <= high) {
      int mid = (low + high) / 2;
      if (searchResults.get(mid).getEnd() + 1 < caretPos) {
        nearestIdx = mid;
        low = mid + 1;
      } else {
        high = mid - 1;
      }
    }
    return Math.min(searchResults.size() - 1, nearestIdx);
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
    public void onCleared() {}

    @Override
    public Object getUser() {
      return MessageViewController.this;
    }
  }
}
