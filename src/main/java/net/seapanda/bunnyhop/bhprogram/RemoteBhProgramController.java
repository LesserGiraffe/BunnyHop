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
public interface RemoteBhProgramController extends BhProgramMessenger {
  
  /**
   * BhProgram を実行する.
   *
   * @param nodeSet 実行可能なノードのリスト
   * @param uostname BhProgram を実行するマシンのホスト名
   * @param uname BhProgram を実行するマシンにログインする際のユーザ名
   * @param password BhProgram を実行するマシンにログインする際のパスワード
   * @return 成功した場合 true
   * @throws UnsupportedOperationException この処理がサポートされていない場合
   */
  boolean execute(ExecutableNodeSet nodeSet, String uostname, String uname, String password)
      throws UnsupportedOperationException;

  /**
   * 引数で指定したマシン上で実行中の BhProgram を終了する.
   *
   * @param hostname ホスト名
   * @param uname マシンにログインする際のユーザ名
   * @param password マシンのログインパスワード
   * @return 成功した場合 true
   */
  boolean terminate(String hostname, String uname, String password);

  /** 
   * 引数で指定したマシン上で実行中の BhProgram との通信を有効化する.
   *
   * @param hostname ホスト名
   * @param uname マシンにログインする際のユーザ名
   * @param password マシンのログインパスワード
   * @return 成功した場合 true
   */
  boolean enableCommunication(String hostname, String uname, String password);

  /** 
   * 現在有効になっている BhProgram との通信を無効化する.
   *
   * @return 成功した場合 true
   */

  boolean disableCommunication();
}
