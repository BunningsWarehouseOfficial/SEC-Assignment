package edu.curtin.krados.comp3003.assignment1;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;

public class FileFinder implements Runnable
{
    public static final String[] TEXT_EXTENSIONS = { ".txt", ".md", ".java", ".cs" };

    private String searchPath;
    private FileComparerUI ui;

    //TODO: Move elsewhere?
    private List<String> textFiles = new ArrayList<>();
    private ExecutorService comparisonService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2); //TODO: Change number of threads

    private Object monitor = new Object();

    public FileFinder(String searchPath, FileComparerUI ui)
    {
        this.searchPath = searchPath;
        this.ui = ui;
    }

    @Override
    public void run()
    {
        //FIXME: App execution doesn't finish if UI is crossed off if FileFinder has begun
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
                                System.out.println("+ add to list: " + fileStr); ///
                                //TODO: Update progress bar

                                //TODO: Test to make sure these callables don't run until after textFiles is filled
                                comparisonService.submit(() -> //Submitting comparison callable to thread pool
                                {
                                    System.out.println("+ start comparison: " + fileStr); ///

                                    try
                                    {
                                        String primaryFile = Files.readString(file);

                                        for (String textFile : textFiles) //FIXME: Need to avoid symmetric comparisons
                                        {
                                            if (!fileStr.equals(textFile))
                                            {
                                                String secondaryFile = Files.readString(Paths.get(textFile));
                                                double similarity = calcSimilarity(primaryFile, secondaryFile);
                                                System.out.println(similarity);
                                                ComparisonResult newComparison = new ComparisonResult(
                                                        fileStr, textFile, similarity);

                                                Platform.runLater(() ->
                                                {
                                                    ui.addComparison(newComparison);
                                                });
                                            }
                                        }
                                    }
                                    catch(IOException e)
                                    {
                                        //TODO (perhaps also add inner IOException for secondaryFile)
                                    }

                                    System.out.println("- finish comparison: " + fileStr); ///
                                }); //TODO: Perhaps move actual callable code elsewhere?

                                //TODO: Re-calculate numMaxComparisons based on increasing numFiles: c = 0.5 * (f^2 - f) and
                                //      atomically update synchronized variable accessible by consumer comparison threads
                                //      OR use textFiles.size() once finished finding all files

                                /// ->
//                                Platform.runLater(() ->
//                                {
//                                    ui.addComparison(new ComparisonResult(fileStr, "test", 0.00));
//                                });
                                /// <-
                            }
                        }
                        catch (IOException e)
                        {
                            //Ignore a file whose size couldn't be checked
                            //TODO: Log to terminal (does it need a .runLater() or something?)
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            System.out.println("FINISHED WALKING FILE TREE"); ///
        }
        catch(IOException e)
        {
            Platform.runLater(() ->
            {
                // This error handling is a bit quick-and-dirty,
                // but it will suffice here. TODO: Fix so it isn't quick-and-dirty... maybe?
                //ui.showError(e.getClass().getName() + ": " + e.getMessage());
                ui.showError("An error occurred while finding files to compare.\n\n" + e.getMessage());
            });
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
            //System.out.println(fileExtension); ///
        }
        return isTextFile;
    }

    /**
     * Implementation of the LCS Dynamic Programming Algorithm for determining the similarity between two files.
     */
    private double calcSimilarity(String file1, String file2)
    {
        int rows    = file1.length() + 1;
        int columns = file2.length() + 1;
        int[][] subsolutions = new int[rows][columns];
        boolean[][] directionLeft = new boolean[rows][columns];

        //Fill first row and first column of subsolutions with zeros
        for (int mm = 0; mm < columns; mm++)
        {
            subsolutions[0][mm] = 0;
        }
        for (int nn = 0; nn < rows; nn++)
        {
            subsolutions[nn][0] = 0;
        }

        for (int ii = 1; ii <= file1.length(); ii++)
        {
            for (int jj = 1; jj <= file2.length(); jj++)
            {
                if (file1.charAt(ii - 1) == file2.charAt(jj - 1))
                {
                    subsolutions[ii][jj] = subsolutions[ii - 1][jj - 1] + 1;
                }
                else if (subsolutions[ii - 1][jj] > subsolutions[ii][jj - 1])
                {
                    subsolutions[ii][jj] = subsolutions[ii - 1][jj];
                    directionLeft[ii][jj] = true;
                }
                else
                {
                    subsolutions[ii][jj] = subsolutions[ii][jj - 1];
                    directionLeft[ii][jj] = false;
                }
            }
        }

        int matches = 0;
        int ii = file1.length();
        int jj = file2.length();

        while (ii > 0 && jj > 0)
        {
            if (file1.charAt(ii - 1) == file2.charAt(jj - 1))
            {
                matches += 1;
                ii -= 1;
                jj -= 1;
            }
            else if (directionLeft[ii][jj])
            {
                ii -= 1;
            }
            else
            {
                jj -= 1;
            }
        }
        return (double)(matches * 2) / (double)(file1.length() + file2.length());
    }
}
