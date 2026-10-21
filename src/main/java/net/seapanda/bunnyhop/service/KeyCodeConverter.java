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

package net.seapanda.bunnyhop.service;

import com.badlogic.gdx.Input.Keys;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javafx.scene.input.KeyCode;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramEvent;

/**
 * キーコードとイベントの対応関係を定義したクラス.
 *
 * @author K.Koike
 */
public class KeyCodeConverter {

  /** libGdx のキーコードと Javafx のキーコードの対応.  */
  private static final Map<Integer, KeyCode> gdxKeyCodeToJfxKeyCode = new HashMap<>();

  static {
    gdxKeyCodeToJfxKeyCode.put(Keys.NUM_0, KeyCode.DIGIT0);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUMPAD_0, KeyCode.DIGIT0);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUM_1, KeyCode.DIGIT1);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUMPAD_1, KeyCode.DIGIT1);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUM_2, KeyCode.DIGIT2);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUMPAD_2, KeyCode.DIGIT2);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUM_3, KeyCode.DIGIT3);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUMPAD_3, KeyCode.DIGIT3);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUM_4, KeyCode.DIGIT4);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUMPAD_4, KeyCode.DIGIT4);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUM_5, KeyCode.DIGIT5);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUMPAD_5, KeyCode.DIGIT5);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUM_6, KeyCode.DIGIT6);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUMPAD_6, KeyCode.DIGIT6);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUM_7, KeyCode.DIGIT7);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUMPAD_7, KeyCode.DIGIT7);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUM_8, KeyCode.DIGIT8);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUMPAD_8, KeyCode.DIGIT8);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUM_9, KeyCode.DIGIT9);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUMPAD_9, KeyCode.DIGIT9);
    gdxKeyCodeToJfxKeyCode.put(Keys.UP, KeyCode.UP);
    gdxKeyCodeToJfxKeyCode.put(Keys.DPAD_UP, KeyCode.UP);
    gdxKeyCodeToJfxKeyCode.put(Keys.DOWN, KeyCode.DOWN);
    gdxKeyCodeToJfxKeyCode.put(Keys.DPAD_DOWN, KeyCode.DOWN);
    gdxKeyCodeToJfxKeyCode.put(Keys.RIGHT, KeyCode.RIGHT);
    gdxKeyCodeToJfxKeyCode.put(Keys.DPAD_RIGHT, KeyCode.RIGHT);
    gdxKeyCodeToJfxKeyCode.put(Keys.LEFT, KeyCode.LEFT);
    gdxKeyCodeToJfxKeyCode.put(Keys.DPAD_LEFT, KeyCode.LEFT);
    gdxKeyCodeToJfxKeyCode.put(Keys.SHIFT_LEFT, KeyCode.SHIFT);
    gdxKeyCodeToJfxKeyCode.put(Keys.SHIFT_RIGHT, KeyCode.SHIFT);
    gdxKeyCodeToJfxKeyCode.put(Keys.CONTROL_LEFT, KeyCode.CONTROL);
    gdxKeyCodeToJfxKeyCode.put(Keys.CONTROL_RIGHT, KeyCode.CONTROL);
    gdxKeyCodeToJfxKeyCode.put(Keys.SPACE, KeyCode.SPACE);
    gdxKeyCodeToJfxKeyCode.put(Keys.ENTER, KeyCode.ENTER);
    gdxKeyCodeToJfxKeyCode.put(Keys.NUMPAD_ENTER, KeyCode.ENTER);
    gdxKeyCodeToJfxKeyCode.put(Keys.A, KeyCode.A);
    gdxKeyCodeToJfxKeyCode.put(Keys.B, KeyCode.B);
    gdxKeyCodeToJfxKeyCode.put(Keys.C, KeyCode.C);
    gdxKeyCodeToJfxKeyCode.put(Keys.D, KeyCode.D);
    gdxKeyCodeToJfxKeyCode.put(Keys.E, KeyCode.E);
    gdxKeyCodeToJfxKeyCode.put(Keys.F, KeyCode.F);
    gdxKeyCodeToJfxKeyCode.put(Keys.G, KeyCode.G);
    gdxKeyCodeToJfxKeyCode.put(Keys.H, KeyCode.H);
    gdxKeyCodeToJfxKeyCode.put(Keys.I, KeyCode.I);
    gdxKeyCodeToJfxKeyCode.put(Keys.J, KeyCode.J);
    gdxKeyCodeToJfxKeyCode.put(Keys.K, KeyCode.K);
    gdxKeyCodeToJfxKeyCode.put(Keys.L, KeyCode.L);
    gdxKeyCodeToJfxKeyCode.put(Keys.M, KeyCode.M);
    gdxKeyCodeToJfxKeyCode.put(Keys.N, KeyCode.N);
    gdxKeyCodeToJfxKeyCode.put(Keys.O, KeyCode.O);
    gdxKeyCodeToJfxKeyCode.put(Keys.P, KeyCode.P);
    gdxKeyCodeToJfxKeyCode.put(Keys.Q, KeyCode.Q);
    gdxKeyCodeToJfxKeyCode.put(Keys.R, KeyCode.R);
    gdxKeyCodeToJfxKeyCode.put(Keys.S, KeyCode.S);
    gdxKeyCodeToJfxKeyCode.put(Keys.T, KeyCode.T);
    gdxKeyCodeToJfxKeyCode.put(Keys.U, KeyCode.U);
    gdxKeyCodeToJfxKeyCode.put(Keys.V, KeyCode.V);
    gdxKeyCodeToJfxKeyCode.put(Keys.W, KeyCode.W);
    gdxKeyCodeToJfxKeyCode.put(Keys.X, KeyCode.X);
    gdxKeyCodeToJfxKeyCode.put(Keys.Y, KeyCode.Y);
    gdxKeyCodeToJfxKeyCode.put(Keys.Z, KeyCode.Z);
  }

  /** Javafx のキーコードと {@link BhProgramEvent.Name} の対応.  */
  private static final Map<KeyCode, BhProgramEvent.Name> jfxKeyCodeToEventName =
      Map.ofEntries(
          Map.entry(KeyCode.DIGIT0, BhProgramEvent.Name.KEY_DIGIT0_PRESSED),
          Map.entry(KeyCode.DIGIT1, BhProgramEvent.Name.KEY_DIGIT1_PRESSED),
          Map.entry(KeyCode.DIGIT2, BhProgramEvent.Name.KEY_DIGIT2_PRESSED),
          Map.entry(KeyCode.DIGIT3, BhProgramEvent.Name.KEY_DIGIT3_PRESSED),
          Map.entry(KeyCode.DIGIT4, BhProgramEvent.Name.KEY_DIGIT4_PRESSED),
          Map.entry(KeyCode.DIGIT5, BhProgramEvent.Name.KEY_DIGIT5_PRESSED),
          Map.entry(KeyCode.DIGIT6, BhProgramEvent.Name.KEY_DIGIT6_PRESSED),
          Map.entry(KeyCode.DIGIT7, BhProgramEvent.Name.KEY_DIGIT7_PRESSED),
          Map.entry(KeyCode.DIGIT8, BhProgramEvent.Name.KEY_DIGIT8_PRESSED),
          Map.entry(KeyCode.DIGIT9, BhProgramEvent.Name.KEY_DIGIT9_PRESSED),
          Map.entry(KeyCode.UP, BhProgramEvent.Name.KEY_UP_PRESSED),
          Map.entry(KeyCode.DOWN, BhProgramEvent.Name.KEY_DOWN_PRESSED),
          Map.entry(KeyCode.RIGHT, BhProgramEvent.Name.KEY_RIGHT_PRESSED),
          Map.entry(KeyCode.LEFT, BhProgramEvent.Name.KEY_LEFT_PRESSED),
          Map.entry(KeyCode.SHIFT, BhProgramEvent.Name.KEY_SHIFT_PRESSED),
          Map.entry(KeyCode.CONTROL, BhProgramEvent.Name.KEY_CTRL_PRESSED),
          Map.entry(KeyCode.SPACE, BhProgramEvent.Name.KEY_SPACE_PRESSED),
          Map.entry(KeyCode.ENTER, BhProgramEvent.Name.KEY_ENTER_PRESSED),
          Map.entry(KeyCode.A, BhProgramEvent.Name.KEY_A_PRESSED),
          Map.entry(KeyCode.B, BhProgramEvent.Name.KEY_B_PRESSED),
          Map.entry(KeyCode.C, BhProgramEvent.Name.KEY_C_PRESSED),
          Map.entry(KeyCode.D, BhProgramEvent.Name.KEY_D_PRESSED),
          Map.entry(KeyCode.E, BhProgramEvent.Name.KEY_E_PRESSED),
          Map.entry(KeyCode.F, BhProgramEvent.Name.KEY_F_PRESSED),
          Map.entry(KeyCode.G, BhProgramEvent.Name.KEY_G_PRESSED),
          Map.entry(KeyCode.H, BhProgramEvent.Name.KEY_H_PRESSED),
          Map.entry(KeyCode.I, BhProgramEvent.Name.KEY_I_PRESSED),
          Map.entry(KeyCode.J, BhProgramEvent.Name.KEY_J_PRESSED),
          Map.entry(KeyCode.K, BhProgramEvent.Name.KEY_K_PRESSED),
          Map.entry(KeyCode.L, BhProgramEvent.Name.KEY_L_PRESSED),
          Map.entry(KeyCode.M, BhProgramEvent.Name.KEY_M_PRESSED),
          Map.entry(KeyCode.N, BhProgramEvent.Name.KEY_N_PRESSED),
          Map.entry(KeyCode.O, BhProgramEvent.Name.KEY_O_PRESSED),
          Map.entry(KeyCode.P, BhProgramEvent.Name.KEY_P_PRESSED),
          Map.entry(KeyCode.Q, BhProgramEvent.Name.KEY_Q_PRESSED),
          Map.entry(KeyCode.R, BhProgramEvent.Name.KEY_R_PRESSED),
          Map.entry(KeyCode.S, BhProgramEvent.Name.KEY_S_PRESSED),
          Map.entry(KeyCode.T, BhProgramEvent.Name.KEY_T_PRESSED),
          Map.entry(KeyCode.U, BhProgramEvent.Name.KEY_U_PRESSED),
          Map.entry(KeyCode.V, BhProgramEvent.Name.KEY_V_PRESSED),
          Map.entry(KeyCode.W, BhProgramEvent.Name.KEY_W_PRESSED),
          Map.entry(KeyCode.X, BhProgramEvent.Name.KEY_X_PRESSED),
          Map.entry(KeyCode.Y, BhProgramEvent.Name.KEY_Y_PRESSED),
          Map.entry(KeyCode.Z, BhProgramEvent.Name.KEY_Z_PRESSED));

  /** libGdx のキーコードから Javafx のキーコードに変換する.  */
  public static Optional<KeyCode> toJfxKeyCode(int gdxKeyCode) {
    return Optional.ofNullable(gdxKeyCodeToJfxKeyCode.get(gdxKeyCode));
  }

  /** libGdx のキーコードから Javafx のキーコードに変換する.  */
  public static Optional<BhProgramEvent.Name> toBhProgramEventName(KeyCode jfxKeyCode) {
    return Optional.ofNullable(jfxKeyCodeToEventName.get(jfxKeyCode));
  }

  /** libGdx のキーコードから Javafx のキーコードに変換する.  */
  public static Optional<BhProgramEvent.Name> toBhProgramEventName(int gdxKeyCode) {
    var jfxKeyCode = gdxKeyCodeToJfxKeyCode.get(gdxKeyCode);
    if (jfxKeyCode == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(jfxKeyCodeToEventName.get(jfxKeyCode));
  }
}
