package net.seapanda.bunnyhop.view.nodeselection;

import java.util.Optional;
import java.util.SequencedSet;
import net.seapanda.bunnyhop.control.node.TemplateNodeController;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;
import net.seapanda.bunnyhop.view.ViewConstructionException;

/**
 * ノードの選択ビューに対する操作を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface BhNodeSelectionViewProxy {
  
  /**
   * ノード選択ビューを登録する.
   *
   * @param name ノード選択ビューの名前
   * @param cssClass ノード選択ビューに適用する css クラス
   */
  void addNodeSelectionView(String name, String cssClass) throws ViewConstructionException;

  /**
   * 引数で指定したカテゴリに {@code root} 以下のノードを全て追加する.
   *
   * <p>{@code root} のコントローラが {@link TemplateNodeController} であること. </p>
   *
   * @param categoryName {@code node} を追加するカテゴリの名前
   * @param root このノード以下のノードを全て追加する.
   * @param userOpe undo 用コマンドオブジェクト
   */
  void addNodeTree(String categoryName, BhNode root, UserOperation userOpe);

  /**
   * {@code root} 以下のノードを全て削除する.
   *
   * <p>{@code root} のコントローラが {@link TemplateNodeController} であること. </p>
   *
   * @param root このノード以下のノードを全て削除する.
   * @param userOpe undo 用コマンドオブジェクト
   */
  void removeNodeTree(BhNode root, UserOperation userOpe);

  /**
   * {@code categoryName} に追加したノードツリーのルートノードを全て取得する.
   *
   * @param categoryName 取得するノードツリーのカテゴリ名.
   * @return {@code categoryName} に対して追加したノードツリーのルートノードのセット.
   *         登録されていないカテゴリを指定した場合は空のセット.
   */
  SequencedSet<BhNode> getNodeTrees(String categoryName);

  /**
   * 全てのノード選択ビューを拡大もしくは縮小する.
   *
   * @param zoomIn 拡大する場合 true
   */
  void zoom(boolean zoomIn);

  /**
   * {@code categoryName} で指定したカテゴリのノード選択ビューを表示する.
   *
   * @param categoryName 表示するノード選択ビューのカテゴリ名
   */
  void show(String categoryName);

  /** 全てのノード選択ビューを非表示にする. */
  void hideCurrentView();

  /**
   * 現在表示されている BhNode のカテゴリの名前を返す.
   *
   * @return 現在表示されている BhNode のカテゴリの名前.  何も表示されていない場合は empty.
   */
  Optional<String> getCurrentCategoryName();

  /** {@link BhNodeSelectionViewProxy} に対するイベントハンドラの追加と削除を行うオブジェクトを返す. */
  CallbackRegistry getCallbackRegistry();

  /** {@link BhNodeSelectionViewProxy} に対してイベントハンドラを追加または削除する機能を規定したインタフェース. */
  interface CallbackRegistry {
    /** 選択中の BhNode のカテゴリが変更されたときのイベントハンドラのレジストリを取得する. */
    ConsumerInvoker<CurrentCategoryChangedEvent>.Registry getOnCurrentCategoryChanged();
  }

  /**
   * 現在表示されている BhNode のカテゴリが変更されたときの情報を格納したレコード.
   *
   * @param proxy 選択中の BhNode のカテゴリが変更された {@link BhNodeSelectionViewProxy}
   * @param oldVal 変更前の BhNode のカテゴリ名 (nullable)
   * @param newVal 変更後の BhNode のカテゴリ名 (nullable)
   */
  record CurrentCategoryChangedEvent(
      BhNodeSelectionViewProxy proxy, String oldVal, String newVal) {}
}
