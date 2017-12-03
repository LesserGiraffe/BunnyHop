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
module net.pflab.bunnyhop {
    requires java.rmi;
    requires java.scripting;
    requires jdk.scripting.nashorn;
    requires javafx.graphics;
    requires java.desktop;
	requires javafx.controls;
	requires javafx.fxml;
	
	exports pflab.bunnyhop.root;
	exports pflab.bunnyhop.model;
	exports pflab.bunnyhop.model.templates;
	exports pflab.bunnyhop.model.connective;
	exports pflab.bunnyhop.modelhandler;
	exports pflab.bunnyhop.model.imitation;
	exports pflab.bunnyhop.common;
	opens pflab.bunnyhop.root to javafx.fxml;
	opens pflab.bunnyhop.view to javafx.fxml;
	opens pflab.bunnyhop.control to javafx.fxml;
}
