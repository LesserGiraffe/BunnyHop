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

import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import net.seapanda.bunnyhop.ui.view.ViewUtil;

/**
 * 検索ボックスのコントローラ.
 *
 * @author K.Koike
 */
public class SearchBoxController implements SearchBox {
  
  @FXML private HBox searchBoxViewBase;
  @FXML private TextField searchWordField;
  @FXML private ToggleButton regexButton;
  @FXML private ToggleButton caseSensitiveButton;
  @FXML private Button searchBoxCloseButton;
  @FXML private Button findPrevButton;
  @FXML private Button findNextButton;

  private Consumer<? super Query> onSearchRequested = query -> {};
  /** 同じ検索クエリと検索ハンドラで検索された回数. */
  private long countSameRequests = 0;
  private Query previousQuery;

  /** このコントローラの UI 要素を初期化する. */
  @FXML
  public void initialize() {
    searchBoxViewBase.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    setEventHandlers();
    Platform.runLater(this::updateSearchWordFieldLength);
  }

  /** イベントハンドラをセットする. */
  private void setEventHandlers() {
    searchWordField.textProperty().addListener(
        (obs, oldVal, newVal) -> updateSearchWordFieldLength());
    searchBoxCloseButton.setOnAction(event -> disable());
    findPrevButton.setOnAction(event -> onSearchRequested(false));
    findNextButton.setOnAction(event -> onSearchRequested(true));
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

  /** UI の状態と引数をもとに {@link Query} オブジェクトを作成する. */
  private Query createQuery(boolean findNext) {
    return new Query(
        searchWordField.getText(),
        regexButton.isSelected(),
        caseSensitiveButton.isSelected(),
        findNext);
  }

  /** 検索がリスエストされたときの処理. */
  private void onSearchRequested(boolean findNext) {
    var currentQuery = createQuery(findNext);
    if (!currentQuery.isEqualTo(previousQuery)) {
      countSameRequests = 0;
    }
    ++countSameRequests;
    onSearchRequested.accept(currentQuery);
    previousQuery = currentQuery;
  }

  @Override
  public void setOnSearchRequested(Consumer<? super Query> handler) {
    if (handler == null) {
      onSearchRequested = query -> {};
      countSameRequests = 0;
      return;
    }
    if (onSearchRequested != handler) {
      onSearchRequested = handler;
      countSameRequests = 0;
    }
  }

  @Override
  public void unsetOnSearchRequested(Consumer<? super Query> handler) {
    if (onSearchRequested == handler) {
      onSearchRequested = query -> {};
      countSameRequests = 0;
    }
  }

  @Override
  public void enable() {
    searchBoxViewBase.visibleProperty().set(true);
  }

  @Override
  public void disable() {
    searchBoxViewBase.visibleProperty().set(false);
    countSameRequests = 0;
  }

  @Override
  public long getNumSameRequests() {
    return countSameRequests;
  }
}
