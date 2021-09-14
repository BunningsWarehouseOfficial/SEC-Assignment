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

public class FileComparer extends Application
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
    
    private void crossCompare(Stage stage)
    {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(new File("."));
        dc.setTitle("Choose directory");
        File directory = dc.showDialog(stage);
        
        System.out.println("Comparing files within " + directory + "...");

        //TODO: Start producer thread(s) to find all non empty (size > 0) text files (.txt, .md, .java, .cs)
        //TODO: Re-calculate numMaxComparisons on the fly based on increasing numFiles: c = 0.5 * (f^2 - f) and
        //      atomically update synchronized variable accessible by consumer comparison threads

        //TODO: Create consumer thread pool for comparisons, with thread for each file, but a capped max thread count?
        //TODO: Update resultTable with .runLater() if similarity > 0.5
        //TODO: Call function that can only run one at a time (i.e. in GUI thread?) or that locks numCompared and
        //      numMaxComparison, and use them to calculate percent completion and update progressBar with .runLater()
        //TODO: Add each ComparisonResult to newResults blocking queue to be consumed/written by a file writer thread
        //      into results.csv
        
        // Extremely fake way of demonstrating how to use the progress bar (noting that it can 
        // actually only be set to one value, from 0-1, at a time.)
        progressBar.setProgress(1.0);

        // Extremely fake way of demonstrating how to update the table (noting that this shouldn't
        // just happen once at the end, but progressively as each result is obtained.)
        List<ComparisonResult> newResults = new ArrayList<>();
        newResults.add(new ComparisonResult("Example File 1", "Example File 2", 0.75));
        newResults.add(new ComparisonResult("Example File 1", "Example File 3", 0.31));
        newResults.add(new ComparisonResult("Example File 2", "Example File 3", 0.45));
        
        resultTable.getItems().setAll(newResults);        
        
        // progressBar.setProgress(0.0); // Reset progress bar after successful comparison?
    }
    
    private void stopComparison()
    {
        //TODO: Stop/interrupt producer thread pool and all threads

        System.out.println("Stopping comparison...");
    }
}
