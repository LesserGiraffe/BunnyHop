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

package net.seapanda.bunnyhop.view.node.component;

import java.util.Objects;

/**
 * コンボボックスなどに登録する選択アイテム.
 *
 * @author K.Koike
 */
public class SelectableItem<T, U> {
  
  /** 選択アイテムのモデル. */
  private final T model;
  /** 選択アイテムのビュー. */
  private final U view;
  
  /** コンストラクタ. */
  public SelectableItem(T model, U view) {
    Objects.requireNonNull(model);
    Objects.requireNonNull(view);
    this.model = model;
    this.view = view;
  }

  /**
   * モデルが保持するオブジェクトを取得する.
   *
   * @return モデルが保持するオブジェクト
   */
  public T getModel() {
    return model;
  }

  /**
   * ビューが保持するオブジェクトを取得する.
   *
   * @return ビューが保持するオブジェクト
   */
  public U getView() {
    return view;
  }

  @Override
  public String toString() {
    return model.toString() + " : " + view.toString();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SelectableItem<?, ?> selectableItem = (SelectableItem<?, ?>) obj;
    return Objects.equals(model, selectableItem.model)
        && Objects.equals(view, selectableItem.view);
  }

  @Override
  public int hashCode() {
    int hash = Objects.hashCode(this.view);
    return 31 * hash + Objects.hashCode(this.model);
  }
}
