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

package net.seapanda.bunnyhop.node.view;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.regex.Pattern;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.Connector;
import net.seapanda.bunnyhop.node.view.factory.BhNodeViewFactory;
import net.seapanda.bunnyhop.node.view.style.BhNodeViewStyle.Arrangement;
import net.seapanda.bunnyhop.node.view.style.ChildArrangement;
import net.seapanda.bunnyhop.node.view.style.ConnectorOrientation;
import net.seapanda.bunnyhop.node.view.traverse.NodeViewWalker;
import net.seapanda.bunnyhop.ui.view.ViewConstructionException;
import net.seapanda.bunnyhop.utility.SimpleCache;
import net.seapanda.bunnyhop.utility.math.Vec2D;

/**
 * {@link BhNodeView} の集合を持つクラス.
 *
 * @author K.Koike
 */
public class BhNodeViewGroup {

  /** `\\...\$`. */
  private static final Pattern escapeDollar = Pattern.compile("^(\\\\)+\\$");

  /** このグループが子となる {@link BhNodeViewGroup} のリスト. */
  private final List<BhNodeViewGroup> subGroupList = new ArrayList<>();
  /** このグループを持つ {@link ConnectiveNodeView}. */
  private final ConnectiveNodeView parentView;
  /** このグループを持つ {@link BhNodeViewGroup}. */
  final BhNodeViewGroup parentGroup;
  /** このグループが内部描画ノードを持つグループの場合 true. */
  public final boolean inner; 
  /** ノードの配置を決めるパラメータ. */
  private Arrangement arrangement;
  /** コネクタ名とそのコネクタにつながる {@link BhNodeView}. */
  private final SequencedMap<String, BhNodeViewBase> childNameToNodeView = new LinkedHashMap<>();
  /** 疑似ビューのファクトリオブジェクト. */
  private final PseudoViewBuilder builder = new PseudoViewBuilder();
  private final Geometry geometry = new Geometry();

  /**
   * コンストラクタ.
   *
   * @param parentView このグループを持つConnectiveNode
   * @param inner このグループが外部描画ノードを持つグループの場合true
   */
  BhNodeViewGroup(ConnectiveNodeView parentView, boolean inner) {
    this.parentView = parentView;
    this.parentGroup = null;
    this.inner = inner;
  }

  /**
   * コンストラクタ.
   *
   * @param parentGroup このグループを持つ {@link BhNodeViewGroup}
   * @param inner このグループが内部描画ノードを持つグループの場合 true
   */
  BhNodeViewGroup(BhNodeViewGroup parentGroup, boolean inner) {
    this.parentView = null;
    this.parentGroup = parentGroup;
    this.inner = inner;
  }

  Geometry getGeometry() {
    return geometry;
  }

  /**
   * このノード以下のサブグループを作成する.
   *
   * @param arrangeParams ノード配置パラメータ
   * @param factory サブグループ内の疑似ビューを作成するのに使用するオブジェクト
   * @param isTemplate このサブグループがテンプレートノードビューの子グループである場合 true
   * @throws ViewConstructionException グループの初期化に失敗した
   */
  void buildSubGroup(Arrangement arrangeParams, BhNodeViewFactory factory, boolean isTemplate)
      throws ViewConstructionException {
    this.arrangement = arrangeParams;
    for (String cnctrName : arrangeParams.cnctrNames) {
      ChildViewBinding entry = createChildViewEntry(cnctrName, factory, isTemplate);
      childNameToNodeView.put(entry.name, entry.view);
      // 外部ノードをつなぐコネクタは1つだけとする
      if (!inner) {
        return;
      }
    }
    for (Arrangement subGroupParams : arrangeParams.subGroups) {
      var subGroup = new BhNodeViewGroup(this, inner);
      subGroup.buildSubGroup(subGroupParams, factory, isTemplate);
      subGroupList.add(subGroup);
    }
  }

  /** コネクタ指定文字列から子ビュー登録用エントリを生成する. */
  private ChildViewBinding createChildViewEntry(
      String cnctrName, BhNodeViewFactory factory, boolean isTemplate)
      throws ViewConstructionException {
    ChildViewBinding entry;
    // 疑似ビュー指定パターン
    if (PseudoViewBuilder.isPseudoView(cnctrName)) {
      entry = builder.build(cnctrName, factory, isTemplate);
    } else if (escapeDollar.matcher(cnctrName).find()) {
      // 先頭の `\` を取り除く.
      entry = new ChildViewBinding(cnctrName.substring(1), null);
    } else {
      entry = new ChildViewBinding(cnctrName, null);
    }
    return entry;
  }

  /**
   * BhNodeView を追加する. view の適切な追加先が見つからなかった場合, 失敗する.
   *
   * @param view 追加するノードビュー
   * @return 追加に成功した場合 true. 失敗した場合 false.
   */
  boolean addNodeView(BhNodeViewBase view) {
    Connector cnctr = view.getModel().map(BhNode::getParentConnector).orElse(null);
    if (cnctr == null) {
      return false;
    }
    String cnctrName = cnctr.getSymbolName();
    // このグループ内に追加すべき場所が見つかった
    if (childNameToNodeView.containsKey(cnctrName)) {
      childNameToNodeView.put(cnctrName, view);
      view.getTreeControl().setParentGroup(this);
      return true;
    } else {
      //サブグループに追加する
      for (BhNodeViewGroup subGroup : subGroupList) {
        if (subGroup.addNodeView(view)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * このグループが持つ {@code oldNodeView} を {@code newNodeView} と入れ替える.
   *
   * @param oldNodeView 入れ替えられる古いノード
   * @param newNodeView 入れ替える新しいノード
   */
  void replace(BhNodeViewBase oldNodeView, BhNodeViewBase newNodeView) {
    for (Map.Entry<String, BhNodeViewBase> entrySet : childNameToNodeView.entrySet()) {
      if (entrySet.getValue().equals(oldNodeView)) {
        entrySet.setValue(newNodeView);
        newNodeView.getTreeControl().setParentGroup(this);    //親をセット
        oldNodeView.getTreeControl().setParentGroup(null);    //親を削除
        return;
      }
    }
  }

  /**
   * このグループの親ノードを返す.
   *
   * <p>このグループが別のグループのサブグループでも, その上にある親ノードビューを返す.
   *
   * @return このグループの親ノードビュー
   */
  public ConnectiveNodeView getParentView() {
    if (parentView != null) {
      return parentView;
    }
    return parentGroup.getParentView();
  }

  /**
   * このグループの親グループを返す.
   *
   * @return このグループの親グループ. 存在しない場合は null.
   */
  public BhNodeViewGroup getParentGroup() {
    return parentGroup;
  }

  /**
   * visitor をサブグループに渡す.
   *
   * @param visitor サブグループに渡す visitor
   */
  public void sendToSubGroupList(NodeViewWalker visitor) {
    subGroupList.forEach(group -> group.accept(visitor));
  }

  /**
   * visitor を子ノードビューに渡す.
   *
   * @param visitor 子ノードビューに渡す visitor
   */
  public void sendToChildNode(NodeViewWalker visitor) {
    for (BhNodeView child : childNameToNodeView.values()) {
      if (child != null) {
        child.accept(visitor);
      }
    }
  }

  /** {@code visitor} にこのオブジェクトを渡す. */
  public void accept(NodeViewWalker visitor) {
    visitor.visit(this);
  }

  /** このグループが保持する疑似ビューを {@code parent} に子要素として追加する. */
  void addPseudoViewToGuiTree(Pane parent) {
    for (BhNodeView child : childNameToNodeView.values()) {
      if (isPseudoView(child)) {
        child.getTreeControl().addToTree(parent);
      }
    }
  }

  /** このグループが保持する疑似ビューを {@code parent} に子要素として追加する. */
  void addPseudoViewToGuiTree(Group parent) {
    for (BhNodeView child : childNameToNodeView.values()) {
      if (isPseudoView(child)) {
        child.getTreeControl().addToTree(parent);
      }
    }
  }

  /** このグループが保持する疑似ビューを親 GUI コンポーネントから取り除く. */
  void removePseudoViewFromGuiTree() {
    for (BhNodeView child : childNameToNodeView.values()) {
      if (isPseudoView(child)) {
        child.getTreeControl().removeFromTree();
      }
    }
  }

  private static boolean isPseudoView(BhNodeView view) {
    // 疑似ビューはモデルを持たない
    return view != null && view.getModel().isEmpty();
  }

  /**
   * このグループが外部描画ノードを持つグループかどうか調べる.
   *
   * @return このグループが外部描画ノードを持つグループである場合 true.
   */
  boolean isOuter() {
    return !inner;
  }

  /** 疑似ビューを作成する機能を提供するクラス. */
  private class PseudoViewBuilder {
    /** 疑似ビュー指定パターン `${a}{b}...{z}`. */
    private static final Pattern embedded =
        Pattern.compile("^\\$(\\{(((\\\\\\{)|(\\\\})|[^{}])*)}){2,}$");

    /** 疑似ビューの ID. */
    private int pseudoViewId = 0;

    /** {@code pattern} が疑似ビューを表す文字列か調べる. */
    private static boolean isPseudoView(String pattern) {
      return embedded.matcher(pattern).find();
    }

    private ChildViewBinding build(String definition, BhNodeViewFactory factory, boolean isTemplate)
        throws ViewConstructionException {
      return new ChildViewBinding(
          "$" + pseudoViewId++,
          createPseudoView(definition, factory, isTemplate));
    }

    /** このグループの中で定義された MVC 構造を持たない {@link BhNodeView} を作成する. */
    private BhNodeViewBase createPseudoView(
        String definition, BhNodeViewFactory factory, boolean isTemplate)
        throws ViewConstructionException {
      BhNodeView nodeView = factory.createViewOf(definition, isTemplate);
      if (nodeView instanceof BhNodeViewBase view) {
        view.getTreeControl().setParentGroup(BhNodeViewGroup.this);
        return view;
      }
      throw new ViewConstructionException("Invalid pseudo view type.  (%s)".formatted(nodeView));
    }
  }

  /** ノードビューグループの幾何形状に関する操作を提供するクラス. */
  class Geometry {
    /** 親ノードまたはグループからの相対位置. */
    private final Vec2D relativePos = new Vec2D(0.0, 0.0);
    /** このグループのサイズのキャッシュデータ. */
    private final SimpleCache<Vec2D> sizeCache = new SimpleCache<>(new Vec2D());

    /**
     * このグループ以下の子孫要素のワークスペース上の位置を設定する.
     *
     * @param posX グループ左上の X 位置
     * @param posY グループ左上の Y 位置
     */
    void setTreePosition(double posX, double posY) {
      for (BhNodeViewBase child : childNameToNodeView.values()) {
        if (child == null) {
          continue;
        }
        Vec2D offset = child.getGeometry().getRelativePosition();
        child.getGeometry().updateTreePosition(posX + offset.x, posY + offset.y);
      }
      subGroupList.forEach(subGroup -> {
        Vec2D subGroupRelPos = subGroup.getGeometry().getRelativePosition();
        subGroup.getGeometry().setTreePosition(posX + subGroupRelPos.x, posY + subGroupRelPos.y);
      });
    }

    /**
     * 親ノードまたはグループからの相対位置を取得する.
     *
     * @return 親ノードまたは親グループからの相対位置
     */
    Vec2D getRelativePosition() {
      return new Vec2D(relativePos.x, relativePos.y);
    }

    /**
     * 親ノードまたはグループからの相対位置をセットする.
     *
     * @param posX 親ノードまたは親グループからの X 相対位置
     * @param posY 親ノードまたは親グループからの Y 相対位置
     */
    void setRelativePosition(double posX, double posY) {
      relativePos.x = posX;
      relativePos.y = posY;
    }

    /**
     * 子孫要素のサイズが変わったことをこのグループに伝える.
     * 親要素にもこの通知を伝える.
     */
    void notifySubTreeSizeChanged() {
      sizeCache.setDirty(true);
      if (parentGroup != null) {
        BhNodeViewGroup.this.parentGroup.getGeometry().notifySubTreeSizeChanged();
      }
      if (parentView != null) {
        BhNodeViewGroup.this.parentView.getSizeChangeNotifier().notifySubtreeSizeChanged();
      }
    }

    /**
     * このグループのサイズを取得する.
     */
    Vec2D getSize() {
      if (!sizeCache.isDirty()) {
        return new Vec2D(sizeCache.getVal());
      }
      Vec2D childSumLen = calcChildSumLen();
      var size = new Vec2D(arrangement.paddingLeft, arrangement.paddingTop);
      int numSpace = Math.max(0, childNameToNodeView.size() + subGroupList.size() - 1);

      // グループの中が縦並び
      if (arrangement.arrangement == ChildArrangement.COLUMN) {
        size.x += calcChildMaxWidth() + arrangement.paddingRight;
        size.y += childSumLen.y + numSpace * arrangement.space + arrangement.paddingBottom;
      // グループの中が横並び
      } else {
        size.x += childSumLen.x + numSpace * arrangement.space + arrangement.paddingRight;
        size.y += calcChildMaxHeight() + arrangement.paddingBottom;
      }
      sizeCache.update(new Vec2D(size));
      return size;
    }

    /** 子要素の縦横の長さの総計を求める. */
    private Vec2D calcChildSumLen() {
      var childSumLen = new Vec2D(0, 0);
      for (BhNodeViewGroup subGroup : subGroupList) {
        childSumLen.add(subGroup.getGeometry().getSize());
      }

      for (BhNodeViewBase child : childNameToNodeView.values()) {
        if (child == null) {
          continue;
        }
        Vec2D childNodeSize = child.getGeometry().getNodeTreeSize(true);
        childSumLen.add(childNodeSize);
      }
      return childSumLen;
    }

    /** 子要素の幅の最大値を求める. */
    private double calcChildMaxWidth() {
      double maxSubGroupWidth = 0;
      for (BhNodeViewGroup subGroup : subGroupList) {
        maxSubGroupWidth = Math.max(maxSubGroupWidth, subGroup.getGeometry().getSize().x);
      }
      double maxCnctrWidth = 0;
      double maxBodyWidth = 0;
      for (BhNodeViewBase child : childNameToNodeView.values()) {
        if (child == null) {
          continue;
        }
        Vec2D cnctrSize = new Vec2D(0, 0);
        Vec2D childBodySize = new Vec2D(0, 0);
        if (child.getStyle().connectorOrientation == ConnectorOrientation.TOP) {
          childBodySize = child.getGeometry().getNodeTreeSize(true);
        } else if (child.getStyle().connectorOrientation == ConnectorOrientation.LEFT) {
          childBodySize = child.getGeometry().getNodeTreeSize(false);
          cnctrSize = child.getGeometry().getConnectorSize();
        }
        maxBodyWidth = Math.max(maxBodyWidth, childBodySize.x);
        maxCnctrWidth = Math.max(cnctrSize.x, maxCnctrWidth);
      }
      return Math.max(maxSubGroupWidth, maxCnctrWidth + maxBodyWidth);
    }

    /** 子要素の高さの最大値を求める. */
    private double calcChildMaxHeight() {
      double maxSubGroupHeight = 0;
      for (BhNodeViewGroup subGroup : subGroupList) {
        maxSubGroupHeight = Math.max(maxSubGroupHeight, subGroup.getGeometry().getSize().y);
      }

      double maxCnctrHeight = 0;
      double maxBodyHeight = 0;
      for (BhNodeViewBase child : childNameToNodeView.values()) {
        if (child == null) {
          continue;
        }
        Vec2D cnctrSize = new Vec2D(0, 0);
        Vec2D childBodySize = new Vec2D(0, 0);
        if (child.getStyle().connectorOrientation == ConnectorOrientation.TOP) {
          childBodySize = child.getGeometry().getNodeTreeSize(false);
          cnctrSize = child.getGeometry().getConnectorSize();
        } else if (child.getStyle().connectorOrientation == ConnectorOrientation.LEFT) {
          childBodySize = child.getGeometry().getNodeTreeSize(true);
        }
        maxBodyHeight = Math.max(maxBodyHeight, childBodySize.y);
        maxCnctrHeight = Math.max(cnctrSize.y, maxCnctrHeight);
      }
      return Math.max(maxSubGroupHeight, maxCnctrHeight + maxBodyHeight);
    }

    /** 子孫要素の親からの相対位置を更新する. */
    void updateDescendantRelativePositions() {
      Vec2D childRelPos = updateChildNodeRelativePositions(
          new Vec2D(arrangement.paddingLeft, arrangement.paddingTop));
      updateChildGroupRelativePositions(childRelPos);
    }

    /**
     * 子ノード以下の要素の相対位置を更新する.
     *
     * <p>子ノードは中央に揃える
     *
     * @param relPos 最初の子ノードの親からの相対位置.
     * @return 次のグループの相対位置.
     */
    private Vec2D updateChildNodeRelativePositions(Vec2D relPos) {
      Vec2D groupSize = getSize();
      for (BhNodeViewBase child : childNameToNodeView.values()) {
        if (child == null) {
          continue;
        }
        Vec2D childNodeSize = child.getGeometry().getNodeTreeSize(true);
        Vec2D bodyPosFromCnctr = child.getGeometry().getBodyPosFromConnector();
        Vec2D offset = new Vec2D(0, 0);
        // グループの中が縦並び
        if (arrangement.arrangement == ChildArrangement.COLUMN) {
          offset.x = ((groupSize.x - childNodeSize.x) / 2) + Math.max(0, bodyPosFromCnctr.x);
          offset.y = Math.max(0, bodyPosFromCnctr.y);

        // グループの中が横並び
        } else {
          offset.x = Math.max(0, bodyPosFromCnctr.x);
          offset.y = ((groupSize.y - childNodeSize.y) / 2) + Math.max(0, bodyPosFromCnctr.y);
        }
        child.getGeometry().setRelativePosition(relPos.x + offset.x, relPos.y + offset.y);
        calcNextRelativePosOffset(relPos, childNodeSize);
        child.getGeometry().updateDescendantRelativePositions();
      }
      return relPos;
    }

    /**
     * 子グループ以下の要素の相対位置を更新する.
     *
     * <p>子グループは左上に揃える
     *
     * @param relPos 最初の子グループの親からの相対位置.
     */
    private void updateChildGroupRelativePositions(Vec2D relPos) {
      for (BhNodeViewGroup subGroup : subGroupList) {
        subGroup.getGeometry().setRelativePosition(relPos.x, relPos.y);
        Vec2D subGroupSize = subGroup.getGeometry().getSize();
        calcNextRelativePosOffset(relPos, subGroupSize);
        subGroup.getGeometry().updateDescendantRelativePositions();
      }
    }

    /**
     * 子要素の相対位置とその大きさから次の子要素の相対位置を計算する.
     *
     * @param relPos 子要素の相対位置.  次の子要素の相対位置で更新される.
     * @param childSize 子要素の大きさ
     */
    private void calcNextRelativePosOffset(Vec2D relPos, Vec2D childSize) {
      //グループの中が縦並び
      if (arrangement.arrangement == ChildArrangement.COLUMN) {
        relPos.y += childSize.y + arrangement.space;
      //グループの中が横並び
      } else {
        relPos.x += childSize.x + arrangement.space;
      }
    }
  }

  record ChildViewBinding(String name, BhNodeViewBase view) {}
}
