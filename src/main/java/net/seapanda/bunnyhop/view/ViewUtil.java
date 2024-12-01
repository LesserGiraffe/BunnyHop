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

package net.seapanda.bunnyhop.view;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.utility.Vec2D;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;
import net.seapanda.bunnyhop.view.workspace.WorkspaceViewPane;

/**
 * View に対する共通の処理を定義したクラス.
 *
 * @author K.Koike
 */
public class ViewUtil {

  /**
   * 移動後のフィールド上の位置を算出する.
   *
   * @param diff 移動量
   * @param fieldSize フィールドサイズ
   * @param curPos 現在のフィールド上の位置
   * @return 移動後の新しい位置
   */
  public static Vec2D newPosition(Vec2D diff, Vec2D fieldSize, Vec2D curPos) {
    double newDiffX = calcNewDiff(fieldSize.x, curPos.x, diff.x);
    double newDiffY = calcNewDiff(fieldSize.y, curPos.y, diff.y);
    return new Vec2D(curPos.x + newDiffX, curPos.y + newDiffY);
  }

  /**
   * 移動時のフィールド上の移動距離を算出する.
   *
   * @param diff 移動量
   * @param fieldSize フィールドサイズ
   * @param curPos 現在のフィールド上の位置
   * @return 移動距離
   */
  public static Vec2D distance(Vec2D diff, Vec2D fieldSize, Vec2D curPos) {
    double newDiffX = calcNewDiff(fieldSize.x, curPos.x, diff.x);
    double newDiffY = calcNewDiff(fieldSize.y, curPos.y, diff.y);
    return new Vec2D(newDiffX, newDiffY);
  }

  /**
   * ワークスペースから出ない範囲で新しい移動量を算出する.
   *
   * @param targetRange 新しい位置を計算する際に使う範囲
   * @param curPos 現在のWS上での位置
   * @param diff 移動量
   * @return 新しい移動量
   */
  private static double calcNewDiff(double targetRange, double curPos, double diff) {
    boolean curPosIsInTargetRange = (0 < curPos) && (curPos < targetRange);
    if (curPosIsInTargetRange) {
      double newPos = curPos + diff;
      boolean newPosIsInTargetRange = (0 < newPos) && (newPos < targetRange);
      // 現在範囲内に居て移動後に範囲外に居る場合, 移動させない
      if (!newPosIsInTargetRange) {
        if (newPos < 0) {
          return -curPos + 1.0;
        } else {
          return targetRange - curPos - 1.0;
        }
      }
    }
    return diff;
  }

  /**
   * {@code node} のワークスペース上での位置を取得する.
   *
   * @param node ワークペース上での位置を計算するノード
   * @return {@code node} のワークスペース上での位置.
   */
  public static Vec2D getPosOnWorkspace(Node node) {
    Parent parent = node.getParent();
    while (parent != null && !BhConstants.Fxml.ID_WS_PANE.equals(parent.getId())) {
      parent = parent.getParent();
    }
    if (parent != null) {
      Point2D posOnScene = node.localToScene(0.0, 0.0);
      Point2D posOnWorkspace = parent.sceneToLocal(posOnScene);
      return new Vec2D(posOnWorkspace.getX(), posOnWorkspace.getY());
    }
    return new Vec2D(0.0, 0.0);
  }

  /**
   * {@code node} が属するワークスペースビューを取得する.
   *
   * @return {@code node} が属するワークスペースビュー.
   *         どのワークスペースビューにも属していない場合は null を返す.
   */
  public static WorkspaceView getWorkspaceView(Node node) {
    Parent parent = node.getParent();
    while (parent != null && !BhConstants.Fxml.ID_WS_PANE.equals(parent.getId())) {
      parent = parent.getParent();
    }
    if (parent != null) {
      return ((WorkspaceViewPane) parent).getContainer();
    }
    return null;
  }

  /**
   * 文字列を表示したときのサイズを計算する.
   *
   * @param str サイズを計算する文字列
   * @param font フォント
   * @param boundType 境界算出方法
   * @param lineSpacing 行間
   */
  public static Vec2D calcStrBounds(
        String str, Font font, TextBoundsType boundType, double lineSpacing) {
    Text text = new Text(str);
    text.setFont(font);
    text.setBoundsType(boundType);
    text.setLineSpacing(lineSpacing);
    return new Vec2D(text.getBoundsInLocal().getWidth(), text.getBoundsInLocal().getHeight());
  }

  /**
   * 引数で指定した文字列の表示幅を計算する.
   *
   * @param str 表示幅を計算する文字列
   * @param font 表示時のフォント
   * @return 文字列を表示したときの幅
   */
  public static double calcStrWidth(String str, Font font) {
    Text text = new Text(str);
    text.setFont(font);
    return text.getBoundsInLocal().getWidth();
  }
}
