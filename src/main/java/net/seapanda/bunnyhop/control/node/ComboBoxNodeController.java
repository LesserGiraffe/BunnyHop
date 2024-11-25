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

package net.seapanda.bunnyhop.control.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.message.BhMsg;
import net.seapanda.bunnyhop.message.MsgData;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.service.ModelExclusiveControl;
import net.seapanda.bunnyhop.view.node.ComboBoxNodeView;
import net.seapanda.bunnyhop.view.node.part.SelectableItem;

/**
 * {@link ComboBoxNodeView} のコントローラ.
 *
 * @author K.Koike
 */
public class ComboBoxNodeController extends BhNodeController {

  private final TextNode model;
  private final ComboBoxNodeView view;

  /** コンストラクタ. */
  public ComboBoxNodeController(TextNode model, ComboBoxNodeView view) {
    super(model, view);
    this.view = view;
    this.model = model;
    setEventHandlers(model, view);
  }

  /**
   * ComboBoxView のアイテム変更時のイベントハンドラを登録する.
   *
   * @param model ComboBoxView に対応する model
   * @param view イベントハンドラを登録するview
   */
  public static void setEventHandlers(TextNode model, ComboBoxNodeView view) {
    List<SelectableItem> items = createItems(model);
    view.setItems(items);
    view.setTextChangeListener(
        (observable, oldVal, newVal) -> checkAndSetContent(model, view, oldVal, newVal));

    view.getItemByModelText(model.getText())
        .ifPresentOrElse(
            item -> view.setValue(item),
            () -> {
              model.setText(items.get(0).getModelText());
              view.setValue(items.get(0));
            });
  }

  /** 新しく選択されたコンボボックスのアイテムが適切かどうかを調べて, 適切ならビューとモデルに設定する. */
  private static void checkAndSetContent(
      TextNode model, ComboBoxNodeView view, SelectableItem oldItem, SelectableItem newItem) {
    if (Objects.equals(newItem.getModelText(), model.getText())) {
      return;
    }
    ModelExclusiveControl.INSTANCE.lockForModification();
    try {
      if (model.isTextAcceptable(newItem.getModelText())) {
        // model の文字列を ComboBox の選択アイテムに対応したものにする
        model.setText(newItem.getModelText());
        model.assignContentsToDerivatives();
      } else {
        view.setValue(oldItem);
      }
    } finally {
      ModelExclusiveControl.INSTANCE.unlockForModification();
    }
  }

  /** コンボボックスの選択肢を作成する. */
  private static List<SelectableItem> createItems(TextNode model) {
    ArrayList<SelectableItem> items = model.getOptions().stream()
        .map(item -> new SelectableItem(item.v1, item.v2.toString()))
        .collect(Collectors.toCollection(ArrayList::new));
    if (items.isEmpty()) {
      items.add(new SelectableItem(model.getText(), model.getText()));
    }
    return items;
  }

  /**
   * 受信したメッセージを処理する.
   *
   * @param msg メッセージの種類
   * @param data メッセージの種類に応じて処理するデータ
   * @return メッセージを処理した結果返すデータ
   */
  @Override
  public MsgData processMsg(BhMsg msg, MsgData data) {
    switch (msg) {
      case MATCH_VIEW_CONTENT_TO_MODEL:
        matchViewToModel(model, view);
        break;

      default:
        return super.processMsg(msg, data);
    };
    return null;
  }

  /** {@code model} の持つ文字列に合わせて {@code view} の内容を変更する. */
  public static void matchViewToModel(TextNode model, ComboBoxNodeView view) {
    view.getItems().stream()
        .filter(item -> item.getModelText().equals(model.getText()))
        .findFirst()
        .ifPresent(view::setValue);
  }
}
