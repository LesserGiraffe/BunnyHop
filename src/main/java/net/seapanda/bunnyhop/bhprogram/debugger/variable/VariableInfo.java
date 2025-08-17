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

package net.seapanda.bunnyhop.bhprogram.debugger.variable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import net.seapanda.bunnyhop.model.node.BhNode;

/**
 * 変数情報とそれに関連するスレッド, コールスタックの情報を保持するクラス.
 *
 * @author K.Koike
 */
public class VariableInfo {

  public final long threadId;
  public final int frameIdx;
  private final Map<BhNode, Variable> nodeToVar;

  /**
   * コンストラクタ.
   *
   * @param threadId {@code variables} を持つコールスタックと関連するスレッドの ID
   * @param frameIdx {@code variables} を持つスタックフレームの ID
   * @param variables このオブジェクトに格納する変数情報
   */
  public VariableInfo(long threadId, int frameIdx, SequencedCollection<Variable> variables) {
    if (threadId <= 0) {
      throw new IllegalArgumentException("Invalid Thread ID (%s)".formatted(threadId));
    }
    if (frameIdx <= -1) {
      throw new IllegalArgumentException("Invalid Frame Index (%s)".formatted(frameIdx));
    }
    this.threadId = threadId;
    this.frameIdx = frameIdx;
    nodeToVar = new LinkedHashMap<>();
    for (Variable variable : variables) {
      nodeToVar.put(variable.node, variable);
    }
  }

  /**
   * コンストラクタ.
   *
   * @param variables このオブジェクトに格納する変数情報
   */
  public VariableInfo(SequencedCollection<Variable> variables) {
    this.threadId = -1;
    this.frameIdx = -1;
    nodeToVar = new LinkedHashMap<>();
    for (Variable variable : variables) {
      nodeToVar.put(variable.node, variable);
    }
  }

  /**
   * このオブジェクトが持つ変数情報を返す.
   */
  public SequencedCollection<Variable> getVariables() {
    return new ArrayList<>(nodeToVar.values());
  }

  /**
   * このオブジェクトが持つ変数情報がローカル変数のものであった場合, 関連するスレッドの ID を返す. <br>
   * このオブジェクトが持つ変数情報がグローバル変数のものであった場合は empty を返す.
   */
  public Optional<Long> getThreadId() {
    if (threadId < 0) {
      return Optional.empty();
    }
    return Optional.of(threadId);
  }

  /**
   * このオブジェクトが持つ変数情報がローカル変数のものであった場合, 関連するスタックフレームのインデックスを返す. <br>
   * このオブジェクトが持つ変数情報がグローバル変数のものであった場合は empty を返す.
   */
  public Optional<Integer> getFrameIdx() {
    if (frameIdx < 0) {
      return Optional.empty();
    }
    return Optional.of(frameIdx);
  }

  /**
   * このスタックフレームに変数情報を追加する.
   *
   * <p>追加された変数情報が {@link ScalarVariable} でかつ, 既に同じ {@link BhNode} を持つ
   * {@link ScalarVariable} が存在していた場合, 新しい値で上書きされる.
   *
   * <p>追加された変数情報が {@link ListVariable} でかつ, 既に同じ {@link BhNode} を持つ
   * {@link ListVariable} が存在していた場合, 既存の {@link ListVariable} に新しい要素が追加される.
   * その際, 重複インデックスの値は追加した変数情報のもので置き換えられる.
   */
  public void addVariables(List<Variable> variables) {
    for (Variable variable : variables) {
      Variable existing = this.nodeToVar.get(variable.node);
      switch (existing) {
        case null -> nodeToVar.put(variable.node, variable);
        case ScalarVariable registered
            when variable instanceof ScalarVariable added ->
            registered.setValue(added.getValue());
        case ListVariable registered
            when variable instanceof ListVariable added ->
            registered.addItems(added.getItems());
        default -> {
          throw new AssertionError("Unknown variable type");
        }
      }
    }
  }
}
