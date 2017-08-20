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
package pflab.bunnyhop.compiler;

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
		public static final String numVarDecl = "NumVarDecl";
		public static final String numVar = "NumVar";
		public static final String numListDecl = "NumListDecl";
		public static final String numList = "NumList";
		public static final String strVarDecl = "StrVarDecl";
		public static final String strVar = "StrVar";
		public static final String strListDecl = "StrListDecl";
		public static final String strList = "StrList";
		public static final String boolVarDecl = "BoolVarDecl";
		public static final String boolVar = "BoolVar";
		public static final String boolListDecl = "BoolListDecl";
		public static final String boolList = "BoolList";
		public static final String varName = "VarName";
		public static final String listName = "ListName";
		public static final String nextVarDecl = "NextVarDecl";
		public static HashSet<String> varDeclList = 
			new HashSet<>(Arrays.asList(
				numVarDecl, 
				numListDecl,
				strVarDecl, 
				strListDecl,
				boolVarDecl,
				boolListDecl));
		public static HashSet<String> varList = 
			new HashSet<>(Arrays.asList(
				numVar, 
				numList,
				strVar, 
				strList,
				boolVar,
				boolList));
		public static Map<String, String> initValMap = 
			new HashMap<String,String>() {{
				put(numVarDecl, "0");
				put(numListDecl, "[]");
				put(strVarDecl, "''");
				put(strListDecl, "[]");
				put(boolVarDecl, "false");
				put(boolListDecl, "[]");
			}};
	}
	
	public static class Stat {
		public static final String voidStat = "VoidStat";
		public static final String statList = "StatList";
	}
	
	public static class AssignStat {
		
		public static final String numAssignStat = "NumAssignStat";
		public static final String numAddAssignStat = "NumAddAssignStat";
		public static final String strAssignStat = "StrAssignStat";
		public static final String boolAssignStat = "BoolAssignStat";
		public static final String nextStat = "NextStat";
		public static final String leftVar = "LeftVar";
		public static HashSet<String> list = 
			new HashSet<>(Arrays.asList(
				numAssignStat,
				numAddAssignStat,
				strAssignStat, 
				boolAssignStat));
	}
	
	public static class ControlStat {
		
		public static final String localVarDecl = "LocalVarDecl";
		
		public static final String ifStat = "IfStat";
		public static final String ifElseStat = "IfElseStat";
		public static final String whileStat = "WhileStat";
		public static final String repeatStat = "RepeatStat";
		public static final String thenStat = "ThenStat";
		public static final String elseStat = "ElseStat";
		public static final String condExp = "CondExp";
		public static final String loopStat = "LoopStat";
		public static final String compoundStat = "CompoundStat";
		public static final String continueStat = "ContinueStat";
		public static final String breakStat = "BreakStat";
		public static HashSet<String> list = 
			new HashSet<>(Arrays.asList(
				ifStat, 
				ifElseStat, 
				whileStat,
				repeatStat,
				compoundStat,
				continueStat,
				breakStat));
	}
		
	public static class BinaryExp {
		
		public static final String fourArithExp = "FourArithExp";
		public static final String binaryBoolExp = "BinaryBoolExp";
		public static final String modExp = "ModExp";
		public static final String numCompExp = "NumCompExp";
		public static final String strCompExp = "StrCompExp";
		public static final String appendStrExp = "AppendStrExp";
		public static final String leftExp = "LeftExp";
		public static final String rightExp = "RightExp";
		public static final String operator ="Operator";
		public static HashSet<String> list = 
			new HashSet<>(Arrays.asList(
				fourArithExp,
				modExp,
				binaryBoolExp,
				numCompExp,
				strCompExp,
				appendStrExp));	//二項演算子キリスト
		public static HashSet<String> arithExceptionExp =
			new HashSet<>(Arrays.asList(
				fourArithExp,
				modExp));	//!< 算術演算例外を発生させる式のノード名
		public static Map<String, String> operatorMap = 
			new HashMap<String,String>() {{
				put("＋", " + ");
				put("－", " - ");
				put("÷", " / ");
				put("×", " * ");
				put(">>", " >> ");
				put(">>>"," >>> ");
				put("<<", " << ");
				put("｜", " | ");
				put("＆", " & ");
				put("＾", " ^ ");
				put("かつ", " && ");
				put("または", " || ");
				put("＝", " === ");
				put("≠", " !== ");
				put("＜", " < ");
				put("≦", " <= ");
				put("＞", " > ");
				put("≧", " >= ");}};		
	}
	
	public static class UnaryExp {

		public static final String notExp = "NotExp";
		public static final String primaryExp = "primaryExp";
		public static HashSet<String> list = 
			new HashSet<>(Arrays.asList(
				notExp));
	}
	
	public static class PreDefFunc {
	
		public static final String arg = "Arg";
		public static final String option = "Option";
		
		//ノード名
		public static final String isFinite = "isFinite";
		public static final String numToStrExp = "NumToStrExp";
		public static final String strToNumExp = "StrToNumExp";
		public static final String boolToStrExp = "BoolToStrExp";
		public static final String printStat = "PrintStat";
		public static final String scanExp = "ScanExp";
		public static final String randomIntExp = "RandomIntExp";
		public static final String numRoundExp = "NumRoundExp";

		//オプション名
		public static final String optRound = "四捨五入";
		public static final String optCeil = "切り上げ";
		public static final String optFloor = "切り捨て";
		
		public static HashSet<String> preDefFuncCallExpList = 
			new HashSet<>(Arrays.asList(
				numToStrExp,
				strToNumExp,
				boolToStrExp,
				scanExp,
				numRoundExp,
				randomIntExp));	//!< 定義済み関数式のリスト
	
		public static HashSet<String> preDefFuncCallStatList = 
			new HashSet<>(Arrays.asList(
				printStat,
				Array.strArrayPushStat,
				Array.strArrayPopStat,
				Array.strArraySetStat,
				Array.strArrayInsertStat,
				Array.strArrayRemoveStat,
				Array.strArrayAppendStat,
				Array.strArrayClearStat,
				
				Array.numArrayPushStat,
				Array.numArrayPopStat,
				Array.numArraySetStat,
				Array.numArrayInsertStat,
				Array.numArrayRemoveStat,
				Array.numArrayAppendStat,
				Array.numArrayClearStat,
				
				Array.boolArrayPushStat,
				Array.boolArrayPopStat,
				Array.boolArraySetStat,
				Array.boolArrayInsertStat,
				Array.boolArrayRemoveStat,
				Array.boolArrayAppendStat,
				Array.boolArrayClearStat)
			);	//!< 定義済み関数文のリスト
		
		public static Map<List<String>, String> preDefFuncNameMap = 
			new HashMap<List<String>, String>() {{
				put(Arrays.asList(numToStrExp), "String");
				put(Arrays.asList(boolToStrExp), "_boolToStr");
				put(Arrays.asList(strToNumExp), "_strToNum");
				put(Arrays.asList(printStat), "_println");
				put(Arrays.asList(scanExp), "_scan");
				put(Arrays.asList(isFinite), "isFinite");
				put(Arrays.asList(numRoundExp, optRound), "Math.round");
				put(Arrays.asList(numRoundExp, optCeil), "Math.ceil");
				put(Arrays.asList(numRoundExp, optFloor), "Math.floor");
				put(Arrays.asList(randomIntExp), "_randomInt");
				
				put(Arrays.asList(Array.strArrayPushStat), "_aryPush");
				put(Arrays.asList(Array.strArrayPopStat), "_aryPop");
				put(Arrays.asList(Array.strArrayGetExp), "_aryGet");
				put(Arrays.asList(Array.strArrayGetLastExp), "_aryGet");
				put(Arrays.asList(Array.strArraySetStat), "_arySet");
				put(Arrays.asList(Array.strArrayInsertStat), "_aryInsert");
				put(Arrays.asList(Array.strArrayRemoveStat), "_aryRemove");
				put(Arrays.asList(Array.strArrayAppendStat), "_aryAddAll");
				put(Arrays.asList(Array.strArrayClearStat), "_aryClear");
				
				put(Arrays.asList(Array.numArrayPushStat), "_aryPush");
				put(Arrays.asList(Array.numArrayPopStat), "_aryPop");
				put(Arrays.asList(Array.numArrayGetExp), "_aryGet");
				put(Arrays.asList(Array.numArrayGetLastExp), "_aryGet");
				put(Arrays.asList(Array.numArraySetStat), "_arySet");
				put(Arrays.asList(Array.numArrayInsertStat), "_aryInsert");
				put(Arrays.asList(Array.numArrayRemoveStat), "_aryRemove");
				put(Arrays.asList(Array.numArrayAppendStat), "_aryAddAll");
				put(Arrays.asList(Array.numArrayClearStat), "_aryClear");
				
				put(Arrays.asList(Array.boolArrayPushStat), "_aryPush");
				put(Arrays.asList(Array.boolArrayPopStat), "_aryPop");
				put(Arrays.asList(Array.boolArrayGetExp), "_aryGet");
				put(Arrays.asList(Array.boolArrayGetLastExp), "_aryGet");
				put(Arrays.asList(Array.boolArraySetStat), "_arySet");
				put(Arrays.asList(Array.boolArrayInsertStat), "_aryInsert");
				put(Arrays.asList(Array.boolArrayRemoveStat), "_aryRemove");
				put(Arrays.asList(Array.boolArrayAppendStat), "_aryAddAll");
				put(Arrays.asList(Array.boolArrayClearStat), "_aryClear");
			}};	//!<  (関数呼び出しノード名, 関数呼び出しオプション...) -> 関数名
	}
	
	public static class UserDefFunc {
		
		public static final String arg = "Arg";
		public static final String nextArg = "NextArg";
		public static final String argVoid = "ArgVoid";
		public static final String funcDefSctn = "FuncDefSctn";
		public static final String paramDecl = "ParamDecl";

		public static final String voidFuncDef = "VoidFuncDef";
		public static final String numFuncDef = "NumFuncDef";
		public static final String boolFuncDef = "BoolFuncDef";

		public static final String voidFuncCall = "VoidFuncCall";
		
		public static HashSet<String> userDefFuncList = 
			new HashSet<>(Arrays.asList(
				voidFuncDef,
				numFuncDef,
				boolFuncDef));	//!< 関数定義ノードのリスト
		
		public static HashSet<String> userDefFuncCallStatList = 
			new HashSet<>(Arrays.asList(
				voidFuncCall));	//!< ユーザ定義関数文のリスト
	}
	
	public static class Literal {
		public static final String strLiteral = "StrLiteral";
		public static final String lineFeed = "LineFeed";
		public static final String numLiteral = "NumLiteral";
		public static final String boolLiteral = "BoolLiteral";
		public static final String strEmptyList = "StrEmptyList";
		public static final String numEmptyList = "NumEmptyList";
		public static final String boolEmptyList = "BoolEmptyList";
		public static HashSet<String> list = 
			new HashSet<>(Arrays.asList(
				strLiteral, 
				lineFeed,
				numLiteral, 
				boolLiteral,
				strEmptyList,
				numEmptyList,
				boolEmptyList));
		public static HashSet<String> listTypes = 
			new HashSet<>(Arrays.asList(
				strEmptyList,
				numEmptyList,
				boolEmptyList));
		
		public static Map<String, String> boolLiteralMap = 
			new HashMap<String,String>() {{
				put("真", "true");
				put("偽", "false");}};
	}
	
	public static class Array {
		public static final String array = "Array";
		public static final String index = "Index";
		
		public static final String strArrayPushStat = "StrArrayPushStat";
		public static final String strArrayPopStat = "StrArrayPopStat";
		public static final String strArrayInsertStat = "StrArrayInsertStat";
		public static final String strArrayRemoveStat = "StrArrayRemoveStat";
		public static final String strArrayAppendStat = "StrArrayAppendStat";
		public static final String strArrayClearStat = "StrArrayClearStat";
		public static final String strArrayGetExp = "StrArrayGetExp";
		public static final String strArrayGetLastExp = "StrArrayGetLastExp";
		public static final String strArraySetStat = "StrArraySetStat";
		public static final String strArrayLenExp = "StrArrayLengthExp";

		public static final String numArrayPushStat = "NumArrayPushStat";
		public static final String numArrayPopStat = "NumArrayPopStat";
		public static final String numArrayInsertStat = "NumArrayInsertStat";
		public static final String numArrayRemoveStat = "NumArrayRemoveStat";
		public static final String numArrayAppendStat = "NumArrayAppendStat";
		public static final String numArrayClearStat = "NumArrayClearStat";
		public static final String numArrayGetExp = "NumArrayGetExp";
		public static final String numArrayGetLastExp = "NumArrayGetLastExp";
		public static final String numArraySetStat = "NumArraySetStat";
		public static final String numArrayLenExp = "NumArrayLengthExp";
		
		public static final String boolArrayPushStat = "BoolArrayPushStat";
		public static final String boolArrayPopStat = "BoolArrayPopStat";
		public static final String boolArrayInsertStat = "BoolArrayInsertStat";
		public static final String boolArrayRemoveStat = "BoolArrayRemoveStat";
		public static final String boolArrayAppendStat = "BoolArrayAppendStat";
		public static final String boolArrayClearStat = "BoolArrayClearStat";
		public static final String boolArrayGetExp = "BoolArrayGetExp";
		public static final String boolArrayGetLastExp = "BoolArrayGetLastExp";
		public static final String boolArraySetStat = "BoolArraySetStat";
		public static final String boolArrayLenExp = "BoolArrayLengthExp";
				
		public static HashSet<String> lengthExpList = 
			new HashSet<>(Arrays.asList(
				strArrayLenExp,
				numArrayLenExp,
				boolArrayLenExp));
		
		public static HashSet<String> getExpList = 
			new HashSet<>(Arrays.asList(
				numArrayGetExp,
				numArrayGetLastExp,
				strArrayGetExp,
				strArrayGetLastExp,
				boolArrayGetExp,
				boolArrayGetLastExp));
	}
	
	public static class Undefined {
		
		public static Map<String, String> substituteLiteralMap = 
			new HashMap<String, String>() {{
				put(Array.numArrayGetExp, "0");
				put(Array.numArrayGetLastExp, "0");
				put(Array.strArrayGetExp, "''");
				put(Array.strArrayGetLastExp, "''");
				put(Array.boolArrayGetExp, "false");
				put(Array.boolArrayGetLastExp, "false");}};	//undefinedが返った場合の代替値
	}
}
