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

/** 生成するコードで使用するキーワード.
 *
 * @author K.Koike
 */
public class Keywords {

  public static final String newLine = "\r\n";

  /** 変数や関数に付く接頭辞. */
  public static class Prefix {
    public static final String var = "_v";
    public static final String lockVar = "_lockObj";
    public static final String func = "_f";
    public static final String outArg = "_vo";
  }

  /** JavaScript のキーワード. */
  public static class Js {
    public static final String _if_ = "if ";
    public static final String _else_ = "else ";
    public static final String _while_ = "while ";
    public static final String _for_ = "for ";
    public static final String _break = "break";
    public static final String _break_ = "break ";
    public static final String _continue = "continue";
    public static final String _let_ = "let ";
    public static final String _const_ = "let ";  // Rhino に const のブロックスコープのバグがあるので実際のキーワードをletに変更
    public static final String _function_ = "function ";
    public static final String _true = "true";
    public static final String _false = "false";
    public static final String _undefined = "undefined";
    public static final String _arguments = "arguments";
    public static final String _return = "return";
    public static final String _new_ = "new ";
    public static final String _this = "this";
    public static final String _try_ = "try ";
    public static final String _catch_ = "catch ";
    public static final String _finally_ = "finally ";
    public static final String _throw_ = "throw ";
  }
}
