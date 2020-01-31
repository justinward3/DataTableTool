package com.company;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.swing.*;
import javax.swing.plaf.synth.SynthTextAreaUI;
import javax.xml.soap.Text;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

import static javafx.application.Application.launch;

public class GUI extends Application {
    private com.company.App model = new com.company.App();
    private String url = "https://www150.statcan.gc.ca/t1/wds/rest/getCubeMetadata";
    private JSONObject jsonObject;
    private JSONObject CodeSets;
    private ArrayList<String> dimensions = new ArrayList<String>();
    private ArrayList<com.company.Node<JSONObject>> currMembers = new ArrayList<com.company.Node<JSONObject>>();
    private Label Header,Header2,Header3,Header4,Header5,Header6,Header7,Data;
    private TextArea Title,ProductId,Classification;
    private Button Submit,Batch,Exit,Copy,StepIn,StepOut;
    private TextField Entry;
    private CheckBox Save,FootNoteCheck,LevelsCheck;
    private ListView<String> DimensionsList,InnerDimensionsList,SubjectList,SurveyList;
    private ToggleGroup language;
    private String toCopy = new String();
    private Boolean toSave = false;
    private Boolean FootNote = false;
    private Boolean Levels = false;

    //Map with Tree which has keys as Strings and Values which are Arraylist containing JSONObject Nodes
    private LinkedHashMap<String,ArrayList<com.company.Node<JSONObject>>> MemberMap;

    public void start(Stage primaryStage){
        model.currLanguage = "En";
        CodeSets = model.getCodeSets();
        //Labels
        Header = new Label("Variable Tool");Header.relocate(10,10);
        Header2 = new Label("Dimension(s)");Header2.relocate(400,10);
        Header3 = new Label("Title");Header3.relocate(20,155);
        Header4 = new Label("CANSIM #");Header4.relocate(20,105);
        Header5 = new Label("Subject(s)");Header5.relocate(20,230);
        Header6 = new Label("Survey(s)");Header6.relocate(20,300);
        Header7 = new Label("Classification");Header7.relocate(850,10);
        Header7.setVisible(false);
        Data = new Label("Cube #");Data.relocate(20,60);
        //TextField
        Entry = new TextField();Entry.relocate(90,60);

        //Buttons
        Submit = new Button("Submit");Submit.relocate(250,60);
        StepIn = new Button("->");StepIn.relocate(610,10);
        //StepOut = new Button("<-");StepOut.relocate(640,10);
        Batch = new Button("Batch");Batch.relocate(250,90);Batch.setVisible(false);
        Exit = new Button("Exit");Exit.relocate(10,600);
        Copy = new Button("Copy");Copy.relocate(60,600);

        //Data Fields
        ProductId = new TextArea("CANSIM");ProductId.setEditable(false);ProductId.relocate(90,100);ProductId.setPrefSize(120,10);
        Title = new TextArea("Title");Title.setWrapText(true);Title.setEditable(false);Title.relocate(90,150);Title.setPrefSize(260,60);
        Classification = new TextArea();Classification.relocate(850,37);Classification.setPrefSize(120,10);
        Classification.setVisible(false);

        //Checkboxes
        Save = new CheckBox("Save");Save.relocate(310,64);
        //Save Checkbox Action Handler
        Save.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                if(toSave){
                    toSave = false;
                    FootNote = false;
                    FootNoteCheck.setSelected(false);
                    FootNoteCheck.setDisable(true);
                    Levels = false;
                    LevelsCheck.setSelected(false);
                    LevelsCheck.setDisable(true);
                }
                else{
                    toSave = true;
                    FootNoteCheck.setDisable(false);
                    LevelsCheck.setDisable(false);
                }
            }
        });
        FootNoteCheck = new CheckBox("FootNotes");FootNoteCheck.relocate(310,84);
        //FootNote Checkbox Action Handler
        FootNoteCheck.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                if(FootNote){
                    FootNote = false;
                }
                else{
                    FootNote = true;
                }
            }
        });
        LevelsCheck = new CheckBox("Level");LevelsCheck.relocate(310,104);
        //Levels Checkbox Action Handler
        LevelsCheck.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                if(Levels){
                    Levels = false;
                }
                else{
                    Levels = true;
                }
            }
        });

        //Disabled by Default
        FootNoteCheck.setDisable(true);
        LevelsCheck.setDisable(true);

        //ListView Creation
        SubjectList = new ListView<>();SubjectList.setEditable(false);SubjectList.relocate(90,225);SubjectList.setPrefSize(260,60);
        SurveyList = new ListView<String>();SurveyList.setEditable(false);SurveyList.relocate(90,300);SurveyList.setPrefSize(260,60);
        DimensionsList = new ListView<String>();DimensionsList.relocate(400,37);DimensionsList.setPrefSize(200,323);
        InnerDimensionsList= new ListView<String>();InnerDimensionsList.relocate(650,37);InnerDimensionsList.setPrefSize(200,323);

        //Language Toggles
        language = new ToggleGroup();
        RadioButton rb1 = new RadioButton("English");
        rb1.setUserData("English");rb1.setToggleGroup(language);rb1.setSelected(true);rb1.relocate(100,10);
        RadioButton rb2 = new RadioButton("Français");
        rb2.setUserData("Français");rb2.setToggleGroup(language);rb2.relocate(200,10);

        //Toggle Radio Buttons for English or French
        language.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override
            public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {
                if (language.getSelectedToggle() != null){
                    if(language.getSelectedToggle().getUserData().toString() == "English"){
                        //Change Language and set Text corresponding to that langauge
                        model.currLanguage = "En";
                        Header.setText("Variable Tool");Header2.setText("Dimension(s)");
                        Header3.setText("Title");Header4.setText("CANSIM #");ProductId.setText("CANSIM");
                        Header5.setText("Subject(s)");Header6.setText("Surveys(s)");
                        Header7.setText("Classification");Title.setText("Title");Submit.setText("Submit");Exit.setText("Exit");
                        Copy.setText("Copy");SurveyList.getItems().clear();SubjectList.getItems().clear();
                        DimensionsList.getItems().clear();InnerDimensionsList.getItems().clear();
                    }
                    else if(language.getSelectedToggle().getUserData().toString() == "Français"){
                        model.currLanguage = "Fr";
                        Header.setText("Outil Variable");Header2.setText("Dimension(s)");
                        Header3.setText("Titre");Header4.setText("CANSIM #");ProductId.setText("CANSIM");
                        Header5.setText("Sujet(s)");Header6.setText("Enquête(s)");
                        Header7.setText("Classification");Title.setText("Titre");Submit.setText("Soumis");Exit.setText("Quitter");
                        Copy.setText("Copie");Copy.relocate(80,600);SurveyList.getItems().clear();SubjectList.getItems().clear();
                        DimensionsList.getItems().clear();InnerDimensionsList.getItems().clear();
                    }
                }
            }
        });

        //Copy Button Action listeners
        Copy.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                StringSelection selection = new StringSelection(toCopy);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, selection);
            }
        });

        /*
        //A Batch function, not in use
        Batch.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
                              @Override
                              public void handle(javafx.event.ActionEvent event) {
                                  if (toSave) {
                                      ArrayList<String> allIds = new ArrayList<>();
                                      Scanner scanner = null;
                                      try {
                                          scanner = new Scanner(new File("/query.csv"));
                                      } catch (FileNotFoundException e) {
                                          e.printStackTrace();
                                      }
                                      scanner.useDelimiter(",");
                                      int delimetercount = 0;
                                      while (scanner.hasNext()) {
                                          delimetercount++;
                                          String temp = scanner.next();
                                          if (temp.contains("\n")) {
                                              allIds.add(((temp.substring(temp.indexOf("\n"), temp.length())).replace("\n", "")).replaceAll("[^0-9]",""));
                                          }
                                      }
                                      scanner.close();
                                      for (String i : allIds) {
                                          System.out.println(i);
                                          try {
                                              jsonObject = model.sendPostRequest(url, Integer.parseInt(i));
                                              if (jsonObject.get("Status") != "Failed") {
                                                  ProductId.setText(model.getCansimId(jsonObject));
                                                  //Title.setText(model.getTitle(jsonObject, 0));
                                                  //SubjectList.setItems(FXCollections.observableArrayList(model.get(jsonObject, CodeSets, "subject")));
                                                  //SurveyList.setItems(FXCollections.observableArrayList(model.get(jsonObject, CodeSets, "survey")));
                                                  MemberMap = model.getTree(jsonObject);
                                                  LinkedHashMap<Long, HashMap<Long, String>> FootnoteMap = new LinkedHashMap<Long, HashMap<Long, String>>();
                                                  ArrayList<JSONObject> Footnotes = (ArrayList<JSONObject>) (jsonObject.get("footnote"));
                                                  for (JSONObject f : Footnotes) {
                                                      long dpi = (long) ((JSONObject) f.get("link")).get("dimensionPositionId");
                                                      long Mid = (long) ((JSONObject) f.get("link")).get("memberId");
                                                      String fn = (String) f.get("footnotes" + model.currLanguage);
                                                      if (FootnoteMap.containsKey(dpi)) {
                                                          FootnoteMap.get(dpi).put(Mid, fn);
                                                      } else {
                                                          FootnoteMap.put(dpi, new HashMap<Long, String>());
                                                      }
                                                  }
                                                  for (String key : MemberMap.keySet()) {
                                                      try {
                                                          dimensions.add(new String(key.getBytes("UTF-8")));
                                                      } catch (UnsupportedEncodingException e) {
                                                          e.printStackTrace();
                                                      }
                                                  }
                                                  //DimensionsList.setItems(FXCollections.observableArrayList(dimensions));

                                                  //IF SAVING DOCUMENT
                                                  if (FootNote) {
                                                      model.SavetoDoc(Levels, CodeSets, MemberMap, String.valueOf(i), model.getTitle(jsonObject, 0), FootnoteMap);
                                                  } else {
                                                      model.SavetoDoc(Levels, CodeSets, MemberMap, String.valueOf(i), model.getTitle(jsonObject, 0));
                                                  }

                                              }
                                          }
                                          catch (Exception e){
                                              System.out.print(e);System.out.println(i);
                                          }
                                      }
                                  }
                                  else{
                                      Title.setText("You must check Save to perform a batch process");
                                  }
                              }
                          });
        */

        //Exit Button Action handler
        Exit.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                System.exit(0);
            }
        });

        //Submit Button Action handler ( MAIN FUNCTIONALITY)
        Submit.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                //Clear Lists, ArrayList, etc.
                Title.clear();
                ProductId.clear();
                InnerDimensionsList.getItems().clear();
                DimensionsList.getItems().clear();
                dimensions.clear();
                currMembers.clear();
                Classification.clear();
                SubjectList.getItems().clear();
                SurveyList.getItems().clear();

                //Get id from user
                String id = Entry.getText();
                id = id.replace("-", "");
                if (id.length() > 7) {
                    if (id.length() == 10) {
                        id = id.substring(0, 8);
                    }
                    if (id.length() == 8) {
                        int intId = 0;
                        try {
                            intId = Integer.valueOf(id);
                            //Send HTTP Request
                            jsonObject = model.sendPostRequest(url, intId);
                            //If API Responds with a result
                            if (jsonObject.get("Status") != "Failed") {
                                //Set Text in GUI
                                ProductId.setText(model.getCansimId(jsonObject));
                                Title.setText(model.getTitle(jsonObject, 0));
                                SubjectList.setItems(FXCollections.observableArrayList(model.get(jsonObject, CodeSets, "subject")));
                                SurveyList.setItems(FXCollections.observableArrayList(model.get(jsonObject, CodeSets, "survey")));
                                //Create the Tree Structure for the Data Table Member and Dimensions
                                MemberMap = model.getTree(jsonObject);
                                //Create Footnotes Map
                                LinkedHashMap<Long,HashMap<Long,String>> FootnoteMap = new LinkedHashMap<Long,HashMap<Long, String>>();
                                ArrayList<JSONObject> Footnotes = (ArrayList<JSONObject>)(jsonObject.get("footnote"));
                                for (JSONObject f : Footnotes){
                                    long dpi = (long)((JSONObject)f.get("link")).get("dimensionPositionId");
                                    long Mid = (long)((JSONObject)f.get("link")).get("memberId");
                                    String fn = (String)f.get("footnotes"+model.currLanguage);
                                    if(FootnoteMap.containsKey(dpi)){
                                        FootnoteMap.get(dpi).put(Mid,fn);
                                    }
                                    else {
                                        FootnoteMap.put(dpi, new HashMap<Long, String>());
                                    }
                                }
                                //Add the Dimensions to the dimension list ( These are the keys for the inner Trees within MemberMap )
                                for( String key : MemberMap.keySet()){
                                    try {
                                        dimensions.add(new String(key.getBytes("UTF-8")));
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                }
                                DimensionsList.setItems(FXCollections.observableArrayList(dimensions));

                                //IF SAVING DOCUMENT
                                if(toSave){
                                    if(FootNote) {
                                        model.SavetoDoc(Levels,CodeSets,MemberMap, id, model.getTitle(jsonObject, 0), FootnoteMap);
                                    }
                                    else{
                                        model.SavetoDoc(Levels,CodeSets,MemberMap,id,model.getTitle(jsonObject,0));
                                    }
                                }
                            } else {
                                ProductId.setText("Failed...try again");
                                Title.setText("No Cube Match");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Failed...");
                        }
                    }
                    else{
                        ProductId.setText("Failed...try again");
                        Title.setText("Table Number Invalid, Table Numbers must contain either 8 or 10 numbers. Ex: 41100003, 41-10-0003,41-10-0003-01");
                    }
                }
                else{
                    ProductId.setText("Failed...try again");
                    Title.setText("Table Number Invalid, Table Numbers must contain either 8 or 10 numbers. Ex: 41100003, 41-10-0003,41-10-0003-01");
                }
            }
        });
        //Step in to Levels, ie children
        StepIn.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                if((InnerDimensionsList.getSelectionModel().getSelectedIndex() != -1)){
                    if(currMembers.get(InnerDimensionsList.getSelectionModel().getSelectedIndex()).getChildren().size()!=0) {
                        currMembers = (ArrayList<com.company.Node<JSONObject>>) currMembers.get(InnerDimensionsList.getSelectionModel().getSelectedIndex()).getChildren();
                    }
                }

                if ((DimensionsList.getSelectionModel().getSelectedIndex() != -1)) {
                    currMembers = model.getInnerDim(MemberMap, dimensions.get(DimensionsList.getSelectionModel().getSelectedIndex()));
                }

                DimensionsList.getSelectionModel().clearSelection();
                InnerDimensionsList.getSelectionModel().clearSelection();
                InnerDimensionsList.setItems((FXCollections.observableArrayList(model.convert(currMembers))));
            }
        });

        //Not in use, Step Out function ( does not work )
        /*
        StepOut.setOnAction(new EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                System.out.println(currMembers);
                if(InnerDimensionsList.getSelectionModel().getSelectedIndex() != -1) {
                    String key = model.Search((String) currMembers.get(InnerDimensionsList.getSelectionModel().getSelectedIndex()).getData().get("memberName" + model.currLanguage), MemberMap);
                    System.out.println(key);
                    if (currMembers.get(InnerDimensionsList.getSelectionModel().getSelectedIndex()).getParent() != null) {
                        currMembers = currMembers.get(InnerDimensionsList.getSelectionModel().getSelectedIndex()).LevelSearch(currMembers.get(InnerDimensionsList.getSelectionModel().getSelectedIndex()), (String) currMembers.get(InnerDimensionsList.getSelectionModel().getSelectedIndex()).getData().get("memberName" + model.currLanguage),model.currLanguage);
                        if (currMembers == null) {
                            currMembers = model.getInnerDim(MemberMap, key);
                        }
                    }
                }
                InnerDimensionsList.getSelectionModel().clearSelection();
                InnerDimensionsList.setItems((FXCollections.observableArrayList(model.convert(currMembers))));
            }
        });
        */

        //ListView Action Listeners for Clicks
        DimensionsList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                Classification.clear();
                if (DimensionsList.getSelectionModel().getSelectedIndex() != -1) {
                    toCopy = DimensionsList.getSelectionModel().getSelectedItem();
                    //InnerDimensionsList.setItems(FXCollections.observableArrayList(model.getInnerDim(dimensions.get(DimensionsList.getSelectionModel().getSelectedIndex()))));
                }
            }
        });
        InnerDimensionsList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (InnerDimensionsList.getSelectionModel().getSelectedIndex() != -1) {
                    toCopy = InnerDimensionsList.getSelectionModel().getSelectedItem();
                    //Classification.setText(model.getClassification(MemberMap,CodeSets,InnerDimensionsList.getSelectionModel().getSelectedItem()));
                }
            }
        });
        SubjectList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                toCopy = SubjectList.getSelectionModel().getSelectedItem();
            }
        });
        SurveyList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                toCopy = SurveyList.getSelectionModel().getSelectedItem();
            }
        });

        primaryStage.setResizable(false);
        Pane pane1 = new Pane();
        Scene scene = new Scene(pane1,984,626);
        primaryStage.setTitle("Variable Tool");
        Submit.setDefaultButton(true);
        pane1.getChildren().addAll(LevelsCheck,Save,FootNoteCheck,Header,Header2,Header3,Header4,Header5,Header6,Header7,StepIn,Copy,Data,Entry,Submit,Exit,Batch,SubjectList,SurveyList,Title,ProductId,Classification,DimensionsList,InnerDimensionsList,rb1,rb2);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    public static void main(String[] args) {
        System.setProperty("file.encoding","UTF-8");
        Field charset = null;
        try {
            charset = Charset.class.getDeclaredField("defaultCharset");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        charset.setAccessible(true);
        try {
            charset.set(null,null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        launch(args);
    }
}
