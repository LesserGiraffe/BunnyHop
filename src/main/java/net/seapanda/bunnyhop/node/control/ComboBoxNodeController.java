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

package net.seapanda.bunnyhop.node.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.TextNode;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.ComboBoxNodeView;
import net.seapanda.bunnyhop.node.view.component.SelectableItem;
import net.seapanda.bunnyhop.service.accesscontrol.TransactionNotificationService;

/**
 * {@link ComboBoxNodeView} のコントローラ.
 *
 * @author K.Koike
 */
public class ComboBoxNodeController implements BhNodeController {

  private final TextNode model;
  private final ComboBoxNodeView view;
  private final TransactionNotificationService notifService;

  /** コンストラクタ. */
  public ComboBoxNodeController(BhNodeController controller) {
    if (controller.getModel() instanceof TextNode model) {
      this.model = model;
    } else {
      throw new IllegalStateException(
          "The model is not %s".formatted(TextNode.class.getSimpleName()));
    }

    if (controller.getView() instanceof ComboBoxNodeView view) {
      this.view = view;
    } else {
      throw new IllegalStateException(
          "The view is not %s".formatted(ComboBoxNodeView.class.getSimpleName()));
    }
    notifService = controller.getNotificationService();

    setEventHandlers();
    model.getCallbackRegistry().getOnTextChanged().add(event -> {
      view.getItems().stream()
          .filter(item -> item.getModel().equals(event.newText()))
          .findFirst()
          .ifPresent(view::setValue);
    });
  }

  private void setEventHandlers() {
    model.getCallbackRegistry().getOnTextChanged().add(event ->
        view.getItems().stream()
            .filter(item -> item.getModel().equals(event.newText()))
            .findFirst()
            .ifPresent(view::setValue));

    List<SelectableItem<String, Object>> items = createItems();
    view.setItems(items);
    view.setOnItemSelected(
        (observable, oldVal, newVal) -> checkAndSetContent(oldVal, newVal));
    view.getItemByModelText(model.getText())
        .ifPresentOrElse(
            item -> view.setValue(item),
            () -> {
              model.setText(items.get(0).getModel());
              view.setValue(items.get(0));
            });
  }

  /** 新しく選択されたコンボボックスのアイテムが適切かどうかを調べて, 適切ならビューとモデルに設定する. */
  private void checkAndSetContent(
      SelectableItem<String, Object> oldItem, SelectableItem<String, Object> newItem) {
    try {
      notifService.begin();
      if (Objects.equals(newItem.getModel(), model.getText())) {
        return;
      }
      if (model.isTextAcceptable(newItem.getModel())) {
        // model の文字列を ComboBox の選択アイテムに対応したものにする
        model.setText(newItem.getModel());
        model.assignContentsToDerivatives();
      } else {
        view.setValue(oldItem);
      }
    } finally {
      notifService.end();
    }
  }

  /** コンボボックスの選択肢を作成する. */
  private List<SelectableItem<String, Object>> createItems() {
    ArrayList<SelectableItem<String, Object>> items = model.getOptions().stream()
        .map(item ->
            new SelectableItem<String, Object>(item.modelText(), item.viewObj().toString()))
        .collect(Collectors.toCollection(ArrayList::new));
    if (items.isEmpty()) {
      items.add(new SelectableItem<>(model.getText(), model.getText()));
    }
    return items;
  }

  @Override
  public BhNode getModel() {
    return model;
  }

  @Override
  public BhNodeView getView() {
    return view;
  }

  @Override
  public TransactionNotificationService getNotificationService() {
    return notifService;
  }  
}
