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

import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollBar;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.view.node.BhNodeView;
import net.seapanda.bunnyhop.view.node.BhNodeView.LookManager.EffectTarget;
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
    while (parent != null && !BhConstants.UiId.WS_PANE.equals(parent.getId())) {
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
    while (parent != null && !BhConstants.UiId.WS_PANE.equals(parent.getId())) {
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

  /**
   * 現在のスレッドが UI スレッドの場合, {@code handler} を実行してからコントロールを返す.
   * そうでない場合, 後で UI スレッドで {@code handler} が実行されるようにする.
   */
  public static void runSafe(Runnable handler) {
    if (handler == null) {
      return;
    }
    if (Platform.isFxApplicationThread()) {
      handler.run();
    } else {
      Platform.runLater(handler);
    }
  }

  /**
   * 現在のスレッドが UI スレッドの場合, {@code handler} を実行してからコントロールを返す.
   * そうでない場合, UI スレッドで {@code handler} を実行し, 終了してからコントロールを返す.
   */
  public static void runSafeSync(Runnable handler) {
    if (handler == null) {
      return;
    }
    if (Platform.isFxApplicationThread()) {
      handler.run();
    } else {
      CountDownLatch latch = new CountDownLatch(1);
      Platform.runLater(() -> {
        handler.run();
        latch.countDown();
      });
      try {
        latch.await();
      } catch (InterruptedException ignored) { /*Do nothing*/ }
    }
  }

  /**
   * {@code comboBox} のサイズをコンテンツに応じて自動的に変更するようにする.
   *
   * @param comboBox サイズの自動変更を有効にするコンボボックス
   * @param fnConvertToStr コンボボックスのアイテムを引数にとり, それが表示されたときの文字列を返す関数オブジェクト
   */
  public static <T> void enableAutoResize(
      ComboBox<? extends T> comboBox,
      Function<? super T, ? extends String> fnConvertToStr) {
    comboBox.setOnShowing(event -> fitComboBoxWidthToListWidth(comboBox, fnConvertToStr));
    comboBox.setOnHidden(event -> fitComboBoxWidthToContentWidth(comboBox));
    comboBox.getButtonCell().itemProperty().addListener((obs, oldWs, newWs) -> {
      if (newWs != null) {
        fitComboBoxWidthToContentWidth(comboBox);
      }
    });    
  }

  /**
   * コンボボックスの幅を表示されているリストの幅に合わせる.
   *
   * @param comboBox 幅を操作するコンボボックス
   * @param fnConvertToStr コンボボックスのアイテムを引数にとり, それが表示されたときの文字列を返す関数オブジェクト
   */
  private static <T> void fitComboBoxWidthToListWidth(
      ComboBox<? extends T> comboBox,
      Function<? super T, ? extends String> fnConvertToStr) {
    ListCell<? extends T> buttonCell = comboBox.getButtonCell();
    Font font = buttonCell.fontProperty().get();
    double maxWidth = comboBox.getItems().stream()
        .map(fnConvertToStr::apply)
        .mapToDouble(str -> ViewUtil.calcStrWidth(str, font))
        .max().orElse(0);
    
    ScrollBar scrollBar = getVerticalScrollbarOf(buttonCell.getListView());
    if (scrollBar != null) {
      maxWidth += scrollBar.getWidth();
    }
    maxWidth += buttonCell.getInsets().getLeft() + buttonCell.getInsets().getRight();
    maxWidth += buttonCell.getPadding().getLeft() + buttonCell.getPadding().getRight();
    buttonCell.getListView().setPrefWidth(maxWidth);
    buttonCell.setMinWidth(maxWidth);
  }

  /**
   * コンボボックスの幅を表示されている文字の幅に合わせる.
   *
   * @param comboBox 幅を操作するコンボボックス
   */
  private static void fitComboBoxWidthToContentWidth(ComboBox<?> comboBox) {
    ListCell<?> buttonCell = comboBox.getButtonCell();
    double width = ViewUtil.calcStrWidth(buttonCell.getText(), buttonCell.getFont());
    width += buttonCell.getInsets().getLeft() + buttonCell.getInsets().getRight();
    width += buttonCell.getPadding().getLeft() + buttonCell.getPadding().getRight();
    buttonCell.getListView().setPrefWidth(width);
    buttonCell.setMinWidth(width);
  }

  /** コンボボックスの垂直スクロールバーを取得する. */
  private static ScrollBar getVerticalScrollbarOf(Node node) {
    ScrollBar result = null;
    for (Node content : node.lookupAll(".scroll-bar")) {
      if (content instanceof ScrollBar bar) {
        if (bar.getOrientation().equals(Orientation.VERTICAL)) {
          result = bar;
        }
      }
    }
    return result;
  }

  /**
   * {@code view} をワークスペースビュー中央に表示して影をつける.
   *
   * @param view ワークスペースビュー中央に表示するノードビュー
   * @param hideShadow {@code view} が属するワークスペースのノードビューの全ての影を消す場合 true
   * @param target 影をつける対象. null を指定すると影をつけない.
   */
  public static void jump(BhNodeView view, boolean hideShadow, EffectTarget target) {
    runSafe(() -> jumpImpl(view, hideShadow, target));
  }
  /**
   * {@code view} をワークスペースビュー中央に表示して影をつける.
   *
   * @param view ワークスペースビュー中央に表示するノードビュー
   * @param hideShadow {@code view} が属するワークスペースのノードビューの全ての影を消す場合 true
   * @param target 影をつける対象. null を指定すると影をつけない.
   */
  private static void jumpImpl(BhNodeView view, boolean hideShadow, EffectTarget target) {
    if (view == null) {
      throw new IllegalArgumentException();
    }
    WorkspaceView wsView = view.getWorkspaceView();
    if (wsView == null) {
      return;
    }

    wsView.lookAt(view);
    if (hideShadow) {
      wsView.getRootNodeViews().forEach(
          nodeView -> nodeView.getLookManager().hideShadow(EffectTarget.CHILDREN));
    }
    wsView.moveNodeViewToFront(view);
    if (target != null) {
      view.getLookManager().showShadow(EffectTarget.SELF);
    }
  }
}
