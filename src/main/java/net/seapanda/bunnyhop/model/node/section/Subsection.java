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

package net.seapanda.bunnyhop.model.node.section;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.model.node.Connector;
import net.seapanda.bunnyhop.model.node.parameter.ConnectorId;
import net.seapanda.bunnyhop.model.node.syntaxsymbol.SyntaxSymbol;
import net.seapanda.bunnyhop.model.traverse.BhNodeWalker;
import net.seapanda.bunnyhop.undo.UserOperation;

/**
 * サブグループとして Section の集合を持つクラス.
 *
 * @author K.Koike
 */
public class Subsection extends Section {

  List<Section> subsectionList = new ArrayList<>();

  /**
   * コンストラクタ.
   *
   * @param symbolName 終端, 非終端記号名
   * @param subsectionList サブセクションリスト
   */
  public Subsection(String symbolName, Collection<Section> subsectionList) {
    super(symbolName);
    this.subsectionList.addAll(subsectionList);
  }

  /**
   * コピーコンストラクタ.
   *
   * @param org コピー元オブジェクト
   */
  private Subsection(Subsection org) {
    super(org);
  }

  @Override
  public Subsection copy(
      Predicate<? super BhNode> fnIsNodeToBeCopied, UserOperation userOpe) {
    Subsection newSubsection = new Subsection(this);
    subsectionList.forEach(section -> {
      Section newSection = section.copy(fnIsNodeToBeCopied, userOpe);
      newSection.setParent(newSubsection);
      newSubsection.subsectionList.add(newSection);
    });
    return newSubsection;
  }

  @Override
  public void accept(BhNodeWalker visitor) {
    visitor.visit(this);
  }

  /**
   * visitor をこのセクションの下のサブセクションに渡す.
   *
   * @param visitor サブグループに渡す visitor
   */
  public void sendToSubsections(BhNodeWalker visitor) {
    subsectionList.forEach(subsection -> subsection.accept(visitor));
  }

  @Override
  public void findDescendantOf(
      int generation, boolean toBottom, List<SyntaxSymbol> foundSymbolList, String... symbolNames) {
    if (generation == 0) {
      for (String symbolName : symbolNames) {
        if (symbolNameMatches(symbolName)) {
          foundSymbolList.add(this);
        }
      }
      if (!toBottom) {
        return;
      }
    }

    int childLevel = generation - 1;
    for (Section subsection : subsectionList) {
      subsection.findDescendantOf(
          Math.max(0, childLevel), toBottom, foundSymbolList, symbolNames);
    }
  }

  @Override
  public BhNode findOuterNode(int generation) {
    for (int i = subsectionList.size() - 1; i >= 0; --i) {
      BhNode outerNode = subsectionList.get(i).findOuterNode(generation);
      if (outerNode != null) {
        return outerNode;
      }
    }
    return null;
  }

  @Override
  public Connector findConnector(ConnectorId id) {
    for (var subsection : subsectionList) {
      Connector cnctr = subsection.findConnector(id);
      if (cnctr != null) {
        return cnctr;
      }
    }
    return null;
  }

  @Override
  public void show(int depth) {
    var parentInstId =
        (parentNode != null) ? parentNode.getSerialNo() : parentSection.getSerialNo();
    System.out.println("%s<Subsection  name=%s  parent=%s>  %s"
        .formatted(indent(depth), getSymbolName(), parentInstId, getSerialNo()));
    subsectionList.forEach((connector -> connector.show(depth + 1)));
  }
}
