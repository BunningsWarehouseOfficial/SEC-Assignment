package edu.curtin.krados.comp3003.assignment1;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

import javafx.application.Platform;

public class FileFinder
{
    public static final String[] TEXT_EXTENSIONS = { ".txt", ".md", ".java", ".cs" };

    private Thread thread;

    private String searchPath;
    private FileComparerUI ui;
    private FileComparer comparer;
    private List<String> textFiles = new LinkedList<>();

    public FileFinder(String searchPath, FileComparerUI ui)
    {
        this.searchPath = searchPath;
        this.ui = ui;
    }

    public void start()
    {
        thread = new Thread(this::findFiles, "file-finder-thread");
        thread.start();
    }

    public void stop()
    {
        if (thread == null)
        {
            throw new IllegalArgumentException("Writer thread doesn't exist");
        }

        if (comparer != null)
        {
            comparer.stop();
        }

        thread.interrupt(); //TODO: Test if we need InterruptedException catch block in findFiles()
        thread = null;
    }

    public void findFiles()
    {
        try
        {
            // Recurse through the directory tree
            Files.walkFileTree(Paths.get(searchPath), new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                {
                    String fileStr = file.toString();
                    if (isTextFile(fileStr))
                    {
                        try
                        {
                            //Check that the file is not empty
                            if (Files.size(file) > 0)
                            {
                                textFiles.add(fileStr);
                                Platform.runLater(() ->
                                {
                                    ui.displayDetail("Found text file to compare: " + fileStr);
                                });
                            }
                        }
                        catch (IOException e)
                        {
                            //Ignore a file whose size couldn't be checked
                            Platform.runLater(() ->
                            {
                                ui.displayDetail("Couldn't determine file size for " + fileStr);
                            });
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            String[] comparisonFiles = textFiles.toArray(new String[0]);

            //Start producer thread
            comparer = new FileComparer(comparisonFiles, ui);
            comparer.start();

            //Start consumer thread
            ResultFileWriter writer = new ResultFileWriter(comparer, ui);
            writer.start();
        }
        catch(IOException e)
        {
            Platform.runLater(() ->
            {
                ui.showError("An error occurred while finding files to compare.\n\n" + e.getMessage());
            });
            stop(); //TODO: Test/check this doesn't break weirdly
        }
    }

    //Adapted from code by EboMike, https://stackoverflow.com/a/3571239/12350950 (accessed 15 September 2021)
    private boolean isTextFile(String fileStr)
    {
        boolean isTextFile = false;

        int iiDot = fileStr.lastIndexOf('.');
        int iiDirectory = Math.max(fileStr.lastIndexOf('/'), fileStr.lastIndexOf('\\'));
        if (iiDot > iiDirectory)
        {
            String fileExtension = fileStr.substring(iiDot);
            if (Arrays.asList(TEXT_EXTENSIONS).contains(fileExtension))
            {
                isTextFile = true;
            }
        }
        return isTextFile;
    }
}
