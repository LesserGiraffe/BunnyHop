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

package net.seapanda.bunnyhop.model.node.event;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import net.seapanda.bunnyhop.model.factory.BhNodeFactory.MvcType;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.ConnectiveNode;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.model.node.TextNode;
import net.seapanda.bunnyhop.model.node.TextNode.FormatResult;
import net.seapanda.bunnyhop.model.node.TextNode.TextOption;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * ノードに対して定義されたイベントハンドラを呼び出すオブジェクトのインタフェース.
 *
 * @author K.Koike
 */
public interface NodeEventInvoker {

  /**
   * {@code target} がワークスペースから子ノードに移ったときの処理を実行する.
   *
   * @param target イベントハンドラが定義されたノード
   * @param oldReplaced {@code target} がつながった位置に, 元々子ノードとしてつながっていたノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  void onMovedFromWsToChild(BhNode target, BhNode oldReplaced, UserOperation userOpe);

  /**
   * {@code target} が子ノードからワークスペースに移ったときの処理を実行する.
   *
   * @param target イベントハンドラが定義されたノード
   * @param oldParent 移る前に {@code target} が接続されていた親ノード
   * @param oldRoot 移る前に {@code target} が所属していたノードツリーのルートノード
   * @param newReplaced ワークスペースに移る際, {@code target} の替わりにつながったノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  void onMovedFromChildToWs(
      BhNode target,
      ConnectiveNode oldParent,
      BhNode oldRoot,
      BhNode newReplaced,
      UserOperation userOpe);

  /**
   * {@code target} の子ノードが入れ替わったときの処理を実行する.
   *
   * @param target イベントハンドラが定義されたノード
   * @param oldChild 入れ替わった古いノード
   * @param newChild 入れ替わった新しいノード
   * @param parentCnctr 子が入れ替わったコネクタ
   * @param userOpe undo 用コマンドオブジェクト
   */
  void onChildReplaced(
      BhNode target,
      BhNode oldChild,
      BhNode newChild,
      Connector parentCnctr,
      UserOperation userOpe);

  /**
   * {@code target} の削除前に呼ばれる処理を実行する.
   *
   * @param target イベントハンドラが定義されたノード
   * @param nodesToDelete {@code target} と共に削除される予定のノード.
   * @param causeOfDeletion {@code target} の削除原因
   * @param userOpe undo 用コマンドオブジェクト
   * @return 削除をキャンセルする場合 false. 続行する場合 true.
   */
  boolean onDeletionRequested(
      BhNode target,
      Collection<? extends BhNode> nodesToDelete,
      CauseOfDeletion causeOfDeletion,
      UserOperation userOpe);

  /**
   * ユーザー操作により, {@code target} がカット & ペーストされる直前に呼ばれる処理を実行する.
   *
   * @param target イベントハンドラが定義されたノード
   * @param nodesToCut {@code target} ノードとともにカットされる予定のノード
   * @param userOpe undo 用コマンドオブジェクト
   * @return カットをキャンセルする場合 false.  続行する場合 true.
   */
  boolean onCutRequested(
      BhNode target, Collection<? extends BhNode> nodesToCut, UserOperation userOpe);
  

  /**
   * ユーザー操作により, {@code target} がコピー & ペーストされる直前に呼ばれる処理を実行する.
   *
   * @param target イベントハンドラが定義されたノード
   * @param nodesToCopy {@code target} とともにコピーされる予定のノード
   * @param userOpe undo 用コマンドオブジェクト
   * @return {@link BhNode} を引数にとり, コピーするかどうかの boolean 値を返す関数.
   */
  Predicate<? super BhNode> onCopyRequested(
      BhNode target,
      Collection<? extends BhNode> nodesToCopy,
      UserOperation userOpe);

  /**
   * {@code target} がテンプレートノードとして作成されたときの処理を実行する.
   *
   * @param target イベントハンドラが定義されたノード
   * @param userOpe undo 用コマンドオブジェクト
   */
  void onCreatedAsTemplate(BhNode target, UserOperation userOpe);

  /**
   * {@code target} のドラッグが始まったときの処理を実行する.
   *
   * @param target イベントハンドラが定義されたノード
   * @param eventInfo ドラッグ操作に関連するマウスイベントを格納したオブジェクト
   * @param userOpe undo 用コマンドオブジェクト
   */
  void onDragStarted(BhNode target, MouseEventInfo eventInfo, UserOperation userOpe);

  /**
   * 引数の文字列が {@code target} にセット可能かどうか判断するときの処理を実行する.
   *
   * @param target イベントハンドラが定義されたノード
   * @param text セット可能かどうか判断する文字列
   * @return 引数の文字列がセット可能だった場合 true
   */
  boolean onTextChecking(TextNode target, String text);

  /**
   * {@code text} を {@code target} に定義されたフォーマッタで整形して返す.
   *
   * @param text 整形対象の全文字列
   * @param addedText 前回整形したテキストから新たに追加された文字列
   * @return フォーマット結果
   */
  FormatResult onTextFormatting(TextNode target, String text, String addedText);

  /**
   * {@code target} が保持する可能性のあるテキストデータのリストを取得する.
   *
   * @return [ (モデルが保持するテキストとビューが保持するオブジェクト 0), 
   *           (モデルが保持するテキストとビューが保持するオブジェクト 1), 
   *           ... ]
   */
  List<TextOption> onTextOptionCreating(TextNode target);

  /**
   * このノードのコンパニオンノードを作成する.
   *
   * @param target イベントハンドラが定義されたノード
   * @param type コンパニオンノードに対して適用する MVC 構造
   * @param userOpe undo 用コマンドオブジェクト
   * @return {@code target} のコンパニオンノードのリスト. コンパニオンノードを持たない場合, 空のリストを返す.
   */
  List<BhNode> onCompanionNodesCreating(BhNode target, MvcType type, UserOperation userOpe);

  /**
   * {@code target} にコンパイルエラーがあるかどうか調べる.
   *
   * @param target イベントハンドラが定義されたノード
   * @return コンパイルエラーがある場合 true.  無い場合 false.
   */
  boolean onCompileErrChecking(BhNode target);  
}
