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
import java.util.SequencedCollection;

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
  /** プログラムが依存するライブラリのパス. */
  public final SequencedCollection<String> libPaths;
  /** ソースコードにコメントを追加する場合 true. */
  public final boolean withComments;
  /** メインルーチンを起動する際に BhProgram が受け取るイベントの名前. */
  public final String mainRoutineLaunchEvent;
  /** メインルーチンの名前. */
  public final String mainRoutineName;
  /** 出力ファイルのパス. */
  public final Path outFile;

  private CompileOption(Builder builder) {
    addNodeInstIdToContext = builder.isDebug;
    addNodeInstIdToCallStack = builder.isDebug;    
    addVarAccessorToVarStack = builder.isDebug;
    addConditionalWait = builder.isDebug;
    this.libPaths = builder.libPaths;
    this.withComments = builder.withComments;
    this.mainRoutineLaunchEvent = builder.mainRoutineLaunchEvent;
    this.mainRoutineName = builder.mainRoutineName;
    this.outFile = builder.outFile;
  }

  /** {@link Builder} オブジェクトを作成する. */
  public static Builder builder() {
    return new Builder();
  }

  /** {@link CompileOption} のビルダークラス. */
  public static class Builder {

    private Builder() {}

    private boolean isDebug = true;
    private SequencedCollection<String> libPaths;
    private boolean withComments = true;
    private String mainRoutineLaunchEvent = "PROGRAM_START";
    private String mainRoutineName = "_bhMain";

    public Path outFile = Paths.get(".");

    /**
     * プログラムが依存するライブラリのパスを設定する.
     *
     * @param libPaths プログラムが依存するライブラリのパスを設定する.
     * @return このオブジェクト
     */
    public Builder libPaths(SequencedCollection<String> libPaths) {
      Objects.requireNonNull(libPaths);
      this.libPaths = libPaths;
      return this;
    }

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
     * メインルーチンの名前を設定する.
     *
     * @param val メインルーチンの名前
     * @return このオブジェクト
     */
    public Builder mainRoutineName(String val) {
      Objects.requireNonNull(val);
      mainRoutineName = val;
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
     * このオブジェクトに設定した内容から {@link CompileOption} を作成する.
     *
     * @return 作成した {@link CompileOption}
     */
    public CompileOption build() {
      return new CompileOption(this);
    }
  }
}
