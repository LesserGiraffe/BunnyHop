package net.seapanda.bunnyhop.view.proxy;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;
import net.seapanda.bunnyhop.control.node.TemplateNodeController;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.undo.UserOperation;
import net.seapanda.bunnyhop.view.nodeselection.BhNodeSelectionView;

/**
 * ノードの選択画面を操作するプロキシクラスのインタフェース.
 *
 * @author K.Koike
 */
public interface BhNodeSelectionViewProxy {
  
  /**
   * ノード選択ビューを登録する.
   *
   * @param view 登録するビュー.
   */
  public void addNodeSelectionView(BhNodeSelectionView view);

  /**
   * 引数で指定したカテゴリに {@code root} 以下のノードを全て追加する.
   *
   * <p> {@code root} のコントローラが {@link TemplateNodeController} であること. </p>
   *
   * @param categoryName {@code node} を追加するカテゴリの名前
   * @param root このノード以下のノードを全て追加する.
   * @param userOpe undo 用コマンドオブジェクト
   */
  public default void addNodeTree(String categoryName, BhNode root, UserOperation userOpe) {}

  /**
   * {@code root} 以下のノードを全て削除する.
   *
   * <p> {@code root} のコントローラが {@link TemplateNodeController} であること. </p>
   *
   * @param root このノード以下のノードを全て削除する.
   * @param userOpe undo 用コマンドオブジェクト
   */
  public default void removeNodeTree(BhNode root, UserOperation userOpe) {}

  /**
   * {@code categoryName} に追加したノードツリーのルートノードを全て取得する.
   *
   * @param categoryName 取得するノードツリーのカテゴリ名.
   * @return {@code categoryName} に対して追加したノードツリーのルートノードのセット.
   *         登録されていないカテゴリを指定した場合は空のセット.
   */
  public default SequencedSet<BhNode> getNodeTrees(String categoryName) {
    return new LinkedHashSet<>();
  }

  /**
   * 全てのノード選択ビューを拡大もしくは縮小する.
   *
   * @param zoomIn 拡大する場合 true
   */
  public default void zoom(boolean zoomIn) {}

  /**
   * {@code categoryName} で指定したカテゴリのノード選択ビューを表示する.
   *
   * @param categoryName 表示するノード選択ビューのカテゴリ名
   */
  public default void show(String categoryName) {}

  /**
   * 全てのノード選択ビューを非表示にする.
   */
  public default void hideAll() {}

  /**
   * {@categoryName} に対応するノード選択ビューが開いているか調べる.
   *
   * @param categoryName 開いているかどうかを調べるノード選択ビューのカテゴリ名
   * @return {@categoryName} に対応するノード選択ビューが開いている場合 true.
   */
  public default boolean isShowed(String categoryName) {
    return false;
  }

  /**
   * 現在表示されているカテゴリの名前を返す.
   *
   * @return 現在表示されているカテゴリの名前. 表示されているカテゴリがない場合は empty.
   */
  public default Optional<String> getNameOfShowedCategory() {
    return Optional.empty();
  }

  /**
   * ノード選択ビューのうち表示されているものがあるかどうか調べる.
   *
   * @return BhNode選択パネルのうち一つでも表示されている場合true
   */
  public default boolean isAnyShowed() {
    return false;
  }
}
