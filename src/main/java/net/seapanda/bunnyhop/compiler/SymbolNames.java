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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.seapanda.bunnyhop.model.syntaxsymbol.SyntaxSymbol;

/**
 * {@link SyntaxSymbol} が持つシンボル名の定義.
 *
 * @author K.Koike
 */
public class SymbolNames {

  /** 変数宣言に関するシンボル名. */
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

    public static final String COLOR_VAR_DECL = "ColorVarDecl";
    public static final String COLOR_VAR = "ColorVar";
    public static final String COLOR_VAR_VOID = "ColorVarVoid";
    public static final String COLOR_LIST_DECL = "ColorListDecl";
    public static final String COLOR_LIST = "ColorList";
    public static final String COLOR_EMPTY_LIST = "ColorEmptyList";

    public static final String SOUND_VAR_DECL = "SoundVarDecl";
    public static final String SOUND_VAR = "SoundVar";
    public static final String SOUND_VAR_VOID = "SoundVarVoid";
    public static final String SOUND_LIST_DECL = "SoundListDecl";
    public static final String SOUND_LIST = "SoundList";
    public static final String SOUND_EMPTY_LIST = "SoundEmptyList";

    public static final String VAR_NAME = "VarName";
    public static final String LIST_NAME = "ListName";
    public static final String NEXT_VAR_DECL = "NextVarDecl";

    public static final HashSet<String> LIST =
        new HashSet<>(Arrays.asList(
            NUM_VAR_DECL,
            NUM_LIST_DECL,
            STR_VAR_DECL,
            STR_LIST_DECL,
            BOOL_VAR_DECL,
            BOOL_LIST_DECL,
            COLOR_VAR_DECL,
            COLOR_LIST_DECL,
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
            COLOR_VAR,
            COLOR_LIST,
            SOUND_VAR,
            SOUND_LIST));

    public static final Map<String, String> INIT_VAL_MAP =
        new HashMap<String, String>() {{
            put(NUM_VAR_DECL, "0");
            put(NUM_LIST_DECL, "[]");
            put(STR_VAR_DECL, "''");
            put(STR_LIST_DECL, "[]");
            put(BOOL_VAR_DECL, "false");
            put(BOOL_LIST_DECL, "[]");
            put(COLOR_VAR_DECL, ScriptIdentifiers.Vars.NIL_COLOR);
            put(COLOR_LIST_DECL, "[]");
            put(SOUND_VAR_DECL, ScriptIdentifiers.Vars.NIL_SOUND);
            put(SOUND_LIST_DECL, "[]");
            put(NUM_VAR_VOID, "0");
            put(STR_VAR_VOID, "''");
            put(BOOL_VAR_VOID, "false");
            put(COLOR_VAR_VOID, ScriptIdentifiers.Vars.NIL_COLOR);
            put(SOUND_VAR_VOID, ScriptIdentifiers.Vars.NIL_SOUND);
            put(NUM_EMPTY_LIST, "[]");
            put(STR_EMPTY_LIST, "[]");
            put(BOOL_EMPTY_LIST, "[]");
            put(COLOR_EMPTY_LIST, "[]");
            put(SOUND_EMPTY_LIST, "[]");
          }};

    public static final HashSet<String> VAR_VOID_LIST =
        new HashSet<>(Arrays.asList(
            NUM_VAR_VOID,
            STR_VAR_VOID,
            BOOL_VAR_VOID,
            COLOR_VAR_VOID,
            SOUND_VAR_VOID,
            NUM_EMPTY_LIST,
            STR_EMPTY_LIST,
            BOOL_EMPTY_LIST,
            COLOR_EMPTY_LIST,
            SOUND_EMPTY_LIST));

    public static final HashSet<String> VAR_NAME_CNCTR_LIST =
        new HashSet<>(Arrays.asList(
            VAR_NAME,
            LIST_NAME));
  }

  /** 特定の分類を設けない statement に関するシンボル名. */
  public static class Stat {
    public static final String VOID_STAT = "VoidStat";
    public static final String STAT_LIST = "StatList";
    public static final String NEXT_STAT = "NextStat";
  }

  /** コンパイル時に無視される文. */
  public static class StatToBeIgnored {
    public static final String COMMENT_PART = "CommentPart";

    public static final HashSet<String> LIST =
        new HashSet<>(Arrays.asList(
            COMMENT_PART));
  }

  /** 代入文に関するシンボル名. */
  public static class AssignStat {

    public static final String NUM_ADD_ASSIGN_STAT = "NumAddAssignStat";
    public static final String ANY_ASSIGN_STAT = "AnyAssignStat";
    public static final String LEFT_VAR = "LeftVar";
    public static final HashSet<String> LIST =
        new HashSet<>(Arrays.asList(
            ANY_ASSIGN_STAT,
            NUM_ADD_ASSIGN_STAT));
  }

  /** 制御文に関するシンボル名. */
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
    public static final String CRITICAL_SECTION_STAT = "CriticalSectionStat";
    public static final String EXCLUSIVE_STAT = "ExclusiveStat";
    public static final HashSet<String> LIST =
        new HashSet<>(Arrays.asList(
            IF_STAT,
            IF_ELSE_STAT,
            WHILE_STAT,
            REPEAT_STAT,
            COMPOUND_STAT,
            CONTINUE_STAT,
            BREAK_STAT,
            RETURN_STAT,
            CRITICAL_SECTION_STAT));
  }

  /** 二項演算式に関するシンボル名. */
  public static class BinaryExp {

    public static final String FOUR_ARITH_EXP = "FourArithExp";
    public static final String BINARY_BOOL_EXP = "BinaryBoolExp";
    public static final String MOD_EXP = "ModExp";
    public static final String NUM_COMP_EXP = "NumCompExp";
    public static final String STR_COMP_EXP = "StrCompExp";
    public static final String LEFT_EXP = "LeftExp";
    public static final String RIGHT_EXP = "RightExp";
    public static final String OPERATOR = "Operator";

    public static final HashSet<String> LIST =
        new HashSet<>(Arrays.asList(
            FOUR_ARITH_EXP,
            MOD_EXP,
            BINARY_BOOL_EXP,
            NUM_COMP_EXP,
            STR_COMP_EXP));  //二項演算子リスト

    public static final HashSet<String> ARITH_EXCEPTION_EXP =
        new HashSet<>(Arrays.asList(
            FOUR_ARITH_EXP,
            MOD_EXP));  // 算術演算例外を発生させる式のノード名

    public static final Map<String, String> OPERATOR_MAP =
        new HashMap<String, String>() {{
            put("add", " + ");
            put("sub", " - ");
            put("div", " / ");
            put("mul", " * ");
            put("mod", " % ");
            put("and", " && ");
            put("or", " || ");
            put("eq", " === ");
            put("neq", " !== ");
            put("lt", " < ");
            put("lte", " <= ");
            put("gt", " > ");
            put("gte", " >= ");
          }};
  }

  /** 単項演算式に関するシンボル名. */
  public static class UnaryExp {

    public static final String NOT_EXP = "NotExp";
    public static final String NEG_EXP = "NegExp";
    public static final String PRIMARY_EXP = "PrimaryExp";
    public static final HashSet<String> LIST =
        new HashSet<>(Arrays.asList(
            NOT_EXP,
            NEG_EXP));

    public static final Map<String, String> OPERATOR_MAP =
        new HashMap<String, String>() {{
            put(NOT_EXP, "!");
            put(NEG_EXP, "-");
          }};
  }

  /** BhProgram にあらかじめ定義されたメソッドを呼び出す処理に関するシンボル名. */
  public static class PreDefFunc {

    public static final String ARG = "Arg";
    public static final String OUT_ARG = "OutArg";
    public static final String OPTION = "Option";

    //ノード名
    public static final String STR_TO_NUM_EXP = "StrToNumExp";
    public static final String ANY_TO_STR_EXP = "AnyToStrExp";
    public static final String SCAM_EXP = "ScanExp";
    public static final String RAMDOM_INT_EXP = "RandomIntExp";
    public static final String NUM_ROUND_EXP = "NumRoundExp";
    public static final String ABS_EXP = "AbsExp";
    public static final String MAX_MIN_EXP = "MaxMinExp";
    public static final String MEASURE_DISTANCE_EXP = "MeasureDistanceExp";
    public static final String MELODY_EXP = "MelodyExp";
    public static final String ANY_COMP_EXP = "AnyCompExp";
    public static final String BINARY_COLOR_EXP = "BinaryColorExp";
    public static final String DETECT_COLOR_EXP = "DetectColorExp";
    public static final String GET_TIME_SINCE_PROGRAM_STARTED_EXP = "GetTimeSinceProgramStartedExp";
    public static final String STR_CHAIN_LINK_EXP = "StrChainLinkExp";
    public static final String STR_CHAIN_EXP = "StrChainExp";
    public static final String GET_SYNC_TIMER_COUNT_EXP = "GetSyncTimerCountExp";
    public static final String ANY_ARRAY_TO_STR_EXP = "AnyArrayToStrExp";
    public static final String OUT_ARG_TEST_EXP = "OutArgTestExp";
    public static final String PRINT_STAT = "PrintStat";
    public static final String MOVE_STAT = "MoveStat";
    public static final String STOP_RASPI_CAR_STAT = "StopRaspiCarStat";
    public static final String SLEEP_STAT = "SleepStat";
    public static final String PLAY_MELODY_STAT = "PlayMelodyStat";
    public static final String PLAY_SOUND_LIST_STAT = "PlaySoundListStat";
    public static final String SAY_STAT = "SayStat";
    public static final String LIGHT_EYE_STAT = "LightEyeStat";
    public static final String SYNC_TIMER_AWAIT_WITH_TIMEOUT_STAT = "SyncTimerAwaitWithTimeoutStat";
    public static final String SYNC_TIMER_COUNTDOWN_AND_AWAIT_STAT = 
        "SyncTimerCountdownAndAwaitStat";
    public static final String SYNC_TIMER_COUNTDOWN_AND_AWAIT_WITH_TIMEOUT_STAT = 
        "SyncTimerCountdownAndAwaitWithTimeoutStat";
    public static final String SYNC_TIMER_AWAIT_STAT = "SyncTimerAwaitStat";
    public static final String RESET_SYNC_TIMER_STAT = "ResetSyncTimerStat";
    public static final String SYNC_TIMER_COUNTDOWN_STAT = "SyncTimerCountdownStat";

    //オプション名
    public static final String OPT_ROUND = "round";
    public static final String OPT_CEIL = "ceil";
    public static final String OPT_FLOOR = "floor";
    public static final String OPT_TRUNC = "trunc";
    public static final String OPT_MAX = "max";
    public static final String OPT_MIN = "min";
    public static final String OPT_MOVE_FORWARD = "moveForward";
    public static final String OPT_MOVE_BACKWARD = "moveBackward";
    public static final String OPT_TURN_RIGHT = "turnRight";
    public static final String OPT_TURN_LEFT = "turnLeft";
    public static final String OPT_ADD = "add";
    public static final String OPT_SUB = "sub";
    public static final String OPT_REMOVE = "remove";
    public static final String OPT_EXTRACT = "extract";
    public static final String OPT_FIRST = "first";
    public static final String OPT_LAST = "last";
    public static final String OPT_EQ = "eq";
    public static final String OPT_NEQ = "neq";
    public static final String OPT_SUBSET = "subset";
    public static final String OPT_PROPER_SUBSET = "properSubset";
    public static final String OPT_SUPERSET = "superset";
    public static final String OPT_PROPER_SUPERSET = "properSuperset";

    // 定義済み関数式のリスト
    public static final HashSet<String> PREDEF_FUNC_CALL_EXP_LIST =
        new HashSet<>(Arrays.asList(
            STR_TO_NUM_EXP,
            ANY_TO_STR_EXP,
            SCAM_EXP,
            NUM_ROUND_EXP,
            RAMDOM_INT_EXP,
            ABS_EXP,
            MAX_MIN_EXP,
            MEASURE_DISTANCE_EXP,
            MELODY_EXP,
            ANY_COMP_EXP,
            BINARY_COLOR_EXP,
            DETECT_COLOR_EXP,
            GET_TIME_SINCE_PROGRAM_STARTED_EXP,
            STR_CHAIN_LINK_EXP,
            STR_CHAIN_EXP,
            GET_SYNC_TIMER_COUNT_EXP,
            ANY_ARRAY_TO_STR_EXP,
            OUT_ARG_TEST_EXP,

            Array.NUM_ARRAY_GET_EXP,
            Array.STR_ARRAY_GET_EXP,
            Array.BOOL_ARRAY_GET_EXP,
            Array.COLOR_ARRAY_GET_EXP,
            Array.SOUND_ARRAY_GET_EXP,
            Array.ANY_ARRAY_LEN_EXP,
            Array.ANY_ARRAY_INDEX_OF_EXP,
            Array.ANY_ARRAY_INCLUDES_EXP,
            Array.ANY_ARRAY_COMP_EXP,
            Array.ANY_SET_COMP_EXP));

    // 定義済み関数文のリスト
    public static final HashSet<String> PREDEF_FUNC_CALL_STAT_LIST =
        new HashSet<>(Arrays.asList(
            PRINT_STAT,
            MOVE_STAT,
            STOP_RASPI_CAR_STAT,
            SLEEP_STAT,
            PLAY_MELODY_STAT,
            PLAY_SOUND_LIST_STAT,
            SAY_STAT,
            LIGHT_EYE_STAT,
            SYNC_TIMER_AWAIT_STAT,
            SYNC_TIMER_AWAIT_WITH_TIMEOUT_STAT,
            SYNC_TIMER_COUNTDOWN_AND_AWAIT_STAT,
            SYNC_TIMER_COUNTDOWN_AND_AWAIT_WITH_TIMEOUT_STAT,
            RESET_SYNC_TIMER_STAT,
            SYNC_TIMER_COUNTDOWN_STAT,

            Array.ANY_ARRAY_PUSH_STAT,
            Array.ANY_ARRAY_APPEND_STAT,
            Array.ANY_ARRAY_INSERT_STAT,
            Array.ANY_ARRAY_SPLICE_STAT,
            Array.ANY_ARRAY_SET_STAT,
            Array.ANY_ARRAY_REVERSE_STAT));

    //  (関数呼び出しノード名, 関数呼び出しオプション...) -> 関数名
    public static final Map<FuncId, String> PREDEF_FUNC_NAME_MAP =
        new HashMap<FuncId, String>() {{
            put(FuncId.create(STR_TO_NUM_EXP), ScriptIdentifiers.Funcs.STR_TO_NUM);
            put(FuncId.create(ANY_TO_STR_EXP), ScriptIdentifiers.Funcs.TO_STR);
            put(FuncId.create(PRINT_STAT), ScriptIdentifiers.Funcs.PRINTLN);
            put(FuncId.create(SYNC_TIMER_AWAIT_STAT), ScriptIdentifiers.Funcs.SYNC_TIMER_AWAIT);
            put(FuncId.create(SYNC_TIMER_AWAIT_WITH_TIMEOUT_STAT),
                ScriptIdentifiers.Funcs.SYNC_TIMER_AWAIT);
            put(FuncId.create(SYNC_TIMER_COUNTDOWN_AND_AWAIT_STAT),
                ScriptIdentifiers.Funcs.SYNC_TIMER_COUNTDOWN_AND_AWAIT);
            put(FuncId.create(SYNC_TIMER_COUNTDOWN_AND_AWAIT_WITH_TIMEOUT_STAT),
                ScriptIdentifiers.Funcs.SYNC_TIMER_COUNTDOWN_AND_AWAIT);
            put(FuncId.create(RESET_SYNC_TIMER_STAT), ScriptIdentifiers.Funcs.RESET_SYNC_TIMER);
            put(FuncId.create(SYNC_TIMER_COUNTDOWN_STAT),
                ScriptIdentifiers.Funcs.SYNC_TIMER_COUNTDOWN);
            put(FuncId.create(SCAM_EXP), ScriptIdentifiers.Funcs.SCAN);
            put(FuncId.create(NUM_ROUND_EXP, OPT_ROUND), "Math.round");
            put(FuncId.create(NUM_ROUND_EXP, OPT_CEIL), "Math.ceil");
            put(FuncId.create(NUM_ROUND_EXP, OPT_FLOOR), "Math.floor");
            put(FuncId.create(NUM_ROUND_EXP, OPT_TRUNC), "Math.trunc");
            put(FuncId.create(ABS_EXP), "Math.abs");
            put(FuncId.create(MAX_MIN_EXP, OPT_MAX), "Math.max");
            put(FuncId.create(MAX_MIN_EXP, OPT_MIN), "Math.min");
            put(FuncId.create(RAMDOM_INT_EXP), ScriptIdentifiers.Funcs.RANDOM_INT);
            put(FuncId.create(MEASURE_DISTANCE_EXP), ScriptIdentifiers.Funcs.MEASURE_DISTANCE);
            put(FuncId.create(MELODY_EXP), ScriptIdentifiers.Funcs.PUSH_SOUND);
            put(FuncId.create(ANY_COMP_EXP, OPT_EQ), ScriptIdentifiers.Funcs.ANY_EQ);
            put(FuncId.create(ANY_COMP_EXP, OPT_NEQ), ScriptIdentifiers.Funcs.ANY_NEQ);
            put(FuncId.create(BINARY_COLOR_EXP, OPT_ADD), ScriptIdentifiers.Funcs.ADD_COLOR);
            put(FuncId.create(BINARY_COLOR_EXP, OPT_SUB), ScriptIdentifiers.Funcs.SUB_COLOR);
            put(FuncId.create(DETECT_COLOR_EXP), ScriptIdentifiers.Funcs.DETECT_COLOR);
            put(FuncId.create(GET_TIME_SINCE_PROGRAM_STARTED_EXP),
                ScriptIdentifiers.Funcs.GET_TIME_SINCE_PROGRAM_STARTED);
            put(FuncId.create(STR_CHAIN_LINK_EXP), ScriptIdentifiers.Funcs.STRCAT);
            put(FuncId.create(STR_CHAIN_EXP), ScriptIdentifiers.Funcs.IDENTITY);
            put(FuncId.create(GET_SYNC_TIMER_COUNT_EXP),
                ScriptIdentifiers.Funcs.GET_SYNC_TIMER_COUNT);
            put(FuncId.create(ANY_ARRAY_TO_STR_EXP), ScriptIdentifiers.Funcs.ARY_TO_STR);
            put(FuncId.create(OUT_ARG_TEST_EXP), ScriptIdentifiers.Funcs.OUT_ARG_TEST);
            put(FuncId.create(MOVE_STAT, OPT_MOVE_FORWARD), ScriptIdentifiers.Funcs.MOVE_FORWARD);
            put(FuncId.create(MOVE_STAT, OPT_MOVE_BACKWARD), ScriptIdentifiers.Funcs.MOVE_BACKWARD);
            put(FuncId.create(MOVE_STAT, OPT_TURN_RIGHT), ScriptIdentifiers.Funcs.TURN_RIGHT);
            put(FuncId.create(MOVE_STAT, OPT_TURN_LEFT), ScriptIdentifiers.Funcs.TURN_LEFT);
            put(FuncId.create(STOP_RASPI_CAR_STAT), ScriptIdentifiers.Funcs.STOP_RASPI_CAR);
            put(FuncId.create(SLEEP_STAT), ScriptIdentifiers.Funcs.SLEEP);
            put(FuncId.create(PLAY_MELODY_STAT), ScriptIdentifiers.Funcs.PLAY_MELODIES);
            put(FuncId.create(PLAY_SOUND_LIST_STAT), ScriptIdentifiers.Funcs.PLAY_MELODIES);
            put(FuncId.create(SAY_STAT), ScriptIdentifiers.Funcs.SAY);
            put(FuncId.create(LIGHT_EYE_STAT), ScriptIdentifiers.Funcs.LIGHT_EYE);
            put(FuncId.create(GlobalData.CRITICAL_SECTION_DECL),
                ScriptIdentifiers.Funcs.GEN_LOCK_OBJ);
            put(FuncId.create(GlobalData.SYNC_TIMER_DECL), ScriptIdentifiers.Funcs.GEN_SYNC_TIMER);

            put(FuncId.create(Array.ANY_ARRAY_PUSH_STAT), ScriptIdentifiers.Funcs.ARY_PUSH);
            put(FuncId.create(Array.ANY_ARRAY_LEN_EXP), ScriptIdentifiers.Funcs.ARY_LEN);
            put(FuncId.create(Array.ANY_ARRAY_INSERT_STAT), ScriptIdentifiers.Funcs.ARY_INSERT);
            put(FuncId.create(Array.ANY_ARRAY_APPEND_STAT), ScriptIdentifiers.Funcs.ARY_ADD_ALL);
            put(FuncId.create(Array.ANY_ARRAY_SPLICE_STAT, OPT_REMOVE),
                ScriptIdentifiers.Funcs.ARY_REMOVE);
            put(FuncId.create(Array.ANY_ARRAY_SPLICE_STAT, OPT_EXTRACT),
                ScriptIdentifiers.Funcs.ARY_EXTRACT);
            put(FuncId.create(Array.ANY_ARRAY_SET_STAT), ScriptIdentifiers.Funcs.ARY_SET);
            put(FuncId.create(Array.ANY_ARRAY_REVERSE_STAT), ScriptIdentifiers.Funcs.ARY_REVERSE);
            put(FuncId.create(Array.ANY_ARRAY_INDEX_OF_EXP, OPT_FIRST),
                ScriptIdentifiers.Funcs.ARY_FIRST_INDEX_OF);
            put(FuncId.create(Array.ANY_ARRAY_INDEX_OF_EXP, OPT_LAST),
                ScriptIdentifiers.Funcs.ARY_LAST_INDEX_OF);
            put(FuncId.create(Array.ANY_ARRAY_INCLUDES_EXP), ScriptIdentifiers.Funcs.ARY_INCLUDES);
            put(FuncId.create(Array.ANY_ARRAY_COMP_EXP, OPT_EQ), ScriptIdentifiers.Funcs.ARY_EQ);
            put(FuncId.create(Array.ANY_ARRAY_COMP_EXP, OPT_NEQ), ScriptIdentifiers.Funcs.ARY_NEQ);

            put(FuncId.create(Array.ANY_SET_COMP_EXP, OPT_SUBSET),
                ScriptIdentifiers.Funcs.IS_SUBSET);
            put(FuncId.create(Array.ANY_SET_COMP_EXP, OPT_PROPER_SUBSET),
                ScriptIdentifiers.Funcs.IS_PROPER_SUBSET);
            put(FuncId.create(Array.ANY_SET_COMP_EXP, OPT_SUPERSET),
                ScriptIdentifiers.Funcs.IS_SUPERSET);
            put(FuncId.create(Array.ANY_SET_COMP_EXP, OPT_PROPER_SUPERSET),
                ScriptIdentifiers.Funcs.IS_PROPER_SUPERSET);
            put(FuncId.create(Array.ANY_SET_COMP_EXP, OPT_EQ), ScriptIdentifiers.Funcs.SET_EQ);
            put(FuncId.create(Array.ANY_SET_COMP_EXP, OPT_NEQ), ScriptIdentifiers.Funcs.SET_NEQ);

            put(FuncId.create(Array.STR_ARRAY_GET_EXP), ScriptIdentifiers.Funcs.ARY_GET);
            put(FuncId.create(Array.NUM_ARRAY_GET_EXP), ScriptIdentifiers.Funcs.ARY_GET);
            put(FuncId.create(Array.BOOL_ARRAY_GET_EXP), ScriptIdentifiers.Funcs.ARY_GET);
            put(FuncId.create(Array.COLOR_ARRAY_GET_EXP), ScriptIdentifiers.Funcs.ARY_GET);
            put(FuncId.create(Array.SOUND_ARRAY_GET_EXP), ScriptIdentifiers.Funcs.ARY_GET);
          }};
  }

  /** ユーザ定義関数に関するシンボル名. */
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
            VOID_FUNC_DEF));  // 関数定義ノードのリスト

    public static final HashSet<String> USER_DEF_FUNC_CALL_STAT_LIST =
        new HashSet<>(Arrays.asList(
            VOID_FUNC_CALL));  // ユーザ定義関数文のリスト
  }

  /** イベントを発生させる処理に関するシンボル名. */
  public static class Event {

    public static final String KEY_PRESS_EVENT = "KeyPressedEvent";
    public static final String DELAYED_START_EVENT = "DelayedStartEvent";
    public static final String KEY_CODE = "KeyCode";
    public static final String DELAY_TIME = "DelayTime";

    /** イベントノードのリスト. */
    public static final HashSet<String> LIST =
        new HashSet<>(Arrays.asList(
            KEY_PRESS_EVENT,
            DELAYED_START_EVENT));
  }

  /** リテラルに関するシンボル名. */
  public static class Literal {
    public static final String STR_LITERAL = "StrLiteral";
    public static final String NEW_LINE = "NewLine";
    public static final String NUM_LITERAL = "NumLiteral";
    public static final String BOOL_LITERAL = "BoolLiteral";
    public static final String FREQ_SOUND_LITERAL = "FreqSoundLiteral";
    public static final String SCALE_SOUND_LITERAL = "ScaleSoundLiteral";
    public static final String COLOR_LITERAL = "ColorLiteral";
    public static final String SOUND_LITERAL_VOID = "SoundLiteralVoid";
    public static final String MELODY_EXP_VOID = "MelodyExpVoid";
    public static final String STR_CHAIN_LINK_VOID = "StrChainLinkVoid";
    public static final String STR_EMPTY_LIST = VarDecl.STR_EMPTY_LIST;
    public static final String NUM_EMPTY_LIST = VarDecl.NUM_EMPTY_LIST;
    public static final String BOOL_EMPTY_LIST = VarDecl.BOOL_EMPTY_LIST;
    public static final String COLOR_EMPTY_LIST = VarDecl.COLOR_EMPTY_LIST;
    public static final String SOUND_EMPTY_LIST = VarDecl.SOUND_EMPTY_LIST;
    public static final String ANY_ENPTY_LIST = "AnyEmptyList";

    public static final String STR_LITERAL_EXP = "StrLiteralExp";
    public static final String NUM_LITERAL_EXP = "NumLiteralExp";
    public static final String BOOL_LITERAL_EXP = "BoolLiteralExp";
    public static final String COLOR_LITERAL_EXP = "ColorLiteralExp";
    
    public static final HashSet<String> LIST =
        new HashSet<>(Arrays.asList(
            STR_LITERAL,
            NUM_LITERAL,
            BOOL_LITERAL,
            FREQ_SOUND_LITERAL,
            SOUND_LITERAL_VOID,
            SCALE_SOUND_LITERAL,
            COLOR_LITERAL,
            STR_EMPTY_LIST,
            NUM_EMPTY_LIST,
            BOOL_EMPTY_LIST,
            COLOR_EMPTY_LIST,
            SOUND_EMPTY_LIST,
            ANY_ENPTY_LIST,
            MELODY_EXP_VOID,
            STR_CHAIN_LINK_VOID));

    public static final HashSet<String> LITERAL_EXP_LIST =
        new HashSet<>(Arrays.asList(
            STR_LITERAL_EXP,
            NEW_LINE,
            NUM_LITERAL_EXP,
            BOOL_LITERAL_EXP,
            COLOR_LITERAL_EXP));

    public static final HashSet<String> ARRAY_TYPES =
        new HashSet<>(Arrays.asList(
            STR_EMPTY_LIST,
            NUM_EMPTY_LIST,
            BOOL_EMPTY_LIST,
            COLOR_EMPTY_LIST,
            ANY_ENPTY_LIST,
            SOUND_EMPTY_LIST,
            MELODY_EXP_VOID));

    /** 音リテラルに関するシンボル名. */
    public static class Sound {
      public static final String DURATION = "Duration";
      public static final String FREQUENCY = "Frequency";
      public static final String SCALE_SOUND = "ScaleSound";
      public static final String OCTAVE = "Octave";
    }
  }

  /** リストに関するシンボル名. */
  public static class Array {
    public static final String ARRAY = "Array";
    public static final String INDEX = "Index";

    public static final String ANY_ARRAY_PUSH_STAT = "AnyArrayPushStat";
    public static final String ANY_ARRAY_LEN_EXP = "AnyArrayLengthExp";
    public static final String ANY_ARRAY_APPEND_STAT = "AnyArrayAppendStat";
    public static final String ANY_ARRAY_INSERT_STAT = "AnyArrayInsertStat";
    public static final String ANY_ARRAY_SPLICE_STAT = "AnyArraySpliceStat";
    public static final String ANY_ARRAY_SET_STAT = "AnyArraySetStat";
    public static final String ANY_ARRAY_REVERSE_STAT = "AnyArrayReverseStat";
    public static final String ANY_ARRAY_INDEX_OF_EXP = "AnyArrayIndexOfExp";
    public static final String ANY_ARRAY_INCLUDES_EXP = "AnyArrayIncludesExp";
    public static final String ANY_ARRAY_COMP_EXP = "AnyArrayCompExp";
    public static final String ANY_SET_COMP_EXP = "AnySetCompExp";

    public static final String STR_ARRAY_GET_EXP = "StrArrayGetExp";
    public static final String NUM_ARRAY_GET_EXP = "NumArrayGetExp";
    public static final String BOOL_ARRAY_GET_EXP = "BoolArrayGetExp";
    public static final String COLOR_ARRAY_GET_EXP = "ColorArrayGetExp";
    public static final String SOUND_ARRAY_GET_EXP = "SoundArrayGetExp";
  }

  /** グローバルデータに関するシンボル名. */
  public static class GlobalData {

    public static final String CRITICAL_SECTION_DECL = "CriticalSectionDecl";
    public static final String SYNC_TIMER_DECL = "SyncTimerDecl";
    public static final String SYNC_TIMER_VAR = "SyncTimerVar";

    public static final String CRITICAL_SECTION_NAME = "CriticalSectionName";
    public static final String SYNC_TIMER_NAME = "SyncTimerName";
    public static final String NEXT_GLOBAL_DATA_DECL = "NextGlobalDataDecl";

    public static final HashSet<String> LIST =
        new HashSet<>(Arrays.asList(
            CRITICAL_SECTION_DECL,
            SYNC_TIMER_DECL,
            NEXT_GLOBAL_DATA_DECL));

    public static final HashSet<String> VAR_LIST =
        new HashSet<>(Arrays.asList(
            SYNC_TIMER_VAR));

    public static final HashSet<String> DATA_NAME_CNCTR_LIST =
        new HashSet<>(Arrays.asList(
            CRITICAL_SECTION_NAME,
            SYNC_TIMER_NAME));
  }

  /** BhProgram に定義された定数に関するシンボル名. */
  public static class ConstantValue {
    public static final String NIL_COLOR = "NilColor";
    public static final String NIL_SOUND = "NilSound";
    public static final String ANY_EXP_VOID = "AnyExpVoid";
    public static final String LINE_FEED = "LineFeed";
    public static final String SYNC_TIMER_VAR_VOID = "SyncTimerVarVoid";
    public static final HashSet<String> LIST =
        new HashSet<>(Arrays.asList(
            NIL_COLOR,
            NIL_SOUND,
            ANY_EXP_VOID,
            LINE_FEED,
            SYNC_TIMER_VAR_VOID));
  }
}
