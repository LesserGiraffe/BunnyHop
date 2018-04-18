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
package net.seapanda.bunnyhop.programexecenv.tools;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author K.Koike
 */
public class Util {

	public static final Util INSTANCE = new Util();		//!< シングルトンインスタンス
	public final String EXEC_PATH;
	private final String OS_NAME = System.getProperty("os.name").toLowerCase();
	public final Platform PLATFORM = this.new Platform();

	private Util() {

		boolean isModulePath = true;
		String pathStr = System.getProperty("jdk.module.path");
		if (pathStr == null) {
			isModulePath = false;
			pathStr = System.getProperty("java.class.path");
		}

		String[] paths = pathStr.split(System.getProperty("path.separator"));
		pathStr = paths[paths.length - 1];
		File jarFile = new File(pathStr);
		Path jarPath = Paths.get(jarFile.getAbsolutePath());
		String root = (jarPath.getRoot() == null) ? "" : jarPath.getRoot().toString();

		if (isModulePath) {
			EXEC_PATH = root + jarPath.subpath(0, jarPath.getNameCount()).toString();
		}
		else {
			EXEC_PATH = root + jarPath.subpath(0, jarPath.getNameCount() - 1).toString();
		}
	}


	/**
	 * 引数で指定したパスのファイルが存在しない場合作成する
	 * @param filePath 作成するファイルのパス
	 * @return 作成に失敗した場合false. 作成しなかった場合はtrue
	 */
	public boolean createFileIfNotExists(Path filePath) {
		try {
			if (!Files.exists(filePath))
				Files.createFile(filePath);
		}
		catch (IOException e) {
			LogManager.INSTANCE.msgForDebug("create file err " + filePath + "\n" + e.toString());
			return false;
		}
		return true;
	}

	/**
	 * 引数で指定したパスのディレクトリが存在しない場合作成する
	 * @param dirPath 作成するファイルのパス
	 * @return 作成に失敗した場合false. 作成しなかった場合はtrue
	 */
	public boolean createDirectoryIfNotExists(Path dirPath) {
		try {
			if (!Files.isDirectory(dirPath))
				Files.createDirectory(dirPath);
		}
		catch (IOException e) {
			LogManager.INSTANCE.msgForDebug("create dir err " + dirPath + "\n" + e.toString());
			return false;
		}
		return true;
	}


	public class Platform {

		public boolean isWindows() {
			return OS_NAME.startsWith("windows");
		}

		public boolean isLinux() {
			return OS_NAME.startsWith("linux");
		}
	}
}
