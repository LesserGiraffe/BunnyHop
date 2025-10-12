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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;
import net.seapanda.bunnyhop.bhprogram.common.BhSymbolId;
import net.seapanda.bunnyhop.model.node.BhNode;
import net.seapanda.bunnyhop.utility.function.ConsumerInvoker;
import net.seapanda.bunnyhop.utility.function.SimpleConsumerInvoker;

/**
 * 変数情報とそれに関連するスレッド, コールスタックの情報を保持するクラス.
 *
 * @author K.Koike
 */
public class VariableInfo {

  /** このオブジェクトが特定のスタックフレームの変数情報を保持する場合, そのスタックフレームの ID を保持する. */
  private final StackFrameId stackFrameId;
  private final Map<BhSymbolId, Variable> varIdToVar = new LinkedHashMap<>();
  private final CallbackRegistry cbRegistry = new CallbackRegistry();

  /**
   * コンストラクタ.
   *
   * @param stackFrameId {@code variables} を持つスタックフレームの ID
   * @param variables このオブジェクトに格納する変数情報
   */
  public VariableInfo(StackFrameId stackFrameId, SequencedCollection<Variable> variables) {
    Objects.requireNonNull(stackFrameId, "stackFrameId cannot be null");
    this.stackFrameId = stackFrameId;
    for (Variable variable : variables) {
      varIdToVar.put(variable.id, variable);
    }
  }

  /**
   * コンストラクタ.
   *
   * @param stackFrameId {@code variable} を持つスタックフレームの ID
   * @param variable このオブジェクトに格納する変数情報
   */
  public VariableInfo(StackFrameId stackFrameId, Variable variable) {
    this(stackFrameId, List.of(variable));
  }

  /**
   * コンストラクタ.
   *
   * @param stackFrameId {@code variable} を持つスタックフレームの ID
   */
  public VariableInfo(StackFrameId stackFrameId) {
    this(stackFrameId, new ArrayList<>());
  }

  /**
   * コンストラクタ.
   *
   * @param variables このオブジェクトに格納する変数情報
   */
  public VariableInfo(SequencedCollection<Variable> variables) {
    stackFrameId = null;
    for (Variable variable : variables) {
      varIdToVar.put(variable.id, variable);
    }
  }

  /**
   * コンストラクタ.
   *
   * @param variable このオブジェクトに格納する変数情報
   */
  public VariableInfo(Variable variable) {
    this(List.of(variable));
  }

  /** コンストラクタ. */
  public VariableInfo() {
    this(new ArrayList<>());
  }

  /** このオブジェクトが持つ変数情報を返す. */
  public SequencedCollection<Variable> getVariables() {
    return new ArrayList<>(varIdToVar.values());
  }

  /** このオブジェクトが特定のスタックフレームの変数情報を保持する場合, そのスタックフレームの ID を返す. */
  public Optional<StackFrameId> getStackFrameId() {
    return Optional.ofNullable(stackFrameId);
  }

  /**
   * このスタックフレームに変数情報を追加する.
   *
   * <p>追加された変数情報が {@link ScalarVariable} でかつ, 既に同じ {@link BhNode} を持つ
   * {@link ScalarVariable} が存在していた場合, 新しい値で上書きされる.
   *
   * <p>追加された変数情報が {@link ListVariable} でかつ, 既に同じ {@link BhNode} を持つ
   * {@link ListVariable} が存在していた場合, 既存の {@link ListVariable} に新しい要素が追加される.
   * その際, 重複したインデックスの値は追加した変数情報のもので置き換えられる.
   */
  public void addVariables(SequencedCollection<Variable> variables) {
    List<Variable> newVars = new ArrayList<>();
    for (Variable variable : variables) {
      Variable existing = varIdToVar.get(variable.id);
      switch (existing) {
        case null -> {
          varIdToVar.put(variable.id, variable);
          newVars.add(variable);
        }
        case ScalarVariable registered
            when variable instanceof ScalarVariable added ->
            registered.setValue(added.getValue().orElse(null));
        case ListVariable registered
            when variable instanceof ListVariable added ->
            registered.addItems(added.getItems());
        default -> {
          throw new AssertionError("Unknown variable type");
        }
      }
    }
    if (!newVars.isEmpty()) {
      cbRegistry.onVarsAddedInvoker.invoke(new VariablesAddedEvent(this, newVars));
    }
  }

  /**
   * このスタックフレームが持つ数情報を削除する.
   *
   * @param variables 削除する変数一覧
   */
  public void removeVariables(Collection<Variable> variables) {
    List<Variable> removedVars = new ArrayList<>();
    for (Variable variable : variables) {
      Variable removed = varIdToVar.remove(variable.id);
      if (removed != null) {
        removedVars.add(removed);
      }
    }
    if (!removedVars.isEmpty()) {
      cbRegistry.onVarsRemovedInvoker.invoke(new VariablesRemovedEvent(this, removedVars));
    }
  }

  /** このスタックフレームが持つ数情報を全て削除する. */
  public void clearVariables() {
    removeVariables(new ArrayList<>(varIdToVar.values()));
  }

  /**
   * このオブジェクトに対するイベントハンドラの追加と削除を行うオブジェクトを返す.
   *
   * @return このデバッガに対するイベントハンドラの追加と削除を行うオブジェクト
   */
  public CallbackRegistry getCallbackRegistry() {
    return cbRegistry;
  }

  /** {@link VariableInfo} に対するイベントハンドラの登録および削除操作を提供するクラス. */
  public class CallbackRegistry {

    /** 関連する {@link VariableInfo} に変数情報が追加されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<VariablesAddedEvent> onVarsAddedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連する {@link VariableInfo} から変数情報が削除されたときのイベントハンドラを管理するオブジェクト. */
    private final ConsumerInvoker<VariablesRemovedEvent> onVarsRemovedInvoker =
        new SimpleConsumerInvoker<>();

    /** 関連する {@link VariableInfo} に変数情報が追加されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<VariablesAddedEvent>.Registry getOnVariablesAdded() {
      return onVarsAddedInvoker.getRegistry();
    }

    /** 関連する {@link VariableInfo} に変数情報が追加されたときのイベントハンドラのレジストリを取得する. */
    public ConsumerInvoker<VariablesRemovedEvent>.Registry getOnVariablesRemoved() {
      return onVarsRemovedInvoker.getRegistry();
    }
  }

  /**
   * {@link VariableInfo} に変数情報が追加されたときの情報を格納したレコード.
   *
   * @param varInfo 変数情報が追加された {@link VariableInfo}
   * @param added 追加された変数情報
   */
  public record VariablesAddedEvent(VariableInfo varInfo, SequencedCollection<Variable> added) {}

  /**
   * {@link VariableInfo} が持つ変数情報が削除されたときの情報を格納したレコード.
   *
   * @param varInfo 変数情報が削除された {@link VariableInfo}
   * @param removed 削除された変数情報
   */
  public record VariablesRemovedEvent(VariableInfo varInfo, Collection<Variable> removed) {}
}
