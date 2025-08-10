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

package net.seapanda.bunnyhop.control.debugger;

import java.util.ArrayList;
import java.util.List;
import java.util.SequencedCollection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import net.seapanda.bunnyhop.bhprogram.debugger.CallStackItem;
import net.seapanda.bunnyhop.common.BhSettings;
import net.seapanda.bunnyhop.common.TextDefs;
import net.seapanda.bunnyhop.control.SearchBox;
import net.seapanda.bunnyhop.control.SearchBox.Query;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService;
import net.seapanda.bunnyhop.model.ModelAccessNotificationService.Context;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.view.ViewUtil;
import net.seapanda.bunnyhop.view.debugger.CallStackCell;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.BhNodeView.LookManager.EffectTarget;
import org.apache.commons.lang3.StringUtils;

/**
 * コールスタックを表示するビューのコントローラ.
 *
 * @author K.Koike
 */
public class CallStackController {

  @FXML private CheckBox csShowAllCheckBox;
  @FXML private ListView<CallStackItem> callStackListView;
  @FXML private Button csSearchButton;
  @FXML private CheckBox csJumpCheckBox;

  private final List<CallStackItem> callStack;
  private final ModelAccessNotificationService notifService;
  private final SearchBox searchBox;

  /**
   * コンストラクタ.
   *
   * @param callStack コールスタックに表示する内容
   * @param notifService モデルへのアクセスの通知先となるオブジェクト
   * @param searchBox 検索クエリを受け取る UI コンポーネントのインタフェース
   */
  public CallStackController(
      SequencedCollection<CallStackItem> callStack,
      ModelAccessNotificationService notifService,
      SearchBox searchBox) {
    this.notifService = notifService;
    this.callStack = new ArrayList<CallStackItem>(callStack);
    this.searchBox = searchBox;
  }

  /**
   * このコントローラを初期化する.
   *
   * <p>GUI コンポーネントのインジェクション後に FXMLLoader から呼ばれることを期待する.
   */
  public void initialize() {
    callStackListView.setCellFactory(stack -> new CallStackCell());
    callStackListView.setItems(createCallStackItem());
    callStackListView.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldVal, newVal) -> onCallStackItemSelected(oldVal, newVal));
    csShowAllCheckBox.selectedProperty().addListener(
        (observable, oldVal, newVal) -> callStackListView.setItems(createCallStackItem()));
    csSearchButton.setOnAction(action -> {
      searchBox.setOnSearchRequested(this::selectItem);
      searchBox.enable();
    });
  }

  /** {@link #callStackListView} に設定するアイテムを作成する. */
  private ObservableList<CallStackItem> createCallStackItem() {
    if (!csShowAllCheckBox.isSelected()
        && callStack.size() > BhSettings.Debug.maxCallStackItems)  {
      var items = new ArrayList<CallStackItem>();
      int len = BhSettings.Debug.maxCallStackItems / 2;
      for (int i = 0; i < len; ++i) {
        items.add(callStack.get(callStack.size() - 1 - i));
      }
      items.add(new CallStackItem(-1, -1, TextDefs.Debugger.CallStack.ellipsis.get(), false));

      len = BhSettings.Debug.maxCallStackItems - len;
      for (int i = len - 1; i >= 0; --i) {
        items.add(callStack.get(i));
      }
      return FXCollections.observableArrayList(items);
    }
    return FXCollections.observableArrayList(callStack.reversed());
  }

  /** コールスタックの要素が選択されたときのイベントハンドラ. */
  private void onCallStackItemSelected(CallStackItem deselected, CallStackItem selected) {
    Context context = notifService.begin();
    try {
      var userOpe = new UserOperation(); // コールスタックの選択は undo / redo の対象にしない
      if (deselected != null) {
        deselected.deselect(userOpe);
        if (csJumpCheckBox.isSelected()) {
          deselected.getNode().ifPresent(node -> node.deselect(context.userOpe()));
        }
      }
      if (selected != null) {
        selected.select(userOpe);
        if (csJumpCheckBox.isSelected()) {
          selected.getNode().ifPresent(node -> jump(node, context.userOpe()));
        }
      }
    } finally {
      notifService.end();
    }
  }

  /** {@code node} を選択して, これを画面中央に表示する. */
  private static void jump(BhNode node, UserOperation userOpe) {
    BhNodeView nodeView = node.getView().orElse(null);
    if (node.isDeleted() || nodeView == null) {
      return;
    }
    ViewUtil.jump(nodeView, true, EffectTarget.SELF);
    node.getWorkspace().getSelectedNodes().forEach(seletedNode -> seletedNode.deselect(userOpe));
    node.select(userOpe);
  }

  /** コールスタックから {@code query} に一致する要素を探して選択する. */
  private void selectItem(Query query) {
    CallStackItem item = null;
    if (StringUtils.isEmpty(query.word())) {
      return;
    }
    if (query.isRegex()) {
      try {
        int regexFlag = query.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE;
        Pattern pattern = Pattern.compile(query.word(), regexFlag);
        item = searchCallStackFor(pattern, query.findNext());
      } catch (PatternSyntaxException e) { /* do nothing. */ }
    } else {
      item = searchCallStackFor(query.word(), query.findNext(), query.isCaseSensitive());
    }
    if (item != null) {
      callStackListView.getSelectionModel().select(item);
      callStackListView.scrollTo(item);  
    }
  }

  /** コールスタックから {@code word} に一致する {@link CallStackItem} を探す. */
  private CallStackItem searchCallStackFor(String word, boolean findNext, boolean caseSensitive) {
    if (!caseSensitive) {
      word = word.toLowerCase();
    }
    int size = callStackListView.getItems().size();
    int diff = findNext ? 1 : -1;
    int startIdx = callStackListView.getSelectionModel().getSelectedIndex();
    startIdx = (startIdx < 0) ? 0 : (startIdx + diff + size) % size;
    startIdx = findNext ? startIdx : size - 1 - startIdx;
    List<CallStackItem> items =
        findNext ? callStackListView.getItems() : callStackListView.getItems().reversed();
    for (int i = 0; i < items.size(); ++i) {
      CallStackItem item = items.get((i + startIdx) % items.size());
      String itemName = caseSensitive ? item.getName() : item.getName().toLowerCase();
      if (itemName.contains(word)) {
        return item;
      }
    }
    return null;
  }

  /** コールスタックから {@code pattern} に一致する {@link CallStackItem} を探す. */
  private CallStackItem searchCallStackFor(Pattern pattern, boolean findNext) {
    int size = callStackListView.getItems().size();
    int diff = findNext ? 1 : -1;
    int startIdx = callStackListView.getSelectionModel().getSelectedIndex();
    startIdx = (startIdx < 0) ? 0 : (startIdx + diff + size) % size;
    startIdx = findNext ? startIdx : size - 1 - startIdx;
    List<CallStackItem> items =
        findNext ? callStackListView.getItems() : callStackListView.getItems().reversed();
    for (int i = 0; i < items.size(); ++i) {
      CallStackItem item = items.get((i + startIdx) % items.size());
      if (pattern.matcher(item.getName()).find()) {
        return item;
      }
    }
    return null;
  }
}
