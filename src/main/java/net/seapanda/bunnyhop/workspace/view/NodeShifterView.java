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

package net.seapanda.bunnyhop.workspace.view;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import net.seapanda.bunnyhop.common.configuration.BhConstants;
import net.seapanda.bunnyhop.common.configuration.BhConstants.Ui;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.BhNodeView.RegionManager.BodyRange;
import net.seapanda.bunnyhop.ui.view.Rem;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.utility.math.Vec2D;

/**
 * ノードシフタ (複数のノードを同時に移動させるためのマニピュレータ) のビュー.
 *
 * @author K.Koike
 */
public class NodeShifterView extends Pane {

  /** ノードシフタが操作するノードビューとリンクのマップ.  */
  private final ObservableMap<BhNodeView, Line> viewToLink =
      FXCollections.observableMap(new HashMap<BhNodeView, Line>());
  @FXML private Pane shifterBase;
  @FXML private Circle shifterCircle;
  @FXML private Polygon shifterArrow;

  /**
   * コンストラクタ.
   *
   * @param filePath ノードシフタが定義してある fxml ファイルのパス
   */
  public NodeShifterView(Path filePath) throws ViewConstructionException {
    try {
      FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
      loader.setController(this);
      loader.setRoot(this);
      loader.load();
    } catch (IOException e) {
      throw new ViewConstructionException("Failed to initialize  " + getClass().getSimpleName());
    }

    setVisible(false);
    createShape();
    setPickOnBounds(false);
    shifterBase.setPickOnBounds(false);
    shifterArrow.setMouseTransparent(true);
    viewToLink.addListener((MapChangeListener<BhNodeView, Line>) (change -> {
      if (change.getMap().size() >= 2) {
        setVisible(true);
        toFront();
      } else {
        setVisible(false);
      }
    }));
  }

  /**
   * 新しくリンクを作ってリンクとノードシフタの位置の更新を行う.
   *
   * @param view 新しくマニピュレータとリンクするノードビュー.
   *             既にリンクがあるノードは, リンクとノードシフタの位置の更新だけ行う.
   */
  public void createLink(BhNodeView view) {
    if (view == null) {
      return;
    }
    if (!viewToLink.containsKey(view)) {
      var newLink = new Line(0.0, 0.0, 0.0, 0.0);
      newLink.getStyleClass().add(BhConstants.Css.Class.NODE_SHIFTER_LINK);
      newLink.setStrokeDashOffset(1.0);
      viewToLink.put(view, newLink);
      getChildren().add(newLink);
      shifterBase.toFront();
    }
    updateShifterAndAllLinkPositions();
  }

  /**
   * リンクを削除する.
   *
   * @param view ノードシフタとのリンクを消すノードビュー.
   *             マニピュレータとリンクしていないノードビューを指定した場合, 何もしない.
   */
  public void deleteLink(BhNodeView view) {

    Line link = viewToLink.remove(view);
    if (link != null) {
      getChildren().remove(link);
      updateShifterAndAllLinkPositions();
    }
  }

  /**
   * リンクの位置を更新する.
   *
   * @param view このノードビューと繋がるリンクの位置を更新する.
   *             マニピュレータとリンクしていないノードビューを指定した場合, 何もしない.
   */
  public void updateLinkPos(BhNodeView view) {
    Line link = viewToLink.get(view);
    if (link != null) {
      Point2D linkPos = calcLinkPosFor(view);
      Point2D newPos = parentToLocal(linkPos);
      link.setEndX(newPos.getX());
      link.setEndY(newPos.getY());
      updateLinkPosForShifter(link);
    }
  }

  /** シフタと全リンクの位置を更新する. */
  public void updateShifterAndAllLinkPositions() {
    if (viewToLink.isEmpty()) {
      return;
    }
    //ノードシフタの新しい位置を計算する
    double shifterX = 0.0;
    double shifterY = 0.0;
    for (BhNodeView view : viewToLink.keySet()) {
      Point2D linkPos = calcLinkPosFor(view);
      shifterX += linkPos.getX();
      shifterY += linkPos.getY();
    }
    shifterX = shifterX / viewToLink.size() - shifterCircle.getRadius();
    shifterY = shifterY / viewToLink.size() - shifterCircle.getRadius();
    setPosOnWorkspace(shifterX, shifterY);

    for (BhNodeView view : viewToLink.keySet()) {
      updateLinkPos(view);
    }
  }

  /**
   * ノードビュー側のリンクの端点位置を計算する.
   *
   * @param view このノードビューに繋がるリンクの端点位置を計算する
   * @return リンクの端点位置
   */
  private Point2D calcLinkPosFor(BhNodeView view) {
    final double yOffset = 0.5 * Rem.VAL;
    BodyRange bodyRange = view.getRegionManager().getBodyRange();
    double linkPosX = (bodyRange.upperLeft().x + bodyRange.lowerRight().x) / 2;
    double linkPosY = bodyRange.upperLeft().y + yOffset;
    return new Point2D(linkPosX, linkPosY);
  }


  /**
   * シフタ側のリンクの端点を更新する.
   *
   * @param link 端点を更新するリンク
   */
  private void updateLinkPosForShifter(Line link) {

    double x = link.getEndX();
    double y = link.getEndY();
    if (0.0 <= x && x < 1.0) {
      x = 1.0;
    } else if (-1.0 < x && x < 0.0) {
      x = -1.0;
    }
    if (0.0 <= y && y < 1.0) {
      y = 1.0;
    } else if (-1.0 < y && y < 0.0) {
      y = -1.0;
    }
    double len = fastSqrt(x * x + y * y);
    double cosVal = x / len;
    double sinVal = y / len;
    double r = shifterCircle.getRadius();
    link.setStartX(r * cosVal);
    link.setStartY(r * sinVal);
  }

  /** 高速平方根計算. */
  private static double fastSqrt(double x) {
    double half = 0.5 * x;
    long lnum = 0x5FE6EB50C7B537AAL - (Double.doubleToLongBits(x) >> 1);
    double dnum = Double.longBitsToDouble(lnum);
    dnum *= 1.5 - half * dnum * dnum;
    return dnum * x;
  }

  /**
   * 引数のノードビューがノードシフタとリンクしているかどうか調べる.
   *
   * @param view ノードシフタとリンクしているか調べるノードビュー
   * @return {@code view} がノードシフタとリンクしている場合 true.
   */
  public boolean isLinkedWith(BhNodeView view) {
    return viewToLink.containsKey(view);
  }

  /**
   * ノードシフタを移動する.
   *
   * @param diff 移動量
   * @param wsSize ノードシフタがあるワークスペースのサイズ
   * @param moveLink リンクも動かす場合 true
   * @return 実際に移動した量
   */
  public Vec2D move(Vec2D diff, Vec2D wsSize, boolean moveLink) {
    Vec2D distance = ViewUtil.distance(diff, wsSize, getPosOnWorkspace());
    setPosOnWorkspace(getTranslateX() + distance.x, getTranslateY() + distance.y);
    if (moveLink) {
      for (BhNodeView view : viewToLink.keySet()) {
        updateLinkPos(view);
      }
    }
    return distance;
  }

  /** ノードシフタのワークスペース上での位置を取得する. */
  public Vec2D getPosOnWorkspace() {
    return ViewUtil.getPosOnWorkspace(this);
  }

  /** ノードシフタのワークスペース上での位置を設定する. */
  public void setPosOnWorkspace(double x, double y) {
    setTranslateX(x);
    setTranslateY(y);
  }

  /**
   * 現在リンクしているノードビューのリストを取得する.
   *
   * @return 現在リンクしているノードビューのリスト
   */
  public List<BhNodeView> getLinkedNodes() {
    return new ArrayList<>(viewToLink.keySet());
  }

  /** ノードシフタの形を作る. */
  private void createShape() {
    var radius = Ui.NODE_SHIFTER_SIZE / 2.0;
    shifterCircle.setRadius(radius);
    shifterCircle.setCenterX(0);
    shifterCircle.setCenterY(0);

    double l = 0.3;
    double k = 0.42;
    shifterArrow.getPoints().addAll(
              0.5 * Ui.NODE_SHIFTER_SIZE - radius,  0.0 - radius,
        (1.0 - l) * Ui.NODE_SHIFTER_SIZE - radius, (0.5 - l) * Ui.NODE_SHIFTER_SIZE - radius,
        (1.0 - k) * Ui.NODE_SHIFTER_SIZE - radius, (0.5 - l) * Ui.NODE_SHIFTER_SIZE - radius,
        (1.0 - k) * Ui.NODE_SHIFTER_SIZE - radius,         k * Ui.NODE_SHIFTER_SIZE - radius,
        (0.5 + l) * Ui.NODE_SHIFTER_SIZE - radius,         k * Ui.NODE_SHIFTER_SIZE - radius,
        (0.5 + l) * Ui.NODE_SHIFTER_SIZE - radius,         l * Ui.NODE_SHIFTER_SIZE - radius,
                    Ui.NODE_SHIFTER_SIZE - radius,       0.5 * Ui.NODE_SHIFTER_SIZE - radius,
        (0.5 + l) * Ui.NODE_SHIFTER_SIZE - radius, (1.0 - l) * Ui.NODE_SHIFTER_SIZE - radius,
        (0.5 + l) * Ui.NODE_SHIFTER_SIZE - radius, (1.0 - k) * Ui.NODE_SHIFTER_SIZE - radius,
        (1.0 - k) * Ui.NODE_SHIFTER_SIZE - radius, (1.0 - k) * Ui.NODE_SHIFTER_SIZE - radius,
        (1.0 - k) * Ui.NODE_SHIFTER_SIZE - radius, (0.5 + l) * Ui.NODE_SHIFTER_SIZE - radius,
        (1.0 - l) * Ui.NODE_SHIFTER_SIZE - radius, (0.5 + l) * Ui.NODE_SHIFTER_SIZE - radius,
              0.5 * Ui.NODE_SHIFTER_SIZE - radius,             Ui.NODE_SHIFTER_SIZE - radius,
                l * Ui.NODE_SHIFTER_SIZE - radius, (0.5 + l) * Ui.NODE_SHIFTER_SIZE - radius,
                k * Ui.NODE_SHIFTER_SIZE - radius, (0.5 + l) * Ui.NODE_SHIFTER_SIZE - radius,
                k * Ui.NODE_SHIFTER_SIZE - radius, (1.0 - k) * Ui.NODE_SHIFTER_SIZE - radius,
        (0.5 - l) * Ui.NODE_SHIFTER_SIZE - radius, (1.0 - k) * Ui.NODE_SHIFTER_SIZE - radius,
        (0.5 - l) * Ui.NODE_SHIFTER_SIZE - radius, (1.0 - l) * Ui.NODE_SHIFTER_SIZE - radius,
                                      0.0 - radius,       0.5 * Ui.NODE_SHIFTER_SIZE - radius,
        (0.5 - l) * Ui.NODE_SHIFTER_SIZE - radius,         l * Ui.NODE_SHIFTER_SIZE - radius,
        (0.5 - l) * Ui.NODE_SHIFTER_SIZE - radius,         k * Ui.NODE_SHIFTER_SIZE - radius,
                k * Ui.NODE_SHIFTER_SIZE - radius,         k * Ui.NODE_SHIFTER_SIZE - radius,
                k * Ui.NODE_SHIFTER_SIZE - radius, (0.5 - l) * Ui.NODE_SHIFTER_SIZE - radius,
                l * Ui.NODE_SHIFTER_SIZE - radius, (0.5 - l) * Ui.NODE_SHIFTER_SIZE - radius);
  }

  /**
   * CSSの擬似クラスの有効無効を切り替える.
   *
   * @param activate 擬似クラスを有効にする場合true
   * @param pseudoClassName 有効/無効を切り替える擬似クラス名
   */
  public void setPseudoClassState(boolean activate, String pseudoClassName) {
    if (activate) {
      shifterBase.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), true);
      shifterCircle.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), true);
      shifterArrow.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), true);
      viewToLink.values().forEach(
          link -> link.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), true));
    } else {
      shifterBase.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), false);
      shifterCircle.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), false);
      shifterArrow.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), false);
      viewToLink.values().forEach(
          link -> link.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), false));
    }
  }
}
