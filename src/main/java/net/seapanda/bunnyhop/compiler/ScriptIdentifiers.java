/**
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

/**
 * 変数, 関数, プロパティ等の定義
 */
public class ScriptIdentifiers {

	public static class Funcs {
		public static final String BOOL_TO_STR = "_boolToStr";
		public static final String COLOR_TO_STR = "_colorToStr";
		public static final String TO_STR = "_toStr";
		public static final String STR_TO_NUM = "_strToNum";
		public static final String RANDOM_INT = "_randomInt";
		public static final String PRINTLN = "_println";
		public static final String SLEEP = "_sleep";
		public static final String PLAY_MELODIES = "_playMelodies";
		public static final String COMPARE_COLORS = "_compareColors";
		public static final String ADD_COLOR = "_addColor";
		public static final String SUB_COLOR = "_subColor";
		public static final String DETECT_COLOR = "_detectColor";
		public static final String SAY = "_say";
		public static final String LIGHT_EYE = "_lightEye";
		public static final String SCAN = "_scan";
		public static final String ARY_PUSH = "_aryPush";
		public static final String ARY_POP = "_aryPop";
		public static final String ARY_INSERT = "_aryInsert";
		public static final String ARY_REMOVE = "_aryRemove";
		public static final String ARY_CLEAR = "_aryClear";
		public static final String ARY_ADD_ALL = "_aryAddAll";
		public static final String ARY_GET = "_aryGet";
		public static final String ARY_GET_LAST = "_aryGetLast";
		public static final String ARY_SET = "_arySet";
		public static final String MOVE_FORWARD = "_moveForward";
		public static final String MOVE_BACKWARD = "_moveBackward";
		public static final String TURN_RIGHT = "_turnRight";
		public static final String TURN_LEFT = "_turnLeft";
		public static final String MEASURE_DISTANCE = "_measureDistance";
		public static final String GEN_CALL_OBJ =  "_genCallObj";
		public static final String GEN_LOCK_OBJ = "_genLockObj";
		public static final String GEN_REUSABLE_BARRIER = "_genReusableBarrier";
		public static final String AWAIT = "_awit";
		public static final String GET_NUMBER_WAITING = "_getNumberWaiting";
		public static final String TRY_LOCK = "_tryLock";
		public static final String LOCK = "_lock";
		public static final String UNLOCK = "_unlock";
		public static final String ADD_EVENT = "_addEvent";
		public static final String GET_EVENT_HANDLER_NAMES = "_getEventHandlerNames";
		public static final String BH_MAIN = "_bhMain";
		public static final String CREATE_SOUND = "_createSound";
		public static final String CREATE_COLOR_FROM_NAME = "_createColorFromName";
		public static final String PUSH_SOUND = "_pushSound";
		public static final String CURRENT_TIME_MILLS = "_currentTimeMillis";
		public static final String GET_TIME_SINCE_PROGRAM_STARTED = "_getTimeSinceProgramStarted";
		public static final String STRCAT = "_strcat";
		public static final String LIST_TO_STR = "_listToStr";
		public static final String IDENTITY = "_identity";	//恒等写像 (実際に共通コード部には定義しない)
		public static final String SET_EXCEPTION_NODE_INFO = "_setExceptionNodeInfo";
		public static final String NEW_BH_PROGRAM_EXCEPTION = "_newBhProgramExceptioin";
		public static final String INIT_THIS_OBJ = "_initThisObj";
		public static final String OUT_ARG_TEST = "_outArgTest";
	}

	public static class JsFuncs {
		public static final String IS_FINITE = "isFinite";
		public static final String PUSH = "push";
		public static final String POP = "pop";
		public static final String CALL = "call";
	}


	public static class Properties {
		public static final String OUT_VALS = "_outVals";
		public static final String HAS_BH_EXCEPTION = "_hasBhException";
		public static final String CALL_STACK = "_callStack";
	}

	public static class Vars {
		public static final String NIL_SOUND  = "_nilSound";
		public static final String NIL_COLOR = "_nilColor";
		public static final String PROGRAM_STARTING_TIME = "_programStartingTime";
	}

	public static class Label {
		public static final String end = "_end";
	}
}
