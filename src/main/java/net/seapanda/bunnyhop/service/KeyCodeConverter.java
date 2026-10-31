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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javafx.scene.input.KeyCode;
import net.seapanda.bunnyhop.bhprogram.common.message.BhProgramEvent.Name;
import net.seapanda.bunnyhop.simulator.api.KeyboardKey;

/**
 * キーコードとイベントの対応関係を定義したクラス.
 *
 * @author K.Koike
 */
public class KeyCodeConverter {

  /** シミュレータで定義されるキーコードと {@link Name} の対応.  */
  private static final Map<KeyboardKey, Name> keyboardKeyToEventName = new HashMap<>();

  static {
    keyboardKeyToEventName.put(KeyboardKey.DIGIT_0, Name.KEY_DIGIT0_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.DIGIT_1, Name.KEY_DIGIT1_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.DIGIT_2, Name.KEY_DIGIT2_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.DIGIT_3, Name.KEY_DIGIT3_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.DIGIT_4, Name.KEY_DIGIT4_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.DIGIT_5, Name.KEY_DIGIT5_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.DIGIT_6, Name.KEY_DIGIT6_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.DIGIT_7, Name.KEY_DIGIT7_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.DIGIT_8, Name.KEY_DIGIT8_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.DIGIT_9, Name.KEY_DIGIT9_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.UP, Name.KEY_UP_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.DOWN, Name.KEY_DOWN_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.RIGHT, Name.KEY_RIGHT_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.LEFT, Name.KEY_LEFT_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.SHIFT, Name.KEY_SHIFT_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.CONTROL, Name.KEY_CTRL_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.SPACE, Name.KEY_SPACE_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.ENTER, Name.KEY_ENTER_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.A, Name.KEY_A_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.B, Name.KEY_B_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.C, Name.KEY_C_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.D, Name.KEY_D_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.E, Name.KEY_E_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.F, Name.KEY_F_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.G, Name.KEY_G_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.H, Name.KEY_H_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.I, Name.KEY_I_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.J, Name.KEY_J_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.K, Name.KEY_K_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.L, Name.KEY_L_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.M, Name.KEY_M_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.N, Name.KEY_N_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.O, Name.KEY_O_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.P, Name.KEY_P_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.Q, Name.KEY_Q_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.R, Name.KEY_R_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.S, Name.KEY_S_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.T, Name.KEY_T_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.U, Name.KEY_U_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.V, Name.KEY_V_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.W, Name.KEY_W_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.X, Name.KEY_X_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.Y, Name.KEY_Y_PRESSED);
    keyboardKeyToEventName.put(KeyboardKey.Z, Name.KEY_Z_PRESSED);
  }

  /** Javafx のキーコードと {@link Name} の対応.  */
  private static final Map<KeyCode, Name> jfxKeyCodeToEventName =
      Map.ofEntries(
          Map.entry(KeyCode.DIGIT0, Name.KEY_DIGIT0_PRESSED),
          Map.entry(KeyCode.DIGIT1, Name.KEY_DIGIT1_PRESSED),
          Map.entry(KeyCode.DIGIT2, Name.KEY_DIGIT2_PRESSED),
          Map.entry(KeyCode.DIGIT3, Name.KEY_DIGIT3_PRESSED),
          Map.entry(KeyCode.DIGIT4, Name.KEY_DIGIT4_PRESSED),
          Map.entry(KeyCode.DIGIT5, Name.KEY_DIGIT5_PRESSED),
          Map.entry(KeyCode.DIGIT6, Name.KEY_DIGIT6_PRESSED),
          Map.entry(KeyCode.DIGIT7, Name.KEY_DIGIT7_PRESSED),
          Map.entry(KeyCode.DIGIT8, Name.KEY_DIGIT8_PRESSED),
          Map.entry(KeyCode.DIGIT9, Name.KEY_DIGIT9_PRESSED),
          Map.entry(KeyCode.UP, Name.KEY_UP_PRESSED),
          Map.entry(KeyCode.DOWN, Name.KEY_DOWN_PRESSED),
          Map.entry(KeyCode.RIGHT, Name.KEY_RIGHT_PRESSED),
          Map.entry(KeyCode.LEFT, Name.KEY_LEFT_PRESSED),
          Map.entry(KeyCode.SHIFT, Name.KEY_SHIFT_PRESSED),
          Map.entry(KeyCode.CONTROL, Name.KEY_CTRL_PRESSED),
          Map.entry(KeyCode.SPACE, Name.KEY_SPACE_PRESSED),
          Map.entry(KeyCode.ENTER, Name.KEY_ENTER_PRESSED),
          Map.entry(KeyCode.A, Name.KEY_A_PRESSED),
          Map.entry(KeyCode.B, Name.KEY_B_PRESSED),
          Map.entry(KeyCode.C, Name.KEY_C_PRESSED),
          Map.entry(KeyCode.D, Name.KEY_D_PRESSED),
          Map.entry(KeyCode.E, Name.KEY_E_PRESSED),
          Map.entry(KeyCode.F, Name.KEY_F_PRESSED),
          Map.entry(KeyCode.G, Name.KEY_G_PRESSED),
          Map.entry(KeyCode.H, Name.KEY_H_PRESSED),
          Map.entry(KeyCode.I, Name.KEY_I_PRESSED),
          Map.entry(KeyCode.J, Name.KEY_J_PRESSED),
          Map.entry(KeyCode.K, Name.KEY_K_PRESSED),
          Map.entry(KeyCode.L, Name.KEY_L_PRESSED),
          Map.entry(KeyCode.M, Name.KEY_M_PRESSED),
          Map.entry(KeyCode.N, Name.KEY_N_PRESSED),
          Map.entry(KeyCode.O, Name.KEY_O_PRESSED),
          Map.entry(KeyCode.P, Name.KEY_P_PRESSED),
          Map.entry(KeyCode.Q, Name.KEY_Q_PRESSED),
          Map.entry(KeyCode.R, Name.KEY_R_PRESSED),
          Map.entry(KeyCode.S, Name.KEY_S_PRESSED),
          Map.entry(KeyCode.T, Name.KEY_T_PRESSED),
          Map.entry(KeyCode.U, Name.KEY_U_PRESSED),
          Map.entry(KeyCode.V, Name.KEY_V_PRESSED),
          Map.entry(KeyCode.W, Name.KEY_W_PRESSED),
          Map.entry(KeyCode.X, Name.KEY_X_PRESSED),
          Map.entry(KeyCode.Y, Name.KEY_Y_PRESSED),
          Map.entry(KeyCode.Z, Name.KEY_Z_PRESSED));

  /** libGdx のキーコードから Javafx のキーコードに変換する.  */
  public static Optional<Name> toBhProgramEventName(KeyCode jfxKeyCode) {
    return Optional.ofNullable(jfxKeyCodeToEventName.get(jfxKeyCode));
  }

  /** libGdx のキーコードから Javafx のキーコードに変換する.  */
  public static Optional<Name> toBhProgramEventName(KeyboardKey key) {
    return Optional.ofNullable(keyboardKeyToEventName.get(key));
  }
}
