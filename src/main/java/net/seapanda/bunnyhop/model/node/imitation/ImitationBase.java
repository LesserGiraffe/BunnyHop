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

package net.seapanda.bunnyhop.model.node.imitation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.seapanda.bunnyhop.common.constant.VersionInfo;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.attribute.BhNodeId;
import net.seapanda.bunnyhop.model.templates.BhNodeAttributes;
import net.seapanda.bunnyhop.modelprocessor.ImitationBuilder;
import net.seapanda.bunnyhop.undo.UserOperationCommand;

/**
 * イミテーションノードとオリジナルノードに対する操作を実装したクラス.
 *
 * @author K.Koike
 */
public abstract class ImitationBase<T extends ImitationBase<T>> extends Imitatable {

  private static final long serialVersionUID = VersionInfo.SERIAL_VERSION_UID;
  /** このオブジェクトを持つノードがイミテーションノードの手動作成機能を持つ場合 true. */
  public final boolean canCreateImitManually;
  /** イミテーションタグとそれに対応するイミテーションノードIDのマップ. */
  private final Map<ImitationId, BhNodeId> imitIdToImitNodeId;
  /** このオブジェクトを持つノードから作成されたイミテーションノードの集合. */
  private final List<T> imitNodeList;
  /** このオブジェクトを持つノードがイミテーションノードの場合、そのオリジナルノードを保持する. */
  private T orgNode;

  /** サブタイプのインスタンスを返す. */
  protected abstract T self();

  /**
   * 引数で指定したイミテーションタグに対応したイミテーションノードを作成する.
   *
   * @param imitId このイミテーションIDに対応したイミテーションノードを作成する
   * @param userOpeCmd undo 用コマンドオブジェクト
   * @return 作成されたイミテーションノード. イミテーションを持たないノードの場合nullを返す
   */
  public abstract T createImitNode(ImitationId imitId, UserOperationCommand userOpeCmd);

  /** コンストラクタ. */
  public ImitationBase(
      BhNodeAttributes attributes, Map<ImitationId, BhNodeId> imitIdToImitNodeId) {
    super(attributes);
    this.canCreateImitManually = attributes.getCanCreateImitManually();
    this.imitIdToImitNodeId = imitIdToImitNodeId;
    imitNodeList = new ArrayList<>();
    orgNode = null;
  }

  /**
   * コピーコンストラクタ.
   *
   * @param org コピー元オブジェクト
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public ImitationBase(ImitationBase<T> org, UserOperationCommand userOpeCmd) {
    super(org);
    imitIdToImitNodeId = org.imitIdToImitNodeId;
    canCreateImitManually = org.canCreateImitManually;
    imitNodeList = new ArrayList<>();  //元ノードをコピーしても、イミテーションノードとのつながりは無いようにする
    orgNode = null;

    //イミテーションをコピーした場合, コピー元と同じオリジナルノードのイミテーションノードとする
    if (org.isImitationNode()) {
      T original = org.getOriginal();
      original.addImitation(self(), userOpeCmd);
      setOriginal(original, userOpeCmd);
    }
  }

  /**
   * このオブジェクトを持つノードのオリジナルノードを返す.
   *
   * @return このオブジェクトを持つノードのオリジナルノード
   */
  @Override
  public final T getOriginal() {
    return orgNode;
  }

  /**
   * イミテーションノードのオリジナルノードをセットする.
   *
   * @param orgNode イミテーションノードの作成元ノード
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public final void setOriginal(T orgNode, UserOperationCommand userOpeCmd) {
    userOpeCmd.pushCmdOfSetOriginal(self(), this.orgNode);
    this.orgNode = orgNode;
  }

  /**
   * イミテーションノードを追加する.
   *
   * @param imitNode 追加するイミテーションノード
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void addImitation(T imitNode, UserOperationCommand userOpeCmd) {
    imitNodeList.add(imitNode);
    userOpeCmd.pushCmdOfAddImitation(imitNode, self());
  }

  /**
   * イミテーションノードを削除する.
   *
   * @param imitNode 削除するイミテーションノード
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void removeImitation(T imitNode, UserOperationCommand userOpeCmd) {
    imitNodeList.remove(imitNode);
    userOpeCmd.pushCmdOfRemoveImitation(imitNode, self());
  }

  @Override
  public Collection<T> getImitationList() {
    return Collections.unmodifiableList(imitNodeList);
  }

  /**
   * オリジナル - イミテーションの関係を削除する.
   *
   * @param imitToDelete 関係を削除するイミテーションノード
   * @param userOpeCmd undo 用コマンドオブジェクト
   */
  public void disconnectOrgImitRelation(T imitToDelete, UserOperationCommand userOpeCmd) {
    if (!imitToDelete.isImitationNode()) {
      throw new IllegalArgumentException(
          Util.INSTANCE.getCurrentMethodName() + " - try to disconnect non-imitaive node.");
    }
    removeImitation(imitToDelete, userOpeCmd);
    imitToDelete.setOriginal(null, userOpeCmd);
  }

  @Override
  public boolean isImitationNode() {
    return orgNode != null;
  }

  @Override
  public Imitatable findExistingOrCreateNewImit(BhNode oldNode, UserOperationCommand userOpeCmd) {
    BhNode outerTailOfOldNode = oldNode.findOuterNode(-1);
    for (T imit : imitNodeList) {
      //新しく入れ替わるノードの外部末尾ノードが最後に入れ替わったノードの外部末尾ノードと一致するイミテーションノードを入れ替えイミテーションノードとする
      if  (imit.getLastReplaced() != null) {
        if (!imit.isInWorkspace()
            && imit.getLastReplaced().findOuterNode(-1) == outerTailOfOldNode) {
          return imit;
        }
      }
    }
    return ImitationBuilder.buildFromImitIdOfAncestor(this, true, userOpeCmd);
  }

  @Override
  public boolean imitationNodeExists(ImitationId imitId) {
    return imitIdToImitNodeId.containsKey(imitId);
  }


  @Override
  public BhNodeId getImitationNodeId(ImitationId imitId) {
    Objects.requireNonNull(imitId);
    return imitIdToImitNodeId.get(imitId);
  }

  @Override
  public boolean isRemovable() {
    if (parentConnector == null) {
      return false;
    }
    //デフォルトノードは移動不可
    if (isDefault()) {
      return false;
    }
    return !parentConnector.isFixed();
  }

  @Override
  public boolean canBeReplacedWith(BhNode node) {
    if (getState() != BhNode.State.CHILD) {
      return false;
    }
    if (findRootNode().getState() != BhNode.State.ROOT_DIRECTLY_UNDER_WS) {
      return false;
    }
    // 同じ tree に含まれている場合置き換え不可
    if (node.isDescendantOf(this) || this.isDescendantOf(node)) {
      return false;
    }
    return parentConnector.isConnectableWith(node);
  }
}
