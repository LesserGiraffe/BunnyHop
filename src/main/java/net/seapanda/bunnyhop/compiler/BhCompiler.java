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
import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.syntaxsymbol.InstanceId;

/**
 * BhNode をコンパイル機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface BhCompiler {

  /**
   * {@link BhNode} をコンパイルし, 作成されたファイルのパスを返す.
   *
   * @param sourceSet コンパイルの対象となる {@link BhNode} を提供するオブジェクト
   * @param option コンパイルオプション
   * @return コンパイルした結果作成されたファイルのパス
   * @throws CompileError コンパイル中にエラーが発生した場合
   */
  Path compile(SourceSet sourceSet, CompileOption option)
      throws CompileError;

  /** エントリポイントとなるノードの処理を呼ぶ関数の ID. */
  InstanceId mainRoutineId();
}
