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

package net.seapanda.bunnyhop.common;

import javafx.scene.text.Font;

/**
 * システムの標準文字サイズによって変わる大きさの単位.
 *
 * @author K.Koike
 */
public class Rem {

  public static final double VAL = Font.getDefault().getSize();
  private double remVal = VAL;

  public void setRem(double val) {
    remVal = Rem.VAL *  val;
  }

  public double getRem() {
    return remVal;
  }
}
