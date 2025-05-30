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

package net.seapanda.bunnyhop.control.workspace;

import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.image.ImageView;
import net.seapanda.bunnyhop.view.Trashbox;

/**
 * ノードを削除するゴミ箱のコントローラ.
 *
 * @author K.Koike
 */
public class TrashboxController implements Trashbox {
 
  @FXML private ImageView openedTrashboxIv;
  @FXML private ImageView closedTrashboxIv;
  
  private boolean isOpened = false;
  
  @Override
  public void open() {
    openedTrashboxIv.setVisible(true);
    closedTrashboxIv.setVisible(false);
    isOpened = true;
  }
 
  @Override
  public void close() {
    openedTrashboxIv.setVisible(false);
    closedTrashboxIv.setVisible(true);
    isOpened = false;
  }

  @Override
  public void auto(double sceneX, double sceneY) {
    if (isPointInTrashBoxArea(sceneX, sceneY)) {
      open();
    } else {
      close();
    }
  }

  @Override
  public boolean isOpened() {
    return isOpened;
  }

  @Override
  public boolean isClosed() {
    return !isOpened;
  }

  /**
   * 引数で指定した位置がゴミ箱エリアにあるかどうか調べる.
   *
   * @param sceneX シーン上でのX位置
   * @param sceneY シーン上でのY位置
   * @return 引数で指定した位置がゴミ箱エリアにある場合true
   */
  private boolean isPointInTrashBoxArea(double sceneX, double sceneY) {
    Point2D localPos = closedTrashboxIv.sceneToLocal(sceneX, sceneY);
    return closedTrashboxIv.contains(localPos.getX(), localPos.getY());
  }
}
