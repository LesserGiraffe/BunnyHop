package pflab.bunnyHop.compiler;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author K.Koike
 */
public class SymbolNames {

	public static class VarDecl {
		public static final String numVarDecl = "NumVarDecl";
		public static final String numVar = "NumVar";
		public static final String strVarDecl = "StrVarDecl";
		public static final String strVar = "StrVar";
		public static final String boolVarDecl = "BoolVarDecl";
		public static final String boolVar = "BoolVar";
		public static final String strListDecl = "StrListDecl";
		public static final String strList = "StrList";
		public static final String varName = "VarName";
		public static final String listName = "ListName";
		public static final String nextVarDecl = "NextVarDecl";
		public static HashSet<String> varDeclList = 
			new HashSet<>(Arrays.asList(
				numVarDecl, 
				strVarDecl, 
				boolVarDecl,
				strListDecl));
		public static HashSet<String> varList = 
			new HashSet<>(Arrays.asList(
				numVar, 
				strVar, 
				boolVar,
				strList));
		public static Map<String, String> initValMap = 
			new HashMap<String,String>() {{
				put(numVarDecl, "0");
				put(strVarDecl, "''");
				put(boolVarDecl, "false");
				put(strListDecl, "[]");
			}};
	}
	
	public static class Stat {
		public static final String voidStat = "VoidStat";
	}
	
	public static class AssignStat {
		
		public static final String numAssignStat = "NumAssignStat";
		public static final String strAssignStat = "StrAssignStat";
		public static final String boolAssignStat = "BoolAssignStat";
		public static final String nextStat = "NextStat";
		public static final String leftVar = "LeftVar";
		public static HashSet<String> list = 
			new HashSet<>(Arrays.asList(
				numAssignStat, 
				strAssignStat, 
				boolAssignStat));
	}
	
	public static class ControlStat {

		public static final String ifStat = "IfStat";
		public static final String ifElseStat = "IfElseStat";
		public static final String whileStat = "WhileStat";
		public static final String thenStat = "ThenStat";
		public static final String elseStat = "ElseStat";
		public static final String condExp = "CondExp";
		public static final String loopStat = "LoopStat";
		public static final String continueStat = "ContinueStat";
		public static final String breakStat = "BreakStat";
		public static HashSet<String> list = 
			new HashSet<>(Arrays.asList(
				ifStat, 
				ifElseStat, 
				whileStat,
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
		public static final String isFinite = "isFinite";
		public static final String numToStrExp = "NumToStrExp";
		public static final String boolToStrExp = "BoolToStrExp";
		public static final String printStat = "PrintStat";
		public static final String scanExp = "ScanExp";

		public static HashSet<String> preDefFuncCallExpList = 
			new HashSet<>(Arrays.asList(
				numToStrExp,
				boolToStrExp,
				scanExp));	//!< 定義済み関数式のリスト
	
		public static HashSet<String> preDefFuncCallStatList = 
			new HashSet<>(Arrays.asList(
				printStat,
				Array.strArrayPushStat,
				Array.strArrayPopStat,
				Array.strArraySetExp,
				Array.strArrayInsertStat,
				Array.strArrayRemoveStat));	//!< 定義済み関数文のリスト
		
		public static Map<Entry<String, String>, String> preDefFuncNameMap = 
			new HashMap<Entry<String, String>, String>() {{
				put(new AbstractMap.SimpleEntry(numToStrExp, null), "String");
				put(new AbstractMap.SimpleEntry(boolToStrExp, null), "_boolToStr");
				put(new AbstractMap.SimpleEntry(printStat, null), "_println");
				put(new AbstractMap.SimpleEntry(scanExp, null), "_scan");
				put(new AbstractMap.SimpleEntry(isFinite, null), "isFinite");
				put(new AbstractMap.SimpleEntry(Array.strArrayPushStat, null), "_aryPush");
				put(new AbstractMap.SimpleEntry(Array.strArrayPopStat, null), "_aryPop");
				put(new AbstractMap.SimpleEntry(Array.numArrayGetExp, null), "_aryGet");
				put(new AbstractMap.SimpleEntry(Array.strArrayGetExp, null), "_aryGet");
				put(new AbstractMap.SimpleEntry(Array.boolArrayGetExp, null), "_aryGet");
				put(new AbstractMap.SimpleEntry(Array.strArraySetExp, null), "_arySet");
				put(new AbstractMap.SimpleEntry(Array.strArrayInsertStat, null), "_aryInsert");
				put(new AbstractMap.SimpleEntry(Array.strArrayRemoveStat, null), "_aryRemove");
			}};	//!<  <関数呼び出しノード名, 関数呼び出しオプション> -> 関数名
	}
	
	public static class UserDefFunc {
		
		public static final String arg = "Arg";
		public static final String nextArg = "NextArg";
		public static final String argVoid = "ArgVoid";
		public static final String funcDefSctn = "FuncDefSctn";
		public static final String stat = "Stat";
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
		public static HashSet<String> list = 
			new HashSet<>(Arrays.asList(
				strLiteral, 
				lineFeed,
				numLiteral, 
				boolLiteral,
				strEmptyList));
		public static HashSet<String> listTypes = 
			new HashSet<>(Arrays.asList(
				strEmptyList));
		
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
		public static final String strArrayGetExp = "StrArrayGetExp";
		public static final String strArraySetExp = "StrArraySetExp";
		public static final String strArrayLenExp = "StrArrayLengthExp";

		public static final String numArrayGetExp = "NumArrayGetExp";
		public static final String boolArrayGetExp = "BoolArrayGetExp";
		
		public static HashSet<String> lengthExpList = 
			new HashSet<>(Arrays.asList(
				strArrayLenExp));
		
		public static HashSet<String> getExpList = 
			new HashSet<>(Arrays.asList(
				numArrayGetExp,
				strArrayGetExp,
				boolArrayGetExp));
	}
	
	public static class Undefined {
		
		public static Map<String, String> substituteLiteralMap = 
			new HashMap<String,String>() {{
				put(Array.numArrayGetExp, "0");
				put(Array.strArrayGetExp, "''");
				put(Array.boolArrayGetExp, "false");}};	//undefinedが返った場合の代替値
	}
}
