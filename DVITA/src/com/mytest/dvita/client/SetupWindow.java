package com.mytest.dvita.client;

import java.util.LinkedList;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.mytest.dvita.client.DVita;
import com.mytest.dvita.shared.SerializablePair;
import com.mytest.dvita.shared.ConfigRawdataShared;
import com.mytest.dvita.shared.ConfigTopicminingShared;
import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.TreeModelType;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

public class SetupWindow extends Window {

	SetupWindow THIS = this;

	String buttonText = "Load Selected Topic Model";

	ConfigRawdataShared configRawdata = new ConfigRawdataShared();

	private final SetupServiceAsync setupService = GWT.create(SetupService.class);

	private DVita otherWindow;
	private IButton loadButton;
	private TreeGrid treeGrid;

	public void updateTree(LinkedList<SerializablePair<ConfigRawdataShared, LinkedList<ConfigTopicminingShared>>> result) {
		Tree theTree = new Tree();  
		theTree.setModelType(TreeModelType.PARENT);  
		theTree.setRootValue(1);  
		theTree.setOpenProperty("isOpen");  

		TreeNode[] data = new TreeNode[result.size()];
		int databaseNr = 0;
		for(SerializablePair<ConfigRawdataShared, LinkedList<ConfigTopicminingShared>> p : result) {
			data[databaseNr] = new TreeNode("Database: " + p.first.metaTitle);
			data[databaseNr].setAttribute("name","Database: " + p.first.metaTitle);
			data[databaseNr].setIcon("database.png");
			data[databaseNr].setAttribute("isOpen",true);
			data[databaseNr].setAttribute("info",p.first);
			data[databaseNr].setAttribute("descr",p.first.metaDescription);
			data[databaseNr].setAttribute("topics",/*"--"*/ "");		
			data[databaseNr].setCustomStyle(data[databaseNr].getCustomStyle() + ";font-weight:bold");

			TreeNode[] childs = new TreeNode[p.second.size()];
			int pos = 0;
			for(ConfigTopicminingShared i2 : p.second) {
				childs[pos] = new TreeNode(/*"Analysis: " + */i2.metaTitle);
				childs[pos].setAttribute("name",/*"Analysis: " + */i2.metaTitle);
				childs[pos].setIcon("fileopen-icon.gif");
				childs[pos].setAttribute("AnalyzeID",i2.id);
				childs[pos].setAttribute("info",i2);
				childs[pos].setAttribute("descr",i2.metaDescription);
				childs[pos].setAttribute("topics",i2.NumberTopics);


				pos++;
			}
			data[databaseNr].setChildren(childs);
			databaseNr++;
		}

		theTree.setData(data);  



		treeGrid = new TreeGrid();  
		treeGrid.setWidth100();  
		treeGrid.setHeight(THIS.getViewportHeight()-50);  

		treeGrid.setShowOpenIcons(false);  
		treeGrid.setShowDropIcons(false);  
		treeGrid.setClosedIconSuffix("");  
		treeGrid.setData(theTree);  
		treeGrid.setSelectionAppearance(SelectionAppearance.ROW_STYLE);  
		treeGrid.setSelectionType(SelectionStyle.SINGLE);
		treeGrid.setShowSelectedStyle(true);

		treeGrid.setShowPartialSelection(false);  
		treeGrid.setCascadeSelection(false); 

		ListGridField nameField = new ListGridField("name", "Topic Model");
		ListGridField infoField = new ListGridField("descr","Description");
		ListGridField topicField = new ListGridField("topics","# Topics");
		topicField.setWidth(70);

		treeGrid.setFields(nameField,topicField,infoField);


		SelectionChangedHandler handler1 = new SelectionChangedHandler(){

			@Override
			public void onSelectionChanged(SelectionEvent event) {

				if(event.getSelectedRecord()== null) return;

				if(event.getSelectedRecord().getAttributeAsInt("AnalyzeID") != null) {
					loadButton.enable();
					loadButton.setTitle("<b><font color=\"#0000FF\">"+buttonText+"</font></b>");
					
				} else {
					loadButton.disable();
					loadButton.setTitle(buttonText);
				}

			}};
			treeGrid.addSelectionChangedHandler(handler1);


			THIS.addItem(loadButton);
			THIS.addItem(treeGrid);

	}

	public SetupWindow(DVita theotherWindow, boolean initialWindow) {
		otherWindow = theotherWindow; // wenn das fenster geschlossen wird, dann soll die topic liste erscheinen
		// daher brauchen wir hier die variable
		this.setWidth("90%");
		this.setHeight("90%");
		this.setTitle("Select Data Set");
		this.setShowMinimizeButton(false);
		if(initialWindow) {
			this.setShowCloseButton(false);
		} else {
			this.setShowCloseButton(true);	
		}
		this.setIsModal(true);
		this.setShowModalMask(true);
		this.centerInPage();
		this.addCloseClickHandler(new CloseClickHandler() {

			@Override
			public void onCloseClick(CloseClientEvent event) {
				// nur wenn es nicht das initiale window ist erlauben wir ein schließen
				THIS.destroy();
				// so müsste man einfach zum alten fenster zurückkehren
			}
		});

		setupService.getSetupInformation(new AsyncCallback<LinkedList<SerializablePair<ConfigRawdataShared, LinkedList<ConfigTopicminingShared>>>>(){

			@Override
			public void onFailure(Throwable caught) {
			}

			@Override
			public void onSuccess(
					LinkedList<SerializablePair<ConfigRawdataShared, LinkedList<ConfigTopicminingShared>>> result) {


				updateTree(result);

			}});


		loadButton = new IButton(buttonText);
		loadButton.disable();
		loadButton.setAutoFit(true);

		loadButton.addClickHandler(new ClickHandler() {  


			public void onClick(ClickEvent event) {

				if(treeGrid.getSelectedRecords().length==0) {
					com.google.gwt.user.client.Window.alert("Please select an analysis!");
					return;
				}

				Integer selected = treeGrid.getSelectedRecords()[0].getAttributeAsInt("AnalyzeID");
				if(selected == null) {
					com.google.gwt.user.client.Window.alert("Please select an analysis (not a database)!");
					return;
				}
				THIS.disable();
				/////////////////////////////////////////////////////////////////////////////////
				///////////////////////////////////////////////////////////////////

				setUpSession(selected);


			}

		});

	}

	public void setUpSession(Integer selected) {

		///////////////////////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////////

		String user = null;
		String pw = null;
		// TODO hier könnte man auch einbauen, dass user und passort vom programm abgefragt werden
		// TODO bislang wird es automatisch auf der Serverseite/in der Datenbank gesetzt



		setupService.setUpSession(selected, user, pw, new AsyncCallback<ConfigTopicminingShared>(){



			@Override
			public void onFailure(Throwable caught) {
				THIS.enable();
			}

			@Override
			public void onSuccess(ConfigTopicminingShared result) {
				if(result != null) {
					otherWindow.analysisName.setContents("Analysis: "+result.metaTitle);
					otherWindow.retrieveAllTopicsFromServer();
					THIS.destroy();
				} else {
					THIS.enable();
					THIS.show();
				}


			}

		});
	}

}    



