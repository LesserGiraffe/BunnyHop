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

package net.seapanda.bunnyhop.model.node.event;

import net.seapanda.bunnyhop.model.node.parameter.ConnectorId;

/**
 * 外部スクリプトに定義されたコネクタのイベントハンドラを呼び出す機能を規定するインタフェース.
 *
 * @author K.Koike
 */
public interface ScriptConnectorEventInvoker extends ConnectorEventInvoker {
  
  /**
   * 外部スクリプトの情報をこのオブジェクトに登録する.
   *
   * @param cnctrId この ID の {@link Connector} に対して定義されたイベントとスクリプトのファイル名を登録する.
   * @param type コネクタ ID が {@code cnctrId} である {@link Connector} に対して定義されたイベントの種類
   * @param scriptName {@code type} に対応するイベントハンドラが定義されたファイル名
   *                   からの文字列を指定した場合は, 何も登録しない.
   */
  void register(ConnectorId cnctrId, EventType type, String scriptName);
}
