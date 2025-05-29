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

package net.seapanda.bunnyhop.view.node;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import net.seapanda.bunnyhop.control.node.BhNodeController;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle;
import net.seapanda.bunnyhop.quadtree.QuadTreeRectangle.OverlapOption;
import net.seapanda.bunnyhop.utility.math.Vec2D;
import net.seapanda.bunnyhop.view.node.style.BhNodeViewStyle.ConnectorPos;
import net.seapanda.bunnyhop.view.traverse.NodeViewComponent;
import net.seapanda.bunnyhop.view.workspace.WorkspaceView;

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

  /** このノードビューにイベントハンドラ登録する機能を提供するオブジェクトを返す. */
  EventManager getEventManager();

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

  /** このノードビューがテンプレート (ノード選択画面のノード) かどうか調べる. */
  boolean isTemplate();

  /**
   * このノードビューが属している {@link WorkspaceView} を取得する.
   * 見つからない場合は null.
   *
   * @return このノードビューが属している {@link WorkspaceView}
   */
  WorkspaceView getWorkspaceView();

  /**
   * ノードビューの外観を変更する機能を規定したインタフェース.
   *
   * <p>
   * このオブジェクトが管理する {@link BhNodeView} を "target" と呼ぶ
   * </p>
   */
  public interface LookManager {

    /**
     * "target" の CSS の擬似クラスの有効無効を切り替える.
     *
     * @param className 有効/無効を切り替える擬似クラス名
     * @param enable 擬似クラスを有効にする場合true
     */
    void switchPseudoClassState(String className, boolean enable);

    /** "target" 以下のノードビューのワークスペース上の位置, 四分木空間上の位置, および配置を更新する. */
    void arrange();

    /**
     * "target" 以下のノードビューのワークスペース上の位置, 四分木空間上の位置, および配置の更新を
     * UI スレッドの処理としてキューイングする.
     *
     * <p>
     * 大きなノードツリーをワークスペースに初めて配置したときに, 複数の子要素がサイズの更新を要求する.
     * それらの更新要求を全てまとめて処理するために本メソッドを用意した.
     * </p>
     */
    void requestArrangement();
    
    /** "target" 以下の可視性を変更する. */
    void setVisible(boolean visible);

    /**
     * "target" のコンパイルエラー表示の可視性を切り替える.
     *
     * @param visible コンパイルエラー表示を有効にする場合 true. 無効にする場合 false.
     */
    void setCompileErrorVisibility(boolean visible);

    /**
     * "target" のコンパイルエラー表示の状態を調べる.
     *
     * @return コンパイルエラー表示されている場合 true.
     */
    boolean isCompileErrorVisible();

    /**
     * "target" とそれから辿れる外部ノードビューに影を付ける.
     *
     * @param onlyOuter 外部ノードビューのみを辿って影を付ける場合 true.
     *                  内部ノードビューと外部ノードビューを辿って影を付ける場合 false.
     */
    void showShadow(boolean onlyOuter);

    /**
     * "target" とそれから辿れるノードビューの影を消す.
     *
     * @param onlyOuter 外部ノードビューのみを辿って影を消す場合 true.
     *                  内部ノードビューと外部ノードビューを辿って影を消す場合 false.
     */
    void hideShadow(boolean onlyOuter);

    /**
     * "target" のコネクタの位置を取得する.
     *
     * @return "target" のコネクタの位置
     */
    ConnectorPos getConnectorPos();
  }

  /**
   * ノードビューの領域に関する操作を規定したインタフェース.
   *
   * <p>
   * このオブジェクトが管理する {@link BhNodeView} を "target" と呼ぶ
   * </p>
   */
  public interface RegionManager {

    /**
     * コネクタ部分が "target" のコネクタ部分に重なっているノードビューを探す.
     *
     * @return コネクタ部分が "target" のコネクタ部分に重なっているノードビューのリスト
     */
    List<BhNodeView> searchForOverlapped();
    
    /**
     * "target" のボディの領域を保持する {@link QuadTreeRectangle} と
     *  コネクタの領域を保持する {@link QuadTreeRectangle} を返す.
     *
     * @return "target" のボディの領域を保持する {@link QuadTreeRectangle} と
     *         コネクタの領域を保持する {@link QuadTreeRectangle}
     */
    Rectangles getRegions();

    /** "target" をそれが現在所属している 4 分木空間から消す. */
    void removeQuadTreeRect();

    /**
     * "target" に末尾までの全外部ノードビューを加えた大きさを返す.
     *
     * @param includeCnctr コネクタ部分を含む大きさを返す場合 true.
     * @return ノードビューの大きさ
     */
    Vec2D getNodeTreeSize(boolean includeCnctr);

    /**
     * "target" の大きさを返す.
     *
     * @param includeCnctr コネクタ部分を含む大きさを返す場合 true
     * @return ノードビューの大きさ
     */
    Vec2D getNodeSize(boolean includeCnctr);

    /**
     * "target" のコネクタの大きさを返す.
     *
     * @return コネクタの大きさ
     */
    Vec2D getConnectorSize();

    /** "target" のボディ部分のワークスペース上での範囲を取得する. */
    BodyRange getBodyRange();

    /** "target" のコネクタ部分のワークスペース上での範囲を取得する. */
    BodyRange getConnectorRange();

    /**
     * "target" のボディの領域が引数のノードビューのボディの領域と重なっているかどうか調べる.
     *
     * @param view "target" とのボディ部分の重なりを調べるノード
     * @param option 重なり具合を判定するオプション
     * @return このノードビューのボディの領域が引数のノードビューのボディと重なっている場合 true.
     * */
    boolean overlapsWith(BhNodeView view, OverlapOption option);

    /**
     * ノードビューのボディとコネクタ部分の領域に対応する {@link QuadTreeRectangle} をまとめたレコード.
     *
     * @param bodyId ボディ部分の矩形領域に対応する {@link QuadTreeRectangle} オブジェクト
     * @param cnctr コネクタ部分の矩形領域に対応する {@link QuadTreeRectangle} オブジェクト
     */
    public record Rectangles(QuadTreeRectangle body, QuadTreeRectangle cnctr) { }

    /**
     * ノードビューのボディ部分の矩形領域.
     *
     * @param upperLeft 矩形領域の左上のワークスペース上での位置
     * @param lowerRight 矩形領域の右下のワークスペース上での位置
     */
    public record BodyRange(Vec2D upperLeft, Vec2D lowerRight) { }
  }

  /**
   * ノードビューの GUI ツリーに関する操作を規定したインタフェース.
   *
   * <p>
   * このオブジェクトが管理する {@link BhNodeView} を "target" と呼ぶ
   * </p>
   */
  public interface TreeManager {

    /**
     * "target" の親グループを取得する.
     *
     * @return "target" の親グループ. 存在しない場合は null.
     */
    BhNodeViewGroup getParentGroup();

    /**
     * "target" の親ノードビューを取得する.
     *
     * @return "target" の親となるノードビュー.  "target" がルートノードビューの場合は null.
     */
    ConnectiveNodeView getParentView();

    /**
     * {@link BhNodeViewGroup} の中で "target" を引数のノードビューと入れ替える.
     * GUI ツリーからは取り除かない.
     *
     * @param newNode "target" と入れ替えるノードビュー.
     */
    void replace(BhNodeView newNode);

    /**
     * "target" 以下のノードビューを GUI ツリーから取り除く.
     * {@link BhNodeViewGroup} からは取り除かない.
     */
    void removeFromGuiTree();

    /**
     * "target" を {@code parent} に子要素として追加する.
     *
     * @param parent 親となる GUI コンポーネント.
     */
    void addToGuiTree(Group parent);

    /**
     * "target" を {@code parent} に子要素として追加する.
     *
     * @param parent 親となる GUI コンポーネント.
     */
    void addToGuiTree(Pane parent);

    /** "target" のルートノードビューを返す. */
    BhNodeView getRootView();

    /**
     * "target" がルートノードビューかどうか調べる.
     *
     * @return "target" がルートノードビューの場合 true
     */
    boolean isRootView();

    /**
     * "target" を保持する GUI コンポーネントを取得する.
     *
     * @return "target" 保持する GUI コンポーネント.
     */
    Parent getParentGuiComponent();
  }

  /**
   * ノードビューの位置の変更, 取得に関する操作を規定したインタフェース.
   *
   * <p>
   * このオブジェクトが管理する {@link BhNodeView} を "target" と呼ぶ
   * </p>
   */
  public interface PositionManager {

    /**
     * ワークスペース上での位置を返す.
     *
     * @return ワークスペース上での位置
     */
    Vec2D getPosOnWorkspace();

    /**
     * "target" 以下のノードビューのワークスペースの上での位置と 4 分木空間上での位置を更新する.
     *
     * @param posX 本体部分左上のワークスペース上での X 位置
     * @param posY 本体部分左上のワークスペース上での Y 位置
     */
    void setTreePosOnWorkspace(double posX, double posY);

    /**
     * "target" のコネクタの位置を指定して "target" 以下のノードビューのワークスペースの上での位置と 4 分木空間上での位置を更新する.
     *
     * @param posX コネクタ左上のワークスペース上での X 位置
     * @param posY コネクタ左上のワークスペース上での Y 位置
     */
    void setTreePosOnWorkspaceByConnector(double posX, double posY);

    /**
     * "target" 以下のノードビューの Z 位置を設定する.
     *
     * @param pos "target" の Z 位置
     */
    void setTreeZpos(double pos);

    /**
     * "target" の Z 位置を取得する.
     *
     * @return "target" の Z 位置
     */
    double getZpos();

    /**
     * "target"  以下のノードビューをワークスペースビューからはみ出さないように動かす.
     *
     * @param diffX X 方向移動量
     * @param diffY Y 方向移動量
     */
    void move(double diffX, double diffY);

    /**
     * "target"  以下のノードビューをワークスペースビューからはみ出さないように動かす.
     *
     * @param diff 移動量
     */
    void move(Vec2D diff);

    /**
     * シーンの座標空間の位置 {@code pos} を "target" のローカル座標空間の位置に変換する.
     *
     * @param pos シーンの座標空間の位置
     * @return {@code pos} のローカル座標空間における位置.
     */
    Vec2D sceneToLocal(Vec2D pos);

    /**
     * "target"  のローカル座標空間の位置 {@code pos} をシーンの座標空間の位置に変換する.
     *
     * @param pos "target" のローカル座標空間の位置
     * @return {@code pos} のシーンの座標空間の位置
     */
    Vec2D localToScene(Vec2D pos);
  }

  /**
   * ノードビューのイベントハンドラの登録および削除操作を規定したインタフェース.
   *
   * <p>
   * このオブジェクトが管理する {@link BhNodeView} を "target" と呼ぶ
   * </p>
   */
  public interface EventManager {

    /**
     * "target" 上でマウスを押下したときのイベントハンドラを登録する.
     *
     * @param handler 登録するイベントハンドラ
     */
    void setOnMousePressed(EventHandler<? super MouseEvent> handler);

    /**
     * "target" をドラッグしたときのイベントハンドラを登録する.
     *
     * @param handler 登録するイベントハンドラ
     */
    void setOnMouseDragged(EventHandler<? super MouseEvent> handler);

    /**
     * "target" に対するマウスドラッグを検出したときのイベントハンドラを登録する.
     *
     * @param handler 登録するイベントハンドラ
     */
    void setOnDragDetected(EventHandler<? super MouseEvent> handler);

    /**
     * "target" 上でマウスボタンを離したときのイベントハンドラを登録する.
     *
     * @param handler 登録するイベントハンドラ
     */
    void setOnMouseReleased(EventHandler<? super MouseEvent> handler);

    /**
     * "target" にイベントフィルタを登録する.
     *
     * @param type イベントフィルタが受け取るイベントの種類
     * @param handler 登録するイベントフィルタ
     */
    <T extends Event> void addEventFilter(EventType<T> type, EventHandler<? super T> handler);

    /**
     * "target" のイベントフィルタを削除する.
     *
     * @param type イベントフィルタを取り除くイベントの種類
     * @param handler 削除するイベントフィルタ
     */
    <T extends Event> void removeEventFilter(EventType<T> type, EventHandler<? super T> handler);

    /**
     * "target" にイベントを伝える.
     *
     * @param event "target" に伝えるイベント
     */
    void dispatch(Event event);

    /**
     * "target" の位置が変わったときのイベントハンドラを追加する.
     * 登録したハンドラは, GUIスレッド上で実行される.
     *
     * @param handler 追加するイベントハンドラ
     */
    void addOnMoved(BiConsumer<? super BhNodeView, ? super Vec2D> handler);

    /**
     * "target" の位置が変わったときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    void removeOnMoved(BiConsumer<? super BhNodeView, ? super Vec2D> handler);

    /**
     * "target" をワークスペースビューに追加したときのイベントハンドラを追加する.
     * 登録したハンドラは, GUIスレッド上で実行される.
     *
     * @param handler 追加するイベントハンドラ
     */
    void addOnAddedToWorkspaceView(BiConsumer<? super WorkspaceView, ? super BhNodeView> handler);

    /**
     * "target" をワークスペースビューに追加したときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    void removeOnAddedToWorkspaceView(
        BiConsumer<? super WorkspaceView, ? super BhNodeView> handler);

    /**
     * "target" をワークスペースビューから取り除いたときのイベントハンドラを追加する.
     * 登録したハンドラは, GUIスレッド上で実行される.
     *
     * @param handler 追加するイベントハンドラ
     */
    void addOnRemovedFromWorkspaceView(
        BiConsumer<? super WorkspaceView, ? super BhNodeView> handler);

    /**
     * "target" をワークスペースビューから取り除いたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ
     */
    void removeOnRemovedFromWorkspaceView(
        BiConsumer<? super WorkspaceView, ? super BhNodeView> handler);

    /**
     * "target" のサイズが変更されたときのイベントハンドラを追加する.
     *
     * @param handler 追加するイベントハンドラ.
     */
    public void addOnNodeSizeChanged(Consumer<? super BhNodeView> handler);

    /**
     * "target" のサイズが変更されたときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ.
     */
    public void removeOnNodeSizeChanged(Consumer<? super BhNodeView> handler);

    /**
     * "target" が他のノードビューと入れ替わったときのイベントハンドラを追加する.
     * 
     * <pre>
     * イベントハンドラの第 1 引数 : このノードビュー.
     * イベントハンドラの第 2 引数 : このノードビューの代わりに接続されたノードビュー.
     * </pre>
     *
     * @param handler 追加するイベントハンドラ.
     */
    public void addOnNodeReplaced(BiConsumer<? super BhNodeView, ? super BhNodeView> handler);

    /**
     * "target" が他のノードビューと入れ替わったときのイベントハンドラを削除する.
     *
     * @param handler 削除するイベントハンドラ.
     */
    public void removeOnNodeReplaced(BiConsumer<? super BhNodeView, ? super BhNodeView> handler);
  }
}
