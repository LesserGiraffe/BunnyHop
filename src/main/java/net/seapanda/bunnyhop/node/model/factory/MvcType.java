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

package net.seapanda.bunnyhop.node.model.factory;

/**
 * MVC 構造のタイプ.
 *
 * @author  K.Koike
 */
public enum MvcType {
  /** MVC 構造を持たない. */
  NONE,
  /** ワークスペース上で操作可能なノード用の MVC 構造. */
  DEFAULT,
  /** テンプレートノード用の MVC 構造. */
  TEMPLATE,
}
