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

package net.seapanda.bunnyhop.compiler.api;

import java.util.List;

/**
 * コンパイルに必要な情報を保持する構文木の要素が持つインタフェース.
 *
 * @author K.Koike
 */
public interface CompilableSyntaxTreeNode {

  /**
   * 引数で指定したシンボル名を持つ {@link CompilableSyntaxTreeNode} を {@code generation} もしくは
   * それ以下の世代の {@link CompilableSyntaxTreeNode} から探す.
   *
   * @param generation 自分から見てこのレベルの世代もしくはそれ以下を探す.  例 (0:自分. 1:子)
   * @param toBottom {@code generation} で指定した世代のみ探す場合 false.  末端ノードまで探す場合 true.
   * @param symbolNames シンボル名
   * @return 見つかった {@link CompilableSyntaxTreeNode} のリスト
   */
  List<? extends CompilableSyntaxTreeNode> findDescendants(
      int generation, boolean toBottom, String... symbolNames);

  /**
   * 引数で指定したシンボル名を持つ {@link CompilableSyntaxTreeNode} を子以下の {@link CompilableSyntaxTreeNode} から探す.
   *
   * @param symbolNamePath
   *      <pre>
   *      子孫ノードのパスに, この名前のリストのとおりに繋がっているパスがある場合, リストの最後の名前のノードを返す.
   *      symbolName[0] == childName, symbolName[1] == grandsonName
   *      </pre>
   * @return 引数の最後のシンボル名を持つ {@link CompilableSyntaxTreeNode} オブジェクト.  見つからなかった場合は null.
   */
  CompilableSyntaxTreeNode findDescendantOf(String... symbolNamePath);

  /**
   * 引数で指定したシンボル名を持つ {@link CompilableSyntaxTreeNode} を {@code generation} もしくは
   * それ以上の世代の {@link CompilableSyntaxTreeNode} から探す.
   *
   * @param generation 自分から見てこのレベルの世代もしくはそれ以上を探す.  例 (0:自分. 1:親)
   * @param toTop {@code generation} で指定した世代のみ探す場合 false. トップノードまで探す場合 true.
   * @param symbolName シンボル名
   * @return シンボル名を持つ {@link CompilableSyntaxTreeNode} オブジェクト. 見つからなかった場合は null.
   */
  CompilableSyntaxTreeNode findAncestor(int generation, boolean toTop, String symbolName);

  /**
   * 引数で指定したシンボル名を持つ {@link CompilableSyntaxTreeNode} を親以上の {@link CompilableSyntaxTreeNode} から探す.
   *
   * @param symbolNamePath
   *      <pre>
   *      先祖ノードがこの名前のリストのとおりにつながっているとき, リストの最後の名前のノードを返す.<br>
   *      symbolName[0] == 親ノードのシンボル名, symbolName[1] == 祖父母ノードのシンボル名
   *      </pre>
   * @return 引数の最後のシンボル名を持つ {@link CompilableSyntaxTreeNode} オブジェクト. 見つからなかった場合は null.
   */
  CompilableSyntaxTreeNode findAncestorOf(String... symbolNamePath);

  /**
   * このノードをラベル付けしている終端記号, 非終端記号の名前を返す.
   *
   * <p>この名前は {@link #findDescendantOf} や {@link #findAncestorOf} で
   * ノードを特定するときのパス要素として使われる.
   *
   * @return このノードの終端, 非終端記号名. null にはならない.
   */
  String getSymbolName();

  /**
   * このノードの字句 (終端記号に対応する文字列) を返す.
   *
   * <p>字句を持たない非終端記号のノードは空文字列を返す.
   *
   * @return このノードの字句. 持たない場合は空文字列.
   */
  String getLexeme();

  /** このノードの字句 (終端記号に対応する文字列) を設定する. */
  void setLexeme(String val);

  /**
   * オブジェクト固有の ID を取得する.
   *
   * @return オブジェクト固有の ID
   */
  String getInstanceId();

  /** {@link #setTag} で設定した値を取得する. */
  String getTag();

  /** このオブジェクトに対し, タグを設定する. */
  void setTag(String val);

  /**
   * このノードが派生ノードだった場合, そのオリジナルノードを返す.
   *
   * @return このノードのオリジナルノード. 存在しない場合 null を返す.
   */
  CompilableSyntaxTreeNode getOriginal();

  /** このノードが選択の有無に関わらずエントリポイントとして定義してあるノードであるかどうかを返す. */
  boolean isBuiltInEntryPoint();


  /**
   * このノードにブレークポイントを設定可能かどうかを返す.
   *
   * @return このノードにブレークポイントを設定可能な場合 true
   */
  boolean canSetBreakpoint();
}
