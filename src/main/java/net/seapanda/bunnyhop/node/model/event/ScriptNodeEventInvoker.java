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

package net.seapanda.bunnyhop.node.model.event;


import net.seapanda.bunnyhop.node.model.BhNode;
import net.seapanda.bunnyhop.node.model.parameter.BhNodeId;

/**
 * 外部スクリプトに定義されたノードのイベントハンドラを呼び出す機能を規定するインタフェース.
 *
 * @author K.Koike
 */
public interface ScriptNodeEventInvoker extends NodeEventInvoker {
  
  /**
   * 外部スクリプトの情報をこのオブジェクトに登録する.
   *
   * @param nodeId この ID の {@link BhNode} に対して定義されたイベントとスクリプトのファイル名を登録する.
   * @param type ノード ID が {@code nodeId} である {@link BhNode} に対して定義されたイベントの種類
   * @param scriptName {@code type} に対応するイベントハンドラが定義されたファイル名.
   *                   からの文字列を指定した場合は, 何も登録しない.
   */
  public void register(BhNodeId nodeId, EventType type, String scriptName);
}
