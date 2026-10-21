package net.seapanda.bunnyhop.node.model.syntaxtree;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 終端記号・非終端記号の名前 (シンボル名) でラベル付けされた木構造の要素を表すクラス.
 *
 * <p>シンボル名による子孫・先祖ノードの探索など, 木構造の汎用的な操作のみを提供する.
 *
 * @author K.Koike
 */
public abstract class SyntaxTreeNodeBase<T extends SyntaxTreeNodeBase<T>> {

  /** 終端記号・非終端記号名. */
  private final String symbolName;

  public SyntaxTreeNodeBase(String symbolName) {
    Objects.requireNonNull(symbolName);
    this.symbolName = symbolName;
  }

  /**
   * このノードの親要素を返す.
   *
   * @return このノードの親要素. 見つからない場合は null.
   */
  public abstract T getParent();

  /**
   * このノードの全ての子要素を変更不能なリストとして返す.
   *
   * @return このノードの子要素のリスト.
   */
  public abstract List<? extends T> getChildren();


  /**
   * このノードをラベル付けしている終端記号, 非終端記号の名前を返す.
   *
   * <p>この名前は {@link #findDescendantOf} や {@link #findAncestorOf} で
   * ノードを特定するときのパス要素として使われる.
   *
   * @return このノードの終端, 非終端記号名. null にはならない.
   */
  public String getSymbolName() {
    return symbolName;
  }

  /**
   * このノードのルートであるノードを返す <br> ルートノードの場合は自身を返す.
   *
   * @return このノードのルートノード
   */
  public T findRootNode() {
    T parent = getParent();
    if (parent == null) {
      return self();
    }
    return parent.findRootNode();
  }

  /** サブタイプのインスタンスを返す. */
  protected abstract T self();

  /**
   * 引数で指定したシンボル名を持つ {@link SyntaxTreeNodeBase} を {@code generation} もしくは
   * それ以下の世代の {@link SyntaxTreeNodeBase} から探す.
   *
   * @param generation 自分から見てこのレベルの世代もしくはそれ以下を探す.  例 (0:自分. 1:子)
   * @param toBottom {@code generation} で指定した世代のみ探す場合 false. 末端ノードまで探す場合 true.
   * @param symbolNames シンボル名
   * @return 見つかった {@link SyntaxTreeNodeBase} のリスト
   */
  public List<T> findDescendants(int generation, boolean toBottom, String... symbolNames) {
    var foundNodes = new ArrayList<T>();
    if (generation == 0) {
      for (String symbolName : symbolNames) {
        if (symbolNameMatches(symbolName)) {
          foundNodes.add(this.self());
        }
      }
      if (!toBottom) {
        return foundNodes;
      }
    }
    int childLevel = Math.max(generation - 1, 0);
    for (T child : getChildren()) {
      foundNodes.addAll(child.findDescendants(childLevel, toBottom, symbolNames));
    }
    return foundNodes;
  }

  /**
   * 引数で指定したシンボル名を持つ {@link SyntaxTreeNodeBase} を子以下の {@link SyntaxTreeNodeBase} から探す.
   *
   * @param symbolNamePath
   *      <pre>
   *      子孫ノードのパスに, この名前のリストのとおりに繋がっているパスがある場合, リストの最後の名前のノードを返す.
   *      symbolName[0] == childName, symbolName[1] == grandsonName
   *      </pre>
   * @return 引数の最後のシンボル名を持つ {@link SyntaxTreeNodeBase} オブジェクト.  見つからなかった場合は null.
   */
  public T findDescendantOf(String... symbolNamePath) {
    if (symbolNamePath.length == 0) {
      throw new AssertionError("The symbol name path must not be empty.");
    }
    List<? extends T> foundSymbolList =
        findDescendants(symbolNamePath.length, false, symbolNamePath[symbolNamePath.length - 1]);
    // symbolNamePath == (a,b,c)  => reverseSymbolNamePath == (b, a, thisSymbolName)
    String[] reverseSymbolNamePath = new String[symbolNamePath.length];
    for (int i = symbolNamePath.length - 2, j = 0; i >= 0; --i, ++j) {
      reverseSymbolNamePath[j] = symbolNamePath[i];
    }
    reverseSymbolNamePath[reverseSymbolNamePath.length - 1] = getSymbolName();
    for (T foundSymbol : foundSymbolList) {
      if (foundSymbol.findAncestorOf(reverseSymbolNamePath) == this) {
        return foundSymbol;
      }
    }
    return null;
  }

  /**
   * 引数で指定したシンボル名を持つ {@link SyntaxTreeNodeBase} を {@code generation} もしくは
   * それ以上の世代の {@link SyntaxTreeNodeBase} から探す.
   *
   * @param generation 自分から見てこのレベルの世代もしくはそれ以上を探す.  例 (0:自分. 1:親)
   * @param toTop {@code generation} で指定した世代のみ探す場合 false. トップノードまで探す場合 true.
   * @param symbolName シンボル名
   * @return シンボル名を持つ {@link SyntaxTreeNodeBase} オブジェクト. 見つからなかった場合は null.
   */
  public T findAncestor(int generation, boolean toTop, String symbolName) {
    if (generation == 0) {
      if (symbolNameMatches(symbolName)) {
        return this.self();
      }
      if (!toTop) {
        return null;
      }
    }
    T parent = getParent();
    if (parent == null) {
      return null;
    }
    return parent.findAncestor(Math.max(0, generation - 1), toTop, symbolName);
  }

  /**
   * 引数で指定したシンボル名を持つ {@link SyntaxTreeNodeBase} を親以上の {@link SyntaxTreeNodeBase} から探す.
   *
   * @param symbolNamePath
   *      <pre>
   *      先祖ノードがこの名前のリストのとおりにつながっているとき, リストの最後の名前のノードを返す.<br>
   *      symbolName[0] == 親ノードのシンボル名, symbolName[1] == 祖父母ノードのシンボル名
   *      </pre>
   * @return 引数の最後のシンボル名を持つ {@link SyntaxTreeNodeBase} オブジェクト. 見つからなかった場合は null.
   */
  public T findAncestorOf(String... symbolNamePath) {
    if (symbolNamePath.length == 0) {
      throw new AssertionError("The symbol name path must not be empty.");
    }

    int idx = 0;
    T currentLevel = this.self();
    while (idx < symbolNamePath.length) {
      String childName = symbolNamePath[idx];
      ++idx;
      currentLevel = currentLevel.findAncestor(1, false, childName);
      if (currentLevel == null) {
        break;
      }
    }
    return currentLevel;
  }

  /**
   * このノードが {@code ancestor} の子孫ノードである場合 true を返す.
   *
   * <p>{@code ancestor} にこのノード自身を指定した場合も true を返す.
   *
   * @param ancestor このノードの先祖かどうか調べるノード
   * @return このノードが {@code ancestor} 自身かその子孫である場合 true
   */
  public boolean isDescendantOf(T ancestor) {
    if (this == ancestor) {
      return true;
    }
    T parent = getParent();
    if (parent == null) {
      return false;
    }
    return parent.isDescendantOf(ancestor);
  }

  /**
   * このノードと {@code node} が直系である場合 true を返す.
   *
   * <p>一方が他方の先祖である場合に直系とみなす.
   * 兄弟ノードのように, どちらも他方の先祖でない関係の場合は false を返す.
   * {@code node} にこのノード自身を指定した場合は true を返す.
   *
   * @param node このノードと直系かどうか調べるノード
   * @return このノードと {@code node} が同一, またはどちらか一方が他方の先祖である場合 true
   */
  public boolean isLinealWith(T node) {
    return isDescendantOf(node) || node.isDescendantOf(this.self());
  }

  /**
   * このシンボルの名前が {@code name} に一致するか調べる.
   *
   * @param name このシンボルの名前と一致するか調べる文字列.
   *             "*" を指定した場合, このシンボルの名前によらず一致したとみなす.
   * @return このシンボルの名前が {@code name} に一致する場合 true.
   */
  private boolean symbolNameMatches(String name) {
    if (name == null) {
      return false;
    }
    if (name.equals("*")) {
      return true;
    }
    return getSymbolName().equals(name);
  }
}
