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

import static java.util.Map.entry;
import static net.seapanda.bunnyhop.compiler.ScriptIdentifiers.Funcs;
import static net.seapanda.bunnyhop.compiler.ScriptIdentifiers.Vars;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * シンボル名を定義するクラス.
 *
 * @author K.Koike
 */
class SymbolNames {

  final GlobalDataDecl globalDataDecl = new GlobalDataDecl();
  private final ListCtrl listCtrl = new ListCtrl();
  final PredefFunc predefFunc = new PredefFunc(globalDataDecl, listCtrl);
  final VarDecl varDecl = new VarDecl();
  final Statement stat = new Statement(predefFunc, listCtrl);
  final Expression exp = new Expression(predefFunc, listCtrl);
  final UserDefFunc userDefFunc = new UserDefFunc();
  final Event event = new Event();


  /** 変数宣言に関するシンボル名. */
  static class VarDecl {

    final String numVarDecl = "NumVarDecl";
    final String numListDecl = "NumListDecl";

    final String strVarDecl = "StrVarDecl";
    final String strListDecl = "StrListDecl";

    final String boolVarDecl = "BoolVarDecl";
    final String boolListDecl = "BoolListDecl";

    final String colorVarDecl = "ColorVarDecl";
    final String colorListDecl = "ColorListDecl";

    final String soundVarDecl = "SoundVarDecl";
    final String soundListDecl = "SoundListDecl";

    final String varName = "VarName";
    final String listName = "ListName";
    final String nextVarDecl = "NextVarDecl";

    final Set<String> list =
        new HashSet<>(java.util.List.of(
            numVarDecl,
            numListDecl,
            strVarDecl,
            strListDecl,
            boolVarDecl,
            boolListDecl,
            colorVarDecl,
            colorListDecl,
            soundVarDecl,
            soundListDecl));

    final Map<String, String> initValMap =
        new HashMap<>(Map.ofEntries(
            entry(numVarDecl, "0"),
            entry(strVarDecl, "''"),
            entry(boolVarDecl, "false"),
            entry(colorVarDecl, Vars.NIL_COLOR),
            entry(soundVarDecl, Vars.NIL_SOUND),
            entry(numListDecl, "[]"),
            entry(strListDecl, "[]"),
            entry(boolListDecl, "[]"),
            entry(colorListDecl, "[]"),
            entry(soundListDecl, "[]")
        ));

    /** 変数名を保持するノードが接続されるコネクタのリスト. */
    final Set<String> varNameCnctrList =
        new HashSet<>(java.util.List.of(
            varName,
            listName));
  }

  /** 文に関するシンボル名. */
  static class Statement {

    final Ignored ignored = new Ignored();
    final AssignStat assign = new AssignStat();
    final ControlStat ctrl = new ControlStat();
    final PredefFunc predefFunc;

    final String statVoid = "StatVoid";
    final String statList = "StatList";
    final String nextStat = "NextStat";

    private Statement(SymbolNames.PredefFunc builtInFunc, ListCtrl listCtrl) {
      predefFunc = new PredefFunc(builtInFunc, listCtrl);
    }

    /** コンパイルの対象にならない文. */
    static class Ignored {
      final String commentPart = "CommentPart";

      final Set<String> list =
          new HashSet<>(java.util.List.of(
              commentPart));
    }

    /** 代入文に関するシンボル名. */
    static class AssignStat {

      final String numAddAssignStat = "NumAddAssignStat";
      final String strAddAssignStat = "StrAddAssignStat";
      final String anyAssignStat = "AnyAssignStat";
      final String anyListAssignStat = "AnyListAssignStat";
      final String leftVar = "LeftVar";

      final Set<String> list =
          new HashSet<>(java.util.List.of(
              anyAssignStat,
              anyListAssignStat,
              numAddAssignStat,
              strAddAssignStat));
    }

    /** 制御文に関するシンボル名. */
    static class ControlStat {

      final String localVarDecl = "LocalVarDecl";
      final String ifStat = "IfStat";
      final String ifElseStat = "IfElseStat";
      final String whileStat = "WhileStat";
      final String repeatStat = "RepeatStat";
      final String thenStat = "ThenStat";
      final String elseStat = "ElseStat";
      final String condExp = "CondExp";
      final String loopStat = "LoopStat";
      final String compoundStat = "CompoundStat";
      final String continueStat = "ContinueStat";
      final String breakStat = "BreakStat";
      final String returnStat = "ReturnStat";
      final String mutexBlockStat = "MutexBlockStat";
      final String exclusiveStat = "ExclusiveStat";
      final String expAdapterStat = "ExpAdapterStat";
      final String listAdapterStat = "ListAdapterStat";
      final String target = "Target";

      final Set<String> list =
          new HashSet<>(java.util.List.of(
              ifStat,
              ifElseStat,
              whileStat,
              repeatStat,
              compoundStat,
              continueStat,
              breakStat,
              returnStat,
              mutexBlockStat,
              expAdapterStat,
              listAdapterStat));
    }

    /** 定義済み関数に関するシンボル名. */
    static class PredefFunc {

      /** 定義済み関数文のリスト. */
      final Set<String> list;

      private PredefFunc(SymbolNames.PredefFunc predef, ListCtrl listCtrl) {
        list = new HashSet<>(java.util.List.of(
              predef.printStat,
              predef.moveStat,
              predef.stopRaspiCarStat,
              predef.sleepStat,
              predef.playMelodyStat,
              predef.playSoundListStat,
              predef.sayStat,
              predef.lightEyeStat,
              predef.syncTimerAwaitStat,
              predef.resetSyncTimerStat,
              predef.syncTimerCountdownStat,
              predef.semaphoreAcquireStat,
              predef.semaphoreReleaseStat,
              predef.saveTextStat,
              predef.deleteTextFileStat,
              predef.deleteTextFilesStat,
              predef.setAudioVolumeStat,
              predef.recordAudioStat,
              predef.playAudioStat,
              predef.deleteAudioFileStat,
              predef.deleteAudioFilesStat,

              listCtrl.anyListPushStat,
              listCtrl.anyListInsertAllStat,
              listCtrl.anyListInsertStat,
              listCtrl.anyListSpliceStat,
              listCtrl.anyListSetStat));
      }
    }
  }

  /** 式に関するシンボル名. */
  static class Expression {

    final BinaryExp binary = new BinaryExp();
    final UnaryExp unary = new UnaryExp();
    final Literal literal = new Literal();
    final Variable variable = new Variable();
    final GlobalDataVar globalDataVar = new GlobalDataVar();
    final ConstantValue constant = new ConstantValue();
    final PredefFunc predefFunc;

    private Expression(SymbolNames.PredefFunc predefFunc, ListCtrl listCtrl) {
      this.predefFunc = new PredefFunc(predefFunc, listCtrl);
    }

    /** 二項演算式に関するシンボル名. */
    static class BinaryExp {

      final String fourArithExp = "FourArithExp";
      final String binaryBoolExp = "BinaryBoolExp";
      final String modExp = "ModExp";
      final String numCompExp = "NumCompExp";
      final String strCompExp = "StrCompExp";
      final String leftExp = "LeftExp";
      final String rightExp = "RightExp";
      final String operator = "Operator";

      final String opAdd = "add";
      final String opSub = "sub";
      final String opDiv = "div";
      final String opMul = "mul";
      final String opMod = "mod";
      final String opAnd = "and";
      final String opOr = "or";
      final String opEq = "eq";
      final String opNeq = "neq";
      final String opLt = "lt";
      final String opLte = "lte";
      final String opGt = "gt";
      final String opGte = "gte";

      /** 二項演算のリスト. */
      final Set<String> list =
          new HashSet<>(java.util.List.of(
              fourArithExp,
              modExp,
              binaryBoolExp,
              numCompExp,
              strCompExp));

      /** 論理二項演算のリスト. */
      final Set<String> logicalList =
          new HashSet<>(java.util.List.of(binaryBoolExp));

      /** 非論理二項演算のリスト. */
      final Set<String> nonlogicalList =
          new HashSet<>(java.util.List.of(
              fourArithExp,
              modExp,
              numCompExp,
              strCompExp));

      final Map<String, String> operatorMap =
          new HashMap<>(Map.ofEntries(
              entry(opAdd, " + "),
              entry(opSub, " - "),
              entry(opDiv, " / "),
              entry(opMul, " * "),
              entry(opMod, " % "),
              entry(opAnd, " && "),
              entry(opOr,  " || "),
              entry(opEq,  " === "),
              entry(opNeq, " !== "),
              entry(opLt,  " < "),
              entry(opLte, " <= "),
              entry(opGt,  " > "),
              entry(opGte, " >= ")
          ));
    }

    /** 単項演算式に関するシンボル名. */
    static class UnaryExp {

      final String notExp = "NotExp";
      final String negExp = "NegExp";
      final String primaryExp = "PrimaryExp";
      final Set<String> list =
          new HashSet<>(java.util.List.of(
              notExp,
              negExp));

      final Map<String, String> operatorMap =
          new HashMap<>(Map.ofEntries(
              entry(notExp, "!"),
              entry(negExp, "-")
          ));
    }

    /** リテラルに関するシンボル名. */
    static class Literal {

      final String strLiteral = "StrLiteral";
      final String newLine = "NewLine";
      final String numLiteral = "NumLiteral";
      final String boolLiteral = "BoolLiteral";
      final String freqSoundLiteral = "FreqSoundLiteral";
      final String scaleSoundLiteral = "ScaleSoundLiteral";
      final String colorLiteral = "ColorLiteral";

      final String strLiteralExp = "StrLiteralExp";
      final String numLiteralExp = "NumLiteralExp";
      final String boolLiteralExp = "BoolLiteralExp";
      final String colorLiteralExp = "ColorLiteralExp";
      final String freqSoundLiteralExp = "FreqSoundLiteralExp";
      final String scaleSoundLiteralExp = "ScaleSoundLiteralExp";

      final Sound sound = new Sound();

      final Set<String> values =
          new HashSet<>(java.util.List.of(
              strLiteral,
              numLiteral,
              boolLiteral,
              freqSoundLiteral,
              scaleSoundLiteral,
              colorLiteral));

      final Set<String> list =
          new HashSet<>(java.util.List.of(
              strLiteralExp,
              newLine,
              numLiteralExp,
              boolLiteralExp,
              colorLiteralExp,
              freqSoundLiteralExp,
              scaleSoundLiteralExp));

      /** 音リテラルに関するシンボル名. */
      static class Sound {
        final String volume = "Volume";
        final String duration = "Duration";
        final String frequency = "Frequency";
        final String scaleSound = "ScaleSound";
        final String octave = "Octave";
      }
    }

    /** 変数に関するシンボル名. */
    static class Variable {

      final String numVar = "NumVar";
      final String numVarVoid = "NumVarVoid";
      final String numList = "NumList";
      final String numListVoid = "NumListVoid";

      final String strVar = "StrVar";
      final String strVarVoid = "StrVarVoid";
      final String strList = "StrList";
      final String strListVoid = "StrListVoid";

      final String boolVar = "BoolVar";
      final String boolVarVoid = "BoolVarVoid";
      final String boolList = "BoolList";
      final String boolListVoid = "BoolListVoid";

      final String colorVar = "ColorVar";
      final String colorVarVoid = "ColorVarVoid";
      final String colorList = "ColorList";
      final String colorListVoid = "ColorListVoid";

      final String soundVar = "SoundVar";
      final String soundVarVoid = "SoundVarVoid";
      final String soundList = "SoundList";
      final String soundListVoid = "SoundListVoid";

      final Set<String> list =
          new HashSet<>(java.util.List.of(
              numVar,
              numList,
              strVar,
              strList,
              boolVar,
              boolList,
              colorVar,
              colorList,
              soundVar,
              soundList));

      final Set<String> voidList =
          new HashSet<>(java.util.List.of(
              numVarVoid,
              strVarVoid,
              boolVarVoid,
              colorVarVoid,
              soundVarVoid,
              numListVoid,
              strListVoid,
              boolListVoid,
              colorListVoid,
              soundListVoid));

      final Map<String, String> initValMap =
          new HashMap<>(Map.ofEntries(
              entry(numVarVoid, Vars.NIL),
              entry(strVarVoid, Vars.NIL),
              entry(boolVarVoid, Vars.NIL),
              entry(colorVarVoid, Vars.NIL),
              entry(soundVarVoid, Vars.NIL),
              entry(numListVoid, "[]"),
              entry(strListVoid, "[]"),
              entry(boolListVoid, "[]"),
              entry(colorListVoid, "[]"),
              entry(soundListVoid, "[]")
          ));
    }

    /** グローバルデータを格納する変数に関するシンボル名. */
    static class GlobalDataVar {

      final String syncTimerVar = "SyncTimerVar";
      final String semaphoreVar = "SemaphoreVar";

      final Set<String> list =
          new HashSet<>(java.util.List.of(
              syncTimerVar,
              semaphoreVar));
    }

    /** 定数もしくは常に同じ値に評価される式のシンボル名. */
    static class ConstantValue {
      final String anyExpVoid = "AnyExpVoid";
      final String numExpVoid = "NumExpVoid";
      final String strExpVoid = "StrExpVoid";
      final String boolExpVoid = "BoolExpVoid";
      final String colorExpVoid = "ColorExpVoid";
      final String soundExpVoid = "SoundExpVoid";
      final String melodyExpVoid = "MelodyExpVoid";
      final String syncTimerVarVoid = "SyncTimerVarVoid";
      final String semaphoreVarVoid = "SemaphoreVarVoid";
      final String strChainLinkVoid = "StrChainLinkVoid";
      final String lineFeed = "LineFeed";
      final String anyListExpVoid = "AnyListExpVoid";
      final String numListExpVoid = "NumListExpVoid";
      final String strListExpVoid = "StrListExpVoid";
      final String boolListExpVoid = "BoolListExpVoid";
      final String colorListExpVoid = "ColorListExpVoid";
      final String soundListExpVoid = "SoundListExpVoid";

      final Set<String> list =
          new HashSet<>(java.util.List.of(
              anyExpVoid,
              numExpVoid,
              strExpVoid,
              boolExpVoid,
              colorExpVoid,
              soundExpVoid,
              melodyExpVoid,
              syncTimerVarVoid,
              semaphoreVarVoid,
              strChainLinkVoid,
              lineFeed,
              anyListExpVoid,
              numListExpVoid,
              strListExpVoid,
              boolListExpVoid,
              colorListExpVoid,
              soundListExpVoid));

      final Map<String, String> valueMap =
          new HashMap<>(Map.ofEntries(
              entry(anyExpVoid, Vars.NIL),
              entry(numExpVoid, Vars.NIL),
              entry(strExpVoid, Vars.NIL),
              entry(boolExpVoid, Vars.NIL),
              entry(colorExpVoid, Vars.NIL),
              entry(soundExpVoid, Vars.NIL),
              entry(melodyExpVoid, "[]"),
              entry(syncTimerVarVoid, Vars.NIL),
              entry(semaphoreVarVoid, Vars.NIL),
              entry(strChainLinkVoid, "''"),
              entry(lineFeed, "'\\n'"),
              entry(anyListExpVoid, "[]"),
              entry(numListExpVoid, "[]"),
              entry(strListExpVoid, "[]"),
              entry(boolListExpVoid, "[]"),
              entry(colorListExpVoid, "[]"),
              entry(soundListExpVoid, "[]")
          ));
    }

    /** 定義済み関数に関するシンボル名. */
    static class PredefFunc {

      SymbolNames.PredefFunc predefFunc;

      /** 定義済み関数式のリスト. */
      final Set<String> list;

      PredefFunc(SymbolNames.PredefFunc predefFunc, ListCtrl listCtrl) {
        this.predefFunc = predefFunc;

        list = new HashSet<>(java.util.List.of(
            predefFunc.strToNumExp,
            predefFunc.strToNumWithDefaultValExp,
            predefFunc.anyToStrExp,
            predefFunc.scamExp,
            predefFunc.numRoundExp,
            predefFunc.randomIntExp,
            predefFunc.absExp,
            predefFunc.maxMinExp,
            predefFunc.measureDistanceExp,
            predefFunc.melodyExp,
            predefFunc.anyCompExp,
            predefFunc.binaryColorExp,
            predefFunc.detectColorExp,
            predefFunc.getTimeSinceProgramStartedExp,
            predefFunc.strChainLinkExp,
            predefFunc.strLineFeedChainLinkExp,
            predefFunc.strChainExp,
            predefFunc.getSyncTimerCountExp,
            predefFunc.syncTimerTimedAwaitExp,
            predefFunc.semaphoreTryAcquireExp,
            predefFunc.getNumSemaphorePermitsExp,
            predefFunc.anyListToStrExp,
            predefFunc.checkNumTypeExp,
            predefFunc.numPowExp,
            predefFunc.numClampExp,
            predefFunc.outArgTestExp,
            predefFunc.loadTextExp,
            predefFunc.getTextFilesExp,
            predefFunc.getAudioVolumeExp,
            predefFunc.getAudioFilesExp,
            predefFunc.measureSoundPressureExp,

            listCtrl.numListGetExp,
            listCtrl.numListSliceExp,
            listCtrl.numListReverseExp,
            listCtrl.numListSortExp,
            listCtrl.numListMaxMinExp,
            listCtrl.strListGetExp,
            listCtrl.strListSliceExp,
            listCtrl.strListReverseExp,
            listCtrl.strListSortExp,
            listCtrl.strListMaxMinExp,
            listCtrl.boolListGetExp,
            listCtrl.boolListSliceExp,
            listCtrl.boolListReverseExp,
            listCtrl.colorListGetExp,
            listCtrl.colorListSliceExp,
            listCtrl.colorListReverseExp,
            listCtrl.soundListGetExp,
            listCtrl.soundListSliceExp,
            listCtrl.soundListReverseExp,
            listCtrl.anyListLenExp,
            listCtrl.anyListIndexOfExp,
            listCtrl.anyListIncludesExp,
            listCtrl.anyListCompExp,
            listCtrl.anySetCompExp));
      }

      /** 定義済み関数の ID から名前を取得する. */
      String getFuncName(FuncId id) {
        return predefFunc.nameMap.get(id);
      }
    }
  }

  /** BhProgram にあらかじめ定義されたメソッドを呼び出す処理に関するシンボル名. */
  static class PredefFunc {

    final String arg = "Arg";
    final String outArg = "OutArg";
    final String option = "Option";

    //ノード名
    private final String strToNumExp = "StrToNumExp";
    private final String strToNumWithDefaultValExp = "StrToNumWithDefaultValExp";
    private final String anyToStrExp = "AnyToStrExp";
    private final String scamExp = "ScanExp";
    private final String randomIntExp = "RandomIntExp";
    private final String numRoundExp = "NumRoundExp";
    private final String absExp = "AbsExp";
    private final String maxMinExp = "MaxMinExp";
    private final String measureDistanceExp = "MeasureDistanceExp";
    private final String melodyExp = "MelodyExp";
    private final String anyCompExp = "AnyCompExp";
    private final String binaryColorExp = "BinaryColorExp";
    private final String detectColorExp = "DetectColorExp";
    private final String getTimeSinceProgramStartedExp = "GetTimeSinceProgramStartedExp";
    private final String strChainLinkExp = "StrChainLinkExp";
    private final String strLineFeedChainLinkExp = "StrLineFeedChainLinkExp";
    private final String strChainExp = "StrChainExp";
    private final String getSyncTimerCountExp = "GetSyncTimerCountExp";
    private final String syncTimerTimedAwaitExp = "SyncTimerTimedAwaitExp";
    final String anyListToStrExp = "AnyListToStrExp";
    private final String checkNumTypeExp = "CheckNumTypeExp";
    private final String numPowExp = "NumPowExp";
    private final String numClampExp = "NumClampExp";
    private final String outArgTestExp = "OutArgTestExp";
    private final String loadTextExp = "LoadTextExp";
    private final String getTextFilesExp = "GetTextFilesExp";
    private final String getAudioFilesExp = "GetAudioFilesExp";
    private final String getAudioVolumeExp = "GetAudioVolumeExp";
    private final String measureSoundPressureExp = "MeasureSoundPressureExp";
    private final String printStat = "PrintStat";
    private final String moveStat = "MoveStat";
    private final String stopRaspiCarStat = "StopRaspiCarStat";
    private final String sleepStat = "SleepStat";
    private final String playMelodyStat = "PlayMelodyStat";
    private final String playSoundListStat = "PlaySoundListStat";
    private final String sayStat = "SayStat";
    private final String lightEyeStat = "LightEyeStat";
    private final String syncTimerAwaitStat = "SyncTimerAwaitStat";
    private final String resetSyncTimerStat = "ResetSyncTimerStat";
    private final String syncTimerCountdownStat = "SyncTimerCountdownStat";
    private final String semaphoreAcquireStat = "SemaphoreAcquireStat";
    private final String semaphoreTryAcquireExp = "SemaphoreTryAcquireExp";
    private final String semaphoreReleaseStat = "SemaphoreReleaseStat";
    private final String getNumSemaphorePermitsExp = "GetNumSemaphorePermitsExp";
    private final String saveTextStat = "SaveTextStat";
    private final String deleteTextFileStat = "DeleteTextFileStat";
    private final String deleteTextFilesStat = "DeleteTextFilesStat";
    private final String recordAudioStat = "RecordAudioStat";
    private final String playAudioStat = "PlayAudioStat";
    private final String setAudioVolumeStat = "SetAudioVolumeStat";
    private final String deleteAudioFileStat = "DeleteAudioFileStat";
    private final String deleteAudioFilesStat = "DeleteAudioFilesStat";

    /** (関数呼び出しノード名, 関数呼び出しオプション...) -> 関数名. */
    private final Map<FuncId, String> nameMap;

    private PredefFunc(GlobalDataDecl globalDataDecl, ListCtrl listCtrl) {

      //オプション名
      final String optRound = "round";
      final String optCeil = "ceil";
      final String optFloor = "floor";
      final String optTrunc = "trunc";
      final String optMax = "max";
      final String optMin = "min";
      final String optPeak = "peak";
      final String optAverage = "average";
      final String optMoveForward = "moveForward";
      final String optMoveBackward = "moveBackward";
      final String optTurnRight = "turnRight";
      final String optTurnLeft = "turnLeft";
      final String optAdd = "add";
      final String optSub = "sub";
      final String optRemove = "remove";
      final String optExtract = "extract";
      final String optFirst = "first";
      final String optLast = "last";
      final String optEq = "eq";
      final String optNeq = "neq";
      final String optSubset = "subset";
      final String optProperSubset = "properSubset";
      final String optSuperset = "superset";
      final String optProperSuperset = "properSuperset";
      final String optFinite = "finite";
      final String optInfinite = "infinite";
      final String optPosInf = "posInf";
      final String optNegInf = "negInf";
      final String optNan = "nan";
      final String optWithLineBreak = "withLineBreak";
      final String optWithoutLineBreak = "withoutLineBreak";
      final String optWithCountdown = "withCountdown";
      final String optWithoutCountdown = "withoutCountdown";
      final String optInPlace = "inPlace";
      final String optNotInPlace = "notInPlace";

      nameMap = new HashMap<>();
      nameMap.put(FuncId.of(strToNumExp), Funcs.STR_TO_NUM);
      nameMap.put(FuncId.of(strToNumWithDefaultValExp), Funcs.STR_TO_NUM);
      nameMap.put(FuncId.of(anyToStrExp), Funcs.STR);
      nameMap.put(FuncId.of(printStat, optWithLineBreak), Funcs.PRINTLN);
      nameMap.put(FuncId.of(printStat, optWithoutLineBreak), Funcs.PRINT);
      nameMap.put(
          FuncId.of(syncTimerAwaitStat, optWithCountdown), Funcs.SYNC_TIMER_COUNTDOWN_AND_AWAIT);
      nameMap.put(FuncId.of(syncTimerAwaitStat, optWithoutCountdown), Funcs.SYNC_TIMER_AWAIT);
      nameMap.put(FuncId.of(resetSyncTimerStat), Funcs.RESET_SYNC_TIMER);
      nameMap.put(FuncId.of(syncTimerCountdownStat), Funcs.SYNC_TIMER_COUNTDOWN);
      nameMap.put(FuncId.of(semaphoreAcquireStat), Funcs.SEMAPHORE_ACQUIRE);
      nameMap.put(FuncId.of(semaphoreReleaseStat), Funcs.SEMAPHORE_RELEASE);
      nameMap.put(FuncId.of(scamExp), Funcs.SCAN);
      nameMap.put(FuncId.of(numRoundExp, optRound), "Math.round");
      nameMap.put(FuncId.of(numRoundExp, optCeil), "Math.ceil");
      nameMap.put(FuncId.of(numRoundExp, optFloor), "Math.floor");
      nameMap.put(FuncId.of(numRoundExp, optTrunc), "Math.trunc");
      nameMap.put(FuncId.of(absExp), "Math.abs");
      nameMap.put(FuncId.of(maxMinExp, optMax), "Math.max");
      nameMap.put(FuncId.of(maxMinExp, optMin), "Math.min");
      nameMap.put(FuncId.of(randomIntExp), Funcs.RANDOM_INT);
      nameMap.put(FuncId.of(measureDistanceExp), Funcs.MEASURE_DISTANCE);
      nameMap.put(FuncId.of(melodyExp), Funcs.PUSH_SOUND);
      nameMap.put(FuncId.of(anyCompExp, optEq), Funcs.ANY_EQ);
      nameMap.put(FuncId.of(anyCompExp, optNeq), Funcs.ANY_NEQ);
      nameMap.put(FuncId.of(binaryColorExp, optAdd), Funcs.ADD_COLOR);
      nameMap.put(FuncId.of(binaryColorExp, optSub), Funcs.SUB_COLOR);
      nameMap.put(FuncId.of(detectColorExp), Funcs.DETECT_COLOR);
      nameMap.put(FuncId.of(getTimeSinceProgramStartedExp), Funcs.GET_TIMER_VAL);
      nameMap.put(FuncId.of(strChainLinkExp), Funcs.STRCAT);
      nameMap.put(FuncId.of(strLineFeedChainLinkExp), Funcs.STRCAT_LF);
      nameMap.put(FuncId.of(strChainExp), Funcs.IDENTITY);
      nameMap.put(FuncId.of(getSyncTimerCountExp), Funcs.GET_SYNC_TIMER_COUNT);
      nameMap.put(
          FuncId.of(syncTimerTimedAwaitExp, optWithCountdown),
          Funcs.SYNC_TIMER_COUNTDOWN_AND_TIMED_AWAIT);
      nameMap.put(
          FuncId.of(syncTimerTimedAwaitExp, optWithoutCountdown), Funcs.SYNC_TIMER_TIMED_AWAIT);
      nameMap.put(FuncId.of(semaphoreTryAcquireExp), Funcs.SEMAPHORE_TRY_ACQUIRE);
      nameMap.put(FuncId.of(getNumSemaphorePermitsExp), Funcs.GET_NUM_SEMAPHORE_PERMITS);
      nameMap.put(FuncId.of(anyListToStrExp), Funcs.LIST_TO_STR);
      nameMap.put(FuncId.of(checkNumTypeExp, optFinite), "Number.isFinite");
      nameMap.put(FuncId.of(checkNumTypeExp, optInfinite), Funcs.IS_NUM_INFINITE);
      nameMap.put(FuncId.of(checkNumTypeExp, optPosInf), Funcs.IS_NUM_POS_INF);
      nameMap.put(FuncId.of(checkNumTypeExp, optNegInf), Funcs.IS_NUM_NEG_INF);
      nameMap.put(FuncId.of(checkNumTypeExp, optNan), "Number.isNaN");
      nameMap.put(FuncId.of(numPowExp), "Math.pow");
      nameMap.put(FuncId.of(numClampExp), Funcs.NUM_CLAMP);
      nameMap.put(FuncId.of(loadTextExp), Funcs.LOAD_TEXT);
      nameMap.put(FuncId.of(getTextFilesExp), Funcs.GET_TEXT_FILES);
      nameMap.put(FuncId.of(getAudioVolumeExp), Funcs.GET_AUDIO_VOLUME);
      nameMap.put(FuncId.of(getAudioFilesExp), Funcs.GET_AUDIO_FILES);
      nameMap.put(FuncId.of(measureSoundPressureExp, optPeak), Funcs.FIND_SOUND_PRESSURE_PEAK);
      nameMap.put(
          FuncId.of(measureSoundPressureExp, optAverage), Funcs.FIND_SOUND_PRESSURE_AVERAGE);
      nameMap.put(FuncId.of(outArgTestExp), Funcs.OUT_ARG_TEST);
      nameMap.put(FuncId.of(moveStat, optMoveForward), Funcs.MOVE_FORWARD);
      nameMap.put(FuncId.of(moveStat, optMoveBackward), Funcs.MOVE_BACKWARD);
      nameMap.put(FuncId.of(moveStat, optTurnRight), Funcs.TURN_RIGHT);
      nameMap.put(FuncId.of(moveStat, optTurnLeft), Funcs.TURN_LEFT);
      nameMap.put(FuncId.of(stopRaspiCarStat), Funcs.STOP_RASPI_CAR);
      nameMap.put(FuncId.of(sleepStat), Funcs.SLEEP);
      nameMap.put(FuncId.of(playMelodyStat), Funcs.PLAY_MELODIES);
      nameMap.put(FuncId.of(playSoundListStat), Funcs.PLAY_MELODIES);
      nameMap.put(FuncId.of(sayStat), Funcs.SAY);
      nameMap.put(FuncId.of(lightEyeStat), Funcs.LIGHT_EYE);
      nameMap.put(FuncId.of(globalDataDecl.mutexBlockDecl), Funcs.NEW_LOCK_OBJ);
      nameMap.put(FuncId.of(globalDataDecl.syncTimerDecl), Funcs.NEW_SYNC_TIMER);
      nameMap.put(FuncId.of(globalDataDecl.semaphoreDecl), Funcs.NEW_SEMAPHORE);
      nameMap.put(FuncId.of(saveTextStat), Funcs.SAVE_TEXT);
      nameMap.put(FuncId.of(deleteTextFileStat), Funcs.DELETE_TEXT_FILE);
      nameMap.put(FuncId.of(deleteTextFilesStat), Funcs.DELETE_TEXT_FILES);
      nameMap.put(FuncId.of(setAudioVolumeStat), Funcs.SET_AUDIO_VOLUME);
      nameMap.put(FuncId.of(recordAudioStat), Funcs.RECORD_AUDIO);
      nameMap.put(FuncId.of(playAudioStat), Funcs.PLAY_AUDIO);
      nameMap.put(FuncId.of(deleteAudioFileStat), Funcs.DELETE_AUDIO_FILE);
      nameMap.put(FuncId.of(deleteAudioFilesStat), Funcs.DELETE_AUDIO_FILES);

      nameMap.put(FuncId.of(listCtrl.anyListPushStat), Funcs.LIST_PUSH);
      nameMap.put(FuncId.of(listCtrl.anyListLenExp), Funcs.LIST_LEN);
      nameMap.put(FuncId.of(listCtrl.anyListInsertStat), Funcs.LIST_INSERT);
      nameMap.put(FuncId.of(listCtrl.anyListInsertAllStat), Funcs.LIST_INSERT_ALL);
      nameMap.put(FuncId.of(listCtrl.anyListSpliceStat, optRemove), Funcs.LIST_REMOVE);
      nameMap.put(FuncId.of(listCtrl.anyListSpliceStat, optExtract), Funcs.LIST_EXTRACT);
      nameMap.put(FuncId.of(listCtrl.anyListSetStat), Funcs.LIST_SET);
      nameMap.put(FuncId.of(listCtrl.anyListIndexOfExp, optFirst), Funcs.LIST_FIRST_INDEX_OF);
      nameMap.put(FuncId.of(listCtrl.anyListIndexOfExp, optLast), Funcs.LIST_LAST_INDEX_OF);
      nameMap.put(FuncId.of(listCtrl.anyListIncludesExp), Funcs.LIST_INCLUDES);
      nameMap.put(FuncId.of(listCtrl.anyListCompExp, optEq), Funcs.LIST_EQ);
      nameMap.put(FuncId.of(listCtrl.anyListCompExp, optNeq), Funcs.LIST_NEQ);

      nameMap.put(FuncId.of(listCtrl.anySetCompExp, optSubset), Funcs.IS_SUBSET);
      nameMap.put(FuncId.of(listCtrl.anySetCompExp, optProperSubset), Funcs.IS_PROPER_SUBSET);
      nameMap.put(FuncId.of(listCtrl.anySetCompExp, optSuperset), Funcs.IS_SUPERSET);
      nameMap.put(FuncId.of(listCtrl.anySetCompExp, optProperSuperset), Funcs.IS_PROPER_SUPERSET);
      nameMap.put(FuncId.of(listCtrl.anySetCompExp, optEq), Funcs.SET_EQ);
      nameMap.put(FuncId.of(listCtrl.anySetCompExp, optNeq), Funcs.SET_NEQ);

      nameMap.put(FuncId.of(listCtrl.strListGetExp), Funcs.LIST_GET);
      nameMap.put(FuncId.of(listCtrl.strListSliceExp), Funcs.LIST_SLICE);
      nameMap.put(FuncId.of(listCtrl.strListReverseExp, optInPlace), Funcs.LIST_REVERSE);
      nameMap.put(FuncId.of(listCtrl.strListReverseExp, optNotInPlace), Funcs.LIST_REVERSED);
      nameMap.put(FuncId.of(listCtrl.strListSortExp, optInPlace), Funcs.LIST_SORT);
      nameMap.put(FuncId.of(listCtrl.strListSortExp, optNotInPlace), Funcs.LIST_SORTED);
      nameMap.put(FuncId.of(listCtrl.strListMaxMinExp, optMax), Funcs.LIST_MAX);
      nameMap.put(FuncId.of(listCtrl.strListMaxMinExp, optMin), Funcs.LIST_MIN);

      nameMap.put(FuncId.of(listCtrl.numListGetExp), Funcs.LIST_GET);
      nameMap.put(FuncId.of(listCtrl.numListSliceExp), Funcs.LIST_SLICE);
      nameMap.put(FuncId.of(listCtrl.numListReverseExp, optInPlace), Funcs.LIST_REVERSE);
      nameMap.put(FuncId.of(listCtrl.numListReverseExp, optNotInPlace), Funcs.LIST_REVERSED);
      nameMap.put(FuncId.of(listCtrl.numListSortExp, optInPlace), Funcs.LIST_NUM_SORT);
      nameMap.put(FuncId.of(listCtrl.numListSortExp, optNotInPlace), Funcs.LIST_NUM_SORTED);
      nameMap.put(FuncId.of(listCtrl.numListMaxMinExp, optMax), Funcs.LIST_NUM_MAX);
      nameMap.put(FuncId.of(listCtrl.numListMaxMinExp, optMin), Funcs.LIST_NUM_MIN);

      nameMap.put(FuncId.of(listCtrl.boolListGetExp), Funcs.LIST_GET);
      nameMap.put(FuncId.of(listCtrl.boolListSliceExp), Funcs.LIST_SLICE);
      nameMap.put(FuncId.of(listCtrl.boolListReverseExp, optInPlace), Funcs.LIST_REVERSE);
      nameMap.put(FuncId.of(listCtrl.boolListReverseExp, optNotInPlace), Funcs.LIST_REVERSED);

      nameMap.put(FuncId.of(listCtrl.colorListGetExp), Funcs.LIST_GET);
      nameMap.put(FuncId.of(listCtrl.colorListSliceExp), Funcs.LIST_SLICE);
      nameMap.put(FuncId.of(listCtrl.colorListReverseExp, optInPlace), Funcs.LIST_REVERSE);
      nameMap.put(FuncId.of(listCtrl.colorListReverseExp, optNotInPlace), Funcs.LIST_REVERSED);

      nameMap.put(FuncId.of(listCtrl.soundListGetExp), Funcs.LIST_GET);
      nameMap.put(FuncId.of(listCtrl.soundListSliceExp), Funcs.LIST_SLICE);
      nameMap.put(FuncId.of(listCtrl.soundListReverseExp, optInPlace), Funcs.LIST_REVERSE);
      nameMap.put(FuncId.of(listCtrl.soundListReverseExp, optNotInPlace), Funcs.LIST_REVERSED);
    }
  }

  /** ユーザ定義関数に関するシンボル名. */
  static class UserDefFunc {

    final String arg = "Arg";
    final String outArg = "OutArg";
    final String nextArg = "NextArg";
    final String argVoid = "ArgVoid";
    final String paramDecl = "ParamDecl";
    final String outParamDecl = "OutParamDecl";
    final String funcName = "FuncName";
    final String voidFuncDef = "VoidFuncDef";
    final String voidFuncCall = "VoidFuncCall";

    /** 関数定義ノードのリスト. */
    final Set<String> list =
        new HashSet<>(java.util.List.of(
            voidFuncDef));

    /** ユーザ定義関数文のリスト. */
    final Set<String> callStatList =
        new HashSet<>(java.util.List.of(
            voidFuncCall));
  }

  /** イベントを発生させる処理に関するシンボル名. */
  static class Event {

    final String keyPressEvent = "KeyPressedEvent";
    final String delayedStartEvent = "DelayedStartEvent";
    final String keyCode = "KeyCode";
    final String delayTime = "DelayTime";

    /** イベントノードのリスト. */
    final Set<String> list =
        new HashSet<>(java.util.List.of(
            keyPressEvent,
            delayedStartEvent));
  }

  /** リスト操作に関するシンボル名. */
  static class ListCtrl {

    final String anyListPushStat = "AnyListPushStat";
    final String anyListLenExp = "AnyListLengthExp";
    final String anyListInsertAllStat = "AnyListInsertAllStat";
    final String anyListInsertStat = "AnyListInsertStat";
    final String anyListSpliceStat = "AnyListSpliceStat";
    final String anyListSetStat = "AnyListSetStat";
    final String anyListIndexOfExp = "AnyListIndexOfExp";
    final String anyListIncludesExp = "AnyListIncludesExp";
    final String anyListCompExp = "AnyListCompExp";
    final String anySetCompExp = "AnySetCompExp";

    final String strListGetExp = "StrListGetExp";
    final String strListSliceExp = "StrListSliceExp";
    final String strListReverseExp = "StrListReverseExp";
    final String strListSortExp = "StrListSortExp";
    final String strListMaxMinExp = "StrListMaxMinExp";

    final String numListGetExp = "NumListGetExp";
    final String numListSliceExp = "NumListSliceExp";
    final String numListReverseExp = "NumListReverseExp";
    final String numListSortExp = "NumListSortExp";
    final String numListMaxMinExp = "NumListMaxMinExp";

    final String boolListGetExp = "BoolListGetExp";
    final String boolListSliceExp = "BoolListSliceExp";
    final String boolListReverseExp = "BoolListReverseExp";

    final String colorListGetExp = "ColorListGetExp";
    final String colorListSliceExp = "ColorListSliceExp";
    final String colorListReverseExp = "ColorListReverseExp";

    final String soundListGetExp = "SoundListGetExp";
    final String soundListSliceExp = "SoundListSliceExp";
    final String soundListReverseExp = "SoundListReverseExp";
  }

  /** グローバルデータに関するシンボル名. */
  static class GlobalDataDecl {

    final String mutexBlockDecl = "MutexBlockDecl";
    final String syncTimerDecl = "SyncTimerDecl";
    final String semaphoreDecl = "SemaphoreDecl";

    final String mutexBlockName = "MutexBlockName";
    final String syncTimerName = "SyncTimerName";
    final String semaphoreName = "SemaphoreName";
    final String nextGlobalDataDecl = "NextGlobalDataDecl";

    final Set<String> list =
        new HashSet<>(java.util.List.of(
            mutexBlockDecl,
            syncTimerDecl,
            semaphoreDecl,
            nextGlobalDataDecl));

    /** グローバルデータの名前を保持するノードが接続されるコネクタのリスト. */
    final Set<String> dataNameCnctrList =
        new HashSet<>(java.util.List.of(
            mutexBlockName,
            syncTimerName,
            semaphoreName));
  }
}
