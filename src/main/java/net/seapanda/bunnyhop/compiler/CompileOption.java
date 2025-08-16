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
import net.seapanda.bunnyhop.common.BhConstants;
import net.seapanda.bunnyhop.utility.Utility;

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
  /** 変数スタックに変数に対するアクセサを追加および削除するコードを追加する. */
  public final boolean addVarAccessorToVarStack;
  /** 条件付きで一時停止するコードを追加する. */
  public final boolean addConditionalWait;
  /** ソースコードにコメントを追加する場合 true. */
  public final boolean withComments;
  /** 出力ファイルのパス. */
  public final Path outFile;

  private CompileOption(Builder builder) {
    addNodeInstIdToContext = builder.isDebug;
    addNodeInstIdToCallStack = builder.isDebug;    
    addVarAccessorToVarStack = builder.isDebug;
    addConditionalWait = builder.isDebug;
    this.withComments = builder.withComments;
    this.outFile = builder.outFile;
  }

  /** {@link CompileOption} のビルダークラス. */
  public static class Builder {

    private boolean isDebug = true;
    private boolean withComments = true;
    public Path outFile = Paths.get(
        Utility.execPath, BhConstants.Path.Dir.COMPILED, BhConstants.Path.File.APP_FILE_NAME_JS);

    public Builder withComments(boolean withComments) {
      this.withComments = withComments;
      return this;
    }

    public Builder outFile(Path outFile) {
      this.outFile = outFile;
      return this;
    }

    public CompileOption build() {
      return new CompileOption(this);
    }
  }
}
