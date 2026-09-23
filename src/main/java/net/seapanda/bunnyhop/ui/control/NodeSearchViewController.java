package net.seapanda.bunnyhop.ui.control;

import static net.seapanda.bunnyhop.common.configuration.BhConstants.Css.Class.DEFAULT_TEXT_HIGHLIGHT;
import static net.seapanda.bunnyhop.common.configuration.BhSettings.Search.maxItemsInNodeSearchResult;
import static net.seapanda.bunnyhop.node.view.effect.VisualEffectType.SEARCH_RESULT;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import net.seapanda.bunnyhop.node.view.BhNodeView;
import net.seapanda.bunnyhop.node.view.TextNodeView;
import net.seapanda.bunnyhop.node.view.effect.VisualEffectManager;
import net.seapanda.bunnyhop.search.SearchBoxDelegate;
import net.seapanda.bunnyhop.search.SearchQuery;
import net.seapanda.bunnyhop.search.SearchQueryResult;
import net.seapanda.bunnyhop.search.Substring;
import net.seapanda.bunnyhop.ui.model.NodeSearchListItem;
import net.seapanda.bunnyhop.ui.view.NodeSearchListCell;
import net.seapanda.bunnyhop.ui.view.ViewUtil;
import net.seapanda.bunnyhop.workspace.control.WorkspaceSelectorController;
import net.seapanda.bunnyhop.workspace.view.WorkspaceSetViewCallBackRegistry;
import net.seapanda.bunnyhop.workspace.view.WorkspaceView;

/**
 * ノードの検索結果を表示する UI コンポーネントのコントローラ.
 *
 * @author K.Koike
 */
public class NodeSearchViewController {

  @FXML private CheckBox nsJumpCheckBox;
  @FXML private ListView<NodeSearchListItem> nsListView;
  @FXML private WorkspaceSelectorController nsWsSelectorController;
  @FXML private SearchBox nsSearchBoxController;

  private final WorkspaceSetViewCallBackRegistry cbRegistry;
  private final VisualEffectManager effectManager;
  private final ListItemRegistry listItemRegistry = new ListItemRegistry();
  private final Set<BhNodeView> nodeViews = new LinkedHashSet<>();
  private SearchResult searchResult;

  /** コンストラクタ. */
  public NodeSearchViewController(
      WorkspaceSetViewCallBackRegistry cbRegistry, VisualEffectManager effectManager) {
    this.effectManager = effectManager;
    this.cbRegistry = cbRegistry;
  }

  /** このコントローラの UI 要素を初期化する. */
  @FXML
  public void initialize() {
    nsSearchBoxController.open(new SearchBoxDelegateImpl());
    setEventHandlers();
  }

  /** イベントハンドラを設定する. */
  private void setEventHandlers() {
    nsListView.setCellFactory(view -> createListCell());
    nsListView.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldVal, newVal) -> onSearchResultItemSelected(oldVal, newVal));
    nsWsSelectorController.setOnWorkspaceSelected(event -> refreshResultListView(searchResult));
    cbRegistry.getOnNodeTextChanged().add(event -> refreshSearchResultItems(event.nodeView()));
    cbRegistry.getOnNodeAdded().add(event -> {
      nodeViews.add(event.nodeView());
      refreshSearchResultItems(event.nodeView());
    });
    cbRegistry.getOnNodeRemoved().add(event -> {
      nodeViews.remove(event.nodeView());
      removeSearchResultItems(event.nodeView());
    });
  }

  private NodeSearchListCell createListCell() {
    var cell = new NodeSearchListCell();
    cell.enableHighlighting(DEFAULT_TEXT_HIGHLIGHT);
    return cell;
  }

  /** ノード検索結果の要素が選択されたときのイベントハンドラ. */
  private void onSearchResultItemSelected(
      NodeSearchListItem deselected, NodeSearchListItem selected) {
    if (deselected != null) {
      effectManager.setEffectEnabled(deselected.getNodeView(), false, SEARCH_RESULT);
    }
    if (selected == null) {
      return;
    }
    effectManager.setEffectEnabled(selected.getNodeView(), true, SEARCH_RESULT);
    if (!nsJumpCheckBox.isSelected()) {
      return;
    }
    BhNodeView view = selected.getNodeView();
    if (view.getWorkspaceView() != null) {
      ViewUtil.jump(view);
    }
  }

  private SearchQueryResult search(SearchQuery query) {
    clearSearchResult();
    searchResult = new SearchResult(new LinkedHashSet<>(), query);
    for (BhNodeView nodeView : nodeViews) {
      collectMatchesFromView(nodeView, searchResult);
    }
    refreshResultListView(searchResult);
    return toSearchQueryResult(searchResult);
  }

  /**
   * {@code view} のテキストから {@code searchResult} の検索クエリに一致する部分文字列を探し,
   * 見つかった数だけ {@link NodeSearchListItem} を作成して {@code searchResult} に追加する.
   *
   * @param view このビューのテキストから検索する
   * @param searchResult 検索結果の格納先. 結果の数が上限に達している場合は何もしない.
   */
  private void collectMatchesFromView(BhNodeView view, SearchResult searchResult) {
    if (searchResult.items().size() >= maxItemsInNodeSearchResult) {
      return;
    }
    if (view instanceof TextNodeView textNodeView) {
      int maxNumToFind = maxItemsInNodeSearchResult - searchResult.items().size();
      textNodeView.getVisual()
          .enableTextHighlighting(searchResult.query().getPattern(), maxNumToFind)
          .forEach(matched -> {
            NodeSearchListItem item = listItemRegistry.createListItem(matched, textNodeView);
            searchResult.items().add(item);
          });
    }
  }

  /**
   * {@code searchResult} が持つ検索結果のうち, 現在選択されているワークスペースに属するものを抽出し,
   * {@link #nsListView} に表示する.
   *
   * @param searchResult 表示する検索結果. null の場合は何もしない.
   */
  private void refreshResultListView(SearchResult searchResult) {
    if (searchResult == null) {
      return;
    }
    List<NodeSearchListItem> items = searchResult.items().stream()
        .filter(item -> isNodeViewInSelectedWorkspace(item.getNodeView()))
        .toList();
    nsListView.getItems().setAll(items);
  }

  /**
   * {@code view} が, 現在選択されているワークスペースに属するかどうか調べる.
   *
   * @param view 調べる {@link BhNodeView}
   * @return {@code view} が現在選択されているワークスペースに属する場合 true
   */
  private boolean isNodeViewInSelectedWorkspace(BhNodeView view) {
    return Optional.ofNullable(view.getWorkspaceView())
        .map(WorkspaceView::getWorkspace)
        .map(ws -> nsWsSelectorController.matchesSelection(ws))
        .orElse(false);

  }

  private static SearchQueryResult toSearchQueryResult(SearchResult searchResult) {
    boolean truncated = searchResult.items().size() == maxItemsInNodeSearchResult;
    return new SearchQueryResult(0, searchResult.items().size(), truncated);
  }

  /**
   * {@code view} に対応する検索結果を更新する.
   * 既存の {@link NodeSearchListItem} を取り除いた後, {@code view} から一致する文字列を探し直し,
   * 新たに見つかった分を検索結果に追加する.
   *
   * @param view 検索結果を更新する対象のビュー
   */
  private void refreshSearchResultItems(BhNodeView view) {
    if (searchResult == null) {
      return;
    }
    Set<NodeSearchListItem> items = listItemRegistry.removeMapping(view);
    searchResult.items().removeAll(items);
    collectMatchesFromView(view, searchResult);
    refreshResultListView(searchResult);
    nsSearchBoxController.setSearchResult(toSearchQueryResult(searchResult));
  }

  /**
   * {@code view} に対応する {@link NodeSearchListItem} を検索結果から取り除く.
   *
   * @param view このビューに対応する検索結果の項目を取り除く
   */
  private void removeSearchResultItems(BhNodeView view) {
    if (searchResult == null) {
      return;
    }
    Set<NodeSearchListItem> items = listItemRegistry.removeMapping(view);
    searchResult.items().removeAll(items);
    refreshResultListView(searchResult);
    nsSearchBoxController.setSearchResult(toSearchQueryResult(searchResult));
  }

  /** 現在の検索結果を破棄する. */
  private void clearSearchResult() {
    searchResult = null;
    listItemRegistry.getListItems()
        .forEach(NodeSearchViewController::disableTextHighlightingOnNodeView);
    listItemRegistry.clear();
    nsListView.getItems().clear();
  }

  /** {@code item} に対応する {@link BhNodeView} のテキストの強調表示を無効化する. */
  private static void disableTextHighlightingOnNodeView(NodeSearchListItem item) {
    if (item.getNodeView() instanceof TextNodeView textNodeView) {
      textNodeView.getVisual().disableTextHighlighting();
    }
  }

  /** {@link SearchBox} を使ったブレークポイント一覧の検索を担当するクラス. */
  private class SearchBoxDelegateImpl implements SearchBoxDelegate {

    @Override
    public SearchQueryResult onSearchRequested(SearchQuery query) {
      return search(query);
    }

    @Override
    public void onClosed() {}

    @Override
    public void onCleared() {
      clearSearchResult();
    }

    @Override
    public Object getUser() {
      return NodeSearchViewController.this;
    }
  }

  /**
   * {@link NodeSearchViewController} が生成した全ての {@link NodeSearchListItem} を管理し,
   * 各アイテムに割り当てられた {@link BhNodeView} との対応関係を追跡するクラス.
   */
  private static class ListItemRegistry {

    private final Map<BhNodeView, Set<NodeSearchListItem>> nodeViewToListItems = new HashMap<>();

    /** このオブジェクトが持つデータをクリアする. */
    void clear() {
      nodeViewToListItems.clear();
    }

    NodeSearchListItem createListItem(Substring matched, TextNodeView view) {
      var item = new NodeSearchListItem(matched, view);
      nodeViewToListItems.computeIfAbsent(view, key -> new LinkedHashSet<>()).add(item);
      return item;
    }

    /** このオブジェクトが作成した全ての {@link NodeSearchListItem} を取得する. */
    Set<NodeSearchListItem> getListItems() {
      return nodeViewToListItems.values().stream()
          .flatMap(Collection::stream)
          .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 引数で指定した {@link BhNodeView} と {@link NodeSearchListItem} の対応関係を取り除く.
     *
     * @return {@code node} と対応関係にあった {@link NodeSearchListItem} のセット.
     */
    Set<NodeSearchListItem> removeMapping(BhNodeView view) {
      Set<NodeSearchListItem> removed = nodeViewToListItems.remove(view);
      return removed == null ? new HashSet<>() : removed;
    }
  }

  /** 検索結果を格納するレコード. */
  record SearchResult(Set<NodeSearchListItem> items, SearchQuery query) { }
}
