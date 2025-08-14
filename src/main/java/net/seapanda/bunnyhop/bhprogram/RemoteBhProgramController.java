package net.seapanda.bunnyhop.bhprogram;


import net.seapanda.bunnyhop.bhprogram.runtime.RemoteBhRuntimeController;

/**
 * BhProgram の起動とその実行環境の制御用オブジェクトを取得する機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface RemoteBhProgramController {
  
  /**
   * BhProgram を実行する.
   *
   * @param nodeSet 実行可能なノードのリスト
   * @param hostname BhProgram を実行するマシンのホスト名
   * @param uname BhProgram を実行するマシンにログインする際のユーザ名
   * @param password BhProgram を実行するマシンにログインする際のパスワード
   * @return 成功した場合 true
   * @throws UnsupportedOperationException この処理がサポートされていない場合
   */
  boolean launch(ExecutableNodeSet nodeSet, String hostname, String uname, String password)
      throws UnsupportedOperationException;

  /** {@link #launch} 起動したプログラムを実行する BhRuntime を制御するためのオブジェクトを取得する. */
  RemoteBhRuntimeController getBhRuntimeCtrl();
}
