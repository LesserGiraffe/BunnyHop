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

package net.seapanda.bunnyhop.bhprogram;

/**
 * BhProgram の実行環境に関するステータスコード.
 *
 * @author K.Koike
 */
public enum BhRuntimeStatus {
  /** エラーなし. */
  SUCCESS,
  /** 切断中に送信しようとした. */
  SEND_WHEN_DISCONNECTED,
  /** 送信キューが満杯で送信できない. */
  SEND_QUEUE_FULL,
}