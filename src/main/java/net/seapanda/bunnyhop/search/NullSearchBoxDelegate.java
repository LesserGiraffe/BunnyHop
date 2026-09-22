package net.seapanda.bunnyhop.search;

/** {@link SearchBoxDelegate} の Null オブジェクトクラス. */
public class NullSearchBoxDelegate implements SearchBoxDelegate {

  @Override
  public SearchQueryResult onSearchRequested(SearchQuery query) {
    return null;
  }

  @Override
  public void onClosed() {}

  @Override
  public void onCleared() {}

  @Override
  public Object getUser() {
    return this;
  }
}
