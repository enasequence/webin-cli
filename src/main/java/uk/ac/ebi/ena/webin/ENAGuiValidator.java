/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.webin;

//import javafx.application.Application;
//import javafx.beans.value.ChangeListener;
//import javafx.beans.value.ObservableValue;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.event.ActionEvent;
//import javafx.event.EventHandler;
//import javafx.scene.Scene;
//import javafx.scene.control.Button;
//import javafx.scene.control.ComboBox;
//import javafx.scene.control.Label;
//import javafx.scene.control.TextField;
//import javafx.scene.layout.GridPane;
//import javafx.scene.text.Font;
//import javafx.scene.text.FontWeight;
//import javafx.scene.text.Text;
//import javafx.stage.Stage;

public class ENAGuiValidator /*extends Application */{

//	public static void main(String[] args) {
//
//		Application.launch(args);
//	}
//
//	@Override
//	public void start(Stage primaryStage) {
//		GridPane grid = new GridPane();
//		Text scenetitle = new Text("ENA Validator");
//		scenetitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
//		grid.add(scenetitle, 0, 0, 2, 1);
//		Label contextLabel = new Label("context:");
//		grid.add(contextLabel, 0, 1);
//		ObservableList<String> options = FXCollections.observableArrayList("Genome", "Transcriptome", "read");
//		final ComboBox<String> contextCombo = new ComboBox<String>(options);
//		grid.add(contextCombo, 1, 1);
//		contextCombo.valueProperty().addListener(new ChangeListener<String>() {
//		        @Override
//				public void changed(ObservableValue<? extends String> arg0, String arg1, String value) {
//		       		 
//				if(value.equals("Genome"))
//				{
//					Label userName = new Label("organism:");
//					grid.add(userName, 0, 2);
//					TextField organismFiled = new TextField();
//			        grid.add(organismFiled, 1, 2);
//			    	Label fileType = new Label("fileType:");
//					grid.add(fileType, 0, 3);
//					ObservableList<String> fileTypeOptions = FXCollections.observableArrayList("Fasta", "AGP", "AssemblyInfo","FlatFile","ChromosomeList","UnlocalizedList");
//			        ComboBox<String> fileTypes = new ComboBox<String>(fileTypeOptions);
//			        grid.add(fileTypes, 1, 3);
//			        Label filePath = new Label("filePath:");
//					grid.add(filePath, 0, 4);
//					TextField filePathField = new TextField();
//			        grid.add(filePathField, 1, 4);
//			        Button addFile = new Button("addFile");
//			        grid.add(addFile,2,4);
//			        addFile.setOnAction(new EventHandler<ActionEvent>() {
//			            @Override
//			            public void handle(ActionEvent event) {
//			            	Label fileType = new Label("fileType:");
//							grid.add(fileType, 0, 5);
//							ObservableList<String> fileTypeOptions = FXCollections.observableArrayList("Fasta", "AGP", "AssemblyInfo","FlatFile","ChromosomeList","UnlocalizedList");
//					        ComboBox<String> fileTypes = new ComboBox<String>(fileTypeOptions);
//					        grid.add(fileTypes, 1, 5);
//					        Label filePath = new Label("filePath:");
//							grid.add(filePath, 0, 6);
//							TextField filePathField = new TextField();
//					        grid.add(filePathField, 1, 6);
//					        Button addFile = new Button("addFile");
//					        grid.add(addFile,2,6);
//			            }
//			        });
//			  	}
//				}    
//		    });
//		
//		Scene scene = new Scene(grid, 300, 275);
//		primaryStage.setScene(scene);
//		primaryStage.show(); 
//		

//	}
}