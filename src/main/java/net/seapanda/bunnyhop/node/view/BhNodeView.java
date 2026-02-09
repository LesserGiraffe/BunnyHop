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

package net.seapanda.bunnyhop.node.view;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import net.seapanda.bunnyhop.node.control.BhNodeController;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectType;
import net.seapanda.bunnyhop.node.view.style.ConnectorPos;
import net.seapanda.bunnyhop.node.view.traverse.NodeViewComponent;
import net.seapanda.bunnyhop.utility.event.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.workspace.view.WorkspaceView;
import net.seapanda.bunnyhop.workspace.view.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.workspace.view.quadtree.QuadTreeRectangle.OverlapOption;

/**
 * {@link BhNode} に対応するビュークラスのインタフェース.
 *
 * @author K.Koike
 */
public interface BhNodeView extends NodeViewComponent {
  
  /**
   * このノードビューのモデルを取得する.
   *
   * @return このノードビューのモデル
   */
  Optional<? extends BhNode> getModel();
  
  /** このノードビューのコントローラを取得する. */
  Optional<BhNodeController> getController();

  /**
   * このノードビューのコントローラを設定する.
   *
   * @param controller 設定するコントローラ
   */
  void setController(BhNodeController controller);

  /** このノードビューの領域に関する処理を行うオブジェクト返す. */
  RegionManager getRegionManager();

  /** このノードビューの GUI ツリーに関する処理を行うオブジェクト返す. */
  TreeManager getTreeManager();

  /** このノードビューの位置に関する処理を行うオブジェクト返す. */
  PositionManager getPositionManager();

  /** このノードビューに対するイベントハンドラの登録 / 削除機能を提供するオブジェクトを返す. */
  CallbackRegistry getCallbackRegistry();

  /** このノードビューの外観を変更する処理を行うオブジェクトを返す. */
  LookManager getLookManager();

  /**
   * このノードビューがマウスイベントを受け付けるかどうかを設定する.
   *
   * @param value true の場合, このノードビューがマウスイベントを受けなくなる.
   *              false の場合, このノードビューがマウスイベントを受けるようになる.
   */
  void setMouseTransparent(boolean value);

  /** このノードビューに対応する BhNode が固定ノードであるか調べる.. */
  boolean isFixed();

  /** このノードビューがテンプレートノードビュー (ノード選択用のノードビュー) かどうか調べる. */
  boolean isTemplate();

  /**
   * このノードビューが属している {@link WorkspaceView} を取得する.
   * 見つからない場合は null.
   *
   * @return このノードビューが属している {@link WorkspaceView}
   */
  WorkspaceView getWorkspaceView();

  /** ノードビューの外観を変更する機能を規定したインタフェース. */
  interface LookManager {

    /** 関連するノードビュー以下のノードビューのワークスペース上の位置, 四分木空間上の位置, および配置を更新する. */
    void arrange();

    /**
     * 関連するノードビュー以下のノードビューのワークスペース上の位置, 四分木空間上の位置, および配置の更新を
     * UI スレッドの処理としてキューイングする.
     *
     * <p>
     * 大きなノードツリーをワークスペースに初めて配置したときに, 複数の子要素がサイズの更新を要求する.
     * それらの更新要求を全てまとめて処理するために本メソッドを用意した.
     * </p>
     */
    void requestArrangement();

    /** 関連するノードビュー以下の可視性を変更する. */
    void setVisible(boolean visible);

    /**
     * 関連するノードビューのコネクタの位置を取得する.
     *
     * @return 関連するノードビューのコネクタの位置
     */
    ConnectorPos getConnectorPos();

    /**
     * 関連するノードビューの視覚効果の有効 / 無効を切り替える.
     *
     * @param enable 視覚効果を有効にする場合 true
     * @param type 有効または無効にする視覚効果の種類
     */
    void setEffectEnabled(boolean enable, VisualEffectType type);

    /**
     * 現在適用されている視覚効果の一覧を取得する.
     *
     * @return 現在適用されている視覚効果の一覧
     */
    Set<VisualEffectType> getAppliedEffects();

    /**
     * 関連するノードビューに {@code effect} で指定した視覚効果が適用されているかどうか調べる.
     *
     * @return 視覚効果が適用されている場合 true
     */
    boolean isEffectEnabled(VisualEffectType effect);
  }

  /** ノードビューの領域に関する操作を規定したインタフェース. */
  interface RegionManager {

    /**
     * コネクタ部分が 関連するノードビューのコネクタ部分に重なっているノードビューを探す.
     *
     * @return コネクタ部分が 関連するノードビューのコネクタ部分に重なっているノードビューのリスト
     */
    List<BhNodeView> searchForOverlapped();

    /**
     * 関連するノードビューのボディの領域を保持する {@link QuadTreeRectangle} と
     *  コネクタの領域を保持する {@link QuadTreeRectangle} を返す.
     *
     * @return 関連するノードビューのボディの領域を保持する {@link QuadTreeRectangle} と
     *         コネクタの領域を保持する {@link QuadTreeRectangle}
     */
    Rectangles getRegions();

    /** 関連するノードビューをそれが現在所属している 4 分木空間から消す. */
    void removeQuadTreeRect();

    /**
     * 関連するノードビューに末尾までの全外部ノードビューを加えた部分の大きさを返す.
     *
     * @param includeCnctr コネクタ部分を含む大きさを返す場合 true.
     * @return 関連するノードビューに末尾までの全外部ノードビューを加えた部分の大きさ
     */
    Vec2D getNodeTreeSize(boolean includeCnctr);

    /**
     * 関連するノードビューの大きさを返す.
     *
     * @param includeCnctr コネクタ部分を含む大きさを返す場合 true
     * @return ノードビューの大きさ
     */
    Vec2D getNodeSize(boolean includeCnctr);

    /**
     * 関連するノードビューのコネクタの大きさを返す.
     *
     * @return コネクタの大きさ
     */
    Vec2D getConnectorSize();

    /**
     * 関連するノードビューの切り欠きの大きさを返す.
     *
     * @return 切り欠きの大きさ
     */
    Vec2D getNotchSize();

    /** 関連するノードビューのボディ部分のワークスペース上での範囲を取得する. */
    BodyRange getBodyRange();

    /** 関連するノードビューのコネクタ部分のワークスペース上での範囲を取得する. */
    BodyRange getConnectorRange();

    /** 関連するノードビューのコネクタ部分の左上を原点としたときのボディ部分の左上の位置を取得する. */
    Vec2D getBodyPosFromConnector();

    /**
     * 関連するノードビューのボディの領域が引数のノードビューのボディの領域と重なっているかどうか調べる.
     *
     * @param view 関連するノードビューとのボディ部分の重なりを調べるノード
     * @param option 重なり具合を判定するオプション
     * @return 関連するノードビューのボディの領域が引数のノードビューのボディと重なっている場合 true.
     */
    boolean overlapsWith(BhNodeView view, OverlapOption option);

    /**
     * ノードの共通部分のサイズを取得する.
     *
     * @return ノードの共通部分のサイズ
     */
    Vec2D getCommonPartSize();

    /**
     * ノードビューのボディとコネクタ部分の領域に対応する {@link QuadTreeRectangle} をまとめたレコード.
     *
     * @param body ボディ部分の矩形領域に対応する {@link QuadTreeRectangle} オブジェクト
     * @param cnctr コネクタ部分の矩形領域に対応する {@link QuadTreeRectangle} オブジェクト
     */
    record Rectangles(QuadTreeRectangle body, QuadTreeRectangle cnctr) { }

    /**
     * ノードビューのボディ部分の矩形領域.
     *
     * @param upperLeft 矩形領域の左上のワークスペース上での位置
     * @param lowerRight 矩形領域の右下のワークスペース上での位置
     */
    record BodyRange(Vec2D upperLeft, Vec2D lowerRight) { }
  }

  /** ノードビューの GUI ツリーに関する操作を規定したインタフェース. */
  interface TreeManager {

    /**
     * 関連するノードビューの親グループを取得する.
     *
     * @return 関連するノードビューの親グループ. 存在しない場合は null.
     */
    BhNodeViewGroup getParentGroup();

    /**
     * 関連するノードビューの親ノードビューを取得する.
     *
     * @return 関連するノードビューの親となるノードビュー.  関連するノードビューがルートノードビューの場合は null.
     */
    ConnectiveNodeView getParentView();

    /**
     * {@link BhNodeViewGroup} の中で 関連するノードビューを引数のノードビューと入れ替える.
     * GUI ツリーからは取り除かない.
     *
     * @param newNode 関連するノードビューと入れ替えるノードビュー.
     */
    void replace(BhNodeView newNode);

    /**
     * 関連するノードビュー以下のノードビューを GUI ツリーから取り除く.
     * {@link BhNodeViewGroup} からは取り除かない.
     */
    void removeFromGuiTree();

    /**
     * 関連するノードビューを {@code parent} に子要素として追加する.
     *
     * @param parent 親となる GUI コンポーネント.
     */
    void addToGuiTree(Group parent);

    /**
     * 関連するノードビューを {@code parent} に子要素として追加する.
     *
     * @param parent 親となる GUI コンポーネント.
     */
    void addToGuiTree(Pane parent);

    /** 関連するノードビューのルートノードビューを返す. */
    BhNodeView getRootView();

    /**
     * 関連するノードビューが外部ノードかどうか調べる.
     *
     * @return 関連するノードビューがルートノードビューの場合 true
     */
    boolean isRoot();

    /**
     * 関連するノードビューが外部ノードかどうか調べる.
     *
     * <p>外部ノードとは他のノードの外部に描画されるノードのことである.
     *
     * @return 関連するノードビューが外部ノードである場合 true
     */
    boolean isOuter();

    /**
     * 関連するノードビューを保持する GUI コンポーネントを取得する.
     *
     * @return 関連するノードビュー保持する GUI コンポーネント.
     */
    Parent getParentGuiComponent();
  }

  /** ノードビューの位置の変更, 取得に関する操作を規定したインタフェース. */
  interface PositionManager {

    /**
     * 関連するノードビューのワークスペース上での位置を返す.
     *
     * @return 関連するノードビューのワークスペース上での位置
     */
    Vec2D getPosOnWorkspace();

    /**
     * 関連するノードビューのボディ部分のワークスペース上での範囲を返す.
     *
     * @return 関連するノードビューのボディ部分のワークスペース上での範囲
     */
    Bounds getBounds();

    /**
     * 関連するノードビュー以下のノードビューのワークスペースの上での位置と 4 分木空間上での位置を更新する.
     *
     * @param posX 本体部分左上のワークスペース上での X 位置
     * @param posY 本体部分左上のワークスペース上での Y 位置
     */
    void setTreePosOnWorkspace(double posX, double posY);

    /**
     * 関連するノードビューのコネクタも含んだ範囲の左上の位置を基準として,
     * ワークスペースの上での位置と 4 分木空間上での位置を更新する.
     *
     * @param posX コネクタ左上のワークスペース上での X 位置
     * @param posY コネクタ左上のワークスペース上での Y 位置
     */
    void setTreePosOnWorkspaceByUpperLeft(double posX, double posY);

    /**
     * 関連するノードビュー以下のノードビューの Z 位置を設定する.
     *
     * @param pos 関連するノードビューの Z 位置
     */
    void setTreeZpos(double pos);

    /**
     * 関連するノードビューの Z 位置を取得する.
     *
     * @return 関連するノードビューの Z 位置
     */
    double getZpos();

    /**
     * 関連するノードビュー 以下のノードビューをワークスペースビューからはみ出さないように動かす.
     *
     * @param diffX X 方向移動量
     * @param diffY Y 方向移動量
     */
    void move(double diffX, double diffY);

    /**
     * 関連するノードビュー 以下のノードビューをワークスペースビューからはみ出さないように動かす.
     *
     * @param diff 移動量
     */
    void move(Vec2D diff);

    /**
     * シーンの座標空間の位置 {@code pos} を 関連するノードビューのローカル座標空間の位置に変換する.
     *
     * @param pos シーンの座標空間の位置
     * @return {@code pos} のローカル座標空間における位置.
     */
    Vec2D sceneToLocal(Vec2D pos);

    /**
     * 関連するノードビューのローカル座標空間の位置 {@code pos} をシーンの座標空間の位置に変換する.
     *
     * @param pos 関連するノードビューのローカル座標空間の位置
     * @return {@code pos} のシーンの座標空間の位置
     */
    Vec2D localToScene(Vec2D pos);
  }

  /** {@link BhNodeView} に対してイベントハンドラを追加または削除する機能を規定したインタフェース. */
  interface CallbackRegistry {

    /** 関連するノードビュー上でマウスボタンが押下されたときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<MouseEventInfo>.Registry getOnMousePressed();

    /** 関連するノードビューがドラッグされたときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<MouseEventInfo>.Registry getOnMouseDragged();

    /** 関連するノードビュー上でマウスのドラッグが検出されたときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<MouseEventInfo>.Registry getOnMouseDragDetected();

    /** 関連するノードビュー上でマウスボタンが離されたときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<MouseEventInfo>.Registry getOnMouseReleased();

    /** 関連するノードビューの位置が変わったときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<MoveEvent>.Registry getOnMoved();

    /** 関連するノードビューのサイズが変わったときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<SizeChangedEvent>.Registry getOnSizeChanged();

    /** 関連するノードビューの GUI ツリー上の親要素が変わったときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<ParentViewChangedEvent>.Registry getOnParentViewChanged();

    /** 関連するノードビューの親 {@link BhNodeViewGroup} が変わったときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<ParentGroupChangedEvent>.Registry getOnParentGroupChanged();

    /**
     * 関連するノードビューにイベントを伝える.
     *
     * @param event 伝えるイベント
     */
    void dispatch(Event event);

    /**
     * イベントを関連するノードビューに転送する.
     *
     * @param event 転送するイベント
     */
    void forward(MouseEventInfo event);
  }

  /**
   * ノードビューがマウスで操作されたときの情報を格納したレコード.
   */
  class MouseEventInfo {

    /** マウスで操作されたノードビュー. */
    public final BhNodeView view;
    /** マウス操作の情報を格納したオブジェクト. */
    public final MouseEvent event;
    /**
     * このイベントが {@link CallbackRegistry#forward} により発生したものであった場合,
     * 元となったイベントが格納される.
     * 元となったイベントが存在しない場合は null.
     */
    public final MouseEventInfo src;
    private Object userData;

    /** コンストラクタ. */
    public MouseEventInfo(BhNodeView view, MouseEvent event, MouseEventInfo src) {
      this.view = view;
      this.event = event;
      this.src = src;
    }

    /**
     * このオブジェクトのマウスイベントを発生させた
     * 大元のマウスイベントに対応する {@link MouseEventInfo} を取得する.
     */
    public MouseEventInfo getRootEventInfo() {
      MouseEventInfo info = this;
      while (info.src != null) {
        info = info.src;
      }
      return info;
    }

    /** このオブジェクトに対しユーザデータを設定する. */
    public void setUserData(Object userData) {
      this.userData = userData;
    }

    /** {@link #setUserData} で設定したデータを取得する. */
    public Object getUserData() {
      return userData;
    }
  }

  /**
   * ノードビューの位置が変更されたときの情報を格納したレコード.
   *
   * @param view 位置が変更されたノードビュー
   */
  record MoveEvent(BhNodeView view) {}

  /**
   * ノードビューのサイズが変更されたときの情報を格納したレコード.
   *
   * @param view サイズが変更されたノードビュー
   */
  record SizeChangedEvent(BhNodeView view) {}

  /**
   * ノードビューの GUI ツリー上の親要素が変わったときの情報を格納したレコード.
   *
   * @param view 親要素が変わったノードビュー
   * @param oldParent 変更前の親要素.  存在しない場合 null.
   * @param newParent 変更後の親要素.  存在しない場合 null.
   */
  record ParentViewChangedEvent(BhNodeView view, Parent oldParent, Parent newParent) {}

  /**
   * 親となる {@link BhNodeViewGroup} が変わったときの情報を格納したレコード.
   *
   * @param view 親要素が変わったノードビュー
   * @param oldParent 変更前の親グループ.  存在しない場合 null.
   * @param newParent 変更後の親グループ.  存在しない場合 null.
   */
  record ParentGroupChangedEvent(
      BhNodeView view, BhNodeViewGroup oldParent, BhNodeViewGroup newParent) {}
}
