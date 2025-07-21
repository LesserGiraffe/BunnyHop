package net.seapanda.bunnyhop.bhprogram;


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
public interface LocalBhProgramController extends BhProgramMessenger {
  
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
   * BhProgram との通信を無効化する.
   *
   * @return 成功した場合 true
   */
  boolean disableCommunication();
}
