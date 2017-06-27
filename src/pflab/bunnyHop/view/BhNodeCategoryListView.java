package pflab.bunnyHop.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javafx.css.PseudoClass;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import pflab.bunnyHop.ModelProcessor.NodeMVCBuilder;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.TreeNode;
import pflab.bunnyHop.model.BhNode;
import pflab.bunnyHop.model.templates.BhNodeTemplates;
import pflab.bunnyHop.undo.UserOperationCommand;

/**
 * BhNode のカテゴリ選択画面のView
 * @author K.Koike
 * */
public class BhNodeCategoryListView {

	private final TreeView<BhNodeCategory> categoryTree;
	private final Map<BhNodeCategory, BhNodeSelectionView> category_selectionView = new HashMap<>();	//!< BhNode選択カテゴリ名とBhNode選択ビューのマップ
	private final List<BhNodeSelectionView> selectionViewList = new ArrayList<>();	//!< BhNode選択ビューのリスト
	private final List<BhNodeCategory> categoryList = new ArrayList<>();
	
	
	public BhNodeCategoryListView(TreeView<BhNodeCategory> categoryTree) {
		this.categoryTree = categoryTree;
	}

	/**
	 * BhNode のカテゴリリスト画面を作る
	 * @param root 選択リストのノード
	 * */
	public void buildCategoryList(TreeNode<String> root) {

		TreeItem<BhNodeCategory> rootItem = new TreeItem<>(new BhNodeCategory(root.content));
		rootItem.setExpanded(true);
		addChildren(root, rootItem);
		categoryTree.setRoot(rootItem);
		categoryTree.setShowRoot(false);
		categoryTree.setCellFactory(templates -> {
			return new BhNodeCategoryView();
		});
	}

	/**
	 * ノード選択ビューのリストを取得する
	 * @return ノード選択ビューのリスト
	 */
	public List<BhNodeSelectionView> getSelectionViewList() {
		return selectionViewList;
	}
	
	/**
	 * テンプレートツリーに子ノードを追加する.<br>
	 * @param parent 追加する子ノード情報を持ったノード
	 * @param parentItem 子ノードを追加したいノード
	 * */
	private void addChildren(TreeNode<String> parent, TreeItem<BhNodeCategory> parentItem) {

		parent.children.forEach(child -> {
			
			if(child.content.equals(BhParams.NodeTemplateList.keyNameCssClass)) {
				String cssClass = child.children.get(0).content;
				parentItem.getValue().setCssClass(cssClass);
			}
			else if (child.content.equals(BhParams.NodeTemplateList.keyNameContents)) {
				child.children.forEach((bhNodeID) -> {
					addBhNodeToSelectionView(parentItem.getValue(), bhNodeID.content);
				});
			}
			else {
				BhNodeCategory category = new BhNodeCategory(child.content);
				categoryList.add(category);
				TreeItem<BhNodeCategory> childItem = new TreeItem<>(category);
				parentItem.getChildren().add(childItem);
				childItem.setExpanded(true);
				addChildren(child, childItem);
			}
		});
	}

	/**
	 * ノード選択ビューにBhNodeを追加する
	 * @param category ノード選択ビューが属するカテゴリ
	 * @param bhNodeID 追加するBhNodeのID
	 */
	private void addBhNodeToSelectionView(BhNodeCategory category, String bhNodeID) {
		
		if (!category_selectionView.containsKey(category)) {
			BhNodeSelectionView selectionView = new BhNodeSelectionView();
			selectionView.init(category.categoryName, category.cssClass);
			category.setFuncOnSelectionViewShowed(selectionView::setVisible);
			category_selectionView.put(category, selectionView);
			selectionViewList.add(selectionView);
			selectionView.setVisible(false);
		}
		UserOperationCommand userOpeCmd = new UserOperationCommand();
		BhNode node = BhNodeTemplates.instance().genBhNode(bhNodeID, userOpeCmd);
		NodeMVCBuilder builder = new NodeMVCBuilder(NodeMVCBuilder.ControllerType.Template, userOpeCmd);
		node.accept(builder);	//MVC構築
		category_selectionView.get(category).addBhNodeView(builder.getTopNodeView());	//BhNode テンプレートリストパネルにBhNodeテンプレートを追加	
	}
		
	/**
	 * 全てのBhNode選択パネルを隠す
	 * */
	public void hideAll() {
		for (BhNodeCategory category : categoryList) {
			category.hide();
		}
	}

	/**
	 * カテゴリ名格納クラス
	 * */
	public class BhNodeCategory {

		public final String categoryName;
		private boolean displayed = false;
		private String cssClass = "";
		Consumer<Boolean> showCellView;
		Consumer<Boolean> showTemplatePanel;

		public BhNodeCategory(String category) {
			this.categoryName = category;
		}

		@Override
		public String toString() {
			return categoryName==null ? "" : categoryName;
		}

		public void show() {

			if (category_selectionView.containsKey(this)) {	//表示するテンプレートパネルがある場合
				BhNodeCategoryListView.this.hideAll();	//前に選択されていたものを非選択にする
				displayed = true;
				showTemplatePanel.accept(true);
			}
			showCellView.accept(true);
		}

		public void hide() {
			displayed = false;
			showCellView.accept(false);
			if (showTemplatePanel != null)	//葉ノード以外のカテゴリ名はパネルを持っていないことがある
				showTemplatePanel.accept(false);
		}

		public boolean isDisplayed() {
			return displayed;
		}

		/**
		 * このカテゴリが表示/非表示されたときの TreeCell の表示/非表示用関数を登録する
		 * */
		public void setFuncOnCellViewShowed(Consumer<Boolean> func) {
			showCellView = func;
		}

		/**
		 * このカテゴリが表示/非表示されたときのテンプレートパネル表示/非表示用関数を登録する
		 * */
		public void setFuncOnSelectionViewShowed(Consumer<Boolean> func) {
			showTemplatePanel = func;
		}
		
		public void setCssClass(String cssClass) {
			this.cssClass = cssClass;
		}
		
		public String getCssClass() {
			return cssClass;
		}
	}

	/**
	 * BhNode カテゴリのView.  BhNodeCategoryとの結びつきは動的に変わる
	 * */
	public class BhNodeCategoryView extends TreeCell<BhNodeCategory> {

		BhNodeCategory model;
		public BhNodeCategoryView() {

			// BhNode のカテゴリクリック時の処理
			setOnMousePressed(evenet -> {

				//カテゴリ名の無いTreeCell がクリックされたときはノード選択パネルを隠す
				if (isEmpty()) {
					BhNodeCategoryListView.this.hideAll();
					return;
				}

				if (model.isDisplayed()) {	//表示済みカテゴリを再度クリックした場合はそれを隠す
					BhNodeCategoryListView.this.hideAll();
				}
				else {
					model.show();
				}
			});
		}

		/**
		 * TreeItemの選択状態を解除する
		 * */
		public void select(boolean select) {
			pseudoClassStateChanged(PseudoClass.getPseudoClass(BhParams.CSS.pseudoSelected), select);
		}

		@Override
		protected void updateItem(BhNodeCategory category, boolean empty) {
			
			super.updateItem(category, empty);
			model = category;
			if (!empty) {
				category.setFuncOnCellViewShowed(this::select);
				getStyleClass().add(model.getCssClass());				
				pseudoClassStateChanged(PseudoClass.getPseudoClass(BhParams.CSS.pseudoEmpty), false);
				setText(category.toString());
			}
			else {
				select(false);
				getStyleClass().clear();
				getStyleClass().add("tree-cell");
				pseudoClassStateChanged(PseudoClass.getPseudoClass(BhParams.CSS.pseudoEmpty), true);
				setText(null);
			}
		}
	}
}












