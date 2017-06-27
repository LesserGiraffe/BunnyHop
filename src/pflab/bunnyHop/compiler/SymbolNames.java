package pflab.bunnyHop.compiler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

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
		public static final String varName = "VarName";
		public static final String nextVarDecl = "NextVarDecl";
		public static SortedSet<String> varDeclList = 
			new TreeSet<>(Arrays.asList(
				numVarDecl, 
				strVarDecl, 
				boolVarDecl));
		public static SortedSet<String> varList = 
			new TreeSet<>(Arrays.asList(
				numVar, 
				strVar, 
				boolVar));
		public static Map<String, String> initValMap = 
			new HashMap<String,String>() {{
				put(numVarDecl, "0");
				put(strVarDecl, "''");
				put(boolVarDecl, "false");}};
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
		public static SortedSet<String> list = 
			new TreeSet<>(Arrays.asList(
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
		public static SortedSet<String> list = 
			new TreeSet<>(Arrays.asList(
				ifStat, 
				ifElseStat, 
				whileStat));
	}
		
	public static class BinaryExp {
		
		public static final String fourArithExp = "FourArithExp";
		public static final String binaryBoolExp = "BinaryBoolExp";
		public static final String modExp = "ModExp";
		public static final String numCompExp = "NumCompExp";
		public static final String appendStrExp = "AppendStrExp";
		public static final String leftExp = "LeftExp";
		public static final String rightExp = "RightExp";
		public static final String operator ="Operator";
		public static SortedSet<String> list = 
			new TreeSet<>(Arrays.asList(
				fourArithExp,
				modExp,
				binaryBoolExp,
				numCompExp,
				appendStrExp));	//二項演算子キリスト
		public static SortedSet<String> arithExceptionExp =
			new TreeSet<>(Arrays.asList(
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
		public static SortedSet<String> list = 
			new TreeSet<>(Arrays.asList(
				notExp));
	}
	
	public static class PreDefFunc {
	
		public static final String arg = "Arg";
		public static final String isFinite = "isFinite";
		public static final String numToStrExp = "NumToStrExp";
		public static final String boolToStrExp = "BoolToStrExp";
		public static final String printStat = "PrintStat";

		public static SortedSet<String> preDefFuncCallExpList = 
			new TreeSet<>(Arrays.asList(
				numToStrExp,
				boolToStrExp));	//!< 定義済み関数式のリスト
	
		public static SortedSet<String> preDefFuncCallStatList = 
			new TreeSet<>(Arrays.asList(
				printStat));	//!< 定義済み関数文のリスト
		
		public static Map<String, String> preDefFuncNameMap = 
			new HashMap<String,String>() {{
				put(numToStrExp, "String");
				put(boolToStrExp, "boolToStr");
				put(printStat, "print");
				put(isFinite, "isFinite");}};	//定義済み関数ノード名と関数名のマップ
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
		
		public static SortedSet<String> userDefFuncList = 
			new TreeSet<>(Arrays.asList(
				voidFuncDef,
				numFuncDef,
				boolFuncDef));	//!< 関数定義ノードのリスト
		
		public static SortedSet<String> userDefFuncCallStatList = 
			new TreeSet<>(Arrays.asList(
				voidFuncCall));	//!< ユーザ定義関数文のリスト
	}
	
	public static class Literal {
		public static final String strLiteral = "StrLiteral";
		public static final String lineFeed = "LineFeed";
		public static final String numLiteral = "NumLiteral";
		public static final String boolLiteral = "BoolLiteral";
		public static SortedSet<String> literalList = 
			new TreeSet<>(Arrays.asList(
				strLiteral, 
				lineFeed,
				numLiteral, 
				boolLiteral));
		public static Map<String, String> boolLiteralMap = 
			new HashMap<String,String>() {{
				put("真", "true");
				put("偽", "false");}};
	}
	
}
