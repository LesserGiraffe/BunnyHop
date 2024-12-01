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

/**
 * コンパイルオプションを格納するクラス.
 *
 * @author K.Koike
 */
public class CompileOption {

  /** デバッグ用コードを追加する場合 true. */
  public final boolean isDebug;
  /** 算術演算で結果を実数に保つ場合 true. */
  public final boolean keepRealNumber;
  /** 例外を追跡するためのコードを追加する場合 true. */
  public final boolean traceException;
  /** ソースコードにコメントを追加する場合 true. */
  public final boolean withComments;

  private CompileOption(Builder builder) {
    this.isDebug = builder.isDebug;
    this.keepRealNumber = builder.keepRealNumber;
    this.traceException = builder.traceException;
    this.withComments = builder.withComments;
  }

  /** {@link CompileOption} のビルダークラス. */
  public static class Builder {

    private boolean isDebug = true;
    private boolean keepRealNumber = true;
    private boolean traceException = true;
    private boolean withComments = true;

    public Builder keepRealNumber(boolean keepRealNumber) {
      this.keepRealNumber = keepRealNumber;
      return this;
    }

    public Builder traceException(boolean traceException) {
      this.traceException = traceException;
      return this;
    }

    public Builder withComments(boolean withComments) {
      this.withComments = withComments;
      return this;
    }

    public CompileOption build() {
      return new CompileOption(this);
    }
  }
}
