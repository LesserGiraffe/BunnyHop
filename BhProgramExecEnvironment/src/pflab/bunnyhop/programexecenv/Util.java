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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author K.Koike
 */
public class Util {
	
	public static final String execPath;
	public static String scriptDir = "compiled";
	
	static {
		String path = System.getProperty("java.class.path");
		File jarFile = new File(path);
		Path jarPath = Paths.get(jarFile.getAbsolutePath());
		String root = (jarPath.getRoot() == null) ? "" : jarPath.getRoot().toString();
		execPath = root + jarPath.subpath(0, jarPath.getNameCount()-1).toString();
	}
}
