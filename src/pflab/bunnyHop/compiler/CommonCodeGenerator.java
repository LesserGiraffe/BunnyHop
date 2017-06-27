package pflab.bunnyHop.compiler;

import pflab.bunnyHop.model.SyntaxSymbol;

/**
 * @author K.Koike
 * 式や文のコード生成に必要な共通の機能を持つクラス
 */
public class CommonCodeGenerator {
	
	/**
	 * 変数定義から変数名を生成する
	 * @param varDecl 変数定義ノード
	 * @return 変数名
	 */
	public String genVarName(SyntaxSymbol varDecl) {
		return BhCompiler.Keywords.varPrefix + Integer.toHexString(varDecl.hashCode());
	}
	
	/**
	 * 関数定義から関数名を生成する
	 * @param funcDef
	 * @return 関数名
	 */
	public String genFuncName(SyntaxSymbol funcDef) {
		return BhCompiler.Keywords.funcPrefix + Integer.toHexString(funcDef.hashCode());
	}
		
	/**
	 * 関数呼び出しのコードを作成する
	 * @param funcName 関数名
	 * @param argNames 引数名のリスト
	 * @return 関数呼び出しのコード
	 */
	public String genFuncCallCode(String funcName, String... argNames) {
		
		StringBuilder code = new StringBuilder();
		code.append(funcName)
			.append("(");
		for (int i  = 0; i < argNames.length - 1; ++i) {
			code.append(argNames[i]).append(",");
		}
		
		if (argNames.length == 0) {
			code.append(")");
		}
		else {
			code.append(argNames[argNames.length-1])
				.append(")");
		}		
		return code.toString();
	}
	
	public String indent(int depth) {
		
		switch(depth) {
			case 0: return "";
			case 1: return "	";
			case 2: return "		";
			case 3: return "			";
			case 4: return "				";
			case 5: return "					";
			case 6: return "						";
			case 7: return "							";
			case 8: return "								";
			case 9: return "									";
			case 10: return "										";
			case 11: return "											";
			case 12: return "												";
			default:{
				String ret = "";
				for (int i = 0; i < depth; ++i)
					ret += "	";
				return ret;
			}
		}
	}
}
