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

package net.seapanda.bunnyhop.view;

import net.seapanda.bunnyhop.service.Util;

/**
 * ビューの初期化に失敗したことを表す例外.
 *
 * @author K.Koike
 */
public class ViewInitializationException extends Exception {
  public ViewInitializationException(String msg) {
    this(msg, true);
  }

  public ViewInitializationException(String msg, boolean includeMethodInfo) {
    super(msg + "\n  " + Util.INSTANCE.getMethodName(1));
  }
}
