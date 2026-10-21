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

package net.seapanda.bunnyhop.compiler;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * コンパイルオプションを格納するクラス.
 *
 * @author K.Koike
 */
public class CompileOption {

  /** 処理中のノードのインスタンス ID をスレッドコンテキストに設定するコードを追加する. */
  public final boolean addNodeInstIdToContext;
  /** コールスタックに関数呼び出しノードのインスタンス ID を追加および削除するコードを追加する. */
  public final boolean addNodeInstIdToCallStack;
  /** 変数スタックにアクセサオブジェクトを追加および削除するコードを追加する. */
  public final boolean addVarAccessorToVarStack;
  /** 条件付きで一時停止するコードを追加する. */
  public final boolean addConditionalWait;
  /** ソースコードにコメントを追加する場合 true. */
  public final boolean withComments;
  /** メインルーチンを起動する際に BhProgram が受け取るイベントの名前. */
  public final String mainRoutineLaunchEvent;
  /** 出力ファイルのパス. */
  public final Path outFile;
  /** コンパイルして生成されたコードの実行環境. */
  public final RuntimeEnvironment runtimeEnv;

  private CompileOption(Builder builder) {
    addNodeInstIdToContext = builder.isDebug;
    addNodeInstIdToCallStack = builder.isDebug;    
    addVarAccessorToVarStack = builder.isDebug;
    addConditionalWait = builder.isDebug;
    this.withComments = builder.withComments;
    this.mainRoutineLaunchEvent = builder.mainRoutineLaunchEvent;
    this.outFile = builder.outFile;
    this.runtimeEnv = builder.runtimeEnv;
  }

  /** {@link Builder} オブジェクトを作成する. */
  public static Builder builder() {
    return new Builder();
  }

  /** {@link CompileOption} のビルダークラス. */
  public static class Builder {

    private Builder() {}

    private boolean isDebug = true;
    private boolean withComments = true;
    private String mainRoutineLaunchEvent = "PROGRAM_START";
    private RuntimeEnvironment runtimeEnv = RuntimeEnvironment.LOCAL;

    public Path outFile = Paths.get(".");

    /**
     * 生成するソースコードにコメントを追加するかどうかを設定する.
     *
     * @param val ソースコードにコメントを追加する場合 true
     * @return このオブジェクト
     */
    public Builder withComments(boolean val) {
      this.withComments = val;
      return this;
    }

    /**
     * メインルーチンを起動する際に BhProgram が受け取るイベントの名前を設定する.
     *
     * @param val メインルーチンを起動する際に BhProgram が受け取るイベントの名前
     * @return このオブジェクト
     */
    public Builder mainRoutineLaunchEvent(String val) {
      Objects.requireNonNull(val);
      mainRoutineLaunchEvent = val;
      return this;
    }

    /**
     * コンパイル結果の出力先ファイルのパスを設定する.
     *
     * @param val コンパイル結果の出力先ファイルのパス
     * @return このオブジェクト
     */
    public Builder outFile(Path val) {
      Objects.requireNonNull(val);
      this.outFile = val;
      return this;
    }

    /**
     * 生成されたコードの実行環境を設定する.
     *
     * @param val 生成されたコードの実行環境
     * @return このオブジェクト
     */
    public Builder runtimeEnvironment(RuntimeEnvironment val) {
      Objects.requireNonNull(val);
      this.runtimeEnv = val;
      return this;
    }

    /**
     * このオブジェクトに設定した内容から {@link CompileOption} を作成する.
     *
     * @return 作成した {@link CompileOption}
     */
    public CompileOption build() {
      return new CompileOption(this);
    }
  }
}
