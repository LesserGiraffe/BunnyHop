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
package pflab.bunnyhop.control;

import pflab.bunnyhop.model.VoidNode;
import pflab.bunnyhop.view.VoidNodeView;

/**
 * VoidNode のコントローラ
 * @author K.Koike
 */
public class VoidNodeController extends BhNodeController {

	private final VoidNode model;	//!< 管理するモデル
	private final VoidNodeView view;	//!< 管理するビュー

	/**
	 * コンストラクタ
	 * @param model 管理するモデル
	 * @param view 管理するビュー
	 * */
	public VoidNodeController(VoidNode model, VoidNodeView view) {
		super(model, view);
		this.model = model;
		this.view = view;
		view.getEventManager().setOnMousePressedHandler(mouseEvent -> {
			mouseEvent.consume();
		});
		view.getEventManager().setOnMouseDraggedHandler(null);
		view.getEventManager().setOnDragDetectedHandler(null);
		view.getEventManager().setOnMouseReleasedHandler(null);
	}
}








