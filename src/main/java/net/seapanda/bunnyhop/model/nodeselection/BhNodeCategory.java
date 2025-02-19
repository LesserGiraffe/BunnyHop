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

package net.seapanda.bunnyhop.model.nodeselection;

/**
 * ノードカテゴリを表す TreeView の各セルのモデルクラス.
 *
 * @author K.koike
 */
public class BhNodeCategory {
  public final String categoryName;
  private String cssClass = "";

  public BhNodeCategory(String category) {
    this.categoryName = category;
  }

  @Override
  public String toString() {
    return categoryName == null ? "" : categoryName;
  }

  public void setCssClass(String cssClass) {
    this.cssClass = cssClass;
  }

  public String getCssClass() {
    return cssClass;
  }
}