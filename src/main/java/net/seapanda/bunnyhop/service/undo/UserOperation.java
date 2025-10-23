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

package net.seapanda.bunnyhop.service.undo;

import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * undo/redo 用コマンドクラス.
 *
 * @author K.Koike
 */
public class UserOperation {

  public UserOperation() {}

  /** このオブジェクトが表す操作を構成するサブ操作のリスト. */
  private final Deque<SubOperation> subOpeList = new LinkedList<>();

  /**
   * このコマンドの逆の操作を行う (例えば, ノード追加ならノード削除を行う).
   *
   * @return このコマンドの逆の操作を表す UserOperationCommand オブジェクトを返す. <br>
   *          つまり, 戻りオブジェクトの doInverseOperation はこのコマンドの元になった操作を行う
   */
  UserOperation doInverseOperation() {
    UserOperation inverseCmd = new UserOperation();
    while (!subOpeList.isEmpty()) {
      subOpeList.removeLast().doInverseOperation(inverseCmd);
    }
    return inverseCmd;
  }

  /**
   * サブ操作の数を返す,.
   *
   * @return サブ操作の数
   */
  public int getNumSubOps() {
    return subOpeList.size();
  }

  /** for debug. */
  public void printSubOpeList() {
    for (SubOperation subope : subOpeList) {
      System.out.println("subope  " + subope);
    }
  }

  /**
   * 特定の操作をコマンド化してサブ操作リストに加える.
   *
   * @param inverseCmd コマンド化した操作の逆の操作を行う関数オブジェクト.
   */
  public void pushCmd(Consumer<UserOperation> inverseCmd) {
    subOpeList.addLast(new AnonymousCmd(inverseCmd));
  }

  /** このコマンドに追加された全てのサブ操作を削除する. */
  public void clearCmds() {
    subOpeList.clear();
  }

  /** {@link UserOperation} を構成するサブ操作. */
  interface SubOperation {
    /**
     * このSubOperation の逆の操作を行う.
     *
     * @param inverseCmd このサブ操作の逆の操作を作るための UserOperationCommand オブジェクト
     */
    void doInverseOperation(UserOperation inverseCmd);
  }

  /** 特定の操作の逆の操作を関数オブジェクトとして保持するクラス. */
  private static class AnonymousCmd implements SubOperation {

    /** 逆操作. */
    private final Consumer<UserOperation> fnInvert;

    /**
     * コンストラクタ.
     *
     * @param fnInvert 逆操作を行う関数オブジェクト
     */
    public AnonymousCmd(Consumer<UserOperation> fnInvert) {
      this.fnInvert = fnInvert;
    }

    @Override
    public void doInverseOperation(UserOperation inverseCmd) {
      fnInvert.accept(inverseCmd);
    }
  }
}
