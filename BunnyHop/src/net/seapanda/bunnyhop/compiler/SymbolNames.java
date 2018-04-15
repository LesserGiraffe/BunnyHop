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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 *
 * @author K.Koike
 */
public class SymbolNames {

	public static class VarDecl {
		public static final String NUM_VAR_DECL = "NumVarDecl";
		public static final String NUM_VAR = "NumVar";
		public static final String NUM_VAR_VOID = "NumVarVoid";
		public static final String NUM_LIST_DECL = "NumListDecl";
		public static final String NUM_LIST = "NumList";
		public static final String NUM_EMPTY_LIST = "NumEmptyList";
		public static final String STR_VAR_DECL = "StrVarDecl";
		public static final String STR_VAR = "StrVar";
		public static final String STR_VAR_VOID = "StrVarVoid";
		public static final String STR_LIST_DECL = "StrListDecl";
		public static final String STR_LIST = "StrList";
		public static final String STR_EMPTY_LIST = "StrEmptyList";
		public static final String BOOL_VAR_DECL = "BoolVarDecl";
		public static final String BOOL_VAR = "BoolVar";
		public static final String BOOL_VAR_VOID = "BoolVarVoid";
		public static final String BOOL_LIST_DECL = "BoolListDecl";
		public static final String BOOL_LIST = "BoolList";
		public static final String BOOL_EMPTY_LIST = "BoolEmptyList";
		public static final String SOUND_VAR_DECL = "SoundVarDecl";
		public static final String SOUND_VAR = "SoundVar";
		public static final String SOUND_VAR_VOID = "SoundVarVoid";
		public static final String SOUND_LIST_DECL = "SoundListDecl";
		public static final String SOUND_LIST = "SoundList";
		public static final String SOUND_EMPTY_LIST = "SoundEmptyList";

		public static final String VAR_NAME = "VarName";
		public static final String LIST_NAME = "ListName";
		public static final String NEXT_VAR_DECL = "NextVarDecl";
		public static final HashSet<String> VAR_DECL_LIST =
			new HashSet<>(Arrays.asList(
				NUM_VAR_DECL,
				NUM_LIST_DECL,
				STR_VAR_DECL,
				STR_LIST_DECL,
				BOOL_VAR_DECL,
				BOOL_LIST_DECL,
				SOUND_VAR_DECL,
				SOUND_LIST_DECL));
		public static final HashSet<String> VAR_LIST =
			new HashSet<>(Arrays.asList(
				NUM_VAR,
				NUM_LIST,
				STR_VAR,
				STR_LIST,
				BOOL_VAR,
				BOOL_LIST,
				SOUND_VAR,
				SOUND_LIST));
		public static final Map<String, String> INIT_VAL_MAP =
			new HashMap<String,String>() {{
				put(NUM_VAR_DECL, "0");
				put(NUM_LIST_DECL, "[]");
				put(STR_VAR_DECL, "''");
				put(STR_LIST_DECL, "[]");
				put(BOOL_VAR_DECL, "false");
				put(BOOL_LIST_DECL, "[]");
				put(SOUND_VAR_DECL, CommonCodeDefinition.Vars.NIL_SOUND);
				put(SOUND_LIST_DECL, "[]");
				put(NUM_VAR_VOID, "0");
				put(STR_VAR_VOID, "''");
				put(BOOL_VAR_VOID, "false");
				put(SOUND_VAR_VOID, CommonCodeDefinition.Vars.NIL_SOUND);
				put(NUM_EMPTY_LIST, "[]");
				put(STR_EMPTY_LIST, "[]");
				put(BOOL_EMPTY_LIST, "[]");
				put(SOUND_EMPTY_LIST, "[]");
			}};
		public static final HashSet<String> VAR_VOID_LIST =
			new HashSet<>(Arrays.asList(
				NUM_VAR_VOID,
				STR_VAR_VOID,
				BOOL_VAR_VOID,
				SOUND_VAR_VOID,
				NUM_EMPTY_LIST,
				STR_EMPTY_LIST,
				BOOL_EMPTY_LIST,
				SOUND_EMPTY_LIST
			));
	}

	public static class Stat {
		public static final String VOID_STAT = "VoidStat";
		public static final String STAT_LIST = "StatList";
		public static final String NEXT_STAT = "NextStat";
	}

	/**
	 * コンパイル時に無視される文
	 * */
	public static class IgnoredStat {
		public static final String ANY_ASSIGN_STAT = "AnyAssignStat";
		public static final String ANY_ARRAY_APPEND_STAT =  "AnyArrayAppendStat";
		public static final String ANY_ARRAY_CLEAR_STAT = "AnyArrayClearStat";
		public static final String ANY_ARRAY_INSERT_STAT = "AnyArrayInsertStat";
		public static final String ANY_ARRAY_POP_STAT = "AnyArrayPopStat";
		public static final String ANY_ARRAY_PUSH_STAT = "AnyArrayPushStat";
		public static final String ANY_ARRAY_REMOVE_STAT = "AnyArrayRemoveStat";
		public static final String ANY_ARRAY_SET_STAT = "AnyArraySetStat";


		public static final HashSet<String> LIST =
				new HashSet<>(Arrays.asList(
					ANY_ASSIGN_STAT,
					ANY_ARRAY_APPEND_STAT,
					ANY_ARRAY_CLEAR_STAT,
					ANY_ARRAY_INSERT_STAT,
					ANY_ARRAY_POP_STAT,
					ANY_ARRAY_PUSH_STAT,
					ANY_ARRAY_REMOVE_STAT,
					ANY_ARRAY_SET_STAT));
	}

	public static class AssignStat {

		public static final String NUM_ASSIGN_STAT = "NumAssignStat";
		public static final String NUM_ADD_ASSIGN_STAT = "NumAddAssignStat";
		public static final String STR_ASSIGN_STAT = "StrAssignStat";
		public static final String BOOL_ASSIGN_STAT = "BoolAssignStat";
		public static final String SOUND_ASSIGN_STAT = "SoundAssignStat";
		public static final String LEFT_VAR = "LeftVar";
		public static final HashSet<String> LIST =
			new HashSet<>(Arrays.asList(
				NUM_ASSIGN_STAT,
				NUM_ADD_ASSIGN_STAT,
				STR_ASSIGN_STAT,
				BOOL_ASSIGN_STAT,
				SOUND_ASSIGN_STAT));
	}

	public static class ControlStat {

		public static final String LOCAL_VAR_DECL = "LocalVarDecl";
		public static final String IF_STAT = "IfStat";
		public static final String IF_ELSE_STAT = "IfElseStat";
		public static final String WHILE_STAT = "WhileStat";
		public static final String REPEAT_STAT = "RepeatStat";
		public static final String THEN_STAT = "ThenStat";
		public static final String ELSE_STAT = "ElseStat";
		public static final String COND_EXP = "CondExp";
		public static final String LOOP_STAT = "LoopStat";
		public static final String COMPOUND_STAT = "CompoundStat";
		public static final String CONTINUE_STAT = "ContinueStat";
		public static final String BREAK_STAT = "BreakStat";
		public static final String RETURN_STAT = "ReturnStat";
		public static final HashSet<String> LIST =
			new HashSet<>(Arrays.asList(
				IF_STAT,
				IF_ELSE_STAT,
				WHILE_STAT,
				REPEAT_STAT,
				COMPOUND_STAT,
				CONTINUE_STAT,
				BREAK_STAT,
				RETURN_STAT));
	}

	public static class BinaryExp {

		public static final String FOUR_ARITH_EXP = "FourArithExp";
		public static final String BINARY_BOOL_EXP = "BinaryBoolExp";
		public static final String MOD_EXP = "ModExp";
		public static final String NUM_COMP_EXP = "NumCompExp";
		public static final String STR_COMP_EXP = "StrCompExp";
		public static final String APPEND_STR_EXP = "AppendStrExp";
		public static final String LEFT_EXP = "LeftExp";
		public static final String RIGHT_EXP = "RightExp";
		public static final String OPERATOR ="Operator";
		public static final HashSet<String> LIST =
			new HashSet<>(Arrays.asList(
				FOUR_ARITH_EXP,
				MOD_EXP,
				BINARY_BOOL_EXP,
				NUM_COMP_EXP,
				STR_COMP_EXP,
				APPEND_STR_EXP));	//二項演算子リスト
		public static final HashSet<String> ARITH_EXCEPTION_EXP =
			new HashSet<>(Arrays.asList(
				FOUR_ARITH_EXP,
				MOD_EXP));	//!< 算術演算例外を発生させる式のノード名
		public static final Map<String, String> OPERATOR_MAP =
			new HashMap<String,String>() {{
				put("add", " + ");
				put("sub", " - ");
				put("div", " / ");
				put("mul", " * ");
				put("and", " && ");
				put("or", " || ");
				put("eq", " === ");
				put("neq", " !== ");
				put("lt", " < ");
				put("lte", " <= ");
				put("gt", " > ");
				put("gte", " >= ");}};
	}

	public static class UnaryExp {

		public static final String NOT_EXP = "NotExp";
		public static final String NEG_EXP = "NegExp";
		public static final String PRIMARY_EXP = "PrimaryExp";
		public static final HashSet<String> LIST =
			new HashSet<>(Arrays.asList(
				NOT_EXP,
				NEG_EXP));

		public static final Map<String, String> OPERATOR_MAP =
			new HashMap<String,String>() {{
				put(NOT_EXP, "!");
				put(NEG_EXP, "-");}};
	}

	public static class PreDefFunc {

		public static final String ARG = "Arg";
		public static final String OUT_ARG = "OutArg";
		public static final String OPTION = "Option";

		//ノード名
		public static final String NUM_TO_STR_EXP = "NumToStrExp";
		public static final String STR_TO_NUM_EXP = "StrToNumExp";
		public static final String BOOL_TO_STR_EXP = "BoolToStrExp";
		public static final String SCAM_EXP = "ScanExp";
		public static final String RAMDOM_INT_EXP = "RandomIntExp";
		public static final String NUM_ROUND_EXP = "NumRoundExp";
		public static final String ABS_EXP = "AbsExp";
		public static final String MAX_MIN_EXP = "MaxMinExp";
		public static final String MEASURE_DISTANCE_EXP = "MeasureDistanceExp";
		public static final String MELODY_EXP = "MelodyExp";
		public static final String PRINT_STAT = "PrintStat";
		public static final String PRINT_NUM_STAT = "PrintNumStat";
		public static final String MOVE_STAT = "MoveStat";
		public static final String SLEEP_STAT = "SleepStat";
		public static final String PLAY_MELODY_STAT = "PlayMelodyStat";
		public static final String PLAY_SOUND_LIST_STAT = "PlaySoundListStat";
		public static final String SAY_STAT = "SayStat";

		//オプション名
		public static final String OPT_ROUND = "round";
		public static final String OPT_CEIL = "ceil";
		public static final String OPT_FLOOR = "floor";
		public static final String OPT_MAX = "max";
		public static final String OPT_MIN = "min";
		public static final String OPT_MOVE_FORWARD = "moveForward";
		public static final String OPT_MOVE_BACKWARD = "moveBackward";
		public static final String OPT_TURN_RIGHT = "turnRight";
		public static final String OPT_TURN_LEFT = "turnLeft";

		public static final HashSet<String> PREDEF_FUNC_CALL_EXP_LIST =
			new HashSet<>(Arrays.asList(
				NUM_TO_STR_EXP,
				STR_TO_NUM_EXP,
				BOOL_TO_STR_EXP,
				SCAM_EXP,
				NUM_ROUND_EXP,
				RAMDOM_INT_EXP,
				ABS_EXP,
				MAX_MIN_EXP,
				MEASURE_DISTANCE_EXP,
				MELODY_EXP));	//!< 定義済み関数式のリスト

		public static final HashSet<String> PREDEF_FUNC_CALL_STAT_LIST =
			new HashSet<>(Arrays.asList(
				PRINT_STAT,
				PRINT_NUM_STAT,
				MOVE_STAT,
				SLEEP_STAT,
				PLAY_MELODY_STAT,
				PLAY_SOUND_LIST_STAT,
				SAY_STAT,
				Array.STR_ARRAY_PUSH_STAT,
				Array.STR_ARRAY_POP_STAT,
				Array.STR_ARRAY_SET_STAT,
				Array.STR_ARRAY_INSERT_STAT,
				Array.STR_ARRAY_REMOVE_STAT,
				Array.STR_ARRAY_APPEND_STAT,
				Array.STR_ARRAY_CLEAR_STAT,

				Array.NUM_ARRAY_PUSH_STAT,
				Array.NUM_ARRAY_POP_STAT,
				Array.NUM_ARRAY_SET_STAT,
				Array.NUM_ARRAY_INSERT_STAT,
				Array.NUM_ARRAY_REMOVE_STAT,
				Array.NUM_ARRAY_APPEND_STAT,
				Array.NUM_ARRAY_CLEAR_STAT,

				Array.BOOL_ARRAY_PUSH_STAT,
				Array.BOOL_ARRAY_POP_STAT,
				Array.BOOL_ARRAY_SET_STAT,
				Array.BOOL_ARRAY_INSERT_STAT,
				Array.BOOL_ARRAY_REMOVE_STAT,
				Array.BOOL_ARRAY_APPEND_STAT,
				Array.BOOL_ARRAY_CLEAR_STAT,

				Array.SOUND_ARRAY_PUSH_STAT,
				Array.SOUND_ARRAY_POP_STAT,
				Array.SOUND_ARRAY_INSERT_STAT,
				Array.SOUND_ARRAY_REMOVE_STAT,
				Array.SOUND_ARRAY_APPEND_STAT,
				Array.SOUND_ARRAY_CLEAR_STAT,
				Array.SOUND_ARRAY_SET_STAT

					)
			);	//!< 定義済み関数文のリスト

		public static final Map<List<String>, String> PREDEF_FUNC_NAME_MAP =
			new HashMap<List<String>, String>() {{
				put(Arrays.asList(NUM_TO_STR_EXP), "String");
				put(Arrays.asList(BOOL_TO_STR_EXP), CommonCodeDefinition.Funcs.BOOL_TO_STR);
				put(Arrays.asList(STR_TO_NUM_EXP), CommonCodeDefinition.Funcs.STR_TO_NUM);
				put(Arrays.asList(PRINT_STAT), CommonCodeDefinition.Funcs.PRINTLN);
				put(Arrays.asList(PRINT_NUM_STAT), CommonCodeDefinition.Funcs.PRINTLN);
				put(Arrays.asList(SCAM_EXP), CommonCodeDefinition.Funcs.SCAN);
				put(Arrays.asList(NUM_ROUND_EXP, OPT_ROUND), "Math.round");
				put(Arrays.asList(NUM_ROUND_EXP, OPT_CEIL), "Math.ceil");
				put(Arrays.asList(NUM_ROUND_EXP, OPT_FLOOR), "Math.floor");
				put(Arrays.asList(ABS_EXP), "Math.abs");
				put(Arrays.asList(MAX_MIN_EXP, OPT_MAX), "Math.max");
				put(Arrays.asList(MAX_MIN_EXP, OPT_MIN), "Math.min");
				put(Arrays.asList(RAMDOM_INT_EXP), CommonCodeDefinition.Funcs.RANDOM_INT);
				put(Arrays.asList(MEASURE_DISTANCE_EXP), CommonCodeDefinition.Funcs.MEASURE_DISTANCE);
				put(Arrays.asList(MELODY_EXP), CommonCodeDefinition.Funcs.PUSH_SOUND);
				put(Arrays.asList(MOVE_STAT, OPT_MOVE_FORWARD), CommonCodeDefinition.Funcs.MOVE_FORWARD);
				put(Arrays.asList(MOVE_STAT, OPT_MOVE_BACKWARD), CommonCodeDefinition.Funcs.MOVE_BACKWARD);
				put(Arrays.asList(MOVE_STAT, OPT_TURN_RIGHT), CommonCodeDefinition.Funcs.TURN_RIGHT);
				put(Arrays.asList(MOVE_STAT, OPT_TURN_LEFT), CommonCodeDefinition.Funcs.TURN_LEFT);
				put(Arrays.asList(SLEEP_STAT), CommonCodeDefinition.Funcs.SLEEP);
				put(Arrays.asList(PLAY_MELODY_STAT), CommonCodeDefinition.Funcs.PLAY_MELODIES);
				put(Arrays.asList(PLAY_SOUND_LIST_STAT), CommonCodeDefinition.Funcs.PLAY_MELODIES);
				put(Arrays.asList(SAY_STAT), CommonCodeDefinition.Funcs.SAY);

				put(Arrays.asList(Array.STR_ARRAY_PUSH_STAT), CommonCodeDefinition.Funcs.ARY_PUSH);
				put(Arrays.asList(Array.STR_ARRAY_POP_STAT), CommonCodeDefinition.Funcs.ARY_POP);
				put(Arrays.asList(Array.STR_ARRAY_GET_EXP), CommonCodeDefinition.Funcs.ARY_GET);
				put(Arrays.asList(Array.STR_ARRAY_GET_LAST_EXP), CommonCodeDefinition.Funcs.ARY_GET);
				put(Arrays.asList(Array.STR_ARRAY_SET_STAT), CommonCodeDefinition.Funcs.ARY_SET);
				put(Arrays.asList(Array.STR_ARRAY_INSERT_STAT), CommonCodeDefinition.Funcs.ARY_INSERT);
				put(Arrays.asList(Array.STR_ARRAY_REMOVE_STAT), CommonCodeDefinition.Funcs.ARY_REMOVE);
				put(Arrays.asList(Array.STR_ARRAY_APPEND_STAT), CommonCodeDefinition.Funcs.ARY_ADD_ALL);
				put(Arrays.asList(Array.STR_ARRAY_CLEAR_STAT), CommonCodeDefinition.Funcs.ARY_CLEAR);

				put(Arrays.asList(Array.NUM_ARRAY_PUSH_STAT), CommonCodeDefinition.Funcs.ARY_PUSH);
				put(Arrays.asList(Array.NUM_ARRAY_POP_STAT), CommonCodeDefinition.Funcs.ARY_POP);
				put(Arrays.asList(Array.NUM_ARRAY_GET_EXP), CommonCodeDefinition.Funcs.ARY_GET);
				put(Arrays.asList(Array.NUM_ARRAY_GET_LAST_EXP), CommonCodeDefinition.Funcs.ARY_GET);
				put(Arrays.asList(Array.NUM_ARRAY_SET_STAT), CommonCodeDefinition.Funcs.ARY_SET);
				put(Arrays.asList(Array.NUM_ARRAY_INSERT_STAT), CommonCodeDefinition.Funcs.ARY_INSERT);
				put(Arrays.asList(Array.NUM_ARRAY_REMOVE_STAT), CommonCodeDefinition.Funcs.ARY_REMOVE);
				put(Arrays.asList(Array.NUM_ARRAY_APPEND_STAT), CommonCodeDefinition.Funcs.ARY_ADD_ALL);
				put(Arrays.asList(Array.NUM_ARRAY_CLEAR_STAT), CommonCodeDefinition.Funcs.ARY_CLEAR);

				put(Arrays.asList(Array.BOOL_ARRAY_PUSH_STAT), CommonCodeDefinition.Funcs.ARY_PUSH);
				put(Arrays.asList(Array.BOOL_ARRAY_POP_STAT), CommonCodeDefinition.Funcs.ARY_POP);
				put(Arrays.asList(Array.BOOL_ARRAY_GET_EXP), CommonCodeDefinition.Funcs.ARY_GET);
				put(Arrays.asList(Array.BOOL_ARRAY_GET_LAST_EXP), CommonCodeDefinition.Funcs.ARY_GET);
				put(Arrays.asList(Array.BOOL_ARRAY_SET_STAT), CommonCodeDefinition.Funcs.ARY_SET);
				put(Arrays.asList(Array.BOOL_ARRAY_INSERT_STAT), CommonCodeDefinition.Funcs.ARY_INSERT);
				put(Arrays.asList(Array.BOOL_ARRAY_REMOVE_STAT), CommonCodeDefinition.Funcs.ARY_REMOVE);
				put(Arrays.asList(Array.BOOL_ARRAY_APPEND_STAT), CommonCodeDefinition.Funcs.ARY_ADD_ALL);
				put(Arrays.asList(Array.BOOL_ARRAY_CLEAR_STAT), CommonCodeDefinition.Funcs.ARY_CLEAR);

				put(Arrays.asList(Array.SOUND_ARRAY_PUSH_STAT), CommonCodeDefinition.Funcs.ARY_PUSH);
				put(Arrays.asList(Array.SOUND_ARRAY_POP_STAT), CommonCodeDefinition.Funcs.ARY_POP);
				put(Arrays.asList(Array.SOUND_ARRAY_INSERT_STAT),  CommonCodeDefinition.Funcs.ARY_INSERT);
				put(Arrays.asList(Array.SOUND_ARRAY_REMOVE_STAT), CommonCodeDefinition.Funcs.ARY_REMOVE);
				put(Arrays.asList(Array.SOUND_ARRAY_APPEND_STAT), CommonCodeDefinition.Funcs.ARY_ADD_ALL);
				put(Arrays.asList(Array.SOUND_ARRAY_CLEAR_STAT), CommonCodeDefinition.Funcs.ARY_CLEAR);
				put(Arrays.asList(Array.SOUND_ARRAY_GET_EXP), CommonCodeDefinition.Funcs.ARY_GET);
				put(Arrays.asList(Array.SOUND_ARRAY_GET_LAST_EXP), CommonCodeDefinition.Funcs.ARY_GET);
				put(Arrays.asList(Array.SOUND_ARRAY_SET_STAT), CommonCodeDefinition.Funcs.ARY_SET);
			}};	//!<  (関数呼び出しノード名, 関数呼び出しオプション...) -> 関数名
	}

	public static class UserDefFunc {

		public static final String ARG = "Arg";
		public static final String OUT_ARG = "OutArg";
		public static final String NEXT_ARG = "NextArg";
		public static final String ARG_VOID = "ArgVoid";
		public static final String FUNC_DEF_SCTN = "FuncDefSctn";
		public static final String PARAM_DECL = "ParamDecl";
		public static final String OUT_PARAM_DECL = "OutParamDecl";
		public static final String FUNC_NAME = "FuncName";
		public static final String VOID_FUNC_DEF = "VoidFuncDef";
		public static final String VOID_FUNC_CALL = "VoidFuncCall";

		public static final HashSet<String> USER_DEF_FUNC_LIST =
			new HashSet<>(Arrays.asList(
				VOID_FUNC_DEF));	//!< 関数定義ノードのリスト

		public static final HashSet<String> USER_DEF_FUNC_CALL_STAT_LIST =
			new HashSet<>(Arrays.asList(VOID_FUNC_CALL));	//!< ユーザ定義関数文のリスト
	}

	public static class Event {

		public static final String KEY_PRESS_EVENT = "KeyPressedEvent";
		public static final String DELAYED_START_EVENT = "DelayedStartEvent";
		public static final String KEY_CODE = "KeyCode";
		public static final String DELAY_TIME = "DelayTime";

		public static final HashSet<String> LIST =
				new HashSet<>(Arrays.asList(
					KEY_PRESS_EVENT,
					DELAYED_START_EVENT));	//!< イベントノードのリスト
	}

	public static class Literal {
		public static final String STR_LITERAL = "StrLiteral";
		public static final String LINE_FEED = "LineFeed";
		public static final String NUM_LITERAL = "NumLiteral";
		public static final String BOOL_LITERAL = "BoolLiteral";
		public static final String FREQ_SOUND_LITERAL = "FreqSoundLiteral";
		public static final String SCALE_SOUND_LITERAL = "ScaleSoundLiteral";
		public static final String SOUND_LITERAL_VOID = "SoundLiteralVoid";
		public static final String MELODY_EXP_VOID = "MelodyExpVoid";
		public static final String STR_EMPTY_LIST = VarDecl.STR_EMPTY_LIST;
		public static final String NUM_EMPTY_LIST = VarDecl.NUM_EMPTY_LIST;
		public static final String BOOL_EMPTY_LIST = VarDecl.BOOL_EMPTY_LIST;
		public static final String SOUND_EMPTY_LIST = VarDecl.SOUND_EMPTY_LIST;
		public static final String ANY_ENPTY_LIST = "AnyEmptyList";
		public static final HashSet<String> LIST =
			new HashSet<>(Arrays.asList(
				STR_LITERAL,
				LINE_FEED,
				NUM_LITERAL,
				BOOL_LITERAL,
				FREQ_SOUND_LITERAL,
				SOUND_LITERAL_VOID,
				SCALE_SOUND_LITERAL,
				STR_EMPTY_LIST,
				NUM_EMPTY_LIST,
				BOOL_EMPTY_LIST,
				SOUND_EMPTY_LIST,
				ANY_ENPTY_LIST,
				MELODY_EXP_VOID));
		public static final HashSet<String> ARRAY_TYPES =
			new HashSet<>(Arrays.asList(
				STR_EMPTY_LIST,
				NUM_EMPTY_LIST,
				BOOL_EMPTY_LIST,
				ANY_ENPTY_LIST,
				SOUND_EMPTY_LIST,
				MELODY_EXP_VOID));

		public static class Sound {
			public static final String DURATION = "Duration";
			public static final String FREQUENCY = "Frequency";
			public static final String SCALE_SOUND = "ScaleSound";
			public static final String OCTAVE = "Octave";
		}
	}

	public static class Array {
		public static final String ARRAY = "Array";
		public static final String INDEX = "Index";

		public static final String STR_ARRAY_PUSH_STAT = "StrArrayPushStat";
		public static final String STR_ARRAY_POP_STAT = "StrArrayPopStat";
		public static final String STR_ARRAY_INSERT_STAT = "StrArrayInsertStat";
		public static final String STR_ARRAY_REMOVE_STAT = "StrArrayRemoveStat";
		public static final String STR_ARRAY_APPEND_STAT = "StrArrayAppendStat";
		public static final String STR_ARRAY_CLEAR_STAT = "StrArrayClearStat";
		public static final String STR_ARRAY_GET_EXP = "StrArrayGetExp";
		public static final String STR_ARRAY_GET_LAST_EXP = "StrArrayGetLastExp";
		public static final String STR_ARRAY_SET_STAT = "StrArraySetStat";
		public static final String STR_ARRAY_LEN_EXP = "StrArrayLengthExp";

		public static final String NUM_ARRAY_PUSH_STAT = "NumArrayPushStat";
		public static final String NUM_ARRAY_POP_STAT = "NumArrayPopStat";
		public static final String NUM_ARRAY_INSERT_STAT = "NumArrayInsertStat";
		public static final String NUM_ARRAY_REMOVE_STAT = "NumArrayRemoveStat";
		public static final String NUM_ARRAY_APPEND_STAT = "NumArrayAppendStat";
		public static final String NUM_ARRAY_CLEAR_STAT = "NumArrayClearStat";
		public static final String NUM_ARRAY_GET_EXP = "NumArrayGetExp";
		public static final String NUM_ARRAY_GET_LAST_EXP = "NumArrayGetLastExp";
		public static final String NUM_ARRAY_SET_STAT = "NumArraySetStat";
		public static final String NUM_ARRAY_LEN_EXP = "NumArrayLengthExp";

		public static final String BOOL_ARRAY_PUSH_STAT = "BoolArrayPushStat";
		public static final String BOOL_ARRAY_POP_STAT = "BoolArrayPopStat";
		public static final String BOOL_ARRAY_INSERT_STAT = "BoolArrayInsertStat";
		public static final String BOOL_ARRAY_REMOVE_STAT = "BoolArrayRemoveStat";
		public static final String BOOL_ARRAY_APPEND_STAT = "BoolArrayAppendStat";
		public static final String BOOL_ARRAY_CLEAR_STAT = "BoolArrayClearStat";
		public static final String BOOL_ARRAY_GET_EXP = "BoolArrayGetExp";
		public static final String BOOL_ARRAY_GET_LAST_EXP = "BoolArrayGetLastExp";
		public static final String BOOL_ARRAY_SET_STAT = "BoolArraySetStat";
		public static final String BOOL_ARRAY_LEN_EXP = "BoolArrayLengthExp";

		public static final String SOUND_ARRAY_PUSH_STAT = "SoundArrayPushStat";
		public static final String SOUND_ARRAY_POP_STAT = "SoundArrayPopStat";
		public static final String SOUND_ARRAY_INSERT_STAT = "SoundArrayInsertStat";
		public static final String SOUND_ARRAY_REMOVE_STAT = "SoundArrayRemoveStat";
		public static final String SOUND_ARRAY_APPEND_STAT = "SoundArrayAppendStat";
		public static final String SOUND_ARRAY_CLEAR_STAT = "SoundArrayClearStat";
		public static final String SOUND_ARRAY_GET_EXP = "SoundArrayGetExp";
		public static final String SOUND_ARRAY_GET_LAST_EXP = "SoundArrayGetLastExp";
		public static final String SOUND_ARRAY_SET_STAT = "SoundArraySetStat";
		public static final String SOUND_ARRAY_LEN_EXP = "SoundArrayLengthExp";


		public static final String ANY_ARRAY_LEN_EXP = "AnyArrayLengthExp";

		public static final HashSet<String> LENGTH_EXP_LIST =
			new HashSet<>(Arrays.asList(
				STR_ARRAY_LEN_EXP,
				NUM_ARRAY_LEN_EXP,
				BOOL_ARRAY_LEN_EXP,
				SOUND_ARRAY_LEN_EXP,
				ANY_ARRAY_LEN_EXP));

		public static final HashSet<String> GET_EXP_LIST =
			new HashSet<>(Arrays.asList(NUM_ARRAY_GET_EXP,
				NUM_ARRAY_GET_LAST_EXP,
				STR_ARRAY_GET_EXP,
				STR_ARRAY_GET_LAST_EXP,
				BOOL_ARRAY_GET_EXP,
				BOOL_ARRAY_GET_LAST_EXP,
				SOUND_ARRAY_GET_EXP,
				SOUND_ARRAY_GET_LAST_EXP));
	}

	public static class Undefined {

		public static final Map<String, String> SUBSTITUTE_LITERAL_MAP =
			new HashMap<String, String>() {{
				put(Array.NUM_ARRAY_GET_EXP, "0");
				put(Array.NUM_ARRAY_GET_LAST_EXP, "0");
				put(Array.STR_ARRAY_GET_EXP, "''");
				put(Array.STR_ARRAY_GET_LAST_EXP, "''");
				put(Array.BOOL_ARRAY_GET_EXP, "false");
				put(Array.BOOL_ARRAY_GET_LAST_EXP, "false");
				put(Array.SOUND_ARRAY_GET_EXP, CommonCodeDefinition.Vars.NIL_SOUND);
				put(Array.SOUND_ARRAY_GET_LAST_EXP, CommonCodeDefinition.Vars.NIL_SOUND);
			}};	//undefinedが返った場合の代替値
	}
}
