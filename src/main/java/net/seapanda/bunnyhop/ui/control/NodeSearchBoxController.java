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

import java.util.Objects;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import net.seapanda.bunnyhop.common.text.TextDefs;
import net.seapanda.bunnyhop.search.NullSearchBoxDelegate;
import net.seapanda.bunnyhop.search.SearchBoxDelegate;
import net.seapanda.bunnyhop.search.SearchQuery;
import net.seapanda.bunnyhop.search.SearchQueryResult;
import net.seapanda.bunnyhop.ui.view.ViewUtil;


/**
 * ノード検索用検索ボックスのコントローラ.
 *
 * @author K.Koike
 */
public class NodeSearchBoxController implements SearchBox {

  @FXML private HBox nodeSearchBoxViewBase;
  @FXML private TextField searchWordField;
  @FXML private ToggleButton regexButton;
  @FXML private ToggleButton caseSensitiveButton;
  @FXML private Button requestButton;
  @FXML private Button clearButton;
  @FXML private Label searchResultLabel;
  /** 同じ検索クエリと検索ハンドラで検索された回数. */
  private long countConsecutiveSameRequests = 0;
  private SearchQuery previousQuery;
  private SearchBoxDelegate delegate = new NullSearchBoxDelegate();

  /** このコントローラの UI 要素を初期化する. */
  @FXML
  public void initialize() {
    nodeSearchBoxViewBase.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    setEventHandlers();
    Platform.runLater(this::updateSearchWordFieldLength);
  }

  /** イベントハンドラをセットする. */
  private void setEventHandlers() {
    searchWordField.textProperty().addListener(
        (obs, oldVal, newVal) -> updateSearchWordFieldLength());
    searchWordField.setOnKeyPressed(this::onKeyPressed);
    requestButton.setOnAction(event -> onSearchRequested());
    clearButton.setOnAction(event -> clearSearchResult());
  }

  /** 検索ワード入力フィールドの幅をテキストの長さに応じて帰る. */
  private void updateSearchWordFieldLength() {
    Text textPart = (Text) searchWordField.lookup(".text");
    if (textPart == null) {
      return;
    }
    // 正確な文字部分の境界を取得するため, GUI 部品内部の Text の境界は使わない.
    double newWidth = ViewUtil.calcStrWidth(textPart.getText(), textPart.getFont());
    newWidth = Math.clamp(newWidth, searchWordField.getMinWidth(), searchWordField.getMaxWidth());
    // 幅を (文字幅 + パディング) にするとキャレットの移動時に文字が左右に移動するので定数 3 を足す.
    // この定数はフォントやパディングが違っても機能する.
    newWidth +=
        searchWordField.getPadding().getLeft() + searchWordField.getPadding().getRight() + 3;
    searchWordField.setPrefWidth(newWidth);
  }

  private void onKeyPressed(KeyEvent event) {
    if (event.getCode() == KeyCode.ENTER) {
      onSearchRequested();
    }
  }

  /** UI の状態と引数をもとに {@link SearchQuery} オブジェクトを作成する. */
  private SearchQuery createQuery() {
    return new SearchQuery(
        searchWordField.getText(),
        regexButton.isSelected(),
        caseSensitiveButton.isSelected(),
        true);
  }

  /** 検索をリクエストされたときの処理. */
  private void onSearchRequested() {
    clearSearchResult();
    var currentQuery = createQuery();
    if (!currentQuery.isEqualTo(previousQuery)) {
      countConsecutiveSameRequests = 0;
    }
    ++countConsecutiveSameRequests;
    SearchQueryResult result = delegate.onSearchRequested(currentQuery);
    setSearchResult(result);
    previousQuery = currentQuery;
  }

  @Override
  public void open(SearchBoxDelegate delegate) {
    Objects.requireNonNull(delegate);
    if (this.delegate.getUser() != delegate.getUser()) {
      close();
      this.delegate = delegate;
    }
    nodeSearchBoxViewBase.visibleProperty().set(true);
  }

  @Override
  public void close() {
    nodeSearchBoxViewBase.visibleProperty().set(false);
    countConsecutiveSameRequests = 0;
    clearSearchResult();
    delegate.onClosed();
    delegate = new NullSearchBoxDelegate();
  }

  @Override
  public long getNumConsecutiveSameRequests() {
    return countConsecutiveSameRequests;
  }

  @Override
  public void setSearchResult(SearchQueryResult result) {
    if (result == null) {
      clearSearchResult();
      return;
    }
    String plus = result.truncated() ? "+" : "";
    String text = "%s%s".formatted(result.numFound(), plus);
    searchResultLabel.setText(TextDefs.SearchBox.resultCount.get(text));
  }

  @Override
  public void clearSearchResult() {
    searchResultLabel.setText("");
    delegate.onCleared();
  }

  @Override
  public Object getUser() {
    return delegate.getUser();
  }
}
