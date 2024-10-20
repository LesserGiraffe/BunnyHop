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

package net.seapanda.bunnyhop.model.syntaxsymbol;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import net.seapanda.bunnyhop.common.Showable;
import net.seapanda.bunnyhop.common.constant.VersionInfo;
import net.seapanda.bunnyhop.modelprocessor.BhModelProcessor;

/**
 * 終端記号, 非終端記号を表すクラス.
 *
 * @author K.Koike
 */
public abstract class SyntaxSymbol implements Showable, Serializable {

  private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
  /** 終端, 非終端記号名. */
  private final String symbolName;
  /** SyntaxSymbolオブジェクトが持つID. */
  private SyntaxSymbolId symbolId = SyntaxSymbolId.newId();

  /**
   * 引数で指定したシンボル名を持つSyntaxSymbolをgeneration(もしくはそれ以下)の世代のSyntaxSymbolから探す.
   *
   * @param generation 自分から見てこのレベルの世代もしくはそれ以下を探す.  例(0:自分(もしくはそれ以下)を探す. 1:子(もしくはそれ以下)を探す)
   * @param toTerminal generation で指定した階層のみ探す場合false. 末端ノードまで探す場合true
   * @param foundSymbolList 見つかったSyntaxSymbolを格納するリスト
   * @param symbolNames シンボル名
   */
  public abstract void findSymbolInDescendants(
      int generation,
      boolean toTerminal,
      List<SyntaxSymbol> foundSymbolList,
      String... symbolNames);

  /**
   * 引数で指定したシンボル名を持つ {@link SyntaxSymbol} を子以下の {@link SyntaxSymbol} から探す.
   *
   * @param symbolNamePath
   *      <pre>
   *      子孫ノードのパスに, この名前のリストのとおりに繋がっているパスがある場合, リストの最後の名前のノードを返す.
   *      symbolName[0] == childName, symbolName[1] == grandsonName
   *      </pre>
   * @return 引数の最後のシンボル名を持つ SyntaxSymbol オブジェクト.  見つからなかった場合は null.
   */
  public SyntaxSymbol findSymbolInDescendants(String... symbolNamePath) {
    if (symbolNamePath.length == 0) {
      throw new AssertionError("The symbol name path must not be empty.");
    }
    List<SyntaxSymbol> foundSymbolList = new ArrayList<>();
    findSymbolInDescendants(
        symbolNamePath.length, false, foundSymbolList, symbolNamePath[symbolNamePath.length - 1]);
    //symbolNamePath == (a,b,c)  => reverseSymbolNamePath == (b,a,thisSymbolName)
    String[] reverseSymbolNamePath = new String[symbolNamePath.length];
    for (int i = symbolNamePath.length - 2, j = 0; i >= 0; --i, ++j) {
      reverseSymbolNamePath[j] = symbolNamePath[i];
    }
    reverseSymbolNamePath[reverseSymbolNamePath.length - 1] = symbolName;
    for (SyntaxSymbol foundSymbol : foundSymbolList) {
      if (foundSymbol.findSymbolInAncestors(reverseSymbolNamePath) == this) {
        return foundSymbol;
      }
    }
    return null;
  }

  /**
   * 引数で指定したシンボル名を持つ SyntaxSymbol を generation(もしくはそれ以上)の世代の SyntaxSymbol から探す.
   *
   * @param symbolName シンボル名
   * @param generation 自分から見てこのレベルの世代もしくはそれ以上を探す.  例(0:自分(もしくはそれ以上)を探す. 1:親(もしくはそれ以上)を探す)
   * @param upToTop generation で指定した世代のみ探す場合false. トップノードまで探す場合true.
   * @return シンボル名を持つ SyntaxSymbol オブジェクト. 見つからなかった場合は null.
   */
  public abstract SyntaxSymbol findSymbolInAncestors(
      String symbolName, int generation, boolean upToTop);

  /**
   * 引数で指定したシンボル名を持つSyntaxSymbolを親以上のSyntaxSymbolから探す.
   *
   * @param symbolNamePath
   *      <pre>
   *      先祖ノードがこの名前のリストのとおりにつながっているとき, リストの最後の名前のノードを返す.<br>
   *      symbolName[0] == parentName, symbolName[1] == grandParentName
   *      </pre>
   * @return 引数の最後のシンボル名を持つ SyntaxSymbol オブジェクト. 見つからなかった場合は null.
   */
  public SyntaxSymbol findSymbolInAncestors(String... symbolNamePath) {
    if (symbolNamePath.length == 0) {
      throw new AssertionError("The symbol name path must not be empty.");
    }

    int idx = 0;
    SyntaxSymbol currentLevel = this;
    while (idx < symbolNamePath.length) {
      String childName = symbolNamePath[idx];
      ++idx;
      currentLevel = currentLevel.findSymbolInAncestors(childName, 1, false);
      if (currentLevel == null) {
        break;
      }
    }
    return currentLevel;
  }

  /**
   * <pre>
   * 引数で指定したシンボルのこのシンボルに対する相対パスを取得する.
   * 例1) A -> B -> C のとき, A.getRelativeSymbolNamePath(C) なら return [B, C]
   * 例2) A -> B -> C のとき, C.getRelativeSymbolNamePath(A) なら return [B, A]
   * </pre>
   *
   * @param syntaxSymbol このシンボルに対する相対パスを取得するシンボル
   * @return 相対パス. 先祖 or 子孫でないノードを引数に指定した場合 null
   * */
  public String[] getRelativeSymbolNamePath(SyntaxSymbol syntaxSymbol) {
    Deque<String> path = new LinkedList<>();
    if (syntaxSymbol.isDescendantOf(this)) {
      path.addFirst(syntaxSymbol.getSymbolName());
      SyntaxSymbol parent = syntaxSymbol;
      while ((parent = parent.findSymbolInAncestors("*", 1, false)) != this) {
        path.addFirst(parent.getSymbolName());
      }
      return path.toArray(new String[path.size()]);

    } else if (this.isDescendantOf(syntaxSymbol)) {
      SyntaxSymbol parent = this;
      while ((parent = parent.findSymbolInAncestors("*", 1, false)) != syntaxSymbol) {
        path.addLast(parent.getSymbolName());
      }
      path.addLast(syntaxSymbol.getSymbolName());
      return path.toArray(new String[path.size()]);
    }
    return null;
  }


  /**
   * このシンボルが引数で指定したシンボルの子孫ノードであった場合 true を返す.
   *
   * @param ancestor このシンボルが {@code ancestor} の子孫ノードであった場合 true
   * @return このSyntaxSymbolがancestor 以下のノードであった場合true
   */
  public abstract boolean isDescendantOf(SyntaxSymbol ancestor);

  /**
   * コンストラクタ.
   *
   * @param symbolName シンボル名.
   */
  protected SyntaxSymbol(String symbolName) {
    this.symbolName = symbolName;
  }

  /**
   * コピーコンストラクタ.
   *
   * @param org コピー元オブジェクト
   */
  protected SyntaxSymbol(SyntaxSymbol org) {
    symbolName = org.symbolName;
  }

  /**
   * シンボル名を取得する.
   *
   * @return 終端, 非終端記号名
   */
  public String getSymbolName() {
    return symbolName;
  }

  /**
   * シンボル ID を取得する.
   *
   * @return シンボル ID
   */
  public SyntaxSymbolId getSymbolId() {
    return symbolId;
  }

  /**
   * シンボル ID を新しくする.
   *
   * @return 新しく割り振られたシンボルID
   */
  public SyntaxSymbolId renewSymbolId() {
    symbolId = SyntaxSymbolId.newId();
    return symbolId;
  }

  /** {@code visitor} にこのオブジェクトを渡す. */
  public abstract void accept(BhModelProcessor visitor);
}
