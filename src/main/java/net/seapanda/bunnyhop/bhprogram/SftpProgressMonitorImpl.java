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

import com.jcraft.jsch.SftpProgressMonitor;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;

/**
 * ファイル転送プロセス進捗管理クラス.
 *
 * @author K.Koike
 */
class SftpProgressMonitorImpl implements SftpProgressMonitor {
  /** 転送元. */
  private String src;
  /** 転送先. */
  private String dest;
  /** 転送バイト数. */
  private long max;
  /** 今までに転送されたバイト数. */
  private long allByteSent = 0;
  /** 転送されたデータのパーセンテージ. */
  private int rateOfDataSent = 0;
  /** ファイル転送キャンセルフラグ. */
  private final AtomicReference<Boolean> fileCopyIsCancelled;
  /** ファイル転送キャンセルフラグ (ラッチされる). */
  private boolean fileCopyHasBeenCancelled = false;

  /**
   * コンストラクタ.
   *
   * @param fileCopyIsCancelled ファイル転送キャンセルフラグ
   */
  SftpProgressMonitorImpl(AtomicReference<Boolean> fileCopyIsCancelled) {
    this.fileCopyIsCancelled = fileCopyIsCancelled;
  }

  @Override
  public boolean count(long byteSent) {
    String fileName = Paths.get(src).getFileName().toString();
    fileCopyHasBeenCancelled |= fileCopyIsCancelled.get();
    allByteSent += byteSent;
    int newRateOfDataSent = (int) (100L * allByteSent / max);

    if (fileCopyHasBeenCancelled) {
      MsgPrinter.INSTANCE.msgForUser(fileName + " 転送中止\n");
      MsgPrinter.INSTANCE.msgForDebug(
          "transfer is cancelled\n  "
          + src + " -> " + dest + " (" + allByteSent + "/" + max + ")");

    } else if (newRateOfDataSent >= rateOfDataSent + 4) {
      MsgPrinter.INSTANCE.msgForUser(fileName + " 転送中 (" + newRateOfDataSent + "%)\n");
      rateOfDataSent = newRateOfDataSent;
    }
    return !fileCopyHasBeenCancelled;  //trueを返すと転送が続く
  }

  @Override
  public void end() {
    if (fileCopyHasBeenCancelled) {
      return;
    }
    String fileName = Paths.get(src).getFileName().toString();
    MsgPrinter.INSTANCE.msgForUser(fileName + " 転送終了\n");
  }

  @Override
  public void init(int op, String src, String dest, long max) {
    this.src = src;
    this.dest = dest;
    this.max = max;
    MsgPrinter.INSTANCE.msgForUser("転送開始\n  " + src + " -> " + dest + " (" + max + " Byte)\n");
  }

  public boolean  isFileCopyCancelled() {
    return fileCopyHasBeenCancelled;
  }
}
