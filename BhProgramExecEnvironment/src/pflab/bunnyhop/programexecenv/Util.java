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
package pflab.bunnyhop.programexecenv;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author K.Koike
 */
public class Util {
	
	public static final String EXEC_PATH;
	public static final String SCRIPT_DIR = "compiled";
	
	static {		
		String pathStr = System.getProperty("jdk.module.path");
		if (pathStr == null) {	//for java8
			pathStr = System.getProperty("java.class.path");		
			Path path = Paths.get(pathStr);
			String root = ((path.getRoot() != null) ? path.getRoot().toString() : "");
			EXEC_PATH = root + path.subpath(0, path.getNameCount()-1).toString();
		}
		else {
			EXEC_PATH = Paths.get(pathStr).toAbsolutePath().toString();
		}
	}
}
