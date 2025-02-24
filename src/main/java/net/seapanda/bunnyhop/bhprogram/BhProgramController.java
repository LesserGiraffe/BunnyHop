package net.seapanda.bunnyhop.bhprogram;

import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramNotification;

/**
 * BhProgram に対する以下の操作を規定したインタフェース.
 *
 * <pre>
 * - BhProgram の実行
 * - BhProgram の終了
 * - BhProgram とのデータ通信の有効化
 * - BhProgram とのデータ通信の無効化
 * - BhProgram へのデータ送信
 * - このオブジェクトの破棄
 * </pre>
 *
 * @author K.Koike
 */
public interface BhProgramController {
  
  /**
   * BhProgram を実行する.
   *
   * @param nodeSet 実行可能なノードのリスト
   * @param ipAddr BhProgram を実行するマシンの IP アドレス.
   * @param uname BhProgram を実行するマシンにログインする際のユーザ名
   * @param password BhProgram を実行するマシンにログインする際のパスワード
   * @return 成功した場合 true
   * @throws UnsupportedOperationException この処理がサポートされていない場合
   */
  boolean execute(ExecutableNodeSet nodeSet, String ipAddr, String uname, String password)
      throws UnsupportedOperationException;

  /**
   * BhProgram をローカルマシン上で実行する.
   *
   * @param nodeSet 実行可能なノードのリスト
   * @return 成功した場合 true
   * @throws UnsupportedOperationException この処理がサポートされていない場合
   */
  boolean execute(ExecutableNodeSet nodeSet) throws UnsupportedOperationException;

  /**
   * 現在実行中の BhProgram を終了する.
   *
   * @return 成功した場合 true
   */
  boolean terminate();

  /**
   * BhProgram との通信を有効化する.
   *
   * @return 成功した場合 true
   */
  boolean enableCommunication();

  /**
   * BhProgram の実行環境と通信を行わないようにする.
   *
   * @return 成功した場合 true
   */
  boolean disableCommunication();

  /**
   * 引数で指定した {@link BhProgramNotification} を BhProgram に送る.
   *
   * @param notif 送信データ
   * @return ステータスコード
   */
  BhRuntimeStatus send(BhProgramNotification notif);
}
