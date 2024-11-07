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

package net.seapanda.bunnyhop.message;

/**
 * メッセージを送信するクラス.
 *
 * @author K.Koike
 */
public class MsgTransporter {

  public static final MsgTransporter INSTANCE = new MsgTransporter();

  /** コンストラクタ. */
  private MsgTransporter() {}

  /**
   * dispatchers に順番にメッセージを渡す.
   * 2 つめ以降の {@link MsgDispatcher} オブジェクトには,
   * 1 つ前の {@link MsgDispatcher} オブジェクトの処理結果である {@link MsgData} が渡される.
   *
   * @param msg 送信メッセージ
   * @param data 1 つ目の dispatchers に渡されるメッセージ
   * @param dispatchers メッセージ投函先
   * @return 最後のメッセージ送信先から返されるデータ
   */
  public MsgData sendMessage(BhMsg msg, MsgData data, MsgDispatcher... dispatchers) {
    for (MsgDispatcher dispatcher : dispatchers) {
      data = dispatcher.dispatch(msg, data);
    }
    return data;
  }

  /**
   * dispatchers に順番にメッセージを渡す.
   * 2 つめ以降の {@link MsgDispatcher} オブジェクトには, 
   * 1つ前の {@link MsgDispatcher} オブジェクトの処理結果である {@link MsgData} が渡される.
   *
   * @param msg 送信メッセージ
   * @param dispatchers メッセージ投函先
   * @return 最後のメッセージ送信先から返されるデータ
   */
  public MsgData sendMessage(BhMsg msg, MsgDispatcher... dispatchers) {
    MsgData data = null;
    for (MsgDispatcher dispatcher : dispatchers) {
      data = dispatcher.dispatch(msg, data);
    }
    return data;
  }
}
