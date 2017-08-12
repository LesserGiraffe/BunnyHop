package pflab.bunnyHop.view;

import java.io.IOException;
import java.nio.file.Path;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import pflab.bunnyHop.root.MsgPrinter;
import pflab.bunnyHop.common.BhParams;
import pflab.bunnyHop.common.Point2D;
import pflab.bunnyHop.configFileReader.FXMLCollector;

/**
 * ノードカテゴリ選択時にワークスペース上に現れるBhNode 選択パネル
 * @author K.Koike
 * */
public class BhNodeSelectionView extends ScrollPane {

	@FXML Pane nodeSelectionPanel;	//VBoxにしない
	@FXML Pane nodeSelectionPanelWrapper;
	private int zoomLevel = 0;
	private boolean hasNodeHeightChanged = true;	//!< ノードを並べた後に, 表示するノードの高さが変わった場合true
	
	public BhNodeSelectionView() {}

	/**
	 * GUI初期化
	 * @param categoryName このビューに関連付けられたBhNodeリストのカテゴリ名
	 * @param cssClass ビューに適用するcssクラス名
	 * @param categoryListView このビューを保持しているカテゴリリストのビュー
	 */
	public void init(String categoryName, String cssClass, BhNodeCategoryListView categoryListView) {
		try {
			Path filePath = FXMLCollector.instance.getFilePath(BhParams.Path.nodeSelectionPanelFxml);
			FXMLLoader loader = new FXMLLoader(filePath.toUri().toURL());
			loader.setController(this);
			loader.setRoot(this);
			loader.load();
		}
		catch (IOException e) {
			MsgPrinter.instance.ErrMsgForDebug("failed to initialize "  + BhNodeSelectionView.class.getSimpleName());
		}
		
		nodeSelectionPanel.getTransforms().add(new Scale());
		
		//拡大縮小処理
		this.addEventFilter(ScrollEvent.ANY, event -> {
			
			if (event.isControlDown()) {
				event.consume();
				boolean zoomIn = event.getDeltaY() >= 0;
				categoryListView.zoomAll(zoomIn);
			}
		});
		
		getStyleClass().add(cssClass);
		nodeSelectionPanel.getStyleClass().add(cssClass);		
	}
	
	/**
	 * テンプレートリストに表示するBhNode のビューを追加する
	 * @param view テンプレートリストに表示するBhNodeのビュー
	 * */
	public void addBhNodeView(BhNodeView view) {

		nodeSelectionPanel.getChildren().add(view);
		view.heightProperty().addListener((observable, oldVal, newVal) -> {
			hasNodeHeightChanged = true;
		});
	}
	
	/**
	 * ノード選択ビューのズーム処理を行う
	 * @param zoomIn 拡大処理を行う場合true
	 */
	public void zoom(boolean zoomIn) {
		
		if ((BhParams.minZoomLevel == zoomLevel) && !zoomIn)
			return;
		
		if ((BhParams.maxZoomLevel == zoomLevel) && zoomIn)
			return;
		
		Scale scale = new Scale();
		if (zoomIn) {
			scale.setX(nodeSelectionPanel.getTransforms().get(0).getMxx() * BhParams.wsMagnification);
			scale.setY(nodeSelectionPanel.getTransforms().get(0).getMyy() * BhParams.wsMagnification);
			++zoomLevel;
		}
		else {
			scale.setX(nodeSelectionPanel.getTransforms().get(0).getMxx() / BhParams.wsMagnification);
			scale.setY(nodeSelectionPanel.getTransforms().get(0).getMyy() / BhParams.wsMagnification);
			--zoomLevel;
		}
		nodeSelectionPanel.getTransforms().clear();
		nodeSelectionPanel.getTransforms().add(scale);		
		nodeSelectionPanelWrapper.setMinSize(nodeSelectionPanel.getWidth() * nodeSelectionPanel.getTransforms().get(0).getMxx(),
			nodeSelectionPanel.getHeight() * nodeSelectionPanel.getTransforms().get(0).getMyy());	//スクロール時にスクロールバーの可動域が変わるようにする
	}
	
	/**
	 * 表示するノードを並べる
	 */
	public void arrange() {
		
		if (!hasNodeHeightChanged)
			return;
		
		double panelWidth = 0.0;
		double panelHeight = 0.0;
		double offset = 0.0;
		
		for (int i = 0; i < nodeSelectionPanel.getChildren().size(); ++i) {
			BhNodeView nodeToShift = (BhNodeView)nodeSelectionPanel.getChildren().get(i);
			Point2D wholeBodySize = nodeToShift.getRegionManager().getBodyAndOuterSize(true);
			Point2D bodySize = nodeToShift.getRegionManager().getBodyAndOuterSize(false);
			double upperCnctrHeight = wholeBodySize.y - bodySize.y;
			nodeToShift.setTranslateY(offset + upperCnctrHeight);
			offset += wholeBodySize.y + BhParams.bhNodeSpaceOnSelectionPanel;
			panelWidth = Math.max(panelWidth, wholeBodySize.x);
		}
		
		panelHeight = (offset - BhParams.bhNodeSpaceOnSelectionPanel) 
					+ nodeSelectionPanel.getPadding().getTop() 
					+ nodeSelectionPanel.getPadding().getBottom();
		panelWidth += nodeSelectionPanel.getPadding().getRight() + nodeSelectionPanel.getPadding().getLeft();
		nodeSelectionPanel.setMinHeight(panelHeight);
		nodeSelectionPanel.setMinWidth(panelWidth);
		hasNodeHeightChanged = false;
	}
}











