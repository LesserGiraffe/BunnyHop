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

package net.seapanda.bunnyhop.compiler;

/** 変数, 関数, プロパティ等の定義. */
public class ScriptIdentifiers {

  /** BhProgram で使用される関数名. (JavaScript の組み込み関数は除く) */
  public static class Funcs {
    public static final String STR = "_str";
    public static final String STR_TO_NUM = "_strToNum";
    public static final String RANDOM_INT = "_randomInt";
    public static final String PRINTLN = "_println";
    public static final String SLEEP = "_sleep";
    public static final String PLAY_MELODIES = "_playMelodies";
    public static final String ANY_EQ = "_anyEq";
    public static final String ANY_NEQ = "_anyNeq";
    public static final String ADD_COLOR = "_addColor";
    public static final String SUB_COLOR = "_subColor";
    public static final String DETECT_COLOR = "_detectColor";
    public static final String SAY = "_say";
    public static final String LIGHT_EYE = "_lightEye";
    public static final String SCAN = "_scan";
    public static final String ARY_TO_STR = "_aryToStr";
    public static final String ARY_PUSH = "_aryPush";
    public static final String ARY_INSERT = "_aryInsert";
    public static final String ARY_REMOVE = "_aryRemove";
    public static final String ARY_EXTRACT = "_aryExtract";
    public static final String ARY_ADD_ALL = "_aryAddAll";
    public static final String ARY_GET = "_aryGet";
    public static final String ARY_SET = "_arySet";
    public static final String ARY_LEN = "_aryLen";
    public static final String ARY_REVERSE = "_aryReverse";
    public static final String ARY_FIRST_INDEX_OF = "_aryFirstIndexOf";
    public static final String ARY_LAST_INDEX_OF = "_aryLastIndexOf";
    public static final String ARY_INCLUDES = "_aryIncludes";
    public static final String ARY_EQ = "_aryEq";
    public static final String ARY_NEQ = "_aryNeq";
    public static final String ARY_MAX = "_aryMax";
    public static final String ARY_MIN = "_aryMin";
    public static final String ARY_NUM_MAX = "_aryNumMax";
    public static final String ARY_NUM_MIN = "_aryNumMin";
    public static final String ARY_SORT = "_arySort";
    public static final String ARY_NUM_SORT = "_aryNumSort";
    public static final String IS_SUBSET = "_isSubset";
    public static final String IS_PROPER_SUBSET = "_isProperSubset";
    public static final String IS_SUPERSET = "_isSuperset";
    public static final String IS_PROPER_SUPERSET = "_isProperSuperset";
    public static final String SET_EQ = "_setEq";
    public static final String SET_NEQ = "_setNeq";
    public static final String MOVE_FORWARD = "_moveForward";
    public static final String MOVE_BACKWARD = "_moveBackward";
    public static final String TURN_RIGHT = "_turnRight";
    public static final String TURN_LEFT = "_turnLeft";
    public static final String STOP_RASPI_CAR = "_stopRaspiCar";
    public static final String MEASURE_DISTANCE = "_measureDistance";
    public static final String NEW_LOCK_OBJ = "_newLockObj";
    public static final String NEW_SYNC_TIMER = "_newSyncTimer";
    public static final String SYNC_TIMER_COUNTDOWN = "_syncTimerCountdown";
    public static final String SYNC_TIMER_AWAIT = "_syncTimerAwait";
    public static final String SYNC_TIMER_COUNTDOWN_AND_AWAIT = "_syncTimerCountdownAndAwait";
    public static final String RESET_SYNC_TIMER = "_resetSyncTimer";
    public static final String GET_SYNC_TIMER_COUNT = "_getSyncTimerCount";
    public static final String TRY_LOCK = "_tryLock";
    public static final String LOCK = "_lock";
    public static final String UNLOCK = "_unlock";
    public static final String ADD_EVENT = "_addEvent";
    public static final String GET_EVENT_HANDLER_NAMES = "_getEventHandlerNames";
    public static final String BH_MAIN = "_bhMain";
    public static final String CREATE_SOUND = "_createSound";
    public static final String CREATE_COLOR_FROM_NAME = "_createColorFromName";
    public static final String PUSH_SOUND = "_pushSound";
    public static final String START_TIMER = "_startTimer";
    public static final String GET_TIMER_VAL = "_getTimerVal";
    public static final String STRCAT = "_strcat";
    public static final String IDENTITY = "_identity";  // 恒等写像 (実際に共通コード部には定義しない)
    public static final String CREATE_THREAD_CONTEXT = "_createThreadContext";
    public static final String NOTIFY_THREAD_START = "_notifyThreadStart";
    public static final String NOTIFY_THREAD_END = "_notifyThreadEnd";
    public static final String SET_GLOBAL_VARIABLES = "_setGlobalVariables";
    public static final String COND_WAIT = "_condWait";
    public static final String IS_NUM_INFINITE = "_isNumInfinite";
    public static final String NUM_CLAMP = "_numClamp";
    public static final String OUT_ARG_TEST = "_outArgTest";
    public static final String SAVE_TEXT = "_saveText";
    public static final String LOAD_TEXT = "_loadText";
    public static final String GET_TEXT_FILES = "_getTextFiles";
    public static final String DELETE_TEXT_FILE = "_deleteTextFile";
    public static final String DELETE_TEXT_FILES = "_deleteTextFiles";
  }

  /** JavaScript の組み込み関数名.  */
  public static class JsFuncs {
    public static final String PUSH = "push";
    public static final String POP = "pop";
    public static final String CALL = "call";
    public static final String SPLICE = "splice";
  }

  /** JavaScript の組み込みプロパティ名.  */
  public static class JsProperties {
    public static final String LENGTH = "length";
  }

  /** BhProgram で使用されるプロパティ名. */
  public static class Properties {
    public static final String SET = "_set";
    public static final String GET = "_get";
    public static final String ID = "_id";
  }

  /** BhProgram で使用される変数名. */
  public static class Vars {
    public static final String NIL_SOUND  = "_nilSound";
    public static final String NIL_COLOR = "_nilColor";
    public static final String THREAD_CONTEXT = "_threadContext";
    public static final String CALL_STACK = "_callStack";
    public static final String VAR_STACK = "_varStack";
    public static final String VAR_FRAME = "_varFrame";
    public static final String IDX_CALL_STACK = "_idxCallStack";
    public static final String IDX_NEXT_NODE_INST_ID = "_idxNextNodeInstId";
    public static final String IDX_VAR_STACK = "_idxVarStack";
  }

  /** BhProgram で使用されるラベル名. */
  public static class Label {
    public static final String end = "_end";
  }
}
