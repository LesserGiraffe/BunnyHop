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

package net.seapanda.bunnyhop.view.debugger;

import javafx.scene.control.TreeCell;
import net.seapanda.bunnyhop.bhprogram.debugger.VariableListItem;

/**
 * デバッガの変数一覧に表示される要素のビュー.
 *
 * @author K.Koike
 */
public class VariableListCell extends TreeCell<VariableListItem> {

  private VariableListItem model;



  @Override
  protected void updateItem(VariableListItem entry, boolean empty) {
    super.updateItem(entry, empty);
    if (empty || entry == null) {
      setText(null);
    } else {

      setText(entry.toString());
    }
  }
  // BreakpointListCell を参考にする
}
