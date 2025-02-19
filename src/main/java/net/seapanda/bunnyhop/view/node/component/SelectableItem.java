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
public class SelectableItem {
  
  /** ビューが保持するオブジェクト. */
  private final Object viewObject;
  /** モデルが保持するテキスト. */
  private final String modelText;
  
  /** コンストラクタ. */
  public SelectableItem(String modelText, Object viewObj) {
    Objects.requireNonNull(modelText);
    Objects.requireNonNull(viewObj);
    this.modelText = modelText;
    this.viewObject = viewObj;
  }
    
  /**
   * ビューが保持するオブジェクトを取得する.
   *
   * @return ビューが保持するオブジェクト
   */
  public Object getViewObject() {
    return viewObject;
  }

  /**
   * ビューが保持するオブジェクトの文字列表現を取得する.
   *
   * @return ビューが保持するオブジェクトの文字列表現
   */
  public String getViewString() {
    return viewObject.toString();
  }

  /**
   * モデルが保持するテキストを取得する.
   *
   * @return モデル側でのテキスト
   */
  public String getModelText() {
    return modelText;
  }
  
  @Override
  public String toString() {
    return viewObject.toString();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SelectableItem selectableItem = (SelectableItem) obj;
    return Objects.equals(modelText, selectableItem.modelText)
        && Objects.equals(viewObject, selectableItem.viewObject);
  }

  @Override
  public int hashCode() {
    int hash = Objects.hashCode(this.viewObject);
    return 31 * hash + Objects.hashCode(this.modelText);
  }
}
