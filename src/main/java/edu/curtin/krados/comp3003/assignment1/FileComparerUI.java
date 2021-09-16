package edu.curtin.krados.comp3003.assignment1;

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

public class FileComparerUI extends Application
{
    public static void main(String[] args)
    {
        Application.launch(args);
    }

    public static final double MIN_SIMILARITY = 0.5;

    private int missedFiles;
    private int numComparisons;
    private FileFinder finder;
    private ResultFileWriter writer;

    private TableView<ComparisonResult> resultTable = new TableView<>();  
    private ProgressBar progressBar = new ProgressBar();
    
    @Override
    public void start(Stage stage)
    {
        stage.setTitle("Plagiarism Detector!");
        stage.setMinWidth(600);

        // Create toolbar
        Button compareBtn = new Button("Compare...");
        Button stopBtn = new Button("Stop");
        ToolBar toolBar = new ToolBar(compareBtn, stopBtn);
        
        // Set up button event handlers.
        compareBtn.setOnAction(event -> crossCompare(stage));
        stopBtn.setOnAction(event -> stopComparison(finder, writer));
        
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

    public void addTextFile(String textFile)
    {
        System.out.println("Found text file to compare: " + textFile);
    }

    public void addMissedFile(String textFile, String reason)
    {
        System.out.println("Skipped file (" + reason + "): " + textFile);
        missedFiles++;
    }

    public void addComparison(ComparisonResult newComparison)
    {
        System.out.println("+ comparison"); ///
        if (newComparison.getSimilarity() > MIN_SIMILARITY)
        {
            System.out.println("Found two sufficiently similar files: " + newComparison.getString());
            resultTable.getItems().add(newComparison);
        }
    }

    public void incrementProgress(int numMaxComparisons)
    { //FIXME: Progress bar doesn't fill completely when searching GitHub directory (stuck at 642/780... nope, 675)
        numComparisons++;
        double newProgress = (double)numComparisons / (double)(numMaxComparisons - missedFiles); //TODO: Remove -missedFiles if not necessary/helpful
        progressBar.setProgress(newProgress);
        //System.out.println(newProgress + " = " + numComparisons + " / " + numMaxComparisons); ///
    }

    //Adapted from code provided in Practical 3
    public void showError(String message)
    {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.CLOSE);
        a.setResizable(true);
        a.showAndWait();
    }
    
    private void crossCompare(Stage stage)
    {
        //Reset progress bar
        progressBar.setProgress(0.0);
        numComparisons = 0;
        missedFiles = 0;

        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(new File("."));
        dc.setTitle("Choose directory");
        File directory = dc.showDialog(stage);
        //FIXME: ^^ NullPointerException thrown by this if you press cancel in pop up (LOW PRIORITY)

        System.out.println("Comparing files within " + directory + "...");

        FileFinder finder = new FileFinder(directory.getPath(), this);
        finder.start();

        ResultFileWriter writer = new ResultFileWriter(finder, this);
        writer.start();

        //TODO: Catch IOException and call (overloaded with exception?) stopComparison() to show error
    }
    
    private void stopComparison(FileFinder finder, ResultFileWriter writer)
    {
        System.out.println("Stopping comparison...");
        finder.stop();
//        writer.stop();
    }
}
