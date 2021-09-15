package edu.curtin.krados.comp3003.assignment1;

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;

public class FileComparerUI extends Application
{
    public static void main(String[] args)
    {
        Application.launch(args);
    }
    
    private TableView<ComparisonResult> resultTable = new TableView<>();  
    private ProgressBar progressBar = new ProgressBar();
    
    @Override
    public void start(Stage stage)
    {
        stage.setTitle("Papers, Please");
        stage.setMinWidth(600);

        // Create toolbar
        Button compareBtn = new Button("Compare...");
        Button stopBtn = new Button("Stop");
        ToolBar toolBar = new ToolBar(compareBtn, stopBtn);
        
        // Set up button event handlers.
        compareBtn.setOnAction(event -> crossCompare(stage));
        stopBtn.setOnAction(event -> stopComparison());
        
        // Initialise progress bar
        progressBar.setProgress(0.0);
        
        TableColumn<ComparisonResult,String> file1Col = new TableColumn<>("File 1");
        TableColumn<ComparisonResult,String> file2Col = new TableColumn<>("File 2");
        TableColumn<ComparisonResult,String> similarityCol = new TableColumn<>("Similarity");
        
        // The following tell JavaFX how to extract information from a ComparisonResult 
        // object and put it into the three table columns.
        file1Col.setCellValueFactory(   
            (cell) -> new SimpleStringProperty(cell.getValue().getFile1()) );
            
        file2Col.setCellValueFactory(   
            (cell) -> new SimpleStringProperty(cell.getValue().getFile2()) );
            
        similarityCol.setCellValueFactory(  
            (cell) -> new SimpleStringProperty(
                String.format("%.1f%%", cell.getValue().getSimilarity() * 100.0)) );
          
        // Set and adjust table column widths.
        file1Col.prefWidthProperty().bind(resultTable.widthProperty().multiply(0.40));
        file2Col.prefWidthProperty().bind(resultTable.widthProperty().multiply(0.40));
        similarityCol.prefWidthProperty().bind(resultTable.widthProperty().multiply(0.20));            
        
        // Add the columns to the table.
        resultTable.getColumns().add(file1Col);
        resultTable.getColumns().add(file2Col);
        resultTable.getColumns().add(similarityCol);

        // Add the main parts of the UI to the window.
        BorderPane mainBox = new BorderPane();
        mainBox.setTop(toolBar);
        mainBox.setCenter(resultTable);
        mainBox.setBottom(progressBar);
        Scene scene = new Scene(mainBox);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();
    }

    public void addResult(ComparisonResult newResult)
    {
        resultTable.getItems().add(newResult);
    }

    //Adapted from code provided in Practical 3
    public void showError(String message)
    {
        stopComparison();

        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.CLOSE);
        a.setResizable(true);
        a.showAndWait();
    }
    
    private void crossCompare(Stage stage)
    {
        progressBar.setProgress(0.0); //Reset progress bar

        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(new File("."));
        dc.setTitle("Choose directory");
        File directory = dc.showDialog(stage);
        
        System.out.println("Comparing files within " + directory + "...");

        //TODO: Start producer thread(s) to find all non empty (size > 0) text files (.txt, .md, .java, .cs)

        //TODO: Create consumer thread pool for comparisons, with thread for each file, but a capped max thread count?
        //TODO: Update resultTable with .runLater() if similarity > 0.5
        //TODO: Add each ComparisonResult to newResults blocking queue to be consumed/written by a file writer thread
        //      into results.csv

        //TODO: For every discovered file or completed comparison, make .runLater() call that locks numCompared and
        //      numMaxComparison and uses them to calculate percent completion and update progressBar

        FileFinder finder = new FileFinder(directory.getPath(), this);
        new Thread(finder, "file-finder-thread").start();

        // Extremely fake way of demonstrating how to use the progress bar (noting that it can 
        // actually only be set to one value, from 0-1, at a time.)
        progressBar.setProgress(1.0);

        //TODO: Catch IOException and call (overloaded with exception?) stopComparison() to show error
    }
    
    private void stopComparison()
    {
        //TODO: Stop/interrupt producer thread pool and all threads

        System.out.println("Stopping comparison...");
    }
}
