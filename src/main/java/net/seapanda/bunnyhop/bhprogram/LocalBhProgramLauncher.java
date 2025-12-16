package net.seapanda.bunnyhop.bhprogram;


import net.seapanda.bunnyhop.bhprogram.runtime.LocalBhRuntimeController;
import net.seapanda.bunnyhop.compiler.SourceSet;

/**
 * BhProgram の起動とその実行環境の制御用オブジェクトを取得する機能を規定したインタフェース.
 *
 * @author K.Koike
 */
public interface LocalBhProgramLauncher {
  
  /**
   * BhProgram をローカルマシン上で実行する.
   *
   * @param sourceSet コンパイル対象となるノード一覧を提供するオブジェクト
   * @return 成功した場合 true
   * @throws UnsupportedOperationException この処理がサポートされていない場合
   */
  boolean launch(SourceSet sourceSet) throws UnsupportedOperationException;

  /** {@link #launch} 起動したプログラムを実行する BhRuntime を制御するためのオブジェクトを取得する. */
  LocalBhRuntimeController getBhRuntimeCtrl();
}
