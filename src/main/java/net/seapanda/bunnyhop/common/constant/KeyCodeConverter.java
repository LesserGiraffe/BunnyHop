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

package net.seapanda.bunnyhop.common.constant;

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
  private static final Map<Integer, KeyCode> gdxKeyCodeToJfxKeyCode =
      new HashMap<>() {{
          put(Keys.NUM_0, KeyCode.DIGIT0);
          put(Keys.NUMPAD_0, KeyCode.DIGIT0);
          put(Keys.NUM_1, KeyCode.DIGIT1);
          put(Keys.NUMPAD_1, KeyCode.DIGIT1);
          put(Keys.NUM_2, KeyCode.DIGIT2);
          put(Keys.NUMPAD_2, KeyCode.DIGIT2);
          put(Keys.NUM_3, KeyCode.DIGIT3);
          put(Keys.NUMPAD_3, KeyCode.DIGIT3);
          put(Keys.NUM_4, KeyCode.DIGIT4);
          put(Keys.NUMPAD_4, KeyCode.DIGIT4);
          put(Keys.NUM_5, KeyCode.DIGIT5);
          put(Keys.NUMPAD_5, KeyCode.DIGIT5);
          put(Keys.NUM_6, KeyCode.DIGIT6);
          put(Keys.NUMPAD_6, KeyCode.DIGIT6);
          put(Keys.NUM_7, KeyCode.DIGIT7);
          put(Keys.NUMPAD_7, KeyCode.DIGIT7);
          put(Keys.NUM_8, KeyCode.DIGIT8);
          put(Keys.NUMPAD_8, KeyCode.DIGIT8);
          put(Keys.NUM_9, KeyCode.DIGIT9);
          put(Keys.NUMPAD_9, KeyCode.DIGIT9);
          put(Keys.UP, KeyCode.UP);
          put(Keys.DPAD_UP, KeyCode.UP);
          put(Keys.DOWN, KeyCode.DOWN);
          put(Keys.DPAD_DOWN, KeyCode.DOWN);
          put(Keys.RIGHT, KeyCode.RIGHT);
          put(Keys.DPAD_RIGHT, KeyCode.RIGHT);
          put(Keys.LEFT, KeyCode.LEFT);
          put(Keys.DPAD_LEFT, KeyCode.LEFT);
          put(Keys.SHIFT_LEFT, KeyCode.SHIFT);
          put(Keys.SHIFT_RIGHT, KeyCode.SHIFT);
          put(Keys.CONTROL_LEFT, KeyCode.CONTROL);
          put(Keys.CONTROL_RIGHT, KeyCode.CONTROL);
          put(Keys.SPACE, KeyCode.SPACE);
          put(Keys.ENTER, KeyCode.ENTER);
          put(Keys.NUMPAD_ENTER, KeyCode.ENTER);
          put(Keys.A, KeyCode.A);
          put(Keys.B, KeyCode.B);
          put(Keys.C, KeyCode.C);
          put(Keys.D, KeyCode.D);
          put(Keys.E, KeyCode.E);
          put(Keys.F, KeyCode.F);
          put(Keys.G, KeyCode.G);
          put(Keys.H, KeyCode.H);
          put(Keys.I, KeyCode.I);
          put(Keys.J, KeyCode.J);
          put(Keys.K, KeyCode.K);
          put(Keys.L, KeyCode.L);
          put(Keys.M, KeyCode.M);
          put(Keys.N, KeyCode.N);
          put(Keys.O, KeyCode.O);
          put(Keys.P, KeyCode.P);
          put(Keys.Q, KeyCode.Q);
          put(Keys.R, KeyCode.R);
          put(Keys.S, KeyCode.S);
          put(Keys.T, KeyCode.T);
          put(Keys.U, KeyCode.U);
          put(Keys.V, KeyCode.V);
          put(Keys.W, KeyCode.W);
          put(Keys.X, KeyCode.X);
          put(Keys.Y, KeyCode.Y);
          put(Keys.Z, KeyCode.Z);
        }};

  /** Javafx のキーコードと {@link BhProgramEvent.Name} の対応.  */
  private static Map<KeyCode, BhProgramEvent.Name> jfxKeyCodeToEventName =
      new HashMap<>() {{
          put(KeyCode.DIGIT0, BhProgramEvent.Name.KEY_DIGIT0_PRESSED);
          put(KeyCode.DIGIT1, BhProgramEvent.Name.KEY_DIGIT1_PRESSED);
          put(KeyCode.DIGIT2, BhProgramEvent.Name.KEY_DIGIT2_PRESSED);
          put(KeyCode.DIGIT3, BhProgramEvent.Name.KEY_DIGIT3_PRESSED);
          put(KeyCode.DIGIT4, BhProgramEvent.Name.KEY_DIGIT4_PRESSED);
          put(KeyCode.DIGIT5, BhProgramEvent.Name.KEY_DIGIT5_PRESSED);
          put(KeyCode.DIGIT6, BhProgramEvent.Name.KEY_DIGIT6_PRESSED);
          put(KeyCode.DIGIT7, BhProgramEvent.Name.KEY_DIGIT7_PRESSED);
          put(KeyCode.DIGIT8, BhProgramEvent.Name.KEY_DIGIT8_PRESSED);
          put(KeyCode.DIGIT9, BhProgramEvent.Name.KEY_DIGIT9_PRESSED);
          put(KeyCode.UP, BhProgramEvent.Name.KEY_UP_PRESSED);
          put(KeyCode.DOWN, BhProgramEvent.Name.KEY_DOWN_PRESSED);
          put(KeyCode.RIGHT, BhProgramEvent.Name.KEY_RIGHT_PRESSED);
          put(KeyCode.LEFT, BhProgramEvent.Name.KEY_LEFT_PRESSED);
          put(KeyCode.SHIFT, BhProgramEvent.Name.KEY_SHIFT_PRESSED);
          put(KeyCode.CONTROL, BhProgramEvent.Name.KEY_CTRL_PRESSED);
          put(KeyCode.SPACE, BhProgramEvent.Name.KEY_SPACE_PRESSED);
          put(KeyCode.ENTER, BhProgramEvent.Name.KEY_ENTER_PRESSED);
          put(KeyCode.A, BhProgramEvent.Name.KEY_A_PRESSED);
          put(KeyCode.B, BhProgramEvent.Name.KEY_B_PRESSED);
          put(KeyCode.C, BhProgramEvent.Name.KEY_C_PRESSED);
          put(KeyCode.D, BhProgramEvent.Name.KEY_D_PRESSED);
          put(KeyCode.E, BhProgramEvent.Name.KEY_E_PRESSED);
          put(KeyCode.F, BhProgramEvent.Name.KEY_F_PRESSED);
          put(KeyCode.G, BhProgramEvent.Name.KEY_G_PRESSED);
          put(KeyCode.H, BhProgramEvent.Name.KEY_H_PRESSED);
          put(KeyCode.I, BhProgramEvent.Name.KEY_I_PRESSED);
          put(KeyCode.J, BhProgramEvent.Name.KEY_J_PRESSED);
          put(KeyCode.K, BhProgramEvent.Name.KEY_K_PRESSED);
          put(KeyCode.L, BhProgramEvent.Name.KEY_L_PRESSED);
          put(KeyCode.M, BhProgramEvent.Name.KEY_M_PRESSED);
          put(KeyCode.N, BhProgramEvent.Name.KEY_N_PRESSED);
          put(KeyCode.O, BhProgramEvent.Name.KEY_O_PRESSED);
          put(KeyCode.P, BhProgramEvent.Name.KEY_P_PRESSED);
          put(KeyCode.Q, BhProgramEvent.Name.KEY_Q_PRESSED);
          put(KeyCode.R, BhProgramEvent.Name.KEY_R_PRESSED);
          put(KeyCode.S, BhProgramEvent.Name.KEY_S_PRESSED);
          put(KeyCode.T, BhProgramEvent.Name.KEY_T_PRESSED);
          put(KeyCode.U, BhProgramEvent.Name.KEY_U_PRESSED);
          put(KeyCode.V, BhProgramEvent.Name.KEY_V_PRESSED);
          put(KeyCode.W, BhProgramEvent.Name.KEY_W_PRESSED);
          put(KeyCode.X, BhProgramEvent.Name.KEY_X_PRESSED);
          put(KeyCode.Y, BhProgramEvent.Name.KEY_Y_PRESSED);
          put(KeyCode.Z, BhProgramEvent.Name.KEY_Z_PRESSED);
        }};

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
