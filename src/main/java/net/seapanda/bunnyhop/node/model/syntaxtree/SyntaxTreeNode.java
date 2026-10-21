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

package net.seapanda.bunnyhop.node.model.syntaxtree;

/**
 * 構文木のノードを表すクラス.
 *
 * <p>{@link SyntaxTreeNodeBase} が提供する木構造の操作に加え, インスタンス ID や字句など,
 * 構文木のノードとして必要な情報を保持する.
 *
 * @author K.Koike
 */
public abstract class SyntaxTreeNode extends SyntaxTreeNodeBase<SyntaxTreeNode> {

  private NodeInstanceId nodeInstanceId = NodeInstanceId.newId();

  /**
   * コンストラクタ.
   *
   * @param symbolName シンボル名.
   */
  protected SyntaxTreeNode(String symbolName) {
    super(symbolName);
  }

  /**
   * コピーコンストラクタ.
   *
   * @param org コピー元オブジェクト
   */
  protected SyntaxTreeNode(SyntaxTreeNode org) {
    this(org.getSymbolName());
  }

  @Override
  protected SyntaxTreeNode self() {
    return this;
  }

  /**
   * このノードの字句 (終端記号に対応する文字列) を返す.
   *
   * <p>字句を持たない非終端記号のノードは空文字列を返す.
   *
   * @return このノードの字句. 持たない場合は空文字列.
   */
  public String getLexeme() {
    return "";
  }

  /**
   * オブジェクト固有の ID を設定する.
   *
   * @param id 設定する ID
   */
  public void setInstanceId(NodeInstanceId id) {
    nodeInstanceId = id;
  }

  /**
   * オブジェクト固有の ID を取得する.
   *
   * @return インスタンス ID
   */
  public NodeInstanceId getInstanceId() {
    return nodeInstanceId;
  }
}
