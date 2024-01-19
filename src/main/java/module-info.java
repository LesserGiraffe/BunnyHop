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
module net.seapanda.bunnyhop {
	requires java.rmi;
	requires java.scripting;
	requires transitive javafx.controls;
	requires javafx.fxml;
	requires transitive javafx.base;
	requires rhino;
	requires jsch;
	requires org.apache.commons.lang3;
	//requires org.scenicview.scenicview;

	exports net.seapanda.bunnyhop.model.node;
	exports net.seapanda.bunnyhop.model.node.attribute;
	exports net.seapanda.bunnyhop.model.node.event;
	exports net.seapanda.bunnyhop.modelservice;
	exports net.seapanda.bunnyhop.model.syntaxsynbol;
	exports net.seapanda.bunnyhop.common.constant;
	exports net.seapanda.bunnyhop.bhprogram.common;	//[java -jar BhProgramExecEnv.jar] を自己完結型の Javaから呼ぶために必要
	exports net.seapanda.bunnyhop.view;
	exports net.seapanda.bunnyhop.view.node;
	exports net.seapanda.bunnyhop.view.node.part;
	opens net.seapanda.bunnyhop.view.workspace to javafx.fxml;
	opens net.seapanda.bunnyhop.view.nodeselection to javafx.fxml;
	opens net.seapanda.bunnyhop.control.workspace to javafx.fxml;
	opens net.seapanda.bunnyhop.control.nodeselection to javafx.fxml;
}
