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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Optional;

import javafx.scene.control.Alert;
import net.seapanda.bunnyhop.bhprogram.common.BhProgramData;
import net.seapanda.bunnyhop.common.constant.BhParams;
import net.seapanda.bunnyhop.common.tools.MsgPrinter;
import net.seapanda.bunnyhop.common.tools.Util;
import net.seapanda.bunnyhop.model.node.BhNode;

/**
 * BhNode をコンパイルするクラス.
 * @author K.Koike
 */
public class BhCompiler {

	public static final BhCompiler INSTANCE = new BhCompiler() {};	//!< シングルトンインスタンス
	private final VarDeclCodeGenerator varDeclCodeGen;
	private final FuncDefCodeGenerator funcDefCodeGen;
	private final StatCodeGenerator statCodeGen;
	private final EventHandlerCodeGenerator eventHandlerCodeGen;
	private final CommonCodeGenerator common;
	private final GlobalDataDeclCodeGenerator globalDataDeclCodeGen;
	private String commonCode;
	private String remoteCommonCode;
	private String localCommonCode;

	private BhCompiler(){

		common = new CommonCodeGenerator();
		varDeclCodeGen = new VarDeclCodeGenerator(common);
		ExpCodeGenerator expCodeGen = new ExpCodeGenerator(common, varDeclCodeGen);
		statCodeGen = new StatCodeGenerator(common, expCodeGen, varDeclCodeGen);
		funcDefCodeGen = new FuncDefCodeGenerator(common, statCodeGen, varDeclCodeGen);
		eventHandlerCodeGen = new EventHandlerCodeGenerator(common, statCodeGen, varDeclCodeGen);
		globalDataDeclCodeGen = new GlobalDataDeclCodeGenerator(common, expCodeGen);
	}

	/**
	 * コンパイルに必要な初期化処理を行う.
	 * @return 初期化に成功した場合true
	 */
	public boolean init() {

		Path commonCodePath = Paths.get(Util.INSTANCE.EXEC_PATH,
			BhParams.Path.BH_DEF_DIR,
			BhParams.Path.FUNCTIONS_DIR,
			BhParams.Path.lib,
			BhParams.Path.COMMON_CODE_JS);
		Path remoteCommonCodePath = Paths.get(Util.INSTANCE.EXEC_PATH,
			BhParams.Path.BH_DEF_DIR,
			BhParams.Path.FUNCTIONS_DIR,
			BhParams.Path.lib,
			BhParams.Path.REMOTE_COMMON_CODE_JS);
		Path localCommonCodePath = Paths.get(Util.INSTANCE.EXEC_PATH,
			BhParams.Path.BH_DEF_DIR,
			BhParams.Path.FUNCTIONS_DIR,
			BhParams.Path.lib,
			BhParams.Path.LOCAL_COMMON_CODE_JS);
		try {
			byte[] content = Files.readAllBytes(commonCodePath);
			commonCode = new String(content, StandardCharsets.UTF_8);
			content = Files.readAllBytes(remoteCommonCodePath);
			remoteCommonCode = new String(content, StandardCharsets.UTF_8);
			content = Files.readAllBytes(localCommonCodePath);
			localCommonCode = new String(content, StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			MsgPrinter.INSTANCE.errMsgForDebug("failed to initialize + " + BhCompiler.class.getSimpleName() + "\n" + e.toString());
			return false;
		}
		return true;
	}

	/**
	 * ワークスペース中のノードをコンパイルし, 作成されたファイルのパスを返す
	 * @param execNode 実行するノード
	 * @param nodesToCompile コンパイル対象のノードリスト (execNode を含む)
	 * @param option コンパイルオプション
	 * @return コンパイルした結果作成されたファイルのパス(コンパイルできた場合). <br>
	 *          コンパイルできなかった場合はOptional.empty
	 */
	public Optional<Path> compile(
		BhNode execNode, Collection<BhNode> nodesToCompile, CompileOption option) {

		Preprocessor.process(nodesToCompile);
		StringBuilder code = new StringBuilder();
		genCode(code, execNode, nodesToCompile, option);

		Util.INSTANCE.createDirectoryIfNotExists(Paths.get(Util.INSTANCE.EXEC_PATH, BhParams.Path.COMPILED_DIR));
		Path appFilePath = Paths.get(Util.INSTANCE.EXEC_PATH, BhParams.Path.COMPILED_DIR, BhParams.Path.APP_FILE_NAME_JS);
		try (BufferedWriter writer =
			Files.newBufferedWriter(
				appFilePath,
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.WRITE)) {
			writer.write(code.toString());
		}
		catch (IOException e) {
			MsgPrinter.INSTANCE.alert(
				Alert.AlertType.ERROR,
				"ファイル書き込みエラー",
				null,
				e.toString() + "\n" + appFilePath.toString());
			return Optional.empty();
		}
		MsgPrinter.INSTANCE.msgForUser("\n-- コンパイル成功 --\n");
		return Optional.of(appFilePath);
	}

	/**
	 * プログラム全体のコードを生成する.
	 * @param code 生成したソースコードの格納先
	 * @param execNode 実行するノード
	 * @param nodeListToCompile コンパイル対象のノードリスト (execNode を含む)
	 * @param option コンパイルオプション
	 */
	private void genCode(
		StringBuilder code, BhNode execNode, Collection<BhNode> nodeListToCompile, CompileOption option) {

		code.append(commonCode);
		if (option.local)
			code.append(localCommonCode);
		else
			code.append(remoteCommonCode);

		genCodeForIdentifierDef(code, 1, option);
		varDeclCodeGen.genVarDecls(nodeListToCompile, code, 1, option);
		globalDataDeclCodeGen.genGlobalDataDecls(nodeListToCompile, code, 1, option);
		code.append(Keywords.newLine);
		funcDefCodeGen.genFuncDefs(nodeListToCompile, code, 1, option);
		eventHandlerCodeGen.genEventHandlers(nodeListToCompile, code, 1, option);
		String lockVar = Keywords.Prefix.lockVarPrefix + ScriptIdentifiers.Funcs.BH_MAIN;
		eventHandlerCodeGen.genHeaderSnippetOfEventCall(code, ScriptIdentifiers.Funcs.BH_MAIN, lockVar, 1);
		statCodeGen.genStatement(execNode, code, 5, option);
		eventHandlerCodeGen.genFooterSnippetOfEventCall(code, lockVar, 1);
		String addEventCallStat = common.genFuncCallCode(
			ScriptIdentifiers.Funcs.ADD_EVENT,
			ScriptIdentifiers.Funcs.BH_MAIN,
			"'" + BhProgramData.EVENT.PROGRAM_START.toString() + "'");
		addEventCallStat += ";" + Keywords.newLine;
		code.append(common.indent(1)).append(addEventCallStat).append(Keywords.newLine);
		genCodeForProgramStart(code, 1, option);
	}

	/**
	 * 識別子定義の前の準備を行うコードを生成する
	 * @param code 生成したコードの格納先
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 */
	private void genCodeForIdentifierDef(StringBuilder code, int nestLevel, CompileOption option) {

		code.append(common.indent(nestLevel))
			.append(common.genFuncPrototypeCallCode(ScriptIdentifiers.Funcs.INIT_THIS_OBJ, Keywords.JS._this))
			.append(";").append(Keywords.newLine);
	}

	/**
	 * プログラム開始前の初期化用コードを生成する
	 * @param code 生成したコードの格納先
	 * @param nestLevel ソースコードのネストレベル
	 * @param option コンパイルオプション
	 */
	private void genCodeForProgramStart(StringBuilder code, int nestLevel, CompileOption option) {

		// プログラム開始時刻の更新
		code.append(common.indent(nestLevel))
			.append(ScriptIdentifiers.Vars.PROGRAM_STARTING_TIME)
			.append(" = ")
			.append(common.genFuncCallCode(ScriptIdentifiers.Funcs.CURRENT_TIME_MILLS))
			.append(";").append(Keywords.newLine);
	}
}


